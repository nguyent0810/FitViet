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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.ui.exercise.ExerciseMediaBox
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.util.formatVi
import com.fitviet.app.util.formatWeight

/**
 * Redesign Gate 4c — token swap only, no restructure. The live-session phases rendered by this
 * file are unreachable in production: `MonthlyPlanGenerator` never sets `supersetGroup`
 * (`MonthlyPlanExerciseDraft` documents it as always null), so `ProgramDayWorkoutPlanner.buildBlocks`
 * never emits a [WorkoutBlockPlan.Superset] for any real generated *plan*. (Superset data and UI
 * both exist elsewhere in the app — `SeedData.kt` hand-authors `supersetGroup` on two seeded
 * program days, and `WorkoutPreviewScreen`'s `PreviewSupersetCard` renders them on a live nav
 * route — it's specifically the generator → live-session path that's dead.) Per the Phase 4
 * plan-check with the reviewer, that ruled out the mock's own section-7 layout restructure (a real
 * UX redesign effort with no live path to verify it against) in favor of the mechanical swap every
 * other Gate 4 file already got: legacy `Accent`/`MaterialTheme.typography`/etc. → `HrColors`/
 * `HrBody`/`HrDisplay`/etc., same shapes and spacing as before.
 */
@Composable
fun SupersetWorkContent(uiState: WorkoutUiState, viewModel: WorkoutViewModel) {
    val block = (uiState.currentBlock as? WorkoutBlockPlan.Superset)?.plan ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(HrShapes.CardSmall)
                .background(HrColors.Accent)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(text = stringResource(R.string.superset_badge), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HrColors.OnAccent)
        }

        SupersetExerciseRow(
            badge = stringResource(R.string.superset_badge_a1),
            exercise = block.exerciseA,
            plannedWeightKg = block.plannedA.weightKg,
            plannedReps = block.plannedA.reps,
            isActive = uiState.supersetSub == 0,
            isDone = uiState.supersetSub != 0,
            editableWeightKg = uiState.editableWeightKg,
            editableReps = uiState.editableReps,
            onAdjustWeight = viewModel::adjustEditableWeight,
            onAdjustReps = viewModel::adjustEditableReps,
        )
        Text(
            text = stringResource(R.string.superset_no_rest_note),
            fontFamily = HrBody,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = HrColors.TextFaint,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        SupersetExerciseRow(
            badge = stringResource(R.string.superset_badge_a2),
            exercise = block.exerciseB,
            plannedWeightKg = block.plannedB.weightKg,
            plannedReps = block.plannedB.reps,
            isActive = uiState.supersetSub == 1,
            isDone = false,
            editableWeightKg = uiState.editableWeightKg,
            editableReps = uiState.editableReps,
            onAdjustWeight = viewModel::adjustEditableWeight,
            onAdjustReps = viewModel::adjustEditableReps,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(
                    R.string.superset_round_label,
                    stringResource(R.string.superset_round_fraction, uiState.supersetRound.coerceAtMost(block.totalRounds), block.totalRounds),
                ) + "  ·  " + stringResource(R.string.superset_rest_after, 60),
                fontFamily = HrBody,
                fontSize = 13.sp,
                color = HrColors.TextLow,
            )
        }

        HrPrimaryActionButton(text = supersetButtonLabel(uiState, block), onClick = viewModel::supersetNext)

        Box(
            modifier = Modifier
                .clip(HrShapes.CardSmall)
                .border(1.dp, HrColors.Border, HrShapes.CardSmall)
                .clickable(onClick = viewModel::openTechniquePicker)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(text = stringResource(R.string.superset_technique_open), fontFamily = HrBody, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = HrColors.TextLow)
        }
    }
}

