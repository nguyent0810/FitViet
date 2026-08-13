package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.MonthlyPlanDayEntity
import com.fitviet.app.data.local.entity.MonthlyPlanWeekEntity
import java.time.LocalDate

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

    /** Redesign Phase 3a — the Kế hoạch tab's progress card ("X/Y buổi hoàn thành", "TUẦN W/T").
     * [completedSessions]/[totalSessions] count only non-rest days (a "buổi" is a training day, a
     * rest day was never a session to complete) — [completedDayIds] must come from
     * [com.fitviet.app.data.repository.MonthlyPlanRepository.observeCompletedDayIds], NOT
     * [com.fitviet.app.data.repository.MonthlyPlanRepository.observeLockedDayIds] (that one also
     * counts merely-abandoned sessions, the exact conflation the Phase 2 checkpoint review's H2
     * finding was about). [currentWeekNumber] is the 1-based week containing [MonthlyPlanTodayResolver]'s
     * resolved today-row; if today has no row (plan finished / not started yet) it falls back to
     * the plan's last week rather than null, so a finished plan's progress card still shows a
     * sensible "TUẦN T/T" instead of blanking the label. */
    fun summarize(
        weeks: List<MonthlyPlanWeekEntity>,
        days: List<MonthlyPlanDayEntity>,
        completedDayIds: Set<Long>,
        today: LocalDate,
    ): MonthlyPlanProgressSummary {
        val trainingDays = days.filter { !it.isRestDay }
        val completedSessions = trainingDays.count { it.id in completedDayIds }

        val weekIndexById = weeks.associate { it.id to it.weekIndex }
        val resolvedTodayWeekId = MonthlyPlanTodayResolver.resolve(days, today)?.monthlyPlanWeekId
        val currentWeekNumber = resolvedTodayWeekId?.let { weekIndexById[it] }?.plus(1)
            ?: weeks.maxOfOrNull { it.weekIndex }?.plus(1)

        return MonthlyPlanProgressSummary(
            completedSessions = completedSessions,
            totalSessions = trainingDays.size,
            currentWeekNumber = currentWeekNumber,
        )
    }
}

data class MonthlyPlanProgressSummary(
    val completedSessions: Int,
    val totalSessions: Int,
    /** Null only if [weeks] is empty (shouldn't happen for a generator-produced plan, which always
     * has at least one week). */
    val currentWeekNumber: Int?,
)
