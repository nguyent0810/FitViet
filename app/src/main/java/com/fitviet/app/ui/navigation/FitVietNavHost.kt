package com.fitviet.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitviet.app.data.AppContainer
import com.fitviet.app.ui.calendar.WorkoutCalendarScreen
import com.fitviet.app.ui.calendar.WorkoutCalendarViewModel
import com.fitviet.app.ui.community.CommunityScreen
import com.fitviet.app.ui.community.CommunityViewModel
import com.fitviet.app.ui.dashboard.DashboardScreen
import com.fitviet.app.ui.dashboard.DashboardViewModel
import com.fitviet.app.ui.diary.DiaryScreen
import com.fitviet.app.ui.diary.DiaryViewModel
import com.fitviet.app.ui.exercise.ExerciseDetailScreen
import com.fitviet.app.ui.exercise.ExerciseDetailViewModel
import com.fitviet.app.ui.handbook.HandbookScreen
import com.fitviet.app.ui.handbook.HandbookViewModel
import com.fitviet.app.ui.nutrition.NutritionScreen
import com.fitviet.app.ui.nutrition.NutritionViewModel
import com.fitviet.app.ui.onboarding.GoalLevelScreen
import com.fitviet.app.ui.onboarding.OnboardingViewModel
import com.fitviet.app.ui.onboarding.SplitScreen
import com.fitviet.app.ui.profile.ProfileEditScreen
import com.fitviet.app.ui.profile.ProfileEditViewModel
import com.fitviet.app.ui.profile.ProfileScreen
import com.fitviet.app.ui.profile.ProfileViewModel
import com.fitviet.app.ui.programs.ProgramsListScreen
import com.fitviet.app.ui.programs.ProgramsViewModel
import com.fitviet.app.ui.programs.WeeklyScheduleScreen
import com.fitviet.app.ui.programs.WeeklyScheduleViewModel
import com.fitviet.app.ui.settings.SettingsScreen
import com.fitviet.app.ui.settings.SettingsViewModel
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.workout.WorkoutPreviewScreen
import com.fitviet.app.ui.workout.WorkoutPreviewViewModel
import com.fitviet.app.ui.workout.WorkoutScreen
import com.fitviet.app.ui.workout.WorkoutViewModel
import com.fitviet.app.util.LocaleController
import kotlinx.coroutines.launch

private const val ONBOARDING_GRAPH_ROUTE = "onboarding"

@Composable
fun FitVietNavHost(container: AppContainer) {
    // Keeps the real per-app locale (see LocaleController) in sync with the persisted 1i language
    // setting on every launch and every toggle. Idempotent, so re-firing after the locale-change
    // recreation this triggers is harmless. initialValue is null (not false) so a device that has
    // English persisted doesn't flash Vietnamese-then-English (and recreate twice) on cold start —
    // same "unknown yet, don't assume" pattern as onboardingCompleted below.
    val isEnglish by container.languageIsEnglish.collectAsStateWithLifecycle(initialValue = null)
    LaunchedEffect(isEnglish) { isEnglish?.let(LocaleController::apply) }

    // Onboarding completion decides the start destination, so hold off composing the graph
    // until the first read of settings resolves (null = unknown yet, not "not completed").
    val onboardingCompleted by container.onboardingRepository.isOnboardingCompleted()
        .collectAsStateWithLifecycle(initialValue = null)

    when (val completed = onboardingCompleted) {
        null -> Box(modifier = Modifier.fillMaxSize().background(BackgroundPage))
        else -> FitVietNavGraph(startAtOnboarding = !completed, container = container)
    }
}

