package com.fitviet.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fitviet.app.data.local.dao.CommunityPostDao
import com.fitviet.app.data.local.dao.ExerciseDao
import com.fitviet.app.data.local.dao.FoodDao
import com.fitviet.app.data.local.dao.MealDao
import com.fitviet.app.data.local.dao.MeasurementDao
import com.fitviet.app.data.local.dao.MonthlyPlanDao
import com.fitviet.app.data.local.dao.MonthlyPlanDayDao
import com.fitviet.app.data.local.dao.MonthlyPlanExerciseDao
import com.fitviet.app.data.local.dao.MonthlyPlanWeekDao
import com.fitviet.app.data.local.dao.ProgramDao
import com.fitviet.app.data.local.dao.ProgramDayDao
import com.fitviet.app.data.local.dao.ProgramExerciseDao
import com.fitviet.app.data.local.dao.ReminderDao
import com.fitviet.app.data.local.dao.SetLogDao
import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.dao.SplitTemplateDao
import com.fitviet.app.data.local.dao.WorkoutSessionDao
import com.fitviet.app.data.local.entity.CommunityPostEntity
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.entity.FoodEntity
import com.fitviet.app.data.local.entity.MealEntity
import com.fitviet.app.data.local.entity.MeasurementEntity
import com.fitviet.app.data.local.entity.MonthlyPlanDayEntity
import com.fitviet.app.data.local.entity.MonthlyPlanEntity
import com.fitviet.app.data.local.entity.MonthlyPlanExerciseEntity
import com.fitviet.app.data.local.entity.MonthlyPlanWeekEntity
import com.fitviet.app.data.local.entity.ProgramDayEntity
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.data.local.entity.ProgramExerciseEntity
import com.fitviet.app.data.local.entity.ReminderEntity
import com.fitviet.app.data.local.entity.SetLogEntity
import com.fitviet.app.data.local.entity.SettingsEntity
import com.fitviet.app.data.local.entity.SplitTemplateEntity
import com.fitviet.app.data.local.entity.WorkoutSessionEntity

@Database(
    entities = [
        ProgramEntity::class,
        ProgramDayEntity::class,
        ProgramExerciseEntity::class,
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        SetLogEntity::class,
        MealEntity::class,
        MeasurementEntity::class,
        SettingsEntity::class,
        CommunityPostEntity::class,
        FoodEntity::class,
        ReminderEntity::class,
        MonthlyPlanEntity::class,
        MonthlyPlanWeekEntity::class,
        MonthlyPlanDayEntity::class,
        MonthlyPlanExerciseEntity::class,
        SplitTemplateEntity::class,
    ],
    // Bump this on every schema change, pre-release — see fallbackToDestructiveMigration() below.
    // Gate 15 raised this from 1 to 2 (new program_days/program_exercises tables, new columns on
    // exercises/settings): a real device already running an earlier version's build has a stored
    // schema identity hash that no longer matches, which Room refuses to open without either this
    // version bump or an explicit Migration. There are no shipped installs to preserve data for
    // yet, so destructive fallback (wipe + reseed from scratch) is the correct, simplest policy —
    // not a real Migration, which would be premature complexity pre-release.
    // Gate 23 raised this from 3 to 4: the table shape didn't change, but MuscleGroup's enum
    // constants were renamed/split (SHOULDERS->DELTOIDS, ARMS->BICEPS/TRICEPS, CORE->ABS) and
    // exercises.muscleGroupCode stores the enum's raw name string with no TypeConverter — a device
    // that already seeded the old codes needs a forced wipe+reseed or those rows would silently
    // drop out of the muscle-workload chart (unrecognized codes are excluded, not migrated).
    // Gate 19 raised this from 2 to 3: new `showRecommendationCard`/`showMuscleBalanceCard`/
    // `showNutritionCard` columns on `settings` (per-widget dashboard visibility toggles).
    // Gate 25 raised this from 4 to 5: new `foods` table, plus a new `difficultyCode` column on
    // `exercises` (same rationale as Gate 23 — a device already running an earlier version needs a
    // forced recreate to pick up the new column at all, Room won't add it in place).
    // Gate 35 raised this from 5 to 6: new `displayName`/`avatarId` columns on `settings`.
    // Gate 38 raised this from 6 to 7: new `reminders` table.
    // Gate 39 raised this from 7 to 8: new `selectedDaysPerWeek` column on `settings`.
    // Gate 40 raised this from 8 to 9: 5 new nullable workout-share columns on `community_posts`.
    // Gate 44 raised this from 9 to 10: new `involvementPercents` column on `exercises` (reuses
    // Gate 38's List<Int> Converters pair — no new converter needed).
    // Gate 47 raised this from 10 to 11: new nullable `supersetGroup` column on
    // `program_exercises`.
    // Gate 48 raised this from 11 to 12: new `hasSeenSupersetHint` column on `settings`.
    // Post-Gate-48 audit pass raised this from 12 to 13: new `label` column on `reminders`
    // (missing per the Tier 1 mockup — a workout-day description shown under the time/days row).
    // "Hit & Run" (Gate 63+) raised this from 13 to 14: 5 new tables (`monthly_plans`,
    // `monthly_plan_weeks`, `monthly_plan_days`, `monthly_plan_exercises`, `split_templates`) for
    // the one-tap generated multi-week plan, plus new `monthlyPlanDayId` column on
    // `workout_sessions` and new `activeMonthlyPlanId`/`skipWorkoutPreview` columns on `settings`.
    version = 14,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class FitVietDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun programDayDao(): ProgramDayDao
    abstract fun programExerciseDao(): ProgramExerciseDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun setLogDao(): SetLogDao
    abstract fun mealDao(): MealDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun settingsDao(): SettingsDao
    abstract fun communityPostDao(): CommunityPostDao
    abstract fun foodDao(): FoodDao
    abstract fun reminderDao(): ReminderDao
    abstract fun monthlyPlanDao(): MonthlyPlanDao
    abstract fun monthlyPlanWeekDao(): MonthlyPlanWeekDao
    abstract fun monthlyPlanDayDao(): MonthlyPlanDayDao
    abstract fun monthlyPlanExerciseDao(): MonthlyPlanExerciseDao
    abstract fun splitTemplateDao(): SplitTemplateDao

    companion object {
        @Volatile private var instance: FitVietDatabase? = null

        fun getInstance(context: Context): FitVietDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FitVietDatabase::class.java,
                    "fitviet.db",
                )
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
