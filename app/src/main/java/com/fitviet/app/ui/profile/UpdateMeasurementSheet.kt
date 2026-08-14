package com.fitviet.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.MeasurementEntity
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.util.formatWeight

/** Parses either "72.5" or the Vietnamese-typed "72,5" as a decimal. Blank/invalid -> null (field left unset). */
private fun String.toMeasurementOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateMeasurementSheet(
    prefill: MeasurementEntity?,
    isEditing: Boolean,
    onSave: (weightKg: Double?, chestCm: Double?, waistCm: Double?, armCm: Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    var weight by remember { mutableStateOf(prefill?.weightKg?.let(::formatWeight).orEmpty()) }
    var chest by remember { mutableStateOf(prefill?.chestCm?.let(::formatWeight).orEmpty()) }
    var waist by remember { mutableStateOf(prefill?.waistCm?.let(::formatWeight).orEmpty()) }
    var arm by remember { mutableStateOf(prefill?.armCm?.let(::formatWeight).orEmpty()) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = HrColors.Surface) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(if (isEditing) R.string.profile_update_sheet_edit_title else R.string.profile_update_sheet_title),
                fontFamily = HrDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = HrColors.TextHi,
            )
            MeasurementField(label = stringResource(R.string.profile_tile_weight), value = weight, onValueChange = { weight = it })
            MeasurementField(label = stringResource(R.string.profile_tile_chest), value = chest, onValueChange = { chest = it })
            MeasurementField(label = stringResource(R.string.profile_tile_waist), value = waist, onValueChange = { waist = it })
            MeasurementField(label = stringResource(R.string.profile_tile_arm), value = arm, onValueChange = { arm = it })
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(HrShapes.ButtonCta)
                    .background(HrColors.Accent)
                    .clickable {
                        onSave(
                            weight.toMeasurementOrNull(),
                            chest.toMeasurementOrNull(),
                            waist.toMeasurementOrNull(),
                            arm.toMeasurementOrNull(),
                        )
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.profile_update_save), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HrColors.OnAccent)
            }
        }
    }
}

@Composable
private fun MeasurementField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, fontFamily = HrBody, fontSize = 12.sp, color = HrColors.TextLow)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(HrShapes.CardSmall)
                .background(HrColors.SurfaceInput)
                .border(1.dp, HrColors.Border, HrShapes.CardSmall)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(fontFamily = HrBody, fontSize = 15.sp, color = HrColors.TextHi),
                cursorBrush = SolidColor(HrColors.Accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
