package com.fitviet.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers [WeekDayCellCalculator.compute] — the "Tuần này" 7-circle card's trailing-7-day window
 * and its 3-real-state mapping (confirmed against the mock's own JS at the Gate 2c review: a
 * future day and a rest day render identically, since the mock's `weekDays` mapping never actually
 * branches on any `rest` flag).
 */
class WeekDayCellCalculatorTest {

    private val today = LocalDate.of(2024, 1, 10) // a Wednesday

    @Test
    fun `always returns exactly 7 cells ending on today`() {
        val cells = WeekDayCellCalculator.compute(today, trainedDates = emptySet())

        assertEquals(7, cells.size)
        assertEquals(today.minusDays(6), cells.first().date)
        assertEquals(today, cells.last().date)
    }

    @Test
    fun `today with a trained session is TODAY_DONE`() {
        val cells = WeekDayCellCalculator.compute(today, trainedDates = setOf(today))

        assertEquals(WeekDayCellState.TODAY_DONE, cells.last().state)
    }

    @Test
    fun `today with no trained session is TODAY_PENDING`() {
        val cells = WeekDayCellCalculator.compute(today, trainedDates = emptySet())

        assertEquals(WeekDayCellState.TODAY_PENDING, cells.last().state)
    }

    @Test
    fun `a past trained day is DONE`() {
        val trainedYesterday = today.minusDays(1)
        val cells = WeekDayCellCalculator.compute(today, trainedDates = setOf(trainedYesterday))

        val cell = cells.first { it.date == trainedYesterday }
        assertEquals(WeekDayCellState.DONE, cell.state)
    }

    @Test
    fun `a past untrained day is EMPTY, same as this app's rest-day representation`() {
        // The calculator itself has no concept of a "rest day" — WeekDayCell only knows a date was
        // trained or wasn't. A rest day and a day the user simply skipped are indistinguishable
        // here by design (matches the mock's own live prototype, not its static comparison block).
        val untrainedPast = today.minusDays(2)
        val cells = WeekDayCellCalculator.compute(today, trainedDates = emptySet())

        val cell = cells.first { it.date == untrainedPast }
        assertEquals(WeekDayCellState.EMPTY, cell.state)
    }

    @Test
    fun `trained dates outside the trailing 7-day window do not affect any cell`() {
        val cells = WeekDayCellCalculator.compute(today, trainedDates = setOf(today.minusDays(30)))

        assertEquals(WeekDayCellState.TODAY_PENDING, cells.last().state)
        assertEquals(true, cells.all { it.state != WeekDayCellState.DONE })
    }
}
