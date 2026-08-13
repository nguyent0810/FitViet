package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The "Hit & Run" one-tap generated multi-week plan (4-5 weeks) — a parallel model to
 * [ProgramEntity], not a reuse of it: [ProgramDayEntity]'s unique `(programId, dayOfWeek)` index
 * assumes one fixed week that repeats forever, which cannot represent week-to-week progression.
 * A user may have both a hand-picked [ProgramEntity] (browsable via Giáo án/Weekly Schedule,
 * unaffected by this table) and an active [MonthlyPlanEntity] at the same time — only the
 * Dashboard "Today" card prefers the monthly plan when one is [MonthlyPlanStatus.ACTIVE].
 */
@Entity(
    tableName = "monthly_plans",
    foreignKeys = [
        ForeignKey(
            entity = SplitTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["customSplitTemplateId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("customSplitTemplateId")],
)
data class MonthlyPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAtEpochMillis: Long,
    /** Redesign Gate 1d-i — the actual day generation ran on; every week's day-offsets are
     * relative to this date, so today (offset 0) is always trainable by construction. See
     * [com.fitviet.app.domain.MonthlyPlanGenerator]'s `SPACING_TABLE` doc. Write-only — nothing
     * reads this column back today. */
    val startEpochDay: Long,
    val totalWeeks: Int,
    /** [com.fitviet.app.domain.TrainingGoal.name]. */
    val goal: String,
    /** [com.fitviet.app.domain.ExerciseDifficulty.name] — reused, not a new level enum. */
    val level: String,
    /** [com.fitviet.app.domain.SplitTemplate.name]. */
    val splitTemplate: String,
    val daysPerWeek: Int,
    val sessionDurationTargetMinutes: Int,
    /** Key into the generator's hardcoded `EQUIPMENT_PROFILES` map, or null = unconstrained. */
    val equipmentProfile: String? = null,
    val exclusionExerciseIds: List<Long> = emptyList(),
    /** [com.fitviet.app.domain.MuscleGroup.name] values, injury/exclusion-style. */
    val excludedMuscleGroupCodes: List<String> = emptyList(),
    /** Non-null only when [splitTemplate] == CUSTOM. */
    val customSplitTemplateId: Long? = null,
    /** [com.fitviet.app.domain.MonthlyPlanStatus.name]. */
    val status: String,
)
