package com.fitviet.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutCompositionCalculatorTest {

    private val since = LocalDate.of(2026, 7, 1)

    @Test
    fun `muscleGroupWorkload returns all six groups with zero entries when there are no sets`() {
        val result = WorkoutCompositionCalculator.muscleGroupWorkload(emptyList(), since)
        assertEquals(MuscleGroup.entries.size, result.size)
        assertEquals(MuscleGroup.entries.toList(), result.map { it.muscleGroup })
        assertEquals(true, result.all { it.volumeKg == 0.0 && it.setCount == 0 })
    }

    @Test
    fun `muscleGroupWorkload sums volume and counts sets per group`() {
        val sets = listOf(
            CompletedSet(since.plusDays(1), "CHEST", "COMPOUND", volumeKg = 100.0),
            CompletedSet(since.plusDays(1), "CHEST", "COMPOUND", volumeKg = 80.0),
            CompletedSet(since.plusDays(2), "BACK", "COMPOUND", volumeKg = 120.0),
        )
        val result = WorkoutCompositionCalculator.muscleGroupWorkload(sets, since)
        val chest = result.first { it.muscleGroup == MuscleGroup.CHEST }
        val back = result.first { it.muscleGroup == MuscleGroup.BACK }
        val legs = result.first { it.muscleGroup == MuscleGroup.LEGS }
        assertEquals(180.0, chest.volumeKg, 0.001)
        assertEquals(2, chest.setCount)
        assertEquals(120.0, back.volumeKg, 0.001)
        assertEquals(1, back.setCount)
        assertEquals(0.0, legs.volumeKg, 0.001)
        assertEquals(0, legs.setCount)
    }

    @Test
    fun `muscleGroupWorkload excludes sets before the window`() {
        val sets = listOf(
            CompletedSet(since.minusDays(1), "CHEST", "COMPOUND", volumeKg = 100.0),
            CompletedSet(since, "CHEST", "COMPOUND", volumeKg = 50.0),
        )
        val chest = WorkoutCompositionCalculator.muscleGroupWorkload(sets, since).first { it.muscleGroup == MuscleGroup.CHEST }
        assertEquals(50.0, chest.volumeKg, 0.001)
        assertEquals(1, chest.setCount)
    }

    @Test
    fun `muscleGroupWorkload silently excludes an unrecognized classification code`() {
        val sets = listOf(CompletedSet(since, "NECK", "COMPOUND", volumeKg = 999.0))
        val result = WorkoutCompositionCalculator.muscleGroupWorkload(sets, since)
        assertEquals(true, result.all { it.volumeKg == 0.0 })
    }

    @Test
    fun `movementTypeDistribution has zero fractions when there are no sets`() {
        val result = WorkoutCompositionCalculator.movementTypeDistribution(emptyList(), since)
        assertEquals(0, result.compoundSets)
        assertEquals(0, result.isolationSets)
        assertEquals(0f, result.compoundFraction, 0.001f)
        assertEquals(0f, result.isolationFraction, 0.001f)
    }

    @Test
    fun `movementTypeDistribution counts and computes fractions`() {
        val sets = listOf(
            CompletedSet(since, "CHEST", "COMPOUND", volumeKg = 10.0),
            CompletedSet(since, "CHEST", "COMPOUND", volumeKg = 10.0),
            CompletedSet(since, "ARMS", "COMPOUND", volumeKg = 10.0),
            CompletedSet(since, "ARMS", "ISOLATION", volumeKg = 10.0),
        )
        val result = WorkoutCompositionCalculator.movementTypeDistribution(sets, since)
        assertEquals(3, result.compoundSets)
        assertEquals(1, result.isolationSets)
        assertEquals(0.75f, result.compoundFraction, 0.001f)
        assertEquals(0.25f, result.isolationFraction, 0.001f)
    }

    @Test
    fun `movementTypeDistribution excludes sets before the window`() {
        val sets = listOf(
            CompletedSet(since.minusDays(1), "CHEST", "COMPOUND", volumeKg = 10.0),
            CompletedSet(since, "CHEST", "ISOLATION", volumeKg = 10.0),
        )
        val result = WorkoutCompositionCalculator.movementTypeDistribution(sets, since)
        assertEquals(0, result.compoundSets)
        assertEquals(1, result.isolationSets)
    }
}
