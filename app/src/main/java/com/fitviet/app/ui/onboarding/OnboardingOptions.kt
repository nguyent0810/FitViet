package com.fitviet.app.ui.onboarding

import com.fitviet.app.R

// Redesign Gate 2a deleted GoalOption/GOAL_OPTIONS and SplitOption/SPLIT_OPTIONS along with
// GoalLevelScreen/SplitScreen themselves — the new single-screen OnboardingScreen picks goal
// pills directly against com.fitviet.app.domain.NutritionGoal (no positional list needed) and no
// longer asks about split at all (auto-derived from days/week, see OnboardingViewModel). Split
// template is still user-editable elsewhere (QuickGenerateScreen's own picker), which defines its
// own options against SplitTemplate directly rather than importing from here.

/** Still used by [com.fitviet.app.ui.quickgenerate.QuickGenerateScreen]'s level picker — onboarding
 * itself no longer asks about level (see the "Hit & Run" plan's documented level-writer gap). */
val LEVEL_OPTIONS = listOf(R.string.level_beginner, R.string.level_intermediate, R.string.level_advanced)
