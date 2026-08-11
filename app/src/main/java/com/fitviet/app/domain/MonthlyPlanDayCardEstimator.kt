package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.MonthlyPlanExerciseEntity
import com.fitviet.app.ui.workout.DEFAULT_REST_SECONDS
import com.fitviet.app.ui.workout.SECONDS_PER_REP
import com.fitviet.app.ui.workout.TRANSITION_SECONDS

/**
 * Dashboard Today-card duration estimate for a monthly-plan day's exercises — mirrors
 * [com.fitviet.app.ui.workout.ProgramDayWorkoutPlanner.estimateDurationMinutes]'s straight-set
 * formula exactly (same constants, same per-set/rest/transition math). Doesn't route through that
 * function's [com.fitviet.app.ui.workout.ResolvedGrouping]/superset-pairing path because
 * [MonthlyPlanExerciseEntity.supersetGroup] is always null for generator output today (see
 * [MonthlyPlanExerciseDraft]'s doc) — every row is guaranteed to resolve as a straight/solo block,
 * so this direct sum is exact, not an approximation. Revisit (route through the real planner
 * instead) if/when the generator ever starts auto-pairing supersets.
 */
object MonthlyPlanDayCardEstimator {
    fun estimateDurationMinutes(exercises: List<MonthlyPlanExerciseEntity>): Int {
        val totalSeconds = exercises.sumOf { exercise ->
            val sets = exercise.targetSets.coerceAtLeast(1)
            val reps = ((exercise.targetRepsMin + exercise.targetRepsMax) / 2).coerceAtLeast(1)
            sets * reps * SECONDS_PER_REP + (sets - 1).coerceAtLeast(0) * DEFAULT_REST_SECONDS + TRANSITION_SECONDS
        }
        return (totalSeconds / 60).coerceAtLeast(1)
    }
}
