package com.fitviet.app.domain

import java.time.DayOfWeek
import java.time.LocalDate

data class WeekVolume(val weekStart: LocalDate, val volumeKg: Double)

/** Pure, unit-testable — same rationale as [DashboardStatsCalculator]. */
object DiaryStatsCalculator {

    fun lastNWeeks(sessions: List<CompletedSession>, today: LocalDate, weeks: Int = 4): List<WeekVolume> {
        val thisMonday = today.with(DayOfWeek.MONDAY)
        return (weeks - 1 downTo 0).map { offset ->
            val weekStart = thisMonday.minusWeeks(offset.toLong())
            val weekEnd = weekStart.plusDays(6)
            val volume = sessions.filter { it.date >= weekStart && it.date <= weekEnd }.sumOf { it.volumeKg }
            WeekVolume(weekStart, volume)
        }
    }
}
