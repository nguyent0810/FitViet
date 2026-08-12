package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.FavoriteRecipeDao
import com.fitviet.app.data.local.dao.FoodDao
import com.fitviet.app.data.local.dao.RecipeDao
import com.fitviet.app.data.local.dao.RecipeIngredientDao
import com.fitviet.app.data.local.dao.RecipeVariantDao
import com.fitviet.app.data.local.entity.FavoriteRecipeEntity
import com.fitviet.app.data.local.entity.FoodEntity
import com.fitviet.app.data.local.entity.RecipeEntity
import com.fitviet.app.data.local.entity.RecipeVariantEntity
import com.fitviet.app.domain.NutritionTotals
import com.fitviet.app.domain.RecipeIngredientWithFood
import com.fitviet.app.domain.RecipeInstructionsCodec
import com.fitviet.app.domain.RecipeNutritionCalculator
import com.fitviet.app.domain.RecipeWithNutrition
import com.fitviet.app.util.normalizeVietnamese
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** One ingredient line for the Recipe Detail screen's display — [foodId] lets the servings
 * stepper recompute [gramsAtBaseServings] client-side without another repository round-trip. */
data class RecipeIngredientDetail(
    val foodId: Long,
    val foodNameVi: String,
    val gramsAtBaseServings: Double,
    val displayQuantity: String?,
)

/** The Recipe Detail screen's full read model — deliberately richer than [RecipeWithNutrition]
 * (the generator's minimal catalog shape): carries [baseServings] and the real
 * [RecipeIngredientDetail] list so the servings stepper can recompute grams/macros live and
 * locally, per the design brief's "single source: servings × per-serving base" requirement. */
data class RecipeDetail(
    val recipeId: Long,
    val nameVi: String,
    val baseServings: Int,
    val prepTimeMinutes: Int,
    val cookTimeMinutes: Int,
    val tags: List<String>,
    val instructions: List<String>,
    val ingredients: List<RecipeIngredientDetail>,
    /** The recipe's raw ingredient-summed totals (all [baseServings] combined, before any
     * division or variant multiplier) — carried alongside [standardTotals] so a servings/variant
     * recompute can call [RecipeNutritionCalculator.computePerServing] directly on real numbers
     * instead of reconstructing it by scaling the already-rounded [standardTotals] back up, which
     * amplifies rounding error at higher [baseServings]/multiplier combinations. */
    val baseTotals: NutritionTotals,
    val standardTotals: NutritionTotals,
    val variants: List<RecipeVariantEntity>,
)

/** [category] doubles as the meal-slot filter, matching [com.fitviet.app.data.local.entity.RecipeEntity.category]'s
 * convention. [searchQuery] must already be normalized via [com.fitviet.app.util.normalizeVietnamese]
 * by the caller (same contract as [com.fitviet.app.data.local.dao.FoodDao.searchByNormalizedName]). */
data class RecipeFilter(
    val category: String? = null,
    val tags: Set<String> = emptySet(),
    val searchQuery: String? = null,
    val maxCookTimeMinutes: Int? = null,
)

/** Real Room-backed Nutrition browse/detail repository — Discover/Foods/Recipe-Detail/Favorites
 * screens (Part C) read through this, never a mock. Translates the design brief's REST-endpoint
 * list into local method contracts (no server/accounts exist in this app — see the "Hit & Run"
 * plan's precedent for the same translation). */
interface RecipeRepository {
    fun observeRecipes(filter: RecipeFilter): Flow<List<RecipeWithNutrition>>
    fun observeFoods(category: String?): Flow<List<FoodEntity>>
    suspend fun getRecipe(id: Long): RecipeWithNutrition?

    /** Gate C4 — the Recipe Detail screen's richer read model; see [RecipeDetail]'s doc. */
    suspend fun getRecipeDetail(id: Long): RecipeDetail?
    fun observeFavorites(): Flow<List<RecipeWithNutrition>>
    fun observeIsFavorite(recipeId: Long): Flow<Boolean>
    suspend fun toggleFavorite(recipeId: Long)
}

