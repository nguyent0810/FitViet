package com.fitviet.app.domain

import java.time.DayOfWeek

/**
 * The next non-rest day in the active program's schedule, found by scanning forward from [today]
 * inclusive (wraps into next week if nothing training-worthy remains this week) — feature #3's
 * dynamic "what's next" content for the 1b hero card.
 */
data class NextTraining(val day: ProgramScheduleDay, val isToday: Boolean)

object NextTrainingCalculator {
    fun findNext(schedule: List<ProgramScheduleDay>, today: DayOfWeek): NextTraining? {
        val byDayOfWeek = schedule.filterNot { it.isRestDay }.associateBy { it.dayOfWeek }
        if (byDayOfWeek.isEmpty()) return null
        for (offset in 0..6) {
            val candidate = today.plus(offset.toLong())
            byDayOfWeek[candidate]?.let { return NextTraining(day = it, isToday = offset == 0) }
        }
        return null // unreachable once byDayOfWeek isn't empty — 7 offsets cover every DayOfWeek
    }
}

/**
 * This week's session count vs. the active program's weekly target — feature #3's "completion %".
 * Deliberately session-count-based, not tied to specific scheduled days: no completed
 * [com.fitviet.app.data.local.entity.WorkoutSessionEntity] is currently linked back to which
 * program day it was for (that would need the workout-start flow to know which day was tapped —
 * a larger change than this feature's scope; see PROGRESS.md). So this counts "did you train this
 * many times this week," not "did you do the specific prescribed exercises on the right days."
 */
data class ProgramProgress(val completedThisWeek: Int, val targetPerWeek: Int) {
    val fraction: Float
        get() = if (targetPerWeek <= 0) 0f else (completedThisWeek.toFloat() / targetPerWeek).coerceIn(0f, 1f)
}
