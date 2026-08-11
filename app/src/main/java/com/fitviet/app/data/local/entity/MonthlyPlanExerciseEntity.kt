package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One exercise assignment within a [MonthlyPlanDayEntity] — unlike [ProgramExerciseEntity]
 * (shared/repeated across every week of a program), this table has a full row set **per day per
 * week**, since progression means sets/reps/weight genuinely differ week to week for the same
 * exercise. Exercise identity/order/tier are fixed for the whole block by the generator (see
 * [com.fitviet.app.domain.MonthlyPlanGenerator]); only the target numbers vary by week. */
@Entity(
    tableName = "monthly_plan_exercises",
    foreignKeys = [
        ForeignKey(
            entity = MonthlyPlanDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["monthlyPlanDayId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("monthlyPlanDayId"), Index("exerciseId")],
)
data class MonthlyPlanExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthlyPlanDayId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    /** [com.fitviet.app.domain.ExerciseTier.name]. */
    val tier: String,
    /** Compact pipe-delimited trace of why this exercise was picked, e.g.
     * "MAIN_COMPOUND|CHEST|equip_ok|not_used_3sessions" — the "explainable, not random"
     * requirement; decoded/localized by the UI, not stored pre-formatted. */
    val selectionReasonCode: String,
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    /** Null = no known PR yet; the live session falls back to
     * [com.fitviet.app.ui.workout.ProgramDayWorkoutPlanner.DEFAULT_RECOMMENDED_WEIGHT_KG], same as
     * the hand-authored-program path. */
    val targetWeightKg: Double? = null,
    /** Same convention as [ProgramExerciseEntity.supersetGroup] — reuses
     * [com.fitviet.app.ui.workout.ProgramDayWorkoutPlanner.resolveGroupings] unmodified. */
    val supersetGroup: String? = null,
)
