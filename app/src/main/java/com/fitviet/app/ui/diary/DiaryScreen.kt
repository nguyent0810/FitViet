package com.fitviet.app.ui.diary

import androidx.annotation.StringRes
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.dao.PersonalBestRow
import com.fitviet.app.data.local.entity.WorkoutSessionEntity
import com.fitviet.app.domain.DayVolume
import com.fitviet.app.domain.MovementTypeDistribution
import com.fitviet.app.domain.MuscleGroupWorkload
import com.fitviet.app.domain.WeekVolume
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.util.formatVi
import com.fitviet.app.util.isoWeekNumber
import com.fitviet.app.util.labelRes
import com.fitviet.app.util.shortLabelRes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Redesign Gate 7c — token re-skin against the mock's `FULL DIARY` section (~L623-660). The mock's
 * own section is a month calendar grid; this screen is a 7-day strip + weekly/muscle-group summary
 * cards instead — a real, pre-existing app extension, not a mock deviation to fix: the month-grid
 * view the mock actually shows already exists as its own separate screen
 * (`ui/calendar/WorkoutCalendarScreen.kt`), reachable from here via [onOpenCalendar] (the mock's
 * own header note: "sau avatar / link cuối màn Hôm nay" — reached via a link, not this screen
 * itself). Kept as-is, token-only re-skin.
 */
@Composable
fun DiaryScreen(viewModel: DiaryViewModel, onBack: () -> Unit, onOpenCalendar: () -> Unit, onOpenWeeklyRecap: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HrColors.Bg)
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
                        .clip(HrShapes.ButtonSmall)
                        .background(HrColors.Surface)
                        .border(1.dp, HrColors.Border, HrShapes.ButtonSmall)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "‹", fontFamily = HrBody, fontSize = 18.sp, color = HrColors.TextMid)
                }
                Text(text = stringResource(R.string.diary_title), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = HrColors.TextHi)
            }
            Box(
                modifier = Modifier
                    .heightIn(min = Dimens.MinTouchTarget)
                    .clip(HrShapes.ButtonSmall)
                    .background(HrColors.Surface)
                    .border(1.dp, HrColors.Border, HrShapes.ButtonSmall)
                    .clickable(onClick = onOpenCalendar)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.calendar_open_button), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HrColors.TextMid)
            }
        }
        DayStrip(last7Days = uiState.last7Days, selectedIndex = uiState.selectedDayIndex, onSelect = viewModel::selectDay)
        DayHintCard(day = uiState.last7Days.getOrNull(uiState.selectedDayIndex), sessions = uiState.allCompletedSessions)
        WeeklyVolumeCard(last4Weeks = uiState.last4Weeks, selectedIndex = uiState.selectedWeekIndex, onSelect = viewModel::selectWeek)
        Text(
            text = stringResource(R.string.diary_open_weekly_recap),
            fontFamily = HrBody,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = HrColors.Accent,
            modifier = Modifier.clickable(onClick = onOpenWeeklyRecap),
        )
        MuscleGroupWorkloadCard(workload = uiState.muscleGroupWorkload)
        MovementTypeCard(distribution = uiState.movementTypeDistribution)
        PersonalBestsCard(personalBests = uiState.personalBests)
        RecentSessionsCard(sessions = uiState.recentSessions, onOpenCalendar = onOpenCalendar)
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
                selected -> HrColors.Accent
                done -> HrColors.SurfaceAccent
                else -> HrColors.Surface
            }
            val border = when {
                selected -> HrColors.Accent
                done -> HrColors.BorderAccentDim
                else -> HrColors.Border
            }
            val markColor = when {
                selected -> HrColors.OnAccent
                done -> HrColors.Accent
                isRest -> HrColors.TextFaint
                else -> HrColors.TextLow
            }
            val labelColor = if (selected) HrColors.Accent else HrColors.TextFaint
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(day.date.dayOfWeek.shortLabelRes()),
                    fontFamily = HrBody,
                    fontSize = 10.sp,
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
                        fontFamily = HrBody,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
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
            .clip(HrShapes.CardSmall)
            .background(HrColors.SurfaceAccent)
            .border(1.5.dp, HrColors.Accent, HrShapes.CardSmall)
            .padding(horizontal = 15.dp, vertical = 13.dp),
    ) {
        Text(text = text, fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextMid)
    }
}

