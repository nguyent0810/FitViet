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
        /** "Hit & Run" redesign (Gate 1b) — [com.fitviet.app.data.local.entity.SettingsEntity
         * .selectedGoal] stores this enum's `.name` directly (onboarding's 3 pills — Tăng cơ/Giảm
         * mỡ/Khỏe mạnh — now map 1:1 onto BULK/CUT/MAINTAIN, replacing the old 4-option positional
         * index this function used to decode). Safe-default parse, matching this codebase's other
         * "unrecognized stored code degrades gracefully" call sites (`ExerciseDifficulty.entries
         * .getOrElse`, `EquipmentProfiles`) rather than a bare `valueOf` that would throw on a
         * corrupt/pre-migration value. */
        fun fromStored(value: String?): NutritionGoal = entries.find { it.name == value } ?: MAINTAIN
    }
}

/** Redesign Gate 2a — the one-time seed from onboarding's [NutritionGoal] pill to a real
 * [TrainingGoal] for the very first generate (before any [com.fitviet.app.data.local.entity
 * .MonthlyPlanEntity] exists to read its own goal back from). Moved here from
 * [com.fitviet.app.ui.quickgenerate.QuickGenerateViewModel] (formerly a private `trainingGoalFor`)
 * so onboarding's own submit — which now generates the first plan directly, matching the mock's
 * "Tạo plan & vào tập →" single-tap CTA — can share the exact same mapping instead of duplicating
 * it. Cutting is a nutrition strategy, not a distinct set/rep prescription, so both CUT and
 * MAINTAIN seed the same GENERAL_FITNESS training goal — deliberately, not a rounding accident. */
fun NutritionGoal.toInitialTrainingGoal(): TrainingGoal = when (this) {
    NutritionGoal.BULK -> TrainingGoal.HYPERTROPHY
    NutritionGoal.CUT, NutritionGoal.MAINTAIN -> TrainingGoal.GENERAL_FITNESS
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
