package com.fitviet.app.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.MealEntity
import com.fitviet.app.data.repository.NutritionRepository
import com.fitviet.app.domain.NutritionCalculator
import com.fitviet.app.domain.NutritionStats
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Locale-neutral key (not display text) so the stored row renders correctly under either
// app language — NutritionScreen maps this to a localized string resource.
const val ADDED_MEAL_SLOT_KEY = "extra"

data class NutritionUiState(
    val stats: NutritionStats = NutritionCalculator.compute(emptyList()),
    val meals: List<MealEntity> = emptyList(),
    val isFoodPickerOpen: Boolean = false,
)

class NutritionViewModel(private val repository: NutritionRepository) : ViewModel() {
    private val isFoodPickerOpen = MutableStateFlow(false)

    val uiState: StateFlow<NutritionUiState> = combine(
        repository.observeToday(),
        isFoodPickerOpen,
    ) { data, pickerOpen ->
        NutritionUiState(stats = data.stats, meals = data.meals, isFoodPickerOpen = pickerOpen)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionUiState())

    fun openFoodPicker() {
        isFoodPickerOpen.value = true
    }

    fun closeFoodPicker() {
        isFoodPickerOpen.value = false
    }

    fun addFood(preset: FoodPreset) {
        viewModelScope.launch {
            repository.addMeal(
                MealEntity(
                    epochDay = LocalDate.now().toEpochDay(),
                    slot = ADDED_MEAL_SLOT_KEY,
                    nameVi = preset.nameVi,
                    kcal = preset.kcal,
                    proteinG = preset.proteinG,
                    carbG = preset.carbG,
                    fatG = preset.fatG,
                ),
            )
        }
        closeFoodPicker()
    }

    fun removeMeal(meal: MealEntity) {
        viewModelScope.launch { repository.removeMeal(meal) }
    }

    class Factory(private val repository: NutritionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = NutritionViewModel(repository) as T
    }
}
