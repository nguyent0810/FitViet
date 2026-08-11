package com.fitviet.app.ui.workout

import com.fitviet.app.data.repository.ExerciseRepository
import com.fitviet.app.data.repository.MonthlyPlanRepository
import com.fitviet.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.first

/**
 * The "Hit & Run" (Gate 63+) counterpart to [ProgramDayWorkoutPlanner] — resolves one specific
 * [com.fitviet.app.data.local.entity.MonthlyPlanDayEntity] (already picked by the caller, e.g. via
 * [com.fitviet.app.domain.MonthlyPlanTodayResolver]) into the same [ResolvedProgramDay]/
 * [ProgramDayWorkoutItem] shape a program day resolves to, so [ProgramDayWorkoutPlanner.resolveGroupings]/
 * [ProgramDayWorkoutPlanner.buildBlocks]/[ProgramDayWorkoutPlanner.estimateDurationMinutes] all work
 * unmodified against it — a monthly-plan exercise row carries the identical sets/reps/weight/superset
 * shape a program exercise row does, so there's nothing template-specific left to reimplement past
 * this mapping step.
 */
object MonthlyPlanDayWorkoutPlanner {

    /** Null if [dayId] doesn't resolve, is a rest day, or has no exercises resolving against the
     * local exercise library — callers show an empty state / fall back the same way
     * [ProgramDayWorkoutPlanner.resolveToday] does. */
    suspend fun resolveDay(
        dayId: Long,
        monthlyPlanRepository: MonthlyPlanRepository,
        exerciseRepository: ExerciseRepository,
        workoutRepository: WorkoutRepository,
    ): ResolvedProgramDay? {
        val day = monthlyPlanRepository.getDay(dayId) ?: return null
        if (day.isRestDay || day.sessionType == null) return null

        val planExercises = monthlyPlanRepository.observeExercisesForDay(dayId).first()
        if (planExercises.isEmpty()) return null

        val exercisesById = exerciseRepository.getAll().associateBy { it.id }
        val items = planExercises.mapNotNull { planExercise ->
            val exercise = exercisesById[planExercise.exerciseId] ?: return@mapNotNull null
            ProgramDayWorkoutItem(
                exercise = exercise,
                targetSets = planExercise.targetSets,
                targetRepsMin = planExercise.targetRepsMin,
                targetRepsMax = planExercise.targetRepsMax,
                // The generator already seeds a PR-aware weight at generation time (see
                // MonthlyPlanGenerator's progression step) — only falls back to a fresh
                // recommendation/default when that row genuinely has none.
                recommendedWeightKg = planExercise.targetWeightKg
                    ?: workoutRepository.getRecommendedWeight(exercise.id)
                    ?: ProgramDayWorkoutPlanner.DEFAULT_RECOMMENDED_WEIGHT_KG,
                supersetGroup = planExercise.supersetGroup,
            )
        }
        if (items.isEmpty()) return null
        return ResolvedProgramDay(titleVi = day.sessionType, items = items)
    }
}
