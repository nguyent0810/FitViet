package com.fitviet.app.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.dao.PersonalBestRow
import com.fitviet.app.data.local.entity.WorkoutSessionEntity
import com.fitviet.app.domain.DayVolume
import com.fitviet.app.domain.WeekVolume
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentBorder
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.ChartBarIdle
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextBody
import com.fitviet.app.ui.theme.TextFaintAlt
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.formatVi
import com.fitviet.app.util.shortLabelRes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields

@Composable
fun DiaryScreen(viewModel: DiaryViewModel, onBack: () -> Unit, onOpenCalendar: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGapLarge),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(Dimens.MinTouchTarget)
                        .clip(MaterialTheme.shapes.small)
                        .background(SurfaceCard)
                        .border(1.dp, CardBorder, MaterialTheme.shapes.small)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "‹", style = MaterialTheme.typography.titleMedium, color = TextMuted)
                }
                Text(text = stringResource(R.string.diary_title), style = MaterialTheme.typography.headlineMedium)
            }
            Box(
                modifier = Modifier
                    .heightIn(min = Dimens.MinTouchTarget)
                    .clip(MaterialTheme.shapes.small)
                    .background(SurfaceCard)
                    .border(1.dp, CardBorder, MaterialTheme.shapes.small)
                    .clickable(onClick = onOpenCalendar)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.calendar_open_button), style = MaterialTheme.typography.labelLarge, color = TextBody)
            }
        }
        DayStrip(last7Days = uiState.last7Days, selectedIndex = uiState.selectedDayIndex, onSelect = viewModel::selectDay)
        DayHintCard(day = uiState.last7Days.getOrNull(uiState.selectedDayIndex), sessions = uiState.allCompletedSessions)
        WeeklyVolumeCard(last4Weeks = uiState.last4Weeks, selectedIndex = uiState.selectedWeekIndex, onSelect = viewModel::selectWeek)
        PersonalBestsCard(personalBests = uiState.personalBests)
        RecentSessionsCard(sessions = uiState.recentSessions)
    }
}

@Composable
private fun DayStrip(last7Days: List<DayVolume>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    // Not `remember`ed: recomputed each recomposition (including the ones the repository's
    // midnight tick triggers) so this doesn't freeze at whatever date the screen first opened on.
    val today = LocalDate.now()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        last7Days.forEachIndexed { index, day ->
            val selected = index == selectedIndex
            val done = day.volumeKg > 0.0
            val isRest = !done && day.date.isBefore(today)
            val bg = when {
                selected -> Accent
                done -> AccentSurfaceSelected
                else -> SurfaceCard
            }
            val border = when {
                selected -> Accent
                done -> AccentBorder
                else -> CardBorder
            }
            val markColor = when {
                selected -> OnAccent
                done -> Accent
                isRest -> TextFaintAlt
                else -> TextMuted
            }
            val labelColor = if (selected) Accent else TextFaintAlt
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(day.date.dayOfWeek.shortLabelRes()),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(bg)
                        .border(Dimens.SelectedBorderWidth, border, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (done) "✓" else if (isRest) "·" else "",
                        style = MaterialTheme.typography.labelLarge,
                        color = markColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayHintCard(day: DayVolume?, sessions: List<WorkoutSessionEntity>) {
    if (day == null) return
    val zone = remember { ZoneId.systemDefault() }
    val today = LocalDate.now() // see DayStrip — deliberately not remember'ed
    val dayLabel = stringResource(day.date.dayOfWeek.shortLabelRes())
    // A day can have more than one completed session — aggregate all of them rather than
    // picking just the first, so the hint's totals match the day-strip's summed volume.
    val matchingSessions = sessions.filter { session ->
        val completedAt = session.completedAt ?: return@filter false
        Instant.ofEpochMilli(completedAt).atZone(zone).toLocalDate() == day.date
    }
    val text = when {
        matchingSessions.isNotEmpty() -> stringResource(
            R.string.diary_hint_trained,
            dayLabel,
            matchingSessions.joinToString(" + ") { it.dayLabel },
            stringResource(R.string.diary_minutes, matchingSessions.sumOf { it.durationSeconds } / 60),
            formatVi(matchingSessions.sumOf { it.totalVolumeKg }),
        )
        day.date == today -> stringResource(R.string.diary_hint_today_pending, dayLabel)
        day.date.isBefore(today) -> stringResource(R.string.diary_hint_rest, dayLabel)
        else -> stringResource(R.string.diary_hint_future, dayLabel)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(AccentSurfaceSelected)
            .border(1.dp, AccentBorder, MaterialTheme.shapes.small)
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = TextBody)
    }
}

@Composable
private fun WeeklyVolumeCard(last4Weeks: List<WeekVolume>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val selected = last4Weeks.getOrNull(selectedIndex)
    val maxVolume = last4Weeks.maxOfOrNull { it.volumeKg }?.takeIf { it > 0 } ?: 1.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large)
            .padding(Dimens.CardPaddingSmall),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(R.string.diary_weekly_volume_title), style = MaterialTheme.typography.titleSmall)
            if (selected != null) {
                Text(text = "${formatVi(selected.volumeKg)} kg", style = MaterialTheme.typography.titleSmall, color = Accent)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(90.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            last4Weeks.forEachIndexed { index, week ->
                val fraction = if (week.volumeKg <= 0.0) 0.04f else (week.volumeKg / maxVolume).toFloat().coerceIn(0.1f, 1f)
                Column(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clickable { onSelect(index) },
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(if (index == selectedIndex) Accent else ChartBarIdle),
                        )
                    }
                    Text(
                        text = stringResource(R.string.diary_week_label, week.weekStart.isoWeekNumber()),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index == selectedIndex) Accent else TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

private fun LocalDate.isoWeekNumber(): Int = this.get(WeekFields.ISO.weekOfWeekBasedYear())

@Composable
private fun PersonalBestsCard(personalBests: List<PersonalBestRow>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large)
            .padding(Dimens.CardPaddingSmall),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.diary_pr_title), style = MaterialTheme.typography.titleSmall)
        if (personalBests.isEmpty()) {
            Text(text = stringResource(R.string.diary_pr_empty), style = MaterialTheme.typography.bodySmall, color = TextMuted)
        } else {
            personalBests.forEach { pr ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = pr.nameVi, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Text(
                        text = "${formatVi(pr.maxWeightKg)} kg",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = Anton),
                        color = Accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSessionsCard(sessions: List<WorkoutSessionEntity>) {
    val zone = remember { ZoneId.systemDefault() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large)
            .padding(Dimens.CardPaddingSmall),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = stringResource(R.string.diary_recent_title), style = MaterialTheme.typography.titleSmall)
        if (sessions.isEmpty()) {
            Text(text = stringResource(R.string.diary_recent_empty), style = MaterialTheme.typography.bodySmall, color = TextMuted)
        } else {
            sessions.forEach { session ->
                val date = session.completedAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "${date?.let { stringResource(it.dayOfWeek.shortLabelRes()) } ?: ""} · ${session.dayLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextBody,
                    )
                    Text(
                        text = stringResource(R.string.diary_recent_meta, (session.durationSeconds / 60).toString(), formatVi(session.totalVolumeKg)),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
            }
        }
    }
}
