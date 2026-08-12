package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fitviet.app.data.local.entity.RecipeIngredientEntity

@Dao
interface RecipeIngredientDao {
    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY orderIndex")
    suspend fun getForRecipe(recipeId: Long): List<RecipeIngredientEntity>

    /** Bulk-resolve every ingredient row for a whole catalog page in one query (Part C's Discover
     * grid needs each recipe's computed totals without an N+1 query per card). */
    @Query("SELECT * FROM recipe_ingredients WHERE recipeId IN (:recipeIds) ORDER BY recipeId, orderIndex")
    suspend fun getForRecipes(recipeIds: List<Long>): List<RecipeIngredientEntity>

    @Insert
    suspend fun insertAll(ingredients: List<RecipeIngredientEntity>)
}
