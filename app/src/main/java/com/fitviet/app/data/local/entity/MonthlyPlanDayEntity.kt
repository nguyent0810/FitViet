package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One calendar day within a [MonthlyPlanWeekEntity] — unlike [ProgramDayEntity] (one row per
 * ISO weekday, repeating forever), this is one row per real calendar date, since a monthly plan
 * spans real weeks, not an abstract repeating week. */
@Entity(
    tableName = "monthly_plan_days",
    foreignKeys = [
        ForeignKey(
            entity = MonthlyPlanWeekEntity::class,
            parentColumns = ["id"],
            childColumns = ["monthlyPlanWeekId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("monthlyPlanWeekId"), Index("effectiveEpochDay")],
)
data class MonthlyPlanDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthlyPlanWeekId: Long,
    /** Immutable — what the generator originally placed this day on. Never changed by a
     * reschedule; [effectiveEpochDay] is. */
    val plannedEpochDay: Long,
    /** Mutable — where this day currently sits on the calendar. Only ever changed by an explicit
     * reschedule action (push-to-today or manual swap), never by [MissedWorkoutDetector]. */
    val effectiveEpochDay: Long,
    /** ISO day-of-week value: 1=Monday..7=Sunday, derived from [effectiveEpochDay] — same
     * convention as [ProgramDayEntity.dayOfWeek]. */
    val dayOfWeek: Int,
    val isRestDay: Boolean = false,
    /** Display label ("Push", "Upper A", "Power Lower"...), null for rest days. */
    val sessionType: String? = null,
    /** [com.fitviet.app.domain.MuscleGroup.name] values this session covers. */
    val muscleGroupCodes: List<String> = emptyList(),
    /** [com.fitviet.app.domain.MonthlyPlanDayStatus.name]. */
    val status: String,
)
