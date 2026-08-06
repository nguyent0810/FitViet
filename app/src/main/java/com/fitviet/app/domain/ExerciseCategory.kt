package com.fitviet.app.domain

/**
 * Stable, language-independent classification codes for [com.fitviet.app.data.local.entity.ExerciseEntity],
 * distinct from its free-text Vietnamese `primaryMuscle`/`equipment` display fields. Exists so the
 * muscle-group-workload chart (feature #8, see [WorkoutCompositionCalculator]) can group reliably
 * regardless of locale or copy changes. Kept free of any Compose/Android import (see
 * [com.fitviet.app.util.labelRes] for the display-label lookup) — this package is the app's pure,
 * Room/Compose-free domain layer, standalone-`kotlinc`-verified on every gate.
 */
enum class MuscleGroup {
    CHEST, BACK, LEGS, GLUTEUS, DELTOIDS, BICEPS, TRICEPS, FOREARM, ABS, FUNCTIONAL, CARDIO, STRETCHING
}

enum class MovementType { COMPOUND, ISOLATION }
