package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fitviet.app.domain.SplitTemplate
import com.fitviet.app.domain.TrainingGoal

/** A training program card shown on the Giáo án (1c) list. All programs are free. */
@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titleVi: String,
    val imageAsset: String,
    val durationWeeks: Int,
    val sessionsPerWeek: Int,
    val level: String,
    val equipment: String,
    /** Filter chip tags, e.g. "Tăng cơ", "Phòng gym" — matches the 1c filter chip labels. */
    val tags: List<String>,
    /** "Hit & Run" redesign (Gate 1b, consumed starting Gate 2b/"3a-arch") — this program's
     * [TrainingGoal]/[SplitTemplate] as generation *input*: tapping a sample program in the
     * redesigned "Kế hoạch" tab generates a real [com.fitviet.app.data.local.entity
     * .MonthlyPlanEntity] seeded from these two values (plus this row's own [sessionsPerWeek]),
     * rather than running the program's own [ProgramDayEntity]/[ProgramExerciseEntity] rows
     * directly — collapsing the app's two previously-parallel "active plan" systems into one.
     * Every current seed program has a real, non-null value (no legacy nullable-migration case to
     * handle); stored as each enum's own `.name`, same convention as [SettingsEntity]'s columns. */
    val goal: String = TrainingGoal.HYPERTROPHY.name,
    val splitTemplate: String = SplitTemplate.FULL_BODY.name,
)
