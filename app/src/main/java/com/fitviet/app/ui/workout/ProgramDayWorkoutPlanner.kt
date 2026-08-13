package com.fitviet.app.ui.workout

import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.repository.ExerciseRepository
import com.fitviet.app.data.repository.ProgramRepository
import com.fitviet.app.data.repository.WorkoutRepository
import java.time.DayOfWeek
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
 * Resolves a program's schedule for a given day into display-ready data (redesign Gate 2b: this
 * program-day preview is read-only now — see [resolveDay]'s own doc — no live session is ever
 * built directly from it anymore; [buildBlocks] stays for
 * [com.fitviet.app.ui.workout.MonthlyPlanDayWorkoutPlanner]'s own generated-day sessions, which use
 * the same [ResolvedProgramDay]/[ProgramDayWorkoutItem] shape this object works against, so
 * [buildBlocks]/[resolveGroupings]/[estimateDurationMinutes] serve both). [resolveDay] does real
 * I/O (schedule + personal-best lookups), so unlike a pure function this isn't kept in the
 * `domain` layer.
 */
object ProgramDayWorkoutPlanner {

    /** Used when an exercise has no logged history yet — a plausible starting point rather than
     * 0kg, which would read as "no weight" instead of "not yet tried." */
    const val DEFAULT_RECOMMENDED_WEIGHT_KG = 20.0

    /** Null if this program has no non-rest day matching [dayOfWeek], or that day's exercises
     * don't resolve against the local exercise library — callers show an empty state in both
     * cases, so they're collapsed into one result rather than distinguished. Redesign Gate 2b's
     * `WorkoutPreviewViewModel` is the sole remaining caller, resolving whatever day
     * `NextTrainingCalculator.findNext` picks (today if trainable, else the next upcoming one) —
     * the old bare "always today" `resolveToday` overload this generalized is gone with the
     * Weekly Schedule screen that used to call it directly. */
    suspend fun resolveDay(
        programId: Long,
        dayOfWeek: DayOfWeek,
        programRepository: ProgramRepository,
        exerciseRepository: ExerciseRepository,
        workoutRepository: WorkoutRepository,
    ): ResolvedProgramDay? {
        val schedule = programRepository.observeSchedule(programId).first()
        val day = schedule.firstOrNull { it.dayOfWeek == dayOfWeek && !it.isRestDay } ?: return null
        if (day.exercises.isEmpty()) return null

        val exercisesById = exerciseRepository.getAll().associateBy { it.id }
        val items = day.exercises.mapNotNull { scheduleExercise ->
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
        return ResolvedProgramDay(titleVi = day.titleVi, items = items)
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

    /** Shared by [toSupersetBlock] (the live session's block) and the preview screen's group
     * header, so both describe a pair's round count identically. */
    fun supersetRounds(a: ProgramDayWorkoutItem, b: ProgramDayWorkoutItem) = minOf(a.targetSets, b.targetSets).coerceAtLeast(1)

    /** Rough end-to-end estimate for the whole day, shown on the preview screen so "Bắt đầu tập"
     * isn't a blind commitment — reuses [SECONDS_PER_REP]/[TRANSITION_SECONDS]'s exact per-rep/
     * per-transition assumptions and [DEFAULT_REST_SECONDS] rather than inventing a second set of
     * constants. A
     * paired group counts its rest once per round (shared between both exercises), not once per
     * exercise like a solo block — mirroring how [WorkoutViewModel]'s superset-rest phase actually
     * runs, so this estimate doesn't overcount rest for supersets versus straight sets. */
    fun estimateDurationMinutes(groupings: List<ResolvedGrouping>): Int {
        val totalSeconds = groupings.sumOf { grouping ->
            when (grouping) {
                is ResolvedGrouping.Solo -> {
                    val item = grouping.item
                    val sets = item.targetSets.coerceAtLeast(1)
                    val reps = midpointReps(item.targetRepsMin, item.targetRepsMax)
                    sets * reps * SECONDS_PER_REP + (sets - 1).coerceAtLeast(0) * DEFAULT_REST_SECONDS + TRANSITION_SECONDS
                }
                is ResolvedGrouping.Paired -> {
                    val rounds = supersetRounds(grouping.first, grouping.second)
                    val repsA = midpointReps(grouping.first.targetRepsMin, grouping.first.targetRepsMax)
                    val repsB = midpointReps(grouping.second.targetRepsMin, grouping.second.targetRepsMax)
                    rounds * (repsA + repsB) * SECONDS_PER_REP + (rounds - 1).coerceAtLeast(0) * DEFAULT_REST_SECONDS + TRANSITION_SECONDS * 2
                }
            }
        }
        return (totalSeconds / 60).coerceAtLeast(1)
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
                totalRounds = supersetRounds(a, b),
            ),
        )
}
