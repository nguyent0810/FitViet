package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.MealDao
import com.fitviet.app.data.local.entity.MealEntity
import com.fitviet.app.data.local.seed.SeedData
import com.fitviet.app.domain.NutritionCalculator
import com.fitviet.app.domain.NutritionTotals
import com.fitviet.app.domain.RecipeWithNutrition
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

private const val ADDED_MEAL_SLOT = "Bữa phụ"

data class NutritionData(
    val meals: List<MealEntity>,
    val totals: NutritionTotals,
)

class NutritionRepository(private val mealDao: MealDao) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<NutritionData> {
        val zone = ZoneId.systemDefault()

        // Re-subscribes to today's meals on midnight rollover — same pattern as DashboardRepository.
        return dayTicker(zone).flatMapLatest { today ->
            mealDao.observeForDay(today.toEpochDay()).map { meals ->
                NutritionData(meals = meals, totals = NutritionCalculator.compute(meals))
            }
        }
    }

    suspend fun addMeal(preset: SeedData.MealPreset) {
        mealDao.insert(
            MealEntity(
                epochDay = LocalDate.now().toEpochDay(),
                slot = ADDED_MEAL_SLOT,
                nameVi = preset.nameVi,
                kcal = preset.kcal,
                proteinG = preset.proteinG,
                carbG = preset.carbG,
                fatG = preset.fatG,
            ),
        )
    }

    // Gate 5b-i — a real recipe logged from the "Món & thực đơn" library, not a fixed preset.
    // Defaults to the same [ADDED_MEAL_SLOT] the preset-adder above uses; takes an explicit [slot]
    // so Gate 5c's own slot-aware Recipe Detail CTAs ("Bữa phụ" / "+ Thêm vào bữa trưa") can reuse
    // this same method rather than duplicating it once that gate needs a chosen slot.
    suspend fun addMeal(recipe: RecipeWithNutrition, slot: String = ADDED_MEAL_SLOT) {
        mealDao.insert(
            MealEntity(
                epochDay = LocalDate.now().toEpochDay(),
                slot = slot,
                nameVi = recipe.nameVi,
                kcal = recipe.standardTotals.kcal,
                proteinG = recipe.standardTotals.proteinG,
                carbG = recipe.standardTotals.carbG,
                fatG = recipe.standardTotals.fatG,
            ),
        )
    }

    suspend fun removeMeal(meal: MealEntity) = mealDao.delete(meal)
}
