package com.fitviet.app.domain

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutCalendarCalculatorTest {

    @Test
    fun `grid size is always a multiple of 7`() {
        // August 2026 starts on a Saturday and ends on a Monday — an awkward case for off-by-ones.
        val cells = WorkoutCalendarCalculator.grid(YearMonth.of(2026, 8), emptySet())

        assertEquals(0, cells.size % 7)
    }

    @Test
    fun `grid starts on the Monday of the first week and ends on the Sunday of the last`() {
        val cells = WorkoutCalendarCalculator.grid(YearMonth.of(2026, 8), emptySet())

        assertEquals(java.time.DayOfWeek.MONDAY, cells.first().date.dayOfWeek)
        assertEquals(java.time.DayOfWeek.SUNDAY, cells.last().date.dayOfWeek)
    }

    @Test
    fun `every day of the target month is present and marked current-month`() {
        val month = YearMonth.of(2026, 8)
        val cells = WorkoutCalendarCalculator.grid(month, emptySet())

        val currentMonthDates = cells.filter { it.isCurrentMonth }.map { it.date }
        assertEquals((1..month.lengthOfMonth()).map { month.atDay(it) }, currentMonthDates)
    }

    @Test
    fun `leading and trailing padding days are marked not current-month`() {
        val month = YearMonth.of(2026, 8)
        val cells = WorkoutCalendarCalculator.grid(month, emptySet())

        val padding = cells.filterNot { it.isCurrentMonth }
        assertTrue(padding.isNotEmpty())
        assertTrue(padding.all { YearMonth.from(it.date) != month })
    }

    @Test
    fun `a month that starts exactly on Monday needs no leading padding`() {
        // 2026-06-01 is a Monday.
        val month = YearMonth.of(2026, 6)
        val cells = WorkoutCalendarCalculator.grid(month, emptySet())

        assertEquals(month.atDay(1), cells.first().date)
    }

    @Test
    fun `days with a completed session are flagged, others are not`() {
        val month = YearMonth.of(2026, 8)
        val completed = setOf(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 12))

        val cells = WorkoutCalendarCalculator.grid(month, completed)

        val flagged = cells.filter { it.hasCompletedSession }.map { it.date }.toSet()
        assertEquals(completed, flagged)
    }

    @Test
    fun `a completed date outside the requested month is ignored`() {
        val month = YearMonth.of(2026, 8)
        val completed = setOf(LocalDate.of(2026, 7, 1))

        val cells = WorkoutCalendarCalculator.grid(month, completed)

        assertTrue(cells.none { it.hasCompletedSession })
    }
}
