package com.fitviet.app.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.dao.PersonalBestRow
import com.fitviet.app.data.local.entity.WorkoutSessionEntity
import com.fitviet.app.data.repository.DiaryRepository
import com.fitviet.app.domain.DayVolume
import com.fitviet.app.domain.WeekVolume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

private const val RECENT_SESSIONS_DISPLAY_LIMIT = 3

data class DiaryUiState(
    val last7Days: List<DayVolume> = emptyList(),
    val last4Weeks: List<WeekVolume> = emptyList(),
    val personalBests: List<PersonalBestRow> = emptyList(),
    /** All completed sessions — used to look up a specific day-strip date's real duration/volume. */
    val allCompletedSessions: List<WorkoutSessionEntity> = emptyList(),
    /** The capped slice actually shown in the "Buổi gần đây" list. */
    val recentSessions: List<WorkoutSessionEntity> = emptyList(),
    val selectedDayIndex: Int = 6,
    val selectedWeekIndex: Int = 3,
)

class DiaryViewModel(repository: DiaryRepository) : ViewModel() {
    private val selectedDayIndex = MutableStateFlow(6)
    private val selectedWeekIndex = MutableStateFlow(3)

    val uiState: StateFlow<DiaryUiState> = combine(
        repository.observe(),
        selectedDayIndex,
        selectedWeekIndex,
    ) { data, dayIndex, weekIndex ->
        DiaryUiState(
            last7Days = data.last7Days,
            last4Weeks = data.last4Weeks,
            personalBests = data.personalBests,
            allCompletedSessions = data.completedSessions,
            recentSessions = data.completedSessions.take(RECENT_SESSIONS_DISPLAY_LIMIT),
            selectedDayIndex = dayIndex.coerceIn(0, (data.last7Days.size - 1).coerceAtLeast(0)),
            selectedWeekIndex = weekIndex.coerceIn(0, (data.last4Weeks.size - 1).coerceAtLeast(0)),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaryUiState())

    fun selectDay(index: Int) {
        selectedDayIndex.value = index
    }

    fun selectWeek(index: Int) {
        selectedWeekIndex.value = index
    }

    class Factory(private val repository: DiaryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DiaryViewModel(repository) as T
    }
}
