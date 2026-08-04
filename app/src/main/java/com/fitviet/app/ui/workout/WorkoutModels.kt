package com.fitviet.app.ui.workout

import com.fitviet.app.data.local.entity.ExerciseEntity

data class PlannedSet(val weightKg: Double, val reps: Int)

/** A straight-set block: one exercise, sets performed one after another with rest in between. */
data class StraightBlockPlan(val exercise: ExerciseEntity, val plannedSets: List<PlannedSet>)

/** A superset block: two exercises performed back-to-back with no rest, then rest, for N rounds. */
data class SupersetBlockPlan(
    val exerciseA: ExerciseEntity,
    val plannedA: PlannedSet,
    val exerciseB: ExerciseEntity,
    val plannedB: PlannedSet,
    val totalRounds: Int,
)

sealed class WorkoutBlockPlan {
    data class Straight(val plan: StraightBlockPlan) : WorkoutBlockPlan()
    data class Superset(val plan: SupersetBlockPlan) : WorkoutBlockPlan()
}

enum class SetRowStatus { DONE, CURRENT, PENDING }

data class SetRowUiState(
    val setIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val status: SetRowStatus,
)

/** A set as actually performed, ready to persist to Room. */
data class LoggedSet(val exerciseId: Long, val exerciseOrder: Int, val setIndex: Int, val weightKg: Double, val reps: Int)

/** Set-technique options shown in the picker sheet (2c) — README §2c. */
enum class SetTechnique { STRAIGHT, SUPERSET, DROP_SET, PYRAMID, REST_PAUSE }

/**
 * Straight and Superset are the two techniques with a distinct, implemented execution flow —
 * they're what determine [WorkoutBlockPlan]'s two variants. Drop set / Pyramid / Rest-pause are
 * selectable in the picker (matching the design) but the spec doesn't define distinct set-by-set
 * mechanics for them beyond the picker itself, so selecting one is informational only for now.
 */

sealed class WorkoutPhase {
    /** Gate 10: pick a time budget (30/60 min, or none) before the session's blocks are built. */
    data object SelectingDuration : WorkoutPhase()
    data object StraightLog : WorkoutPhase()
    data object StraightRest : WorkoutPhase()
    data object StraightBlockDone : WorkoutPhase()
    data object SupersetWork : WorkoutPhase()
    data object SupersetRest : WorkoutPhase()
    data object SupersetBlockDone : WorkoutPhase()
    data object SessionFinished : WorkoutPhase()
}
