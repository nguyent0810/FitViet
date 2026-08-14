package com.fitviet.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.MeasurementEntity
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.util.formatLengthUnit
import com.fitviet.app.util.formatWeightUnit
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementHistorySheet(
    history: List<MeasurementEntity>,
    useImperial: Boolean,
    onEdit: (MeasurementEntity) -> Unit,
    onDelete: (MeasurementEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<MeasurementEntity?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = HrColors.Surface) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringResource(R.string.profile_history_title), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = HrColors.TextHi)
            if (history.isEmpty()) {
                Text(text = stringResource(R.string.profile_history_empty), fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextLow)
            } else {
                Column(
                    modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    history.forEach { measurement ->
                        MeasurementHistoryRow(
                            measurement = measurement,
                            useImperial = useImperial,
                            onEdit = { onEdit(measurement) },
                            onDeleteRequest = { pendingDelete = measurement },
                        )
                    }
                }
            }
        }
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(text = stringResource(R.string.profile_history_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.profile_history_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { onDelete(toDelete); pendingDelete = null }) {
                    Text(text = stringResource(R.string.profile_history_delete_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(text = stringResource(R.string.profile_history_delete_confirm_cancel))
                }
            },
        )
    }
}

@Composable
private fun MeasurementHistoryRow(
    measurement: MeasurementEntity,
    useImperial: Boolean,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val date = LocalDate.ofEpochDay(measurement.epochDay)
    val summary = listOfNotNull(
        measurement.weightKg?.let { formatWeightUnit(it, useImperial) },
        measurement.chestCm?.let { formatLengthUnit(it, useImperial) },
        measurement.waistCm?.let { formatLengthUnit(it, useImperial) },
        measurement.armCm?.let { formatLengthUnit(it, useImperial) },
    ).joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardSmall)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardSmall)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        ) {
            Text(
                text = "${date.dayOfMonth}/${date.monthValue}/${date.year}",
                fontFamily = HrBody,
                fontSize = 14.sp,
                color = HrColors.TextHi,
            )
            Text(
                text = summary,
                fontFamily = HrBody,
                fontSize = 11.sp,
                color = HrColors.TextLow,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            HistoryActionButton(label = stringResource(R.string.profile_history_edit_button), color = HrColors.Accent, onClick = onEdit)
            HistoryActionButton(label = stringResource(R.string.profile_history_delete_button), color = HrColors.TextLow, onClick = onDeleteRequest)
        }
    }
}

@Composable
private fun HistoryActionButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .sizeIn(minWidth = Dimens.MinTouchTarget, minHeight = Dimens.MinTouchTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = color)
    }
}
