package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.MealEntity

/** Daily goals from the 1g spec (README's `NutritionDay.goals`). */
object NutritionGoals {
    const val KCAL = 2200
    const val PROTEIN_G = 140
    const val CARB_G = 250
    const val FAT_G = 70
}

data class NutritionTotals(
    val kcal: Int = 0,
    val proteinG: Int = 0,
    val carbG: Int = 0,
    val fatG: Int = 0,
) {
    val kcalPercent: Int get() = percentOf(kcal, NutritionGoals.KCAL)
    val proteinPercent: Int get() = percentOf(proteinG, NutritionGoals.PROTEIN_G)
    val carbPercent: Int get() = percentOf(carbG, NutritionGoals.CARB_G)
    val fatPercent: Int get() = percentOf(fatG, NutritionGoals.FAT_G)
}

private fun percentOf(value: Int, goal: Int): Int = ((value * 100) / goal).coerceIn(0, 100)

/** Pure sum of a day's logged meals — the 1g kcal ring + macro bars. */
object NutritionCalculator {
    fun compute(meals: List<MealEntity>): NutritionTotals = NutritionTotals(
        kcal = meals.sumOf { it.kcal },
        proteinG = meals.sumOf { it.proteinG },
        carbG = meals.sumOf { it.carbG },
        fatG = meals.sumOf { it.fatG },
    )
}
