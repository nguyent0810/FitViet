package com.fitviet.app.ui.onboarding

import androidx.annotation.StringRes
import com.fitviet.app.R

data class GoalOption(@StringRes val titleRes: Int, @StringRes val subRes: Int)

val GOAL_OPTIONS = listOf(
    GoalOption(R.string.goal_muscle_gain_title, R.string.goal_muscle_gain_sub),
    GoalOption(R.string.goal_fat_loss_title, R.string.goal_fat_loss_sub),
    GoalOption(R.string.goal_strength_title, R.string.goal_strength_sub),
    GoalOption(R.string.goal_health_title, R.string.goal_health_sub),
)

val LEVEL_OPTIONS = listOf(R.string.level_beginner, R.string.level_intermediate, R.string.level_advanced)

data class SplitOption(@StringRes val titleRes: Int, @StringRes val subRes: Int, val recommended: Boolean)

val SPLIT_OPTIONS = listOf(
    SplitOption(R.string.split_ppl_title, R.string.split_ppl_sub, recommended = true),
    SplitOption(R.string.split_upper_lower_title, R.string.split_upper_lower_sub, recommended = false),
    SplitOption(R.string.split_chest_back_title, R.string.split_chest_back_sub, recommended = false),
    SplitOption(R.string.split_bro_title, R.string.split_bro_sub, recommended = false),
    SplitOption(R.string.split_fullbody_title, R.string.split_fullbody_sub, recommended = false),
)
