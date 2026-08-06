package com.fitviet.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.ui.exercise.ExerciseMediaBox
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.Anton
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
fun StraightLogContent(uiState: WorkoutUiState, viewModel: WorkoutViewModel) {
    val block = (uiState.currentBlock as? WorkoutBlockPlan.Straight)?.plan ?: return
    // "Recommended" always reflects this set's planned target, not the value being edited below —
    // it stays a stable reference point while the user's own input can drift from it.
    val plannedCurrent = block.plannedSets.getOrNull(uiState.currentSetIndex) ?: block.plannedSets.first()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ExerciseMediaBox(exercise = block.exercise, height = 180.dp)
            ExerciseNameBlock(exercise = block.exercise)
            RecommendedWeightRow(
                weightKg = plannedCurrent.weightKg,
                reps = plannedCurrent.reps,
                doneSets = uiState.loggedSetsThisExercise.size,
                totalSets = block.plannedSets.size,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                block.plannedSets.forEachIndexed { index, planned ->
                    val status = when {
                        index < uiState.currentSetIndex -> SetRowStatus.DONE
                        index == uiState.currentSetIndex -> SetRowStatus.CURRENT
                        else -> SetRowStatus.PENDING
                    }
                    val logged = uiState.loggedSetsThisExercise.getOrNull(index)
                    SetRow(
                        setNumber = index + 1,
                        status = status,
                        weightKg = logged?.weightKg ?: (if (status == SetRowStatus.CURRENT) uiState.editableWeightKg else planned.weightKg),
                        reps = logged?.reps ?: (if (status == SetRowStatus.CURRENT) uiState.editableReps else planned.reps),
                        editable = status == SetRowStatus.CURRENT,
                        onAdjustWeight = viewModel::adjustEditableWeight,
                        onAdjustReps = viewModel::adjustEditableReps,
                    )
                }
            }
        }
        Box(modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp)) {
            PrimaryActionButton(
                text = stringResource(R.string.workout_complete_set, uiState.currentSetIndex + 1),
                onClick = viewModel::completeCurrentSet,
            )
        }
    }
}

@Composable
private fun ExerciseNameBlock(exercise: ExerciseEntity) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = exercise.nameVi, style = MaterialTheme.typography.headlineSmall)
        Text(text = exercise.nameEn, style = MaterialTheme.typography.bodySmall, color = TextFaint)
    }
}

@Composable
private fun RecommendedWeightRow(weightKg: Double, reps: Int, doneSets: Int, totalSets: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.workout_recommended_weight, formatWeight(weightKg)),
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ReadOnlyStatCircle(value = reps.toString(), label = stringResource(R.string.workout_stat_reps))
            ReadOnlyStatCircle(value = "$doneSets/$totalSets", label = stringResource(R.string.workout_stat_sets))
        }
    }
}

@Composable
private fun ReadOnlyStatCircle(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(DeepSurface2)
                .border(1.dp, CardBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}

@Composable
private fun SetRow(
    setNumber: Int,
    status: SetRowStatus,
    weightKg: Double,
    reps: Int,
    editable: Boolean,
    onAdjustWeight: (Double) -> Unit,
    onAdjustReps: (Int) -> Unit,
) {
    val bg = if (status == SetRowStatus.CURRENT) AccentSurfaceSelected else SurfaceCard
    val border = if (status == SetRowStatus.CURRENT) Accent else CardBorder
    val badgeBg = when (status) {
        SetRowStatus.DONE -> Accent
        SetRowStatus.CURRENT -> AccentSurfaceSelected
        SetRowStatus.PENDING -> DeepSurface2
    }
    val badgeColor = when (status) {
        SetRowStatus.DONE -> OnAccent
        SetRowStatus.CURRENT -> Accent
        SetRowStatus.PENDING -> TextFaintAlt
    }
    val textColor = if (status == SetRowStatus.PENDING) TextFaint else TextPrimary
    val tagRes = when (status) {
        SetRowStatus.DONE -> R.string.workout_tag_done
        SetRowStatus.CURRENT -> R.string.workout_tag_current
        SetRowStatus.PENDING -> R.string.workout_tag_pending
    }
    val tagColor = when (status) {
        SetRowStatus.DONE -> TextMuted
        SetRowStatus.CURRENT -> Accent
        SetRowStatus.PENDING -> TextFaint
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(bg)
            .border(1.dp, border, MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape).background(badgeBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (status == SetRowStatus.DONE) "✓" else setNumber.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = badgeColor,
                )
            }
            if (!editable) {
                Text(
                    text = stringResource(R.string.workout_set_kg_reps, formatWeight(weightKg), reps),
                    style = MaterialTheme.typography.titleSmall,
                    color = textColor,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            Text(text = stringResource(tagRes), style = MaterialTheme.typography.labelMedium, color = tagColor)
        }
        if (editable) {
            // A full-width row of its own — sharing this line with the badge/tag above (as it used
            // to) left too little room for two steppers with 44dp touch targets, squeezing "8 reps"
            // down to a couple of characters' width and wrapping it vertically.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stepper(
                    label = "kg",
                    value = formatWeight(weightKg),
                    onDecrement = { onAdjustWeight(-2.5) },
                    onIncrement = { onAdjustWeight(2.5) },
                    modifier = Modifier.weight(1f),
                )
                Stepper(
                    label = "reps",
                    value = reps.toString(),
                    onDecrement = { onAdjustReps(-1) },
                    onIncrement = { onAdjustReps(1) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, onDecrement: () -> Unit, onIncrement: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        StepperButton(symbol = "–", onClick = onDecrement)
        Text(
            text = "$value $label",
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        StepperButton(symbol = "+", onClick = onIncrement)
    }
}

@Composable
private fun StepperButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(Dimens.MinTouchTarget)
            .clip(CircleShape)
            .background(DeepSurface2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, style = MaterialTheme.typography.titleMedium, color = Accent)
    }
}

@Composable
internal fun PrimaryActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Accent)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, color = OnAccent)
    }
}

@Composable
fun StraightBlockDoneContent(uiState: WorkoutUiState, viewModel: WorkoutViewModel) {
    if (uiState.currentBlock !is WorkoutBlockPlan.Straight) return
    val doneSets = uiState.loggedSetsThisExercise.size
    val doneVolume = uiState.loggedSetsThisExercise.sumOf { it.weightKg * it.reps }
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
                .size(72.dp)
                .clip(CircleShape)
                .background(AccentSurfaceSelected)
                .border(2.dp, Accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "✓", style = MaterialTheme.typography.headlineLarge, color = Accent)
        }
        Text(
            text = stringResource(R.string.workout_exercise_done_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 20.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile(value = doneSets.toString(), label = stringResource(R.string.workout_stat_sets), accent = true, modifier = Modifier.weight(1f))
            SummaryTile(value = formatVi(doneVolume), label = stringResource(R.string.workout_stat_volume), modifier = Modifier.weight(1f))
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

@Composable
internal fun SummaryTile(value: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium.copy(fontFamily = Anton), color = if (accent) Accent else TextPrimary)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
    }
}

internal fun exerciseLabelFor(block: WorkoutBlockPlan): String = when (block) {
    is WorkoutBlockPlan.Straight -> block.plan.exercise.nameVi
    is WorkoutBlockPlan.Superset -> "${block.plan.exerciseA.nameVi} + ${block.plan.exerciseB.nameVi}"
}
