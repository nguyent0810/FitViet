package com.fitviet.app.ui.nutrition

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.MealEntity
import com.fitviet.app.domain.NutritionGoals
import com.fitviet.app.domain.NutritionTotals
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentBorder
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.DeepSurface2
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.MacroBarCarb
import com.fitviet.app.ui.theme.MacroBarFat
import com.fitviet.app.ui.theme.MacroBarProtein
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.formatVi

@Composable
fun NutritionScreen(viewModel: NutritionViewModel) {
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(R.string.nutrition_meals_title), style = MaterialTheme.typography.titleSmall)
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .border(1.dp, AccentBorder, MaterialTheme.shapes.small)
                    .clickable(onClick = viewModel::addNextPreset)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
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
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
    Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 11.dp.toPx()
            drawArc(color = CardBorder, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(strokeWidth))
            if (percent > 0) {
                drawArc(
                    color = Accent,
                    startAngle = -90f,
                    sweepAngle = 360f * (percent / 100f),
                    useCenter = false,
                    style = Stroke(strokeWidth),
                )
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
                    .fillMaxWidth(percent / 100f)
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
                .size(24.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .border(1.dp, CardBorder, MaterialTheme.shapes.extraSmall)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "×", style = MaterialTheme.typography.bodyMedium, color = TextFaint)
        }
    }
}
