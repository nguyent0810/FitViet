package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.MeasurementDao
import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.entity.MeasurementEntity
import com.fitviet.app.data.local.entity.SettingsEntity
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class ProfileData(
    val settings: SettingsEntity,
    val latestMeasurement: MeasurementEntity?,
    val previousMeasurement: MeasurementEntity?,
    // Newest-first, same order as MeasurementDao.observeAll() — full history for the 1i weight
    // chart (see WeightHistoryCalculator), not just the latest/previous pair above.
    val measurementHistory: List<MeasurementEntity>,
    // Re-derived at each local midnight (see dayTicker) so the weight chart's 30-day/3-month
    // windows don't silently go stale if Profile stays open/subscribed across a day change.
    val today: LocalDate,
)

class ProfileRepository(
    private val settingsDao: SettingsDao,
    private val measurementDao: MeasurementDao,
) {
    fun observe(): Flow<ProfileData> = combine(
        settingsDao.observe(),
        measurementDao.observeAll(),
        dayTicker(ZoneId.systemDefault()),
    ) { settings, measurements, today ->
        ProfileData(
            settings = settings ?: SettingsEntity(),
            // observeAll() orders newest-first (see MeasurementDao), so index 0/1 are latest/previous.
            latestMeasurement = measurements.getOrNull(0),
            previousMeasurement = measurements.getOrNull(1),
            measurementHistory = measurements,
            today = today,
        )
    }

    suspend fun cycleLanguage() = updateSettings { it.copy(languageIsEnglish = !it.languageIsEnglish) }

    suspend fun toggleOffline() = updateSettings { it.copy(offlineMode = !it.offlineMode) }

    suspend fun cycleUnits() = updateSettings { it.copy(useImperialUnits = !it.useImperialUnits) }

    suspend fun toggleDonated() = updateSettings { it.copy(hasDonated = !it.hasDonated) }

    /** Feature #12 — Dashboard widget visibility toggles. */
    suspend fun toggleShowRecommendationCard() = updateSettings { it.copy(showRecommendationCard = !it.showRecommendationCard) }

    suspend fun toggleShowMuscleBalanceCard() = updateSettings { it.copy(showMuscleBalanceCard = !it.showMuscleBalanceCard) }

    suspend fun toggleShowNutritionCard() = updateSettings { it.copy(showNutritionCard = !it.showNutritionCard) }

    /** One-shot read for [com.fitviet.app.ui.profile.ProfileEditViewModel]'s initial draft — that
     * screen holds local edits the user can discard by navigating back, so it deliberately does
     * NOT stay subscribed to [observe]'s continuous [SettingsEntity] `Flow` (a mid-edit emission
     * would silently overwrite whatever the user was typing). */
    suspend fun getSettings(): SettingsEntity = settingsDao.get() ?: SettingsEntity()

    /** Feature #1 (Gate 35). [displayName] is assumed already validated/trimmed by the caller. */
    suspend fun updateProfile(displayName: String, avatarId: Int) =
        updateSettings { it.copy(displayName = displayName, avatarId = avatarId) }

    suspend fun addMeasurement(weightKg: Double?, chestCm: Double?, waistCm: Double?, armCm: Double?) {
        measurementDao.insert(
            MeasurementEntity(
                epochDay = LocalDate.now().toEpochDay(),
                weightKg = weightKg,
                chestCm = chestCm,
                waistCm = waistCm,
                armCm = armCm,
            ),
        )
    }

    /** Corrects an existing check-in's values in place. [epochDay] is kept as-is — editing fixes a
     * mistaken measurement, not when it was taken; changing the date of a historical entry is a
     * separate, rarer feature not built here. */
    suspend fun updateMeasurement(id: Long, epochDay: Long, weightKg: Double?, chestCm: Double?, waistCm: Double?, armCm: Double?) {
        measurementDao.update(
            MeasurementEntity(
                id = id,
                epochDay = epochDay,
                weightKg = weightKg,
                chestCm = chestCm,
                waistCm = waistCm,
                armCm = armCm,
            ),
        )
    }

    suspend fun deleteMeasurement(id: Long) {
        measurementDao.deleteById(id)
    }

    private suspend fun updateSettings(transform: (SettingsEntity) -> SettingsEntity) {
        val current = settingsDao.get() ?: SettingsEntity()
        settingsDao.upsert(transform(current))
    }
}
