package com.fitviet.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fitviet.app.ui.common.PlaceholderScreen
import com.fitviet.app.ui.onboarding.GoalLevelScreen
import com.fitviet.app.ui.onboarding.OnboardingViewModel
import com.fitviet.app.ui.onboarding.SplitScreen

private const val ONBOARDING_GRAPH_ROUTE = "onboarding"

@Composable
fun FitVietNavHost() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in BOTTOM_NAV_ROUTES) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onFabClick = { navController.navigate(FitVietDestination.Workout.route) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ONBOARDING_GRAPH_ROUTE,
            modifier = Modifier.padding(innerPadding),
        ) {
            navigation(startDestination = FitVietDestination.OnboardingGoal.route, route = ONBOARDING_GRAPH_ROUTE) {
                composable(FitVietDestination.OnboardingGoal.route) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(ONBOARDING_GRAPH_ROUTE) }
                    val viewModel: OnboardingViewModel = viewModel(parentEntry)
                    GoalLevelScreen(
                        viewModel = viewModel,
                        onContinue = { navController.navigate(FitVietDestination.OnboardingSplit.route) },
                    )
                }
                composable(FitVietDestination.OnboardingSplit.route) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(ONBOARDING_GRAPH_ROUTE) }
                    val viewModel: OnboardingViewModel = viewModel(parentEntry)
                    SplitScreen(
                        viewModel = viewModel,
                        onContinue = {
                            navController.navigate(FitVietDestination.Home.route) {
                                popUpTo(ONBOARDING_GRAPH_ROUTE) { inclusive = true }
                            }
                        },
                    )
                }
            }
            composable(FitVietDestination.Home.route) { PlaceholderScreen(title = "Trang chủ") }
            composable(FitVietDestination.Programs.route) { PlaceholderScreen(title = "Giáo án") }
            composable(FitVietDestination.Workout.route) { PlaceholderScreen(title = "Tập") }
            composable(FitVietDestination.Nutrition.route) { PlaceholderScreen(title = "Dinh dưỡng") }
            composable(FitVietDestination.Community.route) { PlaceholderScreen(title = "Cộng đồng") }
        }
    }
}
