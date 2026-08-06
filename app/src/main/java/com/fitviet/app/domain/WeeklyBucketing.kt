package com.fitviet.app.domain

import java.time.DayOfWeek
import java.time.LocalDate

data class WeekVolume(val weekStart: LocalDate, val volumeKg: Double)

/**
 * Screen-agnostic Monday-start weekly bucketing (Gate 43, mismatch #7) — extracted out of what
 * used to be [DiaryStatsCalculator]'s own private logic so [DashboardStatsCalculator]'s range
 * views can reuse the exact same bucketing rule instead of calling into Diary's calculator
 * directly. [DiaryStatsCalculator.lastNWeeks] is now a thin wrapper over this.
 */
object WeeklyBucketing {
    fun lastNWeeks(sessions: List<CompletedSession>, today: LocalDate, weeks: Int): List<WeekVolume> {
        val thisMonday = today.with(DayOfWeek.MONDAY)
        return (weeks - 1 downTo 0).map { offset ->
            val weekStart = thisMonday.minusWeeks(offset.toLong())
            val weekEnd = weekStart.plusDays(6)
            val volume = sessions.filter { it.date >= weekStart && it.date <= weekEnd }.sumOf { it.volumeKg }
            WeekVolume(weekStart, volume)
        }
    }
}
