package com.fitviet.app.ui.programs

import androidx.annotation.StringRes
import com.fitviet.app.R
import java.time.DayOfWeek

/**
 * The PPL 6-day weekly schedule shown on 2b. This is static reference content matching the
 * prototype exactly — programs don't yet have a real per-day exercise assignment in Room
 * (see PROGRESS.md, Gate 3), so every program's schedule tab shows this same reference week.
 */
data class ScheduleDay(
    val dayOfWeek: DayOfWeek,
    @StringRes val titleRes: Int,
    @StringRes val subRes: Int,
    val exerciseCount: Int?,
    val isRest: Boolean = false,
)

val WEEKLY_SCHEDULE = listOf(
    ScheduleDay(DayOfWeek.MONDAY, R.string.schedule_day_push1_title, R.string.schedule_day_push1_sub, 6),
    ScheduleDay(DayOfWeek.TUESDAY, R.string.schedule_day_pull1_title, R.string.schedule_day_pull1_sub, 6),
    ScheduleDay(DayOfWeek.WEDNESDAY, R.string.schedule_day_legs1_title, R.string.schedule_day_legs1_sub, 5),
    ScheduleDay(DayOfWeek.THURSDAY, R.string.schedule_day_push2_title, R.string.schedule_day_push2_sub, 6),
    ScheduleDay(DayOfWeek.FRIDAY, R.string.schedule_day_pull2_title, R.string.schedule_day_pull2_sub, 6),
    ScheduleDay(DayOfWeek.SATURDAY, R.string.schedule_day_legs2_title, R.string.schedule_day_legs2_sub, 5),
    ScheduleDay(DayOfWeek.SUNDAY, R.string.schedule_day_rest_title, R.string.schedule_day_rest_sub, null, isRest = true),
)
