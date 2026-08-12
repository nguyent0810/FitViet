package com.fitviet.app.data.local.seed

import com.fitviet.app.domain.RecipeIngredientWithFood
import com.fitviet.app.domain.RecipeNutritionCalculator
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gate B4 sanity check — pure JVM, no Room: catches an ingredient-seed typo (a [SeedData.RecipeSeed]
 * referencing a food name that doesn't exist in [SeedData.foods]) and a wildly-off gram quantity
 * (a recipe whose computed per-serving kcal falls way outside a plausible single-meal range)
 * before either would ever surface as a silently-wrong number in the UI.
 */
class SeedDataRecipesTest {

    private val foodsByName = SeedData.foods.associateBy { it.nameVi }

    @Test
    fun `every recipe ingredient references a real food name`() {
        val missing = SeedData.recipes.flatMap { recipe ->
            recipe.ingredients
                .filter { it.foodName !in foodsByName }
                .map { "${recipe.nameVi} -> ${it.foodName}" }
        }
        assertTrue("Unresolvable ingredient food names: $missing", missing.isEmpty())
    }

    @Test
    fun `every recipe has at least one ingredient and a STANDARD variant`() {
        SeedData.recipes.forEach { recipe ->
            assertTrue("${recipe.nameVi} has no ingredients", recipe.ingredients.isNotEmpty())
            assertTrue("${recipe.nameVi} has no STANDARD variant", recipe.variants.any { it.code == "STANDARD" })
        }
    }

    @Test
    fun `every recipe's computed per-serving kcal is a plausible single-meal amount`() {
        SeedData.recipes.forEach { recipe ->
            val ingredients = recipe.ingredients.mapNotNull { seed ->
                foodsByName[seed.foodName]?.let { RecipeIngredientWithFood(grams = seed.grams, food = it) }
            }
            val base = RecipeNutritionCalculator.computeBaseTotals(ingredients)
            val standardVariant = null // STANDARD's multipliers are all 1.0, same result as no variant
            val perServing = RecipeNutritionCalculator.computePerServing(
                base, recipe.baseServings, standardVariant, servings = 1.0,
            )

            assertTrue(
                "${recipe.nameVi} computed kcal ${perServing.kcal} is outside a plausible 50..1200 single-serving range",
                perServing.kcal in 50..1200,
            )
        }
    }
}
