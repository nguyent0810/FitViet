package com.fitviet.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.MonthlyPlanDayEntity
import com.fitviet.app.data.repository.DashboardRepository
import com.fitviet.app.data.repository.MonthlyPlanRepository
import com.fitviet.app.domain.DashboardStats
import com.fitviet.app.domain.DashboardStatsCalculator
import com.fitviet.app.domain.DayVolume
import com.fitviet.app.domain.MuscleGroupWorkload
import com.fitviet.app.domain.Recommendation
import com.fitviet.app.domain.StatsRange
import com.fitviet.app.domain.StreakMilestones
import com.fitviet.app.domain.TodayMonthlyPlanCard
import com.fitviet.app.domain.WeekDayCell
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val stats: DashboardStats = DashboardStats(0, 0, 0.0, emptyList()),
    val kcalToday: Int = 0,
    val kcalGoal: Int = 2200,
    val recommendation: Recommendation? = null,
    /** Feature #7 (Gate 43) — the series [selectedRange] currently produces, and which bar within
     * it is highlighted. Defaults to the series' most recent bar whenever the user hasn't
     * explicitly tapped one for the current range (see [DashboardViewModel]'s `explicitDayIndex`). */
    val selectedRange: StatsRange = StatsRange.WEEK,
    val rangeSeries: List<DayVolume> = emptyList(),
    val selectedDayIndex: Int = 6,
    val muscleGroupWorkloadThisWeek: List<MuscleGroupWorkload> = emptyList(),
    val showRecommendationCard: Boolean = true,
    val showMuscleBalanceCard: Boolean = true,
    val showNutritionCard: Boolean = true,
    val displayName: String = "",
    val avatarId: Int = 0,
    // "Hit & Run" redesign (Gate 1c) — non-nullable; TodayMonthlyPlanCard.NoPlan is now the "no
    // active plan" case itself, not represented by a null card.
    val todayMonthlyPlanCard: TodayMonthlyPlanCard = TodayMonthlyPlanCard.NoPlan,
    /** Redesign Gate 2c — the Today card's "NGÀY N/Y" label; both null with no active plan. */
    val dayOfPlan: Int? = null,
    val totalDaysInPlan: Int? = null,
    /** Redesign Gate 2c — "Tuần này" card's 7 circles, always 7 entries (see
     * [com.fitviet.app.domain.WeekDayCellCalculator]'s own doc for the window definition). */
    val weekDayCells: List<WeekDayCell> = emptyList(),
    val prCountThisWeek: Int = 0,
    val sessionsRolling7Day: Int = 0,
    val weeklySessionTarget: Int? = null,
    val volumeRolling7DayKg: Double = 0.0,
    /** "Hit & Run" (Gate 63+) adaptive scheduling — the oldest still-unresolved missed training
     * day from the active plan, checked once per Dashboard load (not continuously re-scanned; see
     * [DashboardViewModel]'s `init` block), or null if there isn't one / there's no active plan.
     * Only ever one at a time is surfaced. Push/skip resolves it so the next-oldest one (if any)
     * can surface on a later load; viewing the plan leaves it unresolved so it may surface again. */
    val missedDay: MonthlyPlanDayEntity? = null,
    /** Gate D4 — the milestone (7/14/30/60) [com.fitviet.app.domain.StreakMilestones] says
     * [stats]'s current streak just crossed and hasn't been celebrated yet, or null. Unlike
     * [missedDay]'s one-shot-per-load `MutableStateFlow`, this is derived straight from the
     * repository's own reactive data on every emission — dismissing it persists
     * `lastCelebratedStreakDays` via [DashboardRepository.celebrateStreakMilestone], which is what
     * actually makes this go back to null (see [dismissStreakMilestone]), not any local UI state. */
    val streakMilestone: Int? = null,
)

