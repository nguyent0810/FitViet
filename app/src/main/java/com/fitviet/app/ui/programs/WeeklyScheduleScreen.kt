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
import androidx.compose.foundation.layout.heightIn
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
import com.fitviet.app.domain.ProgramScheduleDay
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

        val program = uiState.program
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stringResource(R.string.schedule_title), style = MaterialTheme.typography.headlineMedium)
            if (program != null) {
                Text(
                    text = stringResource(R.string.dashboard_hero_meta, program.sessionsPerWeek, program.level, program.equipment),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
        }

        if (program != null) {
            ActiveProgramRow(isActive = uiState.isActiveProgram, onSetActive = viewModel::setAsActiveProgram)
        }

        if (uiState.schedule.isEmpty()) {
            if (!uiState.isLoading) {
                Text(text = stringResource(R.string.schedule_empty), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                uiState.schedule.forEach { day ->
                    ScheduleRow(
                        day = day,
                        isToday = day.dayOfWeek == LocalDate.now().dayOfWeek,
                        selected = day.dayOfWeek == uiState.selectedDay,
                        onClick = { viewModel.selectDay(day.dayOfWeek) },
                        onStartToday = onStartToday,
                    )
                }
            }

            ScheduleHintCard(schedule = uiState.schedule, selectedDay = uiState.selectedDay)
        }
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
private fun ActiveProgramRow(isActive: Boolean, onSetActive: () -> Unit) {
    if (isActive) {
        Text(text = stringResource(R.string.schedule_active_badge), style = MaterialTheme.typography.labelLarge, color = Accent)
    } else {
        Box(
            modifier = Modifier
                .heightIn(min = Dimens.MinTouchTarget)
                .clip(MaterialTheme.shapes.small)
                .border(1.dp, Accent, MaterialTheme.shapes.small)
                .clickable(onClick = onSetActive)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(text = stringResource(R.string.schedule_set_active), style = MaterialTheme.typography.labelLarge, color = Accent)
        }
    }
}

@Composable
private fun ScheduleRow(
    day: ProgramScheduleDay,
    isToday: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onStartToday: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) AccentSurfaceSelected else if (day.isRestDay) DeepSurface1 else SurfaceCard)
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
            color = if (day.isRestDay) TextFaintAlt else Accent,
            modifier = Modifier.size(width = 26.dp, height = 20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = day.titleVi,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (day.isRestDay) TextMuted else TextPrimary,
                )
                if (isToday && !day.isRestDay) {
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
            if (!day.isRestDay) {
                Text(
                    text = day.exercises.joinToString(", ") { it.nameVi },
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                )
            } else {
                Text(text = stringResource(R.string.schedule_rest_sub), style = MaterialTheme.typography.labelMedium, color = TextMuted)
            }
        }
        if (isToday && !day.isRestDay) {
            Text(
                text = stringResource(R.string.schedule_start_today),
                style = MaterialTheme.typography.labelLarge,
                color = Accent,
                modifier = Modifier.clickable(onClick = onStartToday),
            )
        } else if (!day.isRestDay) {
            Text(text = stringResource(R.string.schedule_exercise_count, day.exercises.size), style = MaterialTheme.typography.labelMedium, color = TextMuted)
        }
    }
}

@Composable
private fun ScheduleHintCard(schedule: List<ProgramScheduleDay>, selectedDay: DayOfWeek) {
    val day = schedule.firstOrNull { it.dayOfWeek == selectedDay } ?: return
    val text = if (day.isRestDay) {
        stringResource(R.string.schedule_hint_rest, stringResource(day.dayOfWeek.shortLabelRes()))
    } else {
        stringResource(
            R.string.schedule_hint_day,
            stringResource(day.dayOfWeek.shortLabelRes()),
            day.titleVi,
            day.exercises.joinToString(", ") { it.nameVi },
            stringResource(R.string.schedule_exercise_count, day.exercises.size),
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
