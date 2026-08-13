package com.fitviet.app.ui.handbook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.domain.MuscleGroup
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.labelRes

@Composable
fun HandbookScreen(
    viewModel: HandbookViewModel,
    // Redesign Phase 3b — Handbook dropped out of the bottom nav (see BottomNavBar's own doc), so
    // it's now a real drill-in (reached from the Kế hoạch tab's "Thư viện bài tập" row) rather than
    // a peer tab — it needs an explicit back affordance the same way every other drill-in screen in
    // this app already has one (MonthlyPlanDetailScreen, ExerciseDetailScreen, DiaryScreen, ...).
    onBack: () -> Unit,
    // Gate E5 — exercises no longer render inline here; a group card opens
    // HandbookMuscleGroupScreen, which is where an individual exercise's own tap-through lives.
    onMuscleGroupClick: (MuscleGroup) -> Unit,
    // Gate E7 — same drill-down shape as onMuscleGroupClick, for the Foods tab's categories.
    onFoodCategoryClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundPage)) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackChip(onClick = onBack)
                Text(text = stringResource(R.string.handbook_title), style = MaterialTheme.typography.headlineMedium)
            }
            TabRow(selected = uiState.selectedTab, onSelect = viewModel::selectTab)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = Dimens.ScreenPaddingHorizontal),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (uiState.selectedTab) {
                HandbookTab.EXERCISES -> {
                    if (uiState.exercisesByMuscleGroup.isEmpty()) {
                        item { EmptyText(text = stringResource(R.string.handbook_exercises_empty)) }
                    } else {
                        items(uiState.exercisesByMuscleGroup, key = { (group, _) -> "group-${group.name}" }) { (group, count) ->
                            MuscleGroupCard(group = group, exerciseCount = count, onClick = { onMuscleGroupClick(group) })
                        }
                    }
                }
                HandbookTab.FOODS -> {
                    if (uiState.foodsByCategory.isEmpty()) {
                        item { EmptyText(text = stringResource(R.string.handbook_foods_empty)) }
                    } else {
                        items(uiState.foodsByCategory, key = { (category, _) -> "category-$category" }) { (category, count) ->
                            FoodCategoryCard(category = category, foodCount = count, onClick = { onFoodCategoryClick(category) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(MaterialTheme.shapes.small)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "‹", style = MaterialTheme.typography.titleMedium, color = TextMuted)
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
private fun EmptyText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextMuted, modifier = Modifier.padding(top = 24.dp))
}

/** Gate E5 — one card per [MuscleGroup] on the Exercises tab, replacing the old flat
 * by-difficulty-level list. [exerciseCount] gives a preview of how much is inside without
 * requiring the drill-down tap. */
@Composable
private fun MuscleGroupCard(group: MuscleGroup, exerciseCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = stringResource(group.labelRes()), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(
                text = stringResource(R.string.handbook_muscle_group_exercise_count, exerciseCount),
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
            )
        }
        Text(text = "›", style = MaterialTheme.typography.titleMedium, color = TextMuted)
    }
}

/** Gate E7 — same shape as [MuscleGroupCard], for the Foods tab's ingredient categories. */
@Composable
private fun FoodCategoryCard(category: String, foodCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = category, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(
                text = pluralStringResource(R.plurals.handbook_food_category_count, foodCount, foodCount),
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
            )
        }
        Text(text = "›", style = MaterialTheme.typography.titleMedium, color = TextMuted)
    }
}
