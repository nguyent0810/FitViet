package com.fitviet.app.ui.monthlyplan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentBorder
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.ChartBarIdle
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.shortLabelRes

@Composable
fun MonthlyPlanDayDetailScreen(
    viewModel: MonthlyPlanDayDetailViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundPage)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackChip(onClick = onBack)
            Column {
                Text(
                    text = uiState.sessionType ?: stringResource(R.string.monthly_plan_rest_day),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(uiState.dayOfWeek.shortLabelRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
        }

        if (uiState.lockedActionMessage) {
            LockedMessageCard(onDismiss = viewModel::dismissLockedMessage)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = Dimens.ScreenPaddingHorizontal),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                if (uiState.isLocked) {
                    LockedBanner()
                } else {
                    RegenerateDayButton(
                        isRegenerating = uiState.isRegeneratingDay,
                        onClick = viewModel::regenerateDay,
                    )
                }
            }
            items(uiState.exercises, key = { it.monthlyPlanExerciseId }) { exercise ->
                ExerciseRow(
                    item = exercise,
                    locked = uiState.isLocked,
                    isSwapping = exercise.monthlyPlanExerciseId in uiState.swappingExerciseIds,
                    onSwap = { viewModel.swapExercise(exercise.monthlyPlanExerciseId) },
                    onSwapForEquipment = { viewModel.swapExercise(exercise.monthlyPlanExerciseId, avoidEquipment = exercise.exercise.equipment) },
                )
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    item: MonthlyPlanDayExerciseUi,
    locked: Boolean,
    isSwapping: Boolean,
    onSwap: () -> Unit,
    onSwapForEquipment: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = item.exercise.nameVi, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        Text(
            text = stringResource(R.string.monthly_plan_exercise_meta, item.targetSets, item.targetRepsMin, item.targetRepsMax),
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
        )
        if (!locked) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val actionColor = if (isSwapping) TextFaint else Accent
                Text(
                    text = stringResource(if (isSwapping) R.string.monthly_plan_regenerating else R.string.monthly_plan_exercise_swap),
                    style = MaterialTheme.typography.labelMedium,
                    color = actionColor,
                    modifier = Modifier.clickable(enabled = !isSwapping, onClick = onSwap),
                )
                Text(
                    text = stringResource(R.string.monthly_plan_exercise_swap_equipment),
                    style = MaterialTheme.typography.labelMedium,
                    color = actionColor,
                    modifier = Modifier.clickable(enabled = !isSwapping, onClick = onSwapForEquipment),
                )
            }
        }
    }
}

@Composable
private fun RegenerateDayButton(isRegenerating: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (isRegenerating) ChartBarIdle else Accent)
            .clickable(enabled = !isRegenerating, onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(if (isRegenerating) R.string.monthly_plan_regenerating else R.string.monthly_plan_day_regenerate),
            style = MaterialTheme.typography.titleSmall,
            color = if (isRegenerating) TextMuted else OnAccent,
        )
    }
}

@Composable
private fun LockedBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Text(text = stringResource(R.string.monthly_plan_day_locked_banner), style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}

@Composable
private fun LockedMessageCard(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 4.dp)
            .clip(MaterialTheme.shapes.small)
            .background(AccentSurfaceSelected)
            .border(1.dp, AccentBorder, MaterialTheme.shapes.small)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Text(
            text = stringResource(R.string.monthly_plan_locked_message),
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BackChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(MaterialTheme.shapes.small)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "‹", style = MaterialTheme.typography.titleMedium, color = TextMuted)
    }
}
