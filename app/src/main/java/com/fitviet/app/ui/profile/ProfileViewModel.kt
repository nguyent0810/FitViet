package com.fitviet.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.MeasurementEntity
import com.fitviet.app.data.local.entity.SettingsEntity
import com.fitviet.app.data.repository.MeasurementRepository
import com.fitviet.app.data.repository.SettingsRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val settings: SettingsEntity = SettingsEntity(),
    val weeksWithApp: Int = 0,
    val latestMeasurement: MeasurementEntity? = null,
    val previousMeasurement: MeasurementEntity? = null,
    val isUpdateMeasurementOpen: Boolean = false,
)

class ProfileViewModel(
    private val settingsRepository: SettingsRepository,
    private val measurementRepository: MeasurementRepository,
) : ViewModel() {
    private val isUpdateMeasurementOpen = MutableStateFlow(false)

    val uiState: StateFlow<ProfileUiState> = combine(
        settingsRepository.observe(),
        measurementRepository.observeLatestTwo(),
        isUpdateMeasurementOpen,
    ) { settings, measurements, pickerOpen ->
        val completedAt = settings.onboardingCompletedAtEpochDay
        val weeks = if (completedAt == null) 0 else ((LocalDate.now().toEpochDay() - completedAt) / 7).toInt().coerceAtLeast(0)
        ProfileUiState(
            settings = settings,
            weeksWithApp = weeks,
            latestMeasurement = measurements.latest,
            previousMeasurement = measurements.previous,
            isUpdateMeasurementOpen = pickerOpen,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun cycleLanguage() = launch { settingsRepository.cycleLanguage() }
    fun toggleOffline() = launch { settingsRepository.toggleOffline() }
    fun cycleUnits() = launch { settingsRepository.cycleUnits() }
    fun toggleDonated() = launch { settingsRepository.toggleDonated() }

    fun openUpdateMeasurement() {
        isUpdateMeasurementOpen.value = true
    }

    fun closeUpdateMeasurement() {
        isUpdateMeasurementOpen.value = false
    }

    fun saveMeasurement(weightKg: Double?, chestCm: Double?, waistCm: Double?, armCm: Double?) {
        viewModelScope.launch {
            measurementRepository.addCheckIn(
                MeasurementEntity(
                    epochDay = LocalDate.now().toEpochDay(),
                    weightKg = weightKg,
                    chestCm = chestCm,
                    waistCm = waistCm,
                    armCm = armCm,
                ),
            )
        }
        closeUpdateMeasurement()
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val measurementRepository: MeasurementRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProfileViewModel(settingsRepository, measurementRepository) as T
    }
}
