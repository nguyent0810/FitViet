package com.fitviet.app.ui.monthlyplan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.domain.PlanPhase
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
fun MonthlyPlanDetailScreen(
    viewModel: MonthlyPlanDetailViewModel,
    onBack: () -> Unit,
    onDayClick: (dayId: Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundPage)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackChip(onClick = onBack)
                Text(text = stringResource(R.string.monthly_plan_detail_title), style = MaterialTheme.typography.headlineMedium)
            }
        }

        if (uiState.lockedActionMessage) {
            LockedMessageCard(onDismiss = viewModel::dismissLockedMessage)
        }

        if (uiState.planId == null && !uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.monthly_plan_empty), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = Dimens.ScreenPaddingHorizontal),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                RegenerateActionRow(
                    label = stringResource(if (uiState.isRegeneratingMonth) R.string.monthly_plan_regenerating else R.string.monthly_plan_regenerate_month),
                    enabled = !uiState.isRegeneratingMonth,
                    onClick = viewModel::regenerateMonth,
                )
            }
            items(uiState.weeks, key = { it.weekId }) { week ->
                WeekSection(
                    week = week,
                    isRegenerating = week.weekId in uiState.regeneratingWeekIds,
                    onRegenerateWeek = { viewModel.regenerateWeek(week.weekId) },
                    onDayClick = onDayClick,
                )
            }
        }
    }
}

@Composable
private fun WeekSection(
    week: MonthlyPlanWeekUi,
    isRegenerating: Boolean,
    onRegenerateWeek: () -> Unit,
    onDayClick: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${stringResource(R.string.monthly_plan_week_label, week.weekIndex + 1)} · ${stringResource(phaseLabelRes(week.phase))}",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(if (isRegenerating) R.string.monthly_plan_regenerating else R.string.monthly_plan_regenerate_week),
                style = MaterialTheme.typography.labelMedium,
                color = if (isRegenerating) TextFaint else Accent,
                modifier = Modifier.clickable(enabled = !isRegenerating, onClick = onRegenerateWeek),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            week.days.forEach { day ->
                DayRow(day = day, onClick = { if (!day.isRestDay) onDayClick(day.dayId) })
            }
        }
    }
}

@Composable
private fun DayRow(day: MonthlyPlanDayUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.medium)
            .clickable(enabled = !day.isRestDay, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(day.dayOfWeek.shortLabelRes()),
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
            )
            Text(
                text = if (day.isRestDay) stringResource(R.string.monthly_plan_rest_day) else day.sessionType.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                color = if (day.isRestDay) TextMuted else TextPrimary,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (day.isLocked) {
                Text(
                    text = stringResource(R.string.monthly_plan_locked_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextFaint,
                    modifier = Modifier
                        .background(color = ChartBarIdle, shape = MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            if (!day.isRestDay) {
                Text(text = "›", style = MaterialTheme.typography.titleMedium, color = TextMuted)
            }
        }
    }
}

@Composable
private fun RegenerateActionRow(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (enabled) Accent else ChartBarIdle)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, color = if (enabled) OnAccent else TextMuted)
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
            textAlign = TextAlign.Start,
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

private fun phaseLabelRes(phase: PlanPhase): Int = when (phase) {
    PlanPhase.BASE -> R.string.monthly_plan_phase_base
    PlanPhase.BUILD -> R.string.monthly_plan_phase_build
    PlanPhase.PEAK -> R.string.monthly_plan_phase_peak
    PlanPhase.DELOAD -> R.string.monthly_plan_phase_deload
}
