package com.fitviet.app.ui.workout

import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.repository.ExerciseRepository
import com.fitviet.app.data.repository.ProgramRepository
import com.fitviet.app.data.repository.WorkoutRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/** One program-day exercise, resolved to its real [ExerciseEntity] (for photo/English name) and a
 * recommended starting weight — feeds both the "day exercise list" preview screen (Gate 24) and
 * the live logging session it starts. */
data class ProgramDayWorkoutItem(
    val exercise: ExerciseEntity,
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val recommendedWeightKg: Double,
)

data class ResolvedProgramDay(val titleVi: String, val items: List<ProgramDayWorkoutItem>)

/**
 * Resolves a program's schedule for *today* into display/session-ready data, and builds a
 * [WorkoutBlockPlan] session from it — the Gate 24 counterpart to [WorkoutPlanSeed]/
 * [WorkoutTimeBudgetPlanner], which build sessions from the free-standing exercise catalog instead
 * of a specific program day. [resolveToday] does real I/O (schedule + personal-best lookups), so
 * unlike its siblings this isn't a pure function — kept in the same `ui/workout` package rather
 * than the pure `domain` layer for that reason.
 */
object ProgramDayWorkoutPlanner {

    /** Used when an exercise has no logged history yet — a plausible starting point rather than
     * 0kg, which would read as "no weight" instead of "not yet tried." */
    const val DEFAULT_RECOMMENDED_WEIGHT_KG = 20.0

    /** Null if this program has no schedule yet, has no non-rest day matching today, or that
     * day's exercises don't resolve against the local exercise library — callers show an empty
     * state in all three cases, so they're collapsed into one result rather than distinguished. */
    suspend fun resolveToday(
        programId: Long,
        programRepository: ProgramRepository,
        exerciseRepository: ExerciseRepository,
        workoutRepository: WorkoutRepository,
    ): ResolvedProgramDay? {
        val schedule = programRepository.observeSchedule(programId).first()
        val today = schedule.firstOrNull { it.dayOfWeek == LocalDate.now().dayOfWeek && !it.isRestDay } ?: return null
        if (today.exercises.isEmpty()) return null

        val exercisesById = exerciseRepository.getAll().associateBy { it.id }
        val items = today.exercises.mapNotNull { scheduleExercise ->
            val exercise = exercisesById[scheduleExercise.exerciseId] ?: return@mapNotNull null
            ProgramDayWorkoutItem(
                exercise = exercise,
                targetSets = scheduleExercise.targetSets,
                targetRepsMin = scheduleExercise.targetRepsMin,
                targetRepsMax = scheduleExercise.targetRepsMax,
                recommendedWeightKg = workoutRepository.getRecommendedWeight(exercise.id) ?: DEFAULT_RECOMMENDED_WEIGHT_KG,
            )
        }
        if (items.isEmpty()) return null
        return ResolvedProgramDay(titleVi = today.titleVi, items = items)
    }

    /** Collapses each item's rep *range* to a single concrete rep count (its midpoint) — a live
     * session logs one rep target per set, unlike the preview screen, which shows the full range. */
    fun buildBlocks(items: List<ProgramDayWorkoutItem>): List<WorkoutBlockPlan> = items.map { item ->
        val reps = ((item.targetRepsMin + item.targetRepsMax) / 2).coerceAtLeast(1)
        WorkoutBlockPlan.Straight(
            StraightBlockPlan(
                exercise = item.exercise,
                plannedSets = List(item.targetSets.coerceAtLeast(1)) { PlannedSet(item.recommendedWeightKg, reps) },
            ),
        )
    }
}
