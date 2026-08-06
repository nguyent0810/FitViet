package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.fitviet.app.data.local.entity.MealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meals WHERE epochDay = :epochDay ORDER BY id")
    fun observeForDay(epochDay: Long): Flow<List<MealEntity>>

    @Insert
    suspend fun insert(meal: MealEntity): Long

    @Delete
    suspend fun delete(meal: MealEntity)

    /** Feature #6 (Gate 37) — the destructive "reset app data" settings action. */
    @Query("DELETE FROM meals")
    suspend fun deleteAll()
}
