package com.fitviet.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fitviet.app.data.repository.MonthlyPlanRepository
import com.fitviet.app.data.repository.MonthlyPlanUserChoices
import com.fitviet.app.data.repository.OnboardingRepository
import com.fitviet.app.domain.ExerciseDifficulty
import com.fitviet.app.domain.NutritionGoal
import com.fitviet.app.domain.defaultSplitTemplateFor
import com.fitviet.app.domain.toInitialTrainingGoal
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OnboardingUiState(
    val goal: NutritionGoal = NutritionGoal.BULK,
    /** Redesign Gate 2a — null = "Phòng gym" (unconstrained), [com.fitviet.app.domain
     * .EquipmentProfiles.HOME_DUMBBELLS] = "Tại nhà". Same semantics as [com.fitviet.app.data
     * .local.entity.SettingsEntity.equipmentProfile]. */
    val equipmentProfile: String? = null,
    // Mock pre-selects "4" on the SỐ BUỔI/TUẦN row (see the onboarding mock section).
    val daysPerWeek: Int = 4,
    val isSubmitting: Boolean = false,
)

/**
 * "Hit & Run" redesign (Gate 2a) — the single-screen onboarding replacing the old
 * GoalLevelScreen + SplitScreen pair. Purely local UI state: the old multi-step flow saved every
 * tap to Room incrementally because progress could be lost navigating between two screens: with
 * only one screen and one explicit submit action, [submit] just writes and generates atomically
 * instead. Level and split are never asked here: level defaults to [ExerciseDifficulty.BEGINNER]
 * (see [OnboardingRepository.updateSelectedLevel] for how Quick Generate later keeps it honest once
 * the user actually picks a level there), and split is auto-derived via [defaultSplitTemplateFor].
 */
class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val monthlyPlanRepository: MonthlyPlanRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun selectGoal(goal: NutritionGoal) = _uiState.update { it.copy(goal = goal) }
    fun selectEquipmentProfile(equipmentProfile: String?) = _uiState.update { it.copy(equipmentProfile = equipmentProfile) }
    fun selectDaysPerWeek(days: Int) = _uiState.update { it.copy(daysPerWeek = days) }

    /**
     * "Tạo plan & vào tập →" — writes the 3 answers, then generates the first monthly plan
     * directly (the mock's single-tap CTA; the caller navigates straight into the live workout
     * session on success — the exact same no-arg entry point Gate 1c's [com.fitviet.app.ui
     * .workout.WorkoutViewModel] already resolves "today" through, guaranteed trainable by
     * Gate 1d-i's today-anchored offsets). Returns false (a no-op) if already submitting, same
     * double-tap guard [com.fitviet.app.ui.quickgenerate.QuickGenerateViewModel.generate] uses;
     * lets real exceptions propagate for the same reason — caught and toasted at the nav call site,
     * not swallowed here.
     */
    suspend fun submit(): Boolean {
        val state = _uiState.value
        if (state.isSubmitting) return false
        _uiState.update { it.copy(isSubmitting = true) }
        return try {
            onboardingRepository.completeOnboarding(state.goal, state.equipmentProfile, state.daysPerWeek)
            monthlyPlanRepository.generate(
                MonthlyPlanUserChoices(
                    goal = state.goal.toInitialTrainingGoal(),
                    level = ExerciseDifficulty.BEGINNER,
                    splitTemplate = defaultSplitTemplateFor(state.daysPerWeek),
                    daysPerWeek = state.daysPerWeek,
                    equipmentProfile = state.equipmentProfile,
                ),
                LocalDate.now(),
            )
            true
        } finally {
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    class Factory(
        private val onboardingRepository: OnboardingRepository,
        private val monthlyPlanRepository: MonthlyPlanRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OnboardingViewModel(onboardingRepository, monthlyPlanRepository) as T
    }
}
