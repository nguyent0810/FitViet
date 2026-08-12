package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A cookable dish (Nutrition module, Gate B2+) — nutrition is deterministically computed from its
 * [RecipeIngredientEntity] rows via [com.fitviet.app.domain.RecipeNutritionCalculator], never
 * hardcoded or AI-estimated. [baseServings] is the recipe's "as written" yield (e.g. 1) that the
 * ingredient totals are divided by before applying a [RecipeVariantEntity] multiplier and the
 * user's chosen serving count. */
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameVi: String,
    /** Display grouping, same free-text idiom as [FoodEntity.category] (e.g. "Món chính", "Bữa phụ"). */
    val category: String,
    /** Resolved to a bundled drawable/asset the same way exercise photos already are; null falls
     * back to a gradient placeholder card (same idiom as [com.fitviet.app.ui.programs.ProgramCoverArt]). */
    val imageAssetKey: String? = null,
    val baseServings: Int,
    val prepTimeMinutes: Int,
    val cookTimeMinutes: Int,
    /** [com.fitviet.app.domain.RecipeDifficulty.name]. */
    val difficultyCode: String,
    /** Cuisine/dietary/filter tags (e.g. "Món Việt", "Dễ làm", "Giảm mỡ") — reuses
     * [com.fitviet.app.data.local.Converters]'s existing `List<String>` JSON-array converter. */
    val tags: List<String> = emptyList(),
    /** JSON-encoded `List<String>` of numbered instruction steps — decoded via
     * [com.fitviet.app.domain.RecipeInstructionsCodec]. */
    val instructionsJson: String,
)
