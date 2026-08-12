package com.fitviet.app.ui.nutrition.discover

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.domain.RecipeWithNutrition
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    onBack: () -> Unit,
    onOpenRecipe: (Long) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
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
            Text(text = stringResource(R.string.nutrition_quick_discover), style = MaterialTheme.typography.headlineMedium)
        }

        Column(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SearchField(query = uiState.searchQuery, onQueryChange = viewModel::onSearchQueryChange)
            FilterChipsRow(selectedTags = uiState.selectedTags, onToggle = viewModel::toggleTag)
        }

        if (uiState.recipes.isEmpty()) {
            Text(
                text = stringResource(R.string.nutrition_empty_discover),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 24.dp),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(top = 14.dp),
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(Dimens.ListGapLarge),
                verticalArrangement = Arrangement.spacedBy(Dimens.ListGapLarge),
            ) {
                items(uiState.recipes, key = { it.recipeId }) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        onClick = { onOpenRecipe(recipe.recipeId) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.small)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (query.isEmpty()) {
            Text(text = stringResource(R.string.nutrition_search_placeholder), style = MaterialTheme.typography.bodyLarge, color = TextFaint)
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = MaterialTheme.typography.bodyLarge.fontSize, color = TextPrimary),
            cursorBrush = SolidColor(Accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipsRow(selectedTags: Set<String>, onToggle: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DISCOVER_FILTER_CHIPS.forEach { chip ->
            val isSelected = chip.tag in selectedTags
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(if (isSelected) Accent else SurfaceCard)
                    .border(1.dp, if (isSelected) Accent else CardBorder, PillShape)
                    .clickable { onToggle(chip.tag) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = stringResource(chip.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) OnAccent else TextMuted,
                )
            }
        }
    }
}

private val RECIPE_COVER_GRADIENTS = listOf(
    Color(0xFF14291B) to Color(0xFF101B14),
    Color(0xFF1B2A20) to Color(0xFF0E1712),
    Color(0xFF16241C) to Color(0xFF0B120D),
    AccentSurfaceSelected to Color(0xFF0F1310),
)

/** No real food photography ships with this app (per the design brief's asset policy, same as
 * [com.fitviet.app.ui.programs.ProgramCoverArt]'s reasoning) — a deterministic gradient card
 * keyed by the recipe's own name stands in for the "image dominant" card the mockup specifies. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun RecipeCard(
    recipe: RecipeWithNutrition,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val gradient = remember(recipe.nameVi) {
        val index = abs(recipe.nameVi.hashCode().let { if (it == Int.MIN_VALUE) 0 else it }) % RECIPE_COVER_GRADIENTS.size
        RECIPE_COVER_GRADIENTS[index]
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(Brush.linearGradient(listOf(gradient.first, gradient.second))),
        ) {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(PillShape)
                    .background(Accent)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = stringResource(R.string.nutrition_recipe_kcal_per_serving, formatVi(recipe.standardTotals.kcal)),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = Anton),
                    color = OnAccent,
                )
            }
        }
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            with(sharedTransitionScope) {
                Text(
                    text = recipe.nameVi,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(key = "recipe-title-${recipe.recipeId}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "P ${recipe.standardTotals.proteinG}g",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                )
                Text(
                    text = stringResource(
                        R.string.nutrition_recipe_meta,
                        recipe.cookTimeMinutes,
                        recipe.tags.firstOrNull().orEmpty(),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
