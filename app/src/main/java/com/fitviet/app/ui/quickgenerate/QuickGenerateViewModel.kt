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
import com.fitviet.app.domain.defaultSplitTemplateFor
import com.fitviet.app.domain.toInitialTrainingGoal
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
     * .SettingsEntity.equipmentProfile], written by Gate 2a's single-screen onboarding ("BẠN TẬP Ở
     * ĐÂU"). Not exposed as a picker on this screen — no UI here has ever let the user set it
     * directly, it's purely a prefill this screen's generation call respects. */
    val equipmentProfile: String? = null,
    val isGenerating: Boolean = false,
)

/**
 * "Hit & Run" (Gate 63+) — Goal → Split → Days/week, pre-filled but freely editable. Both goal and
 * split prefer the active plan's own real stored value once one exists (an exact, honest value,
 * matching what `generate()` will actually replace) instead of the coarser settings-derived guess;
 * before any plan exists, goal is seeded from onboarding's 3-option `NutritionGoal` question via
 * [com.fitviet.app.domain.toInitialTrainingGoal] and split from the same heuristic onboarding used
 * ([com.fitviet.app.domain.defaultSplitTemplateFor], not the unwritten `selectedSplit` setting —
 * redesign Gate 2a's single-screen onboarding never asks for split directly). Neither prefill is
 * ever silently trusted — this screen's own picker is always visible and correctable, so a wrong
 * guess costs one tap, never a wrong plan. One known gap from Gate 1b's onboarding-goal-list trim
 * (3 options, no "Sức mạnh"): a user can't reach `TrainingGoal.STRENGTH` from onboarding's own
 * first-ever seed — only from this screen's picker, or from a sample-program-seeded plan's real
 * stored goal.
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
            // "Hit & Run" redesign (Gate 1b, split prefill fixed Gate 2a) — prefer the active
            // plan's own real `goal`/`splitTemplate` (already written on every generate) when a
            // plan exists, since that's the exact value the redesign's "Kế hoạch" sheet should
            // redisplay honestly; only seed from onboarding's cruder settings-derived guess on the
            // very first-ever generate, when no plan row exists yet to read from. `selectedSplit`
            // is otherwise never written post-Gate-2a (single-screen onboarding never asks for it
            // directly), so falling back to it here would show a split unrelated to the plan
            // onboarding actually generated.
            val activePlan = monthlyPlanRepository.observeActivePlanId().first()
                ?.let { planId -> monthlyPlanRepository.observePlan(planId).first() }
            val planGoal = activePlan?.goal?.let { stored -> TrainingGoal.entries.find { it.name == stored } }
            val planSplit = activePlan?.splitTemplate?.let { stored -> SplitTemplate.entries.find { it.name == stored } }
            _uiState.update {
                it.copy(
                    goal = planGoal ?: NutritionGoal.fromStored(saved.selectedGoal).toInitialTrainingGoal(),
                    level = ExerciseDifficulty.entries.find { d -> d.name == saved.selectedLevel } ?: ExerciseDifficulty.BEGINNER,
                    splitTemplate = planSplit ?: defaultSplitTemplateFor(saved.selectedDaysPerWeek),
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
     * [com.fitviet.app.ui.onboarding.OnboardingViewModel.submit] uses) so a `popUpTo`/screen-pop
     * right after can't cancel the generation mid-transaction. */
    suspend fun generate(): Boolean {
        // Preserve the user's first tap if it happens before prefill completes instead of
        // returning false and requiring an unexplained second tap.
        initialization.join()
        val state = _uiState.value
        if (state.isGenerating) return false
        _uiState.update { it.copy(isGenerating = true) }
        return try {
            onboardingRepository.updateSelectedLevel(state.level)
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
