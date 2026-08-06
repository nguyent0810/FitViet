package com.fitviet.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitviet.app.R
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary

/**
 * One label/value settings row, tap-to-act if [onClick] is non-null (a static informational row,
 * like "Sao lưu dữ liệu," passes `null`). Extracted from `ProfileScreen.kt` in Gate 37 so both
 * `ProfileScreen`/`ProfileEditScreen` and the new `ui/settings/SettingsScreen.kt` share one
 * implementation instead of two copies drifting apart.
 */
@Composable
fun SettingsRow(
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

/** A [SettingsRow] whose value is always the shared "Bật"/"Tắt" (On/Off) label, accent-colored
 * when on — the exact pattern every boolean toggle row in Settings/Profile already uses. */
@Composable
fun WidgetToggleRow(label: String, enabled: Boolean, onClick: () -> Unit, showDivider: Boolean = true) {
    SettingsRow(
        label = label,
        value = stringResource(if (enabled) R.string.profile_offline_on else R.string.profile_offline_off),
        valueColor = if (enabled) Accent else TextMuted,
        valueBold = true,
        onClick = onClick,
        showDivider = showDivider,
    )
}
