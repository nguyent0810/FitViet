package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.ExerciseDao
import com.fitviet.app.data.local.dao.MealDao
import com.fitviet.app.data.local.dao.MeasurementDao
import com.fitviet.app.data.local.dao.MonthlyPlanDayDao
import com.fitviet.app.data.local.dao.MonthlyPlanExerciseDao
import com.fitviet.app.data.local.dao.ProgramDao
import com.fitviet.app.data.local.dao.ProgramDayDao
import com.fitviet.app.data.local.dao.ProgramExerciseDao
import com.fitviet.app.data.local.dao.SetLogDao
import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.dao.WorkoutSessionDao
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.data.local.entity.SettingsEntity
import com.fitviet.app.domain.CompletedSession
import com.fitviet.app.domain.CompletedSet
import com.fitviet.app.domain.DashboardStats
import com.fitviet.app.domain.DashboardStatsCalculator
import com.fitviet.app.domain.MonthlyPlanDayCardEstimator
import com.fitviet.app.domain.MonthlyPlanTodayResolver
import com.fitviet.app.domain.MuscleGroupWorkload
import com.fitviet.app.domain.NextTraining
import com.fitviet.app.domain.NextTrainingCalculator
import com.fitviet.app.domain.ProgramProgress
import com.fitviet.app.domain.ProgramScheduleCalculator
import com.fitviet.app.domain.Recommendation
import com.fitviet.app.domain.RecommendationCalculator
import com.fitviet.app.domain.TodayMonthlyPlanCard
import com.fitviet.app.domain.WorkoutCompositionCalculator
import java.time.DayOfWeek
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
    val today: LocalDate,
    /** Feature #7 (Gate 43) — the raw completed-session list [stats] was computed from, threaded
     * through so [com.fitviet.app.ui.dashboard.DashboardViewModel] can derive range-selectable
     * series client-side via [DashboardStatsCalculator.rangeSeries] without a second DB
     * subscription duplicating [com.fitviet.app.data.local.dao.WorkoutSessionDao.observeCompleted]. */
    val completedSessions: List<CompletedSession>,
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
    /** Feature #5 — this week's (Monday-start) set distribution across muscle groups, always 6
     * entries. A quick "balance" glance, distinct from Diary's #8 chart (that one's a 4-week
     * volume-based window; this one's set-count-based and scoped to just the current week). */
    val muscleGroupWorkloadThisWeek: List<MuscleGroupWorkload>,
    /** Feature #12 — per-widget Dashboard visibility, read straight from [com.fitviet.app.data.local.entity.SettingsEntity]. */
    val showRecommendationCard: Boolean,
    val showMuscleBalanceCard: Boolean,
    val showNutritionCard: Boolean,
    /** Feature #1 (Gate 35) — the user's editable display name/avatar, read straight from
     * [com.fitviet.app.data.local.entity.SettingsEntity]. */
    val displayName: String,
    val avatarId: Int,
    /** "Hit & Run" (Gate 63+) — null when there's no active monthly plan, in which case
     * [featuredProgram]/[nextTraining]/[programProgress] drive the hero card exactly as before this
     * feature. Independent of [featuredProgram]: both can be non-null at once (see
     * [com.fitviet.app.data.repository.MonthlyPlanRepository]'s doc), but the hero card prefers
     * this when present. */
    val todayMonthlyPlanCard: TodayMonthlyPlanCard?,
)

/** Output of the first 5-source `combine{}` below — everything except the completed-set breakdown
 * (a 6th independent source, chained on via a separate 2-flow `.combine()` since kotlinx.coroutines
 * has no typed `combine{}` overload past 5 flows). Named fields instead of nested `Pair`/`Triple`
 * so the chained `.combine()` below can destructure it without a multi-level positional-tuple trace. */
private data class Stage1Data(
    val today: LocalDate,
    val completedSessions: List<CompletedSession>,
    val stats: DashboardStats,
    val kcalToday: Int,
    val featuredProgram: ProgramEntity?,
    val recommendation: Recommendation,
    val settings: SettingsEntity,
)

/** Everything computable before the active program's schedule is known — kept separate from
 * [DashboardData] because resolving the schedule needs [featuredProgram]'s id, which isn't known
 * until this stage's own `combine{}` runs (hence the second [Flow.flatMapLatest] stage below). */
