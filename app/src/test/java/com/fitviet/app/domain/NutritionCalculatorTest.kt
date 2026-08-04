package com.fitviet.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionCalculatorTest {

    @Test
    fun `sums macros and kcal across all meals`() {
        val meals = listOf(
            MealMacros(kcal = 452, proteinG = 30, carbG = 55, fatG = 12),
            MealMacros(kcal = 618, proteinG = 42, carbG = 78, fatG = 14),
        )

        val stats = NutritionCalculator.compute(meals)

        assertEquals(1070, stats.kcalTotal)
        assertEquals(72, stats.proteinG)
        assertEquals(133, stats.carbG)
        assertEquals(26, stats.fatG)
    }

    @Test
    fun `percentages are computed against the fixed goals and capped at 100`() {
        val meals = listOf(MealMacros(kcal = 4400, proteinG = 280, carbG = 500, fatG = 140))

        val stats = NutritionCalculator.compute(meals)

        assertEquals(100, stats.kcalPct) // 4400/2200 would be 200%, capped
        assertEquals(100, stats.proteinPct)
        assertEquals(100, stats.carbPct)
        assertEquals(100, stats.fatPct)
    }

    @Test
    fun `empty meal list is zero across the board, not a division error`() {
        val stats = NutritionCalculator.compute(emptyList())

        assertEquals(0, stats.kcalTotal)
        assertEquals(0, stats.kcalPct)
        assertEquals(NutritionCalculator.KCAL_GOAL, stats.kcalGoal)
    }
}
