package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.MealDao
import com.fitviet.app.data.local.entity.MealEntity
import com.fitviet.app.domain.MealMacros
import com.fitviet.app.domain.NutritionCalculator
import com.fitviet.app.domain.NutritionStats
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

data class NutritionData(val stats: NutritionStats, val meals: List<MealEntity>)

class NutritionRepository(private val mealDao: MealDao) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeToday(): Flow<NutritionData> {
        val zone = ZoneId.systemDefault()
        // Re-derives "today" across midnight, same rationale as DayTicker's other users.
        return dayTicker(zone).flatMapLatest { today ->
            mealDao.observeForDay(today.toEpochDay()).map { meals ->
                val macros = meals.map { MealMacros(it.kcal, it.proteinG, it.carbG, it.fatG) }
                NutritionData(stats = NutritionCalculator.compute(macros), meals = meals)
            }
        }
    }

    suspend fun addMeal(meal: MealEntity) {
        mealDao.insert(meal)
    }

    suspend fun removeMeal(meal: MealEntity) {
        mealDao.delete(meal)
    }
}
