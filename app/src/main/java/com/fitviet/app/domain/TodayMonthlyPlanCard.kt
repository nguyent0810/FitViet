package com.fitviet.app.domain

/**
 * Dashboard "Today" card content sourced from the active "Hit & Run" (Gate 63+) monthly plan.
 * [NoPlan] is the "no active plan" case itself (see its own doc) — redesign Gate 2b retired the
 * old hand-authored-program hero card that case used to fall back to; Dashboard now shows only its
 * empty-state "Tạo plan" CTA instead. Scoped strictly to *today*'s row — unlike [NextTraining]
 * (now used for a program's read-only preview lookahead instead), this doesn't look ahead to the
 * next non-rest day on a rest day; that lookahead isn't part of this phase's scope (see the "Hit &
 * Run" plan's Phase 5 note).
 *
 * "Hit & Run" redesign (Gate 1c) — expanded from 2 cases to a total 5-case classification (6 as of
 * the Phase 2 checkpoint's [Completed] addition) so
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

    /** Today is a training day that already has a genuinely finished
     * [com.fitviet.app.data.local.entity.WorkoutSessionEntity] (`completedAt` set) — deliberately
     * narrower than the "locked" predicate
     * [com.fitviet.app.data.local.dao.WorkoutSessionDao.countForMonthlyPlanDay] backs for the
     * regenerate-lock banner (which also counts merely-abandoned sessions, on purpose, so a
     * regenerate can't silently reinterpret their logged sets — a different question from "did the
     * user finish today"). Added at the Phase 2 checkpoint review — before this case existed,
     * [Training] kept being resolved after the user finished today's session (nothing about
     * date/rest-flag/session-type resolution changes on completion), so the Today card kept
     * showing its full "start" CTA on the same screen as the "Tuần này" card's already-filled-in ✓
     * for today, and tapping it inserted a second session row for the same day, double-counting
     * rolling-7-day stats. Never routes to a "start" CTA — an explicit "Làm lại" redo goes through
     * a separate `allowRestart` path in [com.fitviet.app.ui.workout.WorkoutViewModel] instead. */
    data class Completed(val dayId: Long, val sessionType: String) : TodayMonthlyPlanCard

    /** No active plan at all (never generated one, or [com.fitviet.app.data.local.entity
     * .SettingsEntity.activeMonthlyPlanId] is null for any other reason). */
    data object NoPlan : TodayMonthlyPlanCard

    /** An active plan exists but has no row for today — every day after the block's last date.
     * Distinct from [NoPlan] so the UI can say "your plan finished" rather than "you never made
     * one"; nothing currently clears [com.fitviet.app.data.local.entity.SettingsEntity
     * .activeMonthlyPlanId] on its own, so Kế hoạch may still list this plan as the active one. */
    data object PlanFinished : TodayMonthlyPlanCard
}
