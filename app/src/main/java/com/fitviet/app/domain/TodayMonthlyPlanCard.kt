package com.fitviet.app.domain

/**
 * Dashboard "Today" card content sourced from the active "Hit & Run" (Gate 63+) monthly plan —
 * null (at the [com.fitviet.app.data.repository.DashboardRepository] level) when there's no active
 * plan at all, in which case Dashboard falls back to its pre-existing hand-authored-program hero
 * card unchanged. Scoped strictly to *today*'s row — unlike the hand-authored-program path's
 * [NextTraining], this doesn't look ahead to the next non-rest day on a rest day; that lookahead
 * isn't part of this phase's scope (see the "Hit & Run" plan's Phase 5 note).
 *
 * "Hit & Run" redesign (Gate 1c) — expanded from 2 cases to a total 5-case classification so
 * [com.fitviet.app.data.repository.MonthlyPlanRepository.observeTodaySession] can be the single
 * shared resolution both [com.fitviet.app.data.repository.DashboardRepository] (Today card) and
 * [com.fitviet.app.ui.workout.WorkoutViewModel] (the bare `workout` route's single entry point)
 * call into, instead of [WorkoutViewModel] falling into a duration-picker screen the redesign
 * deletes. [Unavailable] specifically must never route to a "generate a new plan" action the way
 * [NoPlan] does — regenerating with the same exclusions/catalog reproduces the same empty day
 * (see [com.fitviet.app.domain.MonthlyPlanGenerationInput.excludedMuscleGroupCodes]'s own doc),
 * so that would be a silent infinite loop, not a fix.
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

    /** Today is a training day, but it resolved to zero exercises (every candidate muscle group
     * was excluded, or none resolved against the exercise catalog) — a real, generator-permitted
     * state, not an error. Never route this to Generate; there's nothing a regenerate would fix. */
    data class Unavailable(val dayId: Long, val sessionType: String) : TodayMonthlyPlanCard

    /** No active plan at all (never generated one, or [com.fitviet.app.data.local.entity
     * .SettingsEntity.activeMonthlyPlanId] is null for any other reason). */
    data object NoPlan : TodayMonthlyPlanCard

    /** An active plan exists but has no row for today — every day after the block's last date.
     * Distinct from [NoPlan] so the UI can say "your plan finished" rather than "you never made
     * one"; nothing currently clears [com.fitviet.app.data.local.entity.SettingsEntity
     * .activeMonthlyPlanId] on its own, so Kế hoạch may still list this plan as the active one. */
    data object PlanFinished : TodayMonthlyPlanCard
}
