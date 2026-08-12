package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fitviet.app.data.local.entity.UserMealPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMealPlanDao {
    // ORDER BY id DESC is a defensive tiebreaker: the single-ACTIVE-plan invariant is only
    // application-enforced (via supersedeActive() below), not a database constraint, so this
    // stays deterministic (newest wins) even if that invariant is ever violated by a future bug.
    @Query("SELECT * FROM user_meal_plans WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1")
    fun observeActive(): Flow<UserMealPlanEntity?>

    @Query("SELECT * FROM user_meal_plans WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1")
    suspend fun getActive(): UserMealPlanEntity?

    @Query("SELECT * FROM user_meal_plans WHERE id = :id")
    suspend fun getById(id: Long): UserMealPlanEntity?

    @Insert
    suspend fun insert(plan: UserMealPlanEntity): Long

    @Update
    suspend fun update(plan: UserMealPlanEntity)

    /** Supersedes any currently-ACTIVE plan before a new one is generated — same
     * "flip status, never delete" pattern [MonthlyPlanRepository] uses for [MonthlyPlanEntity]. */
    @Query("UPDATE user_meal_plans SET status = 'SUPERSEDED' WHERE status = 'ACTIVE'")
    suspend fun supersedeActive()
}
