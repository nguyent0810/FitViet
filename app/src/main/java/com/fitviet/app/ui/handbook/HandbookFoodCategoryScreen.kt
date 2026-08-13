package com.fitviet.app.ui.handbook

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.FoodEntity
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.formatOneDecimal

/** Gate E7 — the foods inside one ingredient category (real macros/description card content,
 * moved here unchanged from the old flat by-category list on HandbookScreen).
 *
 * Redesign Gate 5e — still deliberately on legacy tokens, not an oversight: this screen's own
 * food-knowledge content (categories/macros/descriptions) was flagged during Phase 3's plan-check
 * as a candidate for absorption into the Nutrition module's own consolidated library screen "in a
 * later phase." Phase 5 was that later phase and kept this screen instead — see
 * [com.fitviet.app.ui.nutrition.library.NutritionLibraryScreen]'s own Gate 5b-i doc comment: the
 * library surfaces individual foods by name search only (Judgment Call A), so browsing by category,
 * [FoodEntity.nameEn], and [FoodEntity.descriptionVi] stay exclusive to this tab. So there is no
 * future gate expected to re-skin or absorb this screen; it stays legacy-token by design until a
 * real reason to revisit it exists. */
@Composable
fun HandbookFoodCategoryScreen(viewModel: HandbookFoodCategoryViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundPage)) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BackRow(onBack = onBack)
            Text(text = uiState.category, style = MaterialTheme.typography.headlineMedium)
        }
        if (uiState.foods.isEmpty()) {
            Text(
                text = stringResource(R.string.handbook_foods_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal).padding(top = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = Dimens.ScreenPaddingHorizontal),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.foods, key = { it.id }) { food -> FoodRow(food = food) }
            }
        }
    }
}

@Composable
private fun BackRow(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
        Text(text = stringResource(R.string.handbook_title), style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}

@Composable
private fun FoodRow(food: FoodEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = food.nameVi, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text(text = food.nameEn, style = MaterialTheme.typography.labelSmall, color = TextFaint)
            }
            Text(
                text = stringResource(R.string.handbook_food_kcal, food.kcalPer100g),
                style = MaterialTheme.typography.labelLarge,
                color = Accent,
            )
        }
        Text(text = food.descriptionVi, style = MaterialTheme.typography.labelMedium, color = TextFaint)
        Text(
            text = stringResource(
                R.string.handbook_food_macros,
                formatOneDecimal(food.proteinG),
                formatOneDecimal(food.carbG),
                formatOneDecimal(food.fatG),
            ),
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
        )
    }
}
