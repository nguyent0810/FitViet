package com.fitviet.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreakMilestonesTest {

    @Test
    fun `no milestone reached yet returns null`() {
        assertNull(StreakMilestones.crossedMilestone(streakDays = 5, lastCelebrated = 0))
    }

    @Test
    fun `exactly hitting a milestone for the first time returns it`() {
        assertEquals(7, StreakMilestones.crossedMilestone(streakDays = 7, lastCelebrated = 0))
    }

    @Test
    fun `already-celebrated milestone is not reported again`() {
        assertNull(StreakMilestones.crossedMilestone(streakDays = 7, lastCelebrated = 7))
    }

    @Test
    fun `streak past a milestone without having celebrated it yet still reports it`() {
        assertEquals(7, StreakMilestones.crossedMilestone(streakDays = 10, lastCelebrated = 0))
    }

    @Test
    fun `skipping past multiple milestones between opens reports only the highest`() {
        assertEquals(30, StreakMilestones.crossedMilestone(streakDays = 45, lastCelebrated = 0))
    }

    @Test
    fun `a broken and restarted streak does not re-earn a milestone already reached before the break`() {
        // lastCelebrated tracks the highest streak ever shown, lifetime — it is never rolled back
        // when a streak breaks. A user who once hit 30 days, broke their streak, and rebuilt to 7
        // does NOT see the 7-day overlay again; only exceeding their previous best (30) fires again.
        assertNull(StreakMilestones.crossedMilestone(streakDays = 7, lastCelebrated = 30))
    }

    @Test
    fun `beyond the highest milestone returns null once already celebrated`() {
        assertNull(StreakMilestones.crossedMilestone(streakDays = 61, lastCelebrated = 61))
    }
}
