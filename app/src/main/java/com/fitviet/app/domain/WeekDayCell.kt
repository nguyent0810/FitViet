package com.fitviet.app.domain

import java.time.LocalDate

/** One of the 7 circles on Dashboard's "Tuần này" card (redesign Gate 2c). [TODAY_DONE]/
 * [TODAY_PENDING] only ever apply to the window's last entry (today); [DONE] to any earlier day
 * with a completed session; [EMPTY] to everything else — a future day and a rest day render
 * identically here, matching the design mock's own live prototype logic (`weekDays` in the
 * interactive HTML never actually branches on a rest flag, only on `done`/`today`). */
enum class WeekDayCellState { TODAY_DONE, TODAY_PENDING, DONE, EMPTY }

data class WeekDayCell(val date: LocalDate, val state: WeekDayCellState)

/**
 * Redesign Gate 2c — both this card and the "Khối lượng" volume chart use the same trailing
 * 7-day-ending-today window, not a calendar Monday-Sunday week (confirmed against the design
 * mock's own interactive prototype: `dayBase`'s 7 entries are built by walking backward from
 * today, and the volume chart's `weekBars` use the identical construction) — so both read the same
 * `trainedDates` set built once per [com.fitviet.app.data.repository.DashboardRepository] emission,
 * no separate query.
 */
object WeekDayCellCalculator {
    fun compute(today: LocalDate, trainedDates: Set<LocalDate>): List<WeekDayCell> =
        (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val isToday = date == today
            val isDone = date in trainedDates
            val state = when {
                isToday && isDone -> WeekDayCellState.TODAY_DONE
                isToday -> WeekDayCellState.TODAY_PENDING
                isDone -> WeekDayCellState.DONE
                else -> WeekDayCellState.EMPTY
            }
            WeekDayCell(date, state)
        }
}
