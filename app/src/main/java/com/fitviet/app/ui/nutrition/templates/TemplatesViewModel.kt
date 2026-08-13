package com.fitviet.app.ui.nutrition.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.entity.MealPlanTemplateEntity
import com.fitviet.app.data.repository.MealPlanRepository
import com.fitviet.app.domain.NutritionGoal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TemplatesUiState(
    val templates: List<MealPlanTemplateEntity> = emptyList(),
    /** [MealPlanTemplateEntity.goalCode] value the "PHÙ HỢP MỤC TIÊU" hero badge highlights —
     * derived from the user's own onboarding goal choice via [NutritionGoal.fromStored]. */
    val matchedGoalCode: String = NutritionGoal.MAINTAIN.name,
    val isGenerating: Boolean = false,
)

class TemplatesViewModel(
    private val mealPlanRepository: MealPlanRepository,
    private val settingsDao: SettingsDao,
) : ViewModel() {
    private val isGenerating = MutableStateFlow(false)

    val uiState: StateFlow<TemplatesUiState> = combine(
        mealPlanRepository.observeTemplates(),
        settingsDao.observe(),
        isGenerating,
    ) { templates, settings, generating ->
        TemplatesUiState(
            templates = templates,
            matchedGoalCode = NutritionGoal.fromStored(settings?.selectedGoal).name,
            isGenerating = generating,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TemplatesUiState())

    /** Returns `true` on success — the caller navigates to the Plan screen only then. A
     * re-entrant tap while already generating is dropped via [isGenerating]'s own value, not a
     * gate on any unrelated loading flag, so no legitimate tap is ever silently swallowed (the
     * Quick Generate CTA bug from the "Hit & Run" feature is the precedent for this care). */
    suspend fun useTemplate(templateId: Long): Boolean {
        if (isGenerating.value) return false
        isGenerating.value = true
        return try {
            mealPlanRepository.generateFromTemplate(templateId)
            true
        } finally {
            isGenerating.value = false
        }
    }

    class Factory(
        private val mealPlanRepository: MealPlanRepository,
        private val settingsDao: SettingsDao,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TemplatesViewModel(mealPlanRepository, settingsDao) as T
    }
}