class RoomRecipeRepository(
    private val recipeDao: RecipeDao,
    private val recipeIngredientDao: RecipeIngredientDao,
    private val recipeVariantDao: RecipeVariantDao,
    private val foodDao: FoodDao,
    private val favoriteRecipeDao: FavoriteRecipeDao,
) : RecipeRepository {

    override fun observeRecipes(filter: RecipeFilter): Flow<List<RecipeWithNutrition>> =
        recipeDao.observeAll().map { recipes ->
            val query = filter.searchQuery?.takeIf { it.isNotBlank() }
            recipes
                .filter { recipe ->
                    (filter.category == null || recipe.category == filter.category) &&
                        (filter.tags.isEmpty() || filter.tags.any { it in recipe.tags }) &&
                        (filter.maxCookTimeMinutes == null || recipe.cookTimeMinutes <= filter.maxCookTimeMinutes) &&
                        (query == null || recipe.normalizedName().contains(query))
                }
                .map { resolveRecipeWithNutrition(it, recipeIngredientDao, recipeVariantDao, foodDao) }
        }

    override fun observeFoods(category: String?): Flow<List<FoodEntity>> =
        if (category == null) foodDao.observeAll() else foodDao.observeByCategory(category)

    override suspend fun getRecipe(id: Long): RecipeWithNutrition? {
        val recipe = recipeDao.getById(id) ?: return null
        return resolveRecipeWithNutrition(recipe, recipeIngredientDao, recipeVariantDao, foodDao)
    }

    override suspend fun getRecipeDetail(id: Long): RecipeDetail? {
        val recipe = recipeDao.getById(id) ?: return null
        val ingredients = recipeIngredientDao.getForRecipe(id)
        val foodsById = foodDao.getByIds(ingredients.map { it.foodId }).associateBy { it.id }
        val ingredientDetails = ingredients.mapNotNull { ingredient ->
            foodsById[ingredient.foodId]?.let { food ->
                RecipeIngredientDetail(
                    foodId = food.id,
                    foodNameVi = food.nameVi,
                    gramsAtBaseServings = ingredient.grams,
                    displayQuantity = ingredient.displayQuantity,
                )
            }
        }
        val ingredientsWithFood = ingredients.mapNotNull { ingredient ->
            foodsById[ingredient.foodId]?.let { food -> RecipeIngredientWithFood(ingredient.grams, food) }
        }
        val baseTotals = RecipeNutritionCalculator.computeBaseTotals(ingredientsWithFood)
        val variants = recipeVariantDao.getForRecipe(id)
        val standardVariant = variants.firstOrNull { it.code == "STANDARD" }
        val standardTotals = RecipeNutritionCalculator.computePerServing(baseTotals, recipe.baseServings, standardVariant, servings = 1.0)
        return RecipeDetail(
            recipeId = recipe.id,
            nameVi = recipe.nameVi,
            baseServings = recipe.baseServings,
            prepTimeMinutes = recipe.prepTimeMinutes,
            cookTimeMinutes = recipe.cookTimeMinutes,
            tags = recipe.tags,
            instructions = RecipeInstructionsCodec.decode(recipe.instructionsJson).orEmpty(),
            ingredients = ingredientDetails,
            baseTotals = baseTotals,
            standardTotals = standardTotals,
            variants = variants,
        )
    }

    override fun observeFavorites(): Flow<List<RecipeWithNutrition>> =
        favoriteRecipeDao.observeAll().map { favorites ->
            favorites.mapNotNull { favorite ->
                recipeDao.getById(favorite.recipeId)?.let { resolveRecipeWithNutrition(it, recipeIngredientDao, recipeVariantDao, foodDao) }
            }
        }

    override fun observeIsFavorite(recipeId: Long): Flow<Boolean> = favoriteRecipeDao.observeIsFavorite(recipeId)

    override suspend fun toggleFavorite(recipeId: Long) {
        if (favoriteRecipeDao.getForRecipe(recipeId) != null) {
            favoriteRecipeDao.deleteForRecipe(recipeId)
        } else {
            favoriteRecipeDao.insert(FavoriteRecipeEntity(recipeId = recipeId, createdAtEpochMillis = System.currentTimeMillis()))
        }
    }
}

/** [RecipeEntity] has no stored normalized name of its own (unlike [FoodEntity.normalizedName]) —
 * recipe names are a much smaller, in-memory-filterable set (tens, not hundreds), so normalizing
 * on the fly here is simpler than adding + backfilling another column for this phase. */
private fun RecipeEntity.normalizedName(): String = normalizeVietnamese(nameVi)
