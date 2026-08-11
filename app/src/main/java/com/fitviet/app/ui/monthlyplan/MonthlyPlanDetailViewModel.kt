package com.fitviet.app.ui.monthlyplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.repository.MonthlyPlanRepository
import com.fitviet.app.data.repository.RegenerateResult
import com.fitviet.app.domain.PlanPhase
import com.fitviet.app.domain.SplitTemplate
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MonthlyPlanDayUi(
    val dayId: Long,
    val date: LocalDate,
    val dayOfWeek: DayOfWeek,
    val sessionType: String?,
    val isRestDay: Boolean,
    val isLocked: Boolean,
)

data class MonthlyPlanWeekUi(val weekId: Long, val weekIndex: Int, val phase: PlanPhase, val days: List<MonthlyPlanDayUi>)

data class MonthlyPlanDetailUiState(
    val isLoading: Boolean = true,
    /** Null once loaded iff there's no active plan — the screen shouldn't normally be reachable in
     * that state (its only entry point is the Training-card link, which only exists when a plan is
     * active), but this is handled rather than assumed impossible. */
    val planId: Long? = null,
    val splitTemplate: SplitTemplate? = null,
    val weeks: List<MonthlyPlanWeekUi> = emptyList(),
    val isRegeneratingMonth: Boolean = false,
    val regeneratingWeekIds: Set<Long> = emptySet(),
    /** Set right after a regenerate action returns [RegenerateResult.Locked] (whole target was
     * locked, nothing changed) — a transient one-shot signal the screen clears once shown, not
     * part of the plan's own persisted state. */
    val lockedActionMessage: Boolean = false,
)

/** "Hit & Run" (Gate 63+) Regenerate UI — the "simple day list" the plan's own scope note calls
 * for (not a full week-tabbed calendar). Always shows the currently *active* plan; there's only
 * ever one, so this takes no nav arg. */
class MonthlyPlanDetailViewModel(private val repository: MonthlyPlanRepository) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val loadedState: StateFlow<MonthlyPlanDetailUiState> = repository.observeActivePlanId()
        .flatMapLatest { planId ->
            if (planId == null) {
                flowOf(MonthlyPlanDetailUiState(isLoading = false, planId = null))
            } else {
                combine(
                    repository.observePlan(planId),
                    repository.observeWeeksForPlan(planId),
                    repository.observeDaysForPlan(planId),
                    repository.observeLockedDayIds(planId),
                ) { plan, weeks, days, lockedDayIds ->
                    val daysByWeekId = days.groupBy { it.monthlyPlanWeekId }
                    MonthlyPlanDetailUiState(
                        isLoading = false,
                        planId = planId,
                        splitTemplate = plan?.let { SplitTemplate.valueOf(it.splitTemplate) },
                        weeks = weeks.sortedBy { it.weekIndex }.map { week ->
                            MonthlyPlanWeekUi(
                                weekId = week.id,
                                weekIndex = week.weekIndex,
                                phase = PlanPhase.valueOf(week.phase),
                                days = daysByWeekId[week.id].orEmpty().sortedBy { it.effectiveEpochDay }.map { day ->
                                    MonthlyPlanDayUi(
                                        dayId = day.id,
                                        date = LocalDate.ofEpochDay(day.effectiveEpochDay),
                                        dayOfWeek = DayOfWeek.of(day.dayOfWeek),
                                        sessionType = day.sessionType,
                                        isRestDay = day.isRestDay,
                                        isLocked = day.id in lockedDayIds,
                                    )
                                },
                            )
                        },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlyPlanDetailUiState())

    private val actionState = MutableStateFlow(ActionState())
    private data class ActionState(
        val isRegeneratingMonth: Boolean = false,
        val regeneratingWeekIds: Set<Long> = emptySet(),
        val lockedActionMessage: Boolean = false,
    )

    val uiState: StateFlow<MonthlyPlanDetailUiState> = combine(loadedState, actionState) { loaded, action ->
        loaded.copy(
            isRegeneratingMonth = action.isRegeneratingMonth,
            regeneratingWeekIds = action.regeneratingWeekIds,
            lockedActionMessage = action.lockedActionMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlyPlanDetailUiState())

    fun dismissLockedMessage() {
        actionState.value = actionState.value.copy(lockedActionMessage = false)
    }

    fun regenerateMonth() {
        val planId = uiState.value.planId ?: return
        if (actionState.value.isRegeneratingMonth) return
        actionState.value = actionState.value.copy(isRegeneratingMonth = true)
        viewModelScope.launch {
            val result = repository.regenerateMonth(planId, LocalDate.now())
            actionState.value = actionState.value.copy(
                isRegeneratingMonth = false,
                lockedActionMessage = result == RegenerateResult.Locked,
            )
        }
    }

    fun regenerateWeek(weekId: Long) {
        if (weekId in actionState.value.regeneratingWeekIds) return
        actionState.value = actionState.value.copy(regeneratingWeekIds = actionState.value.regeneratingWeekIds + weekId)
        viewModelScope.launch {
            val result = repository.regenerateWeek(weekId, LocalDate.now())
            actionState.value = actionState.value.copy(
                regeneratingWeekIds = actionState.value.regeneratingWeekIds - weekId,
                lockedActionMessage = result == RegenerateResult.Locked,
            )
        }
    }

    class Factory(private val repository: MonthlyPlanRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MonthlyPlanDetailViewModel(repository) as T
    }
}
