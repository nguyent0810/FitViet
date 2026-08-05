package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.MeasurementEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightHistoryCalculatorTest {

    private val today = LocalDate.of(2026, 8, 5)

    private fun measurement(id: Long, daysAgo: Long, weight: Double?) =
        MeasurementEntity(id = id, epochDay = today.minusDays(daysAgo).toEpochDay(), weightKg = weight)

    @Test
    fun `empty input yields no points`() {
        val points = WeightHistoryCalculator.points(emptyList(), WeightHistoryRange.ALL_TIME, today)

        assertTrue(points.isEmpty())
    }

    @Test
    fun `rows with no weight reading are dropped`() {
        val rows = listOf(
            measurement(id = 2, daysAgo = 0, weight = null), // e.g. a chest/waist-only check-in
            measurement(id = 1, daysAgo = 1, weight = 71.0),
        )

        val points = WeightHistoryCalculator.points(rows, WeightHistoryRange.ALL_TIME, today)

        assertEquals(1, points.size)
        assertEquals(71.0, points.single().weightKg, 0.0001)
    }

    @Test
    fun `output is sorted ascending by date regardless of input order`() {
        // Newest-first input, matching MeasurementDao.observeAll()'s ordering.
        val rows = listOf(
            measurement(id = 3, daysAgo = 0, weight = 73.0),
            measurement(id = 2, daysAgo = 5, weight = 72.0),
            measurement(id = 1, daysAgo = 10, weight = 71.0),
        )

        val points = WeightHistoryCalculator.points(rows, WeightHistoryRange.ALL_TIME, today)

        assertEquals(listOf(71.0, 72.0, 73.0), points.map { it.weightKg })
        assertEquals(today.minusDays(10), points.first().date)
        assertEquals(today, points.last().date)
    }

    @Test
    fun `a day with two check-ins keeps only the newest-inserted one`() {
        // Newest-first, id DESC within the same day — matches MeasurementDao's tiebreak.
        val rows = listOf(
            measurement(id = 2, daysAgo = 0, weight = 73.5), // corrected same-day re-weigh
            measurement(id = 1, daysAgo = 0, weight = 73.0), // original same-day entry
        )

        val points = WeightHistoryCalculator.points(rows, WeightHistoryRange.ALL_TIME, today)

        assertEquals(1, points.size)
        assertEquals(73.5, points.single().weightKg, 0.0001)
    }

    @Test
    fun `30-day range excludes older check-ins`() {
        val rows = listOf(
            measurement(id = 2, daysAgo = 10, weight = 73.0),
            measurement(id = 1, daysAgo = 40, weight = 70.0),
        )

        val points = WeightHistoryCalculator.points(rows, WeightHistoryRange.THIRTY_DAYS, today)

        assertEquals(1, points.size)
        assertEquals(73.0, points.single().weightKg, 0.0001)
    }

    @Test
    fun `30-day range spans exactly 30 calendar dates, not 31`() {
        val rows = listOf(
            measurement(id = 2, daysAgo = 29, weight = 71.0), // oldest date still inside the window
            measurement(id = 1, daysAgo = 30, weight = 70.0), // one day too old
        )

        val points = WeightHistoryCalculator.points(rows, WeightHistoryRange.THIRTY_DAYS, today)

        assertEquals(1, points.size)
        assertEquals(71.0, points.single().weightKg, 0.0001)
    }

    @Test
    fun `3-month range uses a real calendar-month subtraction`() {
        // Exactly 3 calendar months before today (2026-08-05 -> 2026-05-05) is inside the window;
        // one day earlier is not. A fixed 90-day approximation would get this boundary wrong.
        val rows = listOf(
            measurement(id = 2, daysAgo = today.toEpochDay() - today.minusMonths(3).toEpochDay(), weight = 71.0),
            measurement(id = 1, daysAgo = today.toEpochDay() - today.minusMonths(3).minusDays(1).toEpochDay(), weight = 70.0),
        )

        val points = WeightHistoryCalculator.points(rows, WeightHistoryRange.THREE_MONTHS, today)

        assertEquals(1, points.size)
        assertEquals(71.0, points.single().weightKg, 0.0001)
    }

    @Test
    fun `all-time range keeps every reading no matter how old`() {
        val rows = listOf(measurement(id = 1, daysAgo = 400, weight = 65.0))

        val points = WeightHistoryCalculator.points(rows, WeightHistoryRange.ALL_TIME, today)

        assertEquals(1, points.size)
    }
}
