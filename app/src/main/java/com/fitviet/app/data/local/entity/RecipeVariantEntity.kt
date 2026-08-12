package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A named nutrition multiplier set for a [RecipeEntity] (Standard/Cut/High Protein/Bulk) —
 * applied on top of the ingredient-computed base nutrition, not a second ingredient list. `1.0`
 * on every multiplier (the `STANDARD` row every recipe has) is a no-op. This is a deliberate v1
 * approximation (curated per-recipe in seed data) rather than real per-ingredient substitution —
 * flagged in [com.fitviet.app.domain.RecipeVariantType]'s own doc comment as a future-work item. */
@Entity(
    tableName = "recipe_variants",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId")],
)
data class RecipeVariantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    /** [com.fitviet.app.domain.RecipeVariantType.name]. */
    val code: String,
    val kcalMultiplier: Double = 1.0,
    val proteinMultiplier: Double = 1.0,
    val carbMultiplier: Double = 1.0,
    val fatMultiplier: Double = 1.0,
)
