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
     * in which case the session is built from that program's real schedule for [dayOfWeek]
     * (absent = today, matching this route's original today-only behavior) instead of the generic
     * duration-picker flow. */
    data object Workout : FitVietDestination("workout?programId={programId}&monthlyPlanDayId={monthlyPlanDayId}&dayOfWeek={dayOfWeek}") {
        const val ARG_PROGRAM_ID = "programId"
        // "Hit & Run" (Gate 63+) — mutually exclusive with programId at every real call site (see
        // WorkoutViewModel's own doc on the two params), both optional so the bare "workout" route
        // (bottom-nav FAB, dashboard "Start workout" for the duration-picker flow) keeps working.
        const val ARG_MONTHLY_PLAN_DAY_ID = "monthlyPlanDayId"
        // Gate E4 — only ever paired with [programId] (WorkoutPreview's "Begin workout" for a
        // specific tapped day, not just today); [java.time.DayOfWeek.getValue], 1-7.
        const val ARG_DAY_OF_WEEK = "dayOfWeek"
        fun createRoute(programId: Long? = null, monthlyPlanDayId: Long? = null, dayOfWeek: Int? = null): String {
            val params = buildList {
                if (programId != null) add("programId=$programId")
                if (monthlyPlanDayId != null) add("monthlyPlanDayId=$monthlyPlanDayId")
                if (dayOfWeek != null) add("dayOfWeek=$dayOfWeek")
            }
            return if (params.isEmpty()) "workout" else "workout?${params.joinToString("&")}"
        }
    }

    data object ProgramSchedule : FitVietDestination("programs/{programId}/schedule") {
        const val ARG_PROGRAM_ID = "programId"
        fun createRoute(programId: Long) = "programs/$programId/schedule"
    }

    /** The "day exercise list" screen (Gate 24), shown either for *today* (the FAB/dashboard
     * "start workout" entry points, and the Weekly Schedule's own inline today-only quick-start
     * text) or for a specific tapped day (Gate E4 — the Weekly Schedule's day rows now open this
     * screen for whichever day was tapped, not just today). [dayOfWeek] is the ISO day-of-week
     * value 1-7 ([java.time.DayOfWeek.getValue]); absent/null means "resolve today", matching this
     * screen's original today-only behavior. */
    data object WorkoutPreview : FitVietDestination("workout_preview/{programId}?dayOfWeek={dayOfWeek}") {
        const val ARG_PROGRAM_ID = "programId"
        const val ARG_DAY_OF_WEEK = "dayOfWeek"
        fun createRoute(programId: Long, dayOfWeek: Int? = null): String {
            val base = "workout_preview/$programId"
            return if (dayOfWeek != null) "$base?dayOfWeek=$dayOfWeek" else base
        }
    }

    data object ExerciseDetail : FitVietDestination("exercises/{exerciseId}") {
        const val ARG_EXERCISE_ID = "exerciseId"
        fun createRoute(exerciseId: Long) = "exercises/$exerciseId"
    }

    data object Diary : FitVietDestination("diary")

    data object WorkoutCalendar : FitVietDestination("diary/calendar")

    /** Gate D3 — a shareable weekly summary card, reusing Diary's own already-loaded stats. */
    data object DiaryWeeklyRecap : FitVietDestination("diary/weekly-recap")

    data object Profile : FitVietDestination("profile")

    /** Feature #1 (Gate 35) — reached from Profile's header avatar tap or its "Chỉnh sửa hồ sơ ›" row. */
    data object ProfileEdit : FitVietDestination("profile/edit")

    /** Feature #6 (Gate 37) — reached from Profile's "Cài đặt ›" row. */
    data object Settings : FitVietDestination("settings")

    /** Feature #5 (Gate 38) — reached from Settings' "Nhắc nhở tập luyện" row. */
    data object Reminders : FitVietDestination("settings/reminders")

    /** Exercise library grouped by muscle group + a static food reference grouped by ingredient
     * category (Gate 25, restructured Gate E5/E7). */
    data object Handbook : FitVietDestination("handbook")

    /** Gate E5 — exercises within one muscle group, reached by tapping a group card on the
     * Handbook's Exercises tab. */
    data object HandbookMuscleGroup : FitVietDestination("handbook/muscle-group/{muscleGroupCode}") {
        const val ARG_MUSCLE_GROUP_CODE = "muscleGroupCode"
        fun createRoute(muscleGroupCode: String) = "handbook/muscle-group/$muscleGroupCode"
    }

    /** Gate E7 — foods within one ingredient category, reached by tapping a category card on the
     * Handbook's Foods tab. [category] is the raw display string (Vietnamese, e.g. "Thịt"), used
     * as-is since food categories aren't a stable enum ([com.fitviet.app.data.local.entity
     * .FoodEntity.category] is free text) — matches how [MonthlyPlanDayDetail]/[NutritionRecipeDetail]
     * pass a raw id/string segment, just a string instead of a numeric id here. Uses
     * [android.net.Uri.encode]/[android.net.Uri.decode], not `java.net.URLEncoder`/`URLDecoder` —
     * Navigation Compose's own path-argument decoding is `Uri.decode` (percent-decoding only, no
     * `+`-to-space form-decoding step), so pairing it with `URLEncoder` (which escapes a space as
     * `+`) would round-trip correctly today only by accident and break on a future category
     * containing a literal `+` or `%`. */
    data object HandbookFoodCategory : FitVietDestination("handbook/food-category/{category}") {
        const val ARG_CATEGORY = "category"
        fun createRoute(category: String) = "handbook/food-category/${android.net.Uri.encode(category)}"
    }

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

    // ---- Nutrition module (real backend, Part C) — Nutrition (above) is the module's Home. ----

    data object NutritionDiscover : FitVietDestination("nutrition/discover")

    data object NutritionFoods : FitVietDestination("nutrition/foods")

    data object NutritionRecipeDetail : FitVietDestination("nutrition/recipe/{recipeId}") {
        const val ARG_RECIPE_ID = "recipeId"
        fun createRoute(recipeId: Long) = "nutrition/recipe/$recipeId"
    }

    data object NutritionTemplates : FitVietDestination("nutrition/templates")

    data object NutritionCreatePlan : FitVietDestination("nutrition/create-plan")

    /** The active plan's generated week — no arg, always shows the one active
     * [com.fitviet.app.data.local.entity.UserMealPlanEntity], same "one active plan" convention as
     * [MonthlyPlanDetail]. */
    data object NutritionPlan : FitVietDestination("nutrition/plan")

    data object NutritionPlanCalendar : FitVietDestination("nutrition/plan/calendar")
}

// Destinations that show the persistent bottom nav bar (matches 1b/1c/1g/1h in the design spec).
val BOTTOM_NAV_ROUTES = setOf(
    FitVietDestination.Home.route,
    FitVietDestination.Programs.route,
    FitVietDestination.Nutrition.route,
    FitVietDestination.Community.route,
    FitVietDestination.Handbook.route,
)
