package com.fitviet.app.domain

import java.time.DayOfWeek

/**
 * Plain output tree of [MonthlyPlanGenerator] — mirrors the persisted
 * [com.fitviet.app.data.local.entity.MonthlyPlanEntity]/`Week`/`Day`/`Exercise` shape 1:1 but
 * carries no Room ids yet. [com.fitviet.app.data.repository.MonthlyPlanRepository] is what turns
 * this into real rows inside a transaction — same "pure build, transactional persist" split as
 * [ProgramTransfer]'s decode step.
 */
data class MonthlyPlanDraft(
    val goal: TrainingGoal,
    val level: ExerciseDifficulty,
    val splitTemplate: SplitTemplate,
    val daysPerWeek: Int,
    val totalWeeks: Int,
    /** The Monday week 1 anchors to. */
    val startEpochDay: Long,
    val equipmentProfile: String?,
    val exclusionExerciseIds: Set<Long>,
    val excludedMuscleGroupCodes: Set<String>,
    val sessionDurationTargetMinutes: Int,
    val weeks: List<MonthlyPlanWeekDraft>,
)

data class MonthlyPlanWeekDraft(
    val weekIndex: Int,
    val phase: PlanPhase,
    val days: List<MonthlyPlanDayDraft>,
)

data class MonthlyPlanDayDraft(
    val epochDay: Long,
    val dayOfWeek: DayOfWeek,
    val isRestDay: Boolean,
    /** Display label ("Push", "Upper A", "Power Lower"...), null for rest days. */
    val sessionType: String?,
    val muscleGroupCodes: List<String>,
    /** Empty for rest days. */
    val exercises: List<MonthlyPlanExerciseDraft>,
)

data class MonthlyPlanExerciseDraft(
    val exerciseId: Long,
    val orderIndex: Int,
    val tier: ExerciseTier,
    val selectionReasonCode: String,
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val targetWeightKg: Double?,
    /** Always null in this pass — [MonthlyPlanGenerator] doesn't auto-pair exercises into
     * supersets (no specified pairing rule to generate from, same reasoning
     * [com.fitviet.app.ui.workout.WorkoutTimeBudgetPlanner] already documents for why its own
     * generator is straight-sets-only). The column exists so a future manual "pair these two"
     * regenerate action, or a later gate's auto-pairing rule, doesn't need a schema change. */
    val supersetGroup: String? = null,
)
