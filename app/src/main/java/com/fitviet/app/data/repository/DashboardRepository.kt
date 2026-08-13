package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.MealDao
import com.fitviet.app.data.local.dao.MeasurementDao
import com.fitviet.app.data.local.dao.SetLogDao
import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.dao.WorkoutSessionDao
import com.fitviet.app.data.local.entity.SettingsEntity
import com.fitviet.app.domain.CompletedSession
import com.fitviet.app.domain.CompletedSet
import com.fitviet.app.domain.DashboardStats
import com.fitviet.app.domain.DashboardStatsCalculator
import com.fitviet.app.domain.MuscleGroupWorkload
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

data class DashboardData(
    val today: LocalDate,
    /** Feature #7 (Gate 43) — the raw completed-session list [stats] was computed from, threaded
     * through so [com.fitviet.app.ui.dashboard.DashboardViewModel] can derive range-selectable
     * series client-side via [DashboardStatsCalculator.rangeSeries] without a second DB
     * subscription duplicating [com.fitviet.app.data.local.dao.WorkoutSessionDao.observeCompleted]. */
    val completedSessions: List<CompletedSession>,
    val stats: DashboardStats,
    val kcalToday: Int,
    val recommendation: Recommendation,
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
    /** "Hit & Run" (Gate 63+); redesign Gate 1c made this a total, always-non-null 5-case type
     * (was nullable). Redesign Gate 2b made [TodayMonthlyPlanCard.NoPlan] the sole "nothing to
     * train today" outcome — the old hand-authored-program hero-card fallback this used to defer
     * to is gone; Dashboard shows its empty-state "Tạo plan" CTA instead (see
     * [TodayMonthlyPlanCard]'s own doc). */
    val todayMonthlyPlanCard: TodayMonthlyPlanCard,
    /** Gate D4 — read straight from [com.fitviet.app.data.local.entity.SettingsEntity], threaded
     * through so [com.fitviet.app.ui.dashboard.DashboardViewModel] can derive
     * [com.fitviet.app.domain.StreakMilestones.crossedMilestone] without a second settings
     * subscription. */
    val lastCelebratedStreakDays: Int,
)

/** Output of the first 5-source `combine{}` in [DashboardRepository.observe] — everything except
 * the completed-set breakdown (a 6th independent source, chained on via a separate 2-flow
 * `.combine()` since kotlinx.coroutines has no typed `combine{}` overload past 5 flows). Named
 * fields instead of a positional tuple so the chained `.combine()` can destructure it clearly. */
private data class Stage1Data(
    val today: LocalDate,
    val completedSessions: List<CompletedSession>,
    val stats: DashboardStats,
    val kcalToday: Int,
    val monthlyPlanCard: TodayMonthlyPlanCard,
    val recommendation: Recommendation,
    val settings: SettingsEntity,
)

class DashboardRepository(
    private val workoutSessionDao: WorkoutSessionDao,
    private val mealDao: MealDao,
    private val measurementDao: MeasurementDao,
    private val settingsDao: SettingsDao,
    private val setLogDao: SetLogDao,
    // "Hit & Run" redesign (Gate 1c) — Today-card resolution moved to this repository's own
    // observeTodaySession, shared with WorkoutViewModel's single session entry point; replaces
    // this class's direct monthlyPlanDayDao/monthlyPlanExerciseDao dependencies. Redesign Gate 2b
    // removed the parallel program-schedule resolution this repository used to also run
    // (featuredProgram/nextTraining/programProgress) — this is now the only plan source Dashboard
    // reads at all.
    private val monthlyPlanRepository: MonthlyPlanRepository,
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
                monthlyPlanRepository.observeTodaySession(today),
                measurementDao.observeLatest(),
                settingsDao.observe(),
            ) { sessions, meals, monthlyPlanCard, latestMeasurement, settings ->
                val completedSessions = sessions.mapNotNull { session ->
                    val completedAt = session.completedAt ?: return@mapNotNull null
                    CompletedSession(
                        date = Instant.ofEpochMilli(completedAt).atZone(zone).toLocalDate(),
                        volumeKg = session.totalVolumeKg,
                    )
                }
                val stats = DashboardStatsCalculator.compute(completedSessions, today)
                Stage1Data(
                    today = today,
                    completedSessions = completedSessions,
                    stats = stats,
                    kcalToday = meals.sumOf { it.kcal },
                    monthlyPlanCard = monthlyPlanCard,
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
                DashboardData(
                    today = stage1.today,
                    completedSessions = stage1.completedSessions,
                    stats = stage1.stats,
                    kcalToday = stage1.kcalToday,
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
                    todayMonthlyPlanCard = stage1.monthlyPlanCard,
                    lastCelebratedStreakDays = stage1.settings.lastCelebratedStreakDays,
                )
            }
        }
    }

    /** Gate D4 — persists that a streak-milestone overlay was just shown, so
     * [com.fitviet.app.domain.StreakMilestones.crossedMilestone] doesn't fire it again on the next
     * data emission. Read-modify-write rather than a targeted `@Query UPDATE`, matching this DAO's
     * existing single-row upsert idiom (see [com.fitviet.app.data.repository.ProgramRepository]'s
     * `dismissSupersetHint`). */
    suspend fun celebrateStreakMilestone(streakDays: Int) {
        val current = settingsDao.get() ?: SettingsEntity()
        // maxOf, not a plain overwrite — this is a lifetime high-water mark (see
        // StreakMilestones' doc), so a call carrying a lower value than what's already stored
        // (e.g. delayed by a slow dispatcher after a newer, higher one already landed) must never
        // roll it back.
        val newMark = maxOf(current.lastCelebratedStreakDays, streakDays)
        settingsDao.upsert(current.copy(lastCelebratedStreakDays = newMark))
    }

}
