package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fitviet.app.domain.SplitTemplate
import com.fitviet.app.domain.TrainingGoal

/** A training program card shown on the Giáo án (1c) list. All programs are free. Redesign
 * Gate 2b made this read-only generation input — [ProgramDayEntity]/[ProgramExerciseEntity]
 * back a preview only (`ProgramDayWorkoutPlanner`, `WorkoutPreviewScreen`) and `exportProgram`'s
 * fidelity, never a directly-loggable live session; tapping a program card generates a real
 * [MonthlyPlanEntity] from [goal]/[splitTemplate]/[sessionsPerWeek] instead (see
 * `ProgramsViewModel.generateFromProgram`). There is no more "active program" concept — see
 * [SettingsEntity.activeMonthlyPlanId]'s own doc. */
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
    /** "Hit & Run" redesign (Gate 1b, consumed starting Gate 2b) — this program's
     * [TrainingGoal]/[SplitTemplate] as generation *input*, read by `ProgramsViewModel
     * .generateFromProgram` (see this class's own doc). Every current seed program has a real,
     * non-null value; stored as each enum's own `.name`, same convention as [SettingsEntity]'s
     * columns. An imported program carries these too as of Gate 2b (see [ProgramTransferData][
     * com.fitviet.app.domain.ProgramTransferData]'s doc) — optional there since older exports
     * predate the field. */
    val goal: String = TrainingGoal.HYPERTROPHY.name,
    val splitTemplate: String = SplitTemplate.FULL_BODY.name,
)
