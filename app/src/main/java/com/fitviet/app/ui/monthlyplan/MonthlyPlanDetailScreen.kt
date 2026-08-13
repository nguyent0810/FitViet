package com.fitviet.app.ui.monthlyplan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.domain.PlanPhase
import com.fitviet.app.ui.common.HrBackChip
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.util.shortLabelRes

/** Redesign Phase 3b — migrated to [HrColors]/[HrDisplay]/[HrBody]/[HrShapes]/[HrDimens]. Kept
 * (not folded into the Kế hoạch tab) per the Phase 3 plan-check with the reviewer — this is the
 * full week/day list plus per-week regenerate, which the tab's own condensed today+upcoming
 * preview deliberately doesn't duplicate. Reached from the Kế hoạch tab's "Xem cả tháng" link. */
@Composable
fun MonthlyPlanDetailScreen(
    viewModel: MonthlyPlanDetailViewModel,
    onBack: () -> Unit,
    onDayClick: (dayId: Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(HrColors.Bg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HrBackChip(onClick = onBack)
                Text(text = stringResource(R.string.monthly_plan_detail_title), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = HrColors.TextHi)
            }
        }

        if (uiState.lockedActionMessage) {
            LockedMessageCard(onDismiss = viewModel::dismissLockedMessage)
        }

        if (uiState.planId == null && !uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.monthly_plan_empty), fontFamily = HrBody, fontSize = 14.sp, color = HrColors.TextLow)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = HrDimens.ScreenPaddingHorizontal),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                RegenerateActionRow(
                    label = stringResource(if (uiState.isRegeneratingMonth) R.string.monthly_plan_regenerating else R.string.monthly_plan_regenerate_month),
                    enabled = !uiState.isRegeneratingMonth,
                    onClick = viewModel::regenerateMonth,
                )
            }
            items(uiState.weeks, key = { it.weekId }) { week ->
                WeekSection(
                    week = week,
                    isRegenerating = week.weekId in uiState.regeneratingWeekIds,
                    onRegenerateWeek = { viewModel.regenerateWeek(week.weekId) },
                    onDayClick = onDayClick,
                )
            }
        }
    }
}

@Composable
private fun WeekSection(
    week: MonthlyPlanWeekUi,
    isRegenerating: Boolean,
    onRegenerateWeek: () -> Unit,
    onDayClick: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${stringResource(R.string.monthly_plan_week_label, week.weekIndex + 1)} · ${stringResource(phaseLabelRes(week.phase))}",
                fontFamily = HrBody,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = HrColors.TextHi,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // fill = false — this label shouldn't claim space the regenerate link needs; it
                // only grows to its own content width and yields the rest, so a long phase name
                // at a large font scale ellipsizes here rather than squeezing the link's own
                // 44dp touch target on the narrower devices this row targets.
                modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
            )
            Text(
                text = stringResource(if (isRegenerating) R.string.monthly_plan_regenerating else R.string.monthly_plan_regenerate_week),
                fontFamily = HrBody,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = if (isRegenerating) HrColors.TextFaint else HrColors.Accent,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .heightIn(min = Dimens.MinTouchTarget)
                    .clickable(enabled = !isRegenerating, onClick = onRegenerateWeek)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            week.days.forEach { day ->
                DayRow(day = day, onClick = { if (!day.isRestDay) onDayClick(day.dayId) })
            }
        }
    }
}

@Composable
private fun DayRow(day: MonthlyPlanDayUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .clickable(enabled = !day.isRestDay, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(day.dayOfWeek.shortLabelRes()),
                fontFamily = HrBody,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = HrColors.TextFaint,
            )
            Text(
                text = if (day.isRestDay) stringResource(R.string.monthly_plan_rest_day) else day.sessionType.orEmpty(),
                fontFamily = HrBody,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (day.isRestDay) HrColors.TextLow else HrColors.TextHi,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (day.isLocked) {
                Text(
                    text = stringResource(R.string.monthly_plan_locked_badge),
                    fontFamily = HrBody,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = HrColors.TextFaint,
                    modifier = Modifier
                        .background(color = HrColors.BarDim, shape = HrShapes.CardSmall)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            if (!day.isRestDay) {
                Text(text = "›", fontFamily = HrBody, fontSize = 16.sp, color = HrColors.TextFaint)
            }
        }
    }
}

@Composable
private fun RegenerateActionRow(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.ButtonCta)
            .background(if (enabled) HrColors.Accent else HrColors.BarDim)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (enabled) HrColors.OnAccent else HrColors.TextLow)
    }
}

@Composable
private fun LockedMessageCard(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 4.dp)
            .clip(HrShapes.CardSmall)
            .background(HrColors.SurfaceAccent)
            .border(1.dp, HrColors.BorderAccentDim, HrShapes.CardSmall)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Text(
            text = stringResource(R.string.monthly_plan_locked_message),
            fontFamily = HrBody,
            fontSize = 13.sp,
            color = HrColors.TextHi,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start,
        )
    }
}

private fun phaseLabelRes(phase: PlanPhase): Int = when (phase) {
    PlanPhase.BASE -> R.string.monthly_plan_phase_base
    PlanPhase.BUILD -> R.string.monthly_plan_phase_build
    PlanPhase.PEAK -> R.string.monthly_plan_phase_peak
    PlanPhase.DELOAD -> R.string.monthly_plan_phase_deload
}
