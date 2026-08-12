package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fitviet.app.data.local.entity.MealPlanMealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanMealDao {
    @Query("SELECT * FROM meal_plan_meals WHERE mealPlanDayId = :mealPlanDayId ORDER BY orderIndex")
    fun observeForDay(mealPlanDayId: Long): Flow<List<MealPlanMealEntity>>

    @Query("SELECT * FROM meal_plan_meals WHERE mealPlanDayId = :mealPlanDayId ORDER BY orderIndex")
    suspend fun getForDay(mealPlanDayId: Long): List<MealPlanMealEntity>

    @Query(
        """
        SELECT m.* FROM meal_plan_meals m
        INNER JOIN meal_plan_days d ON d.id = m.mealPlanDayId
        WHERE d.userMealPlanId = :userMealPlanId
        """,
    )
    suspend fun getForPlan(userMealPlanId: Long): List<MealPlanMealEntity>

    @Query("SELECT * FROM meal_plan_meals WHERE id = :id")
    suspend fun getById(id: Long): MealPlanMealEntity?

    @Insert
    suspend fun insertAll(meals: List<MealPlanMealEntity>): List<Long>

    @Update
    suspend fun update(meal: MealPlanMealEntity)

    @Delete
    suspend fun deleteAll(meals: List<MealPlanMealEntity>)

    @Query("DELETE FROM meal_plan_meals WHERE mealPlanDayId = :mealPlanDayId")
    suspend fun deleteForDay(mealPlanDayId: Long)
}
