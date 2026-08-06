package com.fitviet.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseHistoryCalculatorTest {

    private val today = LocalDate.of(2026, 8, 4)

    @Test
    fun `an empty set list returns an empty history`() {
        assertEquals(emptyList<ExerciseHistoryEntry>(), ExerciseHistoryCalculator.bestSetPerDate(emptyList()))
    }

    @Test
    fun `a single set on one date becomes a single entry`() {
        val result = ExerciseHistoryCalculator.bestSetPerDate(listOf(LoggedSetPoint(today, 40.0, 10)))

        assertEquals(listOf(ExerciseHistoryEntry(today, 40.0, 10)), result)
    }

    @Test
    fun `multiple sets on the same date collapse into that date's heaviest set`() {
        val sets = listOf(
            LoggedSetPoint(today, 40.0, 10),
            LoggedSetPoint(today, 50.0, 8),
            LoggedSetPoint(today, 45.0, 6),
        )

        val result = ExerciseHistoryCalculator.bestSetPerDate(sets)

        assertEquals(listOf(ExerciseHistoryEntry(today, 50.0, 8)), result)
    }

    @Test
    fun `a tie on weight is broken by more reps`() {
        val sets = listOf(
            LoggedSetPoint(today, 50.0, 6),
            LoggedSetPoint(today, 50.0, 10),
        )

        val result = ExerciseHistoryCalculator.bestSetPerDate(sets)

        assertEquals(10, result.single().reps)
    }

    @Test
    fun `multiple dates return one entry each, sorted newest first`() {
        val sets = listOf(
            LoggedSetPoint(today.minusDays(7), 30.0, 10),
            LoggedSetPoint(today, 40.0, 8),
            LoggedSetPoint(today.minusDays(3), 35.0, 8),
        )

        val result = ExerciseHistoryCalculator.bestSetPerDate(sets)

        assertEquals(listOf(today, today.minusDays(3), today.minusDays(7)), result.map { it.date })
    }
}
