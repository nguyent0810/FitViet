package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.fitviet.app.data.local.entity.MonthlyPlanExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyPlanExerciseDao {
    @Query("SELECT * FROM monthly_plan_exercises WHERE monthlyPlanDayId = :monthlyPlanDayId ORDER BY orderIndex")
    fun observeForDay(monthlyPlanDayId: Long): Flow<List<MonthlyPlanExerciseEntity>>

    @Query("SELECT * FROM monthly_plan_exercises WHERE monthlyPlanDayId = :monthlyPlanDayId ORDER BY orderIndex")
    suspend fun getForDay(monthlyPlanDayId: Long): List<MonthlyPlanExerciseEntity>

    @Query("SELECT * FROM monthly_plan_exercises WHERE id = :id")
    suspend fun getById(id: Long): MonthlyPlanExerciseEntity?

    /** The PR-integration hook's target: every not-yet-locked row for [exerciseId] within the
     * current plan, so a fresh PR can bump future weeks without touching completed history. Lock
     * status itself is checked by the repository (via `workout_sessions.monthlyPlanDayId`), not
     * here — this just narrows by plan + exercise. */
    @Query(
        """
        SELECT e.* FROM monthly_plan_exercises e
        INNER JOIN monthly_plan_days d ON d.id = e.monthlyPlanDayId
        INNER JOIN monthly_plan_weeks w ON w.id = d.monthlyPlanWeekId
        WHERE w.monthlyPlanId = :monthlyPlanId AND e.exerciseId = :exerciseId
        """,
    )
    suspend fun getForPlanAndExercise(monthlyPlanId: Long, exerciseId: Long): List<MonthlyPlanExerciseEntity>

    @Insert
    suspend fun insertAll(exercises: List<MonthlyPlanExerciseEntity>)

    @Delete
    suspend fun deleteAll(exercises: List<MonthlyPlanExerciseEntity>)

    @Query("DELETE FROM monthly_plan_exercises WHERE monthlyPlanDayId = :monthlyPlanDayId")
    suspend fun deleteForDay(monthlyPlanDayId: Long)
}
