package com.fitviet.app.domain

/**
 * Stable classification codes for the "Hit & Run" monthly plan generator, same pattern as
 * [MuscleGroup]/[MovementType]/[ExerciseDifficulty] — plain enum `.name` stored as a raw String
 * column (no Room TypeConverter), kept in this Room/Compose-free package.
 */
enum class TrainingGoal { STRENGTH, HYPERTROPHY, GENERAL_FITNESS }

/** The 5 built-in day-assignment templates [MonthlyPlanGenerator] knows how to lay out, plus
 * [CUSTOM] for a user-authored [com.fitviet.app.data.local.entity.SplitTemplateEntity]. */
enum class SplitTemplate { PPL, UPPER_LOWER, FULL_BODY, BRO_SPLIT, PHUL, CUSTOM }

/** Redesign Gate 2a — the app's own "no split question asked" default, per the mock's own rule:
 * "Kiểu chia buổi app tự chọn theo số buổi (2–3=Full body, 4=Upper–Lower, 5+=PPL) hiển thị dạng
 * text, đổi được sau." Used by onboarding's first-ever generate (never asks about split at all)
 * and, from Gate 3b on, the Quick Generate sheet's own auto-picked split display before the user
 * overrides it — one shared rule so the two entry points can never silently disagree. */
fun defaultSplitTemplateFor(daysPerWeek: Int): SplitTemplate = when {
    daysPerWeek <= 3 -> SplitTemplate.FULL_BODY
    daysPerWeek == 4 -> SplitTemplate.UPPER_LOWER
    else -> SplitTemplate.PPL
}

/** One block's week-to-week progression stage — see [MonthlyPlanGenerator]'s phase table (a
 * 4-week block is BASE,BUILD,PEAK,DELOAD; a 5-week block inserts one extra BUILD). */
enum class PlanPhase { BASE, BUILD, PEAK, DELOAD }

/** Where an exercise sits in a session's priority order — drives both selection order and the
 * `selectionReasonCode` trace on [com.fitviet.app.data.local.entity.MonthlyPlanExerciseEntity]. */
enum class ExerciseTier { MAIN_COMPOUND, SECONDARY_COMPOUND, TARGETED_ACCESSORY, ISOLATION, FINISHER }

enum class MonthlyPlanStatus { ACTIVE, SUPERSEDED, ARCHIVED }

/** [SCHEDULED] is the only status a day starts in. [MISSED] is a detection marker set by
 * [MissedWorkoutDetector] on Dashboard load — never a data mutation on its own. [RESCHEDULED]
 * covers both "pushed to today" and "manually swapped." [COMPLETED] is derived (a linked
 * [com.fitviet.app.data.local.entity.WorkoutSessionEntity] exists), not set by this enum's own
 * writer — kept as an explicit value so a completed day's UI state doesn't need a join to render. */
enum class MonthlyPlanDayStatus { SCHEDULED, COMPLETED, SKIPPED, MISSED, RESCHEDULED }
