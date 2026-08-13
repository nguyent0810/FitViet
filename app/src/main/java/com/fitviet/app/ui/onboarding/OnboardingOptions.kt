package com.fitviet.app.ui.onboarding

// Redesign Gate 2a deleted GoalOption/GOAL_OPTIONS and SplitOption/SPLIT_OPTIONS along with
// GoalLevelScreen/SplitScreen themselves — the new single-screen OnboardingScreen picks goal
// pills directly against com.fitviet.app.domain.NutritionGoal (no positional list needed) and no
// longer asks about split at all (auto-derived from days/week, see OnboardingViewModel). Split
// template is still user-editable elsewhere ([com.fitviet.app.ui.quickgenerate.GenerateSheet]'s
// own picker), which defines its own options against SplitTemplate directly rather than importing
// from here. Redesign Gate 3c dropped the level picker entirely (level is prefilled and no longer
// user-editable there either — see GenerateSheet's own doc), so LEVEL_OPTIONS, which existed only
// for that picker, was deleted along with it.
