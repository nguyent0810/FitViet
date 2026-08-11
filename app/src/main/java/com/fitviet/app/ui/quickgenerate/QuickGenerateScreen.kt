package com.fitviet.app.ui.quickgenerate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.domain.ExerciseDifficulty
import com.fitviet.app.domain.SplitTemplate
import com.fitviet.app.domain.TrainingGoal
import com.fitviet.app.ui.onboarding.LEVEL_OPTIONS
import com.fitviet.app.ui.onboarding.LevelChip
import com.fitviet.app.ui.onboarding.OnboardingPrimaryButton
import com.fitviet.app.ui.onboarding.PillChip
import com.fitviet.app.ui.onboarding.SelectionCard
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextMuted

private data class QuickGenerateGoalOption(val goal: TrainingGoal, val titleRes: Int, val subRes: Int)

private val GOAL_CHOICES = listOf(
    QuickGenerateGoalOption(TrainingGoal.HYPERTROPHY, R.string.goal_muscle_gain_title, R.string.goal_muscle_gain_sub),
    QuickGenerateGoalOption(TrainingGoal.STRENGTH, R.string.goal_strength_title, R.string.goal_strength_sub),
    QuickGenerateGoalOption(TrainingGoal.GENERAL_FITNESS, R.string.goal_health_title, R.string.goal_health_sub),
)

private data class QuickGenerateSplitOption(val split: SplitTemplate, val titleRes: Int, val subRes: Int)

// CUSTOM is deliberately excluded — its authoring UI isn't part of this pass (see the "Hit & Run"
// plan's scope note); it's only reachable via a future dedicated flow, not this quick picker.
private val SPLIT_CHOICES = listOf(
    QuickGenerateSplitOption(SplitTemplate.PPL, R.string.split_ppl_title, R.string.split_ppl_sub),
    QuickGenerateSplitOption(SplitTemplate.UPPER_LOWER, R.string.split_upper_lower_title, R.string.split_upper_lower_sub),
    QuickGenerateSplitOption(SplitTemplate.FULL_BODY, R.string.split_fullbody_title, R.string.split_fullbody_sub),
    QuickGenerateSplitOption(SplitTemplate.BRO_SPLIT, R.string.split_bro_title, R.string.split_bro_sub),
    QuickGenerateSplitOption(SplitTemplate.PHUL, R.string.split_phul_title, R.string.split_phul_sub),
)

private val DAYS_PER_WEEK_OPTIONS = 2..6

@Composable
fun QuickGenerateScreen(
    viewModel: QuickGenerateViewModel,
    onBack: () -> Unit,
    // Invoked on tap; the actual [QuickGenerateViewModel.generate] suspend call + post-navigation
    // is owned by the caller (same "await the write, then navigate" pattern
    // FitVietNavHost.kt already uses for onboarding's completeOnboarding()), not by this screen.
    onGenerateClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPage),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            BackRow(onBack = onBack)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = stringResource(R.string.quick_generate_title), style = MaterialTheme.typography.displayLarge)
                Text(text = stringResource(R.string.quick_generate_subtitle), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.quick_generate_goal_label), style = MaterialTheme.typography.labelLarge, color = TextMuted)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GOAL_CHOICES.forEach { option ->
                        SelectionCard(
                            title = stringResource(option.titleRes),
                            subtitle = stringResource(option.subRes),
                            selected = uiState.goal == option.goal,
                            onClick = { viewModel.selectGoal(option.goal) },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.quick_generate_level_label), style = MaterialTheme.typography.labelLarge, color = TextMuted)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExerciseDifficulty.entries.forEachIndexed { index, level ->
                        LevelChip(
                            label = stringResource(LEVEL_OPTIONS[index]),
                            selected = uiState.level == level,
                            onClick = { viewModel.selectLevel(level) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.quick_generate_days_label), style = MaterialTheme.typography.labelLarge, color = TextMuted)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DAYS_PER_WEEK_OPTIONS.forEach { days ->
                        PillChip(
                            label = days.toString(),
                            selected = uiState.daysPerWeek == days,
                            onClick = { viewModel.selectDaysPerWeek(days) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(text = stringResource(R.string.quick_generate_split_label), style = MaterialTheme.typography.labelLarge, color = TextMuted)
                SPLIT_CHOICES.forEach { option ->
                    SelectionCard(
                        title = stringResource(option.titleRes),
                        subtitle = stringResource(option.subRes),
                        selected = uiState.splitTemplate == option.split,
                        onClick = { viewModel.selectSplit(option.split) },
                        horizontalPadding = 16.dp,
                        verticalPadding = 14.dp,
                    )
                }
            }
        }

        OnboardingPrimaryButton(
            text = stringResource(if (uiState.isGenerating) R.string.quick_generate_generating else R.string.quick_generate_cta),
            onClick = { if (!uiState.isLoading && !uiState.isGenerating) onGenerateClick() },
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 14.dp),
        )
    }
}

/** Same look as [com.fitviet.app.ui.workout.WorkoutPreviewScreen]'s own private back affordance —
 * this screen isn't part of the onboarding graph (which relies on the system back gesture alone),
 * so it needs an explicit way back the way other secondary screens reached by tapping into already do. */
@Composable
private fun BackRow(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(MaterialTheme.shapes.small)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.small)
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "‹", style = MaterialTheme.typography.titleMedium, color = TextMuted)
    }
}
