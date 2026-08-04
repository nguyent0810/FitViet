package com.fitviet.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.MeasurementEntity
import com.fitviet.app.domain.cmToIn
import com.fitviet.app.domain.inToCm
import com.fitviet.app.domain.kgToLb
import com.fitviet.app.domain.lbToKg
import com.fitviet.app.ui.onboarding.GOAL_OPTIONS
import com.fitviet.app.ui.onboarding.LEVEL_OPTIONS
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentBorder
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.DeepSurface1
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HeroGradientEnd
import com.fitviet.app.ui.theme.HeroGradientStart
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.formatOneDecimalVi
import com.fitviet.app.util.parseDecimalInput

// Placeholder until there's a real editable profile name field.
private const val PLACEHOLDER_FULL_NAME = "Minh Nguyễn"

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGapLarge),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(SurfaceCard)
                        .border(1.dp, CardBorder, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "‹", style = MaterialTheme.typography.titleMedium, color = TextMuted)
                }
            }
            Text(text = stringResource(R.string.profile_back), style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }

        ProfileHeader(uiState = uiState)
        MeasurementsCard(
            latest = uiState.latestMeasurement,
            previous = uiState.previousMeasurement,
            useImperialUnits = uiState.settings.useImperialUnits,
            onUpdate = viewModel::openUpdateMeasurement,
        )
        SettingsList(uiState = uiState, viewModel = viewModel)
        DonateCard(hasDonated = uiState.settings.hasDonated, onToggleDonated = viewModel::toggleDonated)
    }

    if (uiState.isUpdateMeasurementOpen) {
        UpdateMeasurementSheet(
            useImperialUnits = uiState.settings.useImperialUnits,
            onSave = viewModel::saveMeasurement,
            onDismiss = viewModel::closeUpdateMeasurement,
        )
    }
}

@Composable
private fun ProfileHeader(uiState: ProfileUiState) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(DeepSurface1)
                .border(2.dp, Accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = PLACEHOLDER_FULL_NAME.take(1), style = MaterialTheme.typography.headlineSmall, color = Accent)
        }
        Column {
            Text(text = PLACEHOLDER_FULL_NAME, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(
                    R.string.profile_summary,
                    stringResource(LEVEL_OPTIONS[uiState.settings.selectedLevel]),
                    stringResource(GOAL_OPTIONS[uiState.settings.selectedGoal].titleRes),
                    uiState.weeksWithApp,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun MeasurementsCard(
    latest: MeasurementEntity?,
    previous: MeasurementEntity?,
    useImperialUnits: Boolean,
    onUpdate: () -> Unit,
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
            Text(text = stringResource(R.string.profile_measurements_title), style = MaterialTheme.typography.titleSmall)
            Box(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clickable(onClick = onUpdate)
                    .padding(start = 8.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = stringResource(R.string.profile_update),
                    style = MaterialTheme.typography.labelLarge,
                    color = Accent,
                )
            }
        }
        if (latest == null) {
            Text(text = stringResource(R.string.profile_measurements_empty), style = MaterialTheme.typography.bodySmall, color = TextMuted)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val weightLabel = stringResource(if (useImperialUnits) R.string.profile_weight_label_imperial else R.string.profile_weight_label)
                val chestLabel = stringResource(if (useImperialUnits) R.string.profile_chest_label_imperial else R.string.profile_chest_label)
                val waistLabel = stringResource(if (useImperialUnits) R.string.profile_waist_label_imperial else R.string.profile_waist_label)
                val armLabel = stringResource(if (useImperialUnits) R.string.profile_arm_label_imperial else R.string.profile_arm_label)
                MeasurementTile(latest.weightKg?.toDisplay(useImperialUnits, isWeight = true), previous?.weightKg?.toDisplay(useImperialUnits, isWeight = true), weightLabel, Modifier.weight(1f))
                MeasurementTile(latest.chestCm?.toDisplay(useImperialUnits, isWeight = false), previous?.chestCm?.toDisplay(useImperialUnits, isWeight = false), chestLabel, Modifier.weight(1f))
                MeasurementTile(latest.waistCm?.toDisplay(useImperialUnits, isWeight = false), previous?.waistCm?.toDisplay(useImperialUnits, isWeight = false), waistLabel, Modifier.weight(1f))
                MeasurementTile(latest.armCm?.toDisplay(useImperialUnits, isWeight = false), previous?.armCm?.toDisplay(useImperialUnits, isWeight = false), armLabel, Modifier.weight(1f))
            }
        }
    }
}

private fun Double.toDisplay(useImperialUnits: Boolean, isWeight: Boolean): Double = when {
    !useImperialUnits -> this
    isWeight -> kgToLb(this)
    else -> cmToIn(this)
}

