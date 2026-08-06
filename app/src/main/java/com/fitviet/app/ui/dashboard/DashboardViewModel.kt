package com.fitviet.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.data.repository.DashboardRepository
import com.fitviet.app.domain.DashboardStats
import com.fitviet.app.domain.DashboardStatsCalculator
import com.fitviet.app.domain.DayVolume
import com.fitviet.app.domain.MuscleGroupWorkload
import com.fitviet.app.domain.NextTraining
import com.fitviet.app.domain.ProgramProgress
import com.fitviet.app.domain.Recommendation
import com.fitviet.app.domain.StatsRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val stats: DashboardStats = DashboardStats(0, 0, 0.0, emptyList()),
    val kcalToday: Int = 0,
    val kcalGoal: Int = 2200,
    val featuredProgram: ProgramEntity? = null,
    val recommendation: Recommendation? = null,
    val nextTraining: NextTraining? = null,
    val programProgress: ProgramProgress? = null,
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
)

class DashboardViewModel(private val repository: DashboardRepository) : ViewModel() {
    private val selectedRange = MutableStateFlow(StatsRange.WEEK)

    // Null = "no explicit tap yet for the current range" -> falls back to the series' last (most
    // recent) bar, same default WEEK always had. selectRange() clears this back to null so
    // switching ranges resets the highlighted bar instead of carrying over a now-meaningless index
    // from a series of a different length.
    private val explicitDayIndex = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observe(),
        selectedRange,
        explicitDayIndex,
    ) { data, range, explicitIndex ->
        val series = DashboardStatsCalculator.rangeSeries(data.completedSessions, data.today, range)
        DashboardUiState(
            stats = data.stats,
            kcalToday = data.kcalToday,
            featuredProgram = data.featuredProgram,
            recommendation = data.recommendation,
            nextTraining = data.nextTraining,
            programProgress = data.programProgress,
            selectedRange = range,
            rangeSeries = series,
            selectedDayIndex = explicitIndex ?: series.lastIndex.coerceAtLeast(0),
            muscleGroupWorkloadThisWeek = data.muscleGroupWorkloadThisWeek,
            showRecommendationCard = data.showRecommendationCard,
            showMuscleBalanceCard = data.showMuscleBalanceCard,
            showNutritionCard = data.showNutritionCard,
            displayName = data.displayName,
            avatarId = data.avatarId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun selectDay(index: Int) {
        explicitDayIndex.value = index
    }

    fun selectRange(range: StatsRange) {
        selectedRange.value = range
        explicitDayIndex.value = null
    }

    class Factory(private val repository: DashboardRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DashboardViewModel(repository) as T
    }
}
