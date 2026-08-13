package com.fitviet.app.ui.nutrition.createplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.dao.MeasurementDao
import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.repository.MealPlanRepository
import com.fitviet.app.data.repository.MealPlanUserWizardChoices
import com.fitviet.app.domain.NutritionGoal
import com.fitviet.app.domain.NutritionTargetsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreatePlanUiState(
    val isLoading: Boolean = true,
    val goal: NutritionGoal = NutritionGoal.MAINTAIN,
    val kcalTarget: Int = 2000,
    val proteinTargetG: Int = 130,
    val mealsPerDay: Int = 3,
    /** `null` = no ceiling — matches [MealPlanUserWizardChoices.cookTimeCeilingMinutes]'s own
     * contract 1:1. */
    val cookTimeCeilingMinutes: Int? = null,
    val isGenerating: Boolean = false,
)

/**
 * Gate C6 — one-screen create-plan wizard. Goal/kcal/protein sliders are pre-filled from real
 * data (onboarding's saved goal + the user's latest logged body weight via
 * [NutritionTargetsCalculator]), same "prefilled but freely editable" idiom
 * [com.fitviet.app.ui.quickgenerate.QuickGenerateViewModel] already established for the workout
 * side's own one-screen wizard — including its exact fix for the CTA-swallows-first-tap bug
 * (`initialization.join()` before reading state, guard+`try`/`finally` scoped only to this
 * screen's own `isGenerating` flag).
 */
class CreatePlanViewModel(
    private val mealPlanRepository: MealPlanRepository,
    private val settingsDao: SettingsDao,
    private val measurementDao: MeasurementDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePlanUiState())
    val uiState: StateFlow<CreatePlanUiState> = _uiState.asStateFlow()

    private val initialization = viewModelScope.launch {
        try {
            val settings = settingsDao.get()
            val weightKg = measurementDao.observeLatest().first()?.weightKg ?: DEFAULT_BODY_WEIGHT_KG
            val goal = NutritionGoal.fromStored(settings?.selectedGoal)
            _uiState.update {
                it.copy(
                    goal = goal,
                    kcalTarget = NutritionTargetsCalculator.defaultKcalTarget(weightKg, goal),
                    proteinTargetG = NutritionTargetsCalculator.defaultProteinTargetG(weightKg),
                )
            }
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun selectGoal(goal: NutritionGoal) = _uiState.update { it.copy(goal = goal) }

    fun incrementKcal() = _uiState.update { it.copy(kcalTarget = (it.kcalTarget + KCAL_STEP).coerceAtMost(MAX_KCAL)) }
    fun decrementKcal() = _uiState.update { it.copy(kcalTarget = (it.kcalTarget - KCAL_STEP).coerceAtLeast(MIN_KCAL)) }

    fun incrementProtein() = _uiState.update { it.copy(proteinTargetG = (it.proteinTargetG + PROTEIN_STEP_G).coerceAtMost(MAX_PROTEIN_G)) }
    fun decrementProtein() = _uiState.update { it.copy(proteinTargetG = (it.proteinTargetG - PROTEIN_STEP_G).coerceAtLeast(MIN_PROTEIN_G)) }

    fun selectMealsPerDay(count: Int) = _uiState.update { it.copy(mealsPerDay = count) }

    fun selectCookTimeCeiling(minutes: Int?) = _uiState.update { it.copy(cookTimeCeilingMinutes = minutes) }

    /** Awaited by the caller so a screen-pop right after can't cancel the generation
     * mid-transaction — same contract as [com.fitviet.app.ui.quickgenerate.QuickGenerateViewModel.generate]. */
    suspend fun generate(): Boolean {
        initialization.join()
        val state = _uiState.value
        if (state.isGenerating) return false
        _uiState.update { it.copy(isGenerating = true) }
        return try {
            mealPlanRepository.generateFromWizard(
                MealPlanUserWizardChoices(
                    goal = state.goal,
                    kcalTarget = state.kcalTarget,
                    proteinTargetG = state.proteinTargetG,
                    mealsPerDay = state.mealsPerDay,
                    cookTimeCeilingMinutes = state.cookTimeCeilingMinutes,
                ),
            )
            true
        } finally {
            _uiState.update { it.copy(isGenerating = false) }
        }
    }

    class Factory(
        private val mealPlanRepository: MealPlanRepository,
        private val settingsDao: SettingsDao,
        private val measurementDao: MeasurementDao,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CreatePlanViewModel(mealPlanRepository, settingsDao, measurementDao) as T
    }

    companion object {
        /** No [com.fitviet.app.data.local.entity.MeasurementEntity] logged yet — a plausible
         * adult mid-range fallback so the kcal/protein defaults are still sane numbers instead of
         * zero, exactly the same "slider pre-fill, not a data-integrity concern" spirit
         * [NutritionTargetsCalculator]'s own doc comment already describes. */
        private const val DEFAULT_BODY_WEIGHT_KG = 65.0

        const val MIN_MEALS_PER_DAY = 3
        const val MAX_MEALS_PER_DAY = 6
        val COOK_TIME_OPTIONS = listOf(null, 15, 30, 45)

        private const val KCAL_STEP = 50
        private const val MIN_KCAL = 1200
        private const val MAX_KCAL = 4000
        private const val PROTEIN_STEP_G = 5
        private const val MIN_PROTEIN_G = 40
        private const val MAX_PROTEIN_G = 300
    }
}
