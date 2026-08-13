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

    /** "Hit & Run" redesign (Gate 1c) — everything [TodayMonthlyPlanCard] needs EXCEPT the
     * Training/[TodayMonthlyPlanCard.Unavailable] distinction, which needs that day's exercise
     * list — a second, day-id-dependent query the caller only knows how to run once [HasSession]
     * gives it a `dayId`. Pure and Room-free, so it's directly unit-testable across all 4 states. */
    fun classify(activePlanId: Long?, days: List<MonthlyPlanDayEntity>, today: LocalDate): TodayPlanDayState {
        if (activePlanId == null) return TodayPlanDayState.NoPlan
        val row = resolve(days, today) ?: return TodayPlanDayState.PlanFinished
        val sessionType = row.sessionType
        return if (row.isRestDay || sessionType == null) {
            TodayPlanDayState.RestDay
        } else {
            TodayPlanDayState.HasSession(dayId = row.id, sessionType = sessionType)
        }
    }
}

/** [MonthlyPlanTodayResolver.classify]'s return type — see [TodayMonthlyPlanCard] for the fuller,
 * exercise-list-aware type this feeds into. */
sealed interface TodayPlanDayState {
    data object NoPlan : TodayPlanDayState
    data object PlanFinished : TodayPlanDayState
    data object RestDay : TodayPlanDayState
    data class HasSession(val dayId: Long, val sessionType: String) : TodayPlanDayState
}
