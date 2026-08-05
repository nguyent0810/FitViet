package com.fitviet.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.WorkoutSessionEntity
import com.fitviet.app.data.repository.DiaryRepository
import com.fitviet.app.domain.CalendarDayCell
import com.fitviet.app.domain.WorkoutCalendarCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class WorkoutCalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val days: List<CalendarDayCell> = emptyList(),
    val selectedDate: LocalDate? = null,
    val selectedDaySessions: List<WorkoutSessionEntity> = emptyList(),
)

class WorkoutCalendarViewModel(repository: DiaryRepository) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val month = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow<LocalDate?>(null)

    val uiState: StateFlow<WorkoutCalendarUiState> = combine(
        repository.observe(),
        month,
        selectedDate,
    ) { data, currentMonth, selected ->
        // Reuses DiaryRepository's full (uncapped) completed-session history so the calendar and
        // Diary's day-strip/recent-sessions list stay in sync off one source of truth.
        val sessionsByDate: Map<LocalDate, List<WorkoutSessionEntity>> = data.completedSessions
            .mapNotNull { session -> session.completedAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() to session } }
            .groupBy({ it.first }, { it.second })
        WorkoutCalendarUiState(
            month = currentMonth,
            days = WorkoutCalendarCalculator.grid(currentMonth, sessionsByDate.keys),
            selectedDate = selected,
            selectedDaySessions = selected?.let { sessionsByDate[it] } ?: emptyList(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutCalendarUiState())

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
    }

    fun selectDay(date: LocalDate) {
        selectedDate.value = date
    }

    fun dismissDay() {
        selectedDate.value = null
    }

    class Factory(private val repository: DiaryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = WorkoutCalendarViewModel(repository) as T
    }
}
