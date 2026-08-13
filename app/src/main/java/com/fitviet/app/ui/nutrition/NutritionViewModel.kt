package com.fitviet.app.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.MealEntity
import com.fitviet.app.data.local.seed.SeedData
import com.fitviet.app.data.repository.NutritionRepository
import com.fitviet.app.domain.NutritionTotals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NutritionUiState(
    val meals: List<MealEntity> = emptyList(),
    val totals: NutritionTotals = NutritionTotals(),
)

/** Redesign Gate 5b-ii — [com.fitviet.app.data.local.entity.MealPlanMealEntity]-derived state
 * (`hasActivePlan`, `plannedMeals`) removed: the whole `PlannedMealsSection` it fed was a Gate 5a
 * continuity carry-over, explicitly documented there as retired once the Kế hoạch tuần tab
 * (`NutritionLibraryScreen`/`NutritionLibraryViewModel`) shipped its own `nutrition/plan` entry
 * point — which it now has. `mealPlanRepository`/`recipeRepository` dropped from the constructor
 * since nothing else here used them. */
class NutritionViewModel(private val repository: NutritionRepository) : ViewModel() {
    private val presetIndex = MutableStateFlow(0)

    val uiState: StateFlow<NutritionUiState> = repository.observe()
        .map { data -> NutritionUiState(meals = data.meals, totals = data.totals) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionUiState())

    fun addNextPreset() {
        val index = presetIndex.value
        viewModelScope.launch {
            repository.addMeal(SeedData.mealPresets[index % SeedData.mealPresets.size])
        }
        presetIndex.value = index + 1
    }

    fun removeMeal(meal: MealEntity) {
        viewModelScope.launch { repository.removeMeal(meal) }
    }

    class Factory(private val repository: NutritionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = NutritionViewModel(repository) as T
    }
}
