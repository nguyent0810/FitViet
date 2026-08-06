package com.fitviet.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileEditUiState(
    val displayName: String = "",
    val avatarId: Int = 0,
    /** False until the persisted settings row has loaded once — the screen shows nothing
     * editable before this, so it can't flash the default draft over a real saved name. */
    val isLoaded: Boolean = false,
    /** True for one frame after [ProfileEditViewModel.save] persists — the screen observes this
     * to navigate back, then [ProfileEditViewModel] is torn down with it (no reset needed). */
    val saved: Boolean = false,
)

/** Backs [ProfileEditScreen] (feature #1, Gate 35) — deliberately its own small ViewModel rather
 * than folded into [ProfileViewModel]: this screen holds local draft edits the user can discard by
 * navigating back without saving, which would be awkward to reconcile with [ProfileViewModel]'s
 * continuously-observed [com.fitviet.app.data.local.entity.SettingsEntity] `Flow` (a Room emission
 * mid-edit — e.g. a completely unrelated settings toggle from another screen — would silently
 * overwrite whatever the user was typing here). */
class ProfileEditViewModel(private val repository: ProfileRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = repository.getSettings()
            _uiState.update { it.copy(displayName = settings.displayName, avatarId = settings.avatarId, isLoaded = true) }
        }
    }

    fun updateDisplayName(name: String) {
        _uiState.update { it.copy(displayName = name) }
    }

    fun selectAvatar(avatarId: Int) {
        _uiState.update { it.copy(avatarId = avatarId) }
    }

    /** No-ops on a blank name (after trimming) rather than persisting an empty display name — the
     * Save button is disabled for that case too (see [ProfileEditScreen]), this is the same guard
     * enforced on the ViewModel side in case that ever changes. */
    fun save() {
        val state = _uiState.value
        val trimmedName = state.displayName.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch {
            repository.updateProfile(trimmedName, state.avatarId)
            _uiState.update { it.copy(saved = true) }
        }
    }

    class Factory(private val repository: ProfileRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ProfileEditViewModel(repository) as T
    }
}
