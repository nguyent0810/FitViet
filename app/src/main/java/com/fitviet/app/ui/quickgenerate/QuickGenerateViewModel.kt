package com.fitviet.app.ui.quickgenerate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.repository.MonthlyPlanRepository
import com.fitviet.app.data.repository.MonthlyPlanUserChoices
import com.fitviet.app.data.repository.OnboardingRepository
import com.fitviet.app.domain.ExerciseDifficulty
import com.fitviet.app.domain.SplitTemplate
import com.fitviet.app.domain.TrainingGoal
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickGenerateUiState(
    val isLoading: Boolean = true,
    val goal: TrainingGoal = TrainingGoal.HYPERTROPHY,
    val level: ExerciseDifficulty = ExerciseDifficulty.BEGINNER,
    val splitTemplate: SplitTemplate = SplitTemplate.PPL,
    val daysPerWeek: Int = 3,
    val isGenerating: Boolean = false,
)

/**
 * "Hit & Run" (Gate 63+) — Goal → Split → Days/week, pre-filled from onboarding's saved selections
 * but freely editable (per the "Hit & Run" plan's judgment call #1: the mapping from onboarding's 4
 * goals/5 splits to the generator's 3 goals/5 templates is approximate, so it's always visible and
 * correctable here rather than hidden behind a "just trust onboarding" 1-tap flow).
 */
class QuickGenerateViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val monthlyPlanRepository: MonthlyPlanRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuickGenerateUiState())
    val uiState: StateFlow<QuickGenerateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = onboardingRepository.getSelections()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    goal = mapOnboardingGoal(saved.selectedGoal),
                    level = ExerciseDifficulty.entries.getOrElse(saved.selectedLevel) { ExerciseDifficulty.BEGINNER },
                    splitTemplate = mapOnboardingSplit(saved.selectedSplit),
                    daysPerWeek = saved.selectedDaysPerWeek.coerceIn(MIN_DAYS_PER_WEEK, MAX_DAYS_PER_WEEK),
                )
            }
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
        val state = _uiState.value
        if (state.isLoading || state.isGenerating) return false
        _uiState.update { it.copy(isGenerating = true) }
        return try {
            monthlyPlanRepository.generate(
                MonthlyPlanUserChoices(
                    goal = state.goal,
                    level = state.level,
                    splitTemplate = state.splitTemplate,
                    daysPerWeek = state.daysPerWeek,
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

/** Onboarding's 4 goals (index order: Tăng cơ, Giảm mỡ, Tăng sức mạnh, Giữ dáng — see
 * [com.fitviet.app.ui.onboarding.GOAL_OPTIONS]) map to the generator's 3 per the "Hit & Run"
 * plan's judgment call #1. An out-of-range index (shouldn't happen, but onboarding stores a raw
 * Int) falls back to GENERAL_FITNESS, same as the two goals that already map there. */
private fun mapOnboardingGoal(index: Int): TrainingGoal = when (index) {
    0 -> TrainingGoal.HYPERTROPHY
    2 -> TrainingGoal.STRENGTH
    else -> TrainingGoal.GENERAL_FITNESS
}

/** Onboarding's 5 split options (see [com.fitviet.app.ui.onboarding.SPLIT_OPTIONS]) don't line up
 * 1:1 with the generator's 5 [SplitTemplate]s — onboarding's index 2 ("Ngực + tay sau / Xô + tay
 * trước", a paired-muscle-group split) has no exact generator equivalent. Mapped to BRO_SPLIT as
 * the closest fit (both are muscle-group-focused, just different granularity) — an explicit,
 * flaggable judgment call, not an oversight; the split picker below is always shown and editable
 * so a wrong guess here costs the user one extra tap, not a wrong plan. */
private fun mapOnboardingSplit(index: Int): SplitTemplate = when (index) {
    0 -> SplitTemplate.PPL
    1 -> SplitTemplate.UPPER_LOWER
    3 -> SplitTemplate.BRO_SPLIT
    4 -> SplitTemplate.FULL_BODY
    else -> SplitTemplate.BRO_SPLIT
}
