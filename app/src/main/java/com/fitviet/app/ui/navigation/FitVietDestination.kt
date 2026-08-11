package com.fitviet.app.ui.navigation

sealed class FitVietDestination(val route: String) {
    data object OnboardingGoal : FitVietDestination("onboarding/goal")
    data object OnboardingSplit : FitVietDestination("onboarding/split")
    data object Home : FitVietDestination("home")
    data object Programs : FitVietDestination("programs")
    data object Nutrition : FitVietDestination("nutrition")
    data object Community : FitVietDestination("community")

    /** [programId] is optional — absent for the free-standing entry points (bottom-nav FAB,
     * dashboard "Start workout"), present when started from [WorkoutPreview]'s "Begin workout",
     * in which case the session is built from that program's real schedule for today instead of
     * the generic duration-picker flow. */
    data object Workout : FitVietDestination("workout?programId={programId}&monthlyPlanDayId={monthlyPlanDayId}") {
        const val ARG_PROGRAM_ID = "programId"
        // "Hit & Run" (Gate 63+) — mutually exclusive with programId at every real call site (see
        // WorkoutViewModel's own doc on the two params), both optional so the bare "workout" route
        // (bottom-nav FAB, dashboard "Start workout" for the duration-picker flow) keeps working.
        const val ARG_MONTHLY_PLAN_DAY_ID = "monthlyPlanDayId"
        fun createRoute(programId: Long? = null, monthlyPlanDayId: Long? = null): String {
            val params = buildList {
                if (programId != null) add("programId=$programId")
                if (monthlyPlanDayId != null) add("monthlyPlanDayId=$monthlyPlanDayId")
            }
            return if (params.isEmpty()) "workout" else "workout?${params.joinToString("&")}"
        }
    }

    data object ProgramSchedule : FitVietDestination("programs/{programId}/schedule") {
        const val ARG_PROGRAM_ID = "programId"
        fun createRoute(programId: Long) = "programs/$programId/schedule"
    }

    /** The "day exercise list" screen (Gate 24) shown after tapping today's row on the Weekly
     * Schedule screen — always resolves to *today*, so it only needs [programId]. */
    data object WorkoutPreview : FitVietDestination("workout_preview/{programId}") {
        const val ARG_PROGRAM_ID = "programId"
        fun createRoute(programId: Long) = "workout_preview/$programId"
    }

    data object ExerciseDetail : FitVietDestination("exercises/{exerciseId}") {
        const val ARG_EXERCISE_ID = "exerciseId"
        fun createRoute(exerciseId: Long) = "exercises/$exerciseId"
    }

    data object Diary : FitVietDestination("diary")

    data object WorkoutCalendar : FitVietDestination("diary/calendar")

    data object Profile : FitVietDestination("profile")

    /** Feature #1 (Gate 35) — reached from Profile's header avatar tap or its "Chỉnh sửa hồ sơ ›" row. */
    data object ProfileEdit : FitVietDestination("profile/edit")

    /** Feature #6 (Gate 37) — reached from Profile's "Cài đặt ›" row. */
    data object Settings : FitVietDestination("settings")

    /** Feature #5 (Gate 38) — reached from Settings' "Nhắc nhở tập luyện" row. */
    data object Reminders : FitVietDestination("settings/reminders")

    /** Exercise library by difficulty level + a static food reference (Gate 25). */
    data object Handbook : FitVietDestination("handbook")

    /** "Hit & Run" (Gate 63+) — no args; reached from Dashboard's empty-state CTA and Programs'
     * header card, both of which just want "go generate a plan," not a specific plan/day. */
    data object QuickGenerate : FitVietDestination("quick_generate")

    /** "Hit & Run" (Gate 63+) Regenerate UI — the plan's simple day list (see the plan's scope
     * note: not a full calendar). No arg — always shows the one active plan. */
    data object MonthlyPlanDetail : FitVietDestination("monthly_plan")

    /** "Hit & Run" (Gate 63+) Regenerate UI — one day's exercises + swap/regenerate actions. */
    data object MonthlyPlanDayDetail : FitVietDestination("monthly_plan/day/{dayId}") {
        const val ARG_DAY_ID = "dayId"
        fun createRoute(dayId: Long) = "monthly_plan/day/$dayId"
    }
}

// Destinations that show the persistent bottom nav bar (matches 1b/1c/1g/1h in the design spec).
val BOTTOM_NAV_ROUTES = setOf(
    FitVietDestination.Home.route,
    FitVietDestination.Programs.route,
    FitVietDestination.Nutrition.route,
    FitVietDestination.Community.route,
    FitVietDestination.Handbook.route,
)
