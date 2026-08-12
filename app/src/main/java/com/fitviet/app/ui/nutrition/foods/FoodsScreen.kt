package com.fitviet.app.ui.nutrition.foods

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.FoodEntity
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.formatVi

@Composable
fun FoodsScreen(viewModel: FoodsViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundPage)) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(SurfaceCard)
                    .border(1.dp, CardBorder, MaterialTheme.shapes.small)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "‹", style = MaterialTheme.typography.titleMedium, color = TextMuted)
            }
            Text(text = stringResource(R.string.nutrition_quick_foods), style = MaterialTheme.typography.headlineMedium)
        }

        CategoryChips(
            categories = uiState.categories,
            selected = uiState.selectedCategory,
            onSelect = viewModel::selectCategory,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal),
        )

        if (uiState.foods.isEmpty()) {
            Text(
                text = stringResource(R.string.nutrition_empty_foods),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = Dimens.ScreenPaddingHorizontal,
                    vertical = 4.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.ListGapLarge),
            ) {
                items(uiState.foods, key = { it.id }) { food ->
                    val dimmed = uiState.selectedCategory != null && food.category != uiState.selectedCategory
                    FoodRow(food = food, modifier = Modifier.alpha(if (dimmed) 0.4f else 1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(categories: List<String>, selected: String?, onSelect: (String?) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryChip(label = stringResource(R.string.nutrition_foods_category_all), isSelected = selected == null, onClick = { onSelect(null) })
        categories.forEach { category ->
            CategoryChip(label = category, isSelected = selected == category, onClick = { onSelect(category) })
        }
    }
}

@Composable
private fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(if (isSelected) Accent else SurfaceCard)
            .border(1.dp, if (isSelected) Accent else CardBorder, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) OnAccent else TextMuted,
        )
    }
}

@Composable
private fun FoodRow(food: FoodEntity, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // No real food photography ships with this app (per the design brief's asset policy) — a
        // deterministic category-color swatch stands in for the doc's "thumb" rather than a
        // placeholder image asset that doesn't exist.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AccentSurfaceSelected)
                .border(1.dp, CardBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = food.nameVi.take(1), style = MaterialTheme.typography.labelLarge, color = Accent)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = food.nameVi, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = stringResource(
                    R.string.nutrition_food_macros_per_100g,
                    food.proteinG.toInt(),
                    food.carbG.toInt(),
                    food.fatG.toInt(),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
        Text(
            text = stringResource(R.string.nutrition_food_per_100g, formatVi(food.kcalPer100g)),
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = Anton),
            color = Accent,
        )
    }
}
