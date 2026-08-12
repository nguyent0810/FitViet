package com.fitviet.app.ui.handbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.FoodEntity
import com.fitviet.app.data.repository.HandbookRepository
import com.fitviet.app.domain.MuscleGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class HandbookTab { EXERCISES, FOODS }

data class HandbookUiState(
    val selectedTab: HandbookTab = HandbookTab.EXERCISES,
    /** Gate E5 — ordered by [MuscleGroup]'s own declaration order; a group with zero matching
     * exercises is omitted rather than shown as an empty card (same "omit, don't show empty"
     * precedent the old by-level grouping used). Tapping a card opens
     * [com.fitviet.app.ui.handbook.HandbookMuscleGroupScreen] for that group — the exercise list
     * itself no longer renders inline on this top-level screen. */
    val exercisesByMuscleGroup: List<Pair<MuscleGroup, Int>> = emptyList(),
    /** Gate E5/E7 symmetry — count only, same as [exercisesByMuscleGroup]; the food list itself
     * loads on [HandbookFoodCategoryScreen]'s own drill-down, not here. */
    val foodsByCategory: List<Pair<String, Int>> = emptyList(),
)

/** Gate E7 — display order for the Foods tab's category cards (grouped by kind rather than
 * [FoodEntity.category]'s incidental seed-insertion order). A category not listed here (future
 * seed data) is appended after, alphabetically, so nothing silently disappears. */
private val FOOD_CATEGORY_ORDER = listOf(
    "Thịt", "Cá & hải sản", "Trứng", "Sữa", "Đậu & đạm thực vật",
    "Rau củ", "Trái cây", "Tinh bột", "Hạt & quả hạch", "Dầu & mỡ", "Gia vị",
)

class HandbookViewModel(private val repository: HandbookRepository) : ViewModel() {
    private val selectedTab = MutableStateFlow(HandbookTab.EXERCISES)

    val uiState: StateFlow<HandbookUiState> = combine(repository.observe(), selectedTab) { data, tab ->
        HandbookUiState(
            selectedTab = tab,
            exercisesByMuscleGroup = MuscleGroup.entries
                .map { group -> group to data.exercises.count { it.muscleGroupCode == group.name } }
                .filter { (_, count) -> count > 0 },
            foodsByCategory = data.foods.groupBy { it.category }
                .map { (category, foods) -> category to foods.size }
                .sortedWith(
                    compareBy(
                        { (category, _) -> FOOD_CATEGORY_ORDER.indexOf(category).let { if (it == -1) Int.MAX_VALUE else it } },
                        { (category, _) -> category },
                    ),
                ),
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
