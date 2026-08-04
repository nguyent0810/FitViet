package com.fitviet.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.MeasurementEntity
import com.fitviet.app.data.local.entity.SettingsEntity
import com.fitviet.app.data.repository.ProfileRepository
import com.fitviet.app.domain.MeasurementDeltaCalculator
import com.fitviet.app.domain.MeasurementDeltas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val settings: SettingsEntity = SettingsEntity(),
    val latestMeasurement: MeasurementEntity? = null,
    val deltas: MeasurementDeltas = MeasurementDeltas(null, null, null, null),
    val showUpdateSheet: Boolean = false,
)

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {
    private val showUpdateSheet = MutableStateFlow(false)

    val uiState: StateFlow<ProfileUiState> = combine(repository.observe(), showUpdateSheet) { data, showSheet ->
        ProfileUiState(
            settings = data.settings,
            latestMeasurement = data.latestMeasurement,
            deltas = MeasurementDeltaCalculator.compute(data.latestMeasurement, data.previousMeasurement),
            showUpdateSheet = showSheet,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun cycleLanguage() = viewModelScope.launch { repository.cycleLanguage() }

    fun toggleOffline() = viewModelScope.launch { repository.toggleOffline() }

    fun cycleUnits() = viewModelScope.launch { repository.cycleUnits() }

    fun toggleDonated() = viewModelScope.launch { repository.toggleDonated() }

    fun openUpdateSheet() {
        showUpdateSheet.value = true
    }

    fun dismissUpdateSheet() {
        showUpdateSheet.value = false
    }

    fun saveMeasurement(weightKg: Double?, chestCm: Double?, waistCm: Double?, armCm: Double?) {
        viewModelScope.launch {
            repository.addMeasurement(weightKg, chestCm, waistCm, armCm)
            showUpdateSheet.value = false
        }
    }

    class Factory(private val repository: ProfileRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ProfileViewModel(repository) as T
    }
}
