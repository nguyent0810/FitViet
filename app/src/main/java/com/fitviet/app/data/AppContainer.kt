package com.fitviet.app.data

import android.content.Context
import com.fitviet.app.data.local.FitVietDatabase
import com.fitviet.app.data.local.seed.DatabaseSeeder
import com.fitviet.app.data.repository.DashboardRepository
import com.fitviet.app.data.repository.ExerciseRepository
import com.fitviet.app.data.repository.OnboardingRepository
import com.fitviet.app.data.repository.ProgramRepository
import com.fitviet.app.data.repository.WorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

/** Simple manual DI container — no framework is used, this app has few enough dependencies to wire by hand. */
class AppContainer(context: Context) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: FitVietDatabase = FitVietDatabase.getInstance(context)
    val onboardingRepository = OnboardingRepository(database.settingsDao())
    val programRepository = ProgramRepository(database.programDao())
    val exerciseRepository = ExerciseRepository(database.exerciseDao())
    val workoutRepository = WorkoutRepository(database.workoutSessionDao(), database.setLogDao())
    val dashboardRepository = DashboardRepository(
        workoutSessionDao = database.workoutSessionDao(),
        mealDao = database.mealDao(),
        programDao = database.programDao(),
    )

    /** Seeding runs once on first launch; callers that read seed content (e.g. the workout flow's
     * exercise catalog) must await this first so they don't race an empty, not-yet-seeded table. */
    val databaseReady: Deferred<Unit> = applicationScope.async { DatabaseSeeder(database).seedIfEmpty() }
}
