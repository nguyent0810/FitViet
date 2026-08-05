package com.fitviet.app.domain

/**
 * Stable, language-independent classification codes for [com.fitviet.app.data.local.entity.ExerciseEntity],
 * distinct from its free-text Vietnamese `primaryMuscle`/`equipment` display fields. Exists so a
 * future muscle-group-workload or exercise-type-distribution chart can group reliably regardless
 * of locale or copy changes — not consumed by any UI yet as of this gate.
 */
enum class MuscleGroup { CHEST, BACK, LEGS, SHOULDERS, ARMS, CORE }

enum class MovementType { COMPOUND, ISOLATION }
