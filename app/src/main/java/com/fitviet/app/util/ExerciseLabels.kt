package com.fitviet.app.util

import androidx.annotation.StringRes
import com.fitviet.app.R
import com.fitviet.app.domain.ExerciseDifficulty
import com.fitviet.app.domain.MuscleGroup
import com.fitviet.app.domain.NutritionGoal

/** Display label for [com.fitviet.app.domain.WorkoutCompositionCalculator]'s muscle-group workload
 * chart (feature #8) — same "enum → string resource" pattern as [shortLabelRes]/[Month.labelRes]. */
@StringRes
fun MuscleGroup.labelRes(): Int = when (this) {
    MuscleGroup.CHEST -> R.string.muscle_group_chest
    MuscleGroup.BACK -> R.string.muscle_group_back
    MuscleGroup.LEGS -> R.string.muscle_group_legs
    MuscleGroup.GLUTEUS -> R.string.muscle_group_gluteus
    MuscleGroup.DELTOIDS -> R.string.muscle_group_deltoids
    MuscleGroup.BICEPS -> R.string.muscle_group_biceps
    MuscleGroup.TRICEPS -> R.string.muscle_group_triceps
    MuscleGroup.FOREARM -> R.string.muscle_group_forearm
    MuscleGroup.ABS -> R.string.muscle_group_abs
    MuscleGroup.FUNCTIONAL -> R.string.muscle_group_functional
    MuscleGroup.CARDIO -> R.string.muscle_group_cardio
    MuscleGroup.STRETCHING -> R.string.muscle_group_stretching
}

/** Display label for the Handbook's (Gate 25) exercise-library-by-level grouping. */
@StringRes
fun ExerciseDifficulty.labelRes(): Int = when (this) {
    ExerciseDifficulty.BEGINNER -> R.string.exercise_difficulty_beginner
    ExerciseDifficulty.INTERMEDIATE -> R.string.exercise_difficulty_intermediate
    ExerciseDifficulty.ADVANCED -> R.string.exercise_difficulty_advanced
}

/** "Hit & Run" redesign (Gate 1b) — Profile's goal label, keyed by the enum
 * [com.fitviet.app.data.local.entity.SettingsEntity.selectedGoal] now stores directly, instead of
 * indexing [com.fitviet.app.ui.onboarding.GOAL_OPTIONS] positionally (that list is onboarding-only
 * UI copy and about to shrink/change shape independently of this enum). Reuses the exact same
 * string resources [com.fitviet.app.ui.onboarding.GOAL_OPTIONS] already pointed at. */
@StringRes
fun NutritionGoal.titleRes(): Int = when (this) {
    NutritionGoal.BULK -> R.string.goal_muscle_gain_title
    NutritionGoal.CUT -> R.string.goal_fat_loss_title
    NutritionGoal.MAINTAIN -> R.string.goal_health_title
}