class DashboardViewModel(
    private val repository: DashboardRepository,
    private val monthlyPlanRepository: MonthlyPlanRepository,
) : ViewModel() {
    private val selectedRange = MutableStateFlow(StatsRange.WEEK)

    // Null = "no explicit tap yet for the current range" -> falls back to the series' last (most
    // recent) bar, same default WEEK always had. selectRange() clears this back to null so
    // switching ranges resets the highlighted bar instead of carrying over a now-meaningless index
    // from a series of a different length.
    private val explicitDayIndex = MutableStateFlow<Int?>(null)

    // "On Dashboard load" per the "Hit & Run" plan's adaptive-scheduling section — a one-shot
    // check per ViewModel instance (i.e. per time the user navigates to Dashboard), not a
    // continuous Flow subscription; resolving the prompt updates this directly rather than
    // re-running the whole scan, so it can't re-surface a day the user just acted on this session.
    private val missedDay = MutableStateFlow<MonthlyPlanDayEntity?>(null)

    init {
        viewModelScope.launch {
            val planId = monthlyPlanRepository.observeActivePlanId().first() ?: return@launch
            missedDay.value = monthlyPlanRepository.findMissedDays(planId, LocalDate.now()).firstOrNull()
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observe(),
        selectedRange,
        explicitDayIndex,
        missedDay,
    ) { data, range, explicitIndex, missed ->
        val series = DashboardStatsCalculator.rangeSeries(data.completedSessions, data.today, range)
        DashboardUiState(
            stats = data.stats,
            kcalToday = data.kcalToday,
            recommendation = data.recommendation,
            selectedRange = range,
            rangeSeries = series,
            selectedDayIndex = explicitIndex ?: series.lastIndex.coerceAtLeast(0),
            muscleGroupWorkloadThisWeek = data.muscleGroupWorkloadThisWeek,
            showRecommendationCard = data.showRecommendationCard,
            showMuscleBalanceCard = data.showMuscleBalanceCard,
            showNutritionCard = data.showNutritionCard,
            displayName = data.displayName,
            avatarId = data.avatarId,
            todayMonthlyPlanCard = data.todayMonthlyPlanCard,
            dayOfPlan = data.dayOfPlan,
            totalDaysInPlan = data.totalDaysInPlan,
            weekDayCells = data.weekDayCells,
            prCountThisWeek = data.prCountThisWeek,
            sessionsRolling7Day = data.sessionsRolling7Day,
            weeklySessionTarget = data.weeklySessionTarget,
            volumeRolling7DayKg = data.volumeRolling7DayKg,
            missedDay = missed,
            streakMilestone = StreakMilestones.crossedMilestone(data.stats.streakDays, data.lastCelebratedStreakDays),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun selectDay(index: Int) {
        explicitDayIndex.value = index
    }

    fun selectRange(range: StatsRange) {
        selectedRange.value = range
        explicitDayIndex.value = null
    }

    /** Adaptive scheduling's "dời sang hôm nay" choice — see
     * [MonthlyPlanRepository.pushMissedDayToToday]'s "no cascade" doc. */
    fun pushMissedDayToToday() {
        val day = missedDay.value ?: return
        missedDay.value = null
        viewModelScope.launch { monthlyPlanRepository.pushMissedDayToToday(day.id, LocalDate.now()) }
    }

    /** Adaptive scheduling's "bỏ qua, tiếp tục lịch" choice. */
    fun skipMissedDay() {
        val day = missedDay.value ?: return
        missedDay.value = null
        viewModelScope.launch { monthlyPlanRepository.skipMissedDay(day.id) }
    }

    /** Adaptive scheduling's "xem lịch tháng" choice — nothing about the missed day is resolved
     * yet, so unlike [pushMissedDayToToday]/[skipMissedDay] this deliberately does NOT call
     * [MonthlyPlanRepository.markMissed] or mutate the day at all; the caller navigates to
     * [com.fitviet.app.ui.monthlyplan.MonthlyPlanDetailScreen] so the user can look at the whole
     * plan before deciding. A dedicated "pick two days, swap them" picker UI is deferred —
     * [MonthlyPlanRepository.swapTwoDays] exists and is ready for a future gate to build a real
     * picker on top of; until then this option's only job is honestly not claiming to rearrange
     * anything itself. Since nothing was resolved, the same day is expected to surface again on
     * the next Dashboard load — that's correct, not a bug (see [findMissedDays]'s scan). */
    fun dismissMissedDayToViewPlan() {
        missedDay.value = null
    }

    /** Gate D4 — persists the current streak as the new "last celebrated" high-water mark, which
     * makes [StreakMilestones.crossedMilestone] return null again on the next data emission and
     * closes the overlay. Reads [uiState]'s already-collected value rather than re-observing stats,
     * matching this ViewModel's existing "no reactive continuation" style for one-shot dismiss
     * actions (see [pushMissedDayToToday]/[skipMissedDay] above). */
    fun dismissStreakMilestone() {
        val streakDays = uiState.value.stats.streakDays
        viewModelScope.launch { repository.celebrateStreakMilestone(streakDays) }
    }

    class Factory(
        private val repository: DashboardRepository,
        private val monthlyPlanRepository: MonthlyPlanRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DashboardViewModel(repository, monthlyPlanRepository) as T
    }
}
