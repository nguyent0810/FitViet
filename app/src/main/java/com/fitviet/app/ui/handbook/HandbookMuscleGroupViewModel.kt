package com.fitviet.app.ui.handbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.repository.HandbookRepository
import com.fitviet.app.domain.MuscleGroup
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HandbookMuscleGroupUiState(val group: MuscleGroup, val exercises: List<ExerciseEntity> = emptyList())

/** Gate E5 — one muscle group's exercises, reached by tapping its card on the Handbook's
 * Exercises tab. Reuses [HandbookRepository.observe] (same source [HandbookViewModel] reads),
 * just filtered to this one group — no new DAO query needed. */
class HandbookMuscleGroupViewModel(group: MuscleGroup, repository: HandbookRepository) : ViewModel() {
    val uiState: StateFlow<HandbookMuscleGroupUiState> = repository.observe()
        .map { data ->
            HandbookMuscleGroupUiState(
                group = group,
                exercises = data.exercises.filter { it.muscleGroupCode == group.name },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HandbookMuscleGroupUiState(group = group))

    class Factory(private val group: MuscleGroup, private val repository: HandbookRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HandbookMuscleGroupViewModel(group, repository) as T
    }
}
