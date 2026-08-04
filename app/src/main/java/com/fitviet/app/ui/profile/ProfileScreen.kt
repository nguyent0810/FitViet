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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.SettingsEntity
import com.fitviet.app.domain.MeasurementDeltas
import com.fitviet.app.ui.onboarding.GOAL_OPTIONS
import com.fitviet.app.ui.onboarding.LEVEL_OPTIONS
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentBorderAlt
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.DeepSurface1
import com.fitviet.app.ui.theme.DeepSurface2
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HeroGradientEnd
import com.fitviet.app.ui.theme.HeroGradientStart
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.formatWeight
import kotlin.math.abs

// No profile-editing screen exists in the 12-screen spec (same identity gap noted for the
// dashboard's greeting since Gate 3) — a shared placeholder identity until a real one is built.
private const val PLACEHOLDER_USER_NAME = "Minh Nguyễn"
private const val PLACEHOLDER_USER_INITIAL = "M"

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            Text(text = stringResource(R.string.profile_back), style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        ProfileHeader(settings = uiState.settings)
        MeasurementsCard(
            deltas = uiState.deltas,
            weightKg = uiState.latestMeasurement?.weightKg,
            chestCm = uiState.latestMeasurement?.chestCm,
            waistCm = uiState.latestMeasurement?.waistCm,
            armCm = uiState.latestMeasurement?.armCm,
            onUpdateClick = viewModel::openUpdateSheet,
        )
        SettingsCard(settings = uiState.settings, viewModel = viewModel)
        DonateCard(donated = uiState.settings.hasDonated, onDonateClick = viewModel::toggleDonated)
    }

    if (uiState.showUpdateSheet) {
        UpdateMeasurementSheet(
            latest = uiState.latestMeasurement,
            onSave = viewModel::saveMeasurement,
            onDismiss = viewModel::dismissUpdateSheet,
        )
    }
}

@Composable
private fun ProfileHeader(settings: SettingsEntity) {
    val goalTitle = stringResource(GOAL_OPTIONS[settings.selectedGoal].titleRes)
    val levelTitle = stringResource(LEVEL_OPTIONS[settings.selectedLevel])
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(DeepSurface2)
                .border(2.dp, Accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = PLACEHOLDER_USER_INITIAL, style = MaterialTheme.typography.headlineSmall, color = Accent)
        }
        Column {
            Text(text = PLACEHOLDER_USER_NAME, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(R.string.profile_meta, levelTitle, goalTitle),
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun MeasurementsCard(
    deltas: MeasurementDeltas,
    weightKg: Double?,
    chestCm: Double?,
    waistCm: Double?,
    armCm: Double?,
    onUpdateClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large)
            .padding(Dimens.CardPaddingLarge),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(R.string.profile_measurements_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.profile_update_cta),
                style = MaterialTheme.typography.labelLarge,
                color = Accent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onUpdateClick),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MeasurementTile(value = weightKg, delta = deltas.weightKg, label = stringResource(R.string.profile_tile_weight), modifier = Modifier.weight(1f))
            MeasurementTile(value = chestCm, delta = deltas.chestCm, label = stringResource(R.string.profile_tile_chest), modifier = Modifier.weight(1f))
            MeasurementTile(value = waistCm, delta = deltas.waistCm, label = stringResource(R.string.profile_tile_waist), modifier = Modifier.weight(1f))
            MeasurementTile(value = armCm, delta = deltas.armCm, label = stringResource(R.string.profile_tile_arm), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MeasurementTile(value: Double?, delta: Double?, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(DeepSurface1)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value?.let(::formatWeight) ?: "–",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = Anton),
            color = TextPrimary,
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
        if (delta != null && delta != 0.0) {
            val positive = delta > 0
            Text(
                text = "${if (positive) "+" else "−"}${formatWeight(abs(delta))}",
                style = MaterialTheme.typography.labelSmall,
                color = if (positive) Accent else TextMuted,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SettingsCard(settings: SettingsEntity, viewModel: ProfileViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large),
    ) {
        SettingsRow(
            label = stringResource(R.string.profile_settings_language),
            value = stringResource(if (settings.languageIsEnglish) R.string.profile_lang_en else R.string.profile_lang_vi) + " ›",
            onClick = viewModel::cycleLanguage,
        )
        SettingsRow(
            label = stringResource(R.string.profile_settings_offline),
            value = stringResource(if (settings.offlineMode) R.string.profile_offline_on else R.string.profile_offline_off),
            valueColor = if (settings.offlineMode) Accent else TextMuted,
            valueBold = true,
            onClick = viewModel::toggleOffline,
        )
        // Static in the prototype too (no onClick on this row) — there's no real file-export feature yet.
        SettingsRow(
            label = stringResource(R.string.profile_settings_backup),
            value = stringResource(R.string.profile_settings_backup_value),
            onClick = null,
        )
        SettingsRow(
            label = stringResource(R.string.profile_settings_units),
            value = stringResource(if (settings.useImperialUnits) R.string.profile_unit_imperial else R.string.profile_unit_metric) + " ›",
            onClick = viewModel::cycleUnits,
            showDivider = false,
        )
    }
}

@Composable
private fun SettingsRow(
    label: String,
    value: String,
    onClick: (() -> Unit)?,
    valueColor: Color = TextMuted,
    valueBold: Boolean = false,
    showDivider: Boolean = true,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                fontWeight = if (valueBold) FontWeight.Bold else FontWeight.Normal,
            )
        }
        if (showDivider) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CardBorder))
        }
    }
}

@Composable
private fun DonateCard(donated: Boolean, onDonateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd)))
            .border(1.dp, AccentBorderAlt, MaterialTheme.shapes.large)
            .padding(Dimens.CardPaddingLarge),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (donated) {
            Text(text = stringResource(R.string.profile_donated_title), style = MaterialTheme.typography.titleSmall, color = Accent)
            Text(text = stringResource(R.string.profile_donated_body), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .border(1.dp, AccentBorderAlt, MaterialTheme.shapes.small)
                    .clickable(onClick = onDonateClick)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.profile_donated_undo), style = MaterialTheme.typography.labelLarge, color = TextMuted)
            }
        } else {
            Text(text = stringResource(R.string.profile_donate_title), style = MaterialTheme.typography.titleSmall)
            Text(text = stringResource(R.string.profile_donate_body), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .border(Dimens.SelectedBorderWidth, Accent, MaterialTheme.shapes.small)
                    .clickable(onClick = onDonateClick)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.profile_donate_cta), style = MaterialTheme.typography.titleMedium, color = Accent)
            }
        }
    }
}
