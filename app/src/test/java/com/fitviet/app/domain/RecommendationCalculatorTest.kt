package com.fitviet.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationCalculatorTest {

    private val today = LocalDate.of(2026, 8, 5)

    private fun day(daysAgo: Long, volumeKg: Double) = DayVolume(today.minusDays(daysAgo), volumeKg)

    private val noTrainingLast7Days = (0..6L).map { day(it, 0.0) }
    private val trainedTodayLast7Days = listOf(day(0, 100.0)) + (1..6L).map { day(it, 0.0) }

    @Test
    fun `no completed session in the last 7 days triggers the come-back reminder`() {
        val result = RecommendationCalculator.compute(
            today = today,
            last7Days = noTrainingLast7Days,
            streakDays = 0,
            lastMeasurementDate = today,
        )

        assertEquals(Recommendation.ComeBackReminder, result)
    }

    @Test
    fun `come-back reminder takes priority even with a real streak`() {
        // Streak days can lag a stale last7Days window in edge cases; the 7-day window is the
        // more conservative (harder to fake) signal, so it must win.
        val result = RecommendationCalculator.compute(
            today = today,
            last7Days = noTrainingLast7Days,
            streakDays = 5,
            lastMeasurementDate = today,
        )

        assertEquals(Recommendation.ComeBackReminder, result)
    }

    @Test
    fun `a streak at the threshold triggers streak praise`() {
        val result = RecommendationCalculator.compute(
            today = today,
            last7Days = trainedTodayLast7Days,
            streakDays = RecommendationCalculator.STREAK_PRAISE_THRESHOLD_DAYS,
            lastMeasurementDate = today,
        )

        assertEquals(Recommendation.StreakPraise(RecommendationCalculator.STREAK_PRAISE_THRESHOLD_DAYS), result)
    }

    @Test
    fun `a streak just below the threshold does not trigger streak praise`() {
        val result = RecommendationCalculator.compute(
            today = today,
            last7Days = trainedTodayLast7Days,
            streakDays = RecommendationCalculator.STREAK_PRAISE_THRESHOLD_DAYS - 1,
            lastMeasurementDate = today,
        )

        assertTrue(result !is Recommendation.StreakPraise)
    }

    @Test
    fun `a measurement exactly at the stale threshold triggers the reminder`() {
        val staleDate = today.minusDays(RecommendationCalculator.MEASUREMENT_STALE_THRESHOLD_DAYS)

        val result = RecommendationCalculator.compute(
            today = today,
            last7Days = trainedTodayLast7Days,
            streakDays = 0,
            lastMeasurementDate = staleDate,
        )

        assertEquals(Recommendation.MeasurementReminder(RecommendationCalculator.MEASUREMENT_STALE_THRESHOLD_DAYS), result)
    }

    @Test
    fun `a measurement one day inside the stale threshold does not trigger the reminder`() {
        val freshDate = today.minusDays(RecommendationCalculator.MEASUREMENT_STALE_THRESHOLD_DAYS - 1)

        val result = RecommendationCalculator.compute(
            today = today,
            last7Days = trainedTodayLast7Days,
            streakDays = 0,
            lastMeasurementDate = freshDate,
        )

        assertTrue(result is Recommendation.GenericTip)
    }

    @Test
    fun `never having a measurement triggers the reminder`() {
        val result = RecommendationCalculator.compute(
            today = today,
            last7Days = trainedTodayLast7Days,
            streakDays = 0,
            lastMeasurementDate = null,
        )

        assertEquals(Recommendation.MeasurementReminder(null), result)
    }

    @Test
    fun `with no other rule triggered, the generic tip index is deterministic and in range`() {
        val result = RecommendationCalculator.compute(
            today = today,
            last7Days = trainedTodayLast7Days,
            streakDays = 0,
            lastMeasurementDate = today,
        )

        assertTrue(result is Recommendation.GenericTip)
        val tipIndex = (result as Recommendation.GenericTip).tipIndex
        assertTrue(tipIndex in 0 until RecommendationCalculator.GENERIC_TIP_COUNT)
        // Same inputs -> same tip, every time (not random).
        val repeat = RecommendationCalculator.compute(today, trainedTodayLast7Days, 0, today)
        assertEquals(result, repeat)
    }

    @Test
    fun `the generic tip index changes from one day to the next`() {
        val resultToday = RecommendationCalculator.compute(today, trainedTodayLast7Days, 0, today) as Recommendation.GenericTip
        val tomorrow = today.plusDays(1)
        val resultTomorrow = RecommendationCalculator.compute(tomorrow, trainedTodayLast7Days, 0, tomorrow) as Recommendation.GenericTip

        assertEquals((resultToday.tipIndex + 1) % RecommendationCalculator.GENERIC_TIP_COUNT, resultTomorrow.tipIndex)
    }
}
