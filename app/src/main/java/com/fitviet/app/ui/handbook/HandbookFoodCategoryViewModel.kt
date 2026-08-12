package com.fitviet.app.ui.handbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.FoodEntity
import com.fitviet.app.data.repository.HandbookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HandbookFoodCategoryUiState(val category: String, val foods: List<FoodEntity> = emptyList())

/** Gate E7 — one ingredient category's foods, reached by tapping its card on the Handbook's Foods
 * tab. Reuses [HandbookRepository.observe] (same source [HandbookViewModel] reads), just filtered
 * to this one category — no new DAO query needed, mirrors [HandbookMuscleGroupViewModel]'s shape. */
class HandbookFoodCategoryViewModel(category: String, repository: HandbookRepository) : ViewModel() {
    val uiState: StateFlow<HandbookFoodCategoryUiState> = repository.observe()
        .map { data ->
            HandbookFoodCategoryUiState(
                category = category,
                foods = data.foods.filter { it.category == category },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HandbookFoodCategoryUiState(category = category))

    class Factory(private val category: String, private val repository: HandbookRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HandbookFoodCategoryViewModel(category, repository) as T
    }
}
