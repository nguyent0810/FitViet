package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.MealDao
import com.fitviet.app.data.local.entity.MealEntity
import com.fitviet.app.data.local.seed.SeedData
import com.fitviet.app.domain.NutritionCalculator
import com.fitviet.app.domain.NutritionTotals
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

    suspend fun removeMeal(meal: MealEntity) = mealDao.delete(meal)
}
