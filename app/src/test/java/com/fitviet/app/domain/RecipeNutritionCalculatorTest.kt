package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.FoodEntity
import com.fitviet.app.data.local.entity.RecipeVariantEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeNutritionCalculatorTest {

    private fun food(kcal: Int, protein: Double, carb: Double, fat: Double) = FoodEntity(
        nameVi = "Test", nameEn = "Test", category = "Đạm", descriptionVi = "",
        kcalPer100g = kcal, proteinG = protein, carbG = carb, fatG = fat,
    )

    @Test
    fun `no ingredients sums to zero`() {
        val totals = RecipeNutritionCalculator.computeBaseTotals(emptyList())

        assertEquals(0, totals.kcal)
        assertEquals(0, totals.proteinG)
        assertEquals(0, totals.carbG)
        assertEquals(0, totals.fatG)
    }

    @Test
    fun `base totals scale each ingredient by grams over 100`() {
        // 180g chicken breast (165 kcal, 31p, 0c, 3.6f per 100g) + 150g rice (130 kcal, 2.7p, 28c, 0.3f per 100g)
        val totals = RecipeNutritionCalculator.computeBaseTotals(
            listOf(
                RecipeIngredientWithFood(grams = 180.0, food = food(165, 31.0, 0.0, 3.6)),
                RecipeIngredientWithFood(grams = 150.0, food = food(130, 2.7, 28.0, 0.3)),
            ),
        )

        // 165*1.8 + 130*1.5 = 297 + 195 = 492
        assertEquals(492, totals.kcal)
        // 31*1.8 + 2.7*1.5 = 55.8 + 4.05 = 59.85 -> rounds to 60
        assertEquals(60, totals.proteinG)
        // 0*1.8 + 28*1.5 = 42
        assertEquals(42, totals.carbG)
        // 3.6*1.8 + 0.3*1.5 = 6.48 + 0.45 = 6.93 -> rounds to 7
        assertEquals(7, totals.fatG)
    }

    @Test
    fun `standard variant with matching servings is a no-op`() {
        val base = NutritionTotals(kcal = 492, proteinG = 60, carbG = 42, fatG = 7)
        val standard = RecipeVariantEntity(recipeId = 1, code = "STANDARD")

        val perServing = RecipeNutritionCalculator.computePerServing(base, baseServings = 1, variant = standard, servings = 1.0)

        assertEquals(base, perServing)
    }

    @Test
    fun `servings scaling divides by base servings then multiplies by chosen servings`() {
        val base = NutritionTotals(kcal = 600, proteinG = 40, carbG = 60, fatG = 20)
        val standard = RecipeVariantEntity(recipeId = 1, code = "STANDARD")

        // Recipe as written makes 2 servings; user wants 3.
        val perServing = RecipeNutritionCalculator.computePerServing(base, baseServings = 2, variant = standard, servings = 3.0)

        assertEquals(900, perServing.kcal)
        assertEquals(60, perServing.proteinG)
        assertEquals(90, perServing.carbG)
        assertEquals(30, perServing.fatG)
    }

    @Test
    fun `variant multipliers apply on top of servings scaling`() {
        val base = NutritionTotals(kcal = 400, proteinG = 30, carbG = 40, fatG = 10)
        val highProtein = RecipeVariantEntity(
            recipeId = 1, code = "HIGH_PROTEIN",
            kcalMultiplier = 1.0, proteinMultiplier = 1.35, carbMultiplier = 1.0, fatMultiplier = 1.0,
        )

        val perServing = RecipeNutritionCalculator.computePerServing(base, baseServings = 1, variant = highProtein, servings = 1.0)

        assertEquals(400, perServing.kcal)
        // 30 * 1.35 = 40.5 -> rounds to 41 (Kotlin's roundToInt uses round-half-up for positives)
        assertEquals(41, perServing.proteinG)
        assertEquals(40, perServing.carbG)
        assertEquals(10, perServing.fatG)
    }

    @Test
    fun `null variant behaves the same as an all-1 multiplier set`() {
        val base = NutritionTotals(kcal = 500, proteinG = 35, carbG = 50, fatG = 15)

        val perServing = RecipeNutritionCalculator.computePerServing(base, baseServings = 1, variant = null, servings = 1.0)

        assertEquals(base, perServing)
    }
}
