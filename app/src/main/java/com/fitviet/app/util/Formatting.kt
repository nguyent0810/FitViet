package com.fitviet.app.util

import java.text.NumberFormat
import java.util.Locale

private val viLocale = Locale.forLanguageTag("vi-VN")
private val integerFormatter: NumberFormat = NumberFormat.getIntegerInstance(viLocale)

/** Matches the prototype's `n.toLocaleString('vi-VN')` — e.g. 4120 -> "4.120". */
fun formatVi(value: Int): String = integerFormatter.format(value)

fun formatVi(value: Double): String = integerFormatter.format(Math.round(value))

/** Compact "12,4k" style used for large stat-tile numbers; falls back to the plain integer under 1000. */
fun formatCompactKg(value: Double): String {
    if (value < 1000) return formatVi(value)
    val thousands = value / 1000.0
    return String.format(viLocale, "%.1fk", thousands)
}

/** m:ss countdown/elapsed display, e.g. 75 -> "1:15". */
fun formatMinutesSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

/** Drops a trailing ".0" so integer-valued weights read as "40" not "40.0", matching the prototype. */
fun formatWeight(kg: Double): String =
    if (kg == kg.toLong().toDouble()) kg.toLong().toString() else formatVi(kg)
