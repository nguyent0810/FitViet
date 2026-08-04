package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val settingsDao: SettingsDao) {
    fun observe(): Flow<SettingsEntity> = settingsDao.observe().map { it ?: SettingsEntity() }

    suspend fun cycleLanguage() = update { it.copy(languageIsEnglish = !it.languageIsEnglish) }

    suspend fun toggleOffline() = update { it.copy(offlineMode = !it.offlineMode) }

    suspend fun cycleUnits() = update { it.copy(useImperialUnits = !it.useImperialUnits) }

    suspend fun toggleDonated() = update { it.copy(hasDonated = !it.hasDonated) }

    private suspend fun update(transform: (SettingsEntity) -> SettingsEntity) {
        val current = settingsDao.get() ?: SettingsEntity()
        settingsDao.upsert(transform(current))
    }
}
