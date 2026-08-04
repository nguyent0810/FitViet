package com.fitviet.app.ui.programs

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
import androidx.compose.foundation.verticalScroll
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
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.DeepSurface1
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextFaintAlt
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.shortLabelRes
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun WeeklyScheduleScreen(
    viewModel: WeeklyScheduleViewModel,
    onBack: () -> Unit,
    onStartToday: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPage)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackRow(onBack = onBack)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stringResource(R.string.schedule_title), style = MaterialTheme.typography.headlineMedium)
            if (uiState.program != null) {
                Text(text = stringResource(R.string.schedule_subtitle), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            WEEKLY_SCHEDULE.forEach { day ->
                ScheduleRow(
                    day = day,
                    isToday = day.dayOfWeek == LocalDate.now().dayOfWeek,
                    selected = day.dayOfWeek == uiState.selectedDay,
                    onClick = { viewModel.selectDay(day.dayOfWeek) },
                    onStartToday = onStartToday,
                )
            }
        }

        ScheduleHintCard(selectedDay = uiState.selectedDay)
    }
}

@Composable
private fun BackRow(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
        Text(text = stringResource(R.string.nav_programs), style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}

@Composable
private fun ScheduleRow(
    day: ScheduleDay,
    isToday: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onStartToday: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) AccentSurfaceSelected else if (day.isRest) DeepSurface1 else SurfaceCard)
            .border(
                width = if (selected) Dimens.SelectedBorderWidth else Dimens.IdleBorderWidth,
                color = if (selected) Accent else CardBorder,
                shape = MaterialTheme.shapes.medium,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(day.dayOfWeek.shortLabelRes()),
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = Anton),
            color = if (day.isRest) TextFaintAlt else Accent,
            modifier = Modifier.size(width = 26.dp, height = 20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(day.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (day.isRest) TextMuted else TextPrimary,
                )
                if (isToday && !day.isRest) {
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(Accent)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(text = stringResource(R.string.schedule_today_badge), style = MaterialTheme.typography.labelSmall, color = OnAccent)
                    }
                }
            }
            Text(text = stringResource(day.subRes), style = MaterialTheme.typography.labelMedium, color = TextMuted)
        }
        if (isToday && !day.isRest) {
            Text(
                text = stringResource(R.string.schedule_start_today),
                style = MaterialTheme.typography.labelLarge,
                color = Accent,
                modifier = Modifier.clickable(onClick = onStartToday),
            )
        } else {
            day.exerciseCount?.let {
                Text(text = stringResource(R.string.schedule_exercise_count, it), style = MaterialTheme.typography.labelMedium, color = TextMuted)
            }
        }
    }
}

@Composable
private fun ScheduleHintCard(selectedDay: DayOfWeek) {
    val day = WEEKLY_SCHEDULE.first { it.dayOfWeek == selectedDay }
    val text = if (day.isRest) {
        stringResource(R.string.schedule_hint_rest)
    } else {
        stringResource(
            R.string.schedule_hint_day,
            stringResource(day.dayOfWeek.shortLabelRes()),
            stringResource(day.titleRes),
            stringResource(day.subRes),
            stringResource(R.string.schedule_exercise_count, day.exerciseCount ?: 0),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.small)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}
