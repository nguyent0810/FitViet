package com.fitviet.app.ui.nutrition

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import android.graphics.BlurMaskFilter
import android.graphics.Paint as FrameworkPaint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.MealEntity
import com.fitviet.app.domain.NutritionGoals
import com.fitviet.app.domain.NutritionTotals
import com.fitviet.app.ui.common.pressScale
import com.fitviet.app.ui.common.rememberReducedMotion
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.util.formatVi

/**
 * Redesign Gate 5a — Nutrition Home rebuilt per the mock's own literal markup (design_handoff_hit_and_run_redesign,
 * `FitViet Redesign Hit & Run.dc.html` lines ~149-179): title → gradient summary card (kcal ring +
 * 3 macro bars) → "Hôm nay" header + "+ Thêm món" → today's meal rows → one library link row →
 * footer. The old `QuickActionsRow` (4 separate entry chips to Discover/Foods/Templates/CreatePlan)
 * is deleted outright — the mock replaces all four with the single library link row below.
 *
 * One deliberate deviation from the literal mock, scoped and flagged during the Phase 5 plan-check
 * with the reviewer: [PlannedMealsSection] (today's meals from an active
 * [com.fitviet.app.data.local.entity.UserMealPlanEntity]) has no equivalent block in the mock's
 * markup at all, but this phase's own gate sequence doesn't land the "Kế hoạch tuần" tab (Gate
 * 5b-ii) — the only other place a link to `nutrition/plan` will exist — until one gate from now.
 * Dropping it here would leave `nutrition/plan` (a real, already-shipped screen) unreachable in
 * the interim. Kept for continuity, still re-skinned to Hr tokens; retire this whole section once
 * Gate 5b-ii ships its own entry point. [onOpenLibrary] itself already points at the real
 * `nutrition/library` destination as of Gate 5b-i — landed in the same push as this file's own
 * fixes so `nutrition/foods`/`nutrition/templates` were never shipped orphaned (see Gate 5a's own
 * review finding H2).
 */
@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel,
    onOpenLibrary: () -> Unit,
    onOpenCreatePlan: () -> Unit,
    onOpenPlan: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HrColors.Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(text = stringResource(R.string.nutrition_title), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, color = HrColors.TextHi)

        SummaryCard(totals = uiState.totals)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.nutrition_today_header), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HrColors.TextHi)
            Box(
                modifier = Modifier
                    .heightIn(min = Dimens.MinTouchTarget)
                    .pressScale(onClick = viewModel::addNextPreset)
                    .clip(HrShapes.ButtonSmall)
                    .border(1.5.dp, HrColors.BorderAccentDim, HrShapes.ButtonSmall)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.nutrition_add_meal_button), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HrColors.Accent)
            }
        }

        uiState.meals.forEach { meal ->
            MealRow(meal = meal, onRemove = { viewModel.removeMeal(meal) })
        }

        PlannedMealsSection(
            hasActivePlan = uiState.hasActivePlan,
            plannedMeals = uiState.plannedMeals,
            onOpenCreatePlan = onOpenCreatePlan,
            onOpenPlan = onOpenPlan,
        )

        LibraryLinkRow(onClick = onOpenLibrary)

        Text(
            text = stringResource(R.string.nutrition_footer),
            fontFamily = HrBody,
            fontSize = 11.sp,
            color = HrColors.TextFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LibraryLinkRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardSmall)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardSmall)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.nutrition_library_link_title), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HrColors.TextHi)
            Text(
                text = stringResource(R.string.nutrition_library_link_subtitle),
                fontFamily = HrBody,
                fontSize = 12.sp,
                color = HrColors.TextLow,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(text = "›", fontFamily = HrBody, fontSize = 16.sp, color = HrColors.TextFaint)
    }
}

/** Planned meals (from the active [com.fitviet.app.data.local.entity.UserMealPlanEntity]) shown
 * separately from the "Bữa ăn hôm nay" log above — a planned meal is never rendered as if it were
 * eaten just because it's in the plan; see [PlannedMealStatus]. Continuity carry-over as of Gate
 * 5a — see this file's own header doc for why it survives despite having no mock equivalent. */
