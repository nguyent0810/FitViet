package com.fitviet.app.util

import androidx.annotation.StringRes
import com.fitviet.app.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.WeekFields

/** Short day label (T2..CN) used on the dashboard's 7-day chart and the weekly schedule rows. */
@StringRes
fun DayOfWeek.shortLabelRes(): Int = when (this) {
    DayOfWeek.MONDAY -> R.string.day_mon
    DayOfWeek.TUESDAY -> R.string.day_tue
    DayOfWeek.WEDNESDAY -> R.string.day_wed
    DayOfWeek.THURSDAY -> R.string.day_thu
    DayOfWeek.FRIDAY -> R.string.day_fri
    DayOfWeek.SATURDAY -> R.string.day_sat
    DayOfWeek.SUNDAY -> R.string.day_sun
}

/** Long day label (Thứ Hai..Chủ Nhật) used on the dashboard greeting line. */
@StringRes
fun DayOfWeek.longLabelRes(): Int = when (this) {
    DayOfWeek.MONDAY -> R.string.day_long_mon
    DayOfWeek.TUESDAY -> R.string.day_long_tue
    DayOfWeek.WEDNESDAY -> R.string.day_long_wed
    DayOfWeek.THURSDAY -> R.string.day_long_thu
    DayOfWeek.FRIDAY -> R.string.day_long_fri
    DayOfWeek.SATURDAY -> R.string.day_long_sat
    DayOfWeek.SUNDAY -> R.string.day_long_sun
}

/** ISO week-of-year number, e.g. for the "T.32"/"W32" week-bucket labels on the Diary and (Gate
 * 43) Dashboard weekly-volume charts. Moved here from a private duplicate in `DiaryScreen.kt` when
 * Dashboard needed the exact same label for its own week-bucketed range views. */
fun LocalDate.isoWeekNumber(): Int = this.get(WeekFields.ISO.weekOfWeekBasedYear())

/** Month label ("Tháng 8" / "August") used by the workout calendar's month header. */
@StringRes
fun Month.labelRes(): Int = when (this) {
    Month.JANUARY -> R.string.month_1
    Month.FEBRUARY -> R.string.month_2
    Month.MARCH -> R.string.month_3
    Month.APRIL -> R.string.month_4
    Month.MAY -> R.string.month_5
    Month.JUNE -> R.string.month_6
    Month.JULY -> R.string.month_7
    Month.AUGUST -> R.string.month_8
    Month.SEPTEMBER -> R.string.month_9
    Month.OCTOBER -> R.string.month_10
    Month.NOVEMBER -> R.string.month_11
    Month.DECEMBER -> R.string.month_12
}
