package com.fitviet.app.ui.handbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.entity.FoodEntity
import com.fitviet.app.data.repository.HandbookRepository
import com.fitviet.app.domain.ExerciseDifficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class HandbookTab { EXERCISES, FOODS }

data class HandbookUiState(
    val selectedTab: HandbookTab = HandbookTab.EXERCISES,
    /** Ordered Beginner -> Advanced; a level with zero matching exercises is omitted rather than
     * shown as an empty section. */
    val exercisesByLevel: List<Pair<ExerciseDifficulty, List<ExerciseEntity>>> = emptyList(),
    val foodsByCategory: List<Pair<String, List<FoodEntity>>> = emptyList(),
)

class HandbookViewModel(private val repository: HandbookRepository) : ViewModel() {
    private val selectedTab = MutableStateFlow(HandbookTab.EXERCISES)

    val uiState: StateFlow<HandbookUiState> = combine(repository.observe(), selectedTab) { data, tab ->
        HandbookUiState(
            selectedTab = tab,
            exercisesByLevel = ExerciseDifficulty.entries
                .map { level -> level to data.exercises.filter { it.difficultyCode == level.name } }
                .filter { (_, exercises) -> exercises.isNotEmpty() },
            foodsByCategory = data.foods.groupBy { it.category }.toList(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HandbookUiState())

    fun selectTab(tab: HandbookTab) {
        selectedTab.value = tab
    }

    class Factory(private val repository: HandbookRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HandbookViewModel(repository) as T
    }
}
