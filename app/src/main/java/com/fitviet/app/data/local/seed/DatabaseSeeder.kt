package com.fitviet.app.data.local.seed

import androidx.room.withTransaction
import com.fitviet.app.data.local.FitVietDatabase
import java.time.LocalDate

/** Populates an empty database with the seed content on first app launch. Idempotent, atomic. */
class DatabaseSeeder(private val database: FitVietDatabase) {

    suspend fun seedIfEmpty() {
        database.withTransaction {
            if (database.programDao().count() > 0) return@withTransaction

            val today = LocalDate.now().toEpochDay()
            database.programDao().insertAll(SeedData.programs)
            database.exerciseDao().insertAll(SeedData.exercises)
            SeedData.meals(today).forEach { database.mealDao().insert(it) }
            SeedData.measurements(today).forEach { database.measurementDao().insert(it) }
            SeedData.workoutSessions(System.currentTimeMillis()).forEach { database.workoutSessionDao().insert(it) }
        }
    }
}
