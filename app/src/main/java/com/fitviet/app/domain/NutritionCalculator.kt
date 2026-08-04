package com.fitviet.app.domain

data class MealMacros(val kcal: Int, val proteinG: Int, val carbG: Int, val fatG: Int)

data class NutritionStats(
    val kcalTotal: Int,
    val kcalGoal: Int,
    val kcalPct: Int,
    val proteinG: Int,
    val proteinGoalG: Int,
    val proteinPct: Int,
    val carbG: Int,
    val carbGoalG: Int,
    val carbPct: Int,
    val fatG: Int,
    val fatGoalG: Int,
    val fatPct: Int,
)

/** Pure, unit-testable — goals match README's suggested NutritionDay defaults (kcal 2200, p 140, c 250, f 70). */
object NutritionCalculator {
    const val KCAL_GOAL = 2200
    const val PROTEIN_GOAL_G = 140
    const val CARB_GOAL_G = 250
    const val FAT_GOAL_G = 70

    fun compute(meals: List<MealMacros>): NutritionStats {
        val kcalTotal = meals.sumOf { it.kcal }
        val proteinTotal = meals.sumOf { it.proteinG }
        val carbTotal = meals.sumOf { it.carbG }
        val fatTotal = meals.sumOf { it.fatG }

        return NutritionStats(
            kcalTotal = kcalTotal,
            kcalGoal = KCAL_GOAL,
            kcalPct = percentOf(kcalTotal, KCAL_GOAL),
            proteinG = proteinTotal,
            proteinGoalG = PROTEIN_GOAL_G,
            proteinPct = percentOf(proteinTotal, PROTEIN_GOAL_G),
            carbG = carbTotal,
            carbGoalG = CARB_GOAL_G,
            carbPct = percentOf(carbTotal, CARB_GOAL_G),
            fatG = fatTotal,
            fatGoalG = FAT_GOAL_G,
            fatPct = percentOf(fatTotal, FAT_GOAL_G),
        )
    }

    private fun percentOf(value: Int, goal: Int): Int =
        if (goal <= 0) 0 else (value * 100 / goal).coerceIn(0, 100)
}