@Composable
private fun FitVietNavGraph(startAtOnboarding: Boolean, container: AppContainer) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val startDestination = if (startAtOnboarding) ONBOARDING_GRAPH_ROUTE else FitVietDestination.Home.route

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
                    onFabClick = { navController.navigate(FitVietDestination.Workout.createRoute()) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            navigation(startDestination = FitVietDestination.OnboardingGoal.route, route = ONBOARDING_GRAPH_ROUTE) {
                composable(FitVietDestination.OnboardingGoal.route) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(ONBOARDING_GRAPH_ROUTE) }
                    val viewModel: OnboardingViewModel = viewModel(
                        parentEntry,
                        factory = OnboardingViewModel.Factory(container.onboardingRepository),
                    )
                    GoalLevelScreen(
                        viewModel = viewModel,
                        onContinue = { navController.navigate(FitVietDestination.OnboardingSplit.route) },
                    )
                }
                composable(FitVietDestination.OnboardingSplit.route) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(ONBOARDING_GRAPH_ROUTE) }
                    val viewModel: OnboardingViewModel = viewModel(
                        parentEntry,
                        factory = OnboardingViewModel.Factory(container.onboardingRepository),
                    )
                    val coroutineScope = rememberCoroutineScope()
                    SplitScreen(
                        viewModel = viewModel,
                        onContinue = {
                            // Await the write before navigating — popUpTo below clears the
                            // graph-scoped ViewModel, which would cancel an in-flight write.
                            coroutineScope.launch {
                                viewModel.completeOnboarding()
                                navController.navigate(FitVietDestination.Home.route) {
                                    popUpTo(ONBOARDING_GRAPH_ROUTE) { inclusive = true }
                                }
                            }
                        },
                    )
                }
            }
            composable(FitVietDestination.Home.route) {
                val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory(container.dashboardRepository))
                DashboardScreen(
                    viewModel = viewModel,
                    onStartWorkout = { navController.navigate(FitVietDestination.Workout.createRoute()) },
                    onBrowsePrograms = {
                        navController.navigate(FitVietDestination.Programs.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenDiary = { navController.navigate(FitVietDestination.Diary.route) },
                    onOpenProfile = { navController.navigate(FitVietDestination.Profile.route) },
                )
            }
            composable(FitVietDestination.Programs.route) {
                val viewModel: ProgramsViewModel = viewModel(
                    factory = ProgramsViewModel.Factory(container.programRepository, container.exerciseRepository, container.databaseReady),
                )
                ProgramsListScreen(
                    viewModel = viewModel,
                    onProgramClick = { program ->
                        navController.navigate(FitVietDestination.ProgramSchedule.createRoute(program.id))
                    },
                    onExerciseClick = { exercise ->
                        navController.navigate(FitVietDestination.ExerciseDetail.createRoute(exercise.id))
                    },
                )
            }
            composable(FitVietDestination.Diary.route) {
                val viewModel: DiaryViewModel = viewModel(factory = DiaryViewModel.Factory(container.diaryRepository))
                DiaryScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenCalendar = { navController.navigate(FitVietDestination.WorkoutCalendar.route) },
                )
            }
            composable(FitVietDestination.WorkoutCalendar.route) {
                val viewModel: WorkoutCalendarViewModel = viewModel(factory = WorkoutCalendarViewModel.Factory(container.diaryRepository))
                WorkoutCalendarScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(FitVietDestination.Profile.route) {
                val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory(container.profileRepository))
                ProfileScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onEditProfile = { navController.navigate(FitVietDestination.ProfileEdit.route) },
                    onOpenSettings = { navController.navigate(FitVietDestination.Settings.route) },
                )
            }
            composable(FitVietDestination.ProfileEdit.route) {
                val viewModel: ProfileEditViewModel = viewModel(factory = ProfileEditViewModel.Factory(container.profileRepository))
                ProfileEditScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(FitVietDestination.Settings.route) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(container.profileRepository, container.settingsRepository),
                )
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onResetComplete = {
                        // Explicit imperative nav back to onboarding — see SettingsViewModel's
                        // class doc for why the reactive onboardingCompleted check alone can't
                        // move an already-live NavController sitting deep in this back stack.
                        navController.navigate(ONBOARDING_GRAPH_ROUTE) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = FitVietDestination.ExerciseDetail.route,
                arguments = listOf(navArgument(FitVietDestination.ExerciseDetail.ARG_EXERCISE_ID) { type = NavType.LongType }),
            ) { backStackEntry ->
                val exerciseId = backStackEntry.arguments?.getLong(FitVietDestination.ExerciseDetail.ARG_EXERCISE_ID) ?: 0L
                val viewModel: ExerciseDetailViewModel = viewModel(
                    factory = ExerciseDetailViewModel.Factory(exerciseId, container.exerciseRepository),
                )
                ExerciseDetailScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(
                route = FitVietDestination.ProgramSchedule.route,
                arguments = listOf(navArgument(FitVietDestination.ProgramSchedule.ARG_PROGRAM_ID) { type = NavType.LongType }),
            ) { backStackEntry ->
                val programId = backStackEntry.arguments?.getLong(FitVietDestination.ProgramSchedule.ARG_PROGRAM_ID) ?: 0L
                val viewModel: WeeklyScheduleViewModel = viewModel(
                    factory = WeeklyScheduleViewModel.Factory(programId, container.programRepository),
                )
                WeeklyScheduleScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onStartToday = { navController.navigate(FitVietDestination.WorkoutPreview.createRoute(programId)) },
                )
            }
            composable(
                route = FitVietDestination.WorkoutPreview.route,
                arguments = listOf(navArgument(FitVietDestination.WorkoutPreview.ARG_PROGRAM_ID) { type = NavType.LongType }),
            ) { backStackEntry ->
                val programId = backStackEntry.arguments?.getLong(FitVietDestination.WorkoutPreview.ARG_PROGRAM_ID) ?: 0L
                val viewModel: WorkoutPreviewViewModel = viewModel(
                    factory = WorkoutPreviewViewModel.Factory(
                        programId,
                        container.programRepository,
                        container.exerciseRepository,
                        container.workoutRepository,
                        container.databaseReady,
                    ),
                )
                WorkoutPreviewScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onBeginWorkout = { navController.navigate(FitVietDestination.Workout.createRoute(programId)) },
                )
            }
            composable(
                route = FitVietDestination.Workout.route,
                arguments = listOf(navArgument(FitVietDestination.Workout.ARG_PROGRAM_ID) { type = NavType.LongType; defaultValue = -1L }),
            ) { backStackEntry ->
                val programId = backStackEntry.arguments?.getLong(FitVietDestination.Workout.ARG_PROGRAM_ID)?.takeIf { it != -1L }
                val viewModel: WorkoutViewModel = viewModel(
                    factory = WorkoutViewModel.Factory(
                        container.exerciseRepository,
                        container.workoutRepository,
                        container.programRepository,
                        container.databaseReady,
                        programId,
                    ),
                )
                WorkoutScreen(
                    viewModel = viewModel,
                    onFinishToHome = {
                        navController.navigate(FitVietDestination.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(FitVietDestination.Nutrition.route) {
                val viewModel: NutritionViewModel = viewModel(factory = NutritionViewModel.Factory(container.nutritionRepository))
                NutritionScreen(viewModel = viewModel)
            }
            composable(FitVietDestination.Community.route) {
                val viewModel: CommunityViewModel = viewModel(factory = CommunityViewModel.Factory(container.communityRepository))
                CommunityScreen(viewModel = viewModel)
            }
            composable(FitVietDestination.Handbook.route) {
                val viewModel: HandbookViewModel = viewModel(factory = HandbookViewModel.Factory(container.handbookRepository))
                HandbookScreen(
                    viewModel = viewModel,
                    onExerciseClick = { exercise ->
                        navController.navigate(FitVietDestination.ExerciseDetail.createRoute(exercise.id))
                    },
                )
            }
        }
    }
}
