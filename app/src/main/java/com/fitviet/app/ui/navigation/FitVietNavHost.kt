package com.fitviet.app.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.fitviet.app.ui.diary.WeeklyRecapScreen
import com.fitviet.app.ui.diary.WeeklyRecapViewModel
import com.fitviet.app.ui.exercise.ExerciseDetailScreen
import com.fitviet.app.ui.exercise.ExerciseDetailViewModel
import com.fitviet.app.ui.handbook.HandbookScreen
import com.fitviet.app.ui.handbook.HandbookViewModel
import com.fitviet.app.ui.monthlyplan.MonthlyPlanDayDetailScreen
import com.fitviet.app.ui.monthlyplan.MonthlyPlanDayDetailViewModel
import com.fitviet.app.ui.monthlyplan.MonthlyPlanDetailScreen
import com.fitviet.app.ui.monthlyplan.MonthlyPlanDetailViewModel
import com.fitviet.app.ui.nutrition.NutritionScreen
import com.fitviet.app.ui.nutrition.NutritionViewModel
import com.fitviet.app.ui.nutrition.discover.DiscoverScreen
import com.fitviet.app.ui.nutrition.discover.DiscoverViewModel
import com.fitviet.app.ui.nutrition.foods.FoodsScreen
import com.fitviet.app.ui.nutrition.foods.FoodsViewModel
import com.fitviet.app.ui.nutrition.calendar.CalendarScreen
import com.fitviet.app.ui.nutrition.calendar.CalendarViewModel
import com.fitviet.app.ui.nutrition.createplan.CreatePlanScreen
import com.fitviet.app.ui.nutrition.createplan.CreatePlanViewModel
import com.fitviet.app.ui.nutrition.plan.PlanScreen
import com.fitviet.app.ui.nutrition.plan.PlanViewModel
import com.fitviet.app.ui.nutrition.recipedetail.RecipeDetailScreen
import com.fitviet.app.ui.nutrition.recipedetail.RecipeDetailViewModel
import com.fitviet.app.ui.nutrition.templates.TemplatesScreen
import com.fitviet.app.ui.nutrition.templates.TemplatesViewModel
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
import com.fitviet.app.ui.quickgenerate.QuickGenerateScreen
import com.fitviet.app.ui.quickgenerate.QuickGenerateViewModel
import com.fitviet.app.ui.reminders.RemindersScreen
import com.fitviet.app.ui.reminders.RemindersViewModel
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun FitVietNavGraph(startAtOnboarding: Boolean, container: AppContainer) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val startDestination = if (startAtOnboarding) ONBOARDING_GRAPH_ROUTE else FitVietDestination.Home.route
    // "Hit & Run" (Gate 63+) Phase 9 — read once here, at the graph level, so every "start a
    // program-day session" call site below applies the same toggle consistently.
    val skipWorkoutPreview by container.skipWorkoutPreview.collectAsStateWithLifecycle(initialValue = false)

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
        // Gate D1 — a snappier app-wide screen-to-screen crossfade (220/180ms; nav-compose 2.8.5's
        // own NavHost default is already a 700ms fadeIn/fadeOut, not an abrupt cut, but the brief
        // called for shorter transitions) plus a SharedTransitionLayout scope for the single
        // Discover->RecipeDetail card-title shared element below; wrapping the whole graph here
        // means every OTHER composable() block in this file needed zero changes.
        SharedTransitionLayout {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(animationSpec = tween(220)) },
                exitTransition = { fadeOut(animationSpec = tween(180)) },
                popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                popExitTransition = { fadeOut(animationSpec = tween(180)) },
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
                val viewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.Factory(container.dashboardRepository, container.monthlyPlanRepository),
                )
                DashboardScreen(
                    viewModel = viewModel,
                    // Routes through the same "day exercise list" preview (Gate 24) the
                    // Programs-tab entry point already uses, instead of jumping straight into
                    // logging — the hero card already names today's program day, so the user
                    // should see what it contains before committing, exactly like every other
                    // entry into a program-day session. Unless "Hit & Run" (Gate 63+) Phase 9's
                    // skip-preview toggle is on, in which case this goes straight to Workout.
                    onStartWorkout = { programId ->
                        if (skipWorkoutPreview) {
                            navController.navigate(FitVietDestination.Workout.createRoute(programId))
                        } else {
                            navController.navigate(FitVietDestination.WorkoutPreview.createRoute(programId))
                        }
                    },
                    onViewSchedule = { programId ->
                        navController.navigate(FitVietDestination.ProgramSchedule.createRoute(programId))
                    },
                    onBrowsePrograms = {
                        navController.navigate(FitVietDestination.Programs.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenDiary = { navController.navigate(FitVietDestination.Diary.route) },
                    onOpenProfile = { navController.navigate(FitVietDestination.Profile.route) },
                    // "Hit & Run" (Gate 63+) — goes straight to the live session; see
                    // DashboardScreen's own doc comment on this param for why it skips WorkoutPreview
                    // unconditionally (Phase 9's skipWorkoutPreview toggle above doesn't apply here —
                    // there's no monthly-plan-aware preview path to skip in the first place yet).
                    onStartMonthlyPlanDay = { dayId ->
                        navController.navigate(FitVietDestination.Workout.createRoute(monthlyPlanDayId = dayId))
                    },
                    onGenerateMonthlyPlan = { navController.navigate(FitVietDestination.QuickGenerate.route) },
                    onViewMonthlyPlan = { navController.navigate(FitVietDestination.MonthlyPlanDetail.route) },
                )
            }
            composable(FitVietDestination.QuickGenerate.route) {
                val quickGenerateCoroutineScope = rememberCoroutineScope()
                val viewModel: QuickGenerateViewModel = viewModel(
                    factory = QuickGenerateViewModel.Factory(container.onboardingRepository, container.monthlyPlanRepository),
                )
                QuickGenerateScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    // Await the write before navigating — same reasoning as onboarding's
                    // completeOnboarding() above: popping this screen off the back stack would
                    // otherwise risk cancelling the generation transaction mid-flight.
                    onGenerateClick = {
                        quickGenerateCoroutineScope.launch {
                            if (viewModel.generate()) {
                                navController.navigate(FitVietDestination.Home.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    },
                )
            }
            composable(FitVietDestination.MonthlyPlanDetail.route) {
                val viewModel: MonthlyPlanDetailViewModel = viewModel(
                    factory = MonthlyPlanDetailViewModel.Factory(container.monthlyPlanRepository),
                )
                MonthlyPlanDetailScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onDayClick = { dayId -> navController.navigate(FitVietDestination.MonthlyPlanDayDetail.createRoute(dayId)) },
                )
            }
            composable(
                route = FitVietDestination.MonthlyPlanDayDetail.route,
                arguments = listOf(navArgument(FitVietDestination.MonthlyPlanDayDetail.ARG_DAY_ID) { type = NavType.LongType }),
            ) { backStackEntry ->
                val dayId = backStackEntry.arguments?.getLong(FitVietDestination.MonthlyPlanDayDetail.ARG_DAY_ID) ?: 0L
                val viewModel: MonthlyPlanDayDetailViewModel = viewModel(
                    factory = MonthlyPlanDayDetailViewModel.Factory(dayId, container.monthlyPlanRepository, container.exerciseRepository),
                )
                MonthlyPlanDayDetailScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
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
                    onGenerateMonthlyPlan = { navController.navigate(FitVietDestination.QuickGenerate.route) },
                )
            }
            composable(FitVietDestination.Diary.route) {
                val viewModel: DiaryViewModel = viewModel(factory = DiaryViewModel.Factory(container.diaryRepository))
                DiaryScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenCalendar = { navController.navigate(FitVietDestination.WorkoutCalendar.route) },
                    onOpenWeeklyRecap = { navController.navigate(FitVietDestination.DiaryWeeklyRecap.route) },
                )
            }
            composable(FitVietDestination.WorkoutCalendar.route) {
                val viewModel: WorkoutCalendarViewModel = viewModel(factory = WorkoutCalendarViewModel.Factory(container.diaryRepository))
                WorkoutCalendarScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(FitVietDestination.DiaryWeeklyRecap.route) {
                val viewModel: WeeklyRecapViewModel = viewModel(factory = WeeklyRecapViewModel.Factory(container.diaryRepository))
                WeeklyRecapScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
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
                    factory = SettingsViewModel.Factory(container.profileRepository, container.settingsRepository, container.remindersRepository),
                )
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenProfileEdit = { navController.navigate(FitVietDestination.ProfileEdit.route) },
                    onResetComplete = {
                        // Explicit imperative nav back to onboarding — see SettingsViewModel's
                        // class doc for why the reactive onboardingCompleted check alone can't
                        // move an already-live NavController sitting deep in this back stack.
                        navController.navigate(ONBOARDING_GRAPH_ROUTE) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenReminders = { navController.navigate(FitVietDestination.Reminders.route) },
                )
            }
            composable(FitVietDestination.Reminders.route) {
                val viewModel: RemindersViewModel = viewModel(factory = RemindersViewModel.Factory(container.remindersRepository))
                RemindersScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(
                route = FitVietDestination.ExerciseDetail.route,
                arguments = listOf(navArgument(FitVietDestination.ExerciseDetail.ARG_EXERCISE_ID) { type = NavType.LongType }),
            ) { backStackEntry ->
                val exerciseId = backStackEntry.arguments?.getLong(FitVietDestination.ExerciseDetail.ARG_EXERCISE_ID) ?: 0L
                val viewModel: ExerciseDetailViewModel = viewModel(
                    factory = ExerciseDetailViewModel.Factory(exerciseId, container.exerciseRepository, container.workoutRepository),
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
                    // Same "Hit & Run" (Gate 63+) Phase 9 skip-preview toggle as Dashboard's
                    // onStartWorkout above.
                    onStartToday = {
                        if (skipWorkoutPreview) {
                            navController.navigate(FitVietDestination.Workout.createRoute(programId))
                        } else {
                            navController.navigate(FitVietDestination.WorkoutPreview.createRoute(programId))
                        }
                    },
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
                arguments = listOf(
                    navArgument(FitVietDestination.Workout.ARG_PROGRAM_ID) { type = NavType.LongType; defaultValue = -1L },
                    navArgument(FitVietDestination.Workout.ARG_MONTHLY_PLAN_DAY_ID) { type = NavType.LongType; defaultValue = -1L },
                ),
            ) { backStackEntry ->
                val programId = backStackEntry.arguments?.getLong(FitVietDestination.Workout.ARG_PROGRAM_ID)?.takeIf { it != -1L }
                val monthlyPlanDayId = backStackEntry.arguments?.getLong(FitVietDestination.Workout.ARG_MONTHLY_PLAN_DAY_ID)?.takeIf { it != -1L }
                val viewModel: WorkoutViewModel = viewModel(
                    factory = WorkoutViewModel.Factory(
                        container.exerciseRepository,
                        container.workoutRepository,
                        container.programRepository,
                        container.communityRepository,
                        container.monthlyPlanRepository,
                        container.databaseReady,
                        programId,
                        monthlyPlanDayId,
                    ),
                )
                WorkoutScreen(
                    viewModel = viewModel,
                    // A plain popBackStack(Home.route) rather than the navigate()+popUpTo+
                    // launchSingleTop+restoreState dance used by BottomNavBar's tab switches:
                    // Home is always already sitting lower in the back stack by the time a
                    // session reaches SessionFinished (reached either directly from the FAB, or
                    // via Programs -> WeeklySchedule -> WorkoutPreview -> Workout), so this only
                    // needs to pop back down to it, not re-navigate to a possibly-saved instance
                    // of it. Confirmed via manual testing that the popUpTo/restoreState combo
                    // silently failed to navigate here even though the identical pattern works
                    // for BottomNavBar; a direct popBackStack is what the system Back button
                    // already does successfully from this same screen.
                    onFinishToHome = {
                        val popped = navController.popBackStack(FitVietDestination.Home.route, inclusive = false)
                        if (!popped) {
                            // Home wasn't found in the back stack (shouldn't happen — it's the
                            // graph's own start destination — but fall back rather than strand
                            // the user on this screen).
                            navController.navigate(FitVietDestination.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
            composable(FitVietDestination.Nutrition.route) {
                val viewModel: NutritionViewModel = viewModel(
                    factory = NutritionViewModel.Factory(container.nutritionRepository, container.mealPlanRepository, container.recipeRepository),
                )
                NutritionScreen(
                    viewModel = viewModel,
                    onOpenDiscover = { navController.navigate(FitVietDestination.NutritionDiscover.route) },
                    onOpenFoods = { navController.navigate(FitVietDestination.NutritionFoods.route) },
                    onOpenTemplates = { navController.navigate(FitVietDestination.NutritionTemplates.route) },
                    onOpenCreatePlan = { navController.navigate(FitVietDestination.NutritionCreatePlan.route) },
                    onOpenPlan = { navController.navigate(FitVietDestination.NutritionPlan.route) },
                )
            }
            composable(FitVietDestination.NutritionFoods.route) {
                val viewModel: FoodsViewModel = viewModel(factory = FoodsViewModel.Factory(container.recipeRepository))
                FoodsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(FitVietDestination.NutritionDiscover.route) {
                val viewModel: DiscoverViewModel = viewModel(factory = DiscoverViewModel.Factory(container.recipeRepository))
                DiscoverScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenRecipe = { recipeId -> navController.navigate(FitVietDestination.NutritionRecipeDetail.createRoute(recipeId)) },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                )
            }
            composable(
                route = FitVietDestination.NutritionRecipeDetail.route,
                arguments = listOf(navArgument(FitVietDestination.NutritionRecipeDetail.ARG_RECIPE_ID) { type = NavType.LongType }),
            ) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getLong(FitVietDestination.NutritionRecipeDetail.ARG_RECIPE_ID) ?: 0L
                val viewModel: RecipeDetailViewModel = viewModel(
                    factory = RecipeDetailViewModel.Factory(recipeId, container.recipeRepository),
                )
                RecipeDetailScreen(
                    viewModel = viewModel,
                    recipeId = recipeId,
                    onBack = { navController.popBackStack() },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                )
            }
            composable(FitVietDestination.NutritionTemplates.route) {
                val viewModel: TemplatesViewModel = viewModel(
                    factory = TemplatesViewModel.Factory(container.mealPlanRepository, container.database.settingsDao()),
                )
                TemplatesScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    // Gate C7 (Generated plan screen) lands next — same "navigate to the route
                    // before its composable is registered" incremental sequencing DiscoverScreen's
                    // onOpenRecipe used ahead of Gate C4, not an oversight.
                    onPlanGenerated = {
                        navController.navigate(FitVietDestination.NutritionPlan.route) {
                            popUpTo(FitVietDestination.Nutrition.route)
                        }
                    },
                )
            }
            composable(FitVietDestination.NutritionCreatePlan.route) {
                val viewModel: CreatePlanViewModel = viewModel(
                    factory = CreatePlanViewModel.Factory(
                        container.mealPlanRepository,
                        container.database.settingsDao(),
                        container.database.measurementDao(),
                    ),
                )
                CreatePlanScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    // Same incremental-sequencing precedent as Discover->RecipeDetail and
                    // Templates->Plan: nutrition/plan's own composable lands in Gate C7.
                    onPlanGenerated = {
                        navController.navigate(FitVietDestination.NutritionPlan.route) {
                            popUpTo(FitVietDestination.Nutrition.route)
                        }
                    },
                )
            }
            composable(FitVietDestination.NutritionPlan.route) {
                val viewModel: PlanViewModel = viewModel(
                    factory = PlanViewModel.Factory(container.mealPlanRepository, container.recipeRepository),
                )
                PlanScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenCalendar = { navController.navigate(FitVietDestination.NutritionPlanCalendar.route) },
                )
            }
            composable(FitVietDestination.NutritionPlanCalendar.route) {
                val viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory(container.mealPlanRepository))
                CalendarScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
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
}
