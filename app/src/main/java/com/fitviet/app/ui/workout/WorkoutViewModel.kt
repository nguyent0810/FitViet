package com.fitviet.app.ui.workout

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.repository.CommunityRepository
import com.fitviet.app.data.repository.ExerciseRepository
import com.fitviet.app.data.repository.MonthlyPlanRepository
import com.fitviet.app.data.repository.WorkoutRepository
import com.fitviet.app.domain.MonthlyPlanProgress
import com.fitviet.app.domain.MonthlyPlanProgressSummary
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
    /** Redesign Gate 4a-i — set only while [WorkoutPhase.StraightRest] is the *inter-exercise* rest
     * between the block just finished and this index's block (as opposed to an ordinary
     * intra-exercise rest between two sets of the same block, where this stays null). Read by
     * [WorkoutViewModel]'s own rest-completion paths ([WorkoutViewModel.skipRest], the rest
     * timer's zero-out) to decide whether ending rest should hand off to [WorkoutViewModel.resetForBlock]
     * (a new block) or just flip back to [WorkoutPhase.StraightLog] (same block, next set) — see the
     * mock's own `completeSet`, which folds "advance to the next exercise" into the same rest
     * transition rather than the old interstitial `StraightBlockDone` screen (removed in Gate 4a-ii). */
    val pendingNextBlockIndex: Int? = null,
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
    /** Redesign Gate 4b — the finished screen's "Buổi N/M" line, via
     * [com.fitviet.app.domain.MonthlyPlanProgress.summarize] (same source the Kế hoạch tab's own
     * progress card uses, so the two never disagree). Both null together when there's no active
     * plan row to read (shouldn't happen in practice — reaching [WorkoutPhase.SessionFinished] at
     * all requires a monthly-plan-day session — but this stays a display helper: null means the
     * screen omits the clause entirely rather than showing a wrong number, same convention
     * [com.fitviet.app.domain.MonthlyPlanProgress.dayOfPlan] already sets). */
    val sessionNumberInPlan: Int? = null,
    val sessionTotalInPlan: Int? = null,
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
    /** Phase 2 checkpoint fix — only ever set by [resolveTodaySessionAndStart] (the bare `workout`
     * route's own "today" resolution), never by the [monthlyPlanDayId]-carrying entry point (which
     * already knows its own day id). Lets [resetWorkout]'s bare-route "Làm lại" branch restart the
     * SAME day it originally resolved, bypassing `observeTodaySession`'s fresh re-resolution — which
     * would otherwise now correctly report `Completed` for a day the user just finished, refusing
     * the very redo "Làm lại" exists to allow.
     *
     * Review finding (Gate 4a-ii) — [resetWorkout] has no production UI call site as of this gate
     * (its own header chip was removed to match the mock; see [WorkoutScreen]'s `WorkoutHeader` doc
     * for why that's intentional, not an oversight). This field, [resetWorkout] itself, and
     * [runMonthlyPlanDaySession]'s `allowRestart` parameter are dormant, not dead — they stay
     * correct and test-covered (`WorkoutViewModelTest`) for whenever a redo affordance gets a new
     * home. Do NOT "clean up" `allowRestart` as an always-false parameter — it silently reintroduces
     * the exact same-day-lockout bug `WorkoutSessionDao`'s own doc records as already fixed once. */
    private var resolvedTodayDayId: Long? = null

    /** Redesign Gate 4a-i — set synchronously, in the same call frame as the decision to finish
     * (not inside [finishSession]'s own launched coroutine), the instant [completeCurrentSet]
     * decides the just-logged set is the session's very last. Closes a real duplicate-write window
     * the mock's "last set -> straight to finished, no interstitial" flow opened: [finishSession]
     * awaits two Room round-trips before [WorkoutUiState.phase] actually flips away from
     * [WorkoutPhase.StraightLog], so without this a second tap landing after the debounce window
     * (but before those awaits resolve) would still pass [completeCurrentSet]'s `phase ==
     * StraightLog` guard and log the same set a second time — [finishSession] itself already
     * no-ops on reentry via `sessionFinishJob?.isActive`, but the duplicate [persistSet] call above
     * it wouldn't. Reset in [resetWorkout] alongside every other per-session field. */
    private var finishInFlight = false

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
     * lock rule depends on — see [com.fitviet.app.data.repository.MonthlyPlanRepository]. Phase 2
     * checkpoint — [allowRestart] must be true for [resetWorkout]'s "Làm lại" call (an explicit,
     * intentional redo of a day the user just finished/abandoned on this very screen) and false
     * everywhere else, so a fresh navigation into an already-locked day (e.g. a stale Dashboard
     * card tapped in the brief window before it re-resolves to `Completed`) can't insert a second
     * session row the way "Làm lại" is deliberately allowed to. */
    private fun startMonthlyPlanDaySession(monthlyPlanDayId: Long, allowRestart: Boolean = false) {
        sessionInitJob?.cancel()
        sessionInitJob = viewModelScope.launch {
            databaseReady.await()
            runMonthlyPlanDaySession(monthlyPlanDayId, allowRestart)
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
     * stays defensive rather than leaving a blank screen. Also defensive against a second session
     * row for an already-locked day (see [startMonthlyPlanDaySession]'s [allowRestart] doc) — should
     * likewise be unreachable via [resolveTodaySessionAndStart] now that `observeTodaySession`
     * itself resolves a locked day to `Completed` before this is ever called with its id. */
    private suspend fun runMonthlyPlanDaySession(monthlyPlanDayId: Long, allowRestart: Boolean = false) {
        if (!allowRestart && monthlyPlanRepository.observeIsDayCompleted(monthlyPlanDayId).first()) {
            _uiState.value = WorkoutUiState(isLoading = false, phase = WorkoutPhase.NoSessionToday(NoSessionReason.ALREADY_COMPLETED))
            return
        }
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
                is TodayMonthlyPlanCard.Training -> {
                    resolvedTodayDayId = card.dayId
                    runMonthlyPlanDaySession(card.dayId)
                }
                TodayMonthlyPlanCard.RestDay ->
                    _uiState.value = WorkoutUiState(isLoading = false, phase = WorkoutPhase.NoSessionToday(NoSessionReason.REST_DAY))
                is TodayMonthlyPlanCard.Unavailable ->
                    _uiState.value = WorkoutUiState(isLoading = false, phase = WorkoutPhase.NoSessionToday(NoSessionReason.UNAVAILABLE))
                is TodayMonthlyPlanCard.Completed -> {
                    // Captured so "Làm lại" (reached from the session-finished screen right after
                    // this same day was resolved via the Training branch above, on an earlier
                    // emission) can still redo it — see [resolvedTodayDayId]'s own doc.
                    resolvedTodayDayId = card.dayId
                    _uiState.value = WorkoutUiState(isLoading = false, phase = WorkoutPhase.NoSessionToday(NoSessionReason.ALREADY_COMPLETED))
                }
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
     * loading-state frame, exactly as it did before Gate 24.
     *
     * Gate 4a-ii — dormant, not dead: no screen calls this anymore (its own header chip was
     * removed to match the mock), but `WorkoutViewModelTest` still exercises it directly and the
     * logic stays correct for whenever a redo affordance is given a new home. See
     * [resolvedTodayDayId]'s own doc for why its supporting fields must not be "cleaned up."
     *
     * Phase 2 checkpoint fix — the no-arg branch used to always call [resolveTodaySessionAndStart],
     * which re-resolves "today" from scratch via `observeTodaySession`. Once that resolution
     * correctly reports `Completed` for a day the user just finished (this same gate's `Completed`
     * fix), that re-resolution refused the very redo this button exists to perform. Now it restarts
     * [resolvedTodayDayId] directly with `allowRestart = true` when known — the same day this
     * ViewModel already resolved earlier in its lifetime — and only falls back to a fresh
     * `observeTodaySession` resolution if that's somehow still null (shouldn't happen: "Làm lại" is
     * only reachable after a session already ran, which is exactly what sets it). */
    fun resetWorkout() = debounced {
        restJob?.cancel()
        elapsedJob?.cancel()
        sessionInitJob?.cancel()
        // Review finding (Gate 4a-i) — without this, an in-flight finishSession() from the session
        // being reset can still resolve afterward and slam the brand-new session straight to
        // SessionFinished (it targets `sessionId` captured before the reset, but writes phase
        // unconditionally). Worse with finishInFlight now latched: if the new session's own final
        // set landed while the old job was still alive, finishSession() would no-op on the old
        // job's `isActive` check and never clear the latch, silently swallowing every retry tap.
        sessionFinishJob?.cancel()
        finishInFlight = false
        if (monthlyPlanDayId != null) {
            _uiState.value = WorkoutUiState()
            startMonthlyPlanDaySession(monthlyPlanDayId, allowRestart = true)
        } else {
            _uiState.value = WorkoutUiState()
            val dayId = resolvedTodayDayId
            if (dayId != null) {
                startMonthlyPlanDaySession(dayId, allowRestart = true)
            } else {
                resolveTodaySessionAndStart()
            }
        }
    }

    // ---- Straight block ----

    /**
     * Redesign Gate 4a-i — mirrors the mock's own `completeSet`: the last set of the *session's*
     * last block goes straight to [finishSession] (no interstitial); the last set of any other
     * block advances to rest, then hands off to the next block via [pendingNextBlockIndex] once
     * that rest ends; otherwise it's the old same-block, next-set rest. The old interstitial
     * `StraightBlockDone` phase this replaced was removed entirely in Gate 4a-ii.
     */
    fun completeCurrentSet() = debounced {
        val state = _uiState.value
        if (state.phase != WorkoutPhase.StraightLog || finishInFlight) return@debounced
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
            val nextIndex = state.currentBlockIndex + 1
            val nextBlock = state.blocks.getOrNull(nextIndex)
            if (nextBlock == null) {
                finishInFlight = true
                _uiState.update { it.copy(loggedSetsThisExercise = loggedSets) }
                finishSession()
            } else {
                _uiState.update {
                    it.copy(
                        loggedSetsThisExercise = loggedSets,
                        phase = WorkoutPhase.StraightRest,
                        restSecondsRemaining = DEFAULT_REST_SECONDS,
                        pendingNextBlockIndex = nextIndex,
                    )
                }
                startRestTimer()
            }
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
        val state = _uiState.value
        val pendingIndex = state.pendingNextBlockIndex
        if (pendingIndex != null) {
            _uiState.value = resetForBlock(state, pendingIndex, state.blocks.getOrNull(pendingIndex))
        } else {
            _uiState.update { it.copy(phase = WorkoutPhase.StraightLog, restSecondsRemaining = 0) }
        }
    }

    fun addRest() {
        _uiState.update { it.copy(restSecondsRemaining = it.restSecondsRemaining + 15) }
    }

    fun adjustEditableWeight(deltaKg: Double) {
        _uiState.update { it.copy(editableWeightKg = (it.editableWeightKg + deltaKg).coerceAtLeast(0.0)) }
    }

    fun adjustEditableReps(delta: Int) {
        // Redesign Gate 4a-i — floors at 1, not 0, matching the mock's own `Math.max(1, …)`: a
        // 0-rep set isn't a meaningful log entry to complete, unlike weight, which legitimately
        // floors at 0 for bodyweight movements.
        _uiState.update { it.copy(editableReps = (it.editableReps + delta).coerceAtLeast(1)) }
    }

    private fun startRestTimer() {
        restJob?.cancel()
        restJob = viewModelScope.launch {
            while (_uiState.value.restSecondsRemaining > 0) {
                delay(1_000)
                _uiState.update { it.copy(restSecondsRemaining = (it.restSecondsRemaining - 1).coerceAtLeast(0)) }
            }
            if (_uiState.value.phase == WorkoutPhase.StraightRest) {
                val state = _uiState.value
                val pendingIndex = state.pendingNextBlockIndex
                if (pendingIndex != null) {
                    _uiState.value = resetForBlock(state, pendingIndex, state.blocks.getOrNull(pendingIndex))
                } else {
                    _uiState.update { it.copy(phase = WorkoutPhase.StraightLog) }
                }
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

    // Redesign Gate 4a-ii — only the superset path calls this now (SupersetBlockDoneContent's own
    // CTA); the straight path's completeCurrentSet() handles its own block-to-block hand-off
    // directly (see that function's own doc), so it no longer needs this guard's other disjunct.
    fun advanceToNextBlock() = debounced {
        val state = _uiState.value
        if (state.phase != WorkoutPhase.SupersetBlockDone) return@debounced
        val nextIndex = state.currentBlockIndex + 1
        val nextBlock = state.blocks.getOrNull(nextIndex)
        if (nextBlock == null) {
            finishSession()
        } else {
            _uiState.value = resetForBlock(state, nextIndex, nextBlock)
        }
    }

    // Redesign Gate 4a-i — every branch clears `pendingNextBlockIndex`: this is the one place a
    // pending inter-exercise hand-off is actually consumed (see `completeCurrentSet`/`skipRest`/
    // `startRestTimer`'s own callers), so nothing downstream should ever see a stale value from a
    // hand-off that already happened.
    private fun resetForBlock(state: WorkoutUiState, index: Int, block: WorkoutBlockPlan?): WorkoutUiState = when (block) {
        is WorkoutBlockPlan.Straight -> state.copy(
            currentBlockIndex = index,
            phase = WorkoutPhase.StraightLog,
            currentSetIndex = 0,
            restSecondsRemaining = 0,
            editableWeightKg = block.plan.plannedSets.first().weightKg,
            editableReps = block.plan.plannedSets.first().reps,
            loggedSetsThisExercise = emptyList(),
            pendingNextBlockIndex = null,
        )
        is WorkoutBlockPlan.Superset -> state.copy(
            currentBlockIndex = index,
            phase = WorkoutPhase.SupersetWork,
            supersetRound = 1,
            supersetSub = 0,
            editableWeightKg = block.plan.plannedA.weightKg,
            editableReps = block.plan.plannedA.reps,
            // Review finding (Gate 4a-i) — the Straight branch above already resets these; this
            // branch didn't, so a straight block handing off into a superset (now reachable via
            // this same function, see completeCurrentSet's pending-index path) would carry the
            // JUST-FINISHED straight block's leftover currentSetIndex/restSecondsRemaining/logged
            // sets into the new superset block. Harmless today only because no current Composable
            // reads them on the superset phases — Gate 4a-ii's rewrite is exactly the kind of change
            // that would start reading loggedSetsThisExercise there and silently show stale data.
            currentSetIndex = 0,
            restSecondsRemaining = 0,
            loggedSetsThisExercise = emptyList(),
            pendingNextBlockIndex = null,
        )
        null -> state.copy(phase = WorkoutPhase.SessionFinished, pendingNextBlockIndex = null)
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
            val planProgress = readSessionPlanProgress()
            _uiState.update {
                it.copy(
                    phase = WorkoutPhase.SessionFinished,
                    sessionStreakDays = streakDays,
                    sessionNumberInPlan = planProgress?.completedSessions,
                    sessionTotalInPlan = planProgress?.totalSessions,
                )
            }
        }
    }

    /** Redesign Gate 4b — one-shot read (not observed live, matching [sessionStreakDays]'s own
     * pattern), run after [MonthlyPlanRepository.onMonthlyPlanSessionCompleted] so this session's
     * own completion is already reflected in the summary's `completedSessions`. Reads the same
     * source the Kế hoạch tab's own progress card does
     * ([com.fitviet.app.domain.MonthlyPlanProgress.summarize]), so the finished screen's "Buổi N/M"
     * line never disagrees with that tab. Null when there's no resolvable active plan.
     *
     * Review finding (Gate 4b) — this sits on [finishSession]'s critical path, ahead of the
     * `phase = SessionFinished` flip, for purely display-only data; a throw here would strand the
     * user on the last set behind [finishInFlight]'s latch (which only [resetWorkout] clears, and
     * that affordance is currently unreachable from any screen — see [resolvedTodayDayId]'s own
     * doc). All four reads are Room-backed flows/a settings flow that always emit at least once, so
     * this isn't a live bug today, but a future data source added here should NOT go ahead of the
     * phase flip without reconsidering this ordering. */
    private suspend fun readSessionPlanProgress(): MonthlyPlanProgressSummary? {
        val planId = monthlyPlanRepository.observeActivePlanId().first() ?: return null
        val weeks = monthlyPlanRepository.observeWeeksForPlan(planId).first()
        val days = monthlyPlanRepository.observeDaysForPlan(planId).first()
        val completedDayIds = monthlyPlanRepository.observeCompletedDayIds(planId).first()
        return MonthlyPlanProgress.summarize(weeks, days, completedDayIds, LocalDate.now())
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
