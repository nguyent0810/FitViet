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

/** Redesign Gate 1c's reason for [WorkoutPhase.NoSessionToday] — reuses the exact same three
 * "nothing to train today" outcomes Dashboard's [com.fitviet.app.domain.TodayMonthlyPlanCard]
 * already distinguishes, so [com.fitviet.app.ui.workout.NoSessionTodayContent] can show the same
 * copy the hero card would have shown instead of inventing separate wording. */
enum class NoSessionReason { REST_DAY, UNAVAILABLE, PLAN_FINISHED }

sealed class WorkoutPhase {
    /** Redesign Gate 1c replaced the old "pick a time budget" duration picker: a no-arg entry
     * (bottom-nav FAB) now resolves today's "Hit & Run" monthly-plan session itself via
     * [com.fitviet.app.data.repository.MonthlyPlanRepository.observeTodaySession] instead of
     * asking the user to size a generic session. This phase is the "nothing to train today"
     * outcome (rest day / a training day that resolved to zero exercises / the plan ran out) —
     * see [WorkoutViewModel.resolveTodaySessionAndStart]. */
    data class NoSessionToday(val reason: NoSessionReason) : WorkoutPhase()
    /** No active monthly plan at all — [com.fitviet.app.ui.workout.WorkoutScreen] reacts to this
     * by navigating to Quick Generate instead of rendering session content, since there's nothing
     * to log yet ([WorkoutViewModel] itself never navigates). */
    data object AwaitingPlanGeneration : WorkoutPhase()
    data object StraightLog : WorkoutPhase()
    data object StraightRest : WorkoutPhase()
    data object StraightBlockDone : WorkoutPhase()
    data object SupersetWork : WorkoutPhase()
    data object SupersetRest : WorkoutPhase()
    data object SupersetBlockDone : WorkoutPhase()
    data object SessionFinished : WorkoutPhase()
}
