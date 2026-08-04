package com.fitviet.app.data.local.seed

import androidx.room.withTransaction
import com.fitviet.app.data.local.FitVietDatabase
import java.time.LocalDate

/**
 * Populates an empty database with the seed content on first app launch. Idempotent, atomic.
 *
 * Exercises are backfilled independently of the "first launch" gate below: a database seeded by
 * an earlier app version (e.g. before Gate 4 added the superset exercises) would otherwise never
 * receive exercises added later, since [seedIfEmpty] only checks whether programs exist.
 */
class DatabaseSeeder(private val database: FitVietDatabase) {

    suspend fun seedIfEmpty() {
        database.withTransaction {
            seedMissingExercises()

            if (database.programDao().count() > 0) return@withTransaction

            val today = LocalDate.now().toEpochDay()
            database.programDao().insertAll(SeedData.programs)
            SeedData.meals(today).forEach { database.mealDao().insert(it) }
            SeedData.measurements(today).forEach { database.measurementDao().insert(it) }
            SeedData.workoutSessions(System.currentTimeMillis()).forEach { database.workoutSessionDao().insert(it) }
        }
    }

    private suspend fun seedMissingExercises() {
        val existingNames = database.exerciseDao().getAllOnce().map { it.nameVi }.toSet()
        val missing = SeedData.exercises.filter { it.nameVi !in existingNames }
        if (missing.isNotEmpty()) database.exerciseDao().insertAll(missing)
    }
}
