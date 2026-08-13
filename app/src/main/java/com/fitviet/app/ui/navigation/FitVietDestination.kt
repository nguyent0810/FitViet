package com.fitviet.app.ui.navigation

sealed class FitVietDestination(val route: String) {
    /** Redesign Gate 2a — the old 2-step OnboardingGoal/OnboardingSplit pair collapsed to this one
     * screen (see [com.fitviet.app.ui.onboarding.OnboardingScreen]). */
    data object Onboarding : FitVietDestination("onboarding/main")
    data object Home : FitVietDestination("home")
    data object Programs : FitVietDestination("programs")
    data object Nutrition : FitVietDestination("nutrition")
    data object Community : FitVietDestination("community")

    /** [monthlyPlanDayId] is optional — absent for the free-standing entry points (bottom-nav FAB,
     * the no-arg "resolve today" path), present when started from a specific monthly-plan day
     * (the Today card, a regenerate/preview flow). Redesign Gate 2b removed this route's other
     * former optional arg (`programId`) — every program-triggered session now goes through
     * `MonthlyPlanRepository.generate()` instead (see `ProgramsViewModel.generateFromProgram`),
     * so there's no longer a program-day session this route can be asked to start directly. */
    data object Workout : FitVietDestination("workout?monthlyPlanDayId={monthlyPlanDayId}") {
        const val ARG_MONTHLY_PLAN_DAY_ID = "monthlyPlanDayId"
        fun createRoute(monthlyPlanDayId: Long? = null): String =
            if (monthlyPlanDayId != null) "workout?monthlyPlanDayId=$monthlyPlanDayId" else "workout"
    }

    /** The "Xem trước" (preview) screen for a sample program (Gate 24; redesign Gate 2b demoted
     * it to this one optional, read-only link — the Weekly Schedule screen it used to also be
     * reachable from, with a per-day tap, is retired). Always resolves the program's own nearest
     * upcoming training day — see `WorkoutPreviewViewModel`'s own doc for why a specific
     * `dayOfWeek` arg is no longer meaningful once there's no day-list left to tap into one from. */
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
// Redesign Phase 3b — Handbook dropped: it's no longer a top-level tab (see BottomNavBar's own
// NAV_ITEMS_RIGHT doc), reached instead via the Kế hoạch tab's "Thư viện bài tập" row, same
// drill-in treatment MonthlyPlanDetail/ExerciseDetail already get (neither shows the bottom bar).
val BOTTOM_NAV_ROUTES = setOf(
    FitVietDestination.Home.route,
    FitVietDestination.Programs.route,
    FitVietDestination.Nutrition.route,
    FitVietDestination.Community.route,
)
