package com.fitviet.app.ui.workout

import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.domain.ExerciseDifficulty
import com.fitviet.app.domain.MovementType
import com.fitviet.app.domain.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramDayWorkoutPlannerTest {

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

    private fun item(
        id: Long,
        nameVi: String,
        targetSets: Int = 3,
        targetRepsMin: Int = 8,
        targetRepsMax: Int = 12,
        supersetGroup: String? = null,
    ) = ProgramDayWorkoutItem(
        exercise = exercise(id, nameVi),
        targetSets = targetSets,
        targetRepsMin = targetRepsMin,
        targetRepsMax = targetRepsMax,
        recommendedWeightKg = 20.0,
        supersetGroup = supersetGroup,
    )

    @Test
    fun `an empty item list yields an empty block list`() {
        assertTrue(ProgramDayWorkoutPlanner.buildBlocks(emptyList()).isEmpty())
    }

    @Test
    fun `a null supersetGroup produces a straight block`() {
        val result = ProgramDayWorkoutPlanner.buildBlocks(listOf(item(1, "A")))

        assertEquals(1, result.size)
        assertTrue(result.single() is WorkoutBlockPlan.Straight)
    }

    @Test
    fun `two adjacent items sharing a group form one superset block`() {
        val items = listOf(item(1, "A", supersetGroup = "X"), item(2, "B", supersetGroup = "X"))

        val result = ProgramDayWorkoutPlanner.buildBlocks(items)

        assertEquals(1, result.size)
        val plan = (result.single() as WorkoutBlockPlan.Superset).plan
        assertEquals("A", plan.exerciseA.nameVi)
        assertEquals("B", plan.exerciseB.nameVi)
    }

    @Test
    fun `a superset pair does not need to be the whole list`() {
        val items = listOf(
            item(1, "Bench"),
            item(2, "Shoulder"),
            item(3, "CableFly", supersetGroup = "X"),
            item(4, "LateralRaise", supersetGroup = "X"),
        )

        val result = ProgramDayWorkoutPlanner.buildBlocks(items)

        assertEquals(3, result.size)
        assertTrue(result[0] is WorkoutBlockPlan.Straight)
        assertTrue(result[1] is WorkoutBlockPlan.Straight)
        assertTrue(result[2] is WorkoutBlockPlan.Superset)
    }

    @Test
    fun `a lone item with a group value degrades to straight rather than vanishing`() {
        val result = ProgramDayWorkoutPlanner.buildBlocks(listOf(item(1, "A", supersetGroup = "X")))

        assertEquals(1, result.size)
        assertTrue(result.single() is WorkoutBlockPlan.Straight)
    }

    @Test
    fun `three items sharing one group all degrade to straight, none dropped`() {
        val items = listOf(
            item(1, "A", supersetGroup = "X"),
            item(2, "B", supersetGroup = "X"),
            item(3, "C", supersetGroup = "X"),
        )

        val result = ProgramDayWorkoutPlanner.buildBlocks(items)

        assertEquals(3, result.size)
        assertTrue(result.all { it is WorkoutBlockPlan.Straight })
        assertEquals(listOf("A", "B", "C"), result.map { (it as WorkoutBlockPlan.Straight).plan.exercise.nameVi })
    }

    @Test
    fun `two items sharing a group but not adjacent both degrade to straight`() {
        val items = listOf(
            item(1, "A", supersetGroup = "X"),
            item(2, "B"),
            item(3, "C", supersetGroup = "X"),
        )

        val result = ProgramDayWorkoutPlanner.buildBlocks(items)

        assertEquals(3, result.size)
        assertTrue(result.all { it is WorkoutBlockPlan.Straight })
    }

    @Test
    fun `mismatched targetSets in a pair reconcile to the lower value`() {
        val items = listOf(
            item(1, "A", targetSets = 4, supersetGroup = "X"),
            item(2, "B", targetSets = 3, supersetGroup = "X"),
        )

        val plan = (ProgramDayWorkoutPlanner.buildBlocks(items).single() as WorkoutBlockPlan.Superset).plan

        assertEquals(3, plan.totalRounds)
    }

    @Test
    fun `straight blocks collapse rep range to its midpoint and repeat it per set`() {
        val result = ProgramDayWorkoutPlanner.buildBlocks(listOf(item(1, "A", targetSets = 4, targetRepsMin = 8, targetRepsMax = 10)))

        val plan = (result.single() as WorkoutBlockPlan.Straight).plan
        assertEquals(4, plan.plannedSets.size)
        assertTrue(plan.plannedSets.all { it.reps == 9 })
    }

    @Test
    fun `two distinct groups each pair up independently`() {
        val items = listOf(
            item(1, "A", supersetGroup = "X"),
            item(2, "B", supersetGroup = "X"),
            item(3, "C", supersetGroup = "Y"),
            item(4, "D", supersetGroup = "Y"),
        )

        val result = ProgramDayWorkoutPlanner.buildBlocks(items)

        assertEquals(2, result.size)
        assertTrue(result.all { it is WorkoutBlockPlan.Superset })
    }
}
