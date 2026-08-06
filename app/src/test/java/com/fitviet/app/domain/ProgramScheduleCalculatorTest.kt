package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.entity.ProgramDayEntity
import com.fitviet.app.data.local.entity.ProgramExerciseEntity
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramScheduleCalculatorTest {

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
    fun `empty days yields empty schedule`() {
        val result = ProgramScheduleCalculator.build(emptyList(), emptyList(), emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `days are sorted by day-of-week regardless of input order`() {
        val days = listOf(
            ProgramDayEntity(id = 1, programId = 1, dayOfWeek = 5, titleVi = "Fri"),
            ProgramDayEntity(id = 2, programId = 1, dayOfWeek = 1, titleVi = "Mon"),
            ProgramDayEntity(id = 3, programId = 1, dayOfWeek = 3, titleVi = "Wed"),
        )

        val result = ProgramScheduleCalculator.build(days, emptyList(), emptyList())

        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), result.map { it.dayOfWeek })
    }

    @Test
    fun `exercises within a day are ordered by orderIndex regardless of insertion order`() {
        val days = listOf(ProgramDayEntity(id = 1, programId = 1, dayOfWeek = 1, titleVi = "Mon"))
        val exercises = listOf(exercise(10, "A"), exercise(20, "B"), exercise(30, "C"))
        val programExercises = listOf(
            ProgramExerciseEntity(programDayId = 1, exerciseId = 30, orderIndex = 2, targetSets = 3, targetRepsMin = 8, targetRepsMax = 12),
            ProgramExerciseEntity(programDayId = 1, exerciseId = 10, orderIndex = 0, targetSets = 3, targetRepsMin = 8, targetRepsMax = 12),
            ProgramExerciseEntity(programDayId = 1, exerciseId = 20, orderIndex = 1, targetSets = 3, targetRepsMin = 8, targetRepsMax = 12),
        )

        val result = ProgramScheduleCalculator.build(days, programExercises, exercises)

        assertEquals(listOf("A", "B", "C"), result.single().exercises.map { it.nameVi })
    }

    @Test
    fun `an exercise target with no matching catalog entry is dropped, not crashed on`() {
        val days = listOf(ProgramDayEntity(id = 1, programId = 1, dayOfWeek = 1, titleVi = "Mon"))
        val programExercises = listOf(
            ProgramExerciseEntity(programDayId = 1, exerciseId = 999, orderIndex = 0, targetSets = 3, targetRepsMin = 8, targetRepsMax = 12),
        )

        val result = ProgramScheduleCalculator.build(days, programExercises, emptyList())

        assertTrue(result.single().exercises.isEmpty())
    }

    @Test
    fun `a program-exercise belonging to a different day does not leak into this day`() {
        val days = listOf(
            ProgramDayEntity(id = 1, programId = 1, dayOfWeek = 1, titleVi = "Mon"),
            ProgramDayEntity(id = 2, programId = 1, dayOfWeek = 2, titleVi = "Tue"),
        )
        val exercises = listOf(exercise(10, "A"), exercise(20, "B"))
        val programExercises = listOf(
            ProgramExerciseEntity(programDayId = 1, exerciseId = 10, orderIndex = 0, targetSets = 3, targetRepsMin = 8, targetRepsMax = 12),
            ProgramExerciseEntity(programDayId = 2, exerciseId = 20, orderIndex = 0, targetSets = 3, targetRepsMin = 8, targetRepsMax = 12),
        )

        val result = ProgramScheduleCalculator.build(days, programExercises, exercises)

        assertEquals(listOf("A"), result[0].exercises.map { it.nameVi })
        assertEquals(listOf("B"), result[1].exercises.map { it.nameVi })
    }

    @Test
    fun `rest-day flag passes through unchanged`() {
        val days = listOf(ProgramDayEntity(id = 1, programId = 1, dayOfWeek = 7, titleVi = "Nghỉ", isRestDay = true))

        val result = ProgramScheduleCalculator.build(days, emptyList(), emptyList())

        assertTrue(result.single().isRestDay)
    }

    @Test
    fun `supersetGroup passes through unchanged, including null`() {
        val days = listOf(ProgramDayEntity(id = 1, programId = 1, dayOfWeek = 1, titleVi = "Mon"))
        val exercises = listOf(exercise(10, "A"), exercise(20, "B"))
        val programExercises = listOf(
            ProgramExerciseEntity(programDayId = 1, exerciseId = 10, orderIndex = 0, targetSets = 3, targetRepsMin = 8, targetRepsMax = 12, supersetGroup = "A"),
            ProgramExerciseEntity(programDayId = 1, exerciseId = 20, orderIndex = 1, targetSets = 3, targetRepsMin = 8, targetRepsMax = 12),
        )

        val result = ProgramScheduleCalculator.build(days, programExercises, exercises)

        assertEquals(listOf("A", null), result.single().exercises.map { it.supersetGroup })
    }
}
