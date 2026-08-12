package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fitviet.app.data.local.entity.RecipeVariantEntity

@Dao
interface RecipeVariantDao {
    @Query("SELECT * FROM recipe_variants WHERE recipeId = :recipeId")
    suspend fun getForRecipe(recipeId: Long): List<RecipeVariantEntity>

    @Query("SELECT * FROM recipe_variants WHERE recipeId IN (:recipeIds)")
    suspend fun getForRecipes(recipeIds: List<Long>): List<RecipeVariantEntity>

    @Insert
    suspend fun insertAll(variants: List<RecipeVariantEntity>)
}
