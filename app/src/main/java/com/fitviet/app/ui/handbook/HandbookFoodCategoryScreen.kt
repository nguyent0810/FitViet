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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.FoodEntity
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.util.formatOneDecimal

/** Gate E7 — the foods inside one ingredient category (real macros/description card content,
 * moved here unchanged from the old flat by-category list on HandbookScreen).
 *
 * Redesign Gate 5e deliberately kept this screen's own scope as-is (not absorbed into the
 * Nutrition module's library screen) — see [com.fitviet.app.ui.nutrition.library
 * .NutritionLibraryScreen]'s own Gate 5b-i doc comment: the library surfaces individual foods by
 * name search only (Judgment Call A), so browsing by category, [FoodEntity.nameEn], and
 * [FoodEntity.descriptionVi] stay exclusive to this tab. That scope decision still stands. Gate 8e
 * re-skinned this screen's *tokens* to [HrColors]/[HrBody]/[HrDisplay]/[HrShapes] — it was the last
 * live (non-dead-code) screen still on the legacy palette. */
@Composable
fun HandbookFoodCategoryScreen(viewModel: HandbookFoodCategoryViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(HrColors.Bg)) {
        Column(
            modifier = Modifier.padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BackRow(onBack = onBack)
            Text(text = uiState.category, fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = HrColors.TextHi)
        }
        if (uiState.foods.isEmpty()) {
            Text(
                text = stringResource(R.string.handbook_foods_empty),
                fontFamily = HrBody,
                fontSize = 14.sp,
                color = HrColors.TextLow,
                modifier = Modifier.padding(horizontal = HrDimens.ScreenPaddingHorizontal).padding(top = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = HrDimens.ScreenPaddingHorizontal),
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
                .clip(HrShapes.ButtonSmall)
                .background(HrColors.Surface)
                .border(1.dp, HrColors.Border, HrShapes.ButtonSmall)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "‹", fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = HrColors.TextLow)
        }
        Text(text = stringResource(R.string.handbook_title), fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextLow)
    }
}

@Composable
private fun FoodRow(food: FoodEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = food.nameVi, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HrColors.TextHi)
                Text(text = food.nameEn, fontFamily = HrBody, fontSize = 11.sp, color = HrColors.TextFaint)
            }
            Text(
                text = stringResource(R.string.handbook_food_kcal, food.kcalPer100g),
                fontFamily = HrBody,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = HrColors.Accent,
            )
        }
        Text(text = food.descriptionVi, fontFamily = HrBody, fontSize = 12.sp, color = HrColors.TextFaint)
        Text(
            text = stringResource(
                R.string.handbook_food_macros,
                formatOneDecimal(food.proteinG),
                formatOneDecimal(food.carbG),
                formatOneDecimal(food.fatG),
            ),
            fontFamily = HrBody,
            fontSize = 12.sp,
            color = HrColors.TextLow,
        )
    }
}
