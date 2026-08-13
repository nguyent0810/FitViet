package com.fitviet.app.domain

import java.time.LocalDate

/** One completed set, already resolved to a calendar [date] and exercise identity by the
 * repository — same "repository resolves dates/joins, domain stays pure" split as [CompletedSet]. */
data class ExerciseSetRecord(val exerciseId: Long, val date: LocalDate, val weightKg: Double)

/**
 * Redesign Gate 2c — the Dashboard "PR" stat card: how many personal records were set in the
 * trailing 7-day window (same rolling window `WeekDayCellCalculator`/the volume chart use, not a
 * calendar Mon-Sun week — see that file's own doc for why). No PR-event history is persisted
 * anywhere in this app (`SetLogDao.getPersonalBest` only ever answers "what's the current max,"
 * never "when was it set" or "was there an earlier one this week too"), so this reconstructs PR
 * events from raw set history: per exercise, collapse to one max weight per calendar day (the
 * source query only carries the *session's* completion timestamp, so every set within a session
 * shares one date — without this collapse, ramping 60/70/80kg in a single session would count as 3
 * PRs instead of 1, and since the source has no `ORDER BY`, which of those rows counts would be
 * whatever arbitrary order SQLite returned), then walk those per-day maxima chronologically and
 * count each day whose max exceeds every prior day's max for that exercise, restricted to days
 * whose date falls in the window. A `hasBaseline` flag (not "was the running max > 0") tracks
 * whether *any* prior day exists at all — using the running max itself as the sentinel would wrongly
 * treat an exercise's first-ever *weighted* set as a PR when its very first logged day was a 0kg
 * bodyweight set (e.g. the first belted pull-up after weeks of bodyweight-only reps). An exercise's
 * very first-ever logged day is never a "PR" by this definition — there's nothing yet for it to have
 * beaten.
 */
object PersonalRecordCalculator {
    fun countInWindow(sets: List<ExerciseSetRecord>, windowStart: LocalDate, windowEnd: LocalDate): Int {
        val byExercise = sets.groupBy { it.exerciseId }
        var count = 0
        for ((_, exerciseSets) in byExercise) {
            val maxByDate = exerciseSets.groupBy { it.date }.mapValues { (_, daySets) -> daySets.maxOf { it.weightKg } }
            var runningMax = 0.0
            var hasBaseline = false
            for (date in maxByDate.keys.sorted()) {
                val dayMax = maxByDate.getValue(date)
                val isNewMax = dayMax > runningMax
                if (isNewMax && hasBaseline && date in windowStart..windowEnd) count++
                if (isNewMax) runningMax = dayMax
                hasBaseline = true
            }
        }
        return count
    }
}