@Composable
private fun MeasurementTile(value: Double?, previousValue: Double?, label: String, modifier: Modifier = Modifier) {
    val delta = if (value != null && previousValue != null) value - previousValue else null
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(DeepSurface1)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value?.let { formatOneDecimalVi(it) } ?: "—",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = Anton),
            color = TextPrimary,
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
        if (delta != null) {
            val sign = if (delta >= 0) "+" else ""
            Text(
                text = "$sign${formatOneDecimalVi(delta)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (delta >= 0) Accent else TextMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun SettingsList(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large),
    ) {
        SettingsRow(
            label = stringResource(R.string.profile_setting_language),
            value = stringResource(if (uiState.settings.languageIsEnglish) R.string.profile_lang_en else R.string.profile_lang_vi) + " ›",
            valueColor = TextMuted,
            onClick = viewModel::cycleLanguage,
        )
        SettingsRow(
            label = stringResource(R.string.profile_setting_offline),
            value = stringResource(if (uiState.settings.offlineMode) R.string.profile_offline_on else R.string.profile_offline_off),
            valueColor = if (uiState.settings.offlineMode) Accent else TextFaint,
            onClick = viewModel::toggleOffline,
        )
        SettingsRow(
            label = stringResource(R.string.profile_setting_backup),
            value = stringResource(R.string.profile_setting_backup_value),
            valueColor = TextMuted,
            onClick = null,
        )
        SettingsRow(
            label = stringResource(R.string.profile_setting_unit),
            value = stringResource(if (uiState.settings.useImperialUnits) R.string.profile_unit_imperial else R.string.profile_unit_metric) + " ›",
            valueColor = TextMuted,
            onClick = viewModel::cycleUnits,
            showDivider = false,
        )
    }
}

@Composable
private fun SettingsRow(label: String, value: String, valueColor: Color, onClick: (() -> Unit)?, showDivider: Boolean = true) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, color = valueColor)
        }
        if (showDivider) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CardBorder))
        }
    }
}

@Composable
private fun DonateCard(hasDonated: Boolean, onToggleDonated: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd)))
            .border(1.dp, AccentBorder, MaterialTheme.shapes.large)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!hasDonated) {
            Text(text = stringResource(R.string.profile_donate_free_title), style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(R.string.profile_donate_free_body), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .border(Dimens.SelectedBorderWidth, Accent, MaterialTheme.shapes.small)
                    .clickable(onClick = onToggleDonated)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.profile_donate_cta), style = MaterialTheme.typography.titleSmall, color = Accent)
            }
        } else {
            Text(text = stringResource(R.string.profile_donate_thanks_title), style = MaterialTheme.typography.titleMedium, color = Accent)
            Text(text = stringResource(R.string.profile_donate_thanks_body), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .border(1.dp, CardBorder, MaterialTheme.shapes.small)
                    .clickable(onClick = onToggleDonated)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.profile_donate_undo), style = MaterialTheme.typography.labelLarge, color = TextFaint)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateMeasurementSheet(
    useImperialUnits: Boolean,
    onSave: (Double?, Double?, Double?, Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    var weight by remember { mutableStateOf("") }
    var chest by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var arm by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringResource(R.string.profile_update_measurement_title), style = MaterialTheme.typography.titleMedium)
            NumberField(label = stringResource(if (useImperialUnits) R.string.profile_weight_label_imperial else R.string.profile_weight_label), value = weight, onValueChange = { weight = it })
            NumberField(label = stringResource(if (useImperialUnits) R.string.profile_chest_label_imperial else R.string.profile_chest_label), value = chest, onValueChange = { chest = it })
            NumberField(label = stringResource(if (useImperialUnits) R.string.profile_waist_label_imperial else R.string.profile_waist_label), value = waist, onValueChange = { waist = it })
            NumberField(label = stringResource(if (useImperialUnits) R.string.profile_arm_label_imperial else R.string.profile_arm_label), value = arm, onValueChange = { arm = it })
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(Accent)
                    .clickable {
                        // Inputs are entered in the currently selected unit; storage is always metric.
                        // parseDecimalInput accepts both "72.5" and the vi-VN "72,5".
                        onSave(
                            parseDecimalInput(weight)?.let { if (useImperialUnits) lbToKg(it) else it },
                            parseDecimalInput(chest)?.let { if (useImperialUnits) inToCm(it) else it },
                            parseDecimalInput(waist)?.let { if (useImperialUnits) inToCm(it) else it },
                            parseDecimalInput(arm)?.let { if (useImperialUnits) inToCm(it) else it },
                        )
                    }
                    .padding(vertical = 16.dp)
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.profile_save), style = MaterialTheme.typography.titleMedium, color = OnAccent)
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}
