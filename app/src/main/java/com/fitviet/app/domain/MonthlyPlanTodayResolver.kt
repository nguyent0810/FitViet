package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.MonthlyPlanDayEntity
import java.time.LocalDate

/** Picks today's day out of an active plan's full day list — pure, so the repository/ViewModel
 * just loads the days once and passes [today] in. Returns null if the plan has no row for today
 * at all (shouldn't happen for a plan [MonthlyPlanGenerator.generate] produced, since it always
 * fills a full 7-day-per-week grid, but a defensively-null result beats a crash). */
object MonthlyPlanTodayResolver {
    fun resolve(days: List<MonthlyPlanDayEntity>, today: LocalDate): MonthlyPlanDayEntity? {
        val todayRows = days.filter { it.effectiveEpochDay == today.toEpochDay() }
        // Push-to-today intentionally does not move the row originally occupying today, so a
        // collision is valid. Prefer the user's explicit reschedule, then any workout over rest.
        return todayRows.firstOrNull {
            it.status == MonthlyPlanDayStatus.RESCHEDULED.name && !it.isRestDay
        } ?: todayRows.firstOrNull { !it.isRestDay }
            ?: todayRows.firstOrNull()
    }
}
