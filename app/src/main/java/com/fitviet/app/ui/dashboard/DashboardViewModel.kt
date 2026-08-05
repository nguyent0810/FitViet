package com.fitviet.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.data.repository.DashboardRepository
import com.fitviet.app.domain.DashboardStats
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
    val selectedDayIndex: Int = 6,
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
            selectedDayIndex = selected,
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
