package com.fitviet.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardStatsCalculatorTest {

    private val today = LocalDate.of(2026, 8, 4) // a Tuesday

    @Test
    fun `volume this week sums only sessions within the current Mon-Sun window`() {
        val sessions = listOf(
            CompletedSession(today, 100.0),
            CompletedSession(today.minusDays(1), 200.0), // Monday, same week
            CompletedSession(today.minusDays(2), 300.0), // last Sunday, previous week
        )

        val stats = DashboardStatsCalculator.compute(sessions, today)

        assertEquals(300.0, stats.volumeThisWeekKg, 0.0)
        assertEquals(2, stats.sessionsThisWeek)
    }

    @Test
    fun `same-day sessions accumulate into one day's volume`() {
        val sessions = listOf(
            CompletedSession(today, 40.0),
            CompletedSession(today, 60.0),
        )

        val stats = DashboardStatsCalculator.compute(sessions, today)

        assertEquals(100.0, stats.last7Days.last().volumeKg, 0.0)
    }

    @Test
    fun `last 7 days covers today and the 6 days before it, oldest first`() {
        val stats = DashboardStatsCalculator.compute(emptyList(), today)

        assertEquals(7, stats.last7Days.size)
        assertEquals(today.minusDays(6), stats.last7Days.first().date)
        assertEquals(today, stats.last7Days.last().date)
    }

    @Test
    fun `streak counts consecutive trained days backward from today when today is trained`() {
        val sessions = listOf(
            CompletedSession(today, 10.0),
            CompletedSession(today.minusDays(1), 10.0),
            CompletedSession(today.minusDays(2), 10.0),
            CompletedSession(today.minusDays(4), 10.0), // gap at day 3 breaks the chain here
        )

        val stats = DashboardStatsCalculator.compute(sessions, today)

        assertEquals(3, stats.streakDays)
    }

    @Test
    fun `streak counts from yesterday when today has no session yet`() {
        val sessions = listOf(
            CompletedSession(today.minusDays(1), 10.0),
            CompletedSession(today.minusDays(2), 10.0),
        )

        val stats = DashboardStatsCalculator.compute(sessions, today)

        assertEquals(2, stats.streakDays)
    }

    @Test
    fun `streak is zero when neither today nor yesterday was trained`() {
        val sessions = listOf(CompletedSession(today.minusDays(3), 10.0))

        val stats = DashboardStatsCalculator.compute(sessions, today)

        assertEquals(0, stats.streakDays)
    }

    @Test
    fun `streak keeps counting across the Monday week boundary`() {
        val monday = today.with(java.time.DayOfWeek.MONDAY)
        val sessions = listOf(
            CompletedSession(monday, 10.0), // this week
            CompletedSession(monday.minusDays(1), 10.0), // last Sunday, previous week
            CompletedSession(monday.minusDays(2), 10.0), // last Saturday, previous week
        )

        val stats = DashboardStatsCalculator.compute(sessions, monday)

        assertEquals(3, stats.streakDays)
    }

    // ---- Gate 43: rangeSeries ----

    @Test
    fun `WEEK range series is identical to compute()'s last7Days`() {
        val sessions = listOf(CompletedSession(today, 40.0), CompletedSession(today.minusDays(3), 20.0))

        val series = DashboardStatsCalculator.rangeSeries(sessions, today, StatsRange.WEEK)
        val fromCompute = DashboardStatsCalculator.compute(sessions, today).last7Days

        assertEquals(fromCompute, series)
    }

    @Test
    fun `MONTH range returns 4 weekly buckets, oldest first, ending with the current week`() {
        val series = DashboardStatsCalculator.rangeSeries(emptyList(), today, StatsRange.MONTH)

        assertEquals(4, series.size)
        assertEquals(today.with(java.time.DayOfWeek.MONDAY).minusWeeks(3), series.first().date)
        assertEquals(today.with(java.time.DayOfWeek.MONDAY), series.last().date)
    }

    @Test
    fun `ALL range returns 12 weekly buckets`() {
        val series = DashboardStatsCalculator.rangeSeries(emptyList(), today, StatsRange.ALL)

        assertEquals(12, series.size)
        assertEquals(today.with(java.time.DayOfWeek.MONDAY).minusWeeks(11), series.first().date)
    }

    @Test
    fun `MONTH and ALL bucket sessions into their matching week, matching WeeklyBucketing directly`() {
        val thisMonday = today.with(java.time.DayOfWeek.MONDAY)
        val sessions = listOf(
            CompletedSession(thisMonday, 100.0),
            CompletedSession(thisMonday.minusWeeks(2), 50.0),
        )

        val monthSeries = DashboardStatsCalculator.rangeSeries(sessions, today, StatsRange.MONTH)
        val expected = WeeklyBucketing.lastNWeeks(sessions, today, weeks = 4)

        assertEquals(expected.map { it.weekStart to it.volumeKg }, monthSeries.map { it.date to it.volumeKg })
    }
}
