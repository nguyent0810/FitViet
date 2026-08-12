package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.FoodEntity
import com.fitviet.app.data.local.entity.RecipeVariantEntity
import kotlin.math.roundToInt

/** One resolved ingredient line — [grams] used paired with its [food] row, so the calculator
 * never needs its own Room dependency. */
data class RecipeIngredientWithFood(val grams: Double, val food: FoodEntity)

/**
 * Deterministic recipe nutrition — every number here is derived from a recipe's ingredient rows
 * (grams × [FoodEntity]'s per-100g values), never hardcoded or AI-estimated, per the Nutrition
 * backend's core requirement. Reuses [NutritionTotals] (this package's existing plain
 * kcal/protein/carb/fat shape from the food-log feature) — its `kcalPercent`/etc. getters (which
 * compare against the fixed daily [NutritionGoals]) are simply unused here; they're not a
 * meaningful concept for a single recipe or serving.
 */
object RecipeNutritionCalculator {
    /** Sums grams/100 × each ingredient's per-100g macros — the recipe's *base* totals (all of
     * `RecipeEntity.baseServings` combined, before any variant multiplier). */
    fun computeBaseTotals(ingredients: List<RecipeIngredientWithFood>): NutritionTotals {
        var kcal = 0.0
        var protein = 0.0
        var carb = 0.0
        var fat = 0.0
        ingredients.forEach { line ->
            val fraction = line.grams / 100.0
            kcal += line.food.kcalPer100g * fraction
            protein += line.food.proteinG * fraction
            carb += line.food.carbG * fraction
            fat += line.food.fatG * fraction
        }
        return NutritionTotals(
            kcal = kcal.roundToInt(),
            proteinG = protein.roundToInt(),
            carbG = carb.roundToInt(),
            fatG = fat.roundToInt(),
        )
    }

    /** Divides [base] by [baseServings] to get a true "per serving" figure, applies [variant]'s 4
     * multipliers (null = no-op, same as the `STANDARD` variant's all-`1.0` row), then scales by
     * the user-chosen [servings] — the single source of truth a servings-stepper UI reads from,
     * so dragging it recomputes kcal/macros live with no repository round-trip. */
    fun computePerServing(
        base: NutritionTotals,
        baseServings: Int,
        variant: RecipeVariantEntity?,
        servings: Double,
    ): NutritionTotals {
        val scale = servings / baseServings.coerceAtLeast(1)
        val kcalMultiplier = variant?.kcalMultiplier ?: 1.0
        val proteinMultiplier = variant?.proteinMultiplier ?: 1.0
        val carbMultiplier = variant?.carbMultiplier ?: 1.0
        val fatMultiplier = variant?.fatMultiplier ?: 1.0

        return NutritionTotals(
            kcal = (base.kcal * scale * kcalMultiplier).roundToInt(),
            proteinG = (base.proteinG * scale * proteinMultiplier).roundToInt(),
            carbG = (base.carbG * scale * carbMultiplier).roundToInt(),
            fatG = (base.fatG * scale * fatMultiplier).roundToInt(),
        )
    }
}