@Composable
private fun PlannedMealsSection(
    hasActivePlan: Boolean,
    plannedMeals: List<PlannedMealUiItem>,
    onOpenCreatePlan: () -> Unit,
    onOpenPlan: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.nutrition_planned_title), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HrColors.TextHi)
            if (hasActivePlan) {
                Text(
                    text = stringResource(R.string.nutrition_view_plan),
                    fontFamily = HrBody,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = HrColors.Accent,
                    modifier = Modifier.clickable(onClick = onOpenPlan),
                )
            }
        }
        if (!hasActivePlan) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(HrShapes.CardSmall)
                    .background(HrColors.SurfaceAccent)
                    .border(1.dp, HrColors.BorderAccentDim, HrShapes.CardSmall)
                    .clickable(onClick = onOpenCreatePlan)
                    .padding(Dimens.CardPaddingSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.nutrition_planned_empty),
                    fontFamily = HrBody,
                    fontSize = 13.sp,
                    color = HrColors.TextMid,
                    modifier = Modifier.weight(1f),
                )
                Text(text = stringResource(R.string.nutrition_quick_create_plan), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HrColors.Accent)
            }
        } else if (plannedMeals.isEmpty()) {
            // An active plan exists but today's slot happens to have no meals scheduled (or
            // couldn't be resolved) — distinct from "no plan at all," so this reads as "check the
            // full week" rather than re-offering the create-plan CTA for a plan that already exists.
            Text(
                text = stringResource(R.string.nutrition_planned_empty_today),
                fontFamily = HrBody,
                fontSize = 13.sp,
                color = HrColors.TextLow,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                plannedMeals.forEach { item -> PlannedMealRow(item) }
            }
        }
    }
}

@Composable
private fun PlannedMealRow(item: PlannedMealUiItem) {
    val isEaten = item.status == PlannedMealStatus.EATEN
    val isNext = item.status == PlannedMealStatus.NEXT
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardSmall)
            .background(if (isNext) HrColors.SurfaceAccent else HrColors.Surface)
            .border(if (isNext) Dimens.SelectedBorderWidth else Dimens.IdleBorderWidth, if (isNext) HrColors.Accent else HrColors.Border, HrShapes.CardSmall)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.slot, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp, color = HrColors.TextFaint)
            Text(
                text = item.recipeName,
                fontFamily = HrBody,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = if (isEaten) HrColors.TextLow else HrColors.TextHi,
            )
        }
        if (isEaten) {
            Text(text = stringResource(R.string.nutrition_planned_status_eaten), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = HrColors.Accent)
        } else if (isNext) {
            Text(text = stringResource(R.string.nutrition_planned_status_next), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = HrColors.Accent)
        }
        Text(
            text = stringResource(R.string.nutrition_meal_kcal, formatVi(item.kcal)),
            fontFamily = HrDisplay,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = if (isEaten) HrColors.TextLow else HrColors.TextHi,
        )
    }
}

@Composable
private fun SummaryCard(totals: NutritionTotals) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SummaryCardShape)
            .background(Brush.linearGradient(listOf(HrColors.GradientCardStart, HrColors.GradientCardEnd), start = Offset(0f, 0f)))
            .border(1.dp, HrColors.BorderGradient, SummaryCardShape)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KcalRing(kcal = totals.kcal, percent = totals.kcalPercent)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            MacroBar(
                label = stringResource(R.string.nutrition_macro_protein),
                value = stringResource(R.string.nutrition_macro_value, totals.proteinG, NutritionGoals.PROTEIN_G),
                percent = totals.proteinPercent,
                color = HrColors.Accent,
            )
            MacroBar(
                label = stringResource(R.string.nutrition_macro_carb),
                value = stringResource(R.string.nutrition_macro_value, totals.carbG, NutritionGoals.CARB_G),
                percent = totals.carbPercent,
                color = HrColors.MacroCarb,
            )
            MacroBar(
                label = stringResource(R.string.nutrition_macro_fat),
                value = stringResource(R.string.nutrition_macro_value, totals.fatG, NutritionGoals.FAT_G),
                percent = totals.fatPercent,
                color = HrColors.MacroFat,
            )
        }
    }
}

// The mock's own literal radius (20px) — between [HrShapes.CardRegular] (17dp) and [HrShapes.CardHero]
// (24dp), not an exact match for either, so its own local shape rather than forcing a snap the way
// Gate 1a's 16-18 -> CardRegular resolution did for an actual range.
private val SummaryCardShape = RoundedCornerShape(20.dp)

