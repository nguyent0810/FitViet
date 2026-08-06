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
    /** See [com.fitviet.app.data.local.entity.ProgramExerciseEntity.supersetGroup]. */
    val supersetGroup: String? = null,
)

data class ResolvedProgramDay(val titleVi: String, val items: List<ProgramDayWorkoutItem>)

/** How [resolveGroupings] resolved a run of [ProgramDayWorkoutItem]s — a [Paired] entry is exactly
 * the shape [ProgramDayWorkoutPlanner.buildBlocks] turns into a [WorkoutBlockPlan.Superset]; a
 * [Solo] entry (including every item from a malformed grouping) is what it turns into a
 * [WorkoutBlockPlan.Straight]. Exposed as its own type so a non-session consumer (the "day
 * exercise list" preview screen, Gate 48) can render the exact same resolved pairing without
 * re-deriving the adjacency rule itself. */
sealed class ResolvedGrouping {
    data class Solo(val item: ProgramDayWorkoutItem) : ResolvedGrouping()
    data class Paired(val first: ProgramDayWorkoutItem, val second: ProgramDayWorkoutItem) : ResolvedGrouping()
}

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
                supersetGroup = scheduleExercise.supersetGroup,
            )
        }
        if (items.isEmpty()) return null
        return ResolvedProgramDay(titleVi = today.titleVi, items = items)
    }

    /** A non-null [ProgramDayWorkoutItem.supersetGroup] only resolves to a [ResolvedGrouping.Paired]
     * when exactly two items in [items] share it AND they're adjacent — any other shape (a lone
     * item with a group value, three-or-more sharing one, or two matching items that aren't next
     * to each other) resolves every affected item to its own [ResolvedGrouping.Solo] rather than
     * dropping it, since a malformed grouping shouldn't be able to silently remove an exercise. */
    fun resolveGroupings(items: List<ProgramDayWorkoutItem>): List<ResolvedGrouping> {
        val groupSizes = items.mapNotNull { it.supersetGroup }.groupingBy { it }.eachCount()

        val groupings = mutableListOf<ResolvedGrouping>()
        var i = 0
        while (i < items.size) {
            val item = items[i]
            val group = item.supersetGroup
            val next = items.getOrNull(i + 1)?.takeIf { group != null && groupSizes[group] == 2 && it.supersetGroup == group }
            if (next != null) {
                groupings += ResolvedGrouping.Paired(item, next)
                i += 2
            } else {
                groupings += ResolvedGrouping.Solo(item)
                i += 1
            }
        }
        return groupings
    }

    /** Collapses each item's rep *range* to a single concrete rep count (its midpoint) — a live
     * session logs one rep target per set, unlike the preview screen, which shows the full range. */
    fun buildBlocks(items: List<ProgramDayWorkoutItem>): List<WorkoutBlockPlan> = resolveGroupings(items).map { grouping ->
        when (grouping) {
            is ResolvedGrouping.Solo -> toStraightBlock(grouping.item)
            is ResolvedGrouping.Paired -> toSupersetBlock(grouping.first, grouping.second)
        }
    }

    private fun midpointReps(min: Int, max: Int) = ((min + max) / 2).coerceAtLeast(1)

    private fun toStraightBlock(item: ProgramDayWorkoutItem): WorkoutBlockPlan.Straight {
        val reps = midpointReps(item.targetRepsMin, item.targetRepsMax)
        return WorkoutBlockPlan.Straight(
            StraightBlockPlan(
                exercise = item.exercise,
                plannedSets = List(item.targetSets.coerceAtLeast(1)) { PlannedSet(item.recommendedWeightKg, reps) },
            ),
        )
    }

    /** [SupersetBlockPlan] has one shared `totalRounds` for both exercises (unlike straight
     * blocks, which carry an independent [PlannedSet] list per set) — when the pair's authored
     * `targetSets` disagree, the lower of the two wins so neither side is asked to perform a round
     * the other wasn't planned for. */
    private fun toSupersetBlock(a: ProgramDayWorkoutItem, b: ProgramDayWorkoutItem): WorkoutBlockPlan.Superset =
        WorkoutBlockPlan.Superset(
            SupersetBlockPlan(
                exerciseA = a.exercise,
                plannedA = PlannedSet(a.recommendedWeightKg, midpointReps(a.targetRepsMin, a.targetRepsMax)),
                exerciseB = b.exercise,
                plannedB = PlannedSet(b.recommendedWeightKg, midpointReps(b.targetRepsMin, b.targetRepsMax)),
                totalRounds = minOf(a.targetSets, b.targetSets).coerceAtLeast(1),
            ),
        )
}
