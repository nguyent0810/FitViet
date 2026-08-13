package com.fitviet.app.ui.nutrition.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.MealPlanDayEntity
import com.fitviet.app.data.local.entity.MealPlanMealEntity
import com.fitviet.app.data.repository.MealPlanOperationResult
import com.fitviet.app.data.repository.MealPlanRepository
import com.fitviet.app.data.repository.RecipeRepository
import kotlinx.coroutines.CancellationException
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

data class PlanMealUiItem(
    val mealId: Long,
    val slot: String,
    val recipeName: String,
    val kcal: Int,
    val proteinG: Int,
    val carbG: Int,
    val fatG: Int,
)

data class PlanDayUiItem(
    val dayId: Long,
    val dayOfWeek: Int,
    val meals: List<PlanMealUiItem>,
    val totalKcal: Int,
    val totalProteinG: Int,
    val totalCarbG: Int,
    val totalFatG: Int,
)

data class SwapAlternativeUiItem(val recipeId: Long, val variantCode: String, val recipeName: String, val kcal: Int)

/** [mealId] non-null means the sheet is showing. [alternatives] is fetched ONCE per opening (via
 * [com.fitviet.app.domain.MealPlanGenerator.alternativesFor], a pure deterministic function — a
 * second fetch for the same meal would return the identical list), so "Gợi ý khác ↻" cycles
 * [currentIndex] through the already-fetched pool rather than re-fetching. [openToken] identifies
 * a single *opening* of the sheet, NOT just which meal — [mealId] alone can't distinguish
 * "reopened the same meal" from "the previous opening's async work is still in flight", so every
 * async completion (fetch, apply) must compare against [openToken], not [mealId]. */
data class SwapUiState(
    val mealId: Long? = null,
    val openToken: Long = 0L,
    val originalKcal: Int = 0,
    val alternatives: List<SwapAlternativeUiItem> = emptyList(),
    val currentIndex: Int = 0,
    /** `true` from the moment the sheet opens until the fetch resolves (successfully or not) —
     * [SwapSheet] uses this to distinguish "still loading" from "genuinely no suggestions" (both
     * start as an empty [alternatives] list). */
    val isLoadingAlternatives: Boolean = false,
    val isApplying: Boolean = false,
) {
    val current: SwapAlternativeUiItem? get() = alternatives.getOrNull(currentIndex)
}

data class PlanUiState(
    val hasActivePlan: Boolean = false,
    val activePlanId: Long? = null,
    val days: List<PlanDayUiItem> = emptyList(),
    val selectedDayIndex: Int = 0,
    val isRegenerating: Boolean = false,
    val swap: SwapUiState = SwapUiState(),
)

private data class PlanSnapshot(val planId: Long?, val days: List<PlanDayUiItem>?)

/**
 * Gate C7 — the "generated plan" screen: T2–CN day tabs over the active [com.fitviet.app.data.local.entity.UserMealPlanEntity]'s
 * 7 [MealPlanDayEntity] rows, plus day/week regenerate. Reachable two ways: right after a
 * successful generate (Gates C5/C6, which navigate here directly), or later from Nutrition Home's
 * "Xem thực đơn tuần" link with no plan freshly generated — so [hasActivePlan] must be derived
 * from a real repository read, not assumed `true` just because this screen was reached.
 */
