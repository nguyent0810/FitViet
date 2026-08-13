package com.fitviet.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.ui.common.LoadingSkeleton
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
    // Redesign Gate 1c — fires once when the no-arg entry point resolves to "no active plan"
    // (WorkoutPhase.AwaitingPlanGeneration). This ViewModel has no navigation of its own, so the
    // caller (FitVietNavHost) supplies where "go set one up" actually goes (Quick Generate).
    onNoPlan: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundPage)) {
        if (uiState.isLoading) {
            LoadingSkeleton(modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp))
            return@Column
        }

        if (uiState.phase != WorkoutPhase.SessionFinished &&
            uiState.phase !is WorkoutPhase.NoSessionToday &&
            uiState.phase != WorkoutPhase.AwaitingPlanGeneration
        ) {
            WorkoutHeader(uiState = uiState, onReset = { showResetConfirm = true }, onExit = { showExitConfirm = true })
        }

        when (val phase = uiState.phase) {
            is WorkoutPhase.NoSessionToday -> NoSessionTodayContent(reason = phase.reason, onExit = onFinishToHome)
            WorkoutPhase.AwaitingPlanGeneration -> {
                // Navigates away immediately (see onNoPlan's own doc) — this content only ever
                // renders for the handful of frames between resolution and the nav actually
                // landing, but a loading skeleton beats a blank frame in that window.
                LoadingSkeleton(modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp))
                LaunchedEffect(Unit) { onNoPlan() }
            }
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
            WorkoutPhase.SessionFinished -> SessionFinishedContent(uiState = uiState, onBackToHome = onFinishToHome, onShare = viewModel::shareToCommunity)
        }
    }

    if (uiState.isTechniquePickerOpen) {
        TechniquePickerSheet(
            selected = uiState.selectedTechnique,
            onSelect = viewModel::selectTechnique,
            onDismiss = viewModel::closeTechniquePicker,
        )
    }

    // Neither dialog colors its confirm button Danger — that token is reserved for genuinely
    // permanent data loss (Settings' full-app reset). Losing an in-progress, unsaved workout is
    // real but recoverable (the user can just start again), the same severity tier as the
    // undecorated delete confirms elsewhere (RemindersScreen, MeasurementHistorySheet).
    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text(text = stringResource(R.string.workout_exit_confirm_title)) },
            text = { Text(text = stringResource(R.string.workout_exit_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { showExitConfirm = false; onFinishToHome() }) {
                    Text(text = stringResource(R.string.workout_exit_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text(text = stringResource(R.string.workout_exit_confirm_cancel))
                }
            },
        )
    }

    // Added alongside the exit dialog above: "Làm lại" discards the same in-progress logged sets
    // and totals as "Thoát" does, and sits pixel-adjacent to it in WorkoutHeader with identical
    // chrome — leaving it a single unconfirmed tap while its neighbor gained a confirm dialog
    // protected the wrong button from an accidental double-tap.
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(text = stringResource(R.string.workout_reset_confirm_title)) },
            text = { Text(text = stringResource(R.string.workout_reset_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { showResetConfirm = false; viewModel.resetWorkout() }) {
                    Text(text = stringResource(R.string.workout_reset_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(text = stringResource(R.string.workout_exit_confirm_cancel))
                }
            },
        )
    }
}

@Composable
private fun WorkoutHeader(uiState: WorkoutUiState, onReset: () -> Unit, onExit: () -> Unit) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(SurfaceCard)
                    .border(1.dp, CardBorder, MaterialTheme.shapes.small)
                    .clickable(onClick = onExit)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.workout_exit), style = MaterialTheme.typography.labelMedium, color = TextFaint)
            }
            Box(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(SurfaceCard)
                    .border(1.dp, CardBorder, MaterialTheme.shapes.small)
                    .clickable(onClick = onReset)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.workout_reset), style = MaterialTheme.typography.labelMedium, color = TextFaint)
            }
        }
    }
}
