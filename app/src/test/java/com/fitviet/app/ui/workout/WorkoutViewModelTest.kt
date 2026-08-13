package com.fitviet.app.ui.workout

import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.dao.CommunityPostDao
import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.entity.CommunityPostEntity
import com.fitviet.app.data.local.entity.CommunityPostType
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.entity.MonthlyPlanDayEntity
import com.fitviet.app.data.local.entity.MonthlyPlanEntity
import com.fitviet.app.data.local.entity.MonthlyPlanExerciseEntity
import com.fitviet.app.data.local.entity.MonthlyPlanWeekEntity
import com.fitviet.app.data.local.entity.SettingsEntity
import com.fitviet.app.data.local.seed.SeedExerciseNames
import com.fitviet.app.data.repository.CommunityRepository
import com.fitviet.app.data.repository.ExerciseRepository
import com.fitviet.app.data.repository.MonthlyPlanRepository
import com.fitviet.app.data.repository.MonthlyPlanUserChoices
import com.fitviet.app.data.repository.RegenerateResult
import com.fitviet.app.data.repository.WorkoutRepository
import com.fitviet.app.domain.ExerciseDifficulty
import com.fitviet.app.domain.ExerciseHistoryEntry
import com.fitviet.app.domain.MonthlyPlanDayStatus
import com.fitviet.app.domain.MovementType
import com.fitviet.app.domain.MuscleGroup
import com.fitviet.app.domain.TodayMonthlyPlanCard
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers the workout flow state machine built in Gate 4 — the "unit test cho workout state
 * machine" requirement from the brief. Uses fake repositories (no Room) and an injectable clock
 * (see [WorkoutViewModel]'s `elapsedRealtimeMillis` param — `android.os.SystemClock` is stubbed to
 * a constant 0 in plain JVM unit tests, which would make the debounce block every action forever).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Starts well past the debounce window so the very first debounced call in a test isn't
    // rejected by comparing against WorkoutViewModel's own zero-initialized lastActionAtMillis.
    private class FakeClock(private var millis: Long = 10_000L) {
        fun now(): Long = millis
        fun advance(by: Long = 1_000L) {
            millis += by
        }
    }

    private class FakeExerciseRepository(private val exercises: List<ExerciseEntity>) : ExerciseRepository {
        override suspend fun getAll(): List<ExerciseEntity> = exercises
        override suspend fun getById(id: Long): ExerciseEntity? = exercises.firstOrNull { it.id == id }
    }

    private class FakeWorkoutRepository : WorkoutRepository {
        var nextSessionId = 1L
        val loggedSets = mutableListOf<LoggedSet>()
        var completedSessionId: Long? = null
        var completedVolumeKg: Double? = null
        var completedDurationSeconds: Int? = null
        var startedMonthlyPlanDayId: Long? = null
        var logSetGate: CompletableDeferred<Unit>? = null

        override suspend fun startSession(dayLabel: String, startedAtMillis: Long, monthlyPlanDayId: Long?): Long {
            startedMonthlyPlanDayId = monthlyPlanDayId
            return nextSessionId++
        }

        override suspend fun logSet(sessionId: Long, set: LoggedSet) {
            logSetGate?.await()
            loggedSets += set
        }

        override suspend fun completeSession(sessionId: Long, completedAtMillis: Long, totalVolumeKg: Double, durationSeconds: Int) {
            completedSessionId = sessionId
            completedVolumeKg = totalVolumeKg
            completedDurationSeconds = durationSeconds
        }

        // No test needs a real "personal best" signal — a fixed null keeps every program-day test
        // deterministic on ProgramDayWorkoutPlanner.DEFAULT_RECOMMENDED_WEIGHT_KG.
        override suspend fun getRecommendedWeight(exerciseId: Long): Double? = null

        // No test asserts on a specific streak number today — a fixed 0 keeps every
        // finishSession()-reaching test deterministic without needing real completed-session dates.
        var streakDaysToReturn = 0
        override suspend fun getCurrentStreakDays(today: LocalDate): Int = streakDaysToReturn

        // Nothing in this test file exercises Exercise Detail's "Tiến bộ" tab — a fixed empty flow
        // keeps this fake minimal rather than tracking per-exercise history nobody here reads.
        override fun observeHistoryForExercise(exerciseId: Long): Flow<List<ExerciseHistoryEntry>> = flowOf(emptyList())
    }

    /** Fakes the two Dao dependencies a real [CommunityRepository] is constructed with below — its
     * own logic (author-identity lookup, post construction) runs unmodified in tests, only the Room
     * layer underneath it is faked. */
    private class FakeSettingsDao(private var settings: SettingsEntity = SettingsEntity()) : SettingsDao {
        override fun observe(): Flow<SettingsEntity?> = flowOf(settings)
        override suspend fun get(): SettingsEntity? = settings
        override suspend fun upsert(settings: SettingsEntity) {
            this.settings = settings
        }
    }

    private class FakeCommunityPostDao : CommunityPostDao {
        val inserted = mutableListOf<CommunityPostEntity>()
        override fun observeAll(): Flow<List<CommunityPostEntity>> = flowOf(emptyList())
        override suspend fun count(): Int = 0
        override suspend fun insertAll(posts: List<CommunityPostEntity>) {}
        override suspend fun insert(post: CommunityPostEntity): Long {
            // A real suspension point (unlike a body with no `yield`/real I/O, which a
            // TestDispatcher just runs to completion atomically) — needed so the "sharing twice"
            // test below can actually exercise two shareToCommunity() coroutines interleaving,
            // the exact scenario independent review found WorkoutViewModel wasn't guarding against.
            yield()
            inserted += post
            return inserted.size.toLong()
        }
        override suspend fun setLiked(id: Long, liked: Boolean) {}
        override suspend fun deleteAll() { inserted.clear() }
    }

    /** [days]/[exercisesByDay] fake a single monthly plan's persisted state, keyed by day id —
     * enough for [MonthlyPlanDayWorkoutPlanner.resolveDay] to run against without a real Room
     * database. Every other [MonthlyPlanRepository] method (regenerate/adaptive-scheduling/PR
     * hook) is untested surface here; this file only covers the live-session entry point Gate 63+
     * Phase 4 wires up, not the repository's own logic (already covered independently). */
    private class FakeMonthlyPlanRepository(
        private val days: Map<Long, MonthlyPlanDayEntity> = emptyMap(),
        private val exercisesByDay: Map<Long, List<MonthlyPlanExerciseEntity>> = emptyMap(),
        // Redesign Gate 1c — backs the no-arg entry point's resolution
        // (WorkoutViewModel.resolveTodaySessionAndStart), the same source Dashboard's Today card
        // reads. Defaults to NoPlan (the safest "nothing set up" default) so tests that don't care
        // about the no-arg path (the explicit monthlyPlanDayId tests below) don't need to
        // configure it.
        private val todayCard: TodayMonthlyPlanCard = TodayMonthlyPlanCard.NoPlan,
        // Redesign Gate 4b — backs WorkoutViewModel.readSessionPlanProgress()'s "Buổi N/M" read.
        // Null activePlanId (the default) makes that read resolve to null, matching every test
        // below that doesn't configure it — nothing else in this file calls observeActivePlanId/
        // observeWeeksForPlan/observeDaysForPlan, so this is a purely additive default.
        private val activePlanId: Long? = null,
        private val weeksForActivePlan: List<MonthlyPlanWeekEntity> = emptyList(),
        private val daysForActivePlan: List<MonthlyPlanDayEntity> = emptyList(),
    ) : MonthlyPlanRepository {
        val completedDayIds = mutableListOf<Long>()

        override fun observeTodaySession(today: LocalDate): Flow<TodayMonthlyPlanCard> = flowOf(todayCard)
        override fun observeActivePlanId(): Flow<Long?> = flowOf(activePlanId)
        override fun observePlan(planId: Long): Flow<MonthlyPlanEntity?> = flowOf(null)
        override fun observeWeeksForPlan(planId: Long): Flow<List<MonthlyPlanWeekEntity>> = flowOf(weeksForActivePlan)
        override fun observeDaysForPlan(planId: Long): Flow<List<MonthlyPlanDayEntity>> = flowOf(daysForActivePlan)
        override fun observeExercisesForDay(dayId: Long): Flow<List<MonthlyPlanExerciseEntity>> = flowOf(exercisesByDay[dayId].orEmpty())
        override fun observeLockedDayIds(planId: Long): Flow<Set<Long>> = flowOf(emptySet())
        // Reflects completedDayIds live (not a fixed emptySet()) so a test can complete a session
        // via onMonthlyPlanSessionCompleted and then see it counted by a later
        // readSessionPlanProgress() read within the same test. WorkoutViewModel isn't this
        // method's only production consumer (ProgramsViewModel reads it too), but no OTHER test in
        // this file exercises it — this fake is file-local, so the change is still safely additive.
        override fun observeCompletedDayIds(planId: Long): Flow<Set<Long>> = flowOf(completedDayIds.toSet())
        override fun observeDay(dayId: Long): Flow<MonthlyPlanDayEntity?> = flowOf(days[dayId])
        override fun observeIsDayLocked(dayId: Long): Flow<Boolean> = flowOf(false)
        override fun observeIsDayCompleted(dayId: Long): Flow<Boolean> = flowOf(false)
        override suspend fun getDay(dayId: Long): MonthlyPlanDayEntity? = days[dayId]
        override suspend fun generate(choices: MonthlyPlanUserChoices, today: LocalDate): Long = 0L
        override suspend fun regenerateDay(dayId: Long, today: LocalDate): RegenerateResult = RegenerateResult.NotFound
        override suspend fun regenerateWeek(weekId: Long, today: LocalDate): RegenerateResult = RegenerateResult.NotFound
        override suspend fun regenerateMonth(planId: Long, today: LocalDate): RegenerateResult = RegenerateResult.NotFound
        override suspend fun swapExercise(monthlyPlanExerciseId: Long, avoidEquipment: String?): RegenerateResult = RegenerateResult.NotFound
        override suspend fun findMissedDays(planId: Long, today: LocalDate): List<MonthlyPlanDayEntity> = emptyList()
        override suspend fun markMissed(dayIds: List<Long>) {}
        override suspend fun pushMissedDayToToday(dayId: Long, today: LocalDate): RegenerateResult = RegenerateResult.NotFound
        override suspend fun skipMissedDay(dayId: Long): RegenerateResult = RegenerateResult.NotFound
        override suspend fun swapTwoDays(dayIdA: Long, dayIdB: Long): RegenerateResult = RegenerateResult.NotFound
        override suspend fun onMonthlyPlanSessionCompleted(monthlyPlanDayId: Long) {
            completedDayIds += monthlyPlanDayId
        }
    }

    private fun testMonthlyPlanDay(
        id: Long,
        sessionType: String?,
        isRestDay: Boolean = false,
    ) = MonthlyPlanDayEntity(
        id = id,
        monthlyPlanWeekId = 1L,
        plannedEpochDay = 0L,
        effectiveEpochDay = 0L,
        dayOfWeek = 1,
        isRestDay = isRestDay,
        sessionType = sessionType,
        muscleGroupCodes = emptyList(),
        status = MonthlyPlanDayStatus.SCHEDULED.name,
    )

    private fun testMonthlyPlanExercise(
        dayId: Long,
        exerciseId: Long,
        orderIndex: Int = 0,
        targetSets: Int = 3,
        targetRepsMin: Int = 8,
        targetRepsMax: Int = 10,
        targetWeightKg: Double? = null,
        supersetGroup: String? = null,
    ) = MonthlyPlanExerciseEntity(
        id = orderIndex.toLong() + dayId * 100,
        monthlyPlanDayId = dayId,
        exerciseId = exerciseId,
        orderIndex = orderIndex,
        tier = "MAIN_COMPOUND",
        selectionReasonCode = "test",
        targetSets = targetSets,
        targetRepsMin = targetRepsMin,
        targetRepsMax = targetRepsMax,
        targetWeightKg = targetWeightKg,
        supersetGroup = supersetGroup,
    )

    private fun testExercise(id: Long, nameVi: String) = ExerciseEntity(
        id = id,
        nameVi = nameVi,
        nameEn = nameVi,
        gifAsset = "test.gif",
        primaryMuscle = "Test",
        secondaryMuscles = emptyList(),
        equipment = "Test",
        instructions = emptyList(),
        suggestedSetsMin = 3,
        suggestedSetsMax = 3,
        suggestedRepsMin = 8,
        suggestedRepsMax = 8,
        suggestedRestSeconds = 60,
        muscleGroupCode = MuscleGroup.CHEST.name,
        movementType = MovementType.COMPOUND.name,
        difficultyCode = ExerciseDifficulty.BEGINNER.name,
    )

    private val testExercises = listOf(
        testExercise(1, SeedExerciseNames.BENCH_PRESS),
        testExercise(2, SeedExerciseNames.SHOULDER_PRESS),
        testExercise(3, SeedExerciseNames.CABLE_FLY),
        testExercise(4, SeedExerciseNames.LATERAL_RAISE),
    )

    // Redesign Gate 1c — [Harness]'s default no-arg session used to reach a fixed curated demo
    // (WorkoutPlanSeed) via the now-deleted duration picker. It now resolves through the same
    // "Hit & Run" monthly-plan Today-session path a real no-arg entry (bottom-nav FAB) uses, via
    // [FakeMonthlyPlanRepository]'s `todayCard`. Same 4-exercise/3-block shape as the old curated
    // demo (straight bench, straight shoulder, cable-fly+lateral-raise superset) so the bulk of the
    // block-by-block assertions below didn't need to change, just how the session gets created.
    private val DEMO_DAY_ID = 1L

    private fun demoExercises() = listOf(
        testMonthlyPlanExercise(dayId = DEMO_DAY_ID, exerciseId = 1L, orderIndex = 0, targetSets = 4, targetRepsMin = 8, targetRepsMax = 8, targetWeightKg = 40.0),
        testMonthlyPlanExercise(dayId = DEMO_DAY_ID, exerciseId = 2L, orderIndex = 1, targetSets = 3, targetRepsMin = 10, targetRepsMax = 10, targetWeightKg = 16.0),
        testMonthlyPlanExercise(dayId = DEMO_DAY_ID, exerciseId = 3L, orderIndex = 2, targetSets = 3, targetRepsMin = 12, targetRepsMax = 12, targetWeightKg = 15.0, supersetGroup = "SS1"),
        testMonthlyPlanExercise(dayId = DEMO_DAY_ID, exerciseId = 4L, orderIndex = 3, targetSets = 3, targetRepsMin = 15, targetRepsMax = 15, targetWeightKg = 8.0, supersetGroup = "SS1"),
    )

    private fun demoMonthlyPlanRepository() = FakeMonthlyPlanRepository(
        days = mapOf(DEMO_DAY_ID to testMonthlyPlanDay(id = DEMO_DAY_ID, sessionType = "Đẩy")),
        exercisesByDay = mapOf(DEMO_DAY_ID to demoExercises()),
        todayCard = TodayMonthlyPlanCard.Training(dayId = DEMO_DAY_ID, sessionType = "Đẩy", exerciseCount = 4, estimatedDurationMinutes = 30),
    )

    private inner class Harness {
        val clock = FakeClock()
        val workoutRepository = FakeWorkoutRepository()
        val communityPostDao = FakeCommunityPostDao()
        val viewModel = WorkoutViewModel(
            exerciseRepository = FakeExerciseRepository(testExercises),
            workoutRepository = workoutRepository,
            communityRepository = CommunityRepository(communityPostDao, FakeSettingsDao()),
            monthlyPlanRepository = demoMonthlyPlanRepository(),
            databaseReady = CompletableDeferred(Unit),
            elapsedRealtimeMillis = clock::now,
        )

        init {
            // Unlike the old duration-picker flow, resolution isn't behind a debounced user action
            // (loadInitialSession() runs straight from ViewModel init), so there's no starting
            // timestamp to burn past here before the first real test action.
            testDispatcher.scheduler.runCurrent()
        }

        /** Advances the debounce clock so the next action isn't silently dropped by the previous one. */
        fun tick() {
            clock.advance()
            testDispatcher.scheduler.runCurrent()
        }

        /** Must run as the last statement of every test that reaches this Harness's constructor
         * (i.e. every test below except the no-arg-resolution outcome tests that never start a
         * session) — see [cancelTickerToAvoidRunTestHang]'s doc for why. */
        fun finish() = viewModel.cancelTickerToAvoidRunTestHang()
    }

    // ---- Redesign Gate 1c: single session entry point (replaces Gate 10's duration picker) ----

    @Test
    fun `no active plan lands on AwaitingPlanGeneration`() = runTest(testDispatcher) {
        val vm = WorkoutViewModel(
            exerciseRepository = FakeExerciseRepository(testExercises),
            workoutRepository = FakeWorkoutRepository(),
            communityRepository = CommunityRepository(FakeCommunityPostDao(), FakeSettingsDao()),
            monthlyPlanRepository = FakeMonthlyPlanRepository(), // todayCard defaults to NoPlan
            databaseReady = CompletableDeferred(Unit),
        )
        testDispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        assertEquals(WorkoutPhase.AwaitingPlanGeneration, state.phase)
        assertTrue(state.blocks.isEmpty())
    }

    @Test
    fun `a rest day lands on NoSessionToday with REST_DAY`() = runTest(testDispatcher) {
        val vm = WorkoutViewModel(
            exerciseRepository = FakeExerciseRepository(testExercises),
            workoutRepository = FakeWorkoutRepository(),
            communityRepository = CommunityRepository(FakeCommunityPostDao(), FakeSettingsDao()),
            monthlyPlanRepository = FakeMonthlyPlanRepository(todayCard = TodayMonthlyPlanCard.RestDay),
            databaseReady = CompletableDeferred(Unit),
        )
        testDispatcher.scheduler.runCurrent()

        assertEquals(WorkoutPhase.NoSessionToday(NoSessionReason.REST_DAY), vm.uiState.value.phase)
    }

    @Test
    fun `a training day that resolves to zero exercises lands on NoSessionToday with UNAVAILABLE`() = runTest(testDispatcher) {
        val vm = WorkoutViewModel(
            exerciseRepository = FakeExerciseRepository(testExercises),
            workoutRepository = FakeWorkoutRepository(),
            communityRepository = CommunityRepository(FakeCommunityPostDao(), FakeSettingsDao()),
            monthlyPlanRepository = FakeMonthlyPlanRepository(
                todayCard = TodayMonthlyPlanCard.Unavailable(dayId = 5L, sessionType = "Đẩy"),
            ),
            databaseReady = CompletableDeferred(Unit),
        )
        testDispatcher.scheduler.runCurrent()

        assertEquals(WorkoutPhase.NoSessionToday(NoSessionReason.UNAVAILABLE), vm.uiState.value.phase)
    }

    @Test
    fun `a finished plan lands on NoSessionToday with PLAN_FINISHED`() = runTest(testDispatcher) {
        val vm = WorkoutViewModel(
            exerciseRepository = FakeExerciseRepository(testExercises),
            workoutRepository = FakeWorkoutRepository(),
            communityRepository = CommunityRepository(FakeCommunityPostDao(), FakeSettingsDao()),
            monthlyPlanRepository = FakeMonthlyPlanRepository(todayCard = TodayMonthlyPlanCard.PlanFinished),
            databaseReady = CompletableDeferred(Unit),
        )
        testDispatcher.scheduler.runCurrent()

        assertEquals(WorkoutPhase.NoSessionToday(NoSessionReason.PLAN_FINISHED), vm.uiState.value.phase)
    }

    @Test
    fun `a training day resolves through observeTodaySession straight into logging`() = runTest(testDispatcher) {
        val h = Harness()
        val state = h.viewModel.uiState.value

        assertEquals(WorkoutPhase.StraightLog, state.phase)
        assertEquals("Đẩy", state.dayLabel)
        assertEquals(3, state.blocks.size) // bench, shoulder, superset(cable fly + lateral raise)
        assertEquals(1L, h.workoutRepository.nextSessionId - 1) // a real session row was started
        assertEquals(DEMO_DAY_ID, h.workoutRepository.startedMonthlyPlanDayId) // carrying the regenerate-lock FK
        h.finish()
    }

    @Test
    fun `initial state starts on block 0 with the first planned set`() = runTest(testDispatcher) {
        val h = Harness()
        val state = h.viewModel.uiState.value

        assertEquals(WorkoutPhase.StraightLog, state.phase)
        assertEquals(0, state.currentBlockIndex)
        assertEquals(0, state.currentSetIndex)
        assertEquals(40.0, state.editableWeightKg, 0.0)
        assertEquals(8, state.editableReps)
        h.finish()
    }

    @Test
    fun `completing a non-final set starts rest with the next set's planned values`() = runTest(testDispatcher) {
        val h = Harness()
        h.viewModel.completeCurrentSet()
        testDispatcher.scheduler.runCurrent()

        val state = h.viewModel.uiState.value
        assertEquals(WorkoutPhase.StraightRest, state.phase)
        assertEquals(1, state.currentSetIndex)
        assertEquals(60, state.restSecondsRemaining)
        assertEquals(40.0, state.editableWeightKg, 0.0) // bench press set 2 is also 40kg×8 in the plan
        assertEquals(1, h.workoutRepository.loggedSets.size)
        h.finish()
    }

    @Test
    fun `rest timer auto-returns to log when it reaches zero`() = runTest(testDispatcher) {
        val h = Harness()
        h.viewModel.completeCurrentSet()
        testDispatcher.scheduler.runCurrent()

        testDispatcher.scheduler.advanceTimeBy(60_000)
        testDispatcher.scheduler.runCurrent()

        assertEquals(WorkoutPhase.StraightLog, h.viewModel.uiState.value.phase)
        h.finish()
    }

    @Test
    fun `rest is still counting down before 60 seconds pass`() = runTest(testDispatcher) {
        val h = Harness()
        h.viewModel.completeCurrentSet()
        testDispatcher.scheduler.runCurrent()

        testDispatcher.scheduler.advanceTimeBy(30_000)
        testDispatcher.scheduler.runCurrent()

        val state = h.viewModel.uiState.value
        assertEquals(WorkoutPhase.StraightRest, state.phase)
        assertEquals(30, state.restSecondsRemaining)
        h.finish()
    }

    @Test
    fun `skip rest returns to log immediately`() = runTest(testDispatcher) {
        val h = Harness()
        h.viewModel.completeCurrentSet()
        testDispatcher.scheduler.runCurrent()
        h.tick()

        h.viewModel.skipRest()
        testDispatcher.scheduler.runCurrent()

        val state = h.viewModel.uiState.value
        assertEquals(WorkoutPhase.StraightLog, state.phase)
        assertEquals(0, state.restSecondsRemaining)
        h.finish()
    }

    @Test
    fun `add rest extends the countdown by 15 seconds without changing phase`() = runTest(testDispatcher) {
        val h = Harness()
        h.viewModel.completeCurrentSet()
        testDispatcher.scheduler.runCurrent()

        h.viewModel.addRest()
        testDispatcher.scheduler.runCurrent()

        assertEquals(75, h.viewModel.uiState.value.restSecondsRemaining)
        assertEquals(WorkoutPhase.StraightRest, h.viewModel.uiState.value.phase)
        h.finish()
    }

    @Test
    fun `completing the final set of a block starts rest pending a hand-off to the next block`() = runTest(testDispatcher) {
        val h = Harness()
        // Bench press has 4 planned sets: complete all, skipping rest between each.
        repeat(3) {
            h.viewModel.completeCurrentSet()
            testDispatcher.scheduler.runCurrent()
            h.tick()
            h.viewModel.skipRest()
            testDispatcher.scheduler.runCurrent()
            h.tick()
        }
        h.viewModel.completeCurrentSet()
        testDispatcher.scheduler.runCurrent()

        val state = h.viewModel.uiState.value
        assertEquals(WorkoutPhase.StraightRest, state.phase)
        assertEquals(1, state.pendingNextBlockIndex) // shoulder press, the next block
        assertEquals(0, state.currentBlockIndex) // not advanced yet — only rest has started
        assertEquals(4, state.loggedSetsThisExercise.size)
        assertEquals(4, h.workoutRepository.loggedSets.size)
        h.finish()
    }

    @Test
    fun `skipping the inter-exercise rest hands off to the next block's log screen`() = runTest(testDispatcher) {
        val h = Harness()
        repeat(3) {
            h.viewModel.completeCurrentSet()
            testDispatcher.scheduler.runCurrent()
            h.tick()
            h.viewModel.skipRest()
            testDispatcher.scheduler.runCurrent()
            h.tick()
        }
        h.viewModel.completeCurrentSet()
        testDispatcher.scheduler.runCurrent()
        h.tick()

        h.viewModel.skipRest()
        testDispatcher.scheduler.runCurrent()

        val state = h.viewModel.uiState.value
        assertEquals(WorkoutPhase.StraightLog, state.phase)
        assertEquals(1, state.currentBlockIndex)
        assertEquals(0, state.currentSetIndex)
        assertEquals(null, state.pendingNextBlockIndex)
        assertTrue(state.loggedSetsThisExercise.isEmpty()) // reset for the new block
        h.finish()
    }

    @Test
    fun `the inter-exercise rest timer auto-hands-off to the next block when it reaches zero`() = runTest(testDispatcher) {
        val h = Harness()
        repeat(3) {
            h.viewModel.completeCurrentSet()
            testDispatcher.scheduler.runCurrent()
            h.tick()
            h.viewModel.skipRest()
            testDispatcher.scheduler.runCurrent()
            h.tick()
        }
        h.viewModel.completeCurrentSet()
        testDispatcher.scheduler.runCurrent()

        testDispatcher.scheduler.advanceTimeBy(60_000)
        testDispatcher.scheduler.runCurrent()

        val state = h.viewModel.uiState.value
        assertEquals(WorkoutPhase.StraightLog, state.phase)
        assertEquals(1, state.currentBlockIndex)
        assertEquals(null, state.pendingNextBlockIndex)
        h.finish()
    }

    @Test
    fun `advancing past the last block finishes the session`() = runTest(testDispatcher) {
        val h = Harness()
        advanceThroughEntireSession(h)

        val state = h.viewModel.uiState.value
        assertEquals(WorkoutPhase.SessionFinished, state.phase)
        assertEquals(1L, h.workoutRepository.completedSessionId)
        assertTrue(h.workoutRepository.completedVolumeKg!! > 0.0)
        h.finish()
    }

    // ---- Gate 40: workout-share ----

    @Test
    fun `finishing a session computes and stores the streak from the workout repository`() = runTest(testDispatcher) {
        val h = Harness()
        h.workoutRepository.streakDaysToReturn = 5
        advanceThroughEntireSession(h)

        assertEquals(5, h.viewModel.uiState.value.sessionStreakDays)
        h.finish()
    }

    @Test
    fun `sharing a finished session creates a real community post from the session summary`() = runTest(testDispatcher) {
        val h = Harness()
        h.workoutRepository.streakDaysToReturn = 3
        advanceThroughEntireSession(h)
        val state = h.viewModel.uiState.value

        h.viewModel.shareToCommunity()
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, h.communityPostDao.inserted.size)
        val post = h.communityPostDao.inserted.single()
        assertEquals(CommunityPostType.WORKOUT_SHARE, post.postType)
        assertEquals(state.dayLabel, post.dayLabel)
        assertEquals(state.sessionElapsedSeconds, post.durationSeconds)
        assertEquals(state.sessionTotalVolumeKg, post.totalVolumeKg)
        assertEquals(3, post.streakDays)
        // Gate 6b — this call site omits userText/category (Gate 6c's composer is what will
        // supply them), so shareWorkout()'s own fallback copy is what lands in bodyText, and
        // category stays null (no dedicated tab until the composer tags one).
        assertEquals("Vừa hoàn thành buổi tập!", post.bodyText)
        assertEquals(null, post.category)
        assertTrue(h.viewModel.uiState.value.sessionShared)
        h.finish()
    }

    @Test
    fun `sharing twice back-to-back before either coroutine resumes only creates one post`() = runTest(testDispatcher) {
        val h = Harness()
        advanceThroughEntireSession(h)

        // Deliberately no scheduler.runCurrent() between these two calls — both shareToCommunity()
        // invocations happen on this (single) calling thread before either launched coroutine has
        // had a chance to run at all. FakeCommunityPostDao.insert's yield() then lets the two
        // resulting coroutines genuinely interleave once the scheduler does run, reproducing the
        // exact race independent review found: without a synchronous check-and-set in
        // shareToCommunity() (fixed after that review), this would insert two posts.
        h.viewModel.shareToCommunity()
        h.viewModel.shareToCommunity()
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, h.communityPostDao.inserted.size)
        h.finish()
    }

    @Test
    fun `superset completing A moves to B with B's planned values, no rest between them`() = runTest(testDispatcher) {
        val h = Harness()
        advanceToSupersetBlock(h)

        val before = h.viewModel.uiState.value
        assertEquals(WorkoutPhase.SupersetWork, before.phase)
        assertEquals(0, before.supersetSub)
        assertEquals(15.0, before.editableWeightKg, 0.0) // cable fly plan

        h.viewModel.supersetNext()
        testDispatcher.scheduler.runCurrent()

        val after = h.viewModel.uiState.value
        assertEquals(WorkoutPhase.SupersetWork, after.phase) // no rest between A and B
        assertEquals(1, after.supersetSub)
        assertEquals(8.0, after.editableWeightKg, 0.0) // lateral raise plan
        h.finish()
    }

    @Test
    fun `superset completing B before the last round starts superset rest`() = runTest(testDispatcher) {
        val h = Harness()
        advanceToSupersetBlock(h)

        h.viewModel.supersetNext() // A -> B
        testDispatcher.scheduler.runCurrent()
        h.tick()
        h.viewModel.supersetNext() // B -> rest (round 1 of 3)
        testDispatcher.scheduler.runCurrent()

        val state = h.viewModel.uiState.value
        assertEquals(WorkoutPhase.SupersetRest, state.phase)
        assertEquals(60, state.supersetRestSecondsRemaining)
        h.finish()
    }

    @Test
    fun `superset rest auto-advances to the next round`() = runTest(testDispatcher) {
        val h = Harness()
        advanceToSupersetBlock(h)

        h.viewModel.supersetNext()
        testDispatcher.scheduler.runCurrent()
        h.tick()
        h.viewModel.supersetNext()
        testDispatcher.scheduler.runCurrent()

        testDispatcher.scheduler.advanceTimeBy(60_000)
        testDispatcher.scheduler.runCurrent()

        val state = h.viewModel.uiState.value
        assertEquals(WorkoutPhase.SupersetWork, state.phase)
        assertEquals(2, state.supersetRound)
        assertEquals(0, state.supersetSub)
        assertEquals(15.0, state.editableWeightKg, 0.0) // back to A's planned values for the new round
        h.finish()
    }

    @Test
    fun `superset finishing the last round's B goes to SupersetBlockDone`() = runTest(testDispatcher) {
        val h = Harness()
        advanceToSupersetBlock(h)
        repeat(2) { completeSupersetRound(h) } // rounds 1 and 2

        // Round 3 (last): A then B, no rest expected after.
        h.viewModel.supersetNext()
        testDispatcher.scheduler.runCurrent()
        h.tick()
        h.viewModel.supersetNext()
        testDispatcher.scheduler.runCurrent()

        val state = h.viewModel.uiState.value
        assertEquals(WorkoutPhase.SupersetBlockDone, state.phase)
        assertEquals(4, state.supersetRound) // totalRounds + 1
        h.finish()
    }

    @Test
    fun `a duplicate action within the debounce window is ignored`() = runTest(testDispatcher) {
        val h = Harness()
        h.viewModel.completeCurrentSet()
        h.viewModel.completeCurrentSet() // fired immediately after, clock hasn't advanced
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, h.workoutRepository.loggedSets.size)
        assertEquals(1, h.viewModel.uiState.value.currentSetIndex)
        h.finish()
    }

    // Review finding (Gate 4a-i, MEDIUM-3) — the debounce guard alone does NOT protect the
    // session-final tap: finishSession() awaits Room round-trips before `phase` flips away from
    // StraightLog, so a second tap landing after the debounce window has cleared (but before those
    // awaits resolve) would still pass the `phase == StraightLog` check. This test proves
    // `finishInFlight` is what actually closes that window, not the phase guard — the middle
    // assertion (phase is still StraightLog) is what makes this a real regression test rather than
    // a tautology: remove `finishInFlight` and this still passes up to that line, but
    // `loggedSets.size` becomes 2.
    @Test
    fun `a second session-final tap after the debounce window clears cannot log the set twice`() = runTest(testDispatcher) {
        val monthlyPlanRepository = FakeMonthlyPlanRepository(
            days = mapOf(7L to testMonthlyPlanDay(id = 7L, sessionType = "Push")),
            exercisesByDay = mapOf(7L to listOf(testMonthlyPlanExercise(dayId = 7L, exerciseId = 1L, targetSets = 1, targetRepsMin = 8, targetRepsMax = 8))),
        )
        // Holds the final set's Room insert open, so finishSession() stays suspended on its join
        // and `phase` is still StraightLog when the second tap lands — the exact window
        // finishInFlight owns.
        val workoutRepository = FakeWorkoutRepository().apply { logSetGate = CompletableDeferred() }
        val clock = FakeClock()
        val vm = WorkoutViewModel(
            exerciseRepository = FakeExerciseRepository(testExercises),
            workoutRepository = workoutRepository,
            communityRepository = CommunityRepository(FakeCommunityPostDao(), FakeSettingsDao()),
            monthlyPlanRepository = monthlyPlanRepository,
            databaseReady = CompletableDeferred(Unit),
            monthlyPlanDayId = 7L,
            elapsedRealtimeMillis = clock::now,
        )
        testDispatcher.scheduler.runCurrent()

        vm.completeCurrentSet()
        testDispatcher.scheduler.runCurrent()
        // Load-bearing: proves the phase guard alone would NOT reject the second tap below.
        assertEquals(WorkoutPhase.StraightLog, vm.uiState.value.phase)

        clock.advance(5_000) // debounce window long since cleared
        vm.completeCurrentSet()
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, vm.uiState.value.sessionTotalSets)

        workoutRepository.logSetGate?.complete(Unit)
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, workoutRepository.loggedSets.size)
        assertEquals(WorkoutPhase.SessionFinished, vm.uiState.value.phase)
        vm.cancelTickerToAvoidRunTestHang()
    }

    @Test
    fun `reps floor at 1, matching the mock's Math_max(1, reps)`() = runTest(testDispatcher) {
        val h = Harness()
        repeat(20) { h.viewModel.adjustEditableReps(-1) }

        assertEquals(1, h.viewModel.uiState.value.editableReps)

        h.viewModel.adjustEditableReps(-5)
        assertEquals(1, h.viewModel.uiState.value.editableReps)
        h.finish()
    }

    @Test
    fun `reset re-resolves today's session and starts a fresh one at block 0`() = runTest(testDispatcher) {
        val h = Harness()
        h.viewModel.completeCurrentSet()
        testDispatcher.scheduler.runCurrent()
        h.tick()

        h.viewModel.resetWorkout()
        testDispatcher.scheduler.runCurrent()

        val state = h.viewModel.uiState.value
        assertEquals(WorkoutPhase.StraightLog, state.phase)
        assertEquals(0, state.currentBlockIndex)
        assertEquals(0, state.currentSetIndex)
        assertEquals(2L, h.workoutRepository.nextSessionId - 1) // a fresh session row was started
        h.finish()
    }

    // ---- Gate 63+: monthly-plan-day-driven session ----

    @Test
    fun `a monthly-plan-day session skips the picker and builds blocks from that day's exercises`() = runTest(testDispatcher) {
        val monthlyPlanRepository = FakeMonthlyPlanRepository(
            days = mapOf(7L to testMonthlyPlanDay(id = 7L, sessionType = "Push")),
            exercisesByDay = mapOf(7L to listOf(testMonthlyPlanExercise(dayId = 7L, exerciseId = 1L, targetSets = 3, targetRepsMin = 8, targetRepsMax = 10))),
        )
        val workoutRepository = FakeWorkoutRepository()
        val vm = WorkoutViewModel(
            exerciseRepository = FakeExerciseRepository(testExercises),
            workoutRepository = workoutRepository,
            communityRepository = CommunityRepository(FakeCommunityPostDao(), FakeSettingsDao()),
            monthlyPlanRepository = monthlyPlanRepository,
            databaseReady = CompletableDeferred(Unit),
            monthlyPlanDayId = 7L,
        )
        testDispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        assertEquals(WorkoutPhase.StraightLog, state.phase)
        assertEquals("Push", state.dayLabel)
        assertEquals(1, state.blocks.size)
        val block = (state.blocks.first() as WorkoutBlockPlan.Straight).plan
        assertEquals(SeedExerciseNames.BENCH_PRESS, block.exercise.nameVi)
        assertEquals(3, block.plannedSets.size)
        assertEquals(9, block.plannedSets.first().reps) // midpoint of 8-10
        assertEquals(1L, workoutRepository.nextSessionId - 1) // a real session row was started
        assertEquals(7L, workoutRepository.startedMonthlyPlanDayId) // carrying the regenerate-lock FK
        vm.cancelTickerToAvoidRunTestHang()
    }

    @Test
    fun `finishing a monthly-plan-day session runs the PR-bump hook for that day`() = runTest(testDispatcher) {
        val monthlyPlanRepository = FakeMonthlyPlanRepository(
            days = mapOf(7L to testMonthlyPlanDay(id = 7L, sessionType = "Push")),
            exercisesByDay = mapOf(7L to listOf(testMonthlyPlanExercise(dayId = 7L, exerciseId = 1L, targetSets = 1, targetRepsMin = 8, targetRepsMax = 8))),
        )
        // Phase 1 checkpoint review — this test used to construct WorkoutViewModel without a fake
        // clock, comparing its debounced() completeCurrentSet() call against the real
        // SystemClock.elapsedRealtime, which is stubbed to a constant 0 in a plain JVM unit test —
        // the action was silently dropped, the ViewModel never left StraightLog, and the assertion
        // below failed before ever reaching this test's cancelTickerToAvoidRunTestHang() cleanup,
        // leaving the still-running elapsed ticker to hang runTest's own implicit teardown. A real
        // FakeClock (same pattern every other test in this file already uses) fixes both problems.
        val clock = FakeClock()
        val vm = WorkoutViewModel(
            exerciseRepository = FakeExerciseRepository(testExercises),
            workoutRepository = FakeWorkoutRepository(),
            communityRepository = CommunityRepository(FakeCommunityPostDao(), FakeSettingsDao()),
            monthlyPlanRepository = monthlyPlanRepository,
            databaseReady = CompletableDeferred(Unit),
            monthlyPlanDayId = 7L,
            elapsedRealtimeMillis = clock::now,
        )
        testDispatcher.scheduler.runCurrent()

        // Redesign Gate 4a-i — this day's only set is also the session's last, so
        // completeCurrentSet() now calls finishSession() directly (no more separate
        // advanceToNextBlock() step through a StraightBlockDone interstitial).
        vm.completeCurrentSet()
        testDispatcher.scheduler.runCurrent()

        assertEquals(WorkoutPhase.SessionFinished, vm.uiState.value.phase)
        assertEquals(listOf(7L), monthlyPlanRepository.completedDayIds)
        vm.cancelTickerToAvoidRunTestHang()
    }

    // Redesign Gate 4b — the finished screen's "Buổi N/M" line, via
    // WorkoutViewModel.readSessionPlanProgress(). Three training days (7L, 9L, 10L) and one rest
    // day (8L, correctly excluded from both completedSessions/totalSessions per
    // MonthlyPlanProgress.summarize's own "trainingDays only" doc) in a 1-week plan; day 9L is
    // pre-seeded as already completed, day 10L is left NOT completed — this test's own completion
    // of day 7L is deliberately the second of three, not the last, so sessionNumberInPlan (2) and
    // sessionTotalInPlan (3) can't be satisfied by the same literal (a same-value fixture wouldn't
    // catch the two fields being swapped — caught by review before this landed).
    @Test
    fun `finishing a monthly-plan-day session resolves this plan's session progress for the finished screen`() = runTest(testDispatcher) {
        val planDays = listOf(
            testMonthlyPlanDay(id = 7L, sessionType = "Push"),
            testMonthlyPlanDay(id = 8L, sessionType = null, isRestDay = true),
            testMonthlyPlanDay(id = 9L, sessionType = "Pull"),
            testMonthlyPlanDay(id = 10L, sessionType = "Legs"),
        )
        val monthlyPlanRepository = FakeMonthlyPlanRepository(
            days = mapOf(7L to planDays[0]),
            exercisesByDay = mapOf(7L to listOf(testMonthlyPlanExercise(dayId = 7L, exerciseId = 1L, targetSets = 1, targetRepsMin = 8, targetRepsMax = 8))),
            activePlanId = 1L,
            weeksForActivePlan = listOf(MonthlyPlanWeekEntity(id = 1L, monthlyPlanId = 1L, weekIndex = 0, phase = "BASE")),
            daysForActivePlan = planDays,
        )
        monthlyPlanRepository.completedDayIds += 9L // this plan's other training day, already done
        // day 10L ("Legs") deliberately left incomplete — see this test's own comment above.
        val clock = FakeClock()
        val vm = WorkoutViewModel(
            exerciseRepository = FakeExerciseRepository(testExercises),
            workoutRepository = FakeWorkoutRepository(),
            communityRepository = CommunityRepository(FakeCommunityPostDao(), FakeSettingsDao()),
            monthlyPlanRepository = monthlyPlanRepository,
            databaseReady = CompletableDeferred(Unit),
            monthlyPlanDayId = 7L,
            elapsedRealtimeMillis = clock::now,
        )
        testDispatcher.scheduler.runCurrent()

        vm.completeCurrentSet()
        testDispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        assertEquals(WorkoutPhase.SessionFinished, state.phase)
        assertEquals(2, state.sessionNumberInPlan) // day 9L (pre-seeded) + day 7L (this session)
        assertEquals(3, state.sessionTotalInPlan) // 3 training days; the rest day doesn't count
        vm.cancelTickerToAvoidRunTestHang()
    }

    @Test
    fun `finishing a session with no active plan omits the session-in-plan progress`() = runTest(testDispatcher) {
        val monthlyPlanRepository = FakeMonthlyPlanRepository(
            days = mapOf(7L to testMonthlyPlanDay(id = 7L, sessionType = "Push")),
            exercisesByDay = mapOf(7L to listOf(testMonthlyPlanExercise(dayId = 7L, exerciseId = 1L, targetSets = 1, targetRepsMin = 8, targetRepsMax = 8))),
            // activePlanId defaults to null — no plan row for readSessionPlanProgress() to read.
        )
        val clock = FakeClock()
        val vm = WorkoutViewModel(
            exerciseRepository = FakeExerciseRepository(testExercises),
            workoutRepository = FakeWorkoutRepository(),
            communityRepository = CommunityRepository(FakeCommunityPostDao(), FakeSettingsDao()),
            monthlyPlanRepository = monthlyPlanRepository,
            databaseReady = CompletableDeferred(Unit),
            monthlyPlanDayId = 7L,
            elapsedRealtimeMillis = clock::now,
        )
        testDispatcher.scheduler.runCurrent()

        vm.completeCurrentSet()
        testDispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        assertEquals(WorkoutPhase.SessionFinished, state.phase)
        assertEquals(null, state.sessionNumberInPlan)
        assertEquals(null, state.sessionTotalInPlan)
        vm.cancelTickerToAvoidRunTestHang()
    }

    @Test
    fun `monthly-plan completion waits for the final set write before running the PR-bump hook`() = runTest(testDispatcher) {
        val monthlyPlanRepository = FakeMonthlyPlanRepository(
            days = mapOf(7L to testMonthlyPlanDay(id = 7L, sessionType = "Push")),
            exercisesByDay = mapOf(7L to listOf(testMonthlyPlanExercise(dayId = 7L, exerciseId = 1L, targetSets = 1))),
        )
        val workoutRepository = FakeWorkoutRepository().apply { logSetGate = CompletableDeferred() }
        // See the sibling PR-bump test above for why a real clock is required here.
        val clock = FakeClock()
        val vm = WorkoutViewModel(
            exerciseRepository = FakeExerciseRepository(testExercises),
            workoutRepository = workoutRepository,
            communityRepository = CommunityRepository(FakeCommunityPostDao(), FakeSettingsDao()),
            monthlyPlanRepository = monthlyPlanRepository,
            databaseReady = CompletableDeferred(Unit),
            monthlyPlanDayId = 7L,
            elapsedRealtimeMillis = clock::now,
        )
        testDispatcher.scheduler.runCurrent()

        // Redesign Gate 4a-i — this day's only (and therefore session-final) set now triggers
        // finishSession() directly from completeCurrentSet() itself; no separate advanceToNextBlock()
        // call needed.
        vm.completeCurrentSet()
        testDispatcher.scheduler.runCurrent()

        assertEquals(emptyList<Long>(), monthlyPlanRepository.completedDayIds)
        assertEquals(null, workoutRepository.completedSessionId)

        workoutRepository.logSetGate?.complete(Unit)
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, workoutRepository.loggedSets.size)
        assertEquals(listOf(7L), monthlyPlanRepository.completedDayIds)
        vm.cancelTickerToAvoidRunTestHang()
    }

    @Test
    fun `a monthly-plan rest day falls back to NoSessionToday`() = runTest(testDispatcher) {
        val monthlyPlanRepository = FakeMonthlyPlanRepository(
            days = mapOf(7L to testMonthlyPlanDay(id = 7L, sessionType = null, isRestDay = true)),
        )
        val vm = WorkoutViewModel(
            exerciseRepository = FakeExerciseRepository(testExercises),
            workoutRepository = FakeWorkoutRepository(),
            communityRepository = CommunityRepository(FakeCommunityPostDao(), FakeSettingsDao()),
            monthlyPlanRepository = monthlyPlanRepository,
            databaseReady = CompletableDeferred(Unit),
            monthlyPlanDayId = 7L,
        )
        testDispatcher.scheduler.runCurrent()

        assertEquals(WorkoutPhase.NoSessionToday(NoSessionReason.UNAVAILABLE), vm.uiState.value.phase)
    }

    /** Completes bench press (4 sets) and shoulder press (3 sets), landing on the superset block.
     * Redesign Gate 4a-i — completing a block's last set now lands on [WorkoutPhase.StraightRest]
     * with a pending hand-off to the next block (no more `WorkoutPhase.StraightBlockDone`
     * interstitial), so [WorkoutViewModel.skipRest] is what consumes that hand-off now, not
     * [WorkoutViewModel.advanceToNextBlock] (which the straight path no longer reaches at all). */
    private fun advanceToSupersetBlock(h: Harness) {
        completeStraightBlock(h, setCount = 4)
        h.viewModel.skipRest()
        testDispatcher.scheduler.runCurrent()
        h.tick()

        completeStraightBlock(h, setCount = 3)
        h.viewModel.skipRest()
        testDispatcher.scheduler.runCurrent()
        h.tick()
    }

    private fun completeStraightBlock(h: Harness, setCount: Int) {
        repeat(setCount - 1) {
            h.viewModel.completeCurrentSet()
            testDispatcher.scheduler.runCurrent()
            h.tick()
            h.viewModel.skipRest()
            testDispatcher.scheduler.runCurrent()
            h.tick()
        }
        h.viewModel.completeCurrentSet()
        testDispatcher.scheduler.runCurrent()
        h.tick()
    }

    private fun completeSupersetRound(h: Harness) {
        h.viewModel.supersetNext() // A -> B
        testDispatcher.scheduler.runCurrent()
        h.tick()
        h.viewModel.supersetNext() // B -> rest
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(60_000) // rest -> next round
        testDispatcher.scheduler.runCurrent()
        h.tick()
    }

    private fun advanceThroughEntireSession(h: Harness) {
        advanceToSupersetBlock(h)
        repeat(3) { completeSupersetRound(h) }
        h.viewModel.advanceToNextBlock()
        testDispatcher.scheduler.runCurrent()
    }
}

