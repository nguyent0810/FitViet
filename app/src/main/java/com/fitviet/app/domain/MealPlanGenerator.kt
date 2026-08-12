package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.RecipeVariantEntity
import kotlin.math.abs
import kotlin.math.roundToInt

/** One catalog entry the generator can pick from — pre-resolved by the repository (this file
 * stays Room-free) so the generator never needs to touch [com.fitviet.app.data.local.entity.RecipeEntity]/
 * [com.fitviet.app.data.local.entity.RecipeIngredientEntity] directly. [standardTotals] is the
 * recipe's per-serving totals at the `STANDARD` variant (i.e. already divided by `baseServings`,
 * before any non-1.0 variant multiplier) — [applyVariant] scales it per-candidate. [category]
 * doubles as the meal-slot marker, matching [com.fitviet.app.data.local.seed.SeedData.RecipeSeed]'s
 * convention. */
data class RecipeWithNutrition(
    val recipeId: Long,
    val nameVi: String,
    val category: String,
    val tags: List<String>,
    val cookTimeMinutes: Int,
    val ingredientFoodIds: Set<Long>,
    val standardTotals: NutritionTotals,
    val variants: List<RecipeVariantEntity>,
)

data class MealPlanGenerationInput(
    val goal: NutritionGoal,
    val kcalTarget: Int,
    val proteinTargetG: Int,
    /** 3..6; clamped internally. */
    val mealsPerDay: Int,
    val cuisinePreferenceTags: Set<String> = emptySet(),
    val cookTimeCeilingMinutes: Int? = null,
    /** A recipe intersecting any of these ids on any ingredient is a hard exclusion. */
    val excludedFoodIds: Set<Long> = emptySet(),
    val catalog: List<RecipeWithNutrition>,
)

data class MealPlanMealDraft(
    val dayOfWeek: Int,
    val slot: String,
    val recipeId: Long,
    val variantCode: String,
    val servings: Double,
    val totals: NutritionTotals,
)

/**
 * Nutrition module's deterministic, non-AI meal-plan generator V1 — same "pure input struct,
 * deterministic tie-break, no Room/Android import" idiom as [MonthlyPlanGenerator]. Implements the
 * 8 priorities in this order (highest first): (1) per-meal calorie-slot fit, (2) day's running
 * protein-total fit — recipes scored higher on protein while the day is still under
 * [MealPlanGenerationInput.proteinTargetG], lower once it's met, (3) sane per-meal kcal share —
 * satisfied by construction via [slotsFor]'s fixed percentages (no slot ever exceeds ~35-40% of
 * the daily target), (4)/(8) day-to-day and within-day repeat avoidance — a soft penalty in
 * [candidatesForSlot], tracked via a recipeId->useCount map threaded through [generateDay] (per
 * day) and [generateWeek] (across the whole week), (5) exact slot match — [RecipeWithNutrition.category]
 * is filtered exactly against the target slot, never fuzzy-matched, (6) preference/tag match — a
 * soft scoring bonus, never a hard filter, (7) [MealPlanGenerationInput.cookTimeCeilingMinutes] —
 * a hard filter when set. [MealPlanGenerationInput.excludedFoodIds] is always a hard filter
 * (ingredient-level, not just a scoring signal). Deterministic tie-break by recipeId then variant
 * code, same convention as [MonthlyPlanGenerator.pickExercise].
 */
object MealPlanGenerator {

    fun generateDay(
        input: MealPlanGenerationInput,
        dayOfWeek: Int,
        usedSoFar: Map<Long, Int> = emptyMap(),
    ): List<MealPlanMealDraft> {
        val usedThisDay = usedSoFar.toMutableMap()
        var proteinSoFar = 0
        return slotsFor(input.mealsPerDay).mapNotNull { (slot, sharePercent) ->
            val targetKcal = (input.kcalTarget * sharePercent / 100.0).roundToInt()
            val chosen = candidatesForSlot(input, slot, targetKcal, proteinSoFar < input.proteinTargetG, usedThisDay)
                .firstOrNull() ?: return@mapNotNull null
            usedThisDay[chosen.recipe.recipeId] = (usedThisDay[chosen.recipe.recipeId] ?: 0) + 1
            proteinSoFar += chosen.totals.proteinG
            MealPlanMealDraft(dayOfWeek, slot, chosen.recipe.recipeId, chosen.variant.code, servings = 1.0, chosen.totals)
        }
    }

    /** Generates all 7 days, threading one shared used-recipe-count map through every day so
     * variety is tracked across the whole week, not just within a single day. */
    fun generateWeek(input: MealPlanGenerationInput): List<MealPlanMealDraft> {
        val usedThisWeek = mutableMapOf<Long, Int>()
        val drafts = mutableListOf<MealPlanMealDraft>()
        for (dayOfWeek in 1..7) {
            val dayDrafts = generateDay(input, dayOfWeek, usedThisWeek)
            dayDrafts.forEach { draft -> usedThisWeek[draft.recipeId] = (usedThisWeek[draft.recipeId] ?: 0) + 1 }
            drafts += dayDrafts
        }
        return drafts
    }

