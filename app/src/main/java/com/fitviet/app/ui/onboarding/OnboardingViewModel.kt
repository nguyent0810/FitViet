package com.fitviet.app.ui.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds in-memory onboarding selections shared across the goal/level (1a) and split (2a) screens.
 * Persistence to Room lands in Gate 2 — for now this state is lost if the process dies.
 */
data class OnboardingUiState(
    // Defaults mirror the prototype: goal/level/split each start on their first option pre-selected.
    val selectedGoal: Int = 0,
    val selectedLevel: Int = 0,
    val selectedSplit: Int = 0,
)

class OnboardingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun selectGoal(index: Int) {
        _uiState.update { it.copy(selectedGoal = index) }
    }

    fun selectLevel(index: Int) {
        _uiState.update { it.copy(selectedLevel = index) }
    }

    fun selectSplit(index: Int) {
        _uiState.update { it.copy(selectedSplit = index) }
    }
}
