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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NutritionUiState(
    val meals: List<MealEntity> = emptyList(),
    val totals: NutritionTotals = NutritionTotals(),
    /** The preset "+ Thêm món" will add next — cycles through [SeedData.mealPresets] in order. */
    val nextPresetName: String = SeedData.mealPresets.first().nameVi,
)

class NutritionViewModel(private val repository: NutritionRepository) : ViewModel() {
    private val presetIndex = MutableStateFlow(0)

    val uiState: StateFlow<NutritionUiState> = combine(repository.observe(), presetIndex) { data, index ->
        NutritionUiState(
            meals = data.meals,
            totals = data.totals,
            nextPresetName = SeedData.mealPresets[index % SeedData.mealPresets.size].nameVi,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionUiState())

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
