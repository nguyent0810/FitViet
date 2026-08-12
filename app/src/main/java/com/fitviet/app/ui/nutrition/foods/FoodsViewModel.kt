package com.fitviet.app.ui.nutrition.foods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.FoodEntity
import com.fitviet.app.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class FoodsUiState(
    val categories: List<String> = emptyList(),
    /** Null = "Tất cả" (all categories). */
    val selectedCategory: String? = null,
    val foods: List<FoodEntity> = emptyList(),
)

/** Off-filter groups DIM rather than hide (per the design brief) — [selectedCategory] never
 * actually excludes anything server/repository-side; [recipeRepository.observeFoods] is always
 * called with `null` here, and the screen itself decides how to render a non-matching row. */
class FoodsViewModel(private val recipeRepository: RecipeRepository) : ViewModel() {
    private val selectedCategory = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FoodsUiState> = combine(recipeRepository.observeFoods(null), selectedCategory) { foods, category ->
        FoodsUiState(
            categories = foods.map { it.category }.distinct().sorted(),
            selectedCategory = category,
            foods = foods,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FoodsUiState())

    fun selectCategory(category: String?) {
        selectedCategory.value = category
    }

    class Factory(private val recipeRepository: RecipeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = FoodsViewModel(recipeRepository) as T
    }
}
