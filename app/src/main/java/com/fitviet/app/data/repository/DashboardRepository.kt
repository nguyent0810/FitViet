package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.MealDao
import com.fitviet.app.data.local.dao.MeasurementDao
import com.fitviet.app.data.local.dao.ProgramDao
import com.fitviet.app.data.local.dao.WorkoutSessionDao
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.domain.CompletedSession
import com.fitviet.app.domain.DashboardStats
import com.fitviet.app.domain.DashboardStatsCalculator
import com.fitviet.app.domain.Recommendation
import com.fitviet.app.domain.RecommendationCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

data class DashboardData(
    val stats: DashboardStats,
    val kcalToday: Int,
    val featuredProgram: ProgramEntity?,
    val recommendation: Recommendation,
)

class DashboardRepository(
    private val workoutSessionDao: WorkoutSessionDao,
    private val mealDao: MealDao,
    private val programDao: ProgramDao,
    private val measurementDao: MeasurementDao,
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
                measurementDao.observeLatest(),
            ) { sessions, meals, programs, latestMeasurement ->
                val completedSessions = sessions.mapNotNull { session ->
                    val completedAt = session.completedAt ?: return@mapNotNull null
                    CompletedSession(
                        date = Instant.ofEpochMilli(completedAt).atZone(zone).toLocalDate(),
                        volumeKg = session.totalVolumeKg,
                    )
                }
                val stats = DashboardStatsCalculator.compute(completedSessions, today)
                DashboardData(
                    stats = stats,
                    kcalToday = meals.sumOf { it.kcal },
                    featuredProgram = programs.firstOrNull(),
                    recommendation = RecommendationCalculator.compute(
                        today = today,
                        last7Days = stats.last7Days,
                        streakDays = stats.streakDays,
                        lastMeasurementDate = latestMeasurement?.let { LocalDate.ofEpochDay(it.epochDay) },
                    ),
                )
            }
        }
    }
}
