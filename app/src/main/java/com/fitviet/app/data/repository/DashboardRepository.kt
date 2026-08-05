package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.ExerciseDao
import com.fitviet.app.data.local.dao.MealDao
import com.fitviet.app.data.local.dao.MeasurementDao
import com.fitviet.app.data.local.dao.ProgramDao
import com.fitviet.app.data.local.dao.ProgramDayDao
import com.fitviet.app.data.local.dao.ProgramExerciseDao
import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.dao.WorkoutSessionDao
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.domain.CompletedSession
import com.fitviet.app.domain.DashboardStats
import com.fitviet.app.domain.DashboardStatsCalculator
import com.fitviet.app.domain.NextTraining
import com.fitviet.app.domain.NextTrainingCalculator
import com.fitviet.app.domain.ProgramProgress
import com.fitviet.app.domain.ProgramScheduleCalculator
import com.fitviet.app.domain.Recommendation
import com.fitviet.app.domain.RecommendationCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class DashboardData(
    val stats: DashboardStats,
    val kcalToday: Int,
    val featuredProgram: ProgramEntity?,
    val recommendation: Recommendation,
    /** Feature #3: the active program's next scheduled non-rest day, null if there's no active
     * program or its schedule has no training days (e.g. not yet seeded). */
    val nextTraining: NextTraining?,
    /** Feature #3: this week's session count vs. the active program's weekly target — null only
     * when there's no featured program at all (see [NextTraining]/[ProgramProgress] docs for the
     * "session count, not per-day tracking" scope note). */
    val programProgress: ProgramProgress?,
)

/** Everything computable before the active program's schedule is known — kept separate from
 * [DashboardData] because resolving the schedule needs [featuredProgram]'s id, which isn't known
 * until this stage's own `combine{}` runs (hence the second [Flow.flatMapLatest] stage below). */
private data class BaseDashboardData(
    val today: LocalDate,
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
    private val settingsDao: SettingsDao,
    private val programDayDao: ProgramDayDao,
    private val programExerciseDao: ProgramExerciseDao,
    private val exerciseDao: ExerciseDao,
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
                settingsDao.observe(),
            ) { sessions, meals, programs, latestMeasurement, settings ->
                val completedSessions = sessions.mapNotNull { session ->
                    val completedAt = session.completedAt ?: return@mapNotNull null
                    CompletedSession(
                        date = Instant.ofEpochMilli(completedAt).atZone(zone).toLocalDate(),
                        volumeKg = session.totalVolumeKg,
                    )
                }
                val stats = DashboardStatsCalculator.compute(completedSessions, today)
                // Falls back to the first seeded program until the user explicitly picks one on
                // 2b (see ProgramRepository.setActiveProgram) — matches the pre-Gate-15 default.
                val featuredProgram = programs.firstOrNull { it.id == settings?.activeProgramId } ?: programs.firstOrNull()
                BaseDashboardData(
                    today = today,
                    stats = stats,
                    kcalToday = meals.sumOf { it.kcal },
                    featuredProgram = featuredProgram,
                    recommendation = RecommendationCalculator.compute(
                        today = today,
                        last7Days = stats.last7Days,
                        streakDays = stats.streakDays,
                        lastMeasurementDate = latestMeasurement?.let { LocalDate.ofEpochDay(it.epochDay) },
                    ),
                )
            }
        }.flatMapLatest { base ->
            val program = base.featuredProgram
            val scheduleFlow = if (program != null) {
                combine(
                    programDayDao.observeForProgram(program.id),
                    programExerciseDao.observeForProgram(program.id),
                    exerciseDao.observeAll(),
                    ProgramScheduleCalculator::build,
                )
            } else {
                flowOf(emptyList())
            }
            scheduleFlow.map { schedule ->
                DashboardData(
                    stats = base.stats,
                    kcalToday = base.kcalToday,
                    featuredProgram = program,
                    recommendation = base.recommendation,
                    nextTraining = NextTrainingCalculator.findNext(schedule, base.today.dayOfWeek),
                    programProgress = program?.let { ProgramProgress(base.stats.sessionsThisWeek, it.sessionsPerWeek) },
                )
            }
        }
    }
}
