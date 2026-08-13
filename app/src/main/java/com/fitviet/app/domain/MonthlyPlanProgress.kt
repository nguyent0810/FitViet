package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.MonthlyPlanDayEntity

/**
 * Redesign Gate 2c — the Dashboard Today card's "NGÀY N/Y" label. Deliberately counts [days]'
 * position rather than deriving N from [com.fitviet.app.data.local.entity.MonthlyPlanEntity
 * .startEpochDay] arithmetic: a rescheduled day's `effectiveEpochDay` no longer lines up with a
 * fixed offset from the plan's start, so counting the day's real position in the actual (already
 * effectiveEpochDay-ordered) row sequence stays correct after a push/swap, where date arithmetic
 * would not. Y is always `totalWeeks * 7`, never the mock's own placeholder "56" (an 8-week
 * example this generator doesn't support — every screen in this redesign shows real computed
 * values, never the mock's literal numbers).
 */
object MonthlyPlanProgress {
    /** Null if [todayDayId] isn't found in [days] (shouldn't happen for an active plan's own
     * resolved today-day, but this stays a display helper, not a source of truth — a null result
     * just means the Today card omits the "NGÀY N/Y" label rather than showing a wrong number). */
    fun dayOfPlan(days: List<MonthlyPlanDayEntity>, todayDayId: Long): Int? {
        val index = days.indexOfFirst { it.id == todayDayId }
        return if (index == -1) null else index + 1
    }
}
