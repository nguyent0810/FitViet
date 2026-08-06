package com.fitviet.app.ui.workout

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.repository.CommunityRepository
import com.fitviet.app.data.repository.ExerciseRepository
import com.fitviet.app.data.repository.ProgramRepository
import com.fitviet.app.data.repository.WorkoutRepository
import java.time.LocalDate
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Not private: WorkoutTimeBudgetPlanner's time estimate needs the same value the runtime rest
// timer actually uses (see the comment at its estimateSeconds call site) so estimated session
// length matches what the app really runs.
const val DEFAULT_REST_SECONDS = 60
private const val SESSION_DAY_LABEL = "Thân trên"
private const val ACTION_DEBOUNCE_MILLIS = 350L

data class WorkoutUiState(
    val isLoading: Boolean = true,
    val dayLabel: String = SESSION_DAY_LABEL,
    val blocks: List<WorkoutBlockPlan> = emptyList(),
    val currentBlockIndex: Int = 0,
    val phase: WorkoutPhase = WorkoutPhase.SelectingDuration,
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
    /** Feature #4 (Gate 40) — the program this session came from, null for an ad-hoc
     * duration-picker session (mismatch #4: not every session has a program at all). Only
     * meaningful once [phase] reaches [WorkoutPhase.SessionFinished]; used for the share card. */
    val programTitle: String? = null,
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
    private val programRepository: ProgramRepository,
    private val communityRepository: CommunityRepository,
    private val databaseReady: Deferred<Unit>,
    // Set when entered from WorkoutPreview's "Begin workout" (Gate 24) — the session is then built
    // from this program's real schedule for today instead of the generic duration-picker flow.
    // Null for the free-standing entry points (bottom-nav FAB, dashboard "Start workout").
    private val programId: Long? = null,
    // Injectable so tests can supply a controllable fake — android.os.SystemClock is stubbed to a
    // constant 0 in plain JVM unit tests, which would make every debounced action a permanent no-op.
    private val elapsedRealtimeMillis: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private var sessionId: Long = 0L
    private var loadedExercises: List<ExerciseEntity> = emptyList()
    private var restJob: Job? = null
    private var elapsedJob: Job? = null
    private var sessionInitJob: Job? = null
    private var lastActionAtMillis = 0L

    init {
        loadInitialSession()
    }

    /** Entry point for both the initial load and "Làm lại" (Gate 24 added the [programId] branch;
     * the pre-existing picker flow below is otherwise unchanged). */
    private fun loadInitialSession() {
        if (programId != null) {
            startProgramDaySession(programId)
        } else {
            loadExercisesAndShowDurationPicker()
        }
    }

    /** Resolves [programId]'s schedule for today and starts logging immediately — there's nothing
     * for a duration picker to choose, the program already determines every set. Falls back to the
     * generic picker if the program has no schedule/exercises for today rather than stranding the
     * user on a blank screen (e.g. a program whose schedule hasn't finished seeding yet). */
    private fun startProgramDaySession(programId: Long) {
        sessionInitJob?.cancel()
        sessionInitJob = viewModelScope.launch {
            databaseReady.await()
            val resolved = ProgramDayWorkoutPlanner.resolveToday(programId, programRepository, exerciseRepository, workoutRepository)
            val blocks = resolved?.let { ProgramDayWorkoutPlanner.buildBlocks(it.items) }.orEmpty()
            if (resolved == null || blocks.isEmpty()) {
                showDurationPicker()
                return@launch
            }
            // Fetched here (not re-derived from the session row later) per the gate plan's mismatch
            // #4 — WorkoutSessionEntity has no programTitle column, and completeSession() never
            // writes programId either, so this is the only point that actually has it.
            val programTitle = programRepository.getById(programId)?.titleVi
            sessionId = workoutRepository.startSession(dayLabel = resolved.titleVi, startedAtMillis = System.currentTimeMillis())
            _uiState.value = resetForBlock(
                WorkoutUiState(blocks = blocks, isLoading = false, dayLabel = resolved.titleVi, programTitle = programTitle),
                0,
                blocks.firstOrNull(),
            )
            startElapsedTicker()
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

    /** Loads the catalog once and lands on the "chọn thời gian tập" picker (Gate 10, not part of the
     * original 12-screen spec) — the actual session/blocks aren't built until [selectDuration], no
     * point starting a Room session row for a workout the user hasn't configured yet. */
    private fun loadExercisesAndShowDurationPicker() {
        // Cancel any in-flight init so rapid "Làm lại" taps can't race to create multiple
        // sessions and leave sessionId pointing at whichever insert happened to finish last.
        sessionInitJob?.cancel()
        sessionInitJob = viewModelScope.launch {
            databaseReady.await()
            showDurationPicker()
        }
    }

    /** Plain suspend helper (no job management of its own) so [startProgramDaySession] can fall
     * back to it from inside its own already-launched job — calling [loadExercisesAndShowDurationPicker]
     * directly there would cancel that very job via its own `sessionInitJob?.cancel()`. */
    private suspend fun showDurationPicker() {
        loadedExercises = exerciseRepository.getAll()
        _uiState.value = WorkoutUiState(isLoading = false, phase = WorkoutPhase.SelectingDuration)
    }

    /** [minutes] is `null` for "Không giới hạn" (the original curated demo session, unchanged
     * from before Gate 10); otherwise builds a session sized to fit that time budget from the
     * full exercise catalog (Gate 9) via [WorkoutTimeBudgetPlanner]. */
    fun selectDuration(minutes: Int?) = debounced {
        if (_uiState.value.phase != WorkoutPhase.SelectingDuration) return@debounced
        val blocks = if (minutes == null) {
            WorkoutPlanSeed.buildBlocks(loadedExercises)
        } else {
            WorkoutTimeBudgetPlanner.buildBlocks(loadedExercises, minutes)
        }
        // Guards against a mismatched/empty catalog (e.g. a seeding gap) producing zero blocks —
        // without this, resetForBlock's block == null branch jumps straight to SessionFinished
        // without ever calling completeSession(), leaving a session row that's never marked
        // complete while its elapsed ticker keeps running. Nothing to start, so stay on the picker.
        if (blocks.isEmpty()) return@debounced
        sessionInitJob?.cancel()
        sessionInitJob = viewModelScope.launch {
            sessionId = workoutRepository.startSession(dayLabel = SESSION_DAY_LABEL, startedAtMillis = System.currentTimeMillis())
            _uiState.value = resetForBlock(WorkoutUiState(blocks = blocks, isLoading = false), 0, blocks.firstOrNull())
            startElapsedTicker()
        }
    }

    /** For a program-day session (Gate 24), "Làm lại" restarts that same day's session again
     * rather than dropping to the generic picker, which this flow never shows in the first place.
     * The two branches are kept explicit (rather than always routing through [loadInitialSession])
     * so the pre-existing picker flow keeps resetting synchronously with no loading-state frame,
     * exactly as it did before Gate 24. */
    fun resetWorkout() = debounced {
        restJob?.cancel()
        elapsedJob?.cancel()
        sessionInitJob?.cancel()
        if (programId != null) {
            _uiState.value = WorkoutUiState()
            startProgramDaySession(programId)
        } else {
            _uiState.value = WorkoutUiState(isLoading = false, phase = WorkoutPhase.SelectingDuration)
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
     * landing on [WorkoutPhase.SessionFinished] could read yesterday's streak instead of today's. */
    private fun finishSession() {
        elapsedJob?.cancel()
        val state = _uiState.value
        viewModelScope.launch {
            workoutRepository.completeSession(
                sessionId = sessionId,
                completedAtMillis = System.currentTimeMillis(),
                totalVolumeKg = state.sessionTotalVolumeKg,
                durationSeconds = state.sessionElapsedSeconds,
            )
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
    fun shareToCommunity() {
        val state = _uiState.value
        if (state.sessionShared) return
        _uiState.update { it.copy(sessionShared = true) }
        viewModelScope.launch {
            communityRepository.shareWorkout(
                programTitle = state.programTitle,
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
        viewModelScope.launch { workoutRepository.logSet(sessionId, set) }
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
        private val programRepository: ProgramRepository,
        private val communityRepository: CommunityRepository,
        private val databaseReady: Deferred<Unit>,
        private val programId: Long? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WorkoutViewModel(exerciseRepository, workoutRepository, programRepository, communityRepository, databaseReady, programId) as T
    }
}
