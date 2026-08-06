package com.fitviet.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.data.repository.DashboardRepository
import com.fitviet.app.domain.DashboardStats
import com.fitviet.app.domain.MuscleGroupWorkload
import com.fitviet.app.domain.NextTraining
import com.fitviet.app.domain.ProgramProgress
import com.fitviet.app.domain.Recommendation
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
    val selectedDayIndex: Int = 6,
    val muscleGroupWorkloadThisWeek: List<MuscleGroupWorkload> = emptyList(),
    val showRecommendationCard: Boolean = true,
    val showMuscleBalanceCard: Boolean = true,
    val showNutritionCard: Boolean = true,
    val displayName: String = "",
    val avatarId: Int = 0,
)

class DashboardViewModel(private val repository: DashboardRepository) : ViewModel() {
    private val selectedDayIndex = MutableStateFlow(6)

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observe(),
        selectedDayIndex,
    ) { data, selected ->
        DashboardUiState(
            stats = data.stats,
            kcalToday = data.kcalToday,
            featuredProgram = data.featuredProgram,
            recommendation = data.recommendation,
            nextTraining = data.nextTraining,
            programProgress = data.programProgress,
            selectedDayIndex = selected,
            muscleGroupWorkloadThisWeek = data.muscleGroupWorkloadThisWeek,
            showRecommendationCard = data.showRecommendationCard,
            showMuscleBalanceCard = data.showMuscleBalanceCard,
            showNutritionCard = data.showNutritionCard,
            displayName = data.displayName,
            avatarId = data.avatarId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun selectDay(index: Int) {
        selectedDayIndex.value = index
    }

    class Factory(private val repository: DashboardRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DashboardViewModel(repository) as T
    }
}
