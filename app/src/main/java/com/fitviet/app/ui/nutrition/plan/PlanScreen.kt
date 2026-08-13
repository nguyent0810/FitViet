package com.fitviet.app.ui.nutrition.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.ui.common.HrBackChip
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.util.formatVi
import com.fitviet.app.util.shortLabelRes
import java.time.DayOfWeek
import kotlinx.coroutines.launch

/** Redesign Gate 5d — token re-skin, same convention as Gate 4c's `SupersetScreens.kt`/
 * `TechniquePickerSheet.kt` swap: legacy `Accent`/`SurfaceCard`/`CardBorder`/`MaterialTheme.typography`
 * tokens replaced with `HrColors`/`HrDisplay`/`HrBody`/`HrShapes`/`HrDimens`, `HrBackChip` swapped
 * in for the inline back-chip `Box`.
 *
 * Not a pure token swap — three call-site changes ride along, each because the direct token
 * translation had a real Hr-token sibling precedent that was strictly better, not because this
 * gate went looking for extra scope:
 * - Both regenerate links picked up `MonthlyPlanDetailScreen.WeekSection`'s treatment for the same
 *   kind of link (disabled-state dimming via `TextFaint`, a real 44dp `Dimens.MinTouchTarget` tap
 *   target via `heightIn`, and the padding that creates it) — the pre-Gate-5d links had none of these.
 * - `PlanMealRow`'s recipe name gained `Modifier.padding(top = 2.dp)`, matching `NutritionScreen`'s
 *   `MealRow` eyebrow-title pair (same 10sp/1sp-letterSpacing eyebrow over 15sp title shape), an
 *   idiom this file already reproduces byte-for-byte everywhere else.
 * - The meal-row `LazyColumn`'s gap moved off the legacy `Dimens.ListGapLarge` (12dp) onto a raw
 *   10dp, matching `HandbookScreen`/`HandbookMuscleGroupScreen`'s own Hr-token list gap — `CalendarScreen.kt`
 *   (this screen's own sibling, re-skinned in the same gate) makes the identical change for the
 *   identical reason. */
@Composable
fun PlanScreen(viewModel: PlanViewModel, onBack: () -> Unit, onOpenCalendar: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(HrColors.Bg)) {
        Row(
            modifier = Modifier.padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HrBackChip(onClick = onBack)
            Text(
                text = stringResource(R.string.nutrition_plan_title),
                fontFamily = HrDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = HrColors.TextHi,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (uiState.hasActivePlan) {
                val planId = uiState.activePlanId
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(HrShapes.CardSmall)
                        .background(HrColors.Surface)
                        .border(1.dp, HrColors.Border, HrShapes.CardSmall)
                        .clickable(onClick = onOpenCalendar),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "▦", fontFamily = HrBody, fontSize = 15.sp, color = HrColors.TextLow)
                }
                Text(
                    text = stringResource(R.string.nutrition_plan_regenerate_week),
                    fontFamily = HrBody,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = if (uiState.isRegenerating) HrColors.TextFaint else HrColors.Accent,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .heightIn(min = Dimens.MinTouchTarget)
                        .clickable(enabled = planId != null && !uiState.isRegenerating) {
                            if (planId != null) {
                                coroutineScope.launch { viewModel.regenerateWeek(planId) }
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 12.dp),
                )
            }
        }

        if (!uiState.hasActivePlan) {
            Text(
                text = stringResource(R.string.nutrition_plan_empty),
                fontFamily = HrBody,
                fontSize = 14.sp,
                color = HrColors.TextLow,
                modifier = Modifier.padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 24.dp),
            )
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = HrDimens.ScreenPaddingHorizontal),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            uiState.days.forEachIndexed { index, day ->
                DayTab(
                    label = stringResource(DayOfWeek.of(day.dayOfWeek).shortLabelRes()),
                    isSelected = index == uiState.selectedDayIndex,
                    onClick = { viewModel.selectDay(index) },
                )
            }
        }

        val selectedDay = uiState.days.getOrNull(uiState.selectedDayIndex)
        if (selectedDay != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.nutrition_plan_day_totals,
                        formatVi(selectedDay.totalKcal),
                        selectedDay.totalProteinG,
                        selectedDay.totalCarbG,
                        selectedDay.totalFatG,
                    ),
                    fontFamily = HrBody,
                    fontSize = 13.sp,
                    color = HrColors.TextLow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                )
                Text(
                    text = stringResource(R.string.nutrition_plan_regenerate_day),
                    fontFamily = HrBody,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = if (uiState.isRegenerating) HrColors.TextFaint else HrColors.Accent,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .heightIn(min = Dimens.MinTouchTarget)
                        .clickable(enabled = !uiState.isRegenerating) {
                            coroutineScope.launch { viewModel.regenerateDay(selectedDay.dayId) }
                        }
                        .padding(horizontal = 4.dp, vertical = 12.dp),
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 4.dp),
                // Raw 10.dp, matching HandbookScreen/HandbookMuscleGroupScreen's own Hr-token list
                // gap — Dimens.ListGapLarge (12dp) is a legacy-screen token with no Hr-list precedent.
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(selectedDay.meals, key = { it.mealId }) { meal ->
                    PlanMealRow(meal = meal, onClick = { viewModel.openSwapSheet(meal.mealId, meal.kcal) })
                }
            }
        }
    }

    if (uiState.swap.mealId != null) {
        SwapSheet(
            state = uiState.swap,
            onReroll = viewModel::rerollSwap,
            onApply = { coroutineScope.launch { viewModel.applyCurrentSwap() } },
            onDismiss = viewModel::dismissSwapSheet,
        )
    }
}

@Composable
private fun DayTab(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(if (isSelected) HrColors.Accent else HrColors.Surface)
            .border(1.dp, if (isSelected) HrColors.Accent else HrColors.Border, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            fontFamily = HrBody,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = if (isSelected) HrColors.OnAccent else HrColors.TextLow,
        )
    }
}

@Composable
private fun PlanMealRow(meal: PlanMealUiItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = meal.slot, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp, color = HrColors.TextFaint)
            Text(
                text = meal.recipeName,
                fontFamily = HrBody,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = HrColors.TextHi,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = stringResource(R.string.nutrition_meal_kcal, formatVi(meal.kcal)),
            fontFamily = HrBody,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = HrColors.Accent,
            maxLines = 1,
            softWrap = false,
        )
    }
}