/**
 * Phase 1 checkpoint review (HIGH 1) — pre-existing, not introduced by the Gate 1c rewrite, but it
 * blocked verifying that rewrite: once a session actually starts, [WorkoutViewModel]'s
 * `startElapsedTicker()` launches an unconditional `while (true) { delay(1_000); ... }` in
 * `viewModelScope`. Because [Dispatchers.setMain] points `Dispatchers.Main` (and so
 * `viewModelScope`) at the very same [StandardTestDispatcher] each test's `runTest(testDispatcher)`
 * block runs on, that ticker's suspended `delay` sits on the identical [TestCoroutineScheduler]
 * `runTest` drains to completion when the test body returns — and since the ticker always
 * re-enqueues itself, that drain never reaches "idle" and spins forever (measured at 900s before
 * being force-killed). `onCleared()` is `protected` (AndroidX's own contract), so it can't be
 * called from here directly; `viewModelScope` itself is a public extension property, so cancelling
 * it directly is the standard, non-hacky way to stop this without touching production code or
 * resorting to reflection. Every test that starts a real session (directly or via [Harness.finish])
 * must call this as its last statement so `runTest`'s implicit final drain has nothing left to spin
 * on — see each test body below.
 */
private fun WorkoutViewModel.cancelTickerToAvoidRunTestHang() {
    viewModelScope.cancel()
}
