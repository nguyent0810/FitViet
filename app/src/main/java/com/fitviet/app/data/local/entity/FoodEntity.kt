package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A food reference entry shown on the Handbook's (Gate 25) food section — read-only reference
 * content, not tied to meal logging (see [MealEntity] for that). Also the ingredient catalog for
 * the Nutrition module's real Recipe system (Gate B1+) — [RecipeIngredientEntity] rows reference
 * this table's [id] and scale [kcalPer100g]/[proteinG]/[carbG]/[fatG] by grams used. */
@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameVi: String,
    val nameEn: String,
    /** Free-text ingredient-type grouping label (e.g. "Thịt", "Cá & hải sản", "Rau củ" — see the
     * curated list in [com.fitviet.app.data.local.seed.SeedData.foods], Gate E7 recategorized this
     * from an earlier macro-nutrient taxonomy to real ingredient types). Used both as
     * [com.fitviet.app.ui.nutrition.foods.FoodsScreen]'s filter chips and as the Handbook's
     * (Gate 25/E7) category-drilldown grouping — not purely a static display label. */
    val category: String,
    val descriptionVi: String,
    val kcalPer100g: Int,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    /** Lowercase, diacritic-stripped [nameVi] (see [com.fitviet.app.util.normalizeVietnamese]) —
     * computed once at insert time, used for accent-insensitive search (Gate B1, feeds the
     * Nutrition Foods/Discover screens' search field in Part C). */
    val normalizedName: String = "",
    /** Null for foods that only make sense "per 100g" (most raw ingredients); set for foods with a
     * natural discrete unit (e.g. "1 quả trứng" ≈ 50g), letting recipe authoring express quantities
     * in that unit rather than always in grams. */
    val servingSizeG: Double? = null,
    val servingUnit: String? = null,
    /** Per 100g, defaults to 0.0 for the pre-Gate-B1 seeded rows that never tracked fiber. */
    val fiberG: Double = 0.0,
    /** Cuisine/dietary/filter tags (e.g. "món Việt", "chay") — reuses [com.fitviet.app.data.local.Converters]'s
     * existing `List<String>` JSON-array converter. */
    val tags: List<String> = emptyList(),
    /** Provenance marker ("SEED" for every row this app ships with) — no UI writes a different
     * value yet, but the column exists so a future user-added-food feature isn't a breaking schema
     * change. */
    val source: String = "SEED",
)
