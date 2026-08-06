package com.fitviet.app.ui.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.SettingsEntity
import com.fitviet.app.ui.common.SettingsRow
import com.fitviet.app.ui.common.WidgetToggleRow
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Danger
import com.fitviet.app.ui.theme.DangerBorder
import com.fitviet.app.ui.theme.DangerSurfaceSelected
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextMuted

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, onResetComplete: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // See SettingsViewModel's class doc: an explicit imperative nav call, not the reactive
    // onboarding-completed check FitVietNavHost does on cold start (that path doesn't re-fire once
    // the graph's already composed and sitting somewhere deep in the back stack, like here).
    LaunchedEffect(uiState.resetComplete) {
        if (uiState.resetComplete) onResetComplete()
    }

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
            Text(text = stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        }

        SectionLabel(text = stringResource(R.string.settings_section_account))
        AccountSection(settings = uiState.settings, viewModel = viewModel)

        SectionLabel(text = stringResource(R.string.settings_section_notifications))
        NotificationsSection()

        SectionLabel(text = stringResource(R.string.settings_section_display))
        DisplaySection(settings = uiState.settings, viewModel = viewModel)

        ResetAppRow(onClick = viewModel::requestReset)
    }

    if (uiState.showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissResetConfirm,
            title = { Text(text = stringResource(R.string.settings_reset_confirm_title)) },
            text = { Text(text = stringResource(R.string.settings_reset_confirm_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmReset) {
                    Text(text = stringResource(R.string.settings_reset_confirm_yes), color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissResetConfirm) {
                    Text(text = stringResource(R.string.settings_reset_confirm_cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TextMuted,
        modifier = Modifier.padding(bottom = 2.dp),
    )
}

@Composable
private fun AccountSection(settings: SettingsEntity, viewModel: SettingsViewModel) {
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
private fun NotificationsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large),
    ) {
        // Static row this gate (Gate 37) — same "row exists before its destination does" precedent
        // as the original "Sao lưu dữ liệu" row. Gate 38 builds the real reminders list/route and
        // flips this to a real onClick, matching the plan's own staged sequencing.
        SettingsRow(
            label = stringResource(R.string.settings_reminders_row),
            value = stringResource(R.string.settings_reminders_value),
            onClick = null,
            showDivider = false,
        )
    }
}

@Composable
private fun DisplaySection(settings: SettingsEntity, viewModel: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large),
    ) {
        Text(
            text = stringResource(R.string.profile_widgets_title),
            style = MaterialTheme.typography.labelLarge,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        WidgetToggleRow(
            label = stringResource(R.string.profile_widget_recommendation),
            enabled = settings.showRecommendationCard,
            onClick = viewModel::toggleShowRecommendationCard,
        )
        WidgetToggleRow(
            label = stringResource(R.string.profile_widget_muscle_balance),
            enabled = settings.showMuscleBalanceCard,
            onClick = viewModel::toggleShowMuscleBalanceCard,
        )
        WidgetToggleRow(
            label = stringResource(R.string.profile_widget_nutrition),
            enabled = settings.showNutritionCard,
            onClick = viewModel::toggleShowNutritionCard,
            showDivider = false,
        )
    }
}

@Composable
private fun ResetAppRow(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(DangerSurfaceSelected)
            .border(1.dp, DangerBorder, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(R.string.settings_reset_button), style = MaterialTheme.typography.bodyMedium, color = Danger)
    }
}
