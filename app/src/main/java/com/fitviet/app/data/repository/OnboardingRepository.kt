package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.entity.SettingsEntity
import com.fitviet.app.domain.ExerciseDifficulty
import com.fitviet.app.domain.NutritionGoal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OnboardingRepository(private val settingsDao: SettingsDao) {

    /** True once the user has finished onboarding. Defaults to false if settings haven't been written yet. */
    fun isOnboardingCompleted(): Flow<Boolean> =
        settingsDao.observe().map { it?.onboardingCompleted ?: false }

    suspend fun getSelections(): SettingsEntity = settingsDao.get() ?: SettingsEntity()

    /** Redesign Gate 2a — the single-screen onboarding's one write, replacing the old multi-step
     * incremental-save flow (no intermediate screens to lose progress between anymore, so there's
     * no need to persist every tap separately). [equipmentProfile] mirrors [com.fitviet.app.domain
     * .EquipmentProfiles] semantics (null = unconstrained/Phòng gym, else a profile key) — see
     * [com.fitviet.app.data.local.entity.SettingsEntity.equipmentProfile]'s own doc.
     * `selectedLevel`/`selectedSplit` are deliberately left untouched at their entity defaults —
     * onboarding no longer asks about either (split is auto-derived from [daysPerWeek] at
     * generation time, not a stored preference; level is closed by [updateSelectedLevel] instead —
     * see that function's own doc for why). */
    suspend fun completeOnboarding(goal: NutritionGoal, equipmentProfile: String?, daysPerWeek: Int) {
        val current = settingsDao.get() ?: SettingsEntity()
        settingsDao.upsert(
            current.copy(
                selectedGoal = goal.name,
                equipmentProfile = equipmentProfile,
                selectedDaysPerWeek = daysPerWeek,
                onboardingCompleted = true,
                // Don't overwrite on a later re-completion (e.g. redoing onboarding) if already set.
                onboardingCompletedAtEpochDay = current.onboardingCompletedAtEpochDay ?: LocalDate.now().toEpochDay(),
            ),
        )
    }

    /** Redesign Gate 2a follow-up — [SettingsEntity.selectedLevel] has had zero writers since this
     * gate deleted the old level-picking onboarding step; [com.fitviet.app.ui.profile.ProfileScreen]
     * reads it as the permanent "trình độ" display, and the new onboarding note explicitly promises
     * it's editable later. Quick Generate's own level picker is the only remaining place a level is
     * ever chosen, so it calls this on every `generate()` to keep Profile honest. A dedicated
     * level-editor row on Profile itself (closing the gap for users who never open Quick Generate)
     * is already scoped for Phase 7a of this redesign. */
    suspend fun updateSelectedLevel(level: ExerciseDifficulty) {
        val current = settingsDao.get() ?: SettingsEntity()
        settingsDao.upsert(current.copy(selectedLevel = level.name))
    }
}
