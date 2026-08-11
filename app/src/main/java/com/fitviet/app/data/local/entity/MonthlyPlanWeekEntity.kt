package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One week within a [MonthlyPlanEntity]'s block — carries the progression phase every day's
 * [MonthlyPlanExerciseEntity] rows in that week compute their sets/reps/weight from. */
@Entity(
    tableName = "monthly_plan_weeks",
    foreignKeys = [
        ForeignKey(
            entity = MonthlyPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["monthlyPlanId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("monthlyPlanId"), Index(value = ["monthlyPlanId", "weekIndex"], unique = true)],
)
data class MonthlyPlanWeekEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthlyPlanId: Long,
    /** 0-based. */
    val weekIndex: Int,
    /** [com.fitviet.app.domain.PlanPhase.name]. */
    val phase: String,
)
