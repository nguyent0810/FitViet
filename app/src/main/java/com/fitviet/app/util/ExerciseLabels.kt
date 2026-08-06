package com.fitviet.app.util

import androidx.annotation.StringRes
import com.fitviet.app.R
import com.fitviet.app.domain.MuscleGroup

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
