package com.fitviet.app.domain

/**
 * Gate D4 — the fixed set of workout-streak lengths (in days) that trigger a celebration overlay.
 * Pure/testable, no Room/Android imports, same shape as this app's other small domain calculators.
 *
 * [lastCelebrated] tracks the highest streak a milestone has ever been shown for, not "the
 * highest milestone of the user's *current* streak run" — it's never rolled back when a streak
 * breaks (see [com.fitviet.app.domain.DashboardStatsCalculator.currentStreak]). So a user who once
 * reached a 30-day streak, breaks it, and rebuilds to 7 will NOT see the 7-day overlay again: it's
 * treated as a personal-best tracker across the app's lifetime, not a per-run counter. This is a
 * deliberate v1 simplification — re-firing lower milestones after a break would need the caller to
 * detect "streak just reset to 0", which this pure function has no way to see from streakDays alone.
 */
object StreakMilestones {
    val MILESTONES = listOf(7, 14, 30, 60)

    /**
     * The highest milestone that [streakDays] has reached but [lastCelebrated] hasn't yet, or null
     * if none has. Returns the highest rather than every newly-crossed one so a user who skips a
     * few days of app opens (streak jumps past more than one milestone at once) sees a single
     * overlay for their current milestone instead of a backlog of stale ones.
     */
    fun crossedMilestone(streakDays: Int, lastCelebrated: Int): Int? =
        MILESTONES.filter { it in (lastCelebrated + 1)..streakDays }.maxOrNull()
}
