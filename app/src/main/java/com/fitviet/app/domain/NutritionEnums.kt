package com.fitviet.app.domain

/**
 * Stable classification codes for the real Nutrition backend (Recipe/MealPlan), same pattern as
 * [MonthlyPlanEnums.kt][TrainingGoal] — plain enum `.name` stored as a raw String column (no Room
 * TypeConverter), kept in this Room/Compose-free package.
 */

/** A nutrition-specific goal taxonomy — deliberately separate from [TrainingGoal] (a workout-plan
 * concept). */
enum class NutritionGoal {
    CUT, MAINTAIN, BULK;

    companion object {
        /** Best-effort default for the create-plan wizard's initial goal-card selection — the
         * user still picks explicitly on that screen, so a wrong guess costs one tap, not a
         * data-integrity problem. [selectedGoal] is
         * [com.fitviet.app.data.local.entity.SettingsEntity.selectedGoal] (an index into
         * onboarding's 4 goal options: 0=Tăng cơ, 1=Giảm mỡ, 2=Sức mạnh, 3=Sức khỏe — a
         * workout-domain choice with no exact nutrition-goal equivalent). Out-of-range indices
         * default to MAINTAIN. */
        fun fromOnboardingIndex(selectedGoal: Int): NutritionGoal = when (selectedGoal) {
            0 -> BULK
            1 -> CUT
            2 -> MAINTAIN
            3 -> MAINTAIN
            else -> MAINTAIN
        }
    }
}

/** Same history-preserving lifecycle as [MonthlyPlanStatus] (minus `ARCHIVED`, which that enum
 * never actually uses either) — a new [com.fitviet.app.data.local.entity.UserMealPlanEntity]
 * flips the previous ACTIVE row to SUPERSEDED rather than deleting it. */
enum class MealPlanStatus { ACTIVE, SUPERSEDED }

/** A recipe's curated multiplier-set variants (see
 * [com.fitviet.app.data.local.entity.RecipeVariantEntity]'s doc comment for why this is a flat
 * multiplier rather than a second ingredient list — a deliberate v1 approximation; real
 * per-ingredient substitution per variant is a future-work item, not in this phase's scope). */
enum class RecipeVariantType { STANDARD, CUT, HIGH_PROTEIN, BULK }

enum class RecipeDifficulty { EASY, MEDIUM, HARD }
