package com.fitviet.app.ui.nutrition.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.formatVi
import com.fitviet.app.util.shortLabelRes
import java.time.DayOfWeek
import kotlinx.coroutines.launch

@Composable
fun PlanScreen(viewModel: PlanViewModel, onBack: () -> Unit, onOpenCalendar: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundPage)) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
            Text(
                text = stringResource(R.string.nutrition_plan_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            if (uiState.hasActivePlan) {
                val planId = uiState.activePlanId
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(SurfaceCard)
                        .border(1.dp, CardBorder, MaterialTheme.shapes.small)
                        .clickable(onClick = onOpenCalendar),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "▦", style = MaterialTheme.typography.titleMedium, color = TextMuted)
                }
                Text(
                    text = stringResource(R.string.nutrition_plan_regenerate_week),
                    style = MaterialTheme.typography.labelLarge,
                    color = Accent,
                    modifier = Modifier.clickable(enabled = planId != null && !uiState.isRegenerating) {
                        if (planId != null) {
                            coroutineScope.launch { viewModel.regenerateWeek(planId) }
                        }
                    },
                )
            }
        }

        if (!uiState.hasActivePlan) {
            Text(
                text = stringResource(R.string.nutrition_plan_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 24.dp),
            )
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPaddingHorizontal),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            uiState.days.forEachIndexed { index, day ->
                DayTab(
                    label = stringResource(DayOfWeek.of(day.dayOfWeek).shortLabelRes()),
                    isSelected = index == uiState.selectedDayIndex,
                    onClick = { viewModel.selectDay(index) },
                )
            }
        }

        val selectedDay = uiState.days.getOrNull(uiState.selectedDayIndex)
        if (selectedDay != null) {
            Row(
                modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.nutrition_plan_day_totals,
                        formatVi(selectedDay.totalKcal),
                        selectedDay.totalProteinG,
                        selectedDay.totalCarbG,
                        selectedDay.totalFatG,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
                Text(
                    text = stringResource(R.string.nutrition_plan_regenerate_day),
                    style = MaterialTheme.typography.labelLarge,
                    color = Accent,
                    modifier = Modifier.clickable(enabled = !uiState.isRegenerating) {
                        coroutineScope.launch { viewModel.regenerateDay(selectedDay.dayId) }
                    },
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(Dimens.ListGapLarge),
            ) {
                items(selectedDay.meals, key = { it.mealId }) { meal ->
                    PlanMealRow(meal = meal, onClick = { viewModel.openSwapSheet(meal.mealId, meal.kcal) })
                }
            }
        }
    }

    if (uiState.swap.mealId != null) {
        SwapSheet(
            state = uiState.swap,
            onReroll = viewModel::rerollSwap,
            onApply = { coroutineScope.launch { viewModel.applyCurrentSwap() } },
            onDismiss = viewModel::dismissSwapSheet,
        )
    }
}

@Composable
private fun DayTab(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(if (isSelected) Accent else SurfaceCard)
            .border(1.dp, if (isSelected) Accent else CardBorder, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) OnAccent else TextMuted,
        )
    }
}

@Composable
private fun PlanMealRow(meal: PlanMealUiItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = meal.slot, style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Text(
                text = meal.recipeName,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = stringResource(R.string.nutrition_meal_kcal, formatVi(meal.kcal)),
            style = MaterialTheme.typography.titleSmall,
            color = Accent,
        )
    }
}
