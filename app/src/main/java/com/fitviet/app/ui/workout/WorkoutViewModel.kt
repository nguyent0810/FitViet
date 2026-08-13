package com.fitviet.app.ui.workout

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.repository.CommunityRepository
import com.fitviet.app.data.repository.ExerciseRepository
import com.fitviet.app.data.repository.MonthlyPlanRepository
import com.fitviet.app.data.repository.WorkoutRepository
import com.fitviet.app.domain.TodayMonthlyPlanCard
import java.time.LocalDate
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Not private: ProgramDayWorkoutPlanner.estimateDurationMinutes and
// MonthlyPlanDayCardEstimator/MonthlyPlanGenerator's time estimates need the same value the
// runtime rest timer actually uses so estimated session length matches what the app really runs.
const val DEFAULT_REST_SECONDS = 60
private const val SESSION_DAY_LABEL = "Thân trên"
private const val ACTION_DEBOUNCE_MILLIS = 350L

data class WorkoutUiState(
    val isLoading: Boolean = true,
    val dayLabel: String = SESSION_DAY_LABEL,
    val blocks: List<WorkoutBlockPlan> = emptyList(),
    val currentBlockIndex: Int = 0,
    val phase: WorkoutPhase = WorkoutPhase.AwaitingPlanGeneration,
    // Straight-block sub-state
    val currentSetIndex: Int = 0,
    val restSecondsRemaining: Int = 0,
    val editableWeightKg: Double = 0.0,
    val editableReps: Int = 0,
    val loggedSetsThisExercise: List<LoggedSet> = emptyList(),
    // Superset sub-state
    val supersetRound: Int = 1,
    val supersetSub: Int = 0,
    val supersetRestSecondsRemaining: Int = 0,
    val selectedTechnique: SetTechnique = SetTechnique.SUPERSET,
    val isTechniquePickerOpen: Boolean = false,
    // Session-level
    val sessionElapsedSeconds: Int = 0,
    val sessionTotalVolumeKg: Double = 0.0,
    val sessionTotalSets: Int = 0,
    /** Computed once at session-finish time (see `finishSession()`), not observed live — matches
     * Dashboard's own streak definition via [com.fitviet.app.domain.DashboardStatsCalculator]. */
    val sessionStreakDays: Int = 0,
    /** Guards the "share to Community" action against a double-tap creating two posts — reset per
     * session since each new session is a fresh [WorkoutUiState]. */
    val sessionShared: Boolean = false,
) {
    val currentBlock: WorkoutBlockPlan? get() = blocks.getOrNull(currentBlockIndex)
}

