package com.fitviet.app.ui.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.data.repository.ProgramRepository
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeeklyScheduleUiState(
    val program: ProgramEntity? = null,
    val isLoading: Boolean = true,
    val selectedDay: DayOfWeek = LocalDate.now().dayOfWeek,
)

class WeeklyScheduleViewModel(
    private val programId: Long,
    private val repository: ProgramRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WeeklyScheduleUiState())
    val uiState: StateFlow<WeeklyScheduleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val program = repository.getById(programId)
            _uiState.update { it.copy(program = program, isLoading = false) }
        }
    }

    fun selectDay(dayOfWeek: DayOfWeek) {
        _uiState.update { it.copy(selectedDay = dayOfWeek) }
    }

    class Factory(private val programId: Long, private val repository: ProgramRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WeeklyScheduleViewModel(programId, repository) as T
    }
}
