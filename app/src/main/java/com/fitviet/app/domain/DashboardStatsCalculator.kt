package com.fitviet.app.domain

import java.time.DayOfWeek
import java.time.LocalDate

data class CompletedSession(val date: LocalDate, val volumeKg: Double)

/** Pure, unit-testable dashboard math — kept free of Room/Android so it's cheap to test in isolation. */
object DashboardStatsCalculator {

    /** "Month" and "long view" windows for [rangeSeries] — [ALL_TIME_WEEKS] is a deliberate cap,
     * not literally "since account creation": an unbounded window would grow the bar chart (and
     * its per-bar week-number labels) without limit for a long-lived install, so "all" here means
     * "the last 12 weeks," documented plainly rather than implying true full history. */
    private const val MONTH_WEEKS = 4
    private const val ALL_TIME_WEEKS = 12

    fun compute(sessions: List<CompletedSession>, today: LocalDate): DashboardStats {
        val volumeByDate: Map<LocalDate, Double> = sessions
            .groupingBy { it.date }
            .fold(0.0) { acc, session -> acc + session.volumeKg }

        val mondayThisWeek = today.with(DayOfWeek.MONDAY)
        val thisWeekSessions = sessions.filter { it.date >= mondayThisWeek && it.date <= today }

        return DashboardStats(
            streakDays = currentStreak(volumeByDate.keys, today),
            sessionsThisWeek = thisWeekSessions.size,
            volumeThisWeekKg = thisWeekSessions.sumOf { it.volumeKg },
            last7Days = last7Days(sessions, today),
        )
    }

    private fun last7Days(sessions: List<CompletedSession>, today: LocalDate): List<DayVolume> {
        val volumeByDate: Map<LocalDate, Double> = sessions
            .groupingBy { it.date }
            .fold(0.0) { acc, session -> acc + session.volumeKg }
        return (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            DayVolume(date, volumeByDate[date] ?: 0.0)
        }
    }

    /** Feature #7 (Gate 43) — the Dashboard volume chart's range-selectable series. [StatsRange.WEEK]
     * reuses the same daily-granularity data as [compute]'s `last7Days`; [StatsRange.MONTH]/[StatsRange.ALL]
     * switch to [WeeklyBucketing]'s weekly granularity (mismatch #7 — a shared, screen-agnostic
     * utility, not a direct call into [DiaryStatsCalculator]), differing only in window length.
     * Returned as [DayVolume] either way (its `date` means "the day" for WEEK, "the week's Monday"
     * for MONTH/ALL) so the UI only has one series shape to render regardless of range. */
    fun rangeSeries(sessions: List<CompletedSession>, today: LocalDate, range: StatsRange): List<DayVolume> = when (range) {
        StatsRange.WEEK -> last7Days(sessions, today)
        StatsRange.MONTH -> WeeklyBucketing.lastNWeeks(sessions, today, MONTH_WEEKS).map { DayVolume(it.weekStart, it.volumeKg) }
        StatsRange.ALL -> WeeklyBucketing.lastNWeeks(sessions, today, ALL_TIME_WEEKS).map { DayVolume(it.weekStart, it.volumeKg) }
    }

    /**
     * Consecutive trained days counting back from today if today already has a session,
     * otherwise from yesterday — so an not-yet-done today doesn't zero out an active streak.
     * Not private: [com.fitviet.app.data.repository.WorkoutRepository.getCurrentStreakDays]
     * (Gate 40) reuses this directly rather than re-deriving the same rule.
     */
    fun currentStreak(trainedDates: Set<LocalDate>, today: LocalDate): Int {
        var cursor = if (today in trainedDates) today else today.minusDays(1)
        var streak = 0
        while (cursor in trainedDates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}
