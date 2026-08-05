package com.fitviet.app.ui.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.data.repository.ProgramRepository
import com.fitviet.app.domain.ProgramScheduleDay
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeeklyScheduleUiState(
    val program: ProgramEntity? = null,
    val schedule: List<ProgramScheduleDay> = emptyList(),
    val isActiveProgram: Boolean = false,
    val isLoading: Boolean = true,
    val selectedDay: DayOfWeek = LocalDate.now().dayOfWeek,
)

class WeeklyScheduleViewModel(
    private val programId: Long,
    private val repository: ProgramRepository,
) : ViewModel() {
    private val program = MutableStateFlow<ProgramEntity?>(null)
    private val isLoading = MutableStateFlow(true)
    private val selectedDay = MutableStateFlow(LocalDate.now().dayOfWeek)

    val uiState: StateFlow<WeeklyScheduleUiState> = combine(
        program,
        repository.observeSchedule(programId),
        repository.observeActiveProgramId(),
        selectedDay,
        isLoading,
    ) { currentProgram, schedule, activeProgramId, selected, loading ->
        WeeklyScheduleUiState(
            program = currentProgram,
            schedule = schedule,
            isActiveProgram = activeProgramId == programId,
            isLoading = loading,
            selectedDay = selected,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeklyScheduleUiState())

    init {
        viewModelScope.launch {
            program.value = repository.getById(programId)
            isLoading.value = false
        }
    }

    fun selectDay(dayOfWeek: DayOfWeek) {
        selectedDay.value = dayOfWeek
    }

    fun setAsActiveProgram() = viewModelScope.launch { repository.setActiveProgram(programId) }

    class Factory(private val programId: Long, private val repository: ProgramRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WeeklyScheduleViewModel(programId, repository) as T
    }
}
