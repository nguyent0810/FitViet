package com.fitviet.app.domain

import java.time.DayOfWeek
import java.time.LocalDate

data class CompletedSession(val date: LocalDate, val volumeKg: Double)

/** Pure, unit-testable dashboard math — kept free of Room/Android so it's cheap to test in isolation. */
object DashboardStatsCalculator {

    fun compute(sessions: List<CompletedSession>, today: LocalDate): DashboardStats {
        val volumeByDate: Map<LocalDate, Double> = sessions
            .groupingBy { it.date }
            .fold(0.0) { acc, session -> acc + session.volumeKg }

        val last7Days = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            DayVolume(date, volumeByDate[date] ?: 0.0)
        }

        val mondayThisWeek = today.with(DayOfWeek.MONDAY)
        val thisWeekSessions = sessions.filter { it.date >= mondayThisWeek && it.date <= today }

        return DashboardStats(
            streakDays = currentStreak(volumeByDate.keys, today),
            sessionsThisWeek = thisWeekSessions.size,
            volumeThisWeekKg = thisWeekSessions.sumOf { it.volumeKg },
            last7Days = last7Days,
        )
    }

    /**
     * Consecutive trained days counting back from today if today already has a session,
     * otherwise from yesterday — so an not-yet-done today doesn't zero out an active streak.
     */
    private fun currentStreak(trainedDates: Set<LocalDate>, today: LocalDate): Int {
        var cursor = if (today in trainedDates) today else today.minusDays(1)
        var streak = 0
        while (cursor in trainedDates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}
