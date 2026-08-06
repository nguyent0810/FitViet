package com.fitviet.app.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.repository.ExerciseRepository
import com.fitviet.app.data.repository.WorkoutRepository
import com.fitviet.app.domain.ExerciseHistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class ExerciseDetailUiState(
    val exercise: ExerciseEntity? = null,
    val isLoading: Boolean = true,
    /**
     * "Thêm vào buổi tập" toggle from the prototype. There's no custom session-builder feature
     * yet (the workout flow uses a fixed demo plan — see PROGRESS.md), so this is a local-only
     * visual toggle matching the design, not wired to anything real yet.
     */
    val isAdded: Boolean = false,
    /** Feature #10 (Gate 46) — backs the "Tiến bộ" tab, newest first. */
    val history: List<ExerciseHistoryEntry> = emptyList(),
)

class ExerciseDetailViewModel(
    exerciseId: Long,
    repository: ExerciseRepository,
    workoutRepository: WorkoutRepository,
) : ViewModel() {
    private val isAdded = MutableStateFlow(false)

    // The exercise fetch is a genuine one-shot (it never changes), wrapped in `flow {}` rather
    // than a separate `viewModelScope.launch` so it composes into the same combine/stateIn shape
    // every other ViewModel in this codebase uses — `combine` tolerates this source completing
    // after its single emission, it just keeps using that last value for later combinations.
    val uiState: StateFlow<ExerciseDetailUiState> = combine(
        flow { emit(repository.getById(exerciseId)) },
        workoutRepository.observeHistoryForExercise(exerciseId),
        isAdded,
    ) { exercise, history, added ->
        ExerciseDetailUiState(exercise = exercise, isLoading = false, isAdded = added, history = history)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseDetailUiState())

    fun toggleAdded() {
        isAdded.value = !isAdded.value
    }

    class Factory(
        private val exerciseId: Long,
        private val repository: ExerciseRepository,
        private val workoutRepository: WorkoutRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ExerciseDetailViewModel(exerciseId, repository, workoutRepository) as T
    }
}
