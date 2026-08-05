package com.fitviet.app.domain

import androidx.annotation.StringRes
import com.fitviet.app.R

/**
 * Stable, language-independent classification codes for [com.fitviet.app.data.local.entity.ExerciseEntity],
 * distinct from its free-text Vietnamese `primaryMuscle`/`equipment` display fields. Exists so the
 * muscle-group-workload chart (feature #8, see [WorkoutCompositionCalculator]) can group reliably
 * regardless of locale or copy changes.
 */
enum class MuscleGroup { CHEST, BACK, LEGS, SHOULDERS, ARMS, CORE }

enum class MovementType { COMPOUND, ISOLATION }

/** Display label for [WorkoutCompositionCalculator]'s muscle-group workload chart (feature #8) —
 * same "enum → string resource" pattern as [com.fitviet.app.util.shortLabelRes]. */
@StringRes
fun MuscleGroup.labelRes(): Int = when (this) {
    MuscleGroup.CHEST -> R.string.muscle_group_chest
    MuscleGroup.BACK -> R.string.muscle_group_back
    MuscleGroup.LEGS -> R.string.muscle_group_legs
    MuscleGroup.SHOULDERS -> R.string.muscle_group_shoulders
    MuscleGroup.ARMS -> R.string.muscle_group_arms
    MuscleGroup.CORE -> R.string.muscle_group_core
}
