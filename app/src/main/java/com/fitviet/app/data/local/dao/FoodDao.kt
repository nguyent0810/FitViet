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

    /** Gate B1 — the retired Foods screen's category-chip filter. Its only caller as of Gate
     * 5b-i, `RecipeRepository.observeFoods(category)`, is always invoked with `category = null`
     * (`NutritionLibraryViewModel` filters client-side on the stored `normalizedName` column
     * instead, see [searchByNormalizedName]'s own doc), so this query currently has no reachable
     * non-null caller — left in place rather than deleted since a future category filter UI is a
     * plausible, cheap re-add against an already-correct query. */
    @Query("SELECT * FROM foods WHERE category = :category ORDER BY nameVi")
    fun observeByCategory(category: String): Flow<List<FoodEntity>>

    /** Gate B1 — bulk-resolve ingredient rows for a recipe (Part B/C: [com.fitviet.app.data.local.entity.RecipeIngredientEntity].foodId). */
    @Query("SELECT * FROM foods WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<FoodEntity>

    /** Gate B1 — accent-insensitive search, originally meant to back the Nutrition module's own
     * search fields; [query] must already be normalized via [com.fitviet.app.util.normalizeVietnamese]
     * by the caller. Zero call sites as of Gate 5b-i — `NutritionLibraryViewModel` filters the
     * already-loaded food list client-side on [FoodEntity.normalizedName] instead of a separate
     * DB query, since [com.fitviet.app.data.repository.RecipeRepository.observeFoods] already
     * returns the full unfiltered list for this screen's other purposes. Left in place, not
     * deleted, for the same reason as [observeByCategory] above. */
    @Query("SELECT * FROM foods WHERE normalizedName LIKE '%' || :query || '%' ORDER BY nameVi")
    fun searchByNormalizedName(query: String): Flow<List<FoodEntity>>
}
