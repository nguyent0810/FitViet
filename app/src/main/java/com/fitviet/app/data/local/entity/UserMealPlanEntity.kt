package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user's saved/generated meal plan — parallel to [MonthlyPlanEntity]'s ACTIVE/SUPERSEDED
 * history-preserving lifecycle (generating a new plan flips the previous ACTIVE row to
 * SUPERSEDED rather than deleting it), but otherwise structurally simpler: no week-to-week
 * progression concept exists in the Nutrition brief (the Generated Plan screen is just T2–CN day
 * tabs), so [MealPlanDayEntity] below uses a repeating `dayOfWeek`, not real calendar dates like
 * [MonthlyPlanDayEntity]. No `userId` — single local user, same reasoning as [SettingsEntity]'s
 * singleton-row pattern.
 */
@Entity(
    tableName = "user_meal_plans",
    foreignKeys = [
        ForeignKey(
            entity = MealPlanTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceTemplateId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("sourceTemplateId")],
)
data class UserMealPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAtEpochMillis: Long,
    /** [com.fitviet.app.domain.NutritionGoal.name]. */
    val goalCode: String,
    val kcalTarget: Int,
    val proteinTargetG: Int,
    val mealsPerDay: Int,
    val cuisinePreferenceTags: List<String> = emptyList(),
    val cookTimeCeilingMinutes: Int? = null,
    val excludedFoodIds: List<Long> = emptyList(),
    /** Non-null only when generated via "use this template" rather than the create-plan wizard. */
    val sourceTemplateId: Long? = null,
    /** [com.fitviet.app.domain.MealPlanStatus.name]. */
    val status: String,
)
