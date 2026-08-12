package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One planned meal within a [MealPlanDayEntity] — [slot] reuses [MealEntity.slot]'s existing
 * string convention ("Bữa sáng"/"Bữa trưa"/"Bữa tối"/"Bữa phụ"), matching [RecipeEntity.category]'s
 * own meal-slot use. Nutrition (kcal/proteinG/carbG/fatG) is a snapshot taken at generate/swap
 * time — same idiom as [MealEntity]/[MonthlyPlanExerciseEntity].targetWeightKg — so reading a
 * plan's totals never needs a live join through [RecipeEntity]/[RecipeIngredientEntity].
 *
 * Deliberately never "locks" against [WorkoutSessionEntity]-style completion the way
 * [MonthlyPlanDayEntity] does against [WorkoutSessionEntity]: [MealEntity] (consumed) and this
 * table (planned) stay fully independent, so swap/regenerate are always available regardless of
 * logged consumption for that slot — a planned meal existing is not evidence anything was eaten.
 */
@Entity(
    tableName = "meal_plan_meals",
    foreignKeys = [
        ForeignKey(
            entity = MealPlanDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealPlanDayId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mealPlanDayId"), Index("recipeId")],
)
data class MealPlanMealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealPlanDayId: Long,
    val slot: String,
    val recipeId: Long,
    /** [com.fitviet.app.domain.RecipeVariantType.name]. */
    val variantCode: String,
    val servings: Double = 1.0,
    val orderIndex: Int = 0,
    val kcal: Int,
    val proteinG: Int,
    val carbG: Int,
    val fatG: Int,
)
