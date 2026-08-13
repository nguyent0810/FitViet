package com.fitviet.app.ui.quickgenerate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.repository.MonthlyPlanRepository
import com.fitviet.app.data.repository.MonthlyPlanUserChoices
import com.fitviet.app.data.repository.OnboardingRepository
import com.fitviet.app.domain.ExerciseDifficulty
import com.fitviet.app.domain.NutritionGoal
import com.fitviet.app.domain.SplitTemplate
import com.fitviet.app.domain.TrainingGoal
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickGenerateUiState(
    val isLoading: Boolean = true,
    val goal: TrainingGoal = TrainingGoal.HYPERTROPHY,
    val level: ExerciseDifficulty = ExerciseDifficulty.BEGINNER,
    val splitTemplate: SplitTemplate = SplitTemplate.PPL,
    val daysPerWeek: Int = 3,
    /** Redesign Gate 1d-ii — read straight from [com.fitviet.app.data.local.entity
     * .SettingsEntity.equipmentProfile], which nothing writes yet (no onboarding question asks for
     * it until Phase 2's single-screen redesign). Always null today in practice, but threading it
     * through here means generation starts respecting it the moment that write path exists, with
     * no further change needed on this screen. Not exposed as a picker on this screen — no UI here
     * has ever let the user set it directly. */
    val equipmentProfile: String? = null,
    val isGenerating: Boolean = false,
)

/**
 * "Hit & Run" (Gate 63+) — Goal → Split → Days/week, pre-filled but freely editable. Goal prefers
 * the active plan's own real `TrainingGoal` once one exists (an exact, honest value); before that,
 * it's seeded from onboarding's cruder 3-option `NutritionGoal` question via [trainingGoalFor]
 * (redesign Gate 1b — see that function's own doc comment). Split prefers onboarding's saved
 * `SplitTemplate` directly (a 1:1 mapping, no approximation). Neither prefill is ever silently
 * trusted — this screen's own picker is always visible and correctable, so a wrong guess costs one
 * tap, never a wrong plan. One known gap from Gate 1b's onboarding-goal-list trim (3 options, no
 * "Sức mạnh"): a user can't reach `TrainingGoal.STRENGTH` from onboarding's own first-ever seed —
 * only from this screen's picker, or from a sample-program-seeded plan's real stored goal.
 */
class QuickGenerateViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val monthlyPlanRepository: MonthlyPlanRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuickGenerateUiState())
    val uiState: StateFlow<QuickGenerateUiState> = _uiState.asStateFlow()

    private val initialization = viewModelScope.launch {
        try {
            val saved = onboardingRepository.getSelections()
            // "Hit & Run" redesign (Gate 1b) — prefer the active plan's own real `goal` (a
            // TrainingGoal, already written on every generate) when one exists, since it's the
            // exact value the redesign's "Kế hoạch" sheet should redisplay honestly; only seed
            // from onboarding's cruder NutritionGoal question on the very first-ever generate,
            // when no plan row exists yet to read from.
            val planGoal = monthlyPlanRepository.observeActivePlanId().first()
                ?.let { planId -> monthlyPlanRepository.observePlan(planId).first() }
                ?.goal
                ?.let { stored -> TrainingGoal.entries.find { it.name == stored } }
            _uiState.update {
                it.copy(
                    goal = planGoal ?: trainingGoalFor(NutritionGoal.fromStored(saved.selectedGoal)),
                    level = ExerciseDifficulty.entries.find { d -> d.name == saved.selectedLevel } ?: ExerciseDifficulty.BEGINNER,
                    splitTemplate = SplitTemplate.entries.find { t -> t.name == saved.selectedSplit } ?: SplitTemplate.BRO_SPLIT,
                    daysPerWeek = saved.selectedDaysPerWeek.coerceIn(MIN_DAYS_PER_WEEK, MAX_DAYS_PER_WEEK),
                    equipmentProfile = saved.equipmentProfile,
                )
            }
        } finally {
            // A failed settings read must not leave the CTA permanently and invisibly disabled.
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun selectGoal(goal: TrainingGoal) = _uiState.update { it.copy(goal = goal) }
    fun selectLevel(level: ExerciseDifficulty) = _uiState.update { it.copy(level = level) }
    fun selectSplit(split: SplitTemplate) = _uiState.update { it.copy(splitTemplate = split) }
    fun selectDaysPerWeek(days: Int) = _uiState.update { it.copy(daysPerWeek = days) }

    /** Awaited by the caller (same "await the write before navigating" pattern
     * [com.fitviet.app.ui.onboarding.OnboardingViewModel.completeOnboarding] uses) so a
     * `popUpTo`/screen-pop right after can't cancel the generation mid-transaction. */
    suspend fun generate(): Boolean {
        // Preserve the user's first tap if it happens before prefill completes instead of
        // returning false and requiring an unexplained second tap.
        initialization.join()
        val state = _uiState.value
        if (state.isGenerating) return false
        _uiState.update { it.copy(isGenerating = true) }
        return try {
            monthlyPlanRepository.generate(
                MonthlyPlanUserChoices(
                    goal = state.goal,
                    level = state.level,
                    splitTemplate = state.splitTemplate,
                    daysPerWeek = state.daysPerWeek,
                    equipmentProfile = state.equipmentProfile,
                ),
                LocalDate.now(),
            )
            true
        } finally {
            _uiState.update { it.copy(isGenerating = false) }
        }
    }

    class Factory(
        private val onboardingRepository: OnboardingRepository,
        private val monthlyPlanRepository: MonthlyPlanRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuickGenerateViewModel(onboardingRepository, monthlyPlanRepository) as T
    }
}

private const val MIN_DAYS_PER_WEEK = 2
private const val MAX_DAYS_PER_WEEK = 6

/** "Hit & Run" redesign (Gate 1b) — onboarding only asks a body-composition question
 * ([NutritionGoal]: BULK/CUT/MAINTAIN), not a training-prescription one; this is the one-time seed
 * used only when no [com.fitviet.app.data.local.entity.MonthlyPlanEntity] exists yet to prefill
 * from (first generate ever). Once any plan has been generated, `initialization` above prefers
 * that plan's own real `goal` (see [com.fitviet.app.data.repository.MonthlyPlanRepository]) —
 * this function never runs again after that, so a wrong guess here costs at most one tap on the
 * always-visible, always-editable goal picker below, never a silently wrong plan. Cutting is a
 * nutrition strategy, not a distinct set/rep prescription, so both CUT and MAINTAIN seed the same
 * GENERAL_FITNESS training goal — deliberately, not a rounding accident. */
private fun trainingGoalFor(nutritionGoal: NutritionGoal): TrainingGoal = when (nutritionGoal) {
    NutritionGoal.BULK -> TrainingGoal.HYPERTROPHY
    NutritionGoal.CUT, NutritionGoal.MAINTAIN -> TrainingGoal.GENERAL_FITNESS
}