@Composable
private fun SupersetExerciseRow(
    badge: String,
    exercise: ExerciseEntity,
    plannedWeightKg: Double,
    plannedReps: Int,
    isActive: Boolean,
    isDone: Boolean,
    editableWeightKg: Double,
    editableReps: Int,
    onAdjustWeight: (Double) -> Unit,
    onAdjustReps: (Int) -> Unit,
) {
    val bg = if (isActive) HrColors.SurfaceAccent else HrColors.Surface
    val border = if (isActive) HrColors.Accent else HrColors.Border
    val badgeBg = if (isActive) HrColors.Accent else if (isDone) HrColors.Accent else HrColors.BtnCircle
    val badgeColor = if (isActive) HrColors.OnAccent else if (isDone) HrColors.OnAccent else HrColors.TextFaint
    val tagRes = if (isActive) R.string.workout_tag_current else if (isDone) R.string.workout_tag_done else R.string.workout_tag_pending
    val tagColor = if (isActive) HrColors.Accent else HrColors.TextFaint

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(bg)
            .border(if (isActive) Dimens.SelectedBorderWidth else Dimens.IdleBorderWidth, border, HrShapes.CardRegular)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ExerciseMediaBox(exercise = exercise, height = 100.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(26.dp).clip(HrShapes.ButtonSmall).background(badgeBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = badge, fontFamily = HrBody, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = badgeColor)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = exercise.nameVi, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HrColors.TextHi)
                if (!isActive) {
                    Text(
                        text = stringResource(R.string.workout_set_kg_reps, formatWeight(plannedWeightKg), plannedReps) + " · ${exercise.primaryMuscle}",
                        fontFamily = HrBody,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = HrColors.TextLow,
                    )
                }
            }
            Text(text = stringResource(tagRes), fontFamily = HrBody, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = tagColor)
        }
        if (isActive) {
            // A full-width row of its own — squeezing two steppers next to the badge/name/tag left
            // too little room for their value text, wrapping e.g. "8 reps" down to a couple of
            // characters' width (same fix as the straight-block SetRow's Stepper).
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SupersetStepper(
                    value = formatWeight(editableWeightKg),
                    unit = "kg",
                    onDecrement = { onAdjustWeight(-2.5) },
                    onIncrement = { onAdjustWeight(2.5) },
                    modifier = Modifier.weight(1f),
                )
                SupersetStepper(
                    value = editableReps.toString(),
                    unit = "reps",
                    onDecrement = { onAdjustReps(-1) },
                    onIncrement = { onAdjustReps(1) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SupersetStepper(value: String, unit: String, onDecrement: () -> Unit, onIncrement: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            modifier = Modifier
                .size(Dimens.MinTouchTarget)
                .clip(CircleShape)
                .background(HrColors.BtnCircle)
                .clickable(onClick = onDecrement),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "–", fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = HrColors.Accent)
        }
        Text(
            text = "$value $unit",
            fontFamily = HrBody,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = HrColors.TextHi,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(Dimens.MinTouchTarget)
                .clip(CircleShape)
                .background(HrColors.BtnCircle)
                .clickable(onClick = onIncrement),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "+", fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = HrColors.Accent)
        }
    }
}

@Composable
private fun supersetButtonLabel(uiState: WorkoutUiState, block: SupersetBlockPlan): String = when {
    uiState.supersetSub == 0 -> stringResource(R.string.superset_next_a1_to_a2)
    uiState.supersetRound < block.totalRounds -> stringResource(R.string.superset_next_rest)
    else -> stringResource(R.string.superset_next_done)
}

@Composable
fun SupersetBlockDoneContent(uiState: WorkoutUiState, viewModel: WorkoutViewModel) {
    val block = (uiState.currentBlock as? WorkoutBlockPlan.Superset)?.plan ?: return
    val nextBlock = uiState.blocks.getOrNull(uiState.currentBlockIndex + 1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = HrDimens.ScreenPaddingHorizontal),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(HrColors.SurfaceAccent)
                .border(2.dp, HrColors.Accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "✓", fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = HrColors.Accent)
        }
        Text(
            text = stringResource(R.string.superset_done_title),
            fontFamily = HrDisplay,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = HrColors.TextHi,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(R.string.superset_done_summary, block.totalRounds, block.totalRounds * 2, block.exerciseA.nameVi, block.exerciseB.nameVi),
            fontFamily = HrBody,
            fontSize = 13.sp,
            color = HrColors.TextLow,
            modifier = Modifier.padding(top = 6.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HrSummaryTile(value = (block.totalRounds * 2).toString(), label = stringResource(R.string.workout_stat_sets), accent = true, modifier = Modifier.weight(1f))
            HrSummaryTile(
                value = formatVi(block.totalRounds * (block.plannedA.weightKg * block.plannedA.reps + block.plannedB.weightKg * block.plannedB.reps)),
                label = stringResource(R.string.workout_stat_volume),
                modifier = Modifier.weight(1f),
            )
            HrSummaryTile(
                value = com.fitviet.app.util.formatMinutesSeconds(uiState.sessionElapsedSeconds),
                label = stringResource(R.string.workout_stat_time),
                modifier = Modifier.weight(1f),
            )
        }
        HrPrimaryActionButton(
            text = if (nextBlock != null) {
                stringResource(R.string.workout_next_exercise, exerciseLabelFor(nextBlock))
            } else {
                stringResource(R.string.workout_finish_session)
            },
            onClick = viewModel::advanceToNextBlock,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}
