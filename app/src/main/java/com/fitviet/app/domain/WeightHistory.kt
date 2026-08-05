package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.MeasurementEntity
import java.time.LocalDate

/** A single chartable weight reading. */
data class WeightPoint(val date: LocalDate, val weightKg: Double)

enum class WeightHistoryRange {
    THIRTY_DAYS,
    THREE_MONTHS,
    ALL_TIME,
}

/**
 * Turns raw check-in rows into ascending-by-date chart points for 1i's weight history card.
 * Pure and testable: no Room/Compose dependency.
 */
object WeightHistoryCalculator {
    /**
     * [measurements] is expected newest-first (matches [com.fitviet.app.data.local.dao.MeasurementDao.observeAll]).
     * Rows with no weight reading are dropped. When a day has more than one check-in, only the
     * newest-inserted one for that day is kept (the DAO's `id DESC` tiebreak already puts it first
     * within each day's rows), so the chart shows one point per day, not a jagged same-day cluster.
     */
    fun points(measurements: List<MeasurementEntity>, range: WeightHistoryRange, today: LocalDate = LocalDate.now()): List<WeightPoint> {
        // Inclusive lower bound: THIRTY_DAYS spans exactly 30 calendar dates including today
        // (today.minusDays(30) would wrongly span 31). THREE_MONTHS uses a real calendar-month
        // subtraction rather than a fixed 90-day approximation, so it tracks actual month lengths.
        val cutoffDate = when (range) {
            WeightHistoryRange.THIRTY_DAYS -> today.minusDays(29)
            WeightHistoryRange.THREE_MONTHS -> today.minusMonths(3)
            WeightHistoryRange.ALL_TIME -> null
        }
        val cutoffEpochDay = cutoffDate?.toEpochDay()
        val seenDays = HashSet<Long>()
        val filtered = measurements.asSequence()
            .filter { it.weightKg != null }
            .filter { cutoffEpochDay == null || it.epochDay >= cutoffEpochDay }
            .filter { seenDays.add(it.epochDay) } // newest-first input -> first occurrence per day wins
            .map { WeightPoint(LocalDate.ofEpochDay(it.epochDay), it.weightKg!!) }
            .toList()
        return filtered.sortedBy { it.date.toEpochDay() }
    }
}
