package com.fitviet.app.domain

/**
 * Dashboard "Today" card content sourced from the active "Hit & Run" (Gate 63+) monthly plan —
 * null (at the [com.fitviet.app.data.repository.DashboardRepository] level) when there's no active
 * plan at all, in which case Dashboard falls back to its pre-existing hand-authored-program hero
 * card unchanged. Scoped strictly to *today*'s row — unlike the hand-authored-program path's
 * [NextTraining], this doesn't look ahead to the next non-rest day on a rest day; that lookahead
 * isn't part of this phase's scope (see the "Hit & Run" plan's Phase 5 note).
 */
sealed interface TodayMonthlyPlanCard {
    data class Training(
        val dayId: Long,
        val sessionType: String,
        val exerciseCount: Int,
        val estimatedDurationMinutes: Int,
    ) : TodayMonthlyPlanCard

    /** Today is a rest day within the active plan — shown with no "start" action. */
    data object RestDay : TodayMonthlyPlanCard
}