    /** Alternatives for the swap sheet — same slot, |Δkcal| ≤ 60 vs. [currentKcal], never
     * [currentRecipeId] or anything in [excludeRecipeIds] (already-shown alternatives, so
     * "Gợi ý khác ↻" doesn't immediately repeat). [dayOfWeek] is carried through to the returned
     * drafts for the caller's convenience only — it plays no role in candidate selection. */
    fun alternativesFor(
        input: MealPlanGenerationInput,
        dayOfWeek: Int,
        slot: String,
        currentRecipeId: Long,
        currentKcal: Int,
        excludeRecipeIds: Set<Long> = emptySet(),
        maxResults: Int = 5,
    ): List<MealPlanMealDraft> {
        val kcalDeltaCeiling = 60
        return candidatesForSlot(input, slot, currentKcal, proteinStillNeeded = true, usedThisContext = emptyMap())
            .asSequence()
            .filter { it.recipe.recipeId != currentRecipeId && it.recipe.recipeId !in excludeRecipeIds }
            .filter { abs(it.totals.kcal - currentKcal) <= kcalDeltaCeiling }
            .take(maxResults.coerceAtLeast(0))
            .map { MealPlanMealDraft(dayOfWeek, slot, it.recipe.recipeId, it.variant.code, servings = 1.0, it.totals) }
            .toList()
    }

    // ---- meal-slot spacing ----

    /** 3 meals: no snack slot, the 10-point snack share redistributes into sáng/trưa/tối. 4-6
     * meals: sáng/trưa/tối hold their base 25/35/30 shares, the remaining 10 points split evenly
     * across (mealsPerDay-3) "Bữa phụ" slots (any remainder from integer division goes to the
     * first snack slot, so shares always sum to exactly 100). */
    private fun slotsFor(mealsPerDay: Int): List<Pair<String, Int>> {
        val clamped = mealsPerDay.coerceIn(3, 6)
        if (clamped == 3) {
            return listOf("Bữa sáng" to 30, "Bữa trưa" to 40, "Bữa tối" to 30)
        }
        val snackCount = clamped - 3
        val perSnack = 10 / snackCount
        val remainder = 10 - perSnack * snackCount
        val core = listOf("Bữa sáng" to 25, "Bữa trưa" to 35, "Bữa tối" to 30)
        val snacks = List(snackCount) { index -> "Bữa phụ" to (perSnack + if (index == 0) remainder else 0) }
        return core + snacks
    }

    // ---- selection ----

    private data class ScoredCandidate(val recipe: RecipeWithNutrition, val variant: RecipeVariantEntity, val totals: NutritionTotals, val score: Double)

    private fun applyVariant(standard: NutritionTotals, variant: RecipeVariantEntity): NutritionTotals = NutritionTotals(
        kcal = (standard.kcal * variant.kcalMultiplier).roundToInt(),
        proteinG = (standard.proteinG * variant.proteinMultiplier).roundToInt(),
        carbG = (standard.carbG * variant.carbMultiplier).roundToInt(),
        fatG = (standard.fatG * variant.fatMultiplier).roundToInt(),
    )

    private fun preferredVariantCode(goal: NutritionGoal): String = when (goal) {
        NutritionGoal.CUT -> "CUT"
        NutritionGoal.BULK -> "BULK"
        NutritionGoal.MAINTAIN -> "STANDARD"
    }

    private fun candidatesForSlot(
        input: MealPlanGenerationInput,
        slot: String,
        targetKcal: Int,
        proteinStillNeeded: Boolean,
        usedThisContext: Map<Long, Int>,
    ): List<ScoredCandidate> {
        val hardFiltered = input.catalog.filter { recipe ->
            recipe.category == slot &&
                recipe.ingredientFoodIds.none { it in input.excludedFoodIds } &&
                (input.cookTimeCeilingMinutes == null || recipe.cookTimeMinutes <= input.cookTimeCeilingMinutes)
        }
        if (hardFiltered.isEmpty()) return emptyList()

        val preferredVariant = preferredVariantCode(input.goal)
        val proteinWeight = if (proteinStillNeeded) 1.2 else 0.4

        return hardFiltered.flatMap { recipe ->
            recipe.variants.map { variant ->
                val totals = applyVariant(recipe.standardTotals, variant)
                val kcalDelta = abs(totals.kcal - targetKcal).toDouble()
                val proteinBonus = totals.proteinG * proteinWeight
                val repeatPenalty = (usedThisContext[recipe.recipeId] ?: 0) * 200.0
                val preferenceBonus = if (input.cuisinePreferenceTags.isNotEmpty() && recipe.tags.any { it in input.cuisinePreferenceTags }) -30.0 else 0.0
                val variantBonus = if (variant.code == preferredVariant) -15.0 else 0.0
                val score = kcalDelta - proteinBonus + repeatPenalty + preferenceBonus + variantBonus
                ScoredCandidate(recipe, variant, totals, score)
            }
        }.sortedWith(compareBy({ it.score }, { it.recipe.recipeId }, { it.variant.code }))
    }
}
