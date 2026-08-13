package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fitviet.app.domain.ExerciseDifficulty
import com.fitviet.app.domain.NutritionGoal
import com.fitviet.app.domain.SplitTemplate

/** Single-row table (id is always [SINGLETON_ID]) holding app settings and onboarding selections. */
@Entity(
    tableName = "settings",
    foreignKeys = [
        ForeignKey(
            entity = MonthlyPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["activeMonthlyPlanId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("activeMonthlyPlanId")],
)
data class SettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val languageIsEnglish: Boolean = false,
    val offlineMode: Boolean = true,
    val useImperialUnits: Boolean = false,
    val hasDonated: Boolean = false,
    val onboardingCompleted: Boolean = false,
    /** "Hit & Run" redesign (Gate 1b) — was a positional `Int` index into onboarding's option
     * list; retyped to the enum's own `.name` so the value is self-describing and safe to parse
     * with a fallback default (see each enum's own `fromStored`/`entries.find` helper) rather than
     * silently reinterpreting under a reordered/resized option list, which is exactly what a
     * 4-option-to-3-option onboarding redesign would otherwise do to every already-stored row.
     * Default is [NutritionGoal.BULK] to match the old default's `Int = 0`, which always meant
     * "Tăng cơ" (the pre-redesign onboarding's first goal option) — so a fresh/pre-onboarding row's
     * pre-highlighted pill doesn't silently change (caught in Phase 1 review: an earlier version of
     * this default was `MAINTAIN`, which would have pre-highlighted "Khỏe mạnh" instead for a
     * brand-new install). */
    val selectedGoal: String = NutritionGoal.BULK.name,
    val selectedLevel: String = ExerciseDifficulty.BEGINNER.name,
    val selectedSplit: String = SplitTemplate.PPL.name,
    /** "Hit & Run" redesign (Gate 1b) — onboarding's "BẠN TẬP Ở ĐÂU" (Phòng gym/Tại nhà) answer,
     * same name/semantics as [com.fitviet.app.data.local.entity.MonthlyPlanEntity.equipmentProfile]
     * (a key into [com.fitviet.app.domain.EquipmentProfiles]). Null = unconstrained (Phòng gym).
     * Gate 1d-ii wired the read side through generation; Gate 2a's single-screen onboarding
     * ("BẠN TẬP Ở ĐÂU") is the first real writer. */
    val equipmentProfile: String? = null,
    /** Feature #3 (Gate 39) — days/week chosen during onboarding, 2..6 (Gate 2a's onboarding only
     * offers 2..5; the wider stored range stays valid for values written by the older multi-step
     * flow this replaced). Also drives [com.fitviet.app.domain.defaultSplitTemplateFor]'s
     * auto-derived split at generation time. */
    val selectedDaysPerWeek: Int = 3,
    /** Set once, when onboarding completes — powers 1i's "N tuần đồng hành" (weeks with the app). */
    val onboardingCompletedAtEpochDay: Long? = null,
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
     * card. Redesign Gate 2b made this the app's sole "active plan" concept — `ProgramEntity` is
     * read-only generation input now (see its own doc comment), with no "active program" state of
     * its own anymore. */
    val activeMonthlyPlanId: Long? = null,
    /** Gate D4 — the highest workout streak length (in days) a milestone overlay has already been
     * shown for. [com.fitviet.app.domain.StreakMilestones.crossedMilestone] compares this against
     * the live streak to decide whether a new milestone just came into range; dismissing the
     * overlay bumps this to the current streak so already-passed milestones never re-fire, even if
     * several were skipped over between app opens. */
    val lastCelebratedStreakDays: Int = 0,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
