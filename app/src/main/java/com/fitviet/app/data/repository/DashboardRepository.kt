package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.MealDao
import com.fitviet.app.data.local.dao.MeasurementDao
import com.fitviet.app.data.local.dao.SetLogDao
import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.dao.WorkoutSessionDao
import com.fitviet.app.data.local.entity.MonthlyPlanDayEntity
import com.fitviet.app.data.local.entity.MonthlyPlanEntity
import com.fitviet.app.data.local.entity.SettingsEntity
import com.fitviet.app.domain.CompletedSession
import com.fitviet.app.domain.CompletedSet
import com.fitviet.app.domain.DashboardStats
import com.fitviet.app.domain.DashboardStatsCalculator
import com.fitviet.app.domain.ExerciseSetRecord
import com.fitviet.app.domain.MonthlyPlanProgress
import com.fitviet.app.domain.MonthlyPlanTodayResolver
import com.fitviet.app.domain.MuscleGroupWorkload
import com.fitviet.app.domain.PersonalRecordCalculator
import com.fitviet.app.domain.Recommendation
import com.fitviet.app.domain.RecommendationCalculator
import com.fitviet.app.domain.TodayMonthlyPlanCard
import com.fitviet.app.domain.WeekDayCell
import com.fitviet.app.domain.WeekDayCellCalculator
import com.fitviet.app.domain.WorkoutCompositionCalculator
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

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
    /** Redesign Gate 2c — the Today card's "NGÀY N/Y" label. Null together with no active plan;
     * [dayOfPlan] can also be null on its own when [TodayMonthlyPlanCard.PlanFinished] (an active
     * plan exists but today is past its last row) while [totalDaysInPlan] stays non-null — see
     * [MonthlyPlanProgress.dayOfPlan]'s own doc for why N is a row count, not date arithmetic. */
    val dayOfPlan: Int?,
    val totalDaysInPlan: Int?,
    /** Redesign Gate 2c — "Tuần này" card's 7 circles, trailing 7-day-ending-today window (see
     * [WeekDayCellCalculator]'s own doc for why this isn't a calendar Mon-Sun week). */
    val weekDayCells: List<WeekDayCell>,
    /** Redesign Gate 2c — Dashboard's "PR" stat card, same trailing-7-day window as
     * [weekDayCells]/the volume chart (see [PersonalRecordCalculator]'s own doc). */
    val prCountThisWeek: Int,
    /** Redesign Gate 2c — Dashboard's "Buổi tuần này" stat card numerator, same trailing-7-day
     * window as [weekDayCells], deliberately not [DashboardStats.sessionsThisWeek] (that one's a
     * calendar Mon-Sun count, which would show a different number than the circle row right below
     * it). Paired with [weeklySessionTarget] as the card's denominator. */
    val sessionsRolling7Day: Int,
    /** The active plan's own `daysPerWeek` — [sessionsRolling7Day]'s denominator. Null with no
     * active plan (same as [dayOfPlan]/[totalDaysInPlan]). */
    val weeklySessionTarget: Int?,
    /** Redesign Gate 2c — the "Kg tuần" stat tile's real numerator, same trailing-7-day window as
     * [weekDayCells]/[sessionsRolling7Day]. Deliberately not [DashboardStats.volumeThisWeekKg]
     * (that one's a calendar Mon-Sun sum, which disagreed with the 7 circles/volume-chart bars
     * directly beneath this tile on every day but Monday). */
    val volumeRolling7DayKg: Double,
    /** Gate D4 — read straight from [com.fitviet.app.data.local.entity.SettingsEntity], threaded
     * through so [com.fitviet.app.ui.dashboard.DashboardViewModel] can derive
     * [com.fitviet.app.domain.StreakMilestones.crossedMilestone] without a second settings
     * subscription. */
    val lastCelebratedStreakDays: Int,
)

/** Output of the first 5-source `combine{}` in [DashboardRepository.observe] — chained through two
 * more 2-flow `.combine()` stages (the active plan's own day list, then the completed-set
 * breakdown) since kotlinx.coroutines has no typed `combine{}` overload past 5 flows. Named fields
 * instead of a positional tuple so each chained `.combine()` can destructure it clearly. */
private data class Stage1Data(
    val today: LocalDate,
    val completedSessions: List<CompletedSession>,
    val stats: DashboardStats,
    val kcalToday: Int,
    val monthlyPlanCard: TodayMonthlyPlanCard,
    val recommendation: Recommendation,
    val settings: SettingsEntity,
)

