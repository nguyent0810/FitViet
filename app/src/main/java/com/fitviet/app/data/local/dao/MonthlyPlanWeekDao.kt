package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fitviet.app.data.local.entity.MonthlyPlanWeekEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyPlanWeekDao {
    @Query("SELECT * FROM monthly_plan_weeks WHERE monthlyPlanId = :monthlyPlanId ORDER BY weekIndex")
    fun observeForPlan(monthlyPlanId: Long): Flow<List<MonthlyPlanWeekEntity>>

    @Query("SELECT * FROM monthly_plan_weeks WHERE monthlyPlanId = :monthlyPlanId ORDER BY weekIndex")
    suspend fun getForPlan(monthlyPlanId: Long): List<MonthlyPlanWeekEntity>

    @Query("SELECT * FROM monthly_plan_weeks WHERE id = :id")
    suspend fun getById(id: Long): MonthlyPlanWeekEntity?

    @Insert
    suspend fun insert(week: MonthlyPlanWeekEntity): Long
}
