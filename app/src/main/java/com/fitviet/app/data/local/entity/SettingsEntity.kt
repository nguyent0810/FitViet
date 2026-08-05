package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Single-row table (id is always [SINGLETON_ID]) holding app settings and onboarding selections. */
@Entity(
    tableName = "settings",
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["activeProgramId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("activeProgramId")],
)
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
    /** The program the user has chosen as "current" (2b's "Đặt làm giáo án hiện tại"). Null before
     * any explicit choice — the dashboard falls back to the first seeded program in that case. */
    val activeProgramId: Long? = null,
    /** Feature #12 — per-widget Dashboard visibility toggles, all on by default. The hero card and
     * the weekly-volume/stat-tile row are always shown (core content, not optional widgets). */
    val showRecommendationCard: Boolean = true,
    val showMuscleBalanceCard: Boolean = true,
    val showNutritionCard: Boolean = true,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
