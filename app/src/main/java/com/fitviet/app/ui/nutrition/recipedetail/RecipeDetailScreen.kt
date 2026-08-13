package com.fitviet.app.ui.nutrition.recipedetail

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.repository.RecipeDetail
import com.fitviet.app.data.repository.RecipeIngredientDetail
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.formatVi
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RecipeDetailScreen(
    viewModel: RecipeDetailViewModel,
    recipeId: Long,
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val detail = uiState.detail

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
            with(sharedTransitionScope) {
                Text(
                    text = detail?.nameVi.orEmpty(),
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // Keyed by the nav-arg recipeId (available from frame 1), NOT detail?.recipeId
                    // (null while loading) — a null-derived key on the first frame(s) would
                    // mismatch the source row's already-real-id key (NutritionLibraryScreen's
                    // RecipeRow as of Gate 5b-i) and break the shared-element continuity right as
                    // the transition starts.
                    modifier = Modifier
                        .weight(1f)
                        .sharedElement(
                            rememberSharedContentState(key = "recipe-title-$recipeId"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        ),
                )
            }
            if (detail != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(SurfaceCard)
                        .border(1.dp, CardBorder, MaterialTheme.shapes.small)
                        .clickable(onClick = viewModel::toggleFavorite),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (uiState.isFavorite) "♥" else "♡",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (uiState.isFavorite) Accent else TextMuted,
                    )
                }
            }
        }

        if (detail != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(Dimens.SectionGapLarge),
            ) {
                item { RecipeCover(detail = detail, totalKcal = uiState.totals.kcal) }
                item {
                    ServingsAndVariants(
                        detail = detail,
                        servings = uiState.servings,
                        selectedVariantCode = uiState.selectedVariantCode,
                        onIncrement = viewModel::incrementServings,
                        onDecrement = viewModel::decrementServings,
                        onSelectVariant = viewModel::selectVariant,
                    )
                }
                item {
                    Text(
                        text = stringResource(
                            R.string.nutrition_recipe_macros,
                            uiState.totals.proteinG,
                            uiState.totals.carbG,
                            uiState.totals.fatG,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                    )
                }
                item { SectionHeader(text = stringResource(R.string.nutrition_ingredients_title)) }
                items(detail.ingredients, key = { it.foodId }) { ingredient ->
                    IngredientRow(ingredient = ingredient, servings = uiState.servings, baseServings = detail.baseServings)
                }
                item { SectionHeader(text = stringResource(R.string.nutrition_instructions_title)) }
                itemsIndexed(detail.instructions) { index, step ->
                    InstructionRow(stepNumber = index + 1, text = step)
                }
            }
        }
    }
}

@Composable
private fun RecipeCover(detail: RecipeDetail, totalKcal: Int) {
    val gradient = remember(detail.nameVi) {
        val index = abs(detail.nameVi.hashCode().let { if (it == Int.MIN_VALUE) 0 else it }) % RECIPE_COVER_GRADIENTS.size
        RECIPE_COVER_GRADIENTS[index]
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(MaterialTheme.shapes.large)
                .background(Brush.linearGradient(listOf(gradient.first, gradient.second))),
        ) {
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .clip(PillShape)
                    .background(Accent)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.nutrition_meal_kcal, formatVi(totalKcal)),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = Anton),
                    color = OnAccent,
                )
            }
        }
        Text(
            text = stringResource(R.string.nutrition_recipe_prep_cook, detail.prepTimeMinutes, detail.cookTimeMinutes),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServingsAndVariants(
    detail: RecipeDetail,
    servings: Int,
    selectedVariantCode: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSelectVariant: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(R.string.nutrition_servings_label), style = MaterialTheme.typography.labelLarge, color = TextMuted)
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(SurfaceCard)
                    .border(1.dp, CardBorder, PillShape),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepperButton(symbol = "−", onClick = onDecrement)
                Text(
                    text = formatVi(servings),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
                StepperButton(symbol = "+", onClick = onIncrement)
            }
        }
        if (detail.variants.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                detail.variants.forEach { variant ->
                    val isSelected = variant.code == selectedVariantCode
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(if (isSelected) Accent else SurfaceCard)
                            .border(1.dp, if (isSelected) Accent else CardBorder, PillShape)
                            .clickable { onSelectVariant(variant.code) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = variantLabel(variant.code),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) OnAccent else TextMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, style = MaterialTheme.typography.titleMedium, color = Accent)
    }
}

@Composable
private fun variantLabel(code: String): String = stringResource(
    when (code) {
        "CUT" -> R.string.nutrition_recipe_variant_cut
        "HIGH_PROTEIN" -> R.string.nutrition_recipe_variant_high_protein
        "BULK" -> R.string.nutrition_recipe_variant_bulk
        else -> R.string.nutrition_recipe_variant_standard
    },
)

@Composable
private fun SectionHeader(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
}

@Composable
private fun IngredientRow(ingredient: RecipeIngredientDetail, servings: Int, baseServings: Int) {
    val scaledGrams = (ingredient.gramsAtBaseServings * servings / baseServings.coerceAtLeast(1)).roundToInt()
    val showDisplayQuantity = servings == baseServings && ingredient.displayQuantity != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AccentSurfaceSelected)
                .border(1.dp, CardBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = ingredient.foodNameVi.take(1), style = MaterialTheme.typography.labelLarge, color = Accent)
        }
        Text(
            text = ingredient.foodNameVi,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (showDisplayQuantity) ingredient.displayQuantity!! else stringResource(R.string.nutrition_recipe_grams, scaledGrams),
            style = MaterialTheme.typography.labelLarge,
            color = TextMuted,
        )
    }
}

@Composable
private fun InstructionRow(stepNumber: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(SurfaceCard)
                .border(1.dp, CardBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = formatVi(stepNumber), style = MaterialTheme.typography.labelMedium, color = Accent)
        }
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextFaint, modifier = Modifier.padding(top = 3.dp))
    }
}

private val RECIPE_COVER_GRADIENTS = listOf(
    Color(0xFF14291B) to Color(0xFF101B14),
    Color(0xFF1B2A20) to Color(0xFF0E1712),
    Color(0xFF16241C) to Color(0xFF0B120D),
    AccentSurfaceSelected to Color(0xFF0F1310),
)
