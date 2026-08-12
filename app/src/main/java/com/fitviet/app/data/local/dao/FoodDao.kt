package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fitviet.app.data.local.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY id")
    fun observeAll(): Flow<List<FoodEntity>>

    /** Gate B4 — one-shot equivalent of [observeAll], same idiom as `ExerciseDao.getAllOnce()`;
     * used by [com.fitviet.app.data.local.seed.DatabaseSeeder] to resolve seed recipe ingredients'
     * food names to real ids without collecting a [Flow]. */
    @Query("SELECT * FROM foods ORDER BY id")
    suspend fun getAllOnce(): List<FoodEntity>

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(foods: List<FoodEntity>)

    /** Gate B1 — Nutrition module's Foods screen category chips. */
    @Query("SELECT * FROM foods WHERE category = :category ORDER BY nameVi")
    fun observeByCategory(category: String): Flow<List<FoodEntity>>

    /** Gate B1 — bulk-resolve ingredient rows for a recipe (Part B/C: [com.fitviet.app.data.local.entity.RecipeIngredientEntity].foodId). */
    @Query("SELECT * FROM foods WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<FoodEntity>

    /** Gate B1 — accent-insensitive search backing Nutrition's Foods/Discover search fields
     * (Part C); [query] must already be normalized via [com.fitviet.app.util.normalizeVietnamese]
     * by the caller. */
    @Query("SELECT * FROM foods WHERE normalizedName LIKE '%' || :query || '%' ORDER BY nameVi")
    fun searchByNormalizedName(query: String): Flow<List<FoodEntity>>
}
