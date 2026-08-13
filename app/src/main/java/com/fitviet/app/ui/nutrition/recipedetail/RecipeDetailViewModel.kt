package com.fitviet.app.ui.nutrition.recipedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.repository.NutritionRepository
import com.fitviet.app.data.repository.RecipeDetail
import com.fitviet.app.data.repository.RecipeRepository
import com.fitviet.app.domain.NutritionGoals
import com.fitviet.app.domain.NutritionTotals
import com.fitviet.app.domain.RecipeNutritionCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecipeDetailUiState(
    val isLoading: Boolean = true,
    val detail: RecipeDetail? = null,
    val servings: Int = 1,
    val selectedVariantCode: String = "STANDARD",
    val totals: NutritionTotals = NutritionTotals(kcal = 0, proteinG = 0, carbG = 0, fatG = 0),
    val kcalRemainingToday: Int = 0,
    val isAdding: Boolean = false,
)

/**
 * Gate C4 — Recipe Detail's servings-stepper/variant-pill recompute is entirely local: [detailFlow]
 * is loaded once (a [RecipeDetail] never changes mid-session), then [servingsFlow]/[variantFlow]
 * drive [RecipeNutritionCalculator] directly with no repository round-trip, same "single source
 * of truth" contract [RecipeDetail]'s own doc comment describes.
 *
 * Gate 5c — [nutritionRepository] added for the "Vừa với mục tiêu hôm nay" (fits your goal today)
 * line's kcal-remaining figure, and for [addToMeal] itself, which logs the CURRENT
 * servings/variant-scaled [totals] — deliberately NOT [RecipeRepository]'s own
 * `RecipeWithNutrition.standardTotals` (the unscaled base numbers `NutritionLibraryViewModel.addToToday`
 * uses), since ignoring this screen's own stepper/pills would silently log the wrong amount.
 * `isFavorite`/`toggleFavorite` are deleted — confirmed write-only (this screen was the only
 * reader anywhere in the app, of its own write, per the Phase 5 plan-check's Judgment Call D).
 * [RecipeRepository.observeFavorites]/`observeIsFavorite`/`toggleFavorite` and the underlying
 * `FavoriteRecipeDao`/`FavoriteRecipeEntity` table are left in place, not deleted — same
 * "compiling but unreached, cheap to resurrect" call as `FoodDao`'s own dead queries from Gate
 * 5b-i, and a schema removal is a materially bigger/riskier change than a UI decision warrants.
 */
class RecipeDetailViewModel(
    private val recipeId: Long,
    private val recipeRepository: RecipeRepository,
    private val nutritionRepository: NutritionRepository,
) : ViewModel() {
    private val detailFlow = MutableStateFlow<RecipeDetail?>(null)
    private val servingsFlow = MutableStateFlow(1)
    private val variantFlow = MutableStateFlow(STANDARD_VARIANT_CODE)
    private val isAdding = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val loaded = recipeRepository.getRecipeDetail(recipeId)
            detailFlow.value = loaded
            // Seeds servings to baseServings, i.e. scale 1.0 = "the whole recipe" — deliberately
            // different from RecipeRepository's own RecipeDetail.standardTotals, which is always
            // computed at servings = 1.0 (one serving). For baseServings > 1, this screen's default
            // add-to-meal would log N× what the library's "+ Thêm" logs for the same recipe. Inert
            // today: every seeded recipe has baseServings = 1.
            servingsFlow.value = loaded?.baseServings?.coerceAtLeast(MIN_SERVINGS) ?: MIN_SERVINGS
        }
    }

    private data class Stage1(
        val detail: RecipeDetail?,
        val servings: Int,
        val variantCode: String,
        val kcalToday: Int,
    )

    private val stage1 = combine(detailFlow, servingsFlow, variantFlow, nutritionRepository.observe()) { detail, servings, variantCode, nutritionData ->
        Stage1(detail, servings, variantCode, nutritionData.totals.kcal)
    }

    val uiState: StateFlow<RecipeDetailUiState> = stage1.combine(isAdding) { stage, adding ->
        RecipeDetailUiState(
            isLoading = stage.detail == null,
            detail = stage.detail,
            servings = stage.servings,
            selectedVariantCode = stage.variantCode,
            totals = stage.detail?.let { recomputeTotals(it, stage.servings.toDouble(), stage.variantCode) }
                ?: NutritionTotals(kcal = 0, proteinG = 0, carbG = 0, fatG = 0),
            kcalRemainingToday = (NutritionGoals.KCAL - stage.kcalToday).coerceAtLeast(0),
            isAdding = adding,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeDetailUiState())

    fun incrementServings() {
        servingsFlow.value = (servingsFlow.value + 1).coerceAtMost(MAX_SERVINGS)
    }

    fun decrementServings() {
        servingsFlow.value = (servingsFlow.value - 1).coerceAtLeast(MIN_SERVINGS)
    }

    fun selectVariant(code: String) {
        variantFlow.value = code
    }

    /** Mock's own literal two CTAs — "Bữa phụ" (log as a snack) and "+ Thêm vào bữa trưa" (log to
     * lunch) — call this with the literal Vietnamese slot string ("Bữa phụ"/"Bữa trưa") each button
     * represents; no time-of-day inference, matching the mock's own fixed two-button design rather
     * than a dynamic slot picker. Re-entrancy guarded the same way every other add/apply action in
     * this phase is, so a fast double-tap can't log the meal twice. */
    suspend fun addToMeal(slot: String): Boolean {
        val detail = uiState.value.detail ?: return false
        if (isAdding.value) return false
        isAdding.value = true
        return try {
            nutritionRepository.addMeal(detail.nameVi, uiState.value.totals, slot)
            true
        } finally {
            isAdding.value = false
        }
    }

    /** Recomputes directly from [RecipeDetail.baseTotals] (the real, undivided ingredient sum) —
     * NOT by scaling the already-rounded [RecipeDetail.standardTotals] back up, which would
     * compound rounding error at higher servings/variant-multiplier combinations (caught in Gate
     * C4 review: `round(round(base / B) × servings × multiplier)` drifts from the true
     * `round(base / B × servings × multiplier)` by several units at e.g. 12 servings). */
    private fun recomputeTotals(detail: RecipeDetail, servings: Double, variantCode: String): NutritionTotals {
        val variant = detail.variants.firstOrNull { it.code == variantCode }
        return RecipeNutritionCalculator.computePerServing(detail.baseTotals, detail.baseServings, variant, servings)
    }

    class Factory(
        private val recipeId: Long,
        private val recipeRepository: RecipeRepository,
        private val nutritionRepository: NutritionRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RecipeDetailViewModel(recipeId, recipeRepository, nutritionRepository) as T
    }

    companion object {
        const val STANDARD_VARIANT_CODE = "STANDARD"
        private const val MIN_SERVINGS = 1
        private const val MAX_SERVINGS = 12
    }
}
