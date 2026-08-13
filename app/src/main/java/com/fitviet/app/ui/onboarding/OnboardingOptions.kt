package com.fitviet.app.ui.onboarding

import androidx.annotation.StringRes
import com.fitviet.app.R

data class GoalOption(@StringRes val titleRes: Int, @StringRes val subRes: Int)

/** "Hit & Run" redesign (Gate 1b) — trimmed from 4 entries to 3, matching the redesign mock's own
 * onboarding pills (Tăng cơ/Giảm mỡ/Khỏe mạnh — no "Sức mạnh") and mapping 1:1 onto
 * [com.fitviet.app.domain.NutritionGoal] (BULK/CUT/MAINTAIN) by position, so `SettingsEntity
 * .selectedGoal`'s round-trip through this list is exact, not lossy. "Sức mạnh"/`STRENGTH` stays
 * reachable from [com.fitviet.app.ui.quickgenerate.QuickGenerateScreen]'s own separate goal picker
 * (a `TrainingGoal`, not a `NutritionGoal`, choice) and from the sample-program list once a program
 * carries `STRENGTH` — this list is onboarding-only. */
val GOAL_OPTIONS = listOf(
    GoalOption(R.string.goal_muscle_gain_title, R.string.goal_muscle_gain_sub),
    GoalOption(R.string.goal_fat_loss_title, R.string.goal_fat_loss_sub),
    GoalOption(R.string.goal_health_title, R.string.goal_health_sub),
)

val LEVEL_OPTIONS = listOf(R.string.level_beginner, R.string.level_intermediate, R.string.level_advanced)

/** [recommendedFor] holds the days/week counts (2..6) this split is a good fit for — drives the
 * "GỢI Ý" badge on [SplitScreen] once the user picks a day count via `DaysPerWeekRow`. */
data class SplitOption(@StringRes val titleRes: Int, @StringRes val subRes: Int, val recommendedFor: Set<Int>)

val SPLIT_OPTIONS = listOf(
    // "hợp 3 hoặc 6 buổi/tuần" — copy states this explicitly.
    SplitOption(R.string.split_ppl_title, R.string.split_ppl_sub, recommendedFor = setOf(3, 6)),
    // "mỗi nhóm cơ 2 lần/tuần với 4 buổi" — copy states this explicitly.
    SplitOption(R.string.split_upper_lower_title, R.string.split_upper_lower_sub, recommendedFor = setOf(4)),
    // Paired-muscle sessions (chest+triceps / back+biceps) — no day count in the copy. Reviewed
    // (Gate 39 independent review): the copy's "tiết kiệm thời gian" (saves time) framing could
    // read as favoring lower frequency too, not just Upper-Lower's 4-day band — widened down to
    // include 3 so the range covers both readings rather than picking one exclusively.
    SplitOption(R.string.split_chest_back_title, R.string.split_chest_back_sub, recommendedFor = setOf(3, 4, 5)),
    // Classic bro split (5-6 distinct muscle-group days) needs the higher end of the range.
    SplitOption(R.string.split_bro_title, R.string.split_bro_sub, recommendedFor = setOf(5, 6)),
    // "hợp người mới, 2–3 buổi/tuần" — copy states this explicitly.
    SplitOption(R.string.split_fullbody_title, R.string.split_fullbody_sub, recommendedFor = setOf(2, 3)),
)
