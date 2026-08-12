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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.MealEntity
import com.fitviet.app.domain.NutritionGoals
import com.fitviet.app.domain.NutritionTotals
import com.fitviet.app.ui.common.rememberReducedMotion
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentBorder
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.DeepSurface2
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HeroGradientEnd
import com.fitviet.app.ui.theme.HeroGradientStart
import com.fitviet.app.ui.theme.MacroBarCarb
import com.fitviet.app.ui.theme.MacroBarFat
import com.fitviet.app.ui.theme.MacroBarProtein
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextBody
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.formatVi

@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel,
    onOpenDiscover: () -> Unit,
    onOpenFoods: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenCreatePlan: () -> Unit,
    onOpenPlan: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPage)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGapLarge),
    ) {
        Text(text = stringResource(R.string.nutrition_title), style = MaterialTheme.typography.headlineMedium)
        SummaryCard(totals = uiState.totals)

        QuickActionsRow(
            onOpenDiscover = onOpenDiscover,
            onOpenFoods = onOpenFoods,
            onOpenTemplates = onOpenTemplates,
            onOpenCreatePlan = onOpenCreatePlan,
        )

        PlannedMealsSection(
            hasActivePlan = uiState.hasActivePlan,
            plannedMeals = uiState.plannedMeals,
            onOpenCreatePlan = onOpenCreatePlan,
            onOpenPlan = onOpenPlan,
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(R.string.nutrition_meals_title), style = MaterialTheme.typography.titleSmall)
            Box(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .border(1.dp, AccentBorder, MaterialTheme.shapes.small)
                    .clickable(onClick = viewModel::addNextPreset)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.nutrition_add_meal, uiState.nextPresetName),
                    style = MaterialTheme.typography.labelLarge,
                    color = Accent,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            uiState.meals.forEach { meal ->
                MealRow(meal = meal, onRemove = { viewModel.removeMeal(meal) })
            }
        }
        Text(
            text = stringResource(R.string.nutrition_footer),
            style = MaterialTheme.typography.labelMedium,
            color = TextFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun QuickActionsRow(
    onOpenDiscover: () -> Unit,
    onOpenFoods: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenCreatePlan: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // First card = gradient hero style, matching Dashboard's HeroCard treatment.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd), start = Offset(0f, 0f)))
                .clickable(onClick = onOpenDiscover)
                .padding(Dimens.CardPaddingLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(text = stringResource(R.string.nutrition_quick_discover), style = MaterialTheme.typography.titleSmall, color = Accent)
                Text(
                    text = stringResource(R.string.nutrition_quick_discover_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(text = "›", style = MaterialTheme.typography.titleMedium, color = Accent)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickActionChip(
                text = stringResource(R.string.nutrition_quick_foods),
                onClick = onOpenFoods,
                modifier = Modifier.weight(1f),
            )
            QuickActionChip(
                text = stringResource(R.string.nutrition_quick_templates),
                onClick = onOpenTemplates,
                modifier = Modifier.weight(1f),
            )
            QuickActionChip(
                text = stringResource(R.string.nutrition_quick_create_plan),
                onClick = onOpenCreatePlan,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickActionChip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = Dimens.MinTouchTarget)
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = TextPrimary, textAlign = TextAlign.Center)
    }
}

/** Planned meals (from the active [com.fitviet.app.data.local.entity.UserMealPlanEntity]) shown
 * separately from the "Bữa ăn hôm nay" log above — a planned meal is never rendered as if it were
 * eaten just because it's in the plan; see [PlannedMealStatus]. */
@Composable
private fun PlannedMealsSection(
    hasActivePlan: Boolean,
    plannedMeals: List<PlannedMealUiItem>,
    onOpenCreatePlan: () -> Unit,
    onOpenPlan: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.nutrition_planned_title), style = MaterialTheme.typography.titleSmall)
            if (hasActivePlan) {
                Text(
                    text = stringResource(R.string.nutrition_view_plan),
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                    modifier = Modifier.clickable(onClick = onOpenPlan),
                )
            }
        }
        if (!hasActivePlan) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(AccentSurfaceSelected)
                    .border(1.dp, AccentBorder, MaterialTheme.shapes.medium)
                    .clickable(onClick = onOpenCreatePlan)
                    .padding(Dimens.CardPaddingSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.nutrition_planned_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextBody,
                    modifier = Modifier.weight(1f),
                )
                Text(text = stringResource(R.string.nutrition_quick_create_plan), style = MaterialTheme.typography.labelLarge, color = Accent)
            }
        } else if (plannedMeals.isEmpty()) {
            // An active plan exists but today's slot happens to have no meals scheduled (or
            // couldn't be resolved) — distinct from "no plan at all," so this reads as "check the
            // full week" rather than re-offering the create-plan CTA for a plan that already exists.
            Text(
                text = stringResource(R.string.nutrition_planned_empty_today),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                plannedMeals.forEach { item -> PlannedMealRow(item) }
            }
        }
    }
}

