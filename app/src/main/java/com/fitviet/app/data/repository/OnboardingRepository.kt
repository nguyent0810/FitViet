package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.entity.SettingsEntity
import com.fitviet.app.domain.ExerciseDifficulty
import com.fitviet.app.domain.NutritionGoal
import com.fitviet.app.domain.SplitTemplate
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Read-side shape matching [com.fitviet.app.ui.onboarding.OnboardingUiState]'s 4 index fields. */
data class OnboardingIndices(val goal: Int, val level: Int, val split: Int, val daysPerWeek: Int)

class OnboardingRepository(private val settingsDao: SettingsDao) {

    /** True once the user has finished 1a + 2a. Defaults to false if settings haven't been written yet. */
    fun isOnboardingCompleted(): Flow<Boolean> =
        settingsDao.observe().map { it?.onboardingCompleted ?: false }

    suspend fun getSelections(): SettingsEntity = settingsDao.get() ?: SettingsEntity()

    /** "Hit & Run" redesign (Gate 1b) — [com.fitviet.app.ui.onboarding.OnboardingViewModel]'s own
     * UI state is index-based against `GOAL_OPTIONS`/`LEVEL_OPTIONS`/`SPLIT_OPTIONS`; this is the
     * read-side counterpart of [saveSelections]/[completeOnboarding]'s write-side translation. */
    suspend fun getSelectionsAsIndices(): OnboardingIndices {
        val saved = getSelections()
        return OnboardingIndices(
            goal = storedToGoalIndex(saved.selectedGoal),
            level = storedToLevelIndex(saved.selectedLevel),
            split = storedToSplitIndex(saved.selectedSplit),
            daysPerWeek = saved.selectedDaysPerWeek,
        )
    }

    suspend fun saveSelections(goal: Int, level: Int, split: Int, daysPerWeek: Int) {
        val current = settingsDao.get() ?: SettingsEntity()
        settingsDao.upsert(
            current.copy(
                selectedGoal = goalIndexToStored(goal),
                selectedLevel = levelIndexToStored(level),
                selectedSplit = splitIndexToStored(split),
                selectedDaysPerWeek = daysPerWeek,
            ),
        )
    }

    /** Atomically writes the final selections and marks onboarding done in one write. */
    suspend fun completeOnboarding(goal: Int, level: Int, split: Int, daysPerWeek: Int) {
        val current = settingsDao.get() ?: SettingsEntity()
        settingsDao.upsert(
            current.copy(
                selectedGoal = goalIndexToStored(goal),
                selectedLevel = levelIndexToStored(level),
                selectedSplit = splitIndexToStored(split),
                selectedDaysPerWeek = daysPerWeek,
                onboardingCompleted = true,
                // Don't overwrite on a later re-completion (e.g. redoing onboarding) if already set.
                onboardingCompletedAtEpochDay = current.onboardingCompletedAtEpochDay ?: LocalDate.now().toEpochDay(),
            ),
        )
    }
}

/** "Hit & Run" redesign (Gate 1b) — [com.fitviet.app.ui.onboarding.OnboardingViewModel]'s own UI
 * state stays index-based against [com.fitviet.app.ui.onboarding.GOAL_OPTIONS]/`LEVEL_OPTIONS`/
 * `SPLIT_OPTIONS] (unchanged screens, both slated for deletion in Gate 2a) — only the boundary to
 * [SettingsEntity]'s now-string columns needs a translation, kept here rather than in the
 * soon-to-be-deleted screens/ViewModel. `levelIndexToStored` is a straight 1:1 rename
 * (`LEVEL_OPTIONS`already lists BEGINNER/INTERMEDIATE/ADVANCED in [ExerciseDifficulty]'s own
 * declaration order). `goalIndexToStored`/`storedToGoalIndex` and `splitIndexToStored`/
 * `storedToSplitIndex` preserve today's exact index→enum behavior byte-for-byte (including
 * `SPLIT_OPTIONS`' pre-existing index-2/index-3 collapse to the same [SplitTemplate.BRO_SPLIT] —
 * not a regression introduced here, just carried forward under the new representation). */
private fun goalIndexToStored(index: Int): String = when (index) {
    0 -> NutritionGoal.BULK
    1 -> NutritionGoal.CUT
    else -> NutritionGoal.MAINTAIN
}.name

private fun storedToGoalIndex(value: String): Int = when (NutritionGoal.fromStored(value)) {
    NutritionGoal.BULK -> 0
    NutritionGoal.CUT -> 1
    NutritionGoal.MAINTAIN -> 2
}

private fun levelIndexToStored(index: Int): String =
    ExerciseDifficulty.entries.getOrElse(index) { ExerciseDifficulty.BEGINNER }.name

private fun storedToLevelIndex(value: String): Int =
    ExerciseDifficulty.entries.indexOf(ExerciseDifficulty.entries.find { it.name == value } ?: ExerciseDifficulty.BEGINNER)
        .coerceAtLeast(0)

private fun splitIndexToStored(index: Int): String = when (index) {
    0 -> SplitTemplate.PPL
    1 -> SplitTemplate.UPPER_LOWER
    4 -> SplitTemplate.FULL_BODY
    // Index 2 ("Ngực + tay sau / Xô + tay trước", a paired-muscle split) and index 3 (classic bro
    // split) both fall here — SPLIT_OPTIONS has no distinct generator template for index 2, so it
    // already collapsed onto BRO_SPLIT before this gate; preserved exactly, not newly introduced.
    else -> SplitTemplate.BRO_SPLIT
}.name

private fun storedToSplitIndex(value: String): Int = when (SplitTemplate.entries.find { it.name == value }) {
    SplitTemplate.PPL -> 0
    SplitTemplate.UPPER_LOWER -> 1
    SplitTemplate.FULL_BODY -> 4
    // BRO_SPLIT (and PHUL/CUSTOM, unreachable from onboarding) redisplay as index 3 — the literal
    // "classic bro split" option, the closer of the two indices that collapse onto this template.
    //
    // Known accepted limitation (Phase 1 review): a user who specifically picks index 2 ("Ngực +
    // tay sau / Xô + tay trước") and later re-opens onboarding sees index 3 pre-highlighted
    // instead — a real redisplay round-trip loss this string-based storage introduced (the old raw
    // `Int` column round-tripped every index perfectly, since it stored the index itself with no
    // enum-collapse step at all; only *generation* collapsed 2/3 onto one template, never storage).
    // Not fixed here: `SplitTemplate` has no separate enum value for index 2's pairing, and adding
    // one purely to fix a cosmetic pre-highlight on a screen [OnboardingViewModel]'s own doc already
    // marks "slated for deletion in Gate 2a" isn't worth it — this whole index-based UI goes away
    // within a few gates of the same redesign that introduced the loss.
    else -> 3
}
