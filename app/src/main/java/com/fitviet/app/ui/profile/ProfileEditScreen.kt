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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes

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
            .background(HrColors.Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGapLarge),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(HrShapes.ButtonSmall)
                    .background(HrColors.Surface)
                    .border(1.dp, HrColors.Border, HrShapes.ButtonSmall)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "‹", fontFamily = HrBody, fontSize = 18.sp, color = HrColors.TextMid)
            }
            Text(text = stringResource(R.string.profile_back), fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextLow)
        }

        Text(text = stringResource(R.string.profile_edit_title), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = HrColors.TextHi)

        if (uiState.isLoaded) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MonogramAvatar(
                    initial = avatarInitial(uiState.displayName),
                    avatarId = uiState.avatarId,
                    size = 88.dp,
                    style = TextStyle(fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp),
                    borderWidth = 2.dp,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.profile_edit_avatar_label), fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextLow)
                AvatarPicker(
                    selectedAvatarId = uiState.avatarId,
                    initial = avatarInitial(uiState.displayName),
                    onSelect = viewModel::selectAvatar,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.profile_edit_name_label), fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextLow)
                NameField(value = uiState.displayName, onValueChange = viewModel::updateDisplayName)
            }

            val canSave = uiState.displayName.trim().isNotEmpty() && !uiState.isSaving && !uiState.saved
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(HrShapes.ButtonCta)
                    .background(if (canSave) HrColors.Accent else HrColors.Surface)
                    .let { if (canSave) it.clickable(onClick = viewModel::save) else it }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.profile_edit_save),
                    fontFamily = HrBody,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (canSave) HrColors.OnAccent else HrColors.TextFaint,
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
                    style = TextStyle(fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
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
            .clip(HrShapes.CardSmall)
            .background(HrColors.SurfaceInput)
            .border(1.dp, HrColors.Border, HrShapes.CardSmall)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = stringResource(R.string.profile_edit_name_placeholder),
                fontFamily = HrBody,
                fontSize = 15.sp,
                color = HrColors.TextFaint,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = HrBody,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = HrColors.TextHi,
            ),
            cursorBrush = SolidColor(HrColors.Accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
