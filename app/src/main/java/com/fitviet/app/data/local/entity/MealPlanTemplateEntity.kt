package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A curated starter meal plan (Nutrition module's Templates screen) — stored in the database,
 * never hardcoded in UI (per the backend's explicit requirement). [goalCode] is a
 * [com.fitviet.app.domain.NutritionGoal] name, used to pick which template gets the
 * "PHÙ HỢP MỤC TIÊU" hero treatment for a given user. */
@Entity(tableName = "meal_plan_templates")
data class MealPlanTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameVi: String,
    val descriptionVi: String,
    val goalCode: String,
    val kcalPerDay: Int,
    val proteinPerDayG: Int,
    val mealsPerDay: Int,
    /** [com.fitviet.app.domain.RecipeDifficulty.name]. */
    val difficultyCode: String,
    /** JSON-encoded `List<{slot, kcalSharePercent}>`, decoded via
     * [com.fitviet.app.domain.MealPlanTemplateCodec] — small, read-only, decoded once at
     * generation time (same "curated structured content" shape as
     * [SplitTemplateEntity.dayDefinitionsJson], not the normalized per-row model
     * [MonthlyPlanWeekEntity]/[MonthlyPlanDayEntity] need since those are queried/mutated
     * post-generation and this never is). */
    val dayStructureJson: String,
)
