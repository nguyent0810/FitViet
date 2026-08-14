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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.ui.common.HrBackChip
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.util.shortLabelRes

@Composable
fun MonthlyPlanDayDetailScreen(
    viewModel: MonthlyPlanDayDetailViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(HrColors.Bg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HrBackChip(onClick = onBack)
            Column {
                Text(
                    text = uiState.sessionType ?: stringResource(R.string.monthly_plan_rest_day),
                    fontFamily = HrDisplay,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = HrColors.TextHi,
                )
                Text(
                    text = stringResource(uiState.dayOfWeek.shortLabelRes()),
                    fontFamily = HrBody,
                    fontSize = 13.sp,
                    color = HrColors.TextLow,
                )
            }
        }

        if (uiState.lockedActionMessage) {
            LockedMessageCard(onDismiss = viewModel::dismissLockedMessage)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = HrDimens.ScreenPaddingHorizontal),
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
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = item.exercise.nameVi, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HrColors.TextHi)
        Text(
            text = stringResource(R.string.monthly_plan_exercise_meta, item.targetSets, item.targetRepsMin, item.targetRepsMax),
            fontFamily = HrBody,
            fontSize = 12.sp,
            color = HrColors.TextLow,
        )
        if (!locked) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val actionColor = if (isSwapping) HrColors.TextFaint else HrColors.Accent
                Text(
                    text = stringResource(if (isSwapping) R.string.monthly_plan_regenerating else R.string.monthly_plan_exercise_swap),
                    fontFamily = HrBody,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = actionColor,
                    modifier = Modifier.clickable(enabled = !isSwapping, onClick = onSwap),
                )
                Text(
                    text = stringResource(R.string.monthly_plan_exercise_swap_equipment),
                    fontFamily = HrBody,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
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
            .clip(HrShapes.ButtonCta)
            .background(if (isRegenerating) HrColors.BarDim else HrColors.Accent)
            .clickable(enabled = !isRegenerating, onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(if (isRegenerating) R.string.monthly_plan_regenerating else R.string.monthly_plan_day_regenerate),
            fontFamily = HrBody,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (isRegenerating) HrColors.TextLow else HrColors.OnAccent,
        )
    }
}

@Composable
private fun LockedBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Text(text = stringResource(R.string.monthly_plan_day_locked_banner), fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextLow)
    }
}

@Composable
private fun LockedMessageCard(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 4.dp)
            .clip(HrShapes.CardSmall)
            .background(HrColors.SurfaceAccent)
            .border(1.dp, HrColors.BorderAccentDim, HrShapes.CardSmall)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Text(
            text = stringResource(R.string.monthly_plan_locked_message),
            fontFamily = HrBody,
            fontSize = 13.sp,
            color = HrColors.TextHi,
            modifier = Modifier.weight(1f),
        )
    }
}
