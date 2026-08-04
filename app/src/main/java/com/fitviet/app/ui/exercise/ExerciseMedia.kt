package com.fitviet.app.ui.exercise

import com.fitviet.app.R
import com.fitviet.app.data.local.seed.SeedExerciseNames

/**
 * Real start/end-position photos for the seeded exercises — replaces the 1d media placeholder for
 * exercises we have bundled images for. Sourced from free-exercise-db (github.com/yuhonas/free-exercise-db,
 * public domain / Unlicense); see licenses/exercise-photos/UNLICENSE-free-exercise-db.txt. Exercises with
 * no entry here fall back to the placeholder box (see [ExerciseDetailScreen]'s `ExerciseMediaBox`).
 */
private val EXERCISE_PHOTOS: Map<String, List<Int>> = mapOf(
    SeedExerciseNames.BENCH_PRESS to listOf(R.drawable.barbell_bench_press_0, R.drawable.barbell_bench_press_1),
    SeedExerciseNames.SHOULDER_PRESS to listOf(R.drawable.dumbbell_shoulder_press_0, R.drawable.dumbbell_shoulder_press_1),
    SeedExerciseNames.CABLE_FLY to listOf(R.drawable.cable_crossover_0, R.drawable.cable_crossover_1),
    SeedExerciseNames.LATERAL_RAISE to listOf(R.drawable.side_lateral_raise_0, R.drawable.side_lateral_raise_1),
)

fun exercisePhotosFor(nameVi: String): List<Int> = EXERCISE_PHOTOS[nameVi].orEmpty()
