package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** An exercise from the library, shown on 1d and logged during the 1e workout flow. */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameVi: String,
    val nameEn: String,
    /** Placeholder gif filename — swapped for a real asset later (free-exercise-db / wger.de). */
    val gifAsset: String,
    val primaryMuscle: String,
    val secondaryMuscles: List<String>,
    /** Feature #9 (Gate 44) — editorial estimates of each displayed muscle's share of this
     * exercise's effort, primary muscle first then [secondaryMuscles] in order. Empty is the
     * explicit "hide the involvement card" signal (mismatch #8) for exercises where a clean
     * per-muscle split would be misleading (pure cardio, stretching, loaded carries/strongman
     * movements) — not an omission. See [com.fitviet.app.domain.MuscleInvolvement] for the
     * validation contract every non-empty list here must satisfy. */
    val involvementPercents: List<Int> = emptyList(),
    val equipment: String,
    val instructions: List<String>,
    val suggestedSetsMin: Int,
    val suggestedSetsMax: Int,
    val suggestedRepsMin: Int,
    val suggestedRepsMax: Int,
    val suggestedRestSeconds: Int,
    /** Stable classification codes ([com.fitviet.app.domain.MuscleGroup]/[com.fitviet.app.domain.MovementType]
     * `.name`), distinct from the free-text [primaryMuscle] display string above — for future
     * charts that need to group reliably regardless of locale/copy. */
    val muscleGroupCode: String,
    val movementType: String,
    /** [com.fitviet.app.domain.ExerciseDifficulty] `.name` — backs the Handbook's (Gate 25)
     * exercise-library-by-level grouping, same stable-code convention as [muscleGroupCode]. */
    val difficultyCode: String,
)
