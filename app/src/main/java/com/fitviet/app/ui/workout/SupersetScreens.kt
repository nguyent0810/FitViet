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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitviet.app.R
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.DeepSurface2
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextFaintAlt
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.formatVi
import com.fitviet.app.util.formatWeight

@Composable
fun SupersetWorkContent(uiState: WorkoutUiState, viewModel: WorkoutViewModel) {
    val block = (uiState.currentBlock as? WorkoutBlockPlan.Superset)?.plan ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenPaddingHorizontal),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(Accent)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(text = stringResource(R.string.superset_badge), style = MaterialTheme.typography.labelSmall, color = OnAccent)
        }

        SupersetExerciseRow(
            badge = stringResource(R.string.superset_badge_a1),
            name = block.exerciseA.nameVi,
            muscle = block.exerciseA.primaryMuscle,
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
            style = MaterialTheme.typography.labelMedium,
            color = TextFaint,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        SupersetExerciseRow(
            badge = stringResource(R.string.superset_badge_a2),
            name = block.exerciseB.nameVi,
            muscle = block.exerciseB.primaryMuscle,
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
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }

        PrimaryActionButton(text = supersetButtonLabel(uiState, block), onClick = viewModel::supersetNext)

        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .border(1.dp, CardBorder, MaterialTheme.shapes.small)
                .clickable(onClick = viewModel::openTechniquePicker)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(text = stringResource(R.string.superset_technique_open), style = MaterialTheme.typography.labelLarge, color = TextMuted)
        }
    }
}

@Composable
private fun SupersetExerciseRow(
    badge: String,
    name: String,
    muscle: String,
    plannedWeightKg: Double,
    plannedReps: Int,
    isActive: Boolean,
    isDone: Boolean,
    editableWeightKg: Double,
    editableReps: Int,
    onAdjustWeight: (Double) -> Unit,
    onAdjustReps: (Int) -> Unit,
) {
    val bg = if (isActive) AccentSurfaceSelected else SurfaceCard
    val border = if (isActive) Accent else CardBorder
    val badgeBg = if (isActive) Accent else if (isDone) Accent else DeepSurface2
    val badgeColor = if (isActive) OnAccent else if (isDone) OnAccent else TextFaintAlt
    val tagRes = if (isActive) R.string.workout_tag_current else if (isDone) R.string.workout_tag_done else R.string.workout_tag_pending
    val tagColor = if (isActive) Accent else TextFaint

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(bg)
            .border(if (isActive) Dimens.SelectedBorderWidth else Dimens.IdleBorderWidth, border, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(26.dp).clip(MaterialTheme.shapes.extraSmall).background(badgeBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = badge, style = MaterialTheme.typography.labelMedium, color = badgeColor)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                if (!isActive) {
                    Text(
                        text = stringResource(R.string.workout_set_kg_reps, formatWeight(plannedWeightKg), plannedReps) + " · $muscle",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                    )
                }
            }
            Text(text = stringResource(tagRes), style = MaterialTheme.typography.labelMedium, color = tagColor)
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
                .background(DeepSurface2)
                .clickable(onClick = onDecrement),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "–", style = MaterialTheme.typography.titleSmall, color = Accent)
        }
        Text(
            text = "$value $unit",
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(Dimens.MinTouchTarget)
                .clip(CircleShape)
                .background(DeepSurface2)
                .clickable(onClick = onIncrement),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "+", style = MaterialTheme.typography.titleSmall, color = Accent)
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
            .padding(horizontal = Dimens.ScreenPaddingHorizontal),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(AccentSurfaceSelected)
                .border(2.dp, Accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "✓", style = MaterialTheme.typography.headlineMedium, color = Accent)
        }
        Text(
            text = stringResource(R.string.superset_done_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(R.string.superset_done_summary, block.totalRounds, block.totalRounds * 2, block.exerciseA.nameVi, block.exerciseB.nameVi),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(top = 6.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile(value = (block.totalRounds * 2).toString(), label = stringResource(R.string.workout_stat_sets), accent = true, modifier = Modifier.weight(1f))
            SummaryTile(
                value = formatVi(block.totalRounds * (block.plannedA.weightKg * block.plannedA.reps + block.plannedB.weightKg * block.plannedB.reps)),
                label = stringResource(R.string.workout_stat_volume),
                modifier = Modifier.weight(1f),
            )
            SummaryTile(
                value = com.fitviet.app.util.formatMinutesSeconds(uiState.sessionElapsedSeconds),
                label = stringResource(R.string.workout_stat_time),
                modifier = Modifier.weight(1f),
            )
        }
        PrimaryActionButton(
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
