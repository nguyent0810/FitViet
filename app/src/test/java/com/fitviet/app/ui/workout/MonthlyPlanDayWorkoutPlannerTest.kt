package com.fitviet.app.ui.workout

import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.entity.MonthlyPlanDayEntity
import com.fitviet.app.data.local.entity.MonthlyPlanEntity
import com.fitviet.app.data.local.entity.MonthlyPlanExerciseEntity
import com.fitviet.app.data.local.entity.MonthlyPlanWeekEntity
import com.fitviet.app.data.repository.ExerciseRepository
import com.fitviet.app.data.repository.MonthlyPlanRepository
import com.fitviet.app.data.repository.MonthlyPlanUserChoices
import com.fitviet.app.data.repository.RegenerateResult
import com.fitviet.app.data.repository.WorkoutRepository
import com.fitviet.app.domain.ExerciseDifficulty
import com.fitviet.app.domain.ExerciseHistoryEntry
import com.fitviet.app.domain.MovementType
import com.fitviet.app.domain.MuscleGroup
import com.fitviet.app.domain.TodayMonthlyPlanCard
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers [MonthlyPlanDayWorkoutPlanner.resolveDay] — the "Hit & Run" (Gate 63+) counterpart to
 * [ProgramDayWorkoutPlannerTest], which already covers [ProgramDayWorkoutPlanner.buildBlocks]/
 * [ProgramDayWorkoutPlanner.resolveGroupings]/[ProgramDayWorkoutPlanner.estimateDurationMinutes]
 * directly against [ProgramDayWorkoutItem] lists — those are reused unmodified here (a monthly-plan
 * exercise row resolves into the exact same item shape), so this file only needs to cover the new
 * I/O-mapping step: turning a [MonthlyPlanDayEntity] + its [MonthlyPlanExerciseEntity] rows into a
 * [ResolvedProgramDay].
 */
class MonthlyPlanDayWorkoutPlannerTest {

    private class FakeExerciseRepository(private val exercises: List<ExerciseEntity>) : ExerciseRepository {
        override suspend fun getAll(): List<ExerciseEntity> = exercises
        override suspend fun getById(id: Long): ExerciseEntity? = exercises.firstOrNull { it.id == id }
    }

    private class FakeWorkoutRepository(private val recommendedWeightKg: Double? = null) : WorkoutRepository {
        override suspend fun startSession(dayLabel: String, startedAtMillis: Long, monthlyPlanDayId: Long?): Long = 1L
        override suspend fun logSet(sessionId: Long, set: LoggedSet) {}
        override suspend fun completeSession(sessionId: Long, completedAtMillis: Long, totalVolumeKg: Double, durationSeconds: Int) {}
        override suspend fun getRecommendedWeight(exerciseId: Long): Double? = recommendedWeightKg
        override suspend fun getCurrentStreakDays(today: LocalDate): Int = 0
        override fun observeHistoryForExercise(exerciseId: Long): Flow<List<ExerciseHistoryEntry>> = flowOf(emptyList())
    }

    /** [days]/[exercisesByDay] fake a single monthly plan's persisted state, keyed by day id. */
    private class FakeMonthlyPlanRepository(
        private val days: Map<Long, MonthlyPlanDayEntity> = emptyMap(),
        private val exercisesByDay: Map<Long, List<MonthlyPlanExerciseEntity>> = emptyMap(),
    ) : MonthlyPlanRepository {
        override fun observeActivePlanId(): Flow<Long?> = flowOf(null)
        override fun observePlan(planId: Long): Flow<MonthlyPlanEntity?> = flowOf(null)
        override fun observeWeeksForPlan(planId: Long): Flow<List<MonthlyPlanWeekEntity>> = flowOf(emptyList())
        override fun observeDaysForPlan(planId: Long): Flow<List<MonthlyPlanDayEntity>> = flowOf(emptyList())
        override fun observeExercisesForDay(dayId: Long): Flow<List<MonthlyPlanExerciseEntity>> = flowOf(exercisesByDay[dayId].orEmpty())
        override fun observeTodaySession(today: LocalDate): Flow<TodayMonthlyPlanCard> = flowOf(TodayMonthlyPlanCard.NoPlan)
        override fun observeLockedDayIds(planId: Long): Flow<Set<Long>> = flowOf(emptySet())
        override fun observeCompletedDayIds(planId: Long): Flow<Set<Long>> = flowOf(emptySet())
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
        override suspend fun onMonthlyPlanSessionCompleted(monthlyPlanDayId: Long) {}
    }

