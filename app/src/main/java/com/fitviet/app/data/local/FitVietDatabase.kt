package com.fitviet.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fitviet.app.data.local.dao.CommunityPostDao
import com.fitviet.app.data.local.dao.ExerciseDao
import com.fitviet.app.data.local.dao.FavoriteRecipeDao
import com.fitviet.app.data.local.dao.FoodDao
import com.fitviet.app.data.local.dao.MealDao
import com.fitviet.app.data.local.dao.MealPlanDayDao
import com.fitviet.app.data.local.dao.MealPlanMealDao
import com.fitviet.app.data.local.dao.MealPlanTemplateDao
import com.fitviet.app.data.local.dao.MeasurementDao
import com.fitviet.app.data.local.dao.MonthlyPlanDao
import com.fitviet.app.data.local.dao.MonthlyPlanDayDao
import com.fitviet.app.data.local.dao.MonthlyPlanExerciseDao
import com.fitviet.app.data.local.dao.MonthlyPlanWeekDao
import com.fitviet.app.data.local.dao.ProgramDao
import com.fitviet.app.data.local.dao.ProgramDayDao
import com.fitviet.app.data.local.dao.ProgramExerciseDao
import com.fitviet.app.data.local.dao.RecipeDao
import com.fitviet.app.data.local.dao.RecipeIngredientDao
import com.fitviet.app.data.local.dao.RecipeVariantDao
import com.fitviet.app.data.local.dao.ReminderDao
import com.fitviet.app.data.local.dao.SetLogDao
import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.dao.SplitTemplateDao
import com.fitviet.app.data.local.dao.UserMealPlanDao
import com.fitviet.app.data.local.dao.WorkoutSessionDao
import com.fitviet.app.data.local.entity.CommunityPostEntity
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.entity.FavoriteRecipeEntity
import com.fitviet.app.data.local.entity.FoodEntity
import com.fitviet.app.data.local.entity.MealEntity
import com.fitviet.app.data.local.entity.MealPlanDayEntity
import com.fitviet.app.data.local.entity.MealPlanMealEntity
import com.fitviet.app.data.local.entity.MealPlanTemplateEntity
import com.fitviet.app.data.local.entity.MeasurementEntity
import com.fitviet.app.data.local.entity.MonthlyPlanDayEntity
import com.fitviet.app.data.local.entity.MonthlyPlanEntity
import com.fitviet.app.data.local.entity.MonthlyPlanExerciseEntity
import com.fitviet.app.data.local.entity.MonthlyPlanWeekEntity
import com.fitviet.app.data.local.entity.ProgramDayEntity
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.data.local.entity.ProgramExerciseEntity
import com.fitviet.app.data.local.entity.RecipeEntity
import com.fitviet.app.data.local.entity.RecipeIngredientEntity
import com.fitviet.app.data.local.entity.RecipeVariantEntity
import com.fitviet.app.data.local.entity.ReminderEntity
import com.fitviet.app.data.local.entity.SetLogEntity
import com.fitviet.app.data.local.entity.SettingsEntity
import com.fitviet.app.data.local.entity.SplitTemplateEntity
import com.fitviet.app.data.local.entity.UserMealPlanEntity
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
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        RecipeVariantEntity::class,
        MealPlanTemplateEntity::class,
        UserMealPlanEntity::class,
        MealPlanDayEntity::class,
        MealPlanMealEntity::class,
        FavoriteRecipeEntity::class,
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
    // Nutrition Gate B1 raised this from 14 to 15: new `normalizedName`/`servingSizeG`/
    // `servingUnit`/`fiberG`/`tags`/`source` columns on `foods` (real Recipe/MealPlan backend
    // foundation — `foods` becomes the shared ingredient catalog, not just the Handbook's
    // read-only reference list).
    // Nutrition Gate B2 raised this from 15 to 16: 3 new tables (`recipes`, `recipe_ingredients`,
    // `recipe_variants`) — a recipe's nutrition is always computed from its ingredient rows
    // (see RecipeNutritionCalculator), never hardcoded.
    // Nutrition Gate B5 raised this from 16 to 17: new `meal_plan_templates` table — curated
    // starter meal plans stored in the database, not hardcoded in UI.
    // Nutrition Gate B6 raised this from 17 to 18: 4 new tables (`user_meal_plans`,
    // `meal_plan_days`, `meal_plan_meals`, `favorite_recipes`) — a user's saved/generated plan and
    // favorites, no `userId` column (single local user).
    // Gate D4 raised this from 18 to 19: new `lastCelebratedStreakDays` column on `settings`.
    // Gate E7 raised this from 19 to 20: table shape unchanged, but `foods.category`'s seeded
    // VALUES were recategorized from a macro-nutrient taxonomy to real ingredient types — same
    // "content-only but a device already seeded needs a forced wipe+reseed" case as Gate 23's
    // MuscleGroup rename above, since DatabaseSeeder.seedMissingFoods() only backfills by name and
    // never repairs an existing row's category.
    // "Hit & Run" redesign Gate 1b raised this from 20 to 21: `settings.selectedGoal`/`selectedLevel`
    // /`selectedSplit` retyped from positional `Int` indices to each enum's own `.name` string (see
    // SettingsEntity's doc comment — a resized/reordered onboarding option list would otherwise
    // silently reinterpret every already-stored index), plus a new `settings.equipmentProfile`
    // column. Batched as one gate/one wipe per the redesign's own decision record, rather than
    // bumping separately for each schema need the redesign turns out to have.
    // "Hit & Run" redesign Gate 2b raised this from 21 to 22: collapsed the two parallel "active
    // plan" systems into one — dropped `settings.activeProgramId` (ProgramEntity has no more
    // "active" concept, it's read-only generation input now) and `settings.skipWorkoutPreview`
    // (its only two honoring call sites were both retired this gate; the one remaining
    // WorkoutPreview entry point, a program's optional "Xem trước" link, never honored it even
    // before this gate). Also removes `settings`' foreign key to `programs`.
    version = 22,
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
    abstract fun recipeDao(): RecipeDao
    abstract fun recipeIngredientDao(): RecipeIngredientDao
    abstract fun recipeVariantDao(): RecipeVariantDao
    abstract fun mealPlanTemplateDao(): MealPlanTemplateDao
    abstract fun userMealPlanDao(): UserMealPlanDao
    abstract fun mealPlanDayDao(): MealPlanDayDao
    abstract fun mealPlanMealDao(): MealPlanMealDao
    abstract fun favoriteRecipeDao(): FavoriteRecipeDao

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
