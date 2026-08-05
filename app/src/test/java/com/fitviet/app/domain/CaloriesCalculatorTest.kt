package com.fitviet.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CaloriesCalculatorTest {

    @Test
    fun `zero duration estimates zero calories`() {
        assertEquals(0, CaloriesCalculator.estimateKcal(0))
    }

    @Test
    fun `a negative duration is treated as zero rather than going negative`() {
        assertEquals(0, CaloriesCalculator.estimateKcal(-100))
    }

    @Test
    fun `estimates a plausible calorie count for a 45-minute session`() {
        // 45 min * (5.0 MET * 3.5 * 70kg / 200) kcal/min = 45 * 6.125 = 275.625 -> rounds to 276.
        assertEquals(276, CaloriesCalculator.estimateKcal(45 * 60))
    }

    @Test
    fun `longer sessions estimate proportionally more calories`() {
        val thirtyMin = CaloriesCalculator.estimateKcal(30 * 60)
        val sixtyMin = CaloriesCalculator.estimateKcal(60 * 60)
        assertEquals(true, sixtyMin > thirtyMin)
    }
}
