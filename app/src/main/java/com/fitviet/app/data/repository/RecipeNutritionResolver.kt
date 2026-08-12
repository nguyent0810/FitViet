package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.FoodDao
import com.fitviet.app.data.local.dao.RecipeIngredientDao
import com.fitviet.app.data.local.dao.RecipeVariantDao
import com.fitviet.app.data.local.entity.RecipeEntity
import com.fitviet.app.domain.NutritionTotals
import com.fitviet.app.domain.RecipeIngredientWithFood
import com.fitviet.app.domain.RecipeNutritionCalculator
import com.fitviet.app.domain.RecipeWithNutrition

/** Resolves one [RecipeEntity] into the fully-computed [RecipeWithNutrition] shape both
 * [RecipeRepository] and [MealPlanRepository] independently need (Nutrition Gate B9) — extracted
 * here rather than duplicated, since "recipe -> ingredients -> foods -> computed totals" is the
 * same real query/computation regardless of which repository is asking. */
internal suspend fun resolveRecipeWithNutrition(
    recipe: RecipeEntity,
    recipeIngredientDao: RecipeIngredientDao,
    recipeVariantDao: RecipeVariantDao,
    foodDao: FoodDao,
): RecipeWithNutrition {
    val ingredients = recipeIngredientDao.getForRecipe(recipe.id)
    val foodsById = foodDao.getByIds(ingredients.map { it.foodId }).associateBy { it.id }
    val ingredientsWithFood = ingredients.mapNotNull { ingredient ->
        foodsById[ingredient.foodId]?.let { food -> RecipeIngredientWithFood(ingredient.grams, food) }
    }
    val baseTotals = RecipeNutritionCalculator.computeBaseTotals(ingredientsWithFood)
    val variants = recipeVariantDao.getForRecipe(recipe.id)
    val standardVariant = variants.firstOrNull { it.code == "STANDARD" }
    val standardTotals = RecipeNutritionCalculator.computePerServing(baseTotals, recipe.baseServings, standardVariant, servings = 1.0)
    return RecipeWithNutrition(
        recipeId = recipe.id,
        nameVi = recipe.nameVi,
        category = recipe.category,
        tags = recipe.tags,
        cookTimeMinutes = recipe.cookTimeMinutes,
        ingredientFoodIds = ingredients.map { it.foodId }.toSet(),
        standardTotals = standardTotals,
        variants = variants,
    )
}

/** The swap/apply-swap path's narrower need: totals for one specific recipe+variant (not the
 * whole [RecipeWithNutrition] shape), used to snapshot [com.fitviet.app.data.local.entity.MealPlanMealEntity]'s
 * kcal/proteinG/carbG/fatG columns at swap time. Returns null if the recipe has no ingredients. */
internal suspend fun resolveRecipeTotalsForVariant(
    recipeId: Long,
    variantCode: String,
    baseServings: Int,
    recipeIngredientDao: RecipeIngredientDao,
    recipeVariantDao: RecipeVariantDao,
    foodDao: FoodDao,
): NutritionTotals? {
    val ingredients = recipeIngredientDao.getForRecipe(recipeId)
    if (ingredients.isEmpty()) return null
    val foodsById = foodDao.getByIds(ingredients.map { it.foodId }).associateBy { it.id }
    val ingredientsWithFood = ingredients.mapNotNull { ingredient ->
        foodsById[ingredient.foodId]?.let { food -> RecipeIngredientWithFood(ingredient.grams, food) }
    }
    val baseTotals = RecipeNutritionCalculator.computeBaseTotals(ingredientsWithFood)
    val variant = recipeVariantDao.getForRecipe(recipeId).firstOrNull { it.code == variantCode }
    return RecipeNutritionCalculator.computePerServing(baseTotals, baseServings, variant, servings = 1.0)
}
