package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A food reference entry shown on the Handbook's (Gate 25) food section — read-only reference
 * content, not tied to meal logging (see [MealEntity] for that). */
@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameVi: String,
    val nameEn: String,
    /** Free-text grouping label (e.g. "Đạm", "Tinh bột") — a static display section, not a filter
     * chip taxonomy, matching this gate's "simple reference" scope. */
    val category: String,
    val descriptionVi: String,
    val kcalPer100g: Int,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
)
