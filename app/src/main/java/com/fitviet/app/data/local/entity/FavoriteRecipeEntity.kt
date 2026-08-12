package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A user-favorited recipe. No `userId` — single local user (same reasoning as
 * [UserMealPlanEntity]'s doc comment). Unique index on [recipeId] both enforces "favorite once"
 * and doubles as the fast existence check for the Recipe Detail screen's ♡ toggle. */
@Entity(
    tableName = "favorite_recipes",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId", unique = true)],
)
data class FavoriteRecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val createdAtEpochMillis: Long,
)