class WorkoutViewModel(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val communityRepository: CommunityRepository,
    private val monthlyPlanRepository: MonthlyPlanRepository,
    private val databaseReady: Deferred<Unit>,
    // Set when entered from a "Hit & Run" (Gate 63+) monthly-plan day (the Today card, or a
    // regenerate/preview flow). Null for the free-standing entry points (bottom-nav FAB, the
    // no-arg "resolve today" path) — redesign Gate 2b removed this ViewModel's other former
    // entry point (a program-day session started via `programId`); every program-triggered
    // session now goes through `MonthlyPlanRepository.generate()` instead (see
    // `ProgramsViewModel.generateFromProgram`), so there's no longer a second id this can carry.
    private val monthlyPlanDayId: Long? = null,
    // Injectable so tests can supply a controllable fake — android.os.SystemClock is stubbed to a
    // constant 0 in plain JVM unit tests, which would make every debounced action a permanent no-op.
    private val elapsedRealtimeMillis: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private var sessionId: Long = 0L
    private var restJob: Job? = null
    private var elapsedJob: Job? = null
    private var sessionInitJob: Job? = null
    private var sessionFinishJob: Job? = null
    /** Set inserts are intentionally launched off the UI action path, but session completion (and
     * especially the monthly-plan PR hook) must not overtake an insert that is still suspended in
     * Room. Access is confined to the main dispatcher used by [viewModelScope]. */
    private val pendingSetLogJobs = mutableListOf<Job>()
    private var lastActionAtMillis = 0L

    init {
        loadInitialSession()
    }

    /** Entry point for both the initial load and "Làm lại" (Gate 63+ added the [monthlyPlanDayId]
     * branch; redesign Gate 1c replaced the old duration-picker fallback with
     * [resolveTodaySessionAndStart]; redesign Gate 2b removed the sibling `programId` branch this
     * used to also have — see this class's own constructor doc). */
    private fun loadInitialSession() {
        if (monthlyPlanDayId != null) {
            startMonthlyPlanDaySession(monthlyPlanDayId)
        } else {
            resolveTodaySessionAndStart()
        }
    }

    /** Resolves [monthlyPlanDayId]'s exercises and starts logging immediately — the plan already
     * determines every set, nothing for the user to choose. Passes [monthlyPlanDayId] through to
     * [WorkoutRepository.startSession] so the created session row carries the FK the regenerate
     * lock rule depends on — see [com.fitviet.app.data.repository.MonthlyPlanRepository]. */
    private fun startMonthlyPlanDaySession(monthlyPlanDayId: Long) {
        sessionInitJob?.cancel()
        sessionInitJob = viewModelScope.launch {
            databaseReady.await()
            runMonthlyPlanDaySession(monthlyPlanDayId)
        }
    }

    /** The actual resolve/build/start work behind [startMonthlyPlanDaySession] — split out as a
     * plain suspend helper (no job management of its own) so [resolveTodaySessionAndStart] can
     * call it from inside its own already-launched job: calling [startMonthlyPlanDaySession]
     * directly there would cancel that very job via its own `sessionInitJob?.cancel()`. Falls back
     * to [WorkoutPhase.NoSessionToday]
     * ([NoSessionReason.UNAVAILABLE]) if the day somehow has no resolvable exercises (e.g. a
     * stale/deleted plan) — should be unreachable when the caller already resolved a `Training`
     * outcome via [com.fitviet.app.data.repository.MonthlyPlanRepository.observeTodaySession], but
     * stays defensive rather than leaving a blank screen. */
    private suspend fun runMonthlyPlanDaySession(monthlyPlanDayId: Long) {
        val resolved = MonthlyPlanDayWorkoutPlanner.resolveDay(monthlyPlanDayId, monthlyPlanRepository, exerciseRepository, workoutRepository)
        val blocks = resolved?.let { ProgramDayWorkoutPlanner.buildBlocks(it.items) }.orEmpty()
        if (resolved == null || blocks.isEmpty()) {
            _uiState.value = WorkoutUiState(isLoading = false, phase = WorkoutPhase.NoSessionToday(NoSessionReason.UNAVAILABLE))
            return
        }
        sessionId = workoutRepository.startSession(
            dayLabel = resolved.titleVi,
            startedAtMillis = System.currentTimeMillis(),
            monthlyPlanDayId = monthlyPlanDayId,
        )
        _uiState.value = resetForBlock(
            WorkoutUiState(blocks = blocks, isLoading = false, dayLabel = resolved.titleVi),
            0,
            blocks.firstOrNull(),
        )
        startElapsedTicker()
    }

    /** Redesign Gate 1c — the no-arg entry point's replacement for the old duration picker. Reuses
     * [com.fitviet.app.data.repository.MonthlyPlanRepository.observeTodaySession], the exact same
     * resolution Dashboard's Today card uses, so the two never disagree about what "today" means.
     * `Training` starts logging immediately via [runMonthlyPlanDaySession]; `RestDay`/`Unavailable`/
     * `PlanFinished` land on [WorkoutPhase.NoSessionToday] (there's nothing to log); `NoPlan` lands
     * on [WorkoutPhase.AwaitingPlanGeneration], which [WorkoutScreen] reacts to by navigating to
     * Quick Generate — this ViewModel has no navigation of its own. */
    private fun resolveTodaySessionAndStart() {
        sessionInitJob?.cancel()
        sessionInitJob = viewModelScope.launch {
            databaseReady.await()
            when (val card = monthlyPlanRepository.observeTodaySession(LocalDate.now()).first()) {
                is TodayMonthlyPlanCard.Training -> runMonthlyPlanDaySession(card.dayId)
                TodayMonthlyPlanCard.RestDay ->
                    _uiState.value = WorkoutUiState(isLoading = false, phase = WorkoutPhase.NoSessionToday(NoSessionReason.REST_DAY))
                is TodayMonthlyPlanCard.Unavailable ->
                    _uiState.value = WorkoutUiState(isLoading = false, phase = WorkoutPhase.NoSessionToday(NoSessionReason.UNAVAILABLE))
                TodayMonthlyPlanCard.PlanFinished ->
                    _uiState.value = WorkoutUiState(isLoading = false, phase = WorkoutPhase.NoSessionToday(NoSessionReason.PLAN_FINISHED))
                TodayMonthlyPlanCard.NoPlan ->
                    _uiState.value = WorkoutUiState(isLoading = false, phase = WorkoutPhase.AwaitingPlanGeneration)
            }
        }
    }

    /**
     * Every state-mutating action (set/round completion, rest skip, block advance, reset) runs
     * through this — a fast double-tap can fire two click events before Compose recomposes the
     * button away, and several of these transitions don't change `phase` (e.g. completing
     * superset A only flips `supersetSub`), so a phase check alone doesn't catch every case.
     */
    private inline fun debounced(action: () -> Unit) {
        val now = elapsedRealtimeMillis()
        if (now - lastActionAtMillis < ACTION_DEBOUNCE_MILLIS) return
        lastActionAtMillis = now
        action()
    }

    /** For a monthly-plan-day session (Gate 63+), "Làm lại" restarts that same day's session
     * again; redesign Gate 1c's no-arg branch re-resolves today's monthly-plan session the same
     * way the initial load did (via [resolveTodaySessionAndStart]) rather than dropping to the
     * deleted duration picker. The branches are kept explicit (rather than always routing through
     * [loadInitialSession]) so the monthly-plan-day flow keeps resetting synchronously with no
     * loading-state frame, exactly as it did before Gate 24. */
    fun resetWorkout() = debounced {
        restJob?.cancel()
        elapsedJob?.cancel()
        sessionInitJob?.cancel()
        if (monthlyPlanDayId != null) {
            _uiState.value = WorkoutUiState()
            startMonthlyPlanDaySession(monthlyPlanDayId)
        } else {
            _uiState.value = WorkoutUiState()
            resolveTodaySessionAndStart()
        }
    }

    // ---- Straight block ----

    fun completeCurrentSet() = debounced {
        val state = _uiState.value
        if (state.phase != WorkoutPhase.StraightLog) return@debounced
        val block = (state.currentBlock as? WorkoutBlockPlan.Straight)?.plan ?: return@debounced
        val order = exerciseOrdersForBlock(state.blocks, state.currentBlockIndex).first()
        val logged = LoggedSet(
            exerciseId = block.exercise.id,
            exerciseOrder = order,
            setIndex = state.currentSetIndex,
            weightKg = state.editableWeightKg,
            reps = state.editableReps,
        )
        persistSet(logged)

        val loggedSets = state.loggedSetsThisExercise + logged
        if (state.currentSetIndex >= block.plannedSets.lastIndex) {
            _uiState.update { it.copy(phase = WorkoutPhase.StraightBlockDone, loggedSetsThisExercise = loggedSets) }
        } else {
            val nextIndex = state.currentSetIndex + 1
            val nextPlanned = block.plannedSets[nextIndex]
            _uiState.update {
                it.copy(
                    currentSetIndex = nextIndex,
                    loggedSetsThisExercise = loggedSets,
                    editableWeightKg = nextPlanned.weightKg,
                    editableReps = nextPlanned.reps,
                    phase = WorkoutPhase.StraightRest,
                    restSecondsRemaining = DEFAULT_REST_SECONDS,
                )
            }
            startRestTimer()
        }
    }

    fun skipRest() = debounced {
        if (_uiState.value.phase != WorkoutPhase.StraightRest) return@debounced
        restJob?.cancel()
        _uiState.update { it.copy(phase = WorkoutPhase.StraightLog, restSecondsRemaining = 0) }
    }

    fun addRest() {
        _uiState.update { it.copy(restSecondsRemaining = it.restSecondsRemaining + 15) }
    }

    fun adjustEditableWeight(deltaKg: Double) {
        _uiState.update { it.copy(editableWeightKg = (it.editableWeightKg + deltaKg).coerceAtLeast(0.0)) }
    }

    fun adjustEditableReps(delta: Int) {
        _uiState.update { it.copy(editableReps = (it.editableReps + delta).coerceAtLeast(0)) }
    }

    private fun startRestTimer() {
        restJob?.cancel()
        restJob = viewModelScope.launch {
            while (_uiState.value.restSecondsRemaining > 0) {
                delay(1_000)
                _uiState.update { it.copy(restSecondsRemaining = (it.restSecondsRemaining - 1).coerceAtLeast(0)) }
            }
            if (_uiState.value.phase == WorkoutPhase.StraightRest) {
                _uiState.update { it.copy(phase = WorkoutPhase.StraightLog) }
            }
        }
    }

    // ---- Superset block ----

    fun supersetNext() = debounced {
        val state = _uiState.value
        if (state.phase != WorkoutPhase.SupersetWork) return@debounced
        val block = (state.currentBlock as? WorkoutBlockPlan.Superset)?.plan ?: return@debounced
        val orders = exerciseOrdersForBlock(state.blocks, state.currentBlockIndex)

        // Reuses the same editable weight/reps fields the straight-block flow uses — only one
        // exercise (A or B) is ever "current" at a time, same as only one set is current there.
        if (state.supersetSub == 0) {
            persistSet(LoggedSet(block.exerciseA.id, orders[0], state.supersetRound - 1, state.editableWeightKg, state.editableReps))
            _uiState.update { it.copy(supersetSub = 1, editableWeightKg = block.plannedB.weightKg, editableReps = block.plannedB.reps) }
        } else {
            persistSet(LoggedSet(block.exerciseB.id, orders[1], state.supersetRound - 1, state.editableWeightKg, state.editableReps))
            if (state.supersetRound < block.totalRounds) {
                _uiState.update { it.copy(phase = WorkoutPhase.SupersetRest, supersetRestSecondsRemaining = DEFAULT_REST_SECONDS) }
                startSupersetRestTimer()
            } else {
                _uiState.update { it.copy(supersetRound = block.totalRounds + 1, supersetSub = 0, phase = WorkoutPhase.SupersetBlockDone) }
            }
        }
    }

    fun skipSupersetRest() = debounced {
        if (_uiState.value.phase != WorkoutPhase.SupersetRest) return@debounced
        restJob?.cancel()
        val plannedA = (_uiState.value.currentBlock as? WorkoutBlockPlan.Superset)?.plan?.plannedA
        _uiState.update {
            it.copy(
                phase = WorkoutPhase.SupersetWork,
                supersetSub = 0,
                supersetRound = it.supersetRound + 1,
                supersetRestSecondsRemaining = 0,
                editableWeightKg = plannedA?.weightKg ?: it.editableWeightKg,
                editableReps = plannedA?.reps ?: it.editableReps,
            )
        }
    }

    fun addSupersetRest() {
        _uiState.update { it.copy(supersetRestSecondsRemaining = it.supersetRestSecondsRemaining + 15) }
    }

    private fun startSupersetRestTimer() {
        restJob?.cancel()
        restJob = viewModelScope.launch {
            while (_uiState.value.supersetRestSecondsRemaining > 0) {
                delay(1_000)
                _uiState.update { it.copy(supersetRestSecondsRemaining = (it.supersetRestSecondsRemaining - 1).coerceAtLeast(0)) }
            }
            if (_uiState.value.phase == WorkoutPhase.SupersetRest) {
                val plannedA = (_uiState.value.currentBlock as? WorkoutBlockPlan.Superset)?.plan?.plannedA
                _uiState.update {
                    it.copy(
                        phase = WorkoutPhase.SupersetWork,
                        supersetSub = 0,
                        supersetRound = it.supersetRound + 1,
                        supersetRestSecondsRemaining = 0,
                        editableWeightKg = plannedA?.weightKg ?: it.editableWeightKg,
                        editableReps = plannedA?.reps ?: it.editableReps,
                    )
                }
            }
        }
    }

    // ---- Technique picker (2c) ----

    fun openTechniquePicker() = _uiState.update { it.copy(isTechniquePickerOpen = true) }
    fun closeTechniquePicker() = _uiState.update { it.copy(isTechniquePickerOpen = false) }
    fun selectTechnique(technique: SetTechnique) =
        _uiState.update { it.copy(selectedTechnique = technique, isTechniquePickerOpen = false) }

    // ---- Block/session transitions ----

    fun advanceToNextBlock() = debounced {
        val state = _uiState.value
        if (state.phase != WorkoutPhase.StraightBlockDone && state.phase != WorkoutPhase.SupersetBlockDone) return@debounced
        val nextIndex = state.currentBlockIndex + 1
        val nextBlock = state.blocks.getOrNull(nextIndex)
        if (nextBlock == null) {
            finishSession()
        } else {
            _uiState.value = resetForBlock(state, nextIndex, nextBlock)
        }
    }

    private fun resetForBlock(state: WorkoutUiState, index: Int, block: WorkoutBlockPlan?): WorkoutUiState = when (block) {
        is WorkoutBlockPlan.Straight -> state.copy(
            currentBlockIndex = index,
            phase = WorkoutPhase.StraightLog,
            currentSetIndex = 0,
            editableWeightKg = block.plan.plannedSets.first().weightKg,
            editableReps = block.plan.plannedSets.first().reps,
            loggedSetsThisExercise = emptyList(),
        )
        is WorkoutBlockPlan.Superset -> state.copy(
            currentBlockIndex = index,
            phase = WorkoutPhase.SupersetWork,
            supersetRound = 1,
            supersetSub = 0,
            editableWeightKg = block.plan.plannedA.weightKg,
            editableReps = block.plan.plannedA.reps,
        )
        null -> state.copy(phase = WorkoutPhase.SessionFinished)
    }

    /** Awaits [WorkoutRepository.completeSession] before reading the streak (rather than the old
     * fire-and-forget write + immediate synchronous phase flip) so `getCurrentStreakDays` sees
     * today's just-completed session already persisted — otherwise a share created right after
     * landing on [WorkoutPhase.SessionFinished] could read yesterday's streak instead of today's.
     * For a monthly-plan-day session, the PR-bump hook runs right after — it must come after
     * [WorkoutRepository.completeSession], not before, since [MonthlyPlanRepository.onMonthlyPlanSessionCompleted]
     * only considers a personal best from a session whose `completedAt` is already set. */
    private fun finishSession() {
        if (sessionFinishJob?.isActive == true) return
        elapsedJob?.cancel()
        val state = _uiState.value
        val targetSessionId = sessionId
        sessionFinishJob = viewModelScope.launch {
            val setLogJobs = pendingSetLogJobs.toList()
            pendingSetLogJobs.clear()
            setLogJobs.forEach { it.join() }
            workoutRepository.completeSession(
                sessionId = targetSessionId,
                completedAtMillis = System.currentTimeMillis(),
                totalVolumeKg = state.sessionTotalVolumeKg,
                durationSeconds = state.sessionElapsedSeconds,
            )
            monthlyPlanDayId?.let { monthlyPlanRepository.onMonthlyPlanSessionCompleted(it) }
            val streakDays = workoutRepository.getCurrentStreakDays(LocalDate.now())
            _uiState.update { it.copy(phase = WorkoutPhase.SessionFinished, sessionStreakDays = streakDays) }
        }
    }

    /** Feature #4 (Gate 40) — creates a real workout-share Community post (via
     * [CommunityRepository.shareWorkout]) from this session's already-computed summary.
     *
     * The check-and-set for [WorkoutUiState.sessionShared] happens synchronously here, in the
     * caller's own call frame, *before* [viewModelScope.launch] ever runs — not inside the
     * launched coroutine after the suspending [CommunityRepository.shareWorkout] call. That
     * ordering is load-bearing: an earlier version flipped the flag only after the suspending
     * call returned, which left a real window open on real Android (where the DAO calls inside
     * `shareWorkout` genuinely suspend) for a second rapid tap to also read `sessionShared ==
     * false` and create a duplicate post — caught by independent review. Doing the check-and-set
     * synchronously closes that window entirely, which a debounce (real-time-clock-based, used
     * elsewhere in this file) only would have narrowed. */
    /** Only reachable in practice from [WorkoutScreen]'s `SessionFinished`-phase branch, but the
     * phase guard is enforced here too — this ViewModel shouldn't rely solely on its one current UI
     * call site to keep an in-progress/abandoned session from posting a partial summary. */
    fun shareToCommunity() {
        val state = _uiState.value
        if (state.phase != WorkoutPhase.SessionFinished) return
        if (state.sessionShared) return
        _uiState.update { it.copy(sessionShared = true) }
        viewModelScope.launch {
            communityRepository.shareWorkout(
                // Redesign Gate 2b — no program-day session path remains to populate this (see
                // this class's own constructor doc); a monthly-plan-day session never had a
                // program title to begin with. `CommunityScreen`'s programTitle-present rendering
                // branch is now permanently dead, left as-is pending a later gate's cleanup.
                programTitle = null,
                dayLabel = state.dayLabel,
                durationSeconds = state.sessionElapsedSeconds,
                totalVolumeKg = state.sessionTotalVolumeKg,
                streakDays = state.sessionStreakDays,
            )
        }
    }

    private fun persistSet(set: LoggedSet) {
        _uiState.update {
            it.copy(
                sessionTotalVolumeKg = it.sessionTotalVolumeKg + set.weightKg * set.reps,
                sessionTotalSets = it.sessionTotalSets + 1,
            )
        }
        // Capture now: resetWorkout() can replace the ViewModel's current session id before a
        // suspended/queued insert actually invokes the repository.
        val targetSessionId = sessionId
        pendingSetLogJobs += viewModelScope.launch { workoutRepository.logSet(targetSessionId, set) }
    }

    private fun startElapsedTicker() {
        elapsedJob?.cancel()
        elapsedJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _uiState.update { it.copy(sessionElapsedSeconds = it.sessionElapsedSeconds + 1) }
            }
        }
    }

    override fun onCleared() {
        restJob?.cancel()
        elapsedJob?.cancel()
        sessionFinishJob?.cancel()
    }

    /** Exercise position(s) within the whole session, for [LoggedSet.exerciseOrder] — 2 for a superset block, 1 otherwise. */
    private fun exerciseOrdersForBlock(blocks: List<WorkoutBlockPlan>, blockIndex: Int): List<Int> {
        var counter = 0
        for (i in 0 until blockIndex) {
            counter += when (blocks[i]) {
                is WorkoutBlockPlan.Straight -> 1
                is WorkoutBlockPlan.Superset -> 2
            }
        }
        return when (blocks[blockIndex]) {
            is WorkoutBlockPlan.Straight -> listOf(counter)
            is WorkoutBlockPlan.Superset -> listOf(counter, counter + 1)
        }
    }

    class Factory(
        private val exerciseRepository: ExerciseRepository,
        private val workoutRepository: WorkoutRepository,
        private val communityRepository: CommunityRepository,
        private val monthlyPlanRepository: MonthlyPlanRepository,
        private val databaseReady: Deferred<Unit>,
        private val monthlyPlanDayId: Long? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = WorkoutViewModel(
            exerciseRepository = exerciseRepository,
            workoutRepository = workoutRepository,
            communityRepository = communityRepository,
            monthlyPlanRepository = monthlyPlanRepository,
            databaseReady = databaseReady,
            monthlyPlanDayId = monthlyPlanDayId,
        ) as T
    }
}
