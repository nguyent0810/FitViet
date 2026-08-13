package com.fitviet.app.ui.quickgenerate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.domain.SplitTemplate
import com.fitviet.app.domain.TrainingGoal
import com.fitviet.app.ui.common.pressScale
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.ui.theme.PillShape
import java.time.LocalDate

private data class GoalOption(val goal: TrainingGoal, val labelRes: Int)

private val GOAL_CHOICES = listOf(
    GoalOption(TrainingGoal.HYPERTROPHY, R.string.goal_muscle_gain_title),
    GoalOption(TrainingGoal.STRENGTH, R.string.goal_strength_title),
    GoalOption(TrainingGoal.GENERAL_FITNESS, R.string.goal_health_title),
)

private data class SplitOption(val split: SplitTemplate, val titleRes: Int, val subRes: Int)

// CUSTOM is deliberately excluded — its authoring UI isn't part of this pass (see the "Hit & Run"
// plan's scope note); it's only reachable via a future dedicated flow, not this picker.
private val SPLIT_CHOICES = listOf(
    SplitOption(SplitTemplate.PPL, R.string.split_ppl_title, R.string.split_ppl_sub),
    SplitOption(SplitTemplate.UPPER_LOWER, R.string.split_upper_lower_title, R.string.split_upper_lower_sub),
    SplitOption(SplitTemplate.FULL_BODY, R.string.split_fullbody_title, R.string.split_fullbody_sub),
    SplitOption(SplitTemplate.BRO_SPLIT, R.string.split_bro_title, R.string.split_bro_sub),
    SplitOption(SplitTemplate.PHUL, R.string.split_phul_title, R.string.split_phul_sub),
)

private val DAYS_PER_WEEK_OPTIONS = 2..6

/**
 * Redesign Gate 3c — replaces the old full-screen `QuickGenerateScreen`/`quick_generate` nav
 * destination with a `ModalBottomSheet`, matching the mock's own GENERATE SHEET block (design HTML
 * lines 348-375). Per the Phase 3 plan-check with the reviewer: no precedent in this app for a
 * sheet shared across screens (both existing sheets — `TechniquePickerSheet`, `SwapSheet` — are
 * screen-local), so this is hosted independently by each screen that offers it (the Kế hoạch tab,
 * Dashboard's empty state) rather than behind a single shared route — each host creates its own
 * [QuickGenerateViewModel] instance via its own `viewModel()` call, and calls
 * [QuickGenerateViewModel.refreshPrefill] right before opening the sheet (see that function's own
 * doc for why a plain `viewModel()`-scoped instance needs an explicit refresh, unlike the one-shot
 * full-screen version this replaced).
 *
 * Level is deliberately not a picker here, unlike the old full-screen version — the mock's own
 * sheet has no level control either, and it's already prefilled from onboarding/settings and
 * written back on generate via [QuickGenerateViewModel.generate]'s own `updateSelectedLevel` call,
 * so dropping it from the UI loses no real capability. Split is collapsed behind "Đổi kiểu chia"
 * by default, matching the mock's own read-only "app chọn theo số buổi" line — but unlike the
 * mock's copy ("đổi được sau khi tạo," i.e. changeable after creation), this app has no actual
 * post-creation split-change mechanism ([com.fitviet.app.data.repository.MonthlyPlanRepository]'s
 * own `choicesFrom` always preserves a regenerated plan's original split), so the override stays
 * reachable here instead of being removed outright — dropping it here would have been a real
 * capability regression against the pre-Gate-3c full-screen picker, not just a copy simplification.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GenerateSheet(
    viewModel: QuickGenerateViewModel,
    onDismiss: () -> Unit,
    onGenerateClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var splitExpanded by remember { mutableStateOf(false) }
    val targetMonth = remember { LocalDate.now().monthValue }
    // MEDIUM (Gate 3c review) — a partially-expanded intermediate state serves no purpose here
    // (there's no peek content worth showing collapsed), and without this a short drag can strand
    // the sheet half-open, hiding the CTA below the fold.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = HrColors.Surface) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(R.string.quick_generate_title), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = HrColors.TextHi)
                Text(text = stringResource(R.string.quick_generate_subtitle), fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextLow)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = stringResource(R.string.quick_generate_goal_label))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GOAL_CHOICES.forEach { option ->
                        SheetPill(
                            label = stringResource(option.labelRes),
                            selected = uiState.goal == option.goal,
                            onClick = { viewModel.selectGoal(option.goal) },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = stringResource(R.string.quick_generate_days_label))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DAYS_PER_WEEK_OPTIONS.forEach { days ->
                        SheetPill(
                            label = days.toString(),
                            selected = uiState.daysPerWeek == days,
                            onClick = { viewModel.selectDaysPerWeek(days) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (splitExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(text = stringResource(R.string.quick_generate_split_label))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SPLIT_CHOICES.forEach { option ->
                            SplitCard(
                                title = stringResource(option.titleRes),
                                subtitle = stringResource(option.subRes),
                                selected = uiState.splitTemplate == option.split,
                                onClick = { viewModel.selectSplit(option.split) },
                            )
                        }
                    }
                }
            } else {
                val splitLabel = stringResource(splitTemplateLabelRes(uiState.splitTemplate))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.quick_generate_split_readonly, splitLabel),
                        fontFamily = HrBody,
                        fontSize = 12.sp,
                        color = HrColors.TextLow,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = " · ${stringResource(R.string.quick_generate_change_split)}",
                        fontFamily = HrBody,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = HrColors.Accent,
                        modifier = Modifier
                            .heightIn(min = Dimens.MinTouchTarget)
                            .clickable { splitExpanded = true }
                            .padding(vertical = 6.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale(onClick = onGenerateClick)
                    .clip(HrShapes.ButtonCta)
                    .background(HrColors.Accent)
                    .padding(vertical = HrDimens.CtaPaddingVertical),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        if (uiState.isGenerating) R.string.quick_generate_generating else R.string.quick_generate_cta_with_month,
                        targetMonth,
                    ),
                    fontFamily = HrDisplay,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = HrColors.OnAccent,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp, color = HrColors.TextLow)
}

@Composable
private fun SheetPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(PillShape)
            .background(if (selected) HrColors.Accent else HrColors.Surface)
            .border(1.dp, if (selected) HrColors.Accent else HrColors.Border, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = HrBody,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = if (selected) HrColors.OnAccent else HrColors.TextMid,
        )
    }
}

@Composable
private fun SplitCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(if (selected) HrColors.SurfaceAccent else HrColors.Surface)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) HrColors.Accent else HrColors.Border, HrShapes.CardRegular)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = title, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HrColors.TextHi)
        Text(text = subtitle, fontFamily = HrBody, fontSize = 12.sp, color = HrColors.TextLow)
    }
}

private fun splitTemplateLabelRes(template: SplitTemplate): Int = when (template) {
    SplitTemplate.PPL -> R.string.split_ppl_title
    SplitTemplate.UPPER_LOWER -> R.string.split_upper_lower_title
    SplitTemplate.FULL_BODY -> R.string.split_fullbody_title
    SplitTemplate.BRO_SPLIT -> R.string.split_bro_title
    SplitTemplate.PHUL -> R.string.split_phul_title
    SplitTemplate.CUSTOM -> R.string.split_custom_title
}