/** [plan] is null with no active plan, in which case [days] is always the empty list (never null
 * itself — there's simply nothing to list). */
private data class PlanData(
    val plan: MonthlyPlanEntity?,
    val days: List<MonthlyPlanDayEntity>,
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
    // reads at all. Redesign Gate 2c added the active plan's own entity/day-list subscription
    // (below) for the Today card's "NGÀY N/Y" label and the "Tuần này" card.
    private val monthlyPlanRepository: MonthlyPlanRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<DashboardData> {
        val zone = ZoneId.systemDefault()

        // Re-subscribes to the day's meals and re-derives "today" whenever the calendar day
        // rolls over, so a long-lived screen doesn't silently compute stats for yesterday.
        return dayTicker(zone).flatMapLatest { today ->
            val stage1Flow = combine(
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

            // Active plan's own entity + day list, for the Today card's "NGÀY N/Y" label and the
            // "Tuần này" card. Built as its own flow (not `flatMapLatest`-nested inside stage1Flow)
            // so it only re-subscribes to `observeActivePlanId()` when `today` changes, not on
            // every session/meal/measurement/settings emission stage1Flow produces — nesting it
            // would silently re-run a full plan+day-list requery on every one of those, and
            // `observeActivePlanId()` is itself `settingsDao.observe().map{}`, so a settings write
            // would otherwise trigger it twice over (once directly via stage1Flow, once via this).
            val planFlow = monthlyPlanRepository.observeActivePlanId().flatMapLatest { planId ->
                if (planId == null) {
                    flowOf(PlanData(plan = null, days = emptyList()))
                } else {
                    combine(
                        monthlyPlanRepository.observePlan(planId),
                        monthlyPlanRepository.observeDaysForPlan(planId),
                    ) { plan, days -> PlanData(plan, days) }
                }
            }

            // Completed-set breakdown (feature #5's muscle-group workload + Gate 2c's weekly PR
            // count) joins as a third flow rather than folding into the combine above, same "past
            // 5 flows, no typed overload" reason as Stage1Data's doc.
            combine(stage1Flow, planFlow, setLogDao.observeCompletedSetBreakdown()) { stage1, planData, setBreakdown ->
                val completedSets = setBreakdown.map { row ->
                    CompletedSet(
                        date = Instant.ofEpochMilli(row.completedAt).atZone(zone).toLocalDate(),
                        muscleGroupCode = row.muscleGroupCode,
                        movementType = row.movementType,
                        volumeKg = row.weightKg * row.reps,
                    )
                }
                val windowStart = stage1.today.minusDays(6)
                val trainedDates = stage1.completedSessions.map { it.date }.toSet()
                // Same collision-aware resolution the Today card itself is built from (see
                // MonthlyPlanTodayResolver's own "push-to-today leaves the original row in place"
                // doc) — a bare date-match `firstOrNull` could pick a different row than the card
                // on a collision day, describing a rest row's position while the card shows the
                // pushed session.
                val todayDayId = MonthlyPlanTodayResolver.resolve(planData.days, stage1.today)?.id
                val prSets = setBreakdown.map { row ->
                    ExerciseSetRecord(
                        exerciseId = row.exerciseId,
                        date = Instant.ofEpochMilli(row.completedAt).atZone(zone).toLocalDate(),
                        weightKg = row.weightKg,
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
                    dayOfPlan = todayDayId?.let { MonthlyPlanProgress.dayOfPlan(planData.days, it) },
                    totalDaysInPlan = planData.plan?.let { it.totalWeeks * 7 },
                    weekDayCells = WeekDayCellCalculator.compute(stage1.today, trainedDates),
                    prCountThisWeek = PersonalRecordCalculator.countInWindow(prSets, windowStart, stage1.today),
                    // Session count, not distinct trained-day count (matches DashboardStats
                    // .sessionsThisWeek's own definition — a day with 2 sessions counts as 2).
                    sessionsRolling7Day = stage1.completedSessions.count { it.date in windowStart..stage1.today },
                    weeklySessionTarget = planData.plan?.daysPerWeek,
                    volumeRolling7DayKg = stage1.completedSessions
                        .filter { it.date in windowStart..stage1.today }
                        .sumOf { it.volumeKg },
                    lastCelebratedStreakDays = stage1.settings.lastCelebratedStreakDays,
                )
            }
        }.flowOn(Dispatchers.Default)
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
