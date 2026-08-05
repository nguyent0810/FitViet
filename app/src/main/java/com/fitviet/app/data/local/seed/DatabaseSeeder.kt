package com.fitviet.app.data.local.seed

import androidx.room.withTransaction
import com.fitviet.app.data.local.FitVietDatabase
import com.fitviet.app.data.local.entity.ProgramDayEntity
import com.fitviet.app.data.local.entity.ProgramExerciseEntity
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
            seedMissingCommunityPosts()

            if (database.programDao().count() == 0) {
                val today = LocalDate.now().toEpochDay()
                database.programDao().insertAll(SeedData.programs)
                SeedData.meals(today).forEach { database.mealDao().insert(it) }
                SeedData.measurements(today).forEach { database.measurementDao().insert(it) }
                SeedData.workoutSessions(System.currentTimeMillis()).forEach { database.workoutSessionDao().insert(it) }
            }

            seedMissingProgramSchedules()
        }
    }

    private suspend fun seedMissingExercises() {
        val existingNames = database.exerciseDao().getAllOnce().map { it.nameVi }.toSet()
        val missing = SeedData.exercises.filter { it.nameVi !in existingNames }
        if (missing.isNotEmpty()) database.exerciseDao().insertAll(missing)
    }

    /** Backfills independently of the "is this a fresh DB" gate above — same reason as
     * [seedMissingExercises]: a database already seeded by an earlier gate (pre-Gate-7) would
     * otherwise never pick up the community posts added here, since [seedIfEmpty] only checks
     * whether programs exist. */
    private suspend fun seedMissingCommunityPosts() {
        if (database.communityPostDao().count() > 0) return
        database.communityPostDao().insertAll(SeedData.communityPosts)
    }

    /**
     * Seeds each program's weekly schedule (Gate 15) if it has none — same "content-only, no
     * schema change" backfill pattern as [seedMissingExercises]/[seedMissingCommunityPosts]. In
     * practice this gate's own schema change (new tables/columns) already forces a destructive
     * recreate on any pre-Gate-15 install via [FitVietDatabase]'s version bump, so this method's
     * real-world job today is just seeding the fresh database that recreate produces. It stays in
     * place as durable infrastructure for a *future* content-only addition of a schedule to a
     * program that currently has none.
     *
     * Deliberately NOT a general repair mechanism: it only asks "does this program have zero
     * schedule rows," not "does it have the *current* [SeedData.programSchedules] entry" — inserting
     * only the missing days of an already-partially-seeded program would need matching by
     * `dayOfWeek`, which this doesn't do (a naive re-insert-everything would violate the
     * `(programId, dayOfWeek)` unique index the moment it hit an already-seeded day). Matches
     * programs by [SeedData.programs]'s stable `titleVi`, not by id (real ids are assigned at
     * insert time, unknown to this seed data).
     */
    private suspend fun seedMissingProgramSchedules() {
        val existingPrograms = database.programDao().getAllOnce()
        if (existingPrograms.isEmpty()) return
        val exerciseIdByName = database.exerciseDao().getAllOnce().associateBy({ it.nameVi }, { it.id })

        SeedData.programs.forEachIndexed { index, programSeed ->
            val program = existingPrograms.firstOrNull { it.titleVi == programSeed.titleVi } ?: return@forEachIndexed
            if (database.programDayDao().countForProgram(program.id) > 0) return@forEachIndexed
            val days = SeedData.programSchedules.getOrNull(index) ?: return@forEachIndexed

            days.forEach { daySeed ->
                val dayId = database.programDayDao().insert(
                    ProgramDayEntity(
                        programId = program.id,
                        dayOfWeek = daySeed.dayOfWeek,
                        titleVi = daySeed.titleVi,
                        isRestDay = daySeed.isRestDay,
                    ),
                )
                val programExercises = daySeed.exercises.mapIndexedNotNull { orderIndex, exerciseSeed ->
                    val exerciseId = exerciseIdByName[exerciseSeed.exerciseName] ?: return@mapIndexedNotNull null
                    ProgramExerciseEntity(
                        programDayId = dayId,
                        exerciseId = exerciseId,
                        orderIndex = orderIndex,
                        targetSets = exerciseSeed.targetSets,
                        targetRepsMin = exerciseSeed.targetRepsMin,
                        targetRepsMax = exerciseSeed.targetRepsMax,
                    )
                }
                if (programExercises.isNotEmpty()) database.programExerciseDao().insertAll(programExercises)
            }
        }
    }
}
