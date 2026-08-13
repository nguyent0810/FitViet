package com.fitviet.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import com.fitviet.app.R
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes

private data class TechniqueOption(val technique: SetTechnique, @StringRes val nameRes: Int, @StringRes val descRes: Int)

private val TECHNIQUE_OPTIONS = listOf(
    TechniqueOption(SetTechnique.STRAIGHT, R.string.technique_straight_name, R.string.technique_straight_desc),
    TechniqueOption(SetTechnique.SUPERSET, R.string.technique_superset_name, R.string.technique_superset_desc),
    TechniqueOption(SetTechnique.DROP_SET, R.string.technique_dropset_name, R.string.technique_dropset_desc),
    TechniqueOption(SetTechnique.PYRAMID, R.string.technique_pyramid_name, R.string.technique_pyramid_desc),
    TechniqueOption(SetTechnique.REST_PAUSE, R.string.technique_restpause_name, R.string.technique_restpause_desc),
)

/** Redesign Gate 4c — token swap only, same reasoning as `SupersetScreens.kt`'s own doc: opened
 * only from that dead (production-unreachable) superset flow, kept compiling and visually
 * consistent with the rest of the app rather than restructured. The selection dot is a small
 * Hr-token inline `Box`, not the shared `ui/onboarding.SelectionDot` (which hardcodes the legacy
 * green) — onboarding itself is outside this gate's scope, and duplicating an 18dp circle locally
 * was simpler than adding color parameters to a composable used elsewhere. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechniquePickerSheet(
    selected: SetTechnique,
    onSelect: (SetTechnique) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = HrColors.Surface) {
        Column(
            modifier = Modifier
                .padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = stringResource(R.string.superset_technique_label), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = HrColors.TextHi)
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                TECHNIQUE_OPTIONS.forEach { option ->
                    val isSelected = option.technique == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(HrShapes.CardSmall)
                            .background(if (isSelected) HrColors.SurfaceAccent else HrColors.Surface)
                            .border(
                                width = if (isSelected) Dimens.SelectedBorderWidth else Dimens.IdleBorderWidth,
                                color = if (isSelected) HrColors.Accent else HrColors.Border,
                                shape = HrShapes.CardSmall,
                            )
                            .clickable { onSelect(option.technique) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = stringResource(option.nameRes), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HrColors.TextHi)
                            Text(
                                text = " · ${stringResource(option.descRes)}",
                                fontFamily = HrBody,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = HrColors.TextFaint,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(color = if (isSelected) HrColors.Accent else Color.Transparent, shape = CircleShape)
                                .border(width = 1.5.dp, color = if (isSelected) Color.Transparent else HrColors.Border, shape = CircleShape),
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.superset_technique_other),
                fontFamily = HrBody,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = HrColors.TextFaint,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}
