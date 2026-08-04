package com.fitviet.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.repository.OnboardingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class OnboardingUiState(
    // Defaults mirror the prototype: goal/level/split each start on their first option pre-selected.
    val selectedGoal: Int = 0,
    val selectedLevel: Int = 0,
    val selectedSplit: Int = 0,
)

class OnboardingViewModel(private val repository: OnboardingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // Guards every Room write below so concurrent selection taps can't land out of order
    // and silently revert a newer choice — see Gate 2 codex review.
    private val writeMutex = Mutex()

    init {
        viewModelScope.launch {
            writeMutex.withLock {
                val saved = repository.getSelections()
                _uiState.update {
                    it.copy(
                        selectedGoal = saved.selectedGoal,
                        selectedLevel = saved.selectedLevel,
                        selectedSplit = saved.selectedSplit,
                    )
                }
            }
        }
    }

    fun selectGoal(index: Int) = updateSelection { it.copy(selectedGoal = index) }

    fun selectLevel(index: Int) = updateSelection { it.copy(selectedLevel = index) }

    fun selectSplit(index: Int) = updateSelection { it.copy(selectedSplit = index) }

    /**
     * Marks onboarding done — call and await from the UI before navigating past 2a, so the write
     * can't be cancelled by the graph-scoped ViewModel being cleared right after navigation.
     */
    suspend fun completeOnboarding() {
        writeMutex.withLock {
            val state = _uiState.value
            repository.completeOnboarding(state.selectedGoal, state.selectedLevel, state.selectedSplit)
        }
    }

    private fun updateSelection(transform: (OnboardingUiState) -> OnboardingUiState) {
        _uiState.update(transform)
        viewModelScope.launch {
            writeMutex.withLock {
                // Re-read the freshest state at write time, not what was current when this
                // coroutine was launched — keeps out-of-order writes from reverting a later tap.
                val state = _uiState.value
                repository.saveSelections(state.selectedGoal, state.selectedLevel, state.selectedSplit)
            }
        }
    }

    class Factory(private val repository: OnboardingRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OnboardingViewModel(repository) as T
    }
}
