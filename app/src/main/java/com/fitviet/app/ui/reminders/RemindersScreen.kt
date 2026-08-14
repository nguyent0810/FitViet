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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.ReminderEntity
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.util.shortLabelRes
import java.time.DayOfWeek

@Composable
fun RemindersScreen(viewModel: RemindersViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<ReminderEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HrColors.Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGapLarge),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(HrShapes.ButtonSmall)
                    .background(HrColors.Surface)
                    .border(1.dp, HrColors.Border, HrShapes.ButtonSmall)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "‹", fontFamily = HrBody, fontSize = 18.sp, color = HrColors.TextMid)
            }
            Text(text = stringResource(R.string.reminders_title), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = HrColors.TextHi)
        }
        // Honest disclosure, not a cosmetic caption: ReminderEntity's own doc comment confirms
        // there's no WorkManager/notification-channel scheduling in this app yet, so nothing here
        // ever actually fires a system notification — only in-app "Bật/Tắt" state. Without this,
        // a user sets a 6am reminder, sees it as "Bật," and is never told it won't actually notify.
        Text(text = stringResource(R.string.reminders_no_push_disclaimer), fontFamily = HrBody, fontSize = 11.sp, color = HrColors.TextFaint)

        if (uiState.reminders.isEmpty()) {
            Text(text = stringResource(R.string.reminders_empty), fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextLow)
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
                .clip(HrShapes.CardRegular)
                .dashedBorder(Dimens.SelectedBorderWidth, HrColors.BorderSoft, HrShapes.CardRegular)
                .clickable(onClick = viewModel::addReminder)
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.reminders_add_button), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HrColors.TextLow)
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
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .padding(Dimens.CardPaddingSmall),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "%02d:%02d".format(reminder.hour, reminder.minute),
                fontFamily = HrDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = if (reminder.enabled) HrColors.TextHi else HrColors.TextFaint,
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
                        .background(if (selected) HrColors.Accent else HrColors.SurfaceInput)
                        .clickable { onToggleDay(isoDay) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(dayOfWeek.shortLabelRes()),
                        fontFamily = HrBody,
                        fontSize = 11.sp,
                        color = if (selected) HrColors.OnAccent else HrColors.TextLow,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        if (reminder.label.isNotBlank()) {
            Text(text = reminder.label, fontFamily = HrBody, fontSize = 12.sp, color = HrColors.TextLow)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ReminderActionText(text = stringResource(R.string.reminders_change_time), onClick = onChangeTimeClick)
            ReminderActionText(
                text = stringResource(if (reminder.snoozedUntilEpochDay != null) R.string.reminders_unsnooze else R.string.reminders_snooze),
                onClick = onToggleSnooze,
            )
            ReminderActionText(text = stringResource(R.string.reminders_delete), onClick = onDelete, color = HrColors.TextFaint)
        }
    }
}

@Composable
private fun StatePill(reminder: ReminderEntity, onClick: () -> Unit) {
    val (labelRes, bg, textColor) = when {
        !reminder.enabled -> Triple(R.string.reminders_state_off, HrColors.Surface, HrColors.TextLow)
        reminder.snoozedUntilEpochDay != null -> Triple(R.string.reminders_state_snoozed, HrColors.SurfaceInput, HrColors.TextLow)
        else -> Triple(R.string.reminders_state_on, HrColors.SurfaceAccent, HrColors.Accent)
    }
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(bg)
            .border(1.dp, if (reminder.enabled && reminder.snoozedUntilEpochDay == null) HrColors.Accent else HrColors.Border, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text = stringResource(labelRes), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
    }
}

@Composable
private fun ReminderActionText(text: String, onClick: () -> Unit, color: Color = HrColors.Accent) {
    Text(
        text = text,
        fontFamily = HrBody,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
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

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = HrColors.Surface) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = stringResource(R.string.reminders_time_sheet_title), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = HrColors.TextHi)
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
                    fontFamily = HrBody,
                    fontSize = 12.sp,
                    color = HrColors.TextLow,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(HrShapes.CardSmall)
                        .background(HrColors.SurfaceInput)
                        .border(1.dp, HrColors.Border, HrShapes.CardSmall)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (label.isEmpty()) {
                        Text(
                            text = stringResource(R.string.reminders_time_sheet_label_placeholder),
                            fontFamily = HrBody,
                            fontSize = 14.sp,
                            color = HrColors.TextFaint,
                        )
                    }
                    BasicTextField(
                        value = label,
                        onValueChange = { label = it },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = HrBody, fontSize = 14.sp, color = HrColors.TextHi),
                        cursorBrush = SolidColor(HrColors.Accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(HrShapes.ButtonCta)
                    .background(HrColors.Accent)
                    .clickable { onSave(hour, minute, label) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.reminders_time_sheet_save), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HrColors.OnAccent)
            }
        }
    }
}

@Composable
private fun TimeStepper(label: String, value: Int, onDecrement: () -> Unit, onIncrement: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, fontFamily = HrBody, fontSize = 12.sp, color = HrColors.TextLow)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeStepperButton(symbol = "–", onClick = onDecrement)
            Text(
                text = "%02d".format(value),
                fontFamily = HrDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = HrColors.TextHi,
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
            .background(HrColors.SurfaceInput)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = HrColors.Accent)
    }
}

/** Matches `LockedListItem.kt`'s own `dashedBorder` idiom (same [PathEffect] approach) — this
 * screen's "+ Thêm giờ nhắc" add-row is the mock's own second dashed-border spot, so this is a
 * second, separately-scoped copy of that small modifier rather than a shared extraction: neither
 * call site is large enough on its own to justify promoting it out of the file that actually needs
 * it, and [LockedListItem]'s own copy is `private` inside a component this app doesn't wire up
 * live yet (Gate 36's own doc: "unused-by-design"). */
private fun Modifier.dashedBorder(width: Dp, color: Color, shape: Shape): Modifier = drawWithContent {
    drawContent()
    val outline = shape.createOutline(size, layoutDirection, this)
    drawOutline(
        outline = outline,
        color = color,
        style = Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()), 0f),
        ),
    )
}
