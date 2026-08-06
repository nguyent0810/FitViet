package com.fitviet.app.domain

import java.time.LocalDate

data class LoggedSetPoint(val date: LocalDate, val weightKg: Double, val reps: Int)

data class ExerciseHistoryEntry(val date: LocalDate, val weightKg: Double, val reps: Int)

/** Pure, unit-testable — same rationale as [DashboardStatsCalculator]. Feature #10 (Gate 46). */
object ExerciseHistoryCalculator {
    /** One entry per distinct date the exercise was logged on, newest first, keeping only that
     * date's heaviest set (ties broken by more reps) — matches this app's existing
     * heaviest-weight-logged personal-best convention (see
     * [com.fitviet.app.data.repository.WorkoutRepository.getRecommendedWeight]) rather than
     * averaging or listing every individual set. */
    fun bestSetPerDate(sets: List<LoggedSetPoint>): List<ExerciseHistoryEntry> =
        sets.groupBy { it.date }
            .map { (date, daySets) ->
                val best = daySets.maxWith(compareBy({ it.weightKg }, { it.reps }))
                ExerciseHistoryEntry(date, best.weightKg, best.reps)
            }
            .sortedByDescending { it.date }
}
