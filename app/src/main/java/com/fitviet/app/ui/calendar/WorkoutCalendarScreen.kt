package com.fitviet.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.WorkoutSessionEntity
import com.fitviet.app.domain.CalendarDayCell
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextBody
import com.fitviet.app.ui.theme.TextFaintAlt
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.formatVi
import com.fitviet.app.util.labelRes
import com.fitviet.app.util.shortLabelRes
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private val MONDAY_FIRST_WEEK = listOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutCalendarScreen(viewModel: WorkoutCalendarViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate = uiState.selectedDate

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPage)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGapLarge),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SquareIconButton(symbol = "‹", onClick = onBack)
            Text(text = stringResource(R.string.calendar_title), style = MaterialTheme.typography.headlineMedium)
        }
        MonthNavigator(month = uiState.month, onPrevious = viewModel::previousMonth, onNext = viewModel::nextMonth)
        WeekdayHeader()
        MonthGrid(days = uiState.days, onSelect = viewModel::selectDay)
    }

    if (selectedDate != null) {
        DaySessionsSheet(date = selectedDate, sessions = uiState.selectedDaySessions, onDismiss = viewModel::dismissDay)
    }
}

@Composable
private fun SquareIconButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(Dimens.MinTouchTarget)
            .clip(MaterialTheme.shapes.small)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, style = MaterialTheme.typography.titleMedium, color = TextMuted)
    }
}

@Composable
private fun MonthNavigator(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SquareIconButton(symbol = "‹", onClick = onPrevious)
        Text(
            text = stringResource(R.string.calendar_month_year, stringResource(month.month.labelRes()), month.year),
            style = MaterialTheme.typography.titleMedium,
        )
        SquareIconButton(symbol = "›", onClick = onNext)
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        MONDAY_FIRST_WEEK.forEach { day ->
            Text(
                text = stringResource(day.shortLabelRes()),
                style = MaterialTheme.typography.labelSmall,
                color = TextFaintAlt,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(days: List<CalendarDayCell>, onSelect: (LocalDate) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { cell -> DayCell(cell = cell, onSelect = onSelect) }
            }
        }
    }
}

// A RowScope extension (not a plain function called from inside a Row) so `Modifier.weight()`
// below resolves to the real RowScope member — a bare top-level function wouldn't have the
// RowScope receiver in scope, even when called lexically inside a Row's content lambda.
@Composable
private fun RowScope.DayCell(cell: CalendarDayCell, onSelect: (LocalDate) -> Unit) {
    // Not `remember`ed — see DiaryScreen's DayStrip for why "today" must be recomputed each
    // recomposition rather than frozen at whatever date the screen first opened on.
    val isToday = cell.date == LocalDate.now()
    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = Dimens.MinTouchTarget)
            .clickable(enabled = cell.isCurrentMonth) { onSelect(cell.date) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (cell.hasCompletedSession) AccentSurfaceSelected else Color.Transparent)
                .border(
                    width = if (isToday) Dimens.SelectedBorderWidth else 0.dp,
                    color = if (isToday) Accent else Color.Transparent,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = cell.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    !cell.isCurrentMonth -> TextFaintAlt
                    cell.hasCompletedSession -> Accent
                    else -> TextBody
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySessionsSheet(date: LocalDate, sessions: List<WorkoutSessionEntity>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "${stringResource(date.dayOfWeek.shortLabelRes())} · ${date.dayOfMonth}/${date.monthValue}/${date.year}",
                style = MaterialTheme.typography.titleMedium,
            )
            if (sessions.isEmpty()) {
                Text(text = stringResource(R.string.calendar_no_sessions), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            } else {
                // A day's session count is unbounded (full history) — bound with a max height +
                // its own scroll so a rare heavy day can't push entries past the sheet's bottom.
                Column(
                    modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    sessions.forEach { session ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .background(SurfaceCard)
                                .border(1.dp, CardBorder, MaterialTheme.shapes.small)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(text = session.dayLabel, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text(
                                text = stringResource(
                                    R.string.diary_recent_meta,
                                    (session.durationSeconds / 60).toString(),
                                    formatVi(session.totalVolumeKg),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}
