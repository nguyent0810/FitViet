package com.fitviet.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.ui.common.LoadingSkeleton
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.util.formatMinutesSeconds
import com.fitviet.app.util.formatWeight

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onFinishToHome: () -> Unit,
    // Redesign Gate 1c — fires once when the no-arg entry point resolves to "no active plan"
    // (WorkoutPhase.AwaitingPlanGeneration). This ViewModel has no navigation of its own, so the
    // caller (FitVietNavHost) supplies where "go set one up" actually goes. Redesign Gate 3c
    // retired the dedicated Quick Generate destination this used to target — see FitVietNavHost's
    // own call site doc for where it lands now.
    onNoPlan: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitConfirm by remember { mutableStateOf(false) }

    // Redesign Gate 4b — HrColors.Bg (#0C0E08), not the legacy BackgroundPage (#0D100E): visually
    // near-identical (see Gate 3d's own finding on this exact pair), but 4b is the gate that
    // introduces HrColors.BgDeep for the rest/finished screens, so the outer container should
    // match rather than leave already-migrated Hr surfaces sitting on the old page background.
    Column(modifier = Modifier.fillMaxSize().background(HrColors.Bg)) {
        if (uiState.isLoading) {
            LoadingSkeleton(modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp))
            return@Column
        }

        if (uiState.phase != WorkoutPhase.SessionFinished &&
            uiState.phase !is WorkoutPhase.NoSessionToday &&
            uiState.phase != WorkoutPhase.AwaitingPlanGeneration
        ) {
            WorkoutHeader(uiState = uiState, onExit = { showExitConfirm = true })
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
                // Redesign Gate 4a-i — a non-null pendingNextBlockIndex means this rest is the
                // inter-exercise hand-off the old StraightBlockDone interstitial used to own (see
                // WorkoutViewModel.completeCurrentSet's own doc): uiState.currentBlock/currentSetIndex
                // still describe the block that JUST finished, not what's coming up, so the label
                // must read from the pending block instead. Null means the ordinary same-exercise,
                // next-set rest, unchanged from before this gate.
                val pendingBlock = uiState.pendingNextBlockIndex?.let { uiState.blocks.getOrNull(it) }
                val nextLabel = if (pendingBlock != null) {
                    when (pendingBlock) {
                        is WorkoutBlockPlan.Straight -> {
                            val firstSet = pendingBlock.plan.plannedSets.first()
                            stringResource(
                                R.string.workout_rest_next_exercise,
                                pendingBlock.plan.exercise.nameVi,
                                formatWeight(firstSet.weightKg),
                                firstSet.reps,
                            )
                        }
                        is WorkoutBlockPlan.Superset -> stringResource(R.string.workout_next_exercise, exerciseLabelFor(pendingBlock))
                    }
                } else {
                    val straightBlock = (uiState.currentBlock as? WorkoutBlockPlan.Straight)?.plan
                    val planned = straightBlock?.plannedSets?.getOrNull(uiState.currentSetIndex)
                    if (planned != null) {
                        stringResource(R.string.workout_rest_next, uiState.currentSetIndex + 1, formatWeight(planned.weightKg), planned.reps)
                    } else {
                        ""
                    }
                }
                RestContent(
                    title = stringResource(R.string.workout_rest_title).uppercase(),
                    secondsRemaining = uiState.restSecondsRemaining,
                    nextLabel = nextLabel,
                    onAddRest = viewModel::addRest,
                    onSkipRest = viewModel::skipRest,
                )
            }
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
            WorkoutPhase.SessionFinished -> SessionFinishedContent(uiState = uiState, onBackToHome = onFinishToHome, onShare = viewModel::openShareComposer)
        }
    }

    if (uiState.isTechniquePickerOpen) {
        TechniquePickerSheet(
            selected = uiState.selectedTechnique,
            onSelect = viewModel::selectTechnique,
            onDismiss = viewModel::closeTechniquePicker,
        )
    }

    if (uiState.isShareComposerOpen) {
        ShareComposerOverlay(
            uiState = uiState,
            onTextChange = viewModel::updateShareComposerText,
            onSelectCategory = viewModel::selectShareComposerCategory,
            onSubmit = viewModel::submitShareComposer,
            onDismiss = viewModel::closeShareComposer,
        )
    }

    // Doesn't color its confirm button Danger — that token is reserved for genuinely permanent
    // data loss (Settings' full-app reset). Losing an in-progress, unsaved workout is real but
    // recoverable (the user can just start again), the same severity tier as the undecorated
    // delete confirms elsewhere (RemindersScreen, MeasurementHistorySheet).
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
}

/**
 * Redesign Gate 4a-ii — collapsed to the mock's single line ("Lưng & Tay · Bài 1/3 · 0:42") plus
 * one "Thoát" chip, replacing the old two-line header with a separate "Làm lại" chip next to it.
 *
 * "Làm lại" (restart-in-place) is deliberately dropped here, not just deferred — the mock's own
 * header has no second chip for it, and [WorkoutViewModel.resetWorkout]'s in-progress-restart case
 * (the only case this header's chip ever exercised) already has an equivalent path: "Thoát" already
 * discards the same unsaved sets/totals, and re-entering the workout re-resolves the SAME
 * not-yet-completed day (`observeIsDayCompleted` is false for an abandoned session), landing back
 * at set 1 exactly like "Làm lại" did. The one capability this header's chip is NOT equivalent to —
 * redoing an ALREADY-completed day, via [WorkoutViewModel]'s `resolvedTodayDayId`/`allowRestart`
 * plumbing — was never reachable from here anyway (this header is hidden on
 * [WorkoutPhase.SessionFinished]); it has no mock-specified home yet and is a known gap for a later
 * gate, not something this change strands. [WorkoutViewModel.resetWorkout] itself is untouched and
 * still covered by `WorkoutViewModelTest` — only this screen's own affordance for it is gone.
 */
@Composable
private fun WorkoutHeader(uiState: WorkoutUiState, onExit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            // Review finding (Gate 4a-ii) — this is the session/day title (mock's own {{ exName }}
            // slot on this line is actually the day title in the literal markup, e.g. "Lưng & Tay"),
            // NOT the current exercise name — that already renders in the photo overlay below.
            // Passing the exercise name here duplicated it there and left dayLabel shown nowhere.
            text = stringResource(
                R.string.workout_header_line,
                uiState.dayLabel,
                uiState.currentBlockIndex + 1,
                uiState.blocks.size,
                formatMinutesSeconds(uiState.sessionElapsedSeconds),
            ),
            fontFamily = HrBody,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = HrColors.TextMid,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false).padding(end = 12.dp),
        )
        Box(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clip(HrShapes.CardSmall)
                .background(HrColors.Surface)
                .border(1.dp, HrColors.Border, HrShapes.CardSmall)
                .clickable(onClick = onExit)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.workout_exit), fontFamily = HrBody, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = HrColors.TextLow)
        }
    }
}
