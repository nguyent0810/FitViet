package com.fitviet.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.repository.ExerciseRepository
import com.fitviet.app.data.repository.ProgramRepository
import com.fitviet.app.data.repository.WorkoutRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WorkoutPreviewUiState(
    val isLoading: Boolean = true,
    val dayTitleVi: String = "",
    val items: List<ProgramDayWorkoutItem> = emptyList(),
)

/** Backs the "day exercise list" preview screen (Gate 24) shown after tapping today's row on the
 * Weekly Schedule screen, before the user commits to starting the live logging session. */
class WorkoutPreviewViewModel(
    private val programId: Long,
    private val programRepository: ProgramRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val databaseReady: Deferred<Unit>,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutPreviewUiState())
    val uiState: StateFlow<WorkoutPreviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            databaseReady.await()
            val resolved = ProgramDayWorkoutPlanner.resolveToday(programId, programRepository, exerciseRepository, workoutRepository)
            _uiState.value = WorkoutPreviewUiState(
                isLoading = false,
                dayTitleVi = resolved?.titleVi.orEmpty(),
                items = resolved?.items.orEmpty(),
            )
        }
    }

    class Factory(
        private val programId: Long,
        private val programRepository: ProgramRepository,
        private val exerciseRepository: ExerciseRepository,
        private val workoutRepository: WorkoutRepository,
        private val databaseReady: Deferred<Unit>,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WorkoutPreviewViewModel(programId, programRepository, exerciseRepository, workoutRepository, databaseReady) as T
    }
}
