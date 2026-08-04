package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.MealEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionCalculatorTest {

    private fun meal(kcal: Int, protein: Int, carb: Int, fat: Int) =
        MealEntity(epochDay = 0, slot = "Bữa phụ", nameVi = "Test", kcal = kcal, proteinG = protein, carbG = carb, fatG = fat)

    @Test
    fun `no meals sums to zero`() {
        val totals = NutritionCalculator.compute(emptyList())

        assertEquals(0, totals.kcal)
        assertEquals(0, totals.proteinG)
        assertEquals(0, totals.carbG)
        assertEquals(0, totals.fatG)
    }

    @Test
    fun `totals sum every logged meal`() {
        val totals = NutritionCalculator.compute(
            listOf(meal(452, 30, 55, 12), meal(618, 42, 78, 14)),
        )

        assertEquals(1070, totals.kcal)
        assertEquals(72, totals.proteinG)
        assertEquals(133, totals.carbG)
        assertEquals(26, totals.fatG)
    }

    @Test
    fun `percent is the value's share of its goal`() {
        val totals = NutritionTotals(kcal = 1100, proteinG = 70, carbG = 125, fatG = 35)

        assertEquals(50, totals.kcalPercent)
        assertEquals(50, totals.proteinPercent)
        assertEquals(50, totals.carbPercent)
        assertEquals(50, totals.fatPercent)
    }

    @Test
    fun `percent caps at 100 once a goal is exceeded`() {
        val totals = NutritionTotals(kcal = 5000, proteinG = 999, carbG = 999, fatG = 999)

        assertEquals(100, totals.kcalPercent)
        assertEquals(100, totals.proteinPercent)
        assertEquals(100, totals.carbPercent)
        assertEquals(100, totals.fatPercent)
    }
}
