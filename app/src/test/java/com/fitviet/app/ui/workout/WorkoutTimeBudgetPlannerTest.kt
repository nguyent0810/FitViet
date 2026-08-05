package com.fitviet.app.ui.workout

import com.fitviet.app.data.local.seed.SeedData
import com.fitviet.app.data.local.seed.SeedExerciseNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests against the real [SeedData.exercises] catalog (not a synthetic fixture, unlike
 * [WorkoutViewModelTest]) — this is specifically testing "does a time budget produce a sensible
 * real session from the real exercise library," so it should track actual seed content, including
 * catching behavior drift if someone edits an exercise's suggested sets/reps/rest later.
 */
class WorkoutTimeBudgetPlannerTest {

    private fun blockNames(blocks: List<WorkoutBlockPlan>): List<String> =
        blocks.map { (it as WorkoutBlockPlan.Straight).plan.exercise.nameVi }

    @Test
    fun `30-minute budget fits 5 compound exercises in curriculum order`() {
        // Rest between sets during estimation must match DEFAULT_REST_SECONDS (the fixed value the
        // runtime rest timer actually counts down from, see WorkoutViewModel) rather than each
        // exercise's own suggestedRestSeconds — otherwise the estimate doesn't reflect the session
        // the app actually runs. With the shared fixed rest, Deadlift's block is cheaper than its
        // own 150s suggested rest would imply, so it now fits within 30 minutes.
        val blocks = WorkoutTimeBudgetPlanner.buildBlocks(SeedData.exercises, budgetMinutes = 30)

        assertEquals(
            listOf(
                SeedExerciseNames.SQUAT,
                SeedExerciseNames.BENCH_PRESS,
                SeedExerciseNames.BENT_OVER_ROW,
                SeedExerciseNames.SHOULDER_PRESS,
                SeedExerciseNames.DEADLIFT,
            ),
            blockNames(blocks),
        )
    }

    @Test
    fun `60-minute budget fits 11 exercises before Lateral Raise would push past the limit`() {
        val blocks = WorkoutTimeBudgetPlanner.buildBlocks(SeedData.exercises, budgetMinutes = 60)

        assertEquals(
            listOf(
                SeedExerciseNames.SQUAT,
                SeedExerciseNames.BENCH_PRESS,
                SeedExerciseNames.BENT_OVER_ROW,
                SeedExerciseNames.SHOULDER_PRESS,
                SeedExerciseNames.DEADLIFT,
                SeedExerciseNames.LAT_PULLDOWN,
                SeedExerciseNames.LEG_PRESS,
                SeedExerciseNames.LUNGE,
                SeedExerciseNames.BARBELL_CURL,
                SeedExerciseNames.TRICEPS_PUSHDOWN,
                SeedExerciseNames.CABLE_FLY,
            ),
            blockNames(blocks),
        )
    }

    @Test
    fun `a longer budget never produces fewer exercises than a shorter one`() {
        val thirty = WorkoutTimeBudgetPlanner.buildBlocks(SeedData.exercises, budgetMinutes = 30)
        val sixty = WorkoutTimeBudgetPlanner.buildBlocks(SeedData.exercises, budgetMinutes = 60)

        assertTrue(sixty.size > thirty.size)
        // The 60-minute session is a strict extension of the 30-minute one, not a different mix.
        assertEquals(blockNames(thirty), blockNames(sixty).take(thirty.size))
    }

    @Test
    fun `even a tiny budget always includes at least one exercise`() {
        val blocks = WorkoutTimeBudgetPlanner.buildBlocks(SeedData.exercises, budgetMinutes = 1)

        assertEquals(1, blocks.size)
        assertEquals(SeedExerciseNames.SQUAT, blockNames(blocks).single())
    }

    @Test
    fun `generated blocks are straight-set only, no supersets, and start at 0kg`() {
        val blocks = WorkoutTimeBudgetPlanner.buildBlocks(SeedData.exercises, budgetMinutes = 60)

        assertTrue(blocks.all { it is WorkoutBlockPlan.Straight })
        val allPlannedSets = blocks.flatMap { (it as WorkoutBlockPlan.Straight).plan.plannedSets }
        assertTrue(allPlannedSets.all { it.weightKg == 0.0 })
        assertTrue(allPlannedSets.all { it.reps > 0 })
    }

    @Test
    fun `an empty catalog produces an empty session instead of crashing`() {
        val blocks = WorkoutTimeBudgetPlanner.buildBlocks(emptyList(), budgetMinutes = 30)

        assertTrue(blocks.isEmpty())
    }
}
