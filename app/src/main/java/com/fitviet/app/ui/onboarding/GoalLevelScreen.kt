package com.fitviet.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitviet.app.R
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted

// Prototype gaps for this screen: 22px between sections, 10px between goal cards (README §1a).
private val SECTION_GAP = 22.dp
private val GOAL_CARD_GAP = 10.dp

@Composable
fun GoalLevelScreen(
    viewModel: OnboardingViewModel = viewModel(),
    onContinue: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
        ) {
            OnboardingProgressBar(totalSteps = 3, filledCount = 1)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_goal_title),
                    style = MaterialTheme.typography.displayLarge,
                )
                Text(
                    text = stringResource(R.string.onboarding_goal_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(GOAL_CARD_GAP)) {
                GOAL_OPTIONS.forEachIndexed { index, option ->
                    SelectionCard(
                        title = stringResource(option.titleRes),
                        subtitle = stringResource(option.subRes),
                        selected = uiState.selectedGoal == index,
                        onClick = { viewModel.selectGoal(index) },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_level_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LEVEL_OPTIONS.forEachIndexed { index, labelRes ->
                        LevelChip(
                            label = stringResource(labelRes),
                            selected = uiState.selectedLevel == index,
                            onClick = { viewModel.selectLevel(index) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
        ) {
            OnboardingPrimaryButton(text = stringResource(R.string.onboarding_continue), onClick = onContinue)
            Text(
                text = stringResource(R.string.onboarding_footer),
                style = MaterialTheme.typography.labelMedium,
                color = TextFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
        }
    }
}
