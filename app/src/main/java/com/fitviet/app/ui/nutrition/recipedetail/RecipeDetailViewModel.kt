package com.fitviet.app.ui.nutrition.recipedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.repository.RecipeDetail
import com.fitviet.app.data.repository.RecipeRepository
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
    val isFavorite: Boolean = false,
    val totals: NutritionTotals = NutritionTotals(kcal = 0, proteinG = 0, carbG = 0, fatG = 0),
)

/**
 * Gate C4 — Recipe Detail's servings-stepper/variant-pill recompute is entirely local: [detailFlow]
 * is loaded once (a [RecipeDetail] never changes mid-session), then [servingsFlow]/[variantFlow]
 * drive [RecipeNutritionCalculator] directly with no repository round-trip, same "single source
 * of truth" contract [RecipeDetail]'s own doc comment describes.
 */
class RecipeDetailViewModel(
    private val recipeId: Long,
    private val recipeRepository: RecipeRepository,
) : ViewModel() {
    private val detailFlow = MutableStateFlow<RecipeDetail?>(null)
    private val servingsFlow = MutableStateFlow(1)
    private val variantFlow = MutableStateFlow(STANDARD_VARIANT_CODE)

    init {
        viewModelScope.launch {
            val loaded = recipeRepository.getRecipeDetail(recipeId)
            detailFlow.value = loaded
            servingsFlow.value = loaded?.baseServings?.coerceAtLeast(MIN_SERVINGS) ?: MIN_SERVINGS
        }
    }

    val uiState: StateFlow<RecipeDetailUiState> = combine(
        detailFlow,
        servingsFlow,
        variantFlow,
        recipeRepository.observeIsFavorite(recipeId),
    ) { detail, servings, variantCode, isFavorite ->
        RecipeDetailUiState(
            isLoading = detail == null,
            detail = detail,
            servings = servings,
            selectedVariantCode = variantCode,
            isFavorite = isFavorite,
            totals = detail?.let { recomputeTotals(it, servings.toDouble(), variantCode) }
                ?: NutritionTotals(kcal = 0, proteinG = 0, carbG = 0, fatG = 0),
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

    fun toggleFavorite() {
        viewModelScope.launch { recipeRepository.toggleFavorite(recipeId) }
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
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RecipeDetailViewModel(recipeId, recipeRepository) as T
    }

    companion object {
        const val STANDARD_VARIANT_CODE = "STANDARD"
        private const val MIN_SERVINGS = 1
        private const val MAX_SERVINGS = 12
    }
}
