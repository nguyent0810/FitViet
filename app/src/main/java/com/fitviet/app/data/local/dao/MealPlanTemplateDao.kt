package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fitviet.app.data.local.entity.MealPlanTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanTemplateDao {
    @Query("SELECT * FROM meal_plan_templates ORDER BY id")
    fun observeAll(): Flow<List<MealPlanTemplateEntity>>

    @Query("SELECT * FROM meal_plan_templates WHERE id = :id")
    suspend fun getById(id: Long): MealPlanTemplateEntity?

    @Query("SELECT COUNT(*) FROM meal_plan_templates")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(templates: List<MealPlanTemplateEntity>)
}