private data class BaseDashboardData(
    val today: LocalDate,
    val completedSessions: List<CompletedSession>,
    val stats: DashboardStats,
    val kcalToday: Int,
    val featuredProgram: ProgramEntity?,
    val recommendation: Recommendation,
    val muscleGroupWorkloadThisWeek: List<MuscleGroupWorkload>,
    val showRecommendationCard: Boolean,
    val showMuscleBalanceCard: Boolean,
    val showNutritionCard: Boolean,
    val displayName: String,
    val avatarId: Int,
    val activeMonthlyPlanId: Long?,
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
    private val setLogDao: SetLogDao,
    private val monthlyPlanDayDao: MonthlyPlanDayDao,
    private val monthlyPlanExerciseDao: MonthlyPlanExerciseDao,
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
                Stage1Data(
                    today = today,
                    completedSessions = completedSessions,
                    stats = stats,
                    kcalToday = meals.sumOf { it.kcal },
                    featuredProgram = featuredProgram,
                    recommendation = RecommendationCalculator.compute(
                        today = today,
                        last7Days = stats.last7Days,
                        streakDays = stats.streakDays,
                        lastMeasurementDate = latestMeasurement?.let { LocalDate.ofEpochDay(it.epochDay) },
                    ),
                    settings = settings ?: SettingsEntity(),
                )
            }
            // 6th independent source (completed-set breakdown, for feature #5) — chained via a
            // 2-flow `combine` rather than a 6-arg `combine{}`, which kotlinx.coroutines doesn't
            // offer a typed overload for.
            .combine(setLogDao.observeCompletedSetBreakdown()) { stage1, setBreakdown ->
                val completedSets = setBreakdown.map { row ->
                    CompletedSet(
                        date = Instant.ofEpochMilli(row.completedAt).atZone(zone).toLocalDate(),
                        muscleGroupCode = row.muscleGroupCode,
                        movementType = row.movementType,
                        volumeKg = row.weightKg * row.reps,
                    )
                }
                BaseDashboardData(
                    today = stage1.today,
                    completedSessions = stage1.completedSessions,
                    stats = stage1.stats,
                    kcalToday = stage1.kcalToday,
                    featuredProgram = stage1.featuredProgram,
                    recommendation = stage1.recommendation,
                    muscleGroupWorkloadThisWeek = WorkoutCompositionCalculator.muscleGroupWorkload(
                        completedSets,
                        since = stage1.today.with(DayOfWeek.MONDAY),
                    ),
                    showRecommendationCard = stage1.settings.showRecommendationCard,
                    showMuscleBalanceCard = stage1.settings.showMuscleBalanceCard,
                    showNutritionCard = stage1.settings.showNutritionCard,
                    displayName = stage1.settings.displayName,
                    avatarId = stage1.settings.avatarId,
                    activeMonthlyPlanId = stage1.settings.activeMonthlyPlanId,
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
            combine(scheduleFlow, observeTodayMonthlyPlanCard(base.activeMonthlyPlanId, base.today)) { schedule, monthlyPlanCard ->
                DashboardData(
                    today = base.today,
                    completedSessions = base.completedSessions,
                    stats = base.stats,
                    kcalToday = base.kcalToday,
                    featuredProgram = program,
                    recommendation = base.recommendation,
                    nextTraining = NextTrainingCalculator.findNext(schedule, base.today.dayOfWeek),
                    programProgress = program?.let { ProgramProgress(base.stats.sessionsThisWeek, it.sessionsPerWeek) },
                    muscleGroupWorkloadThisWeek = base.muscleGroupWorkloadThisWeek,
                    showRecommendationCard = base.showRecommendationCard,
                    showMuscleBalanceCard = base.showMuscleBalanceCard,
                    showNutritionCard = base.showNutritionCard,
                    displayName = base.displayName,
                    avatarId = base.avatarId,
                    todayMonthlyPlanCard = monthlyPlanCard,
                )
            }
        }
    }

    /** [monthlyPlanId] null (no active plan) short-circuits to a constant `null` card without
     * subscribing to anything — the common case for a user who hasn't tried "Hit & Run" yet. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeTodayMonthlyPlanCard(monthlyPlanId: Long?, today: LocalDate): Flow<TodayMonthlyPlanCard?> {
        if (monthlyPlanId == null) return flowOf(null)
        return monthlyPlanDayDao.observeForPlan(monthlyPlanId).flatMapLatest { days ->
            val todayDay = MonthlyPlanTodayResolver.resolve(days, today) ?: return@flatMapLatest flowOf<TodayMonthlyPlanCard?>(null)
            if (todayDay.isRestDay || todayDay.sessionType == null) {
                return@flatMapLatest flowOf<TodayMonthlyPlanCard?>(TodayMonthlyPlanCard.RestDay)
            }
            val sessionType = todayDay.sessionType
            monthlyPlanExerciseDao.observeForDay(todayDay.id).map { exercises ->
                if (exercises.isEmpty()) {
                    null
                } else {
                    TodayMonthlyPlanCard.Training(
                        dayId = todayDay.id,
                        sessionType = sessionType,
                        exerciseCount = exercises.size,
                        estimatedDurationMinutes = MonthlyPlanDayCardEstimator.estimateDurationMinutes(exercises),
                    )
                }
            }
        }
    }
}
