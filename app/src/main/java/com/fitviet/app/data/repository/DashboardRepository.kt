package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.MealDao
import com.fitviet.app.data.local.dao.ProgramDao
import com.fitviet.app.data.local.dao.WorkoutSessionDao
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.domain.CompletedSession
import com.fitviet.app.domain.DashboardStats
import com.fitviet.app.domain.DashboardStatsCalculator
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

data class DashboardData(
    val stats: DashboardStats,
    val kcalToday: Int,
    val featuredProgram: ProgramEntity?,
)

class DashboardRepository(
    private val workoutSessionDao: WorkoutSessionDao,
    private val mealDao: MealDao,
    private val programDao: ProgramDao,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<DashboardData> {
        val zone = ZoneId.systemDefault()

        // Re-subscribes to the day's meals and re-derives "today" whenever the calendar day
        // rolls over, so a long-lived screen doesn't silently compute stats for yesterday.
        return dayTicker(zone).flatMapLatest { today ->
            combine(
                workoutSessionDao.observeCompleted(),
                mealDao.observeForDay(today.toEpochDay()),
                programDao.observeAll(),
            ) { sessions, meals, programs ->
                val completedSessions = sessions.mapNotNull { session ->
                    val completedAt = session.completedAt ?: return@mapNotNull null
                    CompletedSession(
                        date = Instant.ofEpochMilli(completedAt).atZone(zone).toLocalDate(),
                        volumeKg = session.totalVolumeKg,
                    )
                }
                DashboardData(
                    stats = DashboardStatsCalculator.compute(completedSessions, today),
                    kcalToday = meals.sumOf { it.kcal },
                    featuredProgram = programs.firstOrNull(),
                )
            }
        }
    }
}
