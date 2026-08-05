package com.fitviet.app.domain

import java.time.LocalDate
import java.time.YearMonth

/** One cell in a 7-column month grid. Leading/trailing cells pad the grid to full weeks. */
data class CalendarDayCell(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val hasCompletedSession: Boolean,
)

/**
 * Builds a Monday-start month grid for [WorkoutCalendarScreen]. Pure and testable: no Room/Compose
 * dependency, mirrors the [com.fitviet.app.domain.WeightHistoryCalculator] pattern of keeping
 * date math out of the ViewModel/Composable layer.
 */
object WorkoutCalendarCalculator {
    /**
     * [completedDates] is the set of calendar dates that have at least one completed workout
     * session. Returns a flat list of cells covering [month] padded to full Monday–Sunday weeks
     * (so the UI can chunk it into rows of 7 with no partial-week special-casing).
     */
    fun grid(month: YearMonth, completedDates: Set<LocalDate>): List<CalendarDayCell> {
        val firstOfMonth = month.atDay(1)
        val lastOfMonth = month.atEndOfMonth()
        val leadingDays = firstOfMonth.dayOfWeek.value - 1 // ISO: Monday=1..Sunday=7
        val trailingDays = 7 - lastOfMonth.dayOfWeek.value
        val gridStart = firstOfMonth.minusDays(leadingDays.toLong())
        val gridEnd = lastOfMonth.plusDays(trailingDays.toLong())

        return generateSequence(gridStart) { it.plusDays(1) }
            .takeWhile { !it.isAfter(gridEnd) }
            .map { date ->
                CalendarDayCell(
                    date = date,
                    isCurrentMonth = YearMonth.from(date) == month,
                    hasCompletedSession = date in completedDates,
                )
            }
            .toList()
    }
}
