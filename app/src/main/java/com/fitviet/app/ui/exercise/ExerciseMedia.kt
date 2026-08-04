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
    // Gate 9
    SeedExerciseNames.SQUAT to listOf(R.drawable.barbell_squat_0, R.drawable.barbell_squat_1),
    SeedExerciseNames.DEADLIFT to listOf(R.drawable.barbell_deadlift_0, R.drawable.barbell_deadlift_1),
    SeedExerciseNames.LAT_PULLDOWN to listOf(R.drawable.lat_pulldown_0, R.drawable.lat_pulldown_1),
    SeedExerciseNames.BENT_OVER_ROW to listOf(R.drawable.bent_over_row_0, R.drawable.bent_over_row_1),
    SeedExerciseNames.BARBELL_CURL to listOf(R.drawable.barbell_curl_0, R.drawable.barbell_curl_1),
    SeedExerciseNames.TRICEPS_PUSHDOWN to listOf(R.drawable.triceps_pushdown_0, R.drawable.triceps_pushdown_1),
    SeedExerciseNames.LEG_PRESS to listOf(R.drawable.leg_press_0, R.drawable.leg_press_1),
    SeedExerciseNames.LUNGE to listOf(R.drawable.dumbbell_lunges_0, R.drawable.dumbbell_lunges_1),
    SeedExerciseNames.CRUNCH to listOf(R.drawable.crunches_0, R.drawable.crunches_1),
    SeedExerciseNames.PUSHUP to listOf(R.drawable.pushups_0, R.drawable.pushups_1),
)

fun exercisePhotosFor(nameVi: String): List<Int> = EXERCISE_PHOTOS[nameVi].orEmpty()
