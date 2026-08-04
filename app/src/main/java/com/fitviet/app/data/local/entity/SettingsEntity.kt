package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row table (id is always [SINGLETON_ID]) holding app settings and onboarding selections. */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val languageIsEnglish: Boolean = false,
    val offlineMode: Boolean = true,
    val useImperialUnits: Boolean = false,
    val hasDonated: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val selectedGoal: Int = 0,
    val selectedLevel: Int = 0,
    val selectedSplit: Int = 0,
    /** Set once, when onboarding completes — powers 1i's "N tuần đồng hành" (weeks with the app). */
    val onboardingCompletedAtEpochDay: Long? = null,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