@Composable
private fun KcalRing(kcal: Int, percent: Int) {
    val reducedMotion = rememberReducedMotion()
    // Gate D2 — grows in from 0 on first composition, then re-animates to any later percent
    // change (e.g. logging another meal). animateFloatAsState was tried first but REJECTED on
    // review: its very first rendered value already equals the initial target (no 0->target
    // transition on first composition, only on later target changes), which would have defeated
    // the entry-animation requirement entirely. An Animatable started at 0f, animated via
    // LaunchedEffect(percent, ...), correctly covers both first-composition AND later changes.
    val animatedPercent = remember { Animatable(0f) }
    LaunchedEffect(percent, reducedMotion) {
        animatedPercent.animateTo(percent.toFloat(), tween(durationMillis = if (reducedMotion) 0 else 700))
    }
    // 104dp outer / 82dp inner hole per the mock's literal px values; the accent glow behind the
    // arc is Gate D2's own Premium Moments addition (not in the mock's static markup) — kept and
    // re-skinned here for the same "preserve existing motion" reasoning as RestContent's ring.
    Box(modifier = Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 11.dp.toPx()
            // drawArc's default topLeft/size centers the stroke on the box's INSCRIBED circle, not
            // one inset by half the stroke width — left at defaults this rendered 115dp outer/93dp
            // hole against the mock's 104/82, and put the glow's centerline 5.5dp inside the crisp
            // arc's own (the glow block below already inset correctly, which is what exposed the
            // mismatch on review). Insetting the rect by strokeWidth/2 on both axes fixes both.
            val inset = strokeWidth / 2f
            val arcTopLeft = Offset(inset, inset)
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(color = HrColors.Border, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(strokeWidth))
            val sweepAngle = 360f * (animatedPercent.value / 100f)
            if (sweepAngle > 0f) {
                drawIntoCanvas { canvas ->
                    val glowPaint = FrameworkPaint().apply {
                        isAntiAlias = true
                        style = FrameworkPaint.Style.STROKE
                        this.strokeWidth = strokeWidth * 2.2f
                        color = HrColors.Accent.copy(alpha = 0.35f).toArgb()
                        maskFilter = BlurMaskFilter(strokeWidth, BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.nativeCanvas.drawArc(
                        inset,
                        inset,
                        size.width - inset,
                        size.height - inset,
                        -90f,
                        sweepAngle,
                        false,
                        glowPaint,
                    )
                }
                drawArc(color = HrColors.Accent, startAngle = -90f, sweepAngle = sweepAngle, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(strokeWidth))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = formatVi(kcal), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = HrColors.TextHi)
            Text(
                text = stringResource(R.string.nutrition_kcal_of_goal, formatVi(NutritionGoals.KCAL)),
                fontFamily = HrBody,
                fontSize = 10.sp,
                color = HrColors.TextLow,
            )
        }
    }
}

@Composable
private fun MacroBar(label: String, value: String, percent: Int, color: Color) {
    val reducedMotion = rememberReducedMotion()
    val animatedPercent = remember { Animatable(0f) }
    LaunchedEffect(percent, reducedMotion) {
        animatedPercent.animateTo(percent.toFloat(), tween(durationMillis = if (reducedMotion) 0 else 700))
    }
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, fontFamily = HrBody, fontSize = 12.sp, color = HrColors.TextMid)
            Text(text = value, fontFamily = HrBody, fontSize = 11.sp, color = HrColors.TextLow)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp)
                .height(5.dp)
                .clip(PillShape)
                .background(HrColors.Border),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((animatedPercent.value / 100f).coerceIn(0f, 1f))
                    .height(5.dp)
                    .clip(PillShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun MealRow(meal: MealEntity, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardSmall)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardSmall)
            // Asymmetric end padding (vs. the mock's flat 16px) — the × glyph sits inside its own
            // 44dp touch target for a real hit area, so a flat 16dp end padding would land its
            // visual center ~38dp from the card edge against the mock's ~20dp; 4dp end padding
            // plus the touch box's own centering lands the glyph in roughly the right place while
            // keeping the full touch target.
            .padding(start = 16.dp, end = 4.dp, top = 13.dp, bottom = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // `meal.slot` displayed as-is (no case transform / vocabulary remap) — the mock's own
            // 3-bucket SÁNG/TRƯA/TỐI vocabulary doesn't reconcile with this app's real slot data
            // (4 buckets, including "Bữa phụ" with no "Bữa tối" at all); reconciling that is Gate
            // 5c's own scoped work, not this token-rebuild gate's.
            Text(text = meal.slot, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp, color = HrColors.TextFaint)
            Text(
                text = meal.nameVi,
                fontFamily = HrBody,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = HrColors.TextHi,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = stringResource(R.string.nutrition_meal_kcal, formatVi(meal.kcal)),
            fontFamily = HrBody,
            fontSize = 13.sp,
            color = HrColors.TextMid,
        )
        Box(
            modifier = Modifier
                .size(Dimens.MinTouchTarget)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "×", fontFamily = HrBody, fontSize = 18.sp, color = HrColors.TextFaint)
        }
    }
}
