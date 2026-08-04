package com.fitviet.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.util.formatWeight

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onFinishToHome: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundPage)) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
            return@Column
        }

        if (uiState.phase != WorkoutPhase.SessionFinished) {
            WorkoutHeader(uiState = uiState, onReset = viewModel::resetWorkout)
        }

        when (uiState.phase) {
            WorkoutPhase.StraightLog -> StraightLogContent(uiState = uiState, viewModel = viewModel)
            WorkoutPhase.StraightRest -> {
                val straightBlock = (uiState.currentBlock as? WorkoutBlockPlan.Straight)?.plan
                val planned = straightBlock?.plannedSets?.getOrNull(uiState.currentSetIndex)
                val nextLabel = if (planned != null) {
                    stringResource(R.string.workout_rest_next, uiState.currentSetIndex + 1, formatWeight(planned.weightKg), planned.reps)
                } else {
                    ""
                }
                RestContent(
                    title = stringResource(R.string.workout_rest_title).uppercase(),
                    secondsRemaining = uiState.restSecondsRemaining,
                    nextLabel = nextLabel,
                    onAddRest = viewModel::addRest,
                    onSkipRest = viewModel::skipRest,
                )
            }
            WorkoutPhase.StraightBlockDone -> StraightBlockDoneContent(uiState = uiState, viewModel = viewModel)
            WorkoutPhase.SupersetWork -> SupersetWorkContent(uiState = uiState, viewModel = viewModel)
            WorkoutPhase.SupersetRest -> {
                val supersetBlock = (uiState.currentBlock as? WorkoutBlockPlan.Superset)?.plan
                val roundFraction = supersetBlock?.let {
                    stringResource(R.string.superset_round_fraction, uiState.supersetRound.coerceAtMost(it.totalRounds), it.totalRounds)
                } ?: ""
                val nextLabel = supersetBlock?.let {
                    stringResource(R.string.superset_rest_next_round, roundFraction, it.exerciseA.nameVi)
                } ?: ""
                RestContent(
                    title = stringResource(R.string.superset_rest_title, uiState.supersetRound.coerceAtMost(supersetBlock?.totalRounds ?: 1)).uppercase(),
                    secondsRemaining = uiState.supersetRestSecondsRemaining,
                    nextLabel = nextLabel,
                    onAddRest = viewModel::addSupersetRest,
                    onSkipRest = viewModel::skipSupersetRest,
                    countdownFontSize = 80.sp,
                )
            }
            WorkoutPhase.SupersetBlockDone -> SupersetBlockDoneContent(uiState = uiState, viewModel = viewModel)
            WorkoutPhase.SessionFinished -> SessionFinishedContent(uiState = uiState, onBackToHome = onFinishToHome)
        }
    }

    if (uiState.isTechniquePickerOpen) {
        TechniquePickerSheet(
            selected = uiState.selectedTechnique,
            onSelect = viewModel::selectTechnique,
            onDismiss = viewModel::closeTechniquePicker,
        )
    }
}

@Composable
private fun WorkoutHeader(uiState: WorkoutUiState, onReset: () -> Unit) {
    val exerciseName = when (val block = uiState.currentBlock) {
        is WorkoutBlockPlan.Straight -> block.plan.exercise.nameVi
        is WorkoutBlockPlan.Superset -> stringResource(R.string.superset_title)
        null -> ""
    }
    Row(
        modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(
                text = stringResource(
                    R.string.workout_exercise_progress,
                    uiState.dayLabel,
                    uiState.currentBlockIndex + 1,
                    uiState.blocks.size,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            Text(text = exerciseName, style = MaterialTheme.typography.headlineSmall)
        }
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(SurfaceCard)
                .border(1.dp, CardBorder, MaterialTheme.shapes.small)
                .clickable(onClick = onReset)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(text = stringResource(R.string.workout_reset), style = MaterialTheme.typography.labelMedium, color = TextFaint)
        }
    }
}