    private fun day(id: Long, sessionType: String?, isRestDay: Boolean = false) = MonthlyPlanDayEntity(
        id = id,
        monthlyPlanWeekId = 1L,
        plannedEpochDay = 0L,
        effectiveEpochDay = 0L,
        dayOfWeek = 1,
        isRestDay = isRestDay,
        sessionType = sessionType,
        muscleGroupCodes = emptyList(),
        status = "SCHEDULED",
    )

    private fun planExercise(
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

    private fun exercise(id: Long, nameVi: String) = ExerciseEntity(
        id = id,
        nameVi = nameVi,
        nameEn = nameVi,
        gifAsset = "x.gif",
        primaryMuscle = "x",
        secondaryMuscles = emptyList(),
        equipment = "x",
        instructions = emptyList(),
        suggestedSetsMin = 3,
        suggestedSetsMax = 3,
        suggestedRepsMin = 8,
        suggestedRepsMax = 12,
        suggestedRestSeconds = 60,
        muscleGroupCode = MuscleGroup.CHEST.name,
        movementType = MovementType.COMPOUND.name,
        difficultyCode = ExerciseDifficulty.BEGINNER.name,
    )

    @Test
    fun `resolves a day's exercises in order into ResolvedProgramDay, titled by sessionType`() = runBlocking {
        val monthlyPlanRepository = FakeMonthlyPlanRepository(
            days = mapOf(7L to day(7L, sessionType = "Push")),
            exercisesByDay = mapOf(
                7L to listOf(
                    planExercise(dayId = 7L, exerciseId = 2L, orderIndex = 1),
                    planExercise(dayId = 7L, exerciseId = 1L, orderIndex = 0),
                ),
            ),
        )
        val exerciseRepository = FakeExerciseRepository(listOf(exercise(1L, "Bench Press"), exercise(2L, "Shoulder Press")))

        val resolved = MonthlyPlanDayWorkoutPlanner.resolveDay(7L, monthlyPlanRepository, exerciseRepository, FakeWorkoutRepository())

        assertEquals("Push", resolved?.titleVi)
        // observeExercisesForDay already orders by orderIndex in the real DAO — the fake here
        // preserves whatever order it's given, matching that contract's shape for this test.
        assertEquals(listOf("Shoulder Press", "Bench Press"), resolved?.items?.map { it.exercise.nameVi })
    }

    @Test
    fun `a rest day resolves to null`() = runBlocking {
        val monthlyPlanRepository = FakeMonthlyPlanRepository(days = mapOf(7L to day(7L, sessionType = null, isRestDay = true)))

        val resolved = MonthlyPlanDayWorkoutPlanner.resolveDay(7L, monthlyPlanRepository, FakeExerciseRepository(emptyList()), FakeWorkoutRepository())

        assertNull(resolved)
    }

    @Test
    fun `a day with no exercises resolves to null`() = runBlocking {
        val monthlyPlanRepository = FakeMonthlyPlanRepository(days = mapOf(7L to day(7L, sessionType = "Push")))

        val resolved = MonthlyPlanDayWorkoutPlanner.resolveDay(7L, monthlyPlanRepository, FakeExerciseRepository(emptyList()), FakeWorkoutRepository())

        assertNull(resolved)
    }

    @Test
    fun `an unresolvable day id resolves to null`() = runBlocking {
        val monthlyPlanRepository = FakeMonthlyPlanRepository()

        val resolved = MonthlyPlanDayWorkoutPlanner.resolveDay(404L, monthlyPlanRepository, FakeExerciseRepository(emptyList()), FakeWorkoutRepository())

        assertNull(resolved)
    }

    @Test
    fun `an exercise row whose exerciseId isn't in the catalog is dropped, not crashed on`() = runBlocking {
        val monthlyPlanRepository = FakeMonthlyPlanRepository(
            days = mapOf(7L to day(7L, sessionType = "Push")),
            exercisesByDay = mapOf(
                7L to listOf(
                    planExercise(dayId = 7L, exerciseId = 1L, orderIndex = 0),
                    planExercise(dayId = 7L, exerciseId = 999L, orderIndex = 1), // not in the catalog
                ),
            ),
        )
        val exerciseRepository = FakeExerciseRepository(listOf(exercise(1L, "Bench Press")))

        val resolved = MonthlyPlanDayWorkoutPlanner.resolveDay(7L, monthlyPlanRepository, exerciseRepository, FakeWorkoutRepository())

        assertEquals(listOf("Bench Press"), resolved?.items?.map { it.exercise.nameVi })
    }

    @Test
    fun `a row's own targetWeightKg wins over the workout repository's recommendation`() = runBlocking {
        val monthlyPlanRepository = FakeMonthlyPlanRepository(
            days = mapOf(7L to day(7L, sessionType = "Push")),
            exercisesByDay = mapOf(7L to listOf(planExercise(dayId = 7L, exerciseId = 1L, targetWeightKg = 42.5))),
        )
        val exerciseRepository = FakeExerciseRepository(listOf(exercise(1L, "Bench Press")))

        val resolved = MonthlyPlanDayWorkoutPlanner.resolveDay(7L, monthlyPlanRepository, exerciseRepository, FakeWorkoutRepository(recommendedWeightKg = 30.0))

        assertEquals(42.5, resolved?.items?.single()?.recommendedWeightKg)
    }

    @Test
    fun `a null targetWeightKg falls back to the workout repository's recommendation`() = runBlocking {
        val monthlyPlanRepository = FakeMonthlyPlanRepository(
            days = mapOf(7L to day(7L, sessionType = "Push")),
            exercisesByDay = mapOf(7L to listOf(planExercise(dayId = 7L, exerciseId = 1L, targetWeightKg = null))),
        )
        val exerciseRepository = FakeExerciseRepository(listOf(exercise(1L, "Bench Press")))

        val resolved = MonthlyPlanDayWorkoutPlanner.resolveDay(7L, monthlyPlanRepository, exerciseRepository, FakeWorkoutRepository(recommendedWeightKg = 30.0))

        assertEquals(30.0, resolved?.items?.single()?.recommendedWeightKg)
    }

    @Test
    fun `no targetWeightKg and no recommendation falls back to the shared default`() = runBlocking {
        val monthlyPlanRepository = FakeMonthlyPlanRepository(
            days = mapOf(7L to day(7L, sessionType = "Push")),
            exercisesByDay = mapOf(7L to listOf(planExercise(dayId = 7L, exerciseId = 1L, targetWeightKg = null))),
        )
        val exerciseRepository = FakeExerciseRepository(listOf(exercise(1L, "Bench Press")))

        val resolved = MonthlyPlanDayWorkoutPlanner.resolveDay(7L, monthlyPlanRepository, exerciseRepository, FakeWorkoutRepository(recommendedWeightKg = null))

        assertEquals(ProgramDayWorkoutPlanner.DEFAULT_RECOMMENDED_WEIGHT_KG, resolved?.items?.single()?.recommendedWeightKg)
    }

    @Test
    fun `supersetGroup passes through unchanged for downstream buildBlocks to pair`() = runBlocking {
        val monthlyPlanRepository = FakeMonthlyPlanRepository(
            days = mapOf(7L to day(7L, sessionType = "Push")),
            exercisesByDay = mapOf(
                7L to listOf(
                    planExercise(dayId = 7L, exerciseId = 1L, orderIndex = 0, supersetGroup = "X"),
                    planExercise(dayId = 7L, exerciseId = 2L, orderIndex = 1, supersetGroup = "X"),
                ),
            ),
        )
        val exerciseRepository = FakeExerciseRepository(listOf(exercise(1L, "Cable Fly"), exercise(2L, "Lateral Raise")))

        val resolved = MonthlyPlanDayWorkoutPlanner.resolveDay(7L, monthlyPlanRepository, exerciseRepository, FakeWorkoutRepository())

        val blocks = ProgramDayWorkoutPlanner.buildBlocks(resolved!!.items)
        assertEquals(1, blocks.size)
        val plan = (blocks.single() as WorkoutBlockPlan.Superset).plan
        assertEquals("Cable Fly", plan.exerciseA.nameVi)
        assertEquals("Lateral Raise", plan.exerciseB.nameVi)
    }
}
