package com.fitviet.app.util

import androidx.annotation.StringRes
import com.fitviet.app.R
import java.time.DayOfWeek

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
