package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One repeating weekday within a [UserMealPlanEntity] — `dayOfWeek` (1=Monday..7=Sunday, same
 * ISO convention as [ProgramDayEntity]) repeats every week, unlike [MonthlyPlanDayEntity]'s
 * real-calendar-date model; see [UserMealPlanEntity]'s doc comment for why. */
@Entity(
    tableName = "meal_plan_days",
    foreignKeys = [
        ForeignKey(
            entity = UserMealPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["userMealPlanId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("userMealPlanId")],
)
data class MealPlanDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userMealPlanId: Long,
    val dayOfWeek: Int,
    val totalKcalTarget: Int,
)
