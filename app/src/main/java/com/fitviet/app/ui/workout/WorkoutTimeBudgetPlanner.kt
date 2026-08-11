package com.fitviet.app.ui.workout

import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.seed.SeedExerciseNames
import kotlin.math.roundToInt

/** Seconds of actual lifting time assumed per rep (concentric + eccentric) when estimating how long
 * a block takes — a transparent estimate, not a promise; the user's real pace, rest add/skip taps,
 * and edited weight/reps all make the real session run long or short. Internal (not private): also
 * used by [ProgramDayWorkoutPlanner.estimateDurationMinutes] for the day-preview screen's duration
 * line, which needs the exact same assumptions this budget-fitting logic uses so the two figures
 * can never quietly drift apart. */
internal const val SECONDS_PER_REP = 3

/** Fixed overhead per exercise — walking to the next station, loading the bar, reading the setup —
 * added once per block regardless of set count. */
internal const val TRANSITION_SECONDS = 30

/**
 * A sensible full-body order — compound lifts first (most training value per minute), then
 * accessory/isolation work — used to greedily fill a time budget from the front. Exercises not
 * listed here simply can't appear in a budget-generated session (still reachable via 1c's search).
 */
private val CURRICULUM_ORDER = listOf(
    SeedExerciseNames.SQUAT,
    SeedExerciseNames.BENCH_PRESS,
    SeedExerciseNames.BENT_OVER_ROW,
    SeedExerciseNames.SHOULDER_PRESS,
    SeedExerciseNames.DEADLIFT,
    SeedExerciseNames.LAT_PULLDOWN,
    SeedExerciseNames.LEG_PRESS,
    SeedExerciseNames.LUNGE,
    SeedExerciseNames.BARBELL_CURL,
    SeedExerciseNames.TRICEPS_PUSHDOWN,
    SeedExerciseNames.CABLE_FLY,
    SeedExerciseNames.LATERAL_RAISE,
    SeedExerciseNames.CRUNCH,
    SeedExerciseNames.PUSHUP,
)

/**
 * Builds a straight-block-only session sized to fit [budgetMinutes] — an alternative to
 * [WorkoutPlanSeed]'s fixed curated demo, drawing from the full exercise catalog (Gate 9) instead
 * of a hand-picked 4. Greedily fills [CURRICULUM_ORDER] from the front, stopping once the next
 * exercise wouldn't fit (always includes at least one exercise, even over a tiny budget).
 *
 * No superset blocks: pairing arbitrary exercises into a superset isn't specified anywhere, so
 * budget-generated sessions keep it simple and straight-set only — the one hand-tuned superset stays
 * exclusive to [WorkoutPlanSeed]'s "Không giới hạn" demo.
 *
 * Generated sets start at 0kg (unlike the curated demo's fixed weights) — there's no known safe
 * starting weight for an arbitrary library exercise. The user fills it in via the existing editable
 * steppers before their first set, same as the design already intends for real input (see Gate 4).
 */
object WorkoutTimeBudgetPlanner {

    fun buildBlocks(exercises: List<ExerciseEntity>, budgetMinutes: Int): List<WorkoutBlockPlan> {
        val byName = exercises.associateBy { it.nameVi }
        val budgetSeconds = budgetMinutes * 60
        var usedSeconds = 0
        val blocks = mutableListOf<WorkoutBlockPlan>()

        for (name in CURRICULUM_ORDER) {
            val exercise = byName[name] ?: continue
            val plannedSets = plannedSetsFor(exercise)
            // Uses DEFAULT_REST_SECONDS, not exercise.suggestedRestSeconds — the runtime rest timer
            // (WorkoutViewModel.completeCurrentSet) always counts down from the same fixed default
            // regardless of exercise, so the estimate has to match that or generated sessions run
            // longer/shorter than the budget the user picked.
            val blockSeconds = estimateSeconds(plannedSets, DEFAULT_REST_SECONDS)
            if (blocks.isNotEmpty() && usedSeconds + blockSeconds > budgetSeconds) break
            usedSeconds += blockSeconds
            blocks += WorkoutBlockPlan.Straight(StraightBlockPlan(exercise, plannedSets))
        }
        return blocks
    }

    private fun plannedSetsFor(exercise: ExerciseEntity): List<PlannedSet> {
        val sets = midpoint(exercise.suggestedSetsMin, exercise.suggestedSetsMax)
        val reps = midpoint(exercise.suggestedRepsMin, exercise.suggestedRepsMax)
        return List(sets) { PlannedSet(weightKg = 0.0, reps = reps) }
    }

    private fun midpoint(min: Int, max: Int): Int = ((min + max) / 2.0).roundToInt().coerceAtLeast(1)

    private fun estimateSeconds(plannedSets: List<PlannedSet>, restSeconds: Int): Int {
        val workSeconds = plannedSets.sumOf { it.reps * SECONDS_PER_REP }
        val restTotal = (plannedSets.size - 1).coerceAtLeast(0) * restSeconds
        return workSeconds + restTotal + TRANSITION_SECONDS
    }
}
