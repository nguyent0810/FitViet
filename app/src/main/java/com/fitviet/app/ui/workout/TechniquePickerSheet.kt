package com.fitviet.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.fitviet.app.R
import com.fitviet.app.ui.onboarding.SelectionDot
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextPrimary

private data class TechniqueOption(val technique: SetTechnique, @StringRes val nameRes: Int, @StringRes val descRes: Int)

private val TECHNIQUE_OPTIONS = listOf(
    TechniqueOption(SetTechnique.STRAIGHT, R.string.technique_straight_name, R.string.technique_straight_desc),
    TechniqueOption(SetTechnique.SUPERSET, R.string.technique_superset_name, R.string.technique_superset_desc),
    TechniqueOption(SetTechnique.DROP_SET, R.string.technique_dropset_name, R.string.technique_dropset_desc),
    TechniqueOption(SetTechnique.PYRAMID, R.string.technique_pyramid_name, R.string.technique_pyramid_desc),
    TechniqueOption(SetTechnique.REST_PAUSE, R.string.technique_restpause_name, R.string.technique_restpause_desc),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechniquePickerSheet(
    selected: SetTechnique,
    onSelect: (SetTechnique) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = stringResource(R.string.superset_technique_label), style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                TECHNIQUE_OPTIONS.forEach { option ->
                    val isSelected = option.technique == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(if (isSelected) AccentSurfaceSelected else SurfaceCard)
                            .border(
                                width = if (isSelected) Dimens.SelectedBorderWidth else Dimens.IdleBorderWidth,
                                color = if (isSelected) Accent else CardBorder,
                                shape = MaterialTheme.shapes.small,
                            )
                            .clickable { onSelect(option.technique) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = stringResource(option.nameRes), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                text = " · ${stringResource(option.descRes)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextFaint,
                            )
                        }
                        SelectionDot(selected = isSelected)
                    }
                }
            }
            Text(
                text = stringResource(R.string.superset_technique_other),
                style = MaterialTheme.typography.labelMedium,
                color = TextFaint,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
