package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fitviet.app.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE completedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeCompleted(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSessionEntity?

    /** "Hit & Run" (Gate 63+) regenerate-lock check: a
     * [com.fitviet.app.data.local.entity.MonthlyPlanDayEntity] is regenerable iff this returns 0
     * for its id — completed AND abandoned sessions both lock the day (a linked row exists either
     * way), since an abandoned session's logged sets would be silently reinterpreted otherwise. */
    @Query("SELECT COUNT(*) FROM workout_sessions WHERE monthlyPlanDayId = :monthlyPlanDayId")
    suspend fun countForMonthlyPlanDay(monthlyPlanDayId: Long): Int

    /** Reactive counterpart of [countForMonthlyPlanDay] — backs the day-detail screen's lock
     * banner, which needs to react live if a session starts against this day from another screen. */
    @Query("SELECT COUNT(*) FROM workout_sessions WHERE monthlyPlanDayId = :monthlyPlanDayId")
    fun observeCountForMonthlyPlanDay(monthlyPlanDayId: Long): Flow<Int>

    /** "Hit & Run" (Gate 63+) Regenerate UI — the bulk form of [countForMonthlyPlanDay] for
     * rendering a whole plan's day list with lock badges without one query per day. */
    @Query(
        """
        SELECT DISTINCT s.monthlyPlanDayId FROM workout_sessions s
        INNER JOIN monthly_plan_days d ON d.id = s.monthlyPlanDayId
        INNER JOIN monthly_plan_weeks w ON w.id = d.monthlyPlanWeekId
        WHERE w.monthlyPlanId = :monthlyPlanId
        """,
    )
    fun observeLockedMonthlyPlanDayIds(monthlyPlanId: Long): Flow<List<Long>>

    @Insert
    suspend fun insert(session: WorkoutSessionEntity): Long

    @Update
    suspend fun update(session: WorkoutSessionEntity)

    /** Feature #6 (Gate 37) — the destructive "reset app data" settings action. Cascades to
     * `set_logs` via [com.fitviet.app.data.local.entity.SetLogEntity]'s `ForeignKey.CASCADE`. */
    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAll()
}
