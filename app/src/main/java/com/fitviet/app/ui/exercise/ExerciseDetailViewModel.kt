package com.fitviet.app.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseDetailUiState(
    val exercise: ExerciseEntity? = null,
    val isLoading: Boolean = true,
    /**
     * "Thêm vào buổi tập" toggle from the prototype. There's no custom session-builder feature
     * yet (the workout flow uses a fixed demo plan — see PROGRESS.md), so this is a local-only
     * visual toggle matching the design, not wired to anything real yet.
     */
    val isAdded: Boolean = false,
)

class ExerciseDetailViewModel(
    private val exerciseId: Long,
    private val repository: ExerciseRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val exercise = repository.getById(exerciseId)
            _uiState.update { it.copy(exercise = exercise, isLoading = false) }
        }
    }

    fun toggleAdded() {
        _uiState.update { it.copy(isAdded = !it.isAdded) }
    }

    class Factory(private val exerciseId: Long, private val repository: ExerciseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ExerciseDetailViewModel(exerciseId, repository) as T
    }
}
