package com.fitviet.app.data.repository

import androidx.room.withTransaction
import com.fitviet.app.data.local.FitVietDatabase
import com.fitviet.app.data.local.dao.CommunityPostDao
import com.fitviet.app.data.local.dao.MealDao
import com.fitviet.app.data.local.dao.MeasurementDao
import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.dao.WorkoutSessionDao
import com.fitviet.app.data.local.entity.SettingsEntity

/** Backs the new `ui/settings/SettingsScreen.kt` (feature #6, Gate 37) — a distinct feature area
 * from [ProfileRepository], which owns profile-identity/measurement concerns. */
class SettingsRepository(
    private val database: FitVietDatabase,
    private val workoutSessionDao: WorkoutSessionDao,
    private val mealDao: MealDao,
    private val measurementDao: MeasurementDao,
    private val settingsDao: SettingsDao,
    private val communityPostDao: CommunityPostDao,
) {
    /**
     * The destructive "Đặt lại ứng dụng" (reset app) action.
     *
     * **Clears** (the user's own logged data): all workout sessions and their set logs (cascades
     * via `ForeignKey.CASCADE`), all logged meals, all body measurements, every setting — including
     * `onboardingCompleted`, so the app returns to onboarding on next launch, and the display
     * name/avatar/language/unit/widget-visibility choices all revert to their defaults — and (Gate
     * 6b) `community_posts`: the previous exclusion here said to "revisit once Gate 40/41 add a
     * real user-post-creation flow," which they did, so a reset now also clears the user's own
     * shared workouts (and any Gate 6c composer text) rather than leaving them snapshotted under a
     * display name the reset just erased. Safe to clear unconditionally including the 3 seeded demo
     * posts — `DatabaseSeeder.seedMissingCommunityPosts()` re-inserts them on next launch.
     *
     * **Does NOT clear** (this app's static content library, not user data): `programs`,
     * `program_days`, `program_exercises`, `exercises`, `foods`.
     *
     * All-or-nothing via a transaction — a partial reset (e.g. sessions cleared but settings still
     * showing the old state) would be worse than either fully succeeding or fully failing.
     */
    suspend fun resetAppData() {
        database.withTransaction {
            workoutSessionDao.deleteAll()
            mealDao.deleteAll()
            measurementDao.deleteAll()
            communityPostDao.deleteAll()
            settingsDao.upsert(SettingsEntity())
        }
    }
}
