package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.ExerciseDao
import com.fitviet.app.data.local.dao.FoodDao
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.entity.FoodEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class HandbookData(val exercises: List<ExerciseEntity>, val foods: List<FoodEntity>)

/** Backs the Handbook screen (Gate 25) — exercise library grouped by difficulty level, plus a
 * static food reference. Read-only; not tied to meal logging (see [NutritionRepository]) or the
 * workout flow (see [ExerciseRepository]). */
class HandbookRepository(private val exerciseDao: ExerciseDao, private val foodDao: FoodDao) {
    fun observe(): Flow<HandbookData> = combine(exerciseDao.observeAll(), foodDao.observeAll()) { exercises, foods ->
        HandbookData(exercises = exercises, foods = foods)
    }
}
