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
    MuscleGroup.SHOULDERS -> R.string.muscle_group_shoulders
    MuscleGroup.ARMS -> R.string.muscle_group_arms
    MuscleGroup.CORE -> R.string.muscle_group_core
}
