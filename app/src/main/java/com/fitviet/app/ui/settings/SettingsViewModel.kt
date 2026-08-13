package com.fitviet.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.SettingsEntity
import com.fitviet.app.data.repository.ProfileRepository
import com.fitviet.app.data.repository.RemindersRepository
import com.fitviet.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: SettingsEntity = SettingsEntity(),
    /** Feeds the "Nhắc nhở tập luyện" row's dynamic "N lịch ›" value (mockup) instead of a static
     * "›" that never reflected how many reminders actually exist. */
    val reminderCount: Int = 0,
    val showResetConfirmDialog: Boolean = false,
    /** True for one frame after [SettingsViewModel.confirmReset] completes — the screen observes
     * this to navigate all the way back to onboarding (see [SettingsViewModel]'s class doc for why
     * that needs an explicit nav call rather than relying on the reactive onboarding-completed
     * check `FitVietNavHost` already does on cold start). */
    val resetComplete: Boolean = false,
)

/**
 * Backs `ui/settings/SettingsScreen.kt` (feature #6, Gate 37). Read/write for language, offline
 * mode, units, and the three Dashboard widget-visibility toggles all delegate to [ProfileRepository]
 * — those settings already lived there since Gates 6/19 and moving *where they're edited from* in
 * the UI doesn't change *who owns the data*. [SettingsRepository] is only consulted for the one
 * action genuinely new to this screen: the destructive reset.
 *
 * Reset navigation note: after [SettingsRepository.resetAppData] flips `SettingsEntity.onboardingCompleted`
 * back to `false`, simply letting that propagate through `FitVietNavHost`'s existing
 * `onboardingCompleted` check would NOT actually move the user anywhere — Compose Navigation's
 * `NavHost.startDestination` is only consulted on the graph's first composition, not on every
 * recomposition, and the already-live `NavController` would just keep sitting on whatever back
 * stack it had (e.g. still showing Settings). [resetComplete] exists so the screen can trigger an
 * explicit, imperative `navController.navigate(...)` back to the onboarding graph instead of
 * depending on that reactive (and here, ineffective) recomposition path.
 */
class SettingsViewModel(
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    remindersRepository: RemindersRepository,
) : ViewModel() {
    private val showResetConfirmDialog = MutableStateFlow(false)
    private val resetComplete = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        profileRepository.observe(), remindersRepository.observeAll(), showResetConfirmDialog, resetComplete,
    ) { data, reminders, showConfirm, reset ->
        SettingsUiState(
            settings = data.settings,
            reminderCount = reminders.size,
            showResetConfirmDialog = showConfirm,
            resetComplete = reset,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun cycleLanguage() = viewModelScope.launch { profileRepository.cycleLanguage() }

    fun toggleOffline() = viewModelScope.launch { profileRepository.toggleOffline() }

    fun cycleUnits() = viewModelScope.launch { profileRepository.cycleUnits() }

    fun toggleShowRecommendationCard() = viewModelScope.launch { profileRepository.toggleShowRecommendationCard() }

    fun toggleShowMuscleBalanceCard() = viewModelScope.launch { profileRepository.toggleShowMuscleBalanceCard() }

    fun toggleShowNutritionCard() = viewModelScope.launch { profileRepository.toggleShowNutritionCard() }

    fun requestReset() {
        showResetConfirmDialog.value = true
    }

    fun dismissResetConfirm() {
        showResetConfirmDialog.value = false
    }

    fun confirmReset() {
        showResetConfirmDialog.value = false
        viewModelScope.launch {
            settingsRepository.resetAppData()
            resetComplete.value = true
        }
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val settingsRepository: SettingsRepository,
        private val remindersRepository: RemindersRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(profileRepository, settingsRepository, remindersRepository) as T
    }
}
