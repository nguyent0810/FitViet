package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.entity.SettingsEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OnboardingRepository(private val settingsDao: SettingsDao) {

    /** True once the user has finished 1a + 2a. Defaults to false if settings haven't been written yet. */
    fun isOnboardingCompleted(): Flow<Boolean> =
        settingsDao.observe().map { it?.onboardingCompleted ?: false }

    suspend fun getSelections(): SettingsEntity = settingsDao.get() ?: SettingsEntity()

    suspend fun saveSelections(goal: Int, level: Int, split: Int, daysPerWeek: Int) {
        val current = settingsDao.get() ?: SettingsEntity()
        settingsDao.upsert(
            current.copy(selectedGoal = goal, selectedLevel = level, selectedSplit = split, selectedDaysPerWeek = daysPerWeek),
        )
    }

    /** Atomically writes the final selections and marks onboarding done in one write. */
    suspend fun completeOnboarding(goal: Int, level: Int, split: Int, daysPerWeek: Int) {
        val current = settingsDao.get() ?: SettingsEntity()
        settingsDao.upsert(
            current.copy(
                selectedGoal = goal,
                selectedLevel = level,
                selectedSplit = split,
                selectedDaysPerWeek = daysPerWeek,
                onboardingCompleted = true,
                // Don't overwrite on a later re-completion (e.g. redoing onboarding) if already set.
                onboardingCompletedAtEpochDay = current.onboardingCompletedAtEpochDay ?: LocalDate.now().toEpochDay(),
            ),
        )
    }
}
