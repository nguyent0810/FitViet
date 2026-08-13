package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A user-authored Custom Split, referenced by [MonthlyPlanEntity.customSplitTemplateId] when
 * [MonthlyPlanEntity.splitTemplate] == CUSTOM. The 5 built-in templates (PPL/Upper-Lower/Full
 * Body/Bro Split/PHUL) are hardcoded in [com.fitviet.app.domain.MonthlyPlanGenerator], not rows
 * here. No authoring UI ships in this pass; this table exists so the data model doesn't need a
 * breaking change when one does. */
@Entity(tableName = "split_templates")
data class SplitTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userDefinedLabel: String,
    val daysPerWeek: Int,
    /** JSON-encoded `List<{dayIndex, muscleGroupCodes, label}>` — same pure-versioned-JSON idiom
     * as [com.fitviet.app.domain.ProgramTransfer]. */
    val dayDefinitionsJson: String,
    /** [com.fitviet.app.domain.MuscleGroup.name] values, highest priority first. */
    val priorityMuscleGroupCodes: List<String> = emptyList(),
)
