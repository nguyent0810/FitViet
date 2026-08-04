package com.fitviet.app.data

import android.content.Context
import com.fitviet.app.data.local.FitVietDatabase
import com.fitviet.app.data.local.seed.DatabaseSeeder
import com.fitviet.app.data.repository.OnboardingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Simple manual DI container — no framework is used, this app has few enough dependencies to wire by hand. */
class AppContainer(context: Context) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: FitVietDatabase = FitVietDatabase.getInstance(context)
    val onboardingRepository = OnboardingRepository(database.settingsDao())

    init {
        applicationScope.launch { DatabaseSeeder(database).seedIfEmpty() }
    }
}