private val TextBodyForPlanned = com.fitviet.app.ui.theme.TextBody

@Composable
private fun PlannedMealRow(item: PlannedMealUiItem) {
    val isEaten = item.status == PlannedMealStatus.EATEN
    val isNext = item.status == PlannedMealStatus.NEXT
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (isNext) AccentSurfaceSelected else SurfaceCard)
            .border(1.dp, if (isNext) Accent else CardBorder, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.slot, style = MaterialTheme.typography.labelSmall, color = TextFaint)
            Text(
                text = item.recipeName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isEaten) TextMuted else TextPrimary,
            )
        }
        if (isEaten) {
            Text(text = stringResource(R.string.nutrition_planned_status_eaten), style = MaterialTheme.typography.labelSmall, color = Accent)
        } else if (isNext) {
            Text(text = stringResource(R.string.nutrition_planned_status_next), style = MaterialTheme.typography.labelSmall, color = Accent)
        }
        Text(
            text = stringResource(R.string.nutrition_meal_kcal, formatVi(item.kcal)),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = Anton),
            color = if (isEaten) TextMuted else TextPrimary,
        )
    }
}

@Composable
private fun SummaryCard(totals: NutritionTotals) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large)
            .padding(Dimens.CardPaddingLarge),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KcalRing(kcal = totals.kcal, percent = totals.kcalPercent)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MacroBar(
                label = stringResource(R.string.nutrition_macro_protein),
                value = stringResource(R.string.nutrition_macro_value, totals.proteinG, NutritionGoals.PROTEIN_G),
                percent = totals.proteinPercent,
                color = MacroBarProtein,
            )
            MacroBar(
                label = stringResource(R.string.nutrition_macro_carb),
                value = stringResource(R.string.nutrition_macro_value, totals.carbG, NutritionGoals.CARB_G),
                percent = totals.carbPercent,
                color = MacroBarCarb,
            )
            MacroBar(
                label = stringResource(R.string.nutrition_macro_fat),
                value = stringResource(R.string.nutrition_macro_value, totals.fatG, NutritionGoals.FAT_G),
                percent = totals.fatPercent,
                color = MacroBarFat,
            )
        }
    }
}

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
    Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 11.dp.toPx()
            drawArc(color = CardBorder, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(strokeWidth))
            val sweepAngle = 360f * (animatedPercent.value / 100f)
            if (sweepAngle > 0f) {
                // Soft accent glow behind the crisp arc — same native BlurMaskFilter idiom as
                // com.fitviet.app.ui.theme.premiumShadow, since Compose's own DrawScope has no
                // blur primitive of its own.
                drawIntoCanvas { canvas ->
                    val glowPaint = FrameworkPaint().apply {
                        isAntiAlias = true
                        style = FrameworkPaint.Style.STROKE
                        this.strokeWidth = strokeWidth * 2.2f
                        color = Accent.copy(alpha = 0.35f).toArgb()
                        maskFilter = BlurMaskFilter(strokeWidth, BlurMaskFilter.Blur.NORMAL)
                    }
                    val inset = strokeWidth / 2f
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
                drawArc(color = Accent, startAngle = -90f, sweepAngle = sweepAngle, useCenter = false, style = Stroke(strokeWidth))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = formatVi(kcal), style = MaterialTheme.typography.titleMedium.copy(fontFamily = Anton))
            Text(
                text = stringResource(R.string.nutrition_kcal_of_goal, formatVi(NutritionGoals.KCAL)),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
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
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(text = value, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(5.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(DeepSurface2),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((animatedPercent.value / 100f).coerceIn(0f, 1f))
                    .height(5.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
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
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = meal.slot, style = MaterialTheme.typography.labelSmall, color = TextFaint)
            Text(text = meal.nameVi, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        }
        Text(
            text = stringResource(R.string.nutrition_meal_kcal, formatVi(meal.kcal)),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = Anton),
            color = TextMuted,
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .border(1.dp, CardBorder, MaterialTheme.shapes.extraSmall),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "×", style = MaterialTheme.typography.bodyMedium, color = TextFaint)
            }
        }
    }
}
