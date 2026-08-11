package com.fitviet.app.ui.workout

import com.fitviet.app.data.local.dao.CommunityPostDao
import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.entity.CommunityPostEntity
import com.fitviet.app.data.local.entity.CommunityPostType
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.entity.MonthlyPlanDayEntity
import com.fitviet.app.data.local.entity.MonthlyPlanExerciseEntity
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.data.local.entity.SettingsEntity
import com.fitviet.app.data.local.seed.SeedExerciseNames
import com.fitviet.app.data.repository.CommunityRepository
import com.fitviet.app.data.repository.ExerciseRepository
import com.fitviet.app.data.repository.ImportProgramResult
import com.fitviet.app.data.repository.MonthlyPlanRepository
import com.fitviet.app.data.repository.MonthlyPlanUserChoices
import com.fitviet.app.data.repository.ProgramRepository
import com.fitviet.app.data.repository.RegenerateResult
import com.fitviet.app.data.repository.WorkoutRepository
import com.fitviet.app.domain.ExerciseDifficulty
import com.fitviet.app.domain.ExerciseHistoryEntry
import com.fitviet.app.domain.MonthlyPlanDayStatus
import com.fitviet.app.domain.MovementType
import com.fitviet.app.domain.MuscleGroup
import com.fitviet.app.domain.ProgramScheduleDay
import com.fitviet.app.domain.ProgramScheduleExercise
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    }

    /** [schedules] maps programId -> its schedule; empty/missing means "no schedule for this
     * program", which [ProgramDayWorkoutPlanner.resolveToday] treats as nothing to preview/start.
     * [programs] maps programId -> its full entity, for [getById] — previously always null
     * regardless of id, which left Gate 40's `programTitle` threading with no way to be tested at
     * all (independent review flagged this as a real coverage gap). */
    private class FakeProgramRepository(
        private val schedules: Map<Long, List<ProgramScheduleDay>> = emptyMap(),
        private val programs: Map<Long, ProgramEntity> = emptyMap(),
    ) : ProgramRepository {
        override fun observeAll(): Flow<List<ProgramEntity>> = flowOf(emptyList())
        override suspend fun getById(id: Long): ProgramEntity? = programs[id]
        override fun observeSchedule(programId: Long): Flow<List<ProgramScheduleDay>> = flowOf(schedules[programId].orEmpty())
        override fun observeActiveProgramId(): Flow<Long?> = flowOf(null)
        override suspend fun setActiveProgram(programId: Long) {}
        override fun observeHasSeenSupersetHint(): Flow<Boolean> = flowOf(false)
        override suspend fun dismissSupersetHint() {}
        override suspend fun exportProgram(programId: Long): String? = null
        override suspend fun importProgram(json: String): ImportProgramResult = ImportProgramResult.Failed
    }

    /** [days]/[exercisesByDay] fake a single monthly plan's persisted state, keyed by day id —
     * enough for [MonthlyPlanDayWorkoutPlanner.resolveDay] to run against without a real Room
     * database. Every other [MonthlyPlanRepository] method (regenerate/adaptive-scheduling/PR
     * hook) is untested surface here; this file only covers the live-session entry point Gate 63+
     * Phase 4 wires up, not the repository's own logic (already covered independently). */
    private class FakeMonthlyPlanRepository(
        private val days: Map<Long, MonthlyPlanDayEntity> = emptyMap(),
        private val exercisesByDay: Map<Long, List<MonthlyPlanExerciseEntity>> = emptyMap(),
    ) : MonthlyPlanRepository {
        val completedDayIds = mutableListOf<Long>()

        override fun observeActivePlanId(): Flow<Long?> = flowOf(null)
        override fun observeDaysForPlan(planId: Long): Flow<List<MonthlyPlanDayEntity>> = flowOf(emptyList())
        override fun observeExercisesForDay(dayId: Long): Flow<List<MonthlyPlanExerciseEntity>> = flowOf(exercisesByDay[dayId].orEmpty())
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
        supersetGroup = null,
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

    private fun testProgram(id: Long, titleVi: String) = ProgramEntity(
        id = id,
        titleVi = titleVi,
        imageAsset = "test.png",
        durationWeeks = 4,
        sessionsPerWeek = 3,
        level = "Mới bắt đầu",
        equipment = "Phòng gym",
        tags = emptyList(),
    )

    private val testExercises = listOf(
        testExercise(1, SeedExerciseNames.BENCH_PRESS),
        testExercise(2, SeedExerciseNames.SHOULDER_PRESS),
        testExercise(3, SeedExerciseNames.CABLE_FLY),
        testExercise(4, SeedExerciseNames.LATERAL_RAISE),
    )

    /**
     * [startSession]: most tests want the harness to land directly on the curated demo session
     * (block 0, `StraightLog`) exactly as it did before Gate 10 added the duration picker in front
     * of it — pass `false` only for tests that specifically cover the picker step itself.
     */
    private inner class Harness(startSession: Boolean = true) {
        val clock = FakeClock()
        val workoutRepository = FakeWorkoutRepository()
        val communityPostDao = FakeCommunityPostDao()
        val viewModel = WorkoutViewModel(
            exerciseRepository = FakeExerciseRepository(testExercises),
            workoutRepository = workoutRepository,
            programRepository = FakeProgramRepository(),
            communityRepository = CommunityRepository(communityPostDao, FakeSettingsDao()),
            monthlyPlanRepository = FakeMonthlyPlanRepository(),
            databaseReady = CompletableDeferred(Unit),
            elapsedRealtimeMillis = clock::now,
        )

        init {
            testDispatcher.scheduler.runCurrent()
            if (startSession) {
                // null == "Không giới hạn" == WorkoutPlanSeed's curated demo, unchanged from before Gate 10.
                viewModel.selectDuration(null)
                testDispatcher.scheduler.runCurrent()
                // selectDuration() is itself debounced, so it stamps lastActionAtMillis at the
                // clock's starting value (10_000). Without advancing past that here, every test's
                // very first action right after `Harness()` would land at the same instant and get
                // silently dropped by the 350ms debounce window — the exact bug FakeClock's 10_000
                // starting offset exists to avoid, reintroduced one level up by this setup call.
                clock.advance()
            }
        }

        /** Advances the debounce clock so the next action isn't silently dropped by the previous one. */
        fun tick() {
            clock.advance()
            testDispatcher.scheduler.runCurrent()
        }
    }

    // ---- Gate 10: duration picker ----

    @Test
    fun `initial state shows the duration picker before any session starts`() = runTest(testDispatcher) {
        val h = Harness(startSession = false)
        val state = h.viewModel.uiState.value

        assertEquals(WorkoutPhase.SelectingDuration, state.phase)
        assertTrue(state.blocks.isEmpty())
    }

    @Test
    fun `selecting a time budget builds a session via the time-budget planner, not the curated demo`() = runTest(testDispatcher) {
        val h = Harness(startSession = false)

        // 5-minute (300s) budget: Bench Press alone is ~222s (3 sets x 8 reps x 3s + 2x60s rest +
        // 30s transition, per the shared testExercise() fixture); Shoulder Press would push past
        // 300s, so only Bench Press fits.
        h.viewModel.selectDuration(5)
        testDispatcher.scheduler.runCurrent()

        val state = h.viewModel.uiState.value
        assertEquals(WorkoutPhase.StraightLog, state.phase)
        assertEquals(1, state.blocks.size)
        val block = (state.blocks.first() as WorkoutBlockPlan.Straight).plan
        assertEquals(SeedExerciseNames.BENCH_PRESS, block.exercise.nameVi)
        assertEquals(3, block.plannedSets.size)
        assertEquals(0.0, state.editableWeightKg, 0.0) // generated blocks start at 0kg, unlike the curated demo
        assertEquals(1L, h.workoutRepository.nextSessionId - 1) // a Room session row was started
    }

    @Test
    fun `selecting Khong gioi han builds the original curated demo, unchanged`() = runTest(testDispatcher) {
        val h = Harness(startSession = false)

        h.viewModel.selectDuration(null)
        testDispatcher.scheduler.runCurrent()

        val state = h.viewModel.uiState.value
        assertEquals(WorkoutPhase.StraightLog, state.phase)
        assertEquals(3, state.blocks.size) // bench, shoulder, superset(cable fly + lateral raise)
        assertEquals(40.0, state.editableWeightKg, 0.0) // bench press's curated first-set weight, not 0
    }

    @Test
    fun `initial state starts on block 0 with the first planned set`() = runTest(testDispatcher) {
        val vm = Harness().viewModel
        val state = vm.uiState.value

        assertEquals(WorkoutPhase.StraightLog, state.phase)
        assertEquals(0, state.currentBlockIndex)
        assertEquals(0, state.currentSetIndex)
        assertEquals(40.0, state.editableWeightKg, 0.0)
        assertEquals(8, state.editableReps)
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
    }

    @Test
    fun `rest timer auto-returns to log when it reaches zero`() = runTest(testDispatcher) {
        val h = Harness()
        h.viewModel.completeCurrentSet()
        testDispatcher.scheduler.runCurrent()

        testDispatcher.scheduler.advanceTimeBy(60_000)
        testDispatcher.scheduler.runCurrent()

        assertEquals(WorkoutPhase.StraightLog, h.viewModel.uiState.value.phase)
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
    }

    @Test
    fun `completing the final set of a block transitions to StraightBlockDone`() = runTest(testDispatcher) {
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
        assertEquals(WorkoutPhase.StraightBlockDone, state.phase)
        assertEquals(4, state.loggedSetsThisExercise.size)
        assertEquals(4, h.workoutRepository.loggedSets.size)
    }

    @Test
    fun `advancing past the last block finishes the session`() = runTest(testDispatcher) {
        val h = Harness()
        advanceThroughEntireSession(h)

        val state = h.viewModel.uiState.value
        assertEquals(WorkoutPhase.SessionFinished, state.phase)
        assertEquals(1L, h.workoutRepository.completedSessionId)
        assertTrue(h.workoutRepository.completedVolumeKg!! > 0.0)
    }

    // ---- Gate 40: workout-share ----

    @Test
    fun `finishing a session computes and stores the streak from the workout repository`() = runTest(testDispatcher) {
        val h = Harness()
        h.workoutRepository.streakDaysToReturn = 5
        advanceThroughEntireSession(h)

        assertEquals(5, h.viewModel.uiState.value.sessionStreakDays)
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
        assertTrue(h.viewModel.uiState.value.sessionShared)
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
    }

    @Test
    fun `a duplicate action within the debounce window is ignored`() = runTest(testDispatcher) {
        val h = Harness()
        h.viewModel.completeCurrentSet()
        h.viewModel.completeCurrentSet() // fired immediately after, clock hasn't advanced
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, h.workoutRepository.loggedSets.size)
        assertEquals(1, h.viewModel.uiState.value.currentSetIndex)
    }

    @Test
    fun `reset returns to the duration picker, and picking again starts a fresh session at block 0`() = runTest(testDispatcher) {
        val h = Harness()
        h.viewModel.completeCurrentSet()
        testDispatcher.scheduler.runCurrent()
        h.tick()

        h.viewModel.resetWorkout()
        testDispatcher.scheduler.runCurrent()

        assertEquals(WorkoutPhase.SelectingDuration, h.viewModel.uiState.value.phase)
        assertEquals(1L, h.workoutRepository.nextSessionId - 1) // no new session row yet, just back at the picker

        h.tick()
        h.viewModel.selectDuration(null)
        testDispatcher.scheduler.runCurrent()

        val state = h.viewModel.uiState.value
        assertEquals(WorkoutPhase.StraightLog, state.phase)
        assertEquals(0, state.currentBlockIndex)
        assertEquals(0, state.currentSetIndex)
        assertEquals(2L, h.workoutRepository.nextSessionId - 1) // a second session was started
    }

    // ---- Gate 24: program-day-driven session ----

    private fun todaySchedule(exerciseId: Long, nameVi: String, sets: Int, repsMin: Int, repsMax: Int) = listOf(
        ProgramScheduleDay(
            dayOfWeek = LocalDate.now().dayOfWeek,
            titleVi = "Ngày kiểm thử",
            isRestDay = false,
            exercises = listOf(ProgramScheduleExercise(exerciseId, nameVi, sets, repsMin, repsMax)),
        ),
    )

    @Test
    fun `a program-day session skips the picker and builds blocks from today's real schedule`() = runTest(testDispatcher) {
        val programRepository = FakeProgramRepository(
            schedules = mapOf(42L to todaySchedule(1L, SeedExerciseNames.BENCH_PRESS, sets = 3, repsMin = 8, repsMax = 10)),
            programs = mapOf(42L to testProgram(42L, "Chương trình kiểm thử")),
        )
        val workoutRepository = FakeWorkoutRepository()
        val vm = WorkoutViewModel(
            exerciseRepository = FakeExerciseRepository(testExercises),
            workoutRepository = workoutRepository,
            programRepository = programRepository,
            communityRepository = CommunityRepository(FakeCommunityPostDao(), FakeSettingsDao()),
            monthlyPlanRepository = FakeMonthlyPlanRepository(),
            databaseReady = CompletableDeferred(Unit),
            programId = 42L,
        )
        testDispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        assertEquals(WorkoutPhase.StraightLog, state.phase)
        assertEquals("Ngày kiểm thử", state.dayLabel)
        assertEquals("Chương trình kiểm thử", state.programTitle)
        assertEquals(1, state.blocks.size)
        val block = (state.blocks.first() as WorkoutBlockPlan.Straight).plan
        assertEquals(SeedExerciseNames.BENCH_PRESS, block.exercise.nameVi)
        assertEquals(3, block.plannedSets.size)
        assertEquals(9, block.plannedSets.first().reps) // midpoint of 8-10
        assertEquals(20.0, block.plannedSets.first().weightKg, 0.0) // no logged history -> default
        assertEquals(1L, workoutRepository.nextSessionId - 1) // a real session row was started
    }

    @Test
    fun `sharing a program-day session's post includes the program title`() = runTest(testDispatcher) {
        val clock = FakeClock()
        val programRepository = FakeProgramRepository(
            schedules = mapOf(42L to todaySchedule(1L, SeedExerciseNames.BENCH_PRESS, sets = 3, repsMin = 8, repsMax = 10)),
            programs = mapOf(42L to testProgram(42L, "Chương trình kiểm thử")),
        )
        val communityPostDao = FakeCommunityPostDao()
        val vm = WorkoutViewModel(
            exerciseRepository = FakeExerciseRepository(testExercises),
            workoutRepository = FakeWorkoutRepository(),
            programRepository = programRepository,
            communityRepository = CommunityRepository(communityPostDao, FakeSettingsDao()),
            monthlyPlanRepository = FakeMonthlyPlanRepository(),
            databaseReady = CompletableDeferred(Unit),
            programId = 42L,
            elapsedRealtimeMillis = clock::now,
        )
        testDispatcher.scheduler.runCurrent()
        fun tick() {
            clock.advance()
            testDispatcher.scheduler.runCurrent()
        }
        // One straight block with 3 planned sets — complete all 3, then finish the session.
        repeat(2) {
            vm.completeCurrentSet()
            tick()
            vm.skipRest()
            tick()
        }
        vm.completeCurrentSet()
        tick()
        vm.advanceToNextBlock()
        tick()
        assertEquals(WorkoutPhase.SessionFinished, vm.uiState.value.phase)

        vm.shareToCommunity()
        testDispatcher.scheduler.runCurrent()

        assertEquals("Chương trình kiểm thử", communityPostDao.inserted.single().programTitle)
    }

    @Test
    fun `a program with no schedule for today falls back to the duration picker`() = runTest(testDispatcher) {
        val vm = WorkoutViewModel(
            exerciseRepository = FakeExerciseRepository(testExercises),
            workoutRepository = FakeWorkoutRepository(),
            programRepository = FakeProgramRepository(), // no schedule for programId 99 at all
            communityRepository = CommunityRepository(FakeCommunityPostDao(), FakeSettingsDao()),
            monthlyPlanRepository = FakeMonthlyPlanRepository(),
            databaseReady = CompletableDeferred(Unit),
            programId = 99L,
        )
        testDispatcher.scheduler.runCurrent()

        assertEquals(WorkoutPhase.SelectingDuration, vm.uiState.value.phase)
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
            programRepository = FakeProgramRepository(),
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
    }

    @Test
    fun `finishing a monthly-plan-day session runs the PR-bump hook for that day`() = runTest(testDispatcher) {
        val monthlyPlanRepository = FakeMonthlyPlanRepository(
            days = mapOf(7L to testMonthlyPlanDay(id = 7L, sessionType = "Push")),
            exercisesByDay = mapOf(7L to listOf(testMonthlyPlanExercise(dayId = 7L, exerciseId = 1L, targetSets = 1, targetRepsMin = 8, targetRepsMax = 8))),
        )
        val vm = WorkoutViewModel(
            exerciseRepository = FakeExerciseRepository(testExercises),
            workoutRepository = FakeWorkoutRepository(),
            programRepository = FakeProgramRepository(),
            communityRepository = CommunityRepository(FakeCommunityPostDao(), FakeSettingsDao()),
            monthlyPlanRepository = monthlyPlanRepository,
            databaseReady = CompletableDeferred(Unit),
            monthlyPlanDayId = 7L,
        )
        testDispatcher.scheduler.runCurrent()

        vm.completeCurrentSet() // this day's only set -> StraightBlockDone
        testDispatcher.scheduler.runCurrent()
        vm.advanceToNextBlock() // no next block -> finishSession()
        testDispatcher.scheduler.runCurrent()

        assertEquals(WorkoutPhase.SessionFinished, vm.uiState.value.phase)
        assertEquals(listOf(7L), monthlyPlanRepository.completedDayIds)
    }

    @Test
    fun `monthly-plan completion waits for the final set write before running the PR-bump hook`() = runTest(testDispatcher) {
        val monthlyPlanRepository = FakeMonthlyPlanRepository(
            days = mapOf(7L to testMonthlyPlanDay(id = 7L, sessionType = "Push")),
            exercisesByDay = mapOf(7L to listOf(testMonthlyPlanExercise(dayId = 7L, exerciseId = 1L, targetSets = 1))),
        )
        val workoutRepository = FakeWorkoutRepository().apply { logSetGate = CompletableDeferred() }
        val vm = WorkoutViewModel(
            exerciseRepository = FakeExerciseRepository(testExercises),
            workoutRepository = workoutRepository,
            programRepository = FakeProgramRepository(),
            communityRepository = CommunityRepository(FakeCommunityPostDao(), FakeSettingsDao()),
            monthlyPlanRepository = monthlyPlanRepository,
            databaseReady = CompletableDeferred(Unit),
            monthlyPlanDayId = 7L,
        )
        testDispatcher.scheduler.runCurrent()

        vm.completeCurrentSet()
        testDispatcher.scheduler.runCurrent()
        vm.advanceToNextBlock()
        testDispatcher.scheduler.runCurrent()

        assertEquals(emptyList<Long>(), monthlyPlanRepository.completedDayIds)
        assertEquals(null, workoutRepository.completedSessionId)

        workoutRepository.logSetGate?.complete(Unit)
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, workoutRepository.loggedSets.size)
        assertEquals(listOf(7L), monthlyPlanRepository.completedDayIds)
    }

    @Test
    fun `a monthly-plan rest day falls back to the duration picker`() = runTest(testDispatcher) {
        val monthlyPlanRepository = FakeMonthlyPlanRepository(
            days = mapOf(7L to testMonthlyPlanDay(id = 7L, sessionType = null, isRestDay = true)),
        )
        val vm = WorkoutViewModel(
            exerciseRepository = FakeExerciseRepository(testExercises),
            workoutRepository = FakeWorkoutRepository(),
            programRepository = FakeProgramRepository(),
            communityRepository = CommunityRepository(FakeCommunityPostDao(), FakeSettingsDao()),
            monthlyPlanRepository = monthlyPlanRepository,
            databaseReady = CompletableDeferred(Unit),
            monthlyPlanDayId = 7L,
        )
        testDispatcher.scheduler.runCurrent()

        assertEquals(WorkoutPhase.SelectingDuration, vm.uiState.value.phase)
    }

    /** Completes bench press (4 sets) and shoulder press (3 sets), landing on the superset block. */
    private fun advanceToSupersetBlock(h: Harness) {
        completeStraightBlock(h, setCount = 4)
        h.viewModel.advanceToNextBlock()
        testDispatcher.scheduler.runCurrent()
        h.tick()

        completeStraightBlock(h, setCount = 3)
        h.viewModel.advanceToNextBlock()
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
