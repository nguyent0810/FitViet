package com.fitviet.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers [PersonalRecordCalculator.countInWindow] — flagged as untested at the Gate 2c review
 * (which also caught the two bugs the per-day-collapse + `hasBaseline` flag below fix: multiple
 * sets in one session inflating the count, and a zero-weight first set never establishing a
 * baseline).
 */
class PersonalRecordCalculatorTest {

    private val today = LocalDate.of(2024, 1, 10)
    private val windowStart = today.minusDays(6)

    @Test
    fun `first-ever logged day for an exercise is never counted as a PR`() {
        val sets = listOf(ExerciseSetRecord(exerciseId = 1L, date = today, weightKg = 50.0))

        assertEquals(0, PersonalRecordCalculator.countInWindow(sets, windowStart, today))
    }

    @Test
    fun `a later day exceeding every prior day counts as one PR`() {
        val sets = listOf(
            ExerciseSetRecord(exerciseId = 1L, date = today.minusDays(3), weightKg = 50.0),
            ExerciseSetRecord(exerciseId = 1L, date = today, weightKg = 60.0),
        )

        assertEquals(1, PersonalRecordCalculator.countInWindow(sets, windowStart, today))
    }

    @Test
    fun `multiple sets in one session collapse to a single day-max, counting at most one PR that day`() {
        // A ramp-up session (60/70/80kg) all sharing the same date, since the source query only
        // carries the session's completion timestamp, not a per-set one.
        val sets = listOf(
            ExerciseSetRecord(exerciseId = 1L, date = today.minusDays(3), weightKg = 50.0),
            ExerciseSetRecord(exerciseId = 1L, date = today, weightKg = 60.0),
            ExerciseSetRecord(exerciseId = 1L, date = today, weightKg = 70.0),
            ExerciseSetRecord(exerciseId = 1L, date = today, weightKg = 80.0),
        )

        assertEquals(1, PersonalRecordCalculator.countInWindow(sets, windowStart, today))
    }

    @Test
    fun `the result is independent of the input row order within a day`() {
        val ascending = listOf(
            ExerciseSetRecord(exerciseId = 1L, date = today.minusDays(3), weightKg = 50.0),
            ExerciseSetRecord(exerciseId = 1L, date = today, weightKg = 60.0),
            ExerciseSetRecord(exerciseId = 1L, date = today, weightKg = 70.0),
        )
        val shuffled = listOf(
            ExerciseSetRecord(exerciseId = 1L, date = today, weightKg = 70.0),
            ExerciseSetRecord(exerciseId = 1L, date = today.minusDays(3), weightKg = 50.0),
            ExerciseSetRecord(exerciseId = 1L, date = today, weightKg = 60.0),
        )

        assertEquals(
            PersonalRecordCalculator.countInWindow(ascending, windowStart, today),
            PersonalRecordCalculator.countInWindow(shuffled, windowStart, today),
        )
    }

    @Test
    fun `a zero-weight first day still establishes a baseline for a later weighted PR`() {
        // e.g. bodyweight-only pull-ups logged at 0kg before the first belted rep.
        val sets = listOf(
            ExerciseSetRecord(exerciseId = 1L, date = today.minusDays(5), weightKg = 0.0),
            ExerciseSetRecord(exerciseId = 1L, date = today, weightKg = 10.0),
        )

        assertEquals(1, PersonalRecordCalculator.countInWindow(sets, windowStart, today))
    }

    @Test
    fun `a day outside the window is never counted even if it is a new max`() {
        val sets = listOf(
            ExerciseSetRecord(exerciseId = 1L, date = today.minusDays(20), weightKg = 50.0),
            ExerciseSetRecord(exerciseId = 1L, date = today.minusDays(10), weightKg = 60.0),
        )

        assertEquals(0, PersonalRecordCalculator.countInWindow(sets, windowStart, today))
    }

    @Test
    fun `different exercises are tracked independently`() {
        val sets = listOf(
            ExerciseSetRecord(exerciseId = 1L, date = today.minusDays(3), weightKg = 50.0),
            ExerciseSetRecord(exerciseId = 1L, date = today, weightKg = 60.0),
            ExerciseSetRecord(exerciseId = 2L, date = today.minusDays(3), weightKg = 20.0),
            ExerciseSetRecord(exerciseId = 2L, date = today, weightKg = 25.0),
        )

        assertEquals(2, PersonalRecordCalculator.countInWindow(sets, windowStart, today))
    }

    @Test
    fun `a day matching but not exceeding the prior max is not a PR`() {
        val sets = listOf(
            ExerciseSetRecord(exerciseId = 1L, date = today.minusDays(3), weightKg = 50.0),
            ExerciseSetRecord(exerciseId = 1L, date = today, weightKg = 50.0),
        )

        assertEquals(0, PersonalRecordCalculator.countInWindow(sets, windowStart, today))
    }

    @Test
    fun `no sets in the window returns zero`() {
        assertEquals(0, PersonalRecordCalculator.countInWindow(emptyList(), windowStart, today))
    }
}
