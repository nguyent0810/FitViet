package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fitviet.app.data.local.entity.MonthlyPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyPlanDao {
    @Query("SELECT * FROM monthly_plans WHERE id = :id")
    suspend fun getById(id: Long): MonthlyPlanEntity?

    @Query("SELECT * FROM monthly_plans WHERE id = :id")
    fun observeById(id: Long): Flow<MonthlyPlanEntity?>

    @Insert
    suspend fun insert(plan: MonthlyPlanEntity): Long

    @Update
    suspend fun update(plan: MonthlyPlanEntity)
}
