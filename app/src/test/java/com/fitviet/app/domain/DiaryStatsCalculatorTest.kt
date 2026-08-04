package com.fitviet.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DiaryStatsCalculatorTest {

    private val today = LocalDate.of(2026, 8, 4) // a Tuesday

    @Test
    fun `returns the requested number of weeks, oldest first, ending with the current week`() {
        val weeks = DiaryStatsCalculator.lastNWeeks(emptyList(), today, weeks = 4)

        assertEquals(4, weeks.size)
        assertEquals(today.with(java.time.DayOfWeek.MONDAY).minusWeeks(3), weeks.first().weekStart)
        assertEquals(today.with(java.time.DayOfWeek.MONDAY), weeks.last().weekStart)
    }

    @Test
    fun `sessions are bucketed into the correct week`() {
        val thisMonday = today.with(java.time.DayOfWeek.MONDAY)
        val sessions = listOf(
            CompletedSession(thisMonday, 100.0), // this week
            CompletedSession(thisMonday.minusDays(1), 200.0), // last week's Sunday
            CompletedSession(thisMonday.minusWeeks(3), 50.0), // oldest week in the 4-week window
        )

        val weeks = DiaryStatsCalculator.lastNWeeks(sessions, today, weeks = 4)

        assertEquals(50.0, weeks[0].volumeKg, 0.0)
        assertEquals(200.0, weeks[2].volumeKg, 0.0)
        assertEquals(100.0, weeks[3].volumeKg, 0.0)
    }
}
