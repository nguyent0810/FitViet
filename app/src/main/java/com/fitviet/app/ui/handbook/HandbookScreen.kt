package com.fitviet.app.ui.handbook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
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
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.entity.FoodEntity
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.formatOneDecimal
import com.fitviet.app.util.labelRes

@Composable
fun HandbookScreen(
    viewModel: HandbookViewModel,
    onExerciseClick: (ExerciseEntity) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundPage)) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = stringResource(R.string.handbook_title), style = MaterialTheme.typography.headlineMedium)
            TabRow(selected = uiState.selectedTab, onSelect = viewModel::selectTab)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = Dimens.ScreenPaddingHorizontal),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (uiState.selectedTab) {
                HandbookTab.EXERCISES -> {
                    if (uiState.exercisesByLevel.isEmpty()) {
                        item { EmptyText(text = stringResource(R.string.handbook_exercises_empty)) }
                    } else {
                        uiState.exercisesByLevel.forEach { (level, exercises) ->
                            item { SectionHeader(text = stringResource(level.labelRes())) }
                            items(exercises, key = { "ex-${it.id}" }) { exercise ->
                                ExerciseRow(exercise = exercise, onClick = { onExerciseClick(exercise) })
                            }
                        }
                    }
                }
                HandbookTab.FOODS -> {
                    if (uiState.foodsByCategory.isEmpty()) {
                        item { EmptyText(text = stringResource(R.string.handbook_foods_empty)) }
                    } else {
                        uiState.foodsByCategory.forEach { (category, foods) ->
                            item { SectionHeader(text = category) }
                            items(foods, key = { "food-${it.id}" }) { food -> FoodRow(food = food) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabRow(selected: HandbookTab, onSelect: (HandbookTab) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TabChip(
            label = stringResource(R.string.handbook_tab_exercises),
            selected = selected == HandbookTab.EXERCISES,
            onClick = { onSelect(HandbookTab.EXERCISES) },
        )
        TabChip(
            label = stringResource(R.string.handbook_tab_foods),
            selected = selected == HandbookTab.FOODS,
            onClick = { onSelect(HandbookTab.FOODS) },
        )
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(if (selected) Accent else SurfaceCard)
            .border(1.dp, if (selected) Accent else CardBorder, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = if (selected) OnAccent else TextMuted)
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = TextMuted,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun EmptyText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextMuted, modifier = Modifier.padding(top = 24.dp))
}

@Composable
private fun ExerciseRow(exercise: ExerciseEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(text = exercise.nameVi, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(text = exercise.nameEn, style = MaterialTheme.typography.labelSmall, color = TextFaint)
            Text(text = exercise.primaryMuscle, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        }
        Text(text = "›", style = MaterialTheme.typography.titleMedium, color = TextMuted)
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
