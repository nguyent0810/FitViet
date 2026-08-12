package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.RecipeVariantEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MealPlanGeneratorTest {

    private fun totals(kcal: Int, protein: Int) = NutritionTotals(kcal = kcal, proteinG = protein, carbG = kcal / 10, fatG = kcal / 30)

    private fun standardVariant(recipeId: Long) = RecipeVariantEntity(recipeId = recipeId, code = "STANDARD")

    private fun recipe(
        id: Long,
        category: String,
        kcal: Int,
        protein: Int,
        tags: List<String> = emptyList(),
        cookTimeMinutes: Int = 15,
        ingredientFoodIds: Set<Long> = emptySet(),
        extraVariants: List<RecipeVariantEntity> = emptyList(),
    ) = RecipeWithNutrition(
        recipeId = id,
        nameVi = "Recipe $id",
        category = category,
        tags = tags,
        cookTimeMinutes = cookTimeMinutes,
        ingredientFoodIds = ingredientFoodIds,
        standardTotals = totals(kcal, protein),
        variants = listOf(standardVariant(id)) + extraVariants,
    )

    // 2 breakfast, 3 lunch, 2 dinner, 1 snack — enough variety for repeat-avoidance to have real options.
    private val catalog = listOf(
        recipe(1, "Bữa sáng", 300, 20, ingredientFoodIds = setOf(101L)),
        recipe(2, "Bữa sáng", 250, 15, ingredientFoodIds = setOf(102L)),
        recipe(3, "Bữa trưa", 500, 35, ingredientFoodIds = setOf(201L)),
        recipe(4, "Bữa trưa", 550, 30, ingredientFoodIds = setOf(202L)),
        recipe(5, "Bữa trưa", 480, 40, cookTimeMinutes = 45, ingredientFoodIds = setOf(203L)),
        recipe(6, "Bữa tối", 450, 30, ingredientFoodIds = setOf(301L)),
        recipe(7, "Bữa tối", 400, 25, ingredientFoodIds = setOf(302L)),
        recipe(8, "Bữa phụ", 150, 10, ingredientFoodIds = setOf(401L)),
    )

    private fun baseInput(
        goal: NutritionGoal = NutritionGoal.MAINTAIN,
        kcalTarget: Int = 2000,
        proteinTargetG: Int = 140,
        mealsPerDay: Int = 3,
        cuisinePreferenceTags: Set<String> = emptySet(),
        cookTimeCeilingMinutes: Int? = null,
        excludedFoodIds: Set<Long> = emptySet(),
        catalog: List<RecipeWithNutrition> = this.catalog,
    ) = MealPlanGenerationInput(
        goal = goal,
        kcalTarget = kcalTarget,
        proteinTargetG = proteinTargetG,
        mealsPerDay = mealsPerDay,
        cuisinePreferenceTags = cuisinePreferenceTags,
        cookTimeCeilingMinutes = cookTimeCeilingMinutes,
        excludedFoodIds = excludedFoodIds,
        catalog = catalog,
    )

    @Test
    fun `generateDay with 3 meals produces exactly breakfast, lunch, dinner`() {
        val drafts = MealPlanGenerator.generateDay(baseInput(mealsPerDay = 3), dayOfWeek = 1)

        assertEquals(listOf("Bữa sáng", "Bữa trưa", "Bữa tối"), drafts.map { it.slot })
    }

    @Test
    fun `generateDay with 4 meals adds a snack slot`() {
        val drafts = MealPlanGenerator.generateDay(baseInput(mealsPerDay = 4), dayOfWeek = 1)

        assertEquals(listOf("Bữa sáng", "Bữa trưa", "Bữa tối", "Bữa phụ"), drafts.map { it.slot })
    }

    @Test
    fun `generated week hits kcal target within a reasonable tolerance`() {
        val drafts = MealPlanGenerator.generateWeek(baseInput(kcalTarget = 1200, mealsPerDay = 3))
        val dailyTotals = drafts.groupBy { it.dayOfWeek }.mapValues { (_, meals) -> meals.sumOf { it.totals.kcal } }

        dailyTotals.values.forEach { dayKcal ->
            assertTrue("day kcal $dayKcal too far from 1200 target", kotlin.math.abs(dayKcal - 1200) <= 400)
        }
    }

    @Test
    fun `excludedFoodIds is an absolute hard filter`() {
        // Exclude food 201, used only by lunch recipe 3 — recipe 3 must never appear.
        val drafts = MealPlanGenerator.generateWeek(baseInput(excludedFoodIds = setOf(201L)))

        assertFalse(drafts.any { it.recipeId == 3L })
    }

    @Test
    fun `cookTimeCeilingMinutes is an absolute hard filter`() {
        // Recipe 5 (lunch) takes 45 minutes; a 30-minute ceiling must exclude it from every day's
        // candidate pool entirely, regardless of how well it would otherwise score.
        val drafts = MealPlanGenerator.generateWeek(baseInput(cookTimeCeilingMinutes = 30, proteinTargetG = 300))

        assertFalse(drafts.any { it.recipeId == 5L })
    }

    @Test
    fun `variety avoids repeating a recipe across the week when alternatives exist`() {
        val drafts = MealPlanGenerator.generateWeek(baseInput())
        val lunchRecipeIds = drafts.filter { it.slot == "Bữa trưa" }.map { it.recipeId }

        // 3 lunch candidates, 7 days — no single recipe should dominate every day when others exist.
        val mostRepeated = lunchRecipeIds.groupingBy { it }.eachCount().values.max()
        assertTrue("one lunch recipe used $mostRepeated/7 days, expected variety", mostRepeated < 7)
    }

    @Test
    fun `alternativesFor never returns the current recipe and always respects the 60 kcal rule`() {
        // Current: recipe 3 (500 kcal). Recipe 4 (550) is within 60; recipe 5 (480) is within 60.
        val alternatives = MealPlanGenerator.alternativesFor(
            baseInput(), dayOfWeek = 1, slot = "Bữa trưa", currentRecipeId = 3L, currentKcal = 500,
        )

        assertFalse(alternatives.any { it.recipeId == 3L })
        alternatives.forEach { assertTrue(kotlin.math.abs(it.totals.kcal - 500) <= 60) }
    }

    @Test
    fun `alternativesFor excludes ids already shown this session`() {
        val alternatives = MealPlanGenerator.alternativesFor(
            baseInput(), dayOfWeek = 1, slot = "Bữa trưa", currentRecipeId = 3L, currentKcal = 500,
            excludeRecipeIds = setOf(4L),
        )

        assertFalse(alternatives.any { it.recipeId == 4L })
    }

    @Test
    fun `goal preference breaks a near-tied kcal fit toward the matching variant`() {
        // Single lunch recipe with 2 variants at an identical kcal distance from target (500 vs
        // 500 after the CUT multiplier happens to land back on 500) isolates the variant-bonus
        // tie-break from every other scoring term.
        val cutVariant = RecipeVariantEntity(recipeId = 3, code = "CUT", kcalMultiplier = 1.0, proteinMultiplier = 1.0)
        val onlyLunchOption = recipe(3, "Bữa trưa", 500, 35, extraVariants = listOf(cutVariant))
        val soloCatalog = listOf(onlyLunchOption)

        val alternatives = MealPlanGenerator.alternativesFor(
            baseInput(goal = NutritionGoal.CUT, catalog = soloCatalog),
            dayOfWeek = 1, slot = "Bữa trưa", currentRecipeId = -1L, currentKcal = 500,
        )

        // Both variants tie exactly on kcal-delta (0) and protein (35); only the CUT-goal variant
        // bonus differs, so it must sort first.
        assertEquals("CUT", alternatives.first().variantCode)
    }
}
