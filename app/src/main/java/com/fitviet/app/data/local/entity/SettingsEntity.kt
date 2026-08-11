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
        ForeignKey(
            entity = MonthlyPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["activeMonthlyPlanId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("activeProgramId"), Index("activeMonthlyPlanId")],
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
    /** Feature #3 (Gate 39) — days/week chosen on the split step, 2..6. Drives the "GỢI Ý" badge
     * on [com.fitviet.app.ui.onboarding.SplitScreen]'s split cards via
     * [com.fitviet.app.ui.onboarding.SplitOption.recommendedFor]. */
    val selectedDaysPerWeek: Int = 3,
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
    /** Feature #1 (Gate 35) — the user's editable display name and chosen monogram avatar (an
     * index into [com.fitviet.app.ui.profile.AvatarStyle.entries]). Defaults match the previous
     * hardcoded placeholder identity so a fresh/pre-migration install looks unchanged. */
    val displayName: String = "Minh Nguyễn",
    val avatarId: Int = 0,
    /** Feature #11b (Gate 48) — flips true once the user has dismissed the "day exercise list"
     * preview screen's first-run superset explainer card, so it doesn't show again. */
    val hasSeenSupersetHint: Boolean = false,
    /** "Hit & Run" (Gate 63+) — the generated monthly plan currently driving the Dashboard "Today"
     * card, independent of [activeProgramId]: a user can have both a hand-picked program and an
     * active monthly plan at once, only the Today card prefers this one when non-null. */
    val activeMonthlyPlanId: Long? = null,
    /** "Hit & Run" — power-user toggle to skip [com.fitviet.app.ui.workout.WorkoutPreviewScreen]
     * entirely and go straight from a Start action into the live session. Off by default so the
     * first-run "day exercise list" screen still shows until the user opts out. */
    val skipWorkoutPreview: Boolean = false,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
