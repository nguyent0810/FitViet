package com.fitviet.app.data

import android.content.Context
import com.fitviet.app.data.local.FitVietDatabase
import com.fitviet.app.data.local.seed.DatabaseSeeder
import com.fitviet.app.data.repository.CommunityRepository
import com.fitviet.app.data.repository.DashboardRepository
import com.fitviet.app.data.repository.DiaryRepository
import com.fitviet.app.data.repository.HandbookRepository
import com.fitviet.app.data.repository.NutritionRepository
import com.fitviet.app.data.repository.RoomMealPlanRepository
import com.fitviet.app.data.repository.RoomMonthlyPlanRepository
import com.fitviet.app.data.repository.RoomRecipeRepository
import com.fitviet.app.data.repository.OnboardingRepository
import com.fitviet.app.data.repository.ProfileRepository
import com.fitviet.app.data.repository.RemindersRepository
import com.fitviet.app.data.repository.RoomExerciseRepository
import com.fitviet.app.data.repository.RoomProgramRepository
import com.fitviet.app.data.repository.RoomWorkoutRepository
import com.fitviet.app.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Simple manual DI container — no framework is used, this app has few enough dependencies to wire by hand. */
class AppContainer(context: Context) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: FitVietDatabase = FitVietDatabase.getInstance(context)
    val onboardingRepository = OnboardingRepository(database.settingsDao())
    val programRepository = RoomProgramRepository(
        database = database,
        programDao = database.programDao(),
        programDayDao = database.programDayDao(),
        programExerciseDao = database.programExerciseDao(),
        exerciseDao = database.exerciseDao(),
        settingsDao = database.settingsDao(),
    )
    val exerciseRepository = RoomExerciseRepository(database.exerciseDao())
    val workoutRepository = RoomWorkoutRepository(database.workoutSessionDao(), database.setLogDao())
    // Declared before dashboardRepository, which now takes it as a constructor dependency
    // (Gate 1c — DashboardRepository's Today-card resolution moved to MonthlyPlanRepository
    // .observeTodaySession, shared with WorkoutViewModel's single session entry point, so it no
    // longer needs monthlyPlanDayDao/monthlyPlanExerciseDao directly).
    val monthlyPlanRepository = RoomMonthlyPlanRepository(
        database = database,
        monthlyPlanDao = database.monthlyPlanDao(),
        monthlyPlanWeekDao = database.monthlyPlanWeekDao(),
        monthlyPlanDayDao = database.monthlyPlanDayDao(),
        monthlyPlanExerciseDao = database.monthlyPlanExerciseDao(),
        splitTemplateDao = database.splitTemplateDao(),
        exerciseDao = database.exerciseDao(),
        setLogDao = database.setLogDao(),
        workoutSessionDao = database.workoutSessionDao(),
        settingsDao = database.settingsDao(),
    )
    val dashboardRepository = DashboardRepository(
        workoutSessionDao = database.workoutSessionDao(),
        mealDao = database.mealDao(),
        programDao = database.programDao(),
        measurementDao = database.measurementDao(),
        settingsDao = database.settingsDao(),
        programDayDao = database.programDayDao(),
        programExerciseDao = database.programExerciseDao(),
        exerciseDao = database.exerciseDao(),
        setLogDao = database.setLogDao(),
        monthlyPlanRepository = monthlyPlanRepository,
    )
    val diaryRepository = DiaryRepository(
        workoutSessionDao = database.workoutSessionDao(),
        setLogDao = database.setLogDao(),
    )
    val nutritionRepository = NutritionRepository(database.mealDao())
    val profileRepository = ProfileRepository(
        settingsDao = database.settingsDao(),
        measurementDao = database.measurementDao(),
    )
    val communityRepository = CommunityRepository(database.communityPostDao(), database.settingsDao())
    val handbookRepository = HandbookRepository(database.exerciseDao(), database.foodDao())
    val settingsRepository = SettingsRepository(
        database = database,
        workoutSessionDao = database.workoutSessionDao(),
        mealDao = database.mealDao(),
        measurementDao = database.measurementDao(),
        settingsDao = database.settingsDao(),
    )
    val remindersRepository = RemindersRepository(database.reminderDao())
    val recipeRepository = RoomRecipeRepository(
        recipeDao = database.recipeDao(),
        recipeIngredientDao = database.recipeIngredientDao(),
        recipeVariantDao = database.recipeVariantDao(),
        foodDao = database.foodDao(),
        favoriteRecipeDao = database.favoriteRecipeDao(),
    )
    val mealPlanRepository = RoomMealPlanRepository(
        database = database,
        userMealPlanDao = database.userMealPlanDao(),
        mealPlanDayDao = database.mealPlanDayDao(),
        mealPlanMealDao = database.mealPlanMealDao(),
        mealPlanTemplateDao = database.mealPlanTemplateDao(),
        recipeDao = database.recipeDao(),
        recipeIngredientDao = database.recipeIngredientDao(),
        recipeVariantDao = database.recipeVariantDao(),
        foodDao = database.foodDao(),
    )

    /** Drives [com.fitviet.app.util.LocaleController] from the persisted 1i language setting — a
     * cross-cutting app-level concern, not Profile-feature business logic, so it lives here rather
     * than on [ProfileRepository]. */
    val languageIsEnglish: Flow<Boolean> = database.settingsDao().observe().map { it?.languageIsEnglish ?: false }

    /** "Hit & Run" (Gate 63+) Phase 9 — the power-user toggle read at every call site that
     * currently always routes through [com.fitviet.app.ui.workout.WorkoutPreviewScreen]; same
     * cross-cutting-concern reasoning as [languageIsEnglish] above for why this lives here rather
     * than being re-derived per screen. */
    val skipWorkoutPreview: Flow<Boolean> = database.settingsDao().observe().map { it?.skipWorkoutPreview ?: false }

    /** Seeding runs once on first launch; callers that read seed content (e.g. the workout flow's
     * exercise catalog) must await this first so they don't race an empty, not-yet-seeded table. */
    val databaseReady: Deferred<Unit> = applicationScope.async { DatabaseSeeder(database).seedIfEmpty() }
}