class PlanViewModel(
    private val mealPlanRepository: MealPlanRepository,
    private val recipeRepository: RecipeRepository,
) : ViewModel() {
    private val selectedDayIndex = MutableStateFlow(0)
    private val isRegenerating = MutableStateFlow(false)
    private val swapState = MutableStateFlow(SwapUiState())
    private var nextSwapOpenToken = 0L

    @OptIn(ExperimentalCoroutinesApi::class)
    private val planSnapshotFlow: Flow<PlanSnapshot> = mealPlanRepository.observeActivePlan().flatMapLatest { plan ->
        if (plan == null) {
            flowOf(PlanSnapshot(planId = null, days = null))
        } else {
            mealPlanRepository.observeDaysForPlan(plan.id).flatMapLatest { days ->
                val sortedDays = days.sortedBy { it.dayOfWeek }
                if (sortedDays.isEmpty()) {
                    flowOf(PlanSnapshot(planId = plan.id, days = emptyList()))
                } else {
                    combine(
                        sortedDays.map { day -> mealPlanRepository.observeMealsForDay(day.id).map { meals -> day to meals } },
                    ) { pairs -> PlanSnapshot(planId = plan.id, days = pairs.map { (day, meals) -> buildDayItem(day, meals) }) }
                }
            }
        }
    }

    val uiState: StateFlow<PlanUiState> = combine(
        planSnapshotFlow,
        selectedDayIndex,
        isRegenerating,
        swapState,
    ) { snapshot, selectedIndex, regenerating, swap ->
        val days = snapshot.days.orEmpty()
        PlanUiState(
            hasActivePlan = snapshot.planId != null,
            activePlanId = snapshot.planId,
            days = days,
            selectedDayIndex = if (days.isEmpty()) 0 else selectedIndex.coerceIn(0, days.size - 1),
            isRegenerating = regenerating,
            swap = swap,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    fun selectDay(index: Int) {
        selectedDayIndex.value = index
    }

    /** Re-entrancy-guarded on its own [isRegenerating] flag only — same care as `NutritionLibraryViewModel.useTemplate`. */
    suspend fun regenerateDay(dayId: Long): Boolean = runRegenerate { mealPlanRepository.regenerateDay(dayId) }

    suspend fun regenerateWeek(planId: Long): Boolean = runRegenerate { mealPlanRepository.regenerateWeek(planId) }

    private suspend fun runRegenerate(action: suspend () -> MealPlanOperationResult): Boolean {
        if (isRegenerating.value) return false
        isRegenerating.value = true
        return try {
            action() is MealPlanOperationResult.Success
        } finally {
            isRegenerating.value = false
        }
    }

    /** Fetches once per opening — see [SwapUiState]'s doc for why a repeat fetch would be
     * pointless, and why [openToken] rather than [mealId] guards the async completion (reopening
     * the SAME meal while a prior opening's fetch is still in flight must not let the stale
     * result land in the new opening). */
    fun openSwapSheet(mealId: Long, originalKcal: Int) {
        val token = ++nextSwapOpenToken
        swapState.value = SwapUiState(mealId = mealId, openToken = token, originalKcal = originalKcal, isLoadingAlternatives = true)
        viewModelScope.launch {
            try {
                val drafts = mealPlanRepository.swapAlternatives(mealId)
                val items = drafts.map { draft ->
                    SwapAlternativeUiItem(
                        recipeId = draft.recipeId,
                        variantCode = draft.variantCode,
                        recipeName = recipeRepository.getRecipe(draft.recipeId)?.nameVi.orEmpty(),
                        kcal = draft.totals.kcal,
                    )
                }
                if (swapState.value.openToken == token) {
                    swapState.value = swapState.value.copy(alternatives = items)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Leave `alternatives` empty — once `isLoadingAlternatives` clears below, the
                // sheet reads this the same as "no suitable suggestions" rather than crashing the
                // app on an unhandled exception from this fire-and-forget viewModelScope.launch.
            } finally {
                if (swapState.value.openToken == token) {
                    swapState.value = swapState.value.copy(isLoadingAlternatives = false)
                }
            }
        }
    }

    fun rerollSwap() {
        val alternatives = swapState.value.alternatives
        if (alternatives.isEmpty()) return
        swapState.value = swapState.value.copy(currentIndex = (swapState.value.currentIndex + 1) % alternatives.size)
    }

    fun dismissSwapSheet() {
        swapState.value = SwapUiState()
    }

    /** Re-entrancy-guarded on [SwapUiState.isApplying] only. Dismisses the sheet on success;
     * leaves it open (so the user can retry or pick a different alternative) on failure. Both the
     * exception path and a dismiss/reopen race (while this suspend call is in flight) are
     * guarded: the `finally` block only ever touches [swapState] if it's still showing the SAME
     * [SwapUiState.openToken] this call started with. [openToken], not [SwapUiState.mealId], is
     * what's compared — reopening the SAME meal while a prior apply for it is still in flight
     * gets a fresh token, so the stale completion correctly no-ops instead of clobbering the new
     * opening (a [SwapUiState.mealId]-only guard would miss exactly this case). */
    suspend fun applyCurrentSwap(): Boolean {
        val state = swapState.value
        val mealId = state.mealId ?: return false
        val token = state.openToken
        val alternative = state.current ?: return false
        if (state.isApplying) return false
        swapState.value = state.copy(isApplying = true)
        var succeeded = false
        try {
            val result = mealPlanRepository.applySwap(mealId, alternative.recipeId, alternative.variantCode)
            succeeded = result is MealPlanOperationResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Caller (PlanScreen) awaits this from its own fire-and-forget `coroutineScope.launch`
            // — an uncaught exception there would crash the app the same way an uncaught one in
            // openSwapSheet's viewModelScope.launch would. Reporting failure via the return value
            // (sheet stays open, same as an explicit MealPlanOperationResult failure) is
            // preferable to a crash.
        } finally {
            if (swapState.value.openToken == token) {
                swapState.value = if (succeeded) SwapUiState() else swapState.value.copy(isApplying = false)
            }
        }
        return succeeded
    }

    private suspend fun buildDayItem(day: MealPlanDayEntity, meals: List<MealPlanMealEntity>): PlanDayUiItem {
        val items = meals.sortedBy { it.orderIndex }.map { meal ->
            PlanMealUiItem(
                mealId = meal.id,
                slot = meal.slot,
                recipeName = recipeRepository.getRecipe(meal.recipeId)?.nameVi.orEmpty(),
                kcal = meal.kcal,
                proteinG = meal.proteinG,
                carbG = meal.carbG,
                fatG = meal.fatG,
            )
        }
        return PlanDayUiItem(
            dayId = day.id,
            dayOfWeek = day.dayOfWeek,
            meals = items,
            totalKcal = meals.sumOf { it.kcal },
            totalProteinG = meals.sumOf { it.proteinG },
            totalCarbG = meals.sumOf { it.carbG },
            totalFatG = meals.sumOf { it.fatG },
        )
    }

    class Factory(
        private val mealPlanRepository: MealPlanRepository,
        private val recipeRepository: RecipeRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PlanViewModel(mealPlanRepository, recipeRepository) as T
    }
}
