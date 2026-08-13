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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.domain.MuscleGroup
import com.fitviet.app.ui.common.HrBackChip
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.util.labelRes

/** Redesign Gate 3d — migrated to Hr tokens on both tabs at once (rather than Exercises-only):
 * [HandbookScreen] is a single screen whose page background, back chip, title, tab row, and
 * empty-state text are shared by both tabs, so a token swap on only one tab would show a jarring
 * accent/surface change on tab switch under a header that didn't change. The Foods tab's own
 * drill-down destination, [HandbookFoodCategoryScreen], is deliberately left on legacy tokens.
 *
 * Gate 3d's plan-check (Phase 3) had flagged the handoff doc's "kiến thức món ăn" (food knowledge)
 * content as a candidate for absorption into Nutrition's own consolidated screen "in a later
 * phase." Phase 5 was that later phase, and it decided against absorbing it: per
 * `NutritionLibraryScreen`'s own Gate 5b-i doc, the library folded in a name-search-only food list
 * (Judgment Call A — name/kcal/macros, hidden entirely with no active query), but left the
 * by-category browsing, English names, and descriptions that only [HandbookFoodCategoryScreen]
 * offers. So [HandbookFoodCategoryScreen] staying on legacy tokens is now a closed scope decision,
 * not a pending migration — see [HandbookFoodCategoryScreen]'s own doc for the re-defer note. */
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

    Column(modifier = Modifier.fillMaxSize().background(HrColors.Bg)) {
        Column(
            modifier = Modifier.padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HrBackChip(onClick = onBack)
                Text(text = stringResource(R.string.handbook_title), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = HrColors.TextHi)
            }
            TabRow(selected = uiState.selectedTab, onSelect = viewModel::selectTab)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = HrDimens.ScreenPaddingHorizontal),
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
            .background(if (selected) HrColors.Accent else HrColors.Surface)
            .border(1.dp, if (selected) HrColors.Accent else HrColors.Border, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(text = label, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (selected) HrColors.OnAccent else HrColors.TextMid)
    }
}

@Composable
private fun EmptyText(text: String) {
    // Explicit lineHeight (review finding, Gate 3d) — HrBody's tight metrics default to well under
    // this text's own ascent+descent at 14sp, which visibly collides wrapped Vietnamese diacritics.
    Text(text = text, fontFamily = HrBody, fontSize = 14.sp, lineHeight = 20.sp, color = HrColors.TextLow, modifier = Modifier.padding(top = 24.dp))
}

/** Gate E5 — one card per [MuscleGroup] on the Exercises tab, replacing the old flat
 * by-difficulty-level list. [exerciseCount] gives a preview of how much is inside without
 * requiring the drill-down tap. */
@Composable
private fun MuscleGroupCard(group: MuscleGroup, exerciseCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = stringResource(group.labelRes()), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HrColors.TextHi)
            Text(
                text = stringResource(R.string.handbook_muscle_group_exercise_count, exerciseCount),
                fontFamily = HrBody,
                fontSize = 12.sp,
                color = HrColors.TextLow,
            )
        }
        Text(text = "›", fontFamily = HrBody, fontSize = 16.sp, color = HrColors.TextFaint)
    }
}

/** Gate E7 — same shape as [MuscleGroupCard], for the Foods tab's ingredient categories. */
@Composable
private fun FoodCategoryCard(category: String, foodCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = category, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HrColors.TextHi)
            Text(
                text = pluralStringResource(R.plurals.handbook_food_category_count, foodCount, foodCount),
                fontFamily = HrBody,
                fontSize = 12.sp,
                color = HrColors.TextLow,
            )
        }
        Text(text = "›", fontFamily = HrBody, fontSize = 16.sp, color = HrColors.TextFaint)
    }
}
