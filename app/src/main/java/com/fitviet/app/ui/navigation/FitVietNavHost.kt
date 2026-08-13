package com.fitviet.app.ui.navigation

import android.util.Log
import android.widget.Toast
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitviet.app.R
import com.fitviet.app.data.AppContainer
import com.fitviet.app.domain.MuscleGroup
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
import com.fitviet.app.ui.handbook.HandbookFoodCategoryScreen
import com.fitviet.app.ui.handbook.HandbookFoodCategoryViewModel
import com.fitviet.app.ui.handbook.HandbookMuscleGroupScreen
import com.fitviet.app.ui.handbook.HandbookMuscleGroupViewModel
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
import com.fitviet.app.ui.onboarding.OnboardingScreen
import com.fitviet.app.ui.onboarding.OnboardingViewModel
import com.fitviet.app.ui.profile.ProfileEditScreen
import com.fitviet.app.ui.profile.ProfileEditViewModel
import com.fitviet.app.ui.profile.ProfileScreen
import com.fitviet.app.ui.profile.ProfileViewModel
import com.fitviet.app.ui.programs.ProgramsListScreen
import com.fitviet.app.ui.programs.ProgramsViewModel
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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
    val startDestination = if (startAtOnboarding) FitVietDestination.Onboarding.route else FitVietDestination.Home.route

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
            composable(FitVietDestination.Onboarding.route) {
                val viewModel: OnboardingViewModel = viewModel(
                    factory = OnboardingViewModel.Factory(container.onboardingRepository, container.monthlyPlanRepository),
                )
                val coroutineScope = rememberCoroutineScope()
                val onboardingContext = LocalContext.current
                OnboardingScreen(
                    viewModel = viewModel,
                    // "Tạo plan & vào tập →" — await the write+generate before navigating (popUpTo
                    // below clears this screen's own ViewModel, which would cancel an in-flight
                    // write), then land straight in the live workout session for today, the exact
                    // same no-arg entry point the bottom-nav FAB uses — Gate 1d-i's today-anchored
                    // offsets guarantee the plan just generated always has a trainable day today.
                    onSubmit = {
                        coroutineScope.launch {
                            try {
                                if (viewModel.submit()) {
                                    navController.navigate(FitVietDestination.Home.route) {
                                        popUpTo(FitVietDestination.Onboarding.route) { inclusive = true }
                                    }
                                    navController.navigate(FitVietDestination.Workout.createRoute())
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Log.e("Onboarding", "submit() failed", e)
                                Toast.makeText(
                                    onboardingContext,
                                    onboardingContext.getString(R.string.quick_generate_error, e.message ?: e.javaClass.simpleName),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    },
                )
            }
            composable(FitVietDestination.Home.route) {
                val viewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.Factory(container.dashboardRepository, container.monthlyPlanRepository),
                )
                // Redesign Gate 3c — GenerateSheet's own state holder, scoped to this route (see
                // GenerateSheet.kt's own doc for why it's hosted per-screen rather than behind a
                // shared nav destination the way the retired QuickGenerateScreen was).
                val dashboardQuickGenerateViewModel: QuickGenerateViewModel = viewModel(
                    factory = QuickGenerateViewModel.Factory(container.onboardingRepository, container.monthlyPlanRepository),
                )
                val dashboardCoroutineScope = rememberCoroutineScope()
                val dashboardContext = LocalContext.current
                DashboardScreen(
                    viewModel = viewModel,
                    onOpenProfile = { navController.navigate(FitVietDestination.Profile.route) },
                    onOpenDiary = { navController.navigate(FitVietDestination.Diary.route) },
                    // "Hit & Run" (Gate 63+) — goes straight to the live session; see
                    // DashboardScreen's own doc comment on this param for why it skips WorkoutPreview
                    // unconditionally.
                    onStartMonthlyPlanDay = { dayId ->
                        navController.navigate(FitVietDestination.Workout.createRoute(monthlyPlanDayId = dayId))
                    },
                    quickGenerateViewModel = dashboardQuickGenerateViewModel,
                    // Same await-the-write-before-navigating reasoning the retired QuickGenerateScreen
                    // call site used — a user-reported "tapping the button does nothing" bug once
                    // traced to this exact call silently swallowing whatever
                    // monthlyPlanRepository.generate() throws, so this logs and Toasts on failure
                    // rather than leaving the UI looking unresponsive with zero feedback.
                    onGenerateConfirmed = {
                        dashboardCoroutineScope.launch {
                            try {
                                if (dashboardQuickGenerateViewModel.generate()) {
                                    navController.navigate(FitVietDestination.Home.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Log.e("Dashboard", "monthlyPlanRepository.generate() failed", e)
                                Toast.makeText(
                                    dashboardContext,
                                    dashboardContext.getString(R.string.quick_generate_error, e.message ?: e.javaClass.simpleName),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    },
                    onViewMonthlyPlan = { navController.navigate(FitVietDestination.MonthlyPlanDetail.route) },
                    // Redesign Gate 2c — Today card's "Chi tiết" link reuses the Regenerate UI's
                    // own day-detail screen rather than a new monthly-plan-aware preview.
                    onPreviewToday = { dayId -> navController.navigate(FitVietDestination.MonthlyPlanDayDetail.createRoute(dayId)) },
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
                val programsCoroutineScope = rememberCoroutineScope()
                val programsContext = LocalContext.current
                val viewModel: ProgramsViewModel = viewModel(
                    factory = ProgramsViewModel.Factory(
                        container.programRepository,
                        container.monthlyPlanRepository,
                        container.onboardingRepository,
                        container.exerciseRepository,
                        container.databaseReady,
                    ),
                )
                // Redesign Gate 3c — GenerateSheet's own state holder, scoped to this route — a
                // separate instance from Dashboard's own, per-screen hosting (see GenerateSheet.kt's
                // own doc).
                val programsQuickGenerateViewModel: QuickGenerateViewModel = viewModel(
                    factory = QuickGenerateViewModel.Factory(container.onboardingRepository, container.monthlyPlanRepository),
                )
                ProgramsListScreen(
                    viewModel = viewModel,
                    // Same await-then-navigate/try-catch-Toast pattern as onboarding's `onSubmit`/
                    // Quick Generate's `onGenerateClick` — straight into the live session on
                    // success, matching the redesign's "one tap, into training" ethos for a
                    // deliberate program pick (as opposed to Quick Generate's own customize-first
                    // sheet, which returns to Home instead).
                    onGenerateFromProgram = { program ->
                        programsCoroutineScope.launch {
                            try {
                                if (viewModel.generateFromProgram(program)) {
                                    navController.navigate(FitVietDestination.Home.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    navController.navigate(FitVietDestination.Workout.createRoute())
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Log.e("Programs", "generateFromProgram() failed", e)
                                Toast.makeText(
                                    programsContext,
                                    programsContext.getString(R.string.quick_generate_error, e.message ?: e.javaClass.simpleName),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    },
                    onPreviewProgram = { program ->
                        navController.navigate(FitVietDestination.WorkoutPreview.createRoute(program.id))
                    },
                    onExerciseClick = { exercise ->
                        navController.navigate(FitVietDestination.ExerciseDetail.createRoute(exercise.id))
                    },
                    quickGenerateViewModel = programsQuickGenerateViewModel,
                    // Same await-the-write-before-navigating/error-Toast pattern as
                    // onGenerateFromProgram above and Dashboard's own onGenerateConfirmed.
                    onGenerateConfirmed = {
                        programsCoroutineScope.launch {
                            try {
                                if (programsQuickGenerateViewModel.generate()) {
                                    navController.navigate(FitVietDestination.Home.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Log.e("Programs", "monthlyPlanRepository.generate() failed", e)
                                Toast.makeText(
                                    programsContext,
                                    programsContext.getString(R.string.quick_generate_error, e.message ?: e.javaClass.simpleName),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    },
                    // Redesign Phase 3a — the plan progress/today card cluster's own nav targets.
                    onStartMonthlyPlanDay = { dayId ->
                        navController.navigate(FitVietDestination.Workout.createRoute(monthlyPlanDayId = dayId))
                    },
                    onOpenMonthlyPlanDay = { dayId ->
                        navController.navigate(FitVietDestination.MonthlyPlanDayDetail.createRoute(dayId))
                    },
                    onViewFullMonth = { navController.navigate(FitVietDestination.MonthlyPlanDetail.route) },
                    // Redesign Phase 3b — now the sole entry point: Handbook's own bottom-nav tab
                    // is gone (see BottomNavBar's own doc), so this is the only way in.
                    onOpenExerciseLibrary = { navController.navigate(FitVietDestination.Handbook.route) },
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
                        navController.navigate(FitVietDestination.Onboarding.route) {
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
                route = FitVietDestination.WorkoutPreview.route,
                arguments = listOf(
                    navArgument(FitVietDestination.WorkoutPreview.ARG_PROGRAM_ID) { type = NavType.LongType },
                ),
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
                    // Gate E4 — lets a user tap into an exercise's "cách tập" (how-to) detail
                    // straight from the preview, instead of it being a dead-end.
                    onExerciseClick = { exerciseId -> navController.navigate(FitVietDestination.ExerciseDetail.createRoute(exerciseId)) },
                )
            }
            composable(
                route = FitVietDestination.Workout.route,
                arguments = listOf(
                    navArgument(FitVietDestination.Workout.ARG_MONTHLY_PLAN_DAY_ID) { type = NavType.LongType; defaultValue = -1L },
                ),
            ) { backStackEntry ->
                val monthlyPlanDayId = backStackEntry.arguments?.getLong(FitVietDestination.Workout.ARG_MONTHLY_PLAN_DAY_ID)?.takeIf { it != -1L }
                val viewModel: WorkoutViewModel = viewModel(
                    factory = WorkoutViewModel.Factory(
                        exerciseRepository = container.exerciseRepository,
                        workoutRepository = container.workoutRepository,
                        communityRepository = container.communityRepository,
                        monthlyPlanRepository = container.monthlyPlanRepository,
                        databaseReady = container.databaseReady,
                        monthlyPlanDayId = monthlyPlanDayId,
                    ),
                )
                WorkoutScreen(
                    viewModel = viewModel,
                    // A plain popBackStack(Home.route) rather than the navigate()+popUpTo+
                    // launchSingleTop+restoreState dance used by BottomNavBar's tab switches:
                    // Home is always already sitting lower in the back stack by the time a
                    // session reaches SessionFinished (reached either directly from the FAB, or
                    // via Programs' onGenerateFromProgram, which explicitly navigates through
                    // Home on its way here — see that call site above), so this only needs to pop
                    // back down to it, not re-navigate to a possibly-saved instance of it.
                    // Confirmed via manual testing that the popUpTo/restoreState combo
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
                    // Redesign Gate 1c — the no-arg entry point's "no active plan" outcome. Pops
                    // this dead-end Workout screen off the back stack (there's no session to return
                    // to), landing on Home. Redesign Gate 3c — used to push the (now-retired)
                    // full-screen Quick Generate on top of Home automatically; the generate flow is
                    // a per-screen sheet now (see GenerateSheet.kt's own doc on why there's no single
                    // shared host to push into from here), so this deliberately stops at Home and
                    // lets its own empty-state CTA be the affordance — one extra tap on a dead-end
                    // error path, not a stranded user (Home's EmptyPlanCard is always right there).
                    onNoPlan = {
                        navController.popBackStack(FitVietDestination.Home.route, inclusive = false)
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
                    // Redesign Phase 3b — Handbook is now a drill-in (see HandbookScreen's own
                    // `onBack` doc), reached only via the Kế hoạch tab, so there's always a real
                    // back-stack entry to pop.
                    onBack = { navController.popBackStack() },
                    onMuscleGroupClick = { group ->
                        navController.navigate(FitVietDestination.HandbookMuscleGroup.createRoute(group.name))
                    },
                    onFoodCategoryClick = { category ->
                        navController.navigate(FitVietDestination.HandbookFoodCategory.createRoute(category))
                    },
                )
            }
            composable(
                route = FitVietDestination.HandbookMuscleGroup.route,
                arguments = listOf(navArgument(FitVietDestination.HandbookMuscleGroup.ARG_MUSCLE_GROUP_CODE) { type = NavType.StringType }),
            ) { backStackEntry ->
                val code = backStackEntry.arguments?.getString(FitVietDestination.HandbookMuscleGroup.ARG_MUSCLE_GROUP_CODE).orEmpty()
                val group = MuscleGroup.entries.find { it.name == code } ?: MuscleGroup.CHEST
                val viewModel: HandbookMuscleGroupViewModel = viewModel(
                    factory = HandbookMuscleGroupViewModel.Factory(group, container.handbookRepository),
                )
                HandbookMuscleGroupScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onExerciseClick = { exercise ->
                        navController.navigate(FitVietDestination.ExerciseDetail.createRoute(exercise.id))
                    },
                )
            }
            composable(
                route = FitVietDestination.HandbookFoodCategory.route,
                arguments = listOf(navArgument(FitVietDestination.HandbookFoodCategory.ARG_CATEGORY) { type = NavType.StringType }),
            ) { backStackEntry ->
                // createRoute() Uri.encode()s the category (it's a free-text string that can contain
                // spaces/diacritics, e.g. "Cá & hải sản") — Navigation Compose's own route-matching
                // already applies Uri.decode() to path arguments before they reach this Bundle, so
                // no further decoding is needed here.
                val category = backStackEntry.arguments?.getString(FitVietDestination.HandbookFoodCategory.ARG_CATEGORY).orEmpty()
                val viewModel: HandbookFoodCategoryViewModel = viewModel(
                    factory = HandbookFoodCategoryViewModel.Factory(category, container.handbookRepository),
                )
                HandbookFoodCategoryScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            }
        }
    }
}
