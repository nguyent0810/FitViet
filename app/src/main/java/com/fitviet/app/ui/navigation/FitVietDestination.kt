package com.fitviet.app.ui.navigation

sealed class FitVietDestination(val route: String) {
    data object OnboardingGoal : FitVietDestination("onboarding/goal")
    data object OnboardingSplit : FitVietDestination("onboarding/split")
    data object Home : FitVietDestination("home")
    data object Programs : FitVietDestination("programs")
    data object Workout : FitVietDestination("workout")
    data object Nutrition : FitVietDestination("nutrition")
    data object Community : FitVietDestination("community")

    data object ProgramSchedule : FitVietDestination("programs/{programId}/schedule") {
        const val ARG_PROGRAM_ID = "programId"
        fun createRoute(programId: Long) = "programs/$programId/schedule"
    }
}

// Destinations that show the persistent bottom nav bar (matches 1b/1c/1g/1h in the design spec).
val BOTTOM_NAV_ROUTES = setOf(
    FitVietDestination.Home.route,
    FitVietDestination.Programs.route,
    FitVietDestination.Nutrition.route,
    FitVietDestination.Community.route,
)
