package com.fitviet.app.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** One "today's tip" recommendation shown on the dashboard. */
sealed class Recommendation {
    /** No completed session anywhere in the last 7 days. */
    data object ComeBackReminder : Recommendation()

    /** Currently on a streak of at least [RecommendationCalculator.STREAK_PRAISE_THRESHOLD_DAYS]. */
    data class StreakPraise(val streakDays: Int) : Recommendation()

    /** No measurement check-in in at least [RecommendationCalculator.MEASUREMENT_STALE_THRESHOLD_DAYS]
     * days, or ever (null). */
    data class MeasurementReminder(val daysSinceLastCheckIn: Long?) : Recommendation()

    /** Fallback when no other rule matches — a rotating general fitness tip, deterministic by date
     * so it's stable for the whole day and predictable/testable, not random. */
    data class GenericTip(val tipIndex: Int) : Recommendation()
}

/**
 * Rule-based, fully transparent "today's tip" for the dashboard — a fixed priority order over a
 * handful of deterministic signals already available from existing dashboard data, no ML/black-box
 * scoring. Returns exactly one recommendation (the highest-priority rule that matches), so the UI
 * never has to choose between competing tips itself.
 */
object RecommendationCalculator {
    const val STREAK_PRAISE_THRESHOLD_DAYS = 3
    const val MEASUREMENT_STALE_THRESHOLD_DAYS = 14L

    /** Must match the number of tip strings the UI layer has for [Recommendation.GenericTip]. */
    const val GENERIC_TIP_COUNT = 8

    fun compute(
        today: LocalDate,
        last7Days: List<DayVolume>,
        streakDays: Int,
        lastMeasurementDate: LocalDate?,
    ): Recommendation {
        val trainedInLast7Days = last7Days.any { it.volumeKg > 0.0 }
        if (!trainedInLast7Days) return Recommendation.ComeBackReminder

        if (streakDays >= STREAK_PRAISE_THRESHOLD_DAYS) return Recommendation.StreakPraise(streakDays)

        val daysSinceMeasurement = lastMeasurementDate?.let { ChronoUnit.DAYS.between(it, today) }
        if (daysSinceMeasurement == null || daysSinceMeasurement >= MEASUREMENT_STALE_THRESHOLD_DAYS) {
            return Recommendation.MeasurementReminder(daysSinceMeasurement)
        }

        // Deterministic by date (not random) so the tip is stable all day and reproducible in tests.
        val tipIndex = today.toEpochDay().mod(GENERIC_TIP_COUNT.toLong()).toInt()
        return Recommendation.GenericTip(tipIndex)
    }
}
