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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary

@Composable
fun ProfileEditScreen(viewModel: ProfileEditViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigates back the moment the save completes, rather than the screen deciding for itself —
    // keeps "what happens after save" a nav-host concern, same as every other save/dismiss flow
    // in this app (e.g. ProfileViewModel's showUpdateSheet).
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onBack()
    }

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

        Text(text = stringResource(R.string.profile_edit_title), style = MaterialTheme.typography.headlineMedium)

        if (uiState.isLoaded) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MonogramAvatar(
                    initial = avatarInitial(uiState.displayName),
                    avatarId = uiState.avatarId,
                    size = 88.dp,
                    style = MaterialTheme.typography.headlineLarge,
                    borderWidth = 2.dp,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.profile_edit_avatar_label), style = MaterialTheme.typography.labelLarge, color = TextMuted)
                AvatarPicker(
                    selectedAvatarId = uiState.avatarId,
                    initial = avatarInitial(uiState.displayName),
                    onSelect = viewModel::selectAvatar,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.profile_edit_name_label), style = MaterialTheme.typography.labelLarge, color = TextMuted)
                NameField(value = uiState.displayName, onValueChange = viewModel::updateDisplayName)
            }

            val canSave = uiState.displayName.trim().isNotEmpty() && !uiState.isSaving && !uiState.saved
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(if (canSave) Accent else SurfaceCard)
                    .let { if (canSave) it.clickable(onClick = viewModel::save) else it }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.profile_edit_save),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (canSave) OnAccent else TextFaint,
                )
            }
        }
    }
}

@Composable
private fun AvatarPicker(selectedAvatarId: Int, initial: String, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AvatarStyle.entries.forEachIndexed { index, _ ->
            val selected = index == selectedAvatarId
            Box(
                modifier = Modifier.clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                MonogramAvatar(
                    initial = initial,
                    avatarId = index,
                    size = 44.dp,
                    style = MaterialTheme.typography.titleMedium,
                    borderWidth = if (selected) 2.dp else 0.dp,
                )
            }
        }
    }
}

@Composable
private fun NameField(value: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.small)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = stringResource(R.string.profile_edit_name_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = TextFaint,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                fontWeight = FontWeight.Normal,
                color = TextPrimary,
            ),
            cursorBrush = SolidColor(Accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
