package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One ingredient line within a [RecipeEntity] — the single source of truth a recipe's nutrition
 * is computed from (see [com.fitviet.app.domain.RecipeNutritionCalculator]); [grams] scales
 * [FoodEntity]'s per-100g values directly. Never stores its own kcal/macro snapshot — those are
 * always derived, so an ingredient-quantity correction automatically fixes the recipe's totals. */
@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId"), Index("foodId")],
)
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val foodId: Long,
    val grams: Double,
    val orderIndex: Int,
    /** Optional friendly override shown instead of a raw gram figure (e.g. "2 quả" instead of
     * "120g") — display only, [grams] is still what the nutrition calculation uses. */
    val displayQuantity: String? = null,
)
