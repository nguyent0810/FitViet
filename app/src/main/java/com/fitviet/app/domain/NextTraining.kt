package com.fitviet.app.domain

import java.time.DayOfWeek

/**
 * The next non-rest day in a program's schedule, found by scanning forward from [today] inclusive
 * (wraps into next week if nothing training-worthy remains this week) — feature #3's original
 * "what's next" content for the pre-redesign hero card; redesign Gate 2b repurposed it for
 * [com.fitviet.app.ui.workout.WorkoutPreviewViewModel]'s "Xem trước" lookahead (today if
 * trainable, else the nearest upcoming training day) once the old hero card/Weekly Schedule that
 * originally called this were retired.
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