@Composable
private fun WeeklyVolumeCard(last4Weeks: List<WeekVolume>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val selected = last4Weeks.getOrNull(selectedIndex)
    val maxVolume = last4Weeks.maxOfOrNull { it.volumeKg }?.takeIf { it > 0 } ?: 1.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .padding(Dimens.CardPaddingSmall),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.diary_weekly_volume_title),
                fontFamily = HrBody,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = HrColors.TextHi,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
            )
            if (selected != null) {
                Text(
                    text = "${formatVi(selected.volumeKg)} kg",
                    fontFamily = HrBody,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = HrColors.Accent,
                    maxLines = 1,
                    softWrap = false,
                )
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
                                .clip(HrShapes.ButtonSmall)
                                .background(if (index == selectedIndex) HrColors.Accent else HrColors.BarDim),
                        )
                    }
                    Text(
                        text = stringResource(R.string.diary_week_label, week.weekStart.isoWeekNumber()),
                        fontFamily = HrBody,
                        fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 10.sp,
                        color = if (index == selectedIndex) HrColors.Accent else HrColors.TextFaint,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MuscleGroupWorkloadCard(workload: List<MuscleGroupWorkload>) {
    val maxVolume = workload.maxOfOrNull { it.volumeKg }?.takeIf { it > 0 } ?: 0.0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .padding(Dimens.CardPaddingSmall),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.diary_muscle_workload_title), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HrColors.TextHi)
        if (maxVolume <= 0.0) {
            Text(text = stringResource(R.string.diary_muscle_workload_empty), fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextLow)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                workload.forEach { entry ->
                    val fraction = (entry.volumeKg / maxVolume).toFloat().coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = stringResource(entry.muscleGroup.labelRes()), fontFamily = HrBody, fontSize = 12.sp, color = HrColors.TextMid)
                            Text(
                                text = stringResource(R.string.diary_muscle_workload_kg, formatVi(entry.volumeKg)),
                                fontFamily = HrBody,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = HrColors.TextHi,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(HrShapes.ButtonCta)
                                .background(HrColors.Border),
                        ) {
                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction).background(HrColors.Accent))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovementTypeCard(distribution: MovementTypeDistribution) {
    val hasSets = distribution.compoundSets + distribution.isolationSets > 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .padding(Dimens.CardPaddingSmall),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.diary_movement_type_title), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HrColors.TextHi)
        if (!hasSets) {
            Text(text = stringResource(R.string.diary_muscle_workload_empty), fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextLow)
        } else {
            MovementTypeRow(
                labelRes = R.string.diary_movement_type_compound,
                sets = distribution.compoundSets,
                fraction = distribution.compoundFraction,
            )
            MovementTypeRow(
                labelRes = R.string.diary_movement_type_isolation,
                sets = distribution.isolationSets,
                fraction = distribution.isolationFraction,
            )
        }
    }
}

@Composable
private fun MovementTypeRow(@StringRes labelRes: Int, sets: Int, fraction: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(labelRes), fontFamily = HrBody, fontSize = 12.sp, color = HrColors.TextMid)
            Text(text = stringResource(R.string.diary_movement_type_sets, sets), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HrColors.TextLow)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(HrShapes.ButtonCta)
                .background(HrColors.Border),
        ) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(HrColors.Accent))
        }
    }
}

@Composable
private fun PersonalBestsCard(personalBests: List<PersonalBestRow>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .padding(Dimens.CardPaddingSmall),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.diary_pr_title), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HrColors.TextHi)
        if (personalBests.isEmpty()) {
            Text(text = stringResource(R.string.diary_pr_empty), fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextLow)
        } else {
            personalBests.forEach { pr ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = pr.nameVi, fontFamily = HrBody, fontSize = 15.sp, color = HrColors.TextHi)
                    Text(
                        text = "${formatVi(pr.maxWeightKg)} kg",
                        fontFamily = HrDisplay,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = HrColors.Accent,
                    )
                }
            }
        }
    }
}

/** [sessions] is already truncated to [RECENT_SESSIONS_DISPLAY_LIMIT][com.fitviet.app.ui.diary.DiaryViewModel]
 * by the ViewModel, with no way for this composable to tell "exactly 3 total" from "3 shown, more
 * exist" — [onOpenCalendar] (the same button DiaryScreen's own header already exposes) is offered
 * whenever the list is non-empty rather than threading an extra has-more flag through just for this. */
@Composable
private fun RecentSessionsCard(sessions: List<WorkoutSessionEntity>, onOpenCalendar: () -> Unit) {
    val zone = remember { ZoneId.systemDefault() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .padding(Dimens.CardPaddingSmall),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.diary_recent_title), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HrColors.TextHi)
            if (sessions.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.diary_recent_see_all),
                    fontFamily = HrBody,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = HrColors.Accent,
                    modifier = Modifier.clickable(onClick = onOpenCalendar),
                )
            }
        }
        if (sessions.isEmpty()) {
            Text(text = stringResource(R.string.diary_recent_empty), fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextLow)
        } else {
            sessions.forEach { session ->
                val date = session.completedAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "${date?.let { stringResource(it.dayOfWeek.shortLabelRes()) } ?: ""} · ${session.dayLabel}",
                        fontFamily = HrBody,
                        fontSize = 13.sp,
                        color = HrColors.TextMid,
                    )
                    Text(
                        text = stringResource(R.string.diary_recent_meta, (session.durationSeconds / 60).toString(), formatVi(session.totalVolumeKg)),
                        fontFamily = HrBody,
                        fontSize = 13.sp,
                        color = HrColors.TextLow,
                    )
                }
            }
        }
    }
}
