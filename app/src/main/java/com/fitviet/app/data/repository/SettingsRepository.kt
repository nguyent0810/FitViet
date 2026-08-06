package com.fitviet.app.data.repository

import androidx.room.withTransaction
import com.fitviet.app.data.local.FitVietDatabase
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
) {
    /**
     * The destructive "Đặt lại ứng dụng" (reset app) action.
     *
     * **Clears** (the user's own logged data): all workout sessions and their set logs (cascades
     * via `ForeignKey.CASCADE`), all logged meals, all body measurements, and every setting —
     * including `onboardingCompleted`, so the app returns to onboarding on next launch, and the
     * display name/avatar/language/unit/widget-visibility choices all revert to their defaults.
     *
     * **Does NOT clear** (this app's static content library, not user data): `programs`,
     * `program_days`, `program_exercises`, `exercises`, `foods`. `community_posts` is also left
     * untouched for now — today those rows are only ever the 3 seeded demo posts (no real
     * user-post-creation flow exists yet); revisit this exclusion once Gate 40/41 add one.
     *
     * All-or-nothing via a transaction — a partial reset (e.g. sessions cleared but settings still
     * showing the old state) would be worse than either fully succeeding or fully failing.
     */
    suspend fun resetAppData() {
        database.withTransaction {
            workoutSessionDao.deleteAll()
            mealDao.deleteAll()
            measurementDao.deleteAll()
            settingsDao.upsert(SettingsEntity())
        }
    }
}
