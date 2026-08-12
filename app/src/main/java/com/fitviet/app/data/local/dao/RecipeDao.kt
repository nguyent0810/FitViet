package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fitviet.app.data.local.entity.RecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY nameVi")
    fun observeAll(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: Long): RecipeEntity?

    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeById(id: Long): Flow<RecipeEntity?>

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(recipes: List<RecipeEntity>): List<Long>
}
