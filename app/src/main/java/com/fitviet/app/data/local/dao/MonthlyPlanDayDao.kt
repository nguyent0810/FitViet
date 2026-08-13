package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fitviet.app.data.local.entity.MonthlyPlanDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyPlanDayDao {
    @Query(
        """
        SELECT d.* FROM monthly_plan_days d
        INNER JOIN monthly_plan_weeks w ON w.id = d.monthlyPlanWeekId
        WHERE w.monthlyPlanId = :monthlyPlanId
        ORDER BY d.effectiveEpochDay, d.id ASC
        """,
    )
    fun observeForPlan(monthlyPlanId: Long): Flow<List<MonthlyPlanDayEntity>>

    @Query("SELECT * FROM monthly_plan_days WHERE id = :id")
    suspend fun getById(id: Long): MonthlyPlanDayEntity?

    /** "Hit & Run" (Gate 63+) Regenerate UI's day-detail screen — reactive counterpart to
     * [getById] so a reschedule/regenerate elsewhere is reflected live without a manual refresh. */
    @Query("SELECT * FROM monthly_plan_days WHERE id = :id")
    fun observeById(id: Long): Flow<MonthlyPlanDayEntity?>

    @Query("SELECT * FROM monthly_plan_days WHERE monthlyPlanWeekId = :weekId ORDER BY effectiveEpochDay")
    suspend fun getForWeek(weekId: Long): List<MonthlyPlanDayEntity>

    /** Backs the Dashboard "Today" card lookup — at most one plan is ever
     * [com.fitviet.app.domain.MonthlyPlanStatus.ACTIVE] at a time (enforced by the repository, not
     * this query), so [monthlyPlanId] scoping isn't needed here. */
    @Query(
        """
        SELECT d.* FROM monthly_plan_days d
        INNER JOIN monthly_plan_weeks w ON w.id = d.monthlyPlanWeekId
        WHERE w.monthlyPlanId = :monthlyPlanId AND d.effectiveEpochDay = :epochDay
        ORDER BY CASE
            WHEN d.status = 'RESCHEDULED' AND d.isRestDay = 0 THEN 0
            WHEN d.isRestDay = 0 THEN 1
            ELSE 2
        END, d.id
        LIMIT 1
        """,
    )
    suspend fun getForPlanOnDay(monthlyPlanId: Long, epochDay: Long): MonthlyPlanDayEntity?

    /** [com.fitviet.app.domain.MissedWorkoutDetector]'s scan — every SCHEDULED, non-rest day whose
     * [MonthlyPlanDayEntity.effectiveEpochDay] has already passed. Status filtering happens in
     * Kotlin against the loaded rows (small per-plan row count), not in this query, so the enum
     * name string stays the single source of truth rather than duplicated into SQL literals. */
    @Query(
        """
        SELECT d.* FROM monthly_plan_days d
        INNER JOIN monthly_plan_weeks w ON w.id = d.monthlyPlanWeekId
        WHERE w.monthlyPlanId = :monthlyPlanId AND d.isRestDay = 0 AND d.effectiveEpochDay < :todayEpochDay
        ORDER BY d.effectiveEpochDay ASC, d.id ASC
        """,
    )
    suspend fun getPastNonRestDays(monthlyPlanId: Long, todayEpochDay: Long): List<MonthlyPlanDayEntity>

    @Insert
    suspend fun insert(day: MonthlyPlanDayEntity): Long

    @Update
    suspend fun update(day: MonthlyPlanDayEntity)

    @Delete
    suspend fun delete(day: MonthlyPlanDayEntity)
}
