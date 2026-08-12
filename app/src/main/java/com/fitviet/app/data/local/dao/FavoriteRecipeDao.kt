package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fitviet.app.data.local.entity.FavoriteRecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteRecipeDao {
    @Query("SELECT * FROM favorite_recipes ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<FavoriteRecipeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_recipes WHERE recipeId = :recipeId)")
    fun observeIsFavorite(recipeId: Long): Flow<Boolean>

    @Query("SELECT * FROM favorite_recipes WHERE recipeId = :recipeId")
    suspend fun getForRecipe(recipeId: Long): FavoriteRecipeEntity?

    @Insert
    suspend fun insert(favorite: FavoriteRecipeEntity)

    @Query("DELETE FROM favorite_recipes WHERE recipeId = :recipeId")
    suspend fun deleteForRecipe(recipeId: Long)
}
