package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fitviet.app.data.local.entity.MealPlanDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDayDao {
    @Query("SELECT * FROM meal_plan_days WHERE userMealPlanId = :userMealPlanId ORDER BY dayOfWeek")
    fun observeForPlan(userMealPlanId: Long): Flow<List<MealPlanDayEntity>>

    @Query("SELECT * FROM meal_plan_days WHERE userMealPlanId = :userMealPlanId ORDER BY dayOfWeek")
    suspend fun getForPlan(userMealPlanId: Long): List<MealPlanDayEntity>

    @Query("SELECT * FROM meal_plan_days WHERE id = :id")
    suspend fun getById(id: Long): MealPlanDayEntity?

    @Query("SELECT * FROM meal_plan_days WHERE userMealPlanId = :userMealPlanId AND dayOfWeek = :dayOfWeek")
    suspend fun getForPlanAndDay(userMealPlanId: Long, dayOfWeek: Int): MealPlanDayEntity?

    @Insert
    suspend fun insertAll(days: List<MealPlanDayEntity>): List<Long>

    /** Nutrition Gate B9 — regenerate/swap need to keep `totalKcalTarget` in sync with the day's
     * actual meal totals after a mutation. */
    @Update
    suspend fun update(day: MealPlanDayEntity)
}
