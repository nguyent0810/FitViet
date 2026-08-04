package com.fitviet.app.ui.workout

import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.seed.SeedExerciseNames

/**
 * Builds the demo session's block plan from the exercise catalog, matching the prototype's
 * exact per-set weight/reps (its `exercises[]` array for 1e, and the A1/A2 values shown on 2c).
 * There's no program→day→exercise assignment in Room yet (see PROGRESS.md, Gates 2-3), so this
 * is a fixed 3-block demo session rather than one derived from the user's chosen program/day.
 */
object WorkoutPlanSeed {

    fun buildBlocks(exercises: List<ExerciseEntity>): List<WorkoutBlockPlan> {
        val byName = exercises.associateBy { it.nameVi }
        val blocks = mutableListOf<WorkoutBlockPlan>()

        byName[SeedExerciseNames.BENCH_PRESS]?.let { exercise ->
            blocks += WorkoutBlockPlan.Straight(
                StraightBlockPlan(
                    exercise = exercise,
                    plannedSets = listOf(
                        PlannedSet(40.0, 8),
                        PlannedSet(40.0, 8),
                        PlannedSet(45.0, 6),
                        PlannedSet(45.0, 6),
                    ),
                ),
            )
        }
        byName[SeedExerciseNames.SHOULDER_PRESS]?.let { exercise ->
            blocks += WorkoutBlockPlan.Straight(
                StraightBlockPlan(
                    exercise = exercise,
                    plannedSets = listOf(
                        PlannedSet(16.0, 10),
                        PlannedSet(16.0, 10),
                        PlannedSet(18.0, 8),
                    ),
                ),
            )
        }
        val cableFly = byName[SeedExerciseNames.CABLE_FLY]
        val lateralRaise = byName[SeedExerciseNames.LATERAL_RAISE]
        if (cableFly != null && lateralRaise != null) {
            blocks += WorkoutBlockPlan.Superset(
                SupersetBlockPlan(
                    exerciseA = cableFly,
                    plannedA = PlannedSet(15.0, 12),
                    exerciseB = lateralRaise,
                    plannedB = PlannedSet(8.0, 15),
                    totalRounds = 3,
                ),
            )
        }
        return blocks
    }
}
