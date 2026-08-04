package com.fitviet.app.ui.nutrition

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.MealEntity
import com.fitviet.app.domain.NutritionStats
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentBorder
import com.fitviet.app.ui.theme.Anton
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenPaddingHorizontal),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text(text = stringResource(R.string.nutrition_title), style = MaterialTheme.typography.headlineMedium) }
        item { KcalAndMacrosCard(stats = uiState.stats) }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.nutrition_meals_title), style = MaterialTheme.typography.titleMedium)
                Box(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clip(MaterialTheme.shapes.small)
                        .border(1.dp, AccentBorder, MaterialTheme.shapes.small)
                        .clickable(onClick = viewModel::openFoodPicker)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(R.string.nutrition_add_meal), style = MaterialTheme.typography.labelLarge, color = Accent)
                }
            }
        }
        if (uiState.meals.isEmpty()) {
            item {
                Text(text = stringResource(R.string.nutrition_meals_empty), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            }
        } else {
            items(uiState.meals, key = { it.id }) { meal ->
                MealRow(meal = meal, onRemove = { viewModel.removeMeal(meal) })
            }
        }
        item {
            Text(
                text = stringResource(R.string.nutrition_footer),
                style = MaterialTheme.typography.labelMedium,
                color = TextFaint,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = TextAlign.Center,
            )
        }
    }

    if (uiState.isFoodPickerOpen) {
        FoodPickerSheet(onSelect = viewModel::addFood, onDismiss = viewModel::closeFoodPicker)
    }
}

@Composable
private fun KcalAndMacrosCard(stats: NutritionStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large)
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KcalRing(pct = stats.kcalPct, kcalTotal = stats.kcalTotal, kcalGoal = stats.kcalGoal)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MacroBar(label = stringResource(R.string.nutrition_macro_protein), grams = stats.proteinG, goal = stats.proteinGoalG, pct = stats.proteinPct, color = MacroBarProtein)
            MacroBar(label = stringResource(R.string.nutrition_macro_carb), grams = stats.carbG, goal = stats.carbGoalG, pct = stats.carbPct, color = MacroBarCarb)
            MacroBar(label = stringResource(R.string.nutrition_macro_fat), grams = stats.fatG, goal = stats.fatGoalG, pct = stats.fatPct, color = MacroBarFat)
        }
    }
}

@Composable
private fun KcalRing(pct: Int, kcalTotal: Int, kcalGoal: Int) {
    Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(96.dp)) {
            val strokeWidth = 11.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            drawArc(
                color = DeepSurface2,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth),
                size = arcSize,
                topLeft = topLeft,
            )
            drawArc(
                color = Accent,
                startAngle = -90f,
                sweepAngle = 360f * (pct / 100f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = arcSize,
                topLeft = topLeft,
            )
        }
        Column(
            modifier = Modifier.size(74.dp).clip(CircleShape).background(SurfaceCard),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = formatVi(kcalTotal), style = MaterialTheme.typography.titleMedium.copy(fontFamily = Anton), color = TextPrimary)
            Text(text = stringResource(R.string.nutrition_kcal_goal, formatVi(kcalGoal)), style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

@Composable
private fun MacroBar(label: String, grams: Int, goal: Int, pct: Int, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Text(text = stringResource(R.string.nutrition_macro_label, grams, goal), style = MaterialTheme.typography.labelMedium, color = TextPrimary)
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
                    .fillMaxHeight()
                    .fillMaxWidth(pct / 100f)
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
            val slotLabel = if (meal.slot == ADDED_MEAL_SLOT_KEY) stringResource(R.string.nutrition_slot_extra) else meal.slot
            Text(text = slotLabel, style = MaterialTheme.typography.labelMedium, color = TextFaint)
            Text(text = meal.nameVi, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        }
        Text(
            text = stringResource(R.string.nutrition_kcal_unit, meal.kcal),
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = Anton),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodPickerSheet(onSelect: (FoodPreset) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = stringResource(R.string.nutrition_picker_title), style = MaterialTheme.typography.titleMedium)
            FOOD_PRESETS.forEach { preset ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(SurfaceCard)
                        .border(1.dp, CardBorder, MaterialTheme.shapes.medium)
                        .clickable { onSelect(preset) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = preset.nameVi, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Text(text = stringResource(R.string.nutrition_kcal_unit, preset.kcal), style = MaterialTheme.typography.labelLarge, color = TextMuted)
                }
            }
        }
    }
}
