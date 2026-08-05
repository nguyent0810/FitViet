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
)
