package com.fitviet.app.ui.reminders

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.ReminderEntity
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.DeepSurface2
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.shortLabelRes
import java.time.DayOfWeek

@Composable
fun RemindersScreen(viewModel: RemindersViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<ReminderEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPage)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGapLarge),
    ) {
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
            Text(text = stringResource(R.string.reminders_title), style = MaterialTheme.typography.headlineMedium)
        }
        // Honest disclosure, not a cosmetic caption: ReminderEntity's own doc comment confirms
        // there's no WorkManager/notification-channel scheduling in this app yet, so nothing here
        // ever actually fires a system notification — only in-app "Bật/Tắt" state. Without this,
        // a user sets a 6am reminder, sees it as "Bật," and is never told it won't actually notify.
        Text(text = stringResource(R.string.reminders_no_push_disclaimer), style = MaterialTheme.typography.labelSmall, color = TextFaint)

        if (uiState.reminders.isEmpty()) {
            Text(text = stringResource(R.string.reminders_empty), style = MaterialTheme.typography.bodySmall, color = TextMuted)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                uiState.reminders.forEach { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onToggleEnabled = { viewModel.toggleEnabled(reminder) },
                        onToggleSnooze = { viewModel.toggleSnooze(reminder) },
                        onToggleDay = { day -> viewModel.toggleDay(reminder, day) },
                        onChangeTimeClick = { viewModel.openTimeSheet(reminder) },
                        onDelete = { pendingDelete = reminder },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .border(Dimens.SelectedBorderWidth, Accent, MaterialTheme.shapes.medium)
                .clickable(onClick = viewModel::addReminder)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.reminders_add_button), style = MaterialTheme.typography.titleMedium, color = Accent)
        }
    }

    uiState.editingReminder?.let { reminder ->
        TimeChangeSheet(reminder = reminder, onSave = viewModel::saveTimeAndLabel, onDismiss = viewModel::dismissTimeSheet)
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(text = stringResource(R.string.reminders_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.reminders_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteReminder(toDelete); pendingDelete = null }) {
                    Text(text = stringResource(R.string.reminders_delete_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(text = stringResource(R.string.reminders_delete_confirm_cancel))
                }
            },
        )
    }
}

@Composable
private fun ReminderCard(
    reminder: ReminderEntity,
    onToggleEnabled: () -> Unit,
    onToggleSnooze: () -> Unit,
    onToggleDay: (Int) -> Unit,
    onChangeTimeClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large)
            .padding(Dimens.CardPaddingSmall),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "%02d:%02d".format(reminder.hour, reminder.minute),
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = Anton),
                color = if (reminder.enabled) TextPrimary else TextFaint,
            )
            StatePill(reminder = reminder, onClick = onToggleEnabled)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (isoDay in 1..7) {
                val dayOfWeek = DayOfWeek.of(isoDay)
                val selected = isoDay in reminder.daysOfWeek
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (selected) Accent else DeepSurface2)
                        .clickable { onToggleDay(isoDay) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(dayOfWeek.shortLabelRes()),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) OnAccent else TextMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        if (reminder.label.isNotBlank()) {
            Text(text = reminder.label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ReminderActionText(text = stringResource(R.string.reminders_change_time), onClick = onChangeTimeClick)
            ReminderActionText(
                text = stringResource(if (reminder.snoozedUntilEpochDay != null) R.string.reminders_unsnooze else R.string.reminders_snooze),
                onClick = onToggleSnooze,
            )
            ReminderActionText(text = stringResource(R.string.reminders_delete), onClick = onDelete, color = TextFaint)
        }
    }
}

@Composable
private fun StatePill(reminder: ReminderEntity, onClick: () -> Unit) {
    val (labelRes, bg, textColor) = when {
        !reminder.enabled -> Triple(R.string.reminders_state_off, SurfaceCard, TextMuted)
        reminder.snoozedUntilEpochDay != null -> Triple(R.string.reminders_state_snoozed, DeepSurface2, TextMuted)
        else -> Triple(R.string.reminders_state_on, AccentSurfaceSelected, Accent)
    }
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(bg)
            .border(1.dp, if (reminder.enabled && reminder.snoozedUntilEpochDay == null) Accent else CardBorder, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text = stringResource(labelRes), style = MaterialTheme.typography.labelLarge, color = textColor)
    }
}

@Composable
private fun ReminderActionText(text: String, onClick: () -> Unit, color: Color = Accent) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeChangeSheet(reminder: ReminderEntity, onSave: (hour: Int, minute: Int, label: String) -> Unit, onDismiss: () -> Unit) {
    var hour by remember(reminder.id) { mutableIntStateOf(reminder.hour) }
    var minute by remember(reminder.id) { mutableIntStateOf(reminder.minute) }
    var label by remember(reminder.id) { mutableStateOf(reminder.label) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = stringResource(R.string.reminders_time_sheet_title), style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TimeStepper(
                    label = stringResource(R.string.reminders_time_sheet_hour),
                    value = hour,
                    onDecrement = { hour = (hour + 23) % 24 },
                    onIncrement = { hour = (hour + 1) % 24 },
                    modifier = Modifier.weight(1f),
                )
                TimeStepper(
                    label = stringResource(R.string.reminders_time_sheet_minute),
                    value = minute,
                    onDecrement = { minute = (minute + 59) % 60 },
                    onIncrement = { minute = (minute + 1) % 60 },
                    modifier = Modifier.weight(1f),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.reminders_time_sheet_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(SurfaceCard)
                        .border(1.dp, CardBorder, MaterialTheme.shapes.small)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (label.isEmpty()) {
                        Text(
                            text = stringResource(R.string.reminders_time_sheet_label_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextFaint,
                        )
                    }
                    BasicTextField(
                        value = label,
                        onValueChange = { label = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            color = TextPrimary,
                        ),
                        cursorBrush = SolidColor(Accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(Accent)
                    .clickable { onSave(hour, minute, label) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.reminders_time_sheet_save), style = MaterialTheme.typography.titleMedium, color = OnAccent)
            }
        }
    }
}

@Composable
private fun TimeStepper(label: String, value: Int, onDecrement: () -> Unit, onIncrement: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeStepperButton(symbol = "–", onClick = onDecrement)
            Text(
                text = "%02d".format(value),
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = Anton),
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            TimeStepperButton(symbol = "+", onClick = onIncrement)
        }
    }
}

@Composable
private fun TimeStepperButton(symbol: String, onClick: () -> Unit) {
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
