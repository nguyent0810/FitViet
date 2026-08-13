package com.fitviet.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.domain.EquipmentProfiles
import com.fitviet.app.domain.NutritionGoal
import com.fitviet.app.ui.common.pressScale
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.ui.theme.PillShape

/** Display order for the 3 goal pills — the mock's own order (Tăng cơ · Giảm mỡ · Khỏe mạnh), which
 * doesn't match [NutritionGoal]'s declaration order (CUT, MAINTAIN, BULK). */
private val GOAL_PILLS = listOf(
    NutritionGoal.BULK to R.string.goal_muscle_gain_title,
    NutritionGoal.CUT to R.string.goal_fat_loss_title,
    NutritionGoal.MAINTAIN to R.string.onboarding_goal_maintain_pill,
)

private val DAYS_PER_WEEK_OPTIONS = 2..5

/**
 * "Hit & Run" redesign (Gate 2a) — the single-screen onboarding, replacing the old
 * GoalLevelScreen + SplitScreen pair. First screen in the app to use the redesign's own
 * [HrColors]/[HrDisplay]/[HrBody]/[HrShapes]/[HrDimens] tokens instead of the pre-redesign palette
 * (Gate 1a added them additively; nothing consumed them until this gate). Copy and layout match
 * the design handoff's onboarding mock verbatim.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onSubmit: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HrColors.Bg),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(HrDimens.CardGapVertical),
        ) {
            Text(
                text = stringResource(R.string.onboarding_kicker),
                color = HrColors.Accent,
                fontFamily = HrBody,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
            )
            Text(
                text = stringResource(R.string.onboarding_title),
                color = HrColors.TextHi,
                fontFamily = HrDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.5).sp,
            )

            OnboardingQuestion(labelRes = R.string.onboarding_goal_label) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    GOAL_PILLS.forEach { (goal, titleRes) ->
                        HrPillChip(
                            label = stringResource(titleRes),
                            selected = uiState.goal == goal,
                            onClick = { viewModel.selectGoal(goal) },
                        )
                    }
                }
            }

            OnboardingQuestion(labelRes = R.string.onboarding_equipment_label) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    HrBlockChip(
                        label = stringResource(R.string.programs_filter_gym),
                        selected = uiState.equipmentProfile == null,
                        onClick = { viewModel.selectEquipmentProfile(null) },
                        modifier = Modifier.weight(1f),
                    )
                    HrBlockChip(
                        label = stringResource(R.string.programs_filter_home),
                        selected = uiState.equipmentProfile == EquipmentProfiles.HOME_DUMBBELLS,
                        onClick = { viewModel.selectEquipmentProfile(EquipmentProfiles.HOME_DUMBBELLS) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            OnboardingQuestion(labelRes = R.string.onboarding_days_label) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    DAYS_PER_WEEK_OPTIONS.forEach { days ->
                        HrBlockChip(
                            label = days.toString(),
                            selected = uiState.daysPerWeek == days,
                            onClick = { viewModel.selectDaysPerWeek(days) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.onboarding_note),
                color = HrColors.TextLow,
                fontFamily = HrBody,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(HrShapes.CardSmall)
                    .background(HrColors.Surface)
                    .border(1.dp, HrColors.Border, HrShapes.CardSmall)
                    .padding(horizontal = 15.dp, vertical = 13.dp),
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = HrDimens.CtaHeight)
                    .pressScale(onClick = { if (!uiState.isSubmitting) onSubmit() })
                    .clip(HrShapes.ButtonCta)
                    .background(HrColors.Accent)
                    .padding(vertical = HrDimens.CtaPaddingVertical),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        if (uiState.isSubmitting) R.string.quick_generate_generating else R.string.onboarding_submit,
                    ),
                    color = HrColors.OnAccent,
                    fontFamily = HrDisplay,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                )
            }
            Text(
                text = stringResource(R.string.onboarding_footer),
                color = HrColors.TextFaint,
                fontFamily = HrBody,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OnboardingQuestion(labelRes: Int, content: @Composable () -> Unit) {
    Column {
        Text(
            text = stringResource(labelRes),
            color = HrColors.TextLow,
            fontFamily = HrBody,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 7.dp),
        )
        content()
    }
}

/** Fully-rounded pill — the MỤC TIÊU row's own treatment in the mock (`border-radius:99px`),
 * distinct from [HrBlockChip]'s smaller corner radius used everywhere else on this screen. */
@Composable
private fun HrPillChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(PillShape)
            .background(if (selected) HrColors.SurfaceAccent else Color.Transparent)
            .border(1.5.dp, if (selected) HrColors.Accent else HrColors.BorderSoft, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) HrColors.Accent else HrColors.TextLow,
            fontFamily = HrBody,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
        )
    }
}

/** 12dp-rounded-rect chip — BẠN TẬP Ở ĐÂU and SỐ BUỔI/TUẦN's own treatment in the mock. */
@Composable
private fun HrBlockChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(HrShapes.ButtonSmall)
            .background(if (selected) HrColors.SurfaceAccent else Color.Transparent)
            .border(1.5.dp, if (selected) HrColors.Accent else HrColors.BorderSoft, HrShapes.ButtonSmall)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) HrColors.Accent else HrColors.TextLow,
            fontFamily = HrBody,
            // Bold (700), not ExtraBold — HrBody only registers 400/600/700 per Gate 1a's type
            // table (see Type.kt), so ExtraBold would fall back to a synthetic emboldened 700.
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
        )
    }
}
