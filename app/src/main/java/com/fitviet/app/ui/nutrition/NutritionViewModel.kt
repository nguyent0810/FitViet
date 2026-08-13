package com.fitviet.app.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.MealEntity
import com.fitviet.app.data.local.entity.MealPlanMealEntity
import com.fitviet.app.data.local.seed.SeedData
import com.fitviet.app.data.repository.NutritionRepository
import com.fitviet.app.data.repository.MealPlanRepository
import com.fitviet.app.data.repository.RecipeRepository
import com.fitviet.app.data.repository.dayTicker
import com.fitviet.app.domain.MealPlanTodayResolver
import com.fitviet.app.domain.NutritionTotals
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PlannedMealStatus { EATEN, NEXT, UPCOMING }

/** One planned meal row on Nutrition Home — deliberately built from BOTH
 * [com.fitviet.app.data.local.entity.MealPlanMealEntity] (planned) and [MealEntity] (consumed):
 * a planned meal existing is never treated as evidence it was eaten (see the "Hit & Run" plan's
 * meal-plan judgment calls) — [PlannedMealStatus.EATEN] only ever comes from a real logged
 * [MealEntity] row matching the same slot. */
data class PlannedMealUiItem(
    val slot: String,
    val recipeName: String,
    val kcal: Int,
    val status: PlannedMealStatus,
)

data class NutritionUiState(
    val meals: List<MealEntity> = emptyList(),
    val totals: NutritionTotals = NutritionTotals(),
    val hasActivePlan: Boolean = false,
    val plannedMeals: List<PlannedMealUiItem> = emptyList(),
)

class NutritionViewModel(
    private val repository: NutritionRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val recipeRepository: RecipeRepository,
) : ViewModel() {
    private val presetIndex = MutableStateFlow(0)

    // hasActivePlan is tracked independently of plannedMealsFlow below — an active plan whose
    // today just happens to have zero meals scheduled (or no matching day) is still an active
    // plan; conflating the two previously hid "Xem thực đơn tuần" and showed the create-plan CTA
    // for a plan that genuinely exists, just not for today specifically.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val hasActivePlanFlow: Flow<Boolean> = mealPlanRepository.observeActivePlan().map { it != null }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val plannedMealsFlow: Flow<List<Pair<String, MealPlanMealEntity>>> =
        dayTicker(ZoneId.systemDefault()).flatMapLatest {
            mealPlanRepository.observeActivePlan().flatMapLatest { plan ->
                if (plan == null) {
                    flowOf(emptyList())
                } else {
                    mealPlanRepository.observeDaysForPlan(plan.id).flatMapLatest { days ->
                        val today = days.firstOrNull { it.dayOfWeek == MealPlanTodayResolver.todayDayOfWeek() }
                        if (today == null) {
                            flowOf(emptyList())
                        } else {
                            mealPlanRepository.observeMealsForDay(today.id).map { meals ->
                                meals.sortedBy { it.orderIndex }.map { meal ->
                                    val recipeName = recipeRepository.getRecipe(meal.recipeId)?.nameVi ?: ""
                                    recipeName to meal
                                }
                            }
                        }
                    }
                }
            }
        }

    val uiState: StateFlow<NutritionUiState> = combine(
        repository.observe(),
        plannedMealsFlow,
        hasActivePlanFlow,
    ) { data, planned, hasActivePlan ->
        val eatenSlots = data.meals.map { it.slot }.toSet()
        var nextAssigned = false
        val plannedItems = planned.map { (recipeName, meal) ->
            val status = when {
                meal.slot in eatenSlots -> PlannedMealStatus.EATEN
                !nextAssigned -> PlannedMealStatus.NEXT.also { nextAssigned = true }
                else -> PlannedMealStatus.UPCOMING
            }
            PlannedMealUiItem(slot = meal.slot, recipeName = recipeName, kcal = meal.kcal, status = status)
        }
        NutritionUiState(
            meals = data.meals,
            totals = data.totals,
            hasActivePlan = hasActivePlan,
            plannedMeals = plannedItems,
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

    class Factory(
        private val repository: NutritionRepository,
        private val mealPlanRepository: MealPlanRepository,
        private val recipeRepository: RecipeRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NutritionViewModel(repository, mealPlanRepository, recipeRepository) as T
    }
}
