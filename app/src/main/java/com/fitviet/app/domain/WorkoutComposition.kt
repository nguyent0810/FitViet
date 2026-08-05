package com.fitviet.app.domain

import java.time.LocalDate

/** One completed set, already resolved to a calendar [date] by the repository and joined to its
 * exercise's stable classification codes — mirrors [CompletedSession]'s "repository resolves
 * dates/joins, domain stays pure" split. */
data class CompletedSet(
    val date: LocalDate,
    val muscleGroupCode: String,
    val movementType: String,
    val volumeKg: Double,
)

data class MuscleGroupWorkload(val muscleGroup: MuscleGroup, val volumeKg: Double, val setCount: Int)

data class MovementTypeDistribution(val compoundSets: Int, val isolationSets: Int) {
    private val totalSets: Int get() = compoundSets + isolationSets

    /** 0f when there are no sets at all — avoids a divide-by-zero rather than propagating NaN. */
    val compoundFraction: Float get() = if (totalSets == 0) 0f else compoundSets.toFloat() / totalSets
    val isolationFraction: Float get() = if (totalSets == 0) 0f else isolationSets.toFloat() / totalSets
}

/**
 * Groups completed sets by [MuscleGroup] (feature #8, a workload chart) and by [MovementType]
 * (feature #9, compound-vs-isolation distribution) over a trailing window. Pure and testable — no
 * Room/Compose dependency, mirrors the [DiaryStatsCalculator]/[WorkoutCalendarCalculator] pattern
 * of keeping join/grouping/date-window logic out of the repository's reactive `combine {}`.
 */
object WorkoutCompositionCalculator {

    /** Always returns exactly [MuscleGroup.entries]-many rows, in enum order, including groups
     * with zero sets in the window — so the chart has a stable set of bars to draw every time
     * rather than the list's length changing as training habits change. A [CompletedSet] whose
     * [CompletedSet.muscleGroupCode] doesn't match any known [MuscleGroup] (shouldn't happen given
     * how [com.fitviet.app.data.local.entity.ExerciseEntity] is seeded, but isn't enforced by the
     * database) is silently excluded rather than crashing a read-only chart. */
    fun muscleGroupWorkload(sets: List<CompletedSet>, since: LocalDate): List<MuscleGroupWorkload> {
        val byGroup = sets.asSequence().filter { it.date >= since }.groupBy { it.muscleGroupCode }
        return MuscleGroup.entries.map { group ->
            val groupSets = byGroup[group.name].orEmpty()
            MuscleGroupWorkload(
                muscleGroup = group,
                volumeKg = groupSets.sumOf { it.volumeKg },
                setCount = groupSets.size,
            )
        }
    }

    fun movementTypeDistribution(sets: List<CompletedSet>, since: LocalDate): MovementTypeDistribution {
        val inWindow = sets.filter { it.date >= since }
        return MovementTypeDistribution(
            compoundSets = inWindow.count { it.movementType == MovementType.COMPOUND.name },
            isolationSets = inWindow.count { it.movementType == MovementType.ISOLATION.name },
        )
    }
}
