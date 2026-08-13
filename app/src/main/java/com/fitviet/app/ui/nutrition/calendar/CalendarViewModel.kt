package com.fitviet.app.ui.nutrition.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.repository.MealPlanRepository
import com.fitviet.app.data.repository.dayTicker
import com.fitviet.app.domain.MealPlanTodayResolver
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CalendarDayUiItem(
    val dayId: Long,
    val dayOfWeek: Int,
    val totalKcal: Int,
    val isToday: Boolean,
)

data class CalendarUiState(
    val hasActivePlan: Boolean = false,
    val days: List<CalendarDayUiItem> = emptyList(),
    val weekAverageKcal: Int = 0,
)

/**
 * Gate C9 — the plan's week-at-a-glance calendar: a compact per-day kcal overview + week average,
 * "today" highlighted via [MealPlanTodayResolver] (the resolver itself landed in an earlier pass
 * of this gate). This is now [MealPlanTodayResolver]'s only consumer as of Gate 5b-ii —
 * `NutritionViewModel` no longer resolves "today" at all after its own planned-meals section was
 * retired. Reads
 * [com.fitviet.app.data.local.entity.MealPlanDayEntity.totalKcalTarget] directly rather than
 * resolving every meal (unlike Gate C7's [com.fitviet.app.ui.nutrition.plan.PlanViewModel]) — this
 * screen only ever needs each day's kcal total, never per-meal detail.
 */
class CalendarViewModel(private val mealPlanRepository: MealPlanRepository) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CalendarUiState> = mealPlanRepository.observeActivePlan().flatMapLatest { plan ->
        if (plan == null) {
            flowOf(CalendarUiState())
        } else {
            // Re-derives "today" on every midnight tick, not just on a database emission — a
            // calendar left open overnight must not keep yesterday's day highlighted. (Not
            // NutritionRepository's own dayTicker use — that one's for its own meal-log day
            // boundary, a different concern this screen doesn't share.)
            combine(
                mealPlanRepository.observeDaysForPlan(plan.id),
                dayTicker(ZoneId.systemDefault()),
            ) { days, _ ->
                val todayDayOfWeek = MealPlanTodayResolver.todayDayOfWeek()
                val items = days.sortedBy { it.dayOfWeek }.map { day ->
                    CalendarDayUiItem(
                        dayId = day.id,
                        dayOfWeek = day.dayOfWeek,
                        totalKcal = day.totalKcalTarget,
                        isToday = day.dayOfWeek == todayDayOfWeek,
                    )
                }
                CalendarUiState(
                    hasActivePlan = true,
                    days = items,
                    weekAverageKcal = if (items.isEmpty()) 0 else items.sumOf { it.totalKcal } / items.size,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    class Factory(private val mealPlanRepository: MealPlanRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CalendarViewModel(mealPlanRepository) as T
    }
}
