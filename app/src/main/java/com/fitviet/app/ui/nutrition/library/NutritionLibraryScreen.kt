package com.fitviet.app.ui.nutrition.library

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.FoodEntity
import com.fitviet.app.data.local.entity.MealPlanTemplateEntity
import com.fitviet.app.domain.NutritionGoal
import com.fitviet.app.domain.RecipeWithNutrition
import com.fitviet.app.ui.common.HrBackChip
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.util.formatVi
import kotlinx.coroutines.launch

/**
 * Redesign Gate 5b-i — "Món & thực đơn" (design_handoff_hit_and_run_redesign, `FitViet Redesign
 * Hit & Run.dc.html` lines ~665-687): merges what used to be `DiscoverScreen`/`FoodsScreen`/
 * `TemplatesScreen` into two tabs — the third pill ("Kế hoạch tuần") is deliberately absent, not
 * disabled, until Gate 5b-ii per the Phase 5 plan-check's own "present-but-dead is worse than
 * absent for one gate" call.
 *
 * Two deliberate deviations from the literal mock, both from the same plan-check:
 * - The Món ăn tab's recipe rows are a plain list (per the mock's own markup here), NOT the old
 *   `DiscoverScreen`'s 2-column gradient-cover grid — this screen's job is the merge, not
 *   preserving every visual detail of a screen it's retiring.
 * - [FoodEntity] rows (Judgment Call A) have no tab of their own — folded into the Món ăn tab as a
 *   second, search-filtered section below the recipe suggestions/template promo, headed by its own
 *   small [R.string.nutrition_library_foods_header] label. Not in the mock's markup at all, and
 *   only rendered while actively searching (review finding, Gate 5b-i) — with no active query it
 *   duplicated the Handbook's own Foods tab (same catalog, category drill-down) directly beneath
 *   25+ ranked recipes and a promo card, with no way to jump past it.
 *
 * One review-confirmed capability regression, accepted rather than silently dropped: the retired
 * `DiscoverScreen`'s 5 tag filter chips (deliberate AND-across-chips narrowing) and `FoodsScreen`'s
 * category chips have no replacement here. Foods stay reachable by category via the Handbook's own
 * Foods tab (see [com.fitviet.app.ui.handbook.HandbookFoodCategoryScreen]'s own Gate 5e doc for the
 * closed decision not to absorb that content here); recipe tag filtering has none. Revisit if this
 * turns out to matter in practice — search-by-name plus the new goal-ranked suggestion order cover
 * the common case, but a user looking for e.g. "≤15 phút" specifically has no way to ask for that
 * anymore.
 *
 * Gate 5b-ii adds the third pill, [LibraryTab.PLAN] — state-dependent per the Phase 5 plan-check's
 * Judgment Call B: no active plan shows the create-plan wizard (merged in from the retired
 * `CreatePlanScreen`/`CreatePlanViewModel`); an active plan shows a summary + "Xem thực đơn tuần ›"
 * into [onOpenPlan] + a regenerate link, never both. [SearchField] is hidden on this tab — neither
 * mode has anything for it to filter, and a visible-but-inert search field on the Thực đơn mẫu tab
 * was exactly the kind of finding the Gate 5b-i review caught once already.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NutritionLibraryScreen(
    viewModel: NutritionLibraryViewModel,
    onBack: () -> Unit,
    onOpenRecipe: (Long) -> Unit,
    onOpenPlan: () -> Unit,
    onPlanGenerated: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    // Review finding (Gate 5b-i) — both tabs share one LazyColumn/scroll state (same idiom
    // HandbookScreen uses), but Recipes can run 25+ items deep against Templates' single digits;
    // without this, switching tabs while scrolled clamps into Templates already scrolled to its
    // own bottom. HandbookScreen's two tabs are close enough in length (~7/~11) that the same gap
    // was invisible there.
    val listState = rememberLazyListState()
    LaunchedEffect(uiState.selectedTab) { listState.scrollToItem(0) }

    Column(modifier = Modifier.fillMaxSize().background(HrColors.Bg)) {
        Column(
            modifier = Modifier.padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // HrBackChip is 34dp, vs. the mock's literal 40dp for this one screen — reused as-is
                // per this codebase's established "one shared back-chip component" convention
                // (Gate 3d) rather than a one-off size for this screen alone.
                HrBackChip(onClick = onBack)
                Text(text = stringResource(R.string.nutrition_library_title), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = HrColors.TextHi)
            }
            if (uiState.selectedTab != LibraryTab.PLAN) {
                SearchField(query = uiState.searchQuery, onQueryChange = viewModel::onSearchQueryChange)
            }
            TabRow(selected = uiState.selectedTab, onSelect = viewModel::selectTab)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = HrDimens.ScreenPaddingHorizontal),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (uiState.selectedTab) {
                LibraryTab.RECIPES -> {
                    item {
                        // Review finding (Gate 5b-i) — the goal-suggestion framing only describes
                        // the browse-mode list (ranked + capped by rankForGoal); once searching,
                        // the list is the server-filtered query match instead, so the header says
                        // so rather than keep claiming a goal-driven order that no longer applies.
                        val header = if (uiState.searchQuery.isBlank()) {
                            stringResource(R.string.nutrition_library_suggested_header, stringResource(goalHeaderLabelRes(uiState.matchedGoal)).uppercase(), formatVi(uiState.kcalRemainingToday))
                        } else {
                            stringResource(R.string.nutrition_library_search_results_header, formatVi(uiState.kcalRemainingToday))
                        }
                        Text(
                            text = header,
                            fontFamily = HrBody,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = HrColors.TextLow,
                        )
                    }
                    if (uiState.recipes.isEmpty()) {
                        item { EmptyText(text = stringResource(R.string.nutrition_empty_discover)) }
                    } else {
                        items(uiState.recipes, key = { "recipe-${it.recipeId}" }) { recipe ->
                            with(sharedTransitionScope) {
                                RecipeRow(
                                    recipe = recipe,
                                    onClick = { onOpenRecipe(recipe.recipeId) },
                                    onAdd = { viewModel.addToToday(recipe) },
                                    animatedVisibilityScope = animatedVisibilityScope,
                                )
                            }
                        }
                    }
                    uiState.matchedTemplate?.let { template ->
                        item(key = "matched-template-promo") {
                            MatchedTemplatePromoCard(
                                template = template,
                                pending = uiState.isApplyingTemplate,
                                onApply = {
                                    coroutineScope.launch { if (viewModel.useTemplate(template.id)) onPlanGenerated() }
                                },
                            )
                        }
                    }
                    if (uiState.foods.isNotEmpty()) {
                        item(key = "foods-header") {
                            Text(
                                text = stringResource(R.string.nutrition_library_foods_header),
                                fontFamily = HrBody,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = HrColors.TextLow,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        items(uiState.foods, key = { "food-${it.id}" }) { food -> FoodRow(food = food) }
                    }
                }
                LibraryTab.TEMPLATES -> {
                    if (uiState.templates.isEmpty()) {
                        item { EmptyText(text = stringResource(R.string.nutrition_empty_templates)) }
                    } else {
                        items(uiState.templates, key = { "template-${it.id}" }) { template ->
                            val isMatched = template.goalCode == uiState.matchedGoal.name
                            if (isMatched) {
                                MatchedTemplatePromoCard(
                                    template = template,
                                    pending = uiState.isApplyingTemplate,
                                    onApply = {
                                        coroutineScope.launch { if (viewModel.useTemplate(template.id)) onPlanGenerated() }
                                    },
                                )
                            } else {
                                PlainTemplateCard(
                                    template = template,
                                    pending = uiState.isApplyingTemplate,
                                    onApply = {
                                        coroutineScope.launch { if (viewModel.useTemplate(template.id)) onPlanGenerated() }
                                    },
                                )
                            }
                        }
                    }
                }
                LibraryTab.PLAN -> {
                    val summary = uiState.activePlanSummary
                    if (summary != null) {
                        item(key = "plan-summary") {
                            ActivePlanManageCard(
                                summary = summary,
                                isRegenerating = uiState.isRegeneratingPlan,
                                onView = onOpenPlan,
                                // Review finding (Gate 5b-ii) — regenerating changes none of the
                                // fields this card itself shows (goal/kcal/protein/meals), so a
                                // successful regenerate was indistinguishable from a no-op beyond a
                                // brief dim. Navigating into nutrition/plan on success makes the new
                                // week actually visible, matching `PlanScreen`'s own regenerate
                                // link, which sits right above the meal list it rewrites. Plain
                                // `onOpenPlan` (not `onPlanGenerated`'s `popUpTo`) is deliberate —
                                // back from the plan should return to this tab's own refreshed
                                // manage card, not all the way out to Nutrition Home.
                                onRegenerate = { coroutineScope.launch { if (viewModel.regenerateActivePlan(summary.planId)) onOpenPlan() } },
                            )
                        }
                    } else {
                        planWizardItems(
                            wizard = uiState.wizard,
                            isGenerating = uiState.isGeneratingPlan,
                            viewModel = viewModel,
                            onGenerate = { coroutineScope.launch { if (viewModel.generatePlan()) onPlanGenerated() } },
                        )
                    }
                }
            }
        }
    }
}

private fun goalHeaderLabelRes(goal: NutritionGoal): Int = when (goal) {
    NutritionGoal.BULK -> R.string.goal_muscle_gain_title
    NutritionGoal.CUT -> R.string.goal_fat_loss_title
    NutritionGoal.MAINTAIN -> R.string.onboarding_goal_maintain_pill
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.ButtonSmall)
            .background(HrColors.SurfaceInput)
            .border(1.dp, HrColors.Border, HrShapes.ButtonSmall)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (query.isEmpty()) {
            Text(text = stringResource(R.string.nutrition_library_search_placeholder), fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextFaint)
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp, color = HrColors.TextHi),
            cursorBrush = SolidColor(HrColors.Accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TabRow(selected: LibraryTab, onSelect: (LibraryTab) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        TabPill(label = stringResource(R.string.nutrition_library_tab_recipes), selected = selected == LibraryTab.RECIPES, onClick = { onSelect(LibraryTab.RECIPES) })
        TabPill(label = stringResource(R.string.nutrition_quick_templates), selected = selected == LibraryTab.TEMPLATES, onClick = { onSelect(LibraryTab.TEMPLATES) })
        TabPill(label = stringResource(R.string.nutrition_library_tab_plan), selected = selected == LibraryTab.PLAN, onClick = { onSelect(LibraryTab.PLAN) })
    }
}

@Composable
private fun TabPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = Dimens.MinTouchTarget)
            .clip(PillShape)
            .background(if (selected) HrColors.Accent else Color.Transparent)
            .border(1.dp, if (selected) HrColors.Accent else HrColors.Border, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Only the active pill is bold, matching the mock's own literal markup (selected has
        // font-weight:700, unselected has none) — the review-found bug was both being Bold.
        Text(text = label, fontFamily = HrBody, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp, color = if (selected) HrColors.OnAccent else HrColors.TextMid)
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(text = text, fontFamily = HrBody, fontSize = 14.sp, lineHeight = 20.sp, color = HrColors.TextLow, modifier = Modifier.padding(top = 24.dp))
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.RecipeRow(
    recipe: RecipeWithNutrition,
    onClick: () -> Unit,
    onAdd: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardSmall)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardSmall)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = recipe.nameVi,
                fontFamily = HrBody,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = HrColors.TextHi,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.sharedElement(
                    rememberSharedContentState(key = "recipe-title-${recipe.recipeId}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                ),
            )
            Text(
                text = stringResource(R.string.nutrition_library_recipe_meta, formatVi(recipe.standardTotals.kcal), recipe.standardTotals.proteinG),
                fontFamily = HrBody,
                fontSize = 11.sp,
                color = HrColors.TextLow,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier = Modifier
                .heightIn(min = Dimens.MinTouchTarget)
                .clip(HrShapes.ButtonSmall)
                .border(1.5.dp, HrColors.BorderAccentDim, HrShapes.ButtonSmall)
                .clickable(onClick = onAdd)
                .padding(horizontal = 13.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.nutrition_library_add_button), fontFamily = HrBody, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = HrColors.Accent)
        }
    }
}

@Composable
private fun FoodRow(food: FoodEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardSmall)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardSmall)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(text = food.nameVi, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HrColors.TextHi, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = stringResource(R.string.nutrition_food_macros_per_100g, food.proteinG.toInt(), food.carbG.toInt(), food.fatG.toInt()),
                fontFamily = HrBody,
                fontSize = 11.sp,
                color = HrColors.TextLow,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = stringResource(R.string.nutrition_food_per_100g, formatVi(food.kcalPer100g)),
            fontFamily = HrDisplay,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = HrColors.Accent,
        )
    }
}

/** The mock's own literal card — gradient shell, "THỰC ĐƠN MẪU · 1 CHẠM" eyebrow, 1-tap apply CTA.
 * Used both as the Món ăn tab's cross-sell teaser and as the matched card within the Thực đơn mẫu
 * tab's own list (Phase 5 plan-check's resolution for the mock's "one promo card" vs. "a real
 * list" conflict — keep the list, give only the matched entry this treatment). [pending] (review
 * finding, Gate 5b-i) disables the CTA and dims it while `generateFromTemplate` runs — a full
 * 7-day generation inside one DB transaction, not instant — using the
 * [com.fitviet.app.ui.nutrition.library.NutritionLibraryViewModel.useTemplate] re-entrancy flag
 * that previously drove no UI at all. */
@Composable
private fun MatchedTemplatePromoCard(template: MealPlanTemplateEntity, pending: Boolean, onApply: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(Brush.linearGradient(listOf(HrColors.GradientCardStart, HrColors.GradientCardEnd), start = Offset(0f, 0f)))
            .border(1.dp, HrColors.BorderGradient, HrShapes.CardRegular)
            .padding(15.dp),
    ) {
        Text(text = stringResource(R.string.nutrition_library_template_badge), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp, color = HrColors.Accent)
        Text(
            text = template.nameVi,
            fontFamily = HrDisplay,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = HrColors.TextHi,
            modifier = Modifier.padding(top = 3.dp),
        )
        Text(text = template.descriptionVi, fontFamily = HrBody, fontSize = 11.sp, color = HrColors.TextLow, modifier = Modifier.padding(top = 2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .alpha(if (pending) 0.6f else 1f)
                .clip(HrShapes.ButtonSmall)
                .background(HrColors.Accent)
                .clickable(enabled = !pending, onClick = onApply)
                .padding(vertical = 11.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.nutrition_library_template_apply), fontFamily = HrBody, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = HrColors.OnAccent, textAlign = TextAlign.Center)
        }
    }
}

/** A non-matched entry in the Thực đơn mẫu tab's list — plain surface card, same shell as the
 * pre-Gate-5b-i `TemplateCard` just re-skinned, not the gradient promo treatment. [pending] — see
 * [MatchedTemplatePromoCard]'s own doc. */
@Composable
private fun PlainTemplateCard(template: MealPlanTemplateEntity, pending: Boolean, onApply: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = template.nameVi, fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = HrColors.TextHi)
        Text(text = template.descriptionVi, fontFamily = HrBody, fontSize = 12.sp, color = HrColors.TextLow)
        Text(
            text = stringResource(R.string.nutrition_template_meta, template.kcalPerDay, template.proteinPerDayG, template.mealsPerDay),
            fontFamily = HrBody,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = HrColors.TextMid,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .alpha(if (pending) 0.6f else 1f)
                .clip(HrShapes.ButtonSmall)
                .border(1.5.dp, HrColors.BorderAccentDim, HrShapes.ButtonSmall)
                .clickable(enabled = !pending, onClick = onApply)
                .padding(vertical = 11.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.nutrition_library_template_apply), fontFamily = HrBody, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = HrColors.Accent, textAlign = TextAlign.Center)
        }
    }
}

/** Kế hoạch tuần tab, "manage" mode (Gate 5b-ii) — an active plan already exists, so this is a
 * summary + two actions rather than the wizard. Same gradient shell as [MatchedTemplatePromoCard]
 * (both are "here's a plan, act on it" cards), distinct copy/actions. */
@Composable
private fun ActivePlanManageCard(summary: ActivePlanSummary, isRegenerating: Boolean, onView: () -> Unit, onRegenerate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(Brush.linearGradient(listOf(HrColors.GradientCardStart, HrColors.GradientCardEnd), start = Offset(0f, 0f)))
            .border(1.dp, HrColors.BorderGradient, HrShapes.CardRegular)
            .padding(15.dp),
    ) {
        Text(text = stringResource(R.string.nutrition_library_plan_summary_title), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp, color = HrColors.Accent)
        Text(
            text = stringResource(
                R.string.nutrition_library_plan_summary,
                stringResource(wizardGoalLabelRes(summary.goal)),
                formatVi(summary.kcalTarget),
                summary.proteinTargetG,
                summary.mealsPerDay,
            ),
            fontFamily = HrDisplay,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = HrColors.TextHi,
            modifier = Modifier.padding(top = 3.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Dimens.MinTouchTarget)
                    .alpha(if (isRegenerating) 0.6f else 1f)
                    .clip(HrShapes.ButtonSmall)
                    .border(1.5.dp, HrColors.BorderAccentDim, HrShapes.ButtonSmall)
                    .clickable(enabled = !isRegenerating, onClick = onRegenerate)
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.nutrition_plan_regenerate_week), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HrColors.Accent, textAlign = TextAlign.Center)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Dimens.MinTouchTarget)
                    .clip(HrShapes.ButtonSmall)
                    .background(HrColors.Accent)
                    .clickable(onClick = onView)
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.nutrition_library_plan_view), fontFamily = HrBody, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = HrColors.OnAccent, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Kế hoạch tuần tab, "create" mode (Gate 5b-ii) — no active plan, so the tab shows the wizard
 * merged in from the retired `CreatePlanScreen`: same fields (goal/kcal/protein/meals-per-day/cook
 * time), re-skinned to Hr tokens, one `item{}` per section so it lives inside the shared
 * `LazyColumn` rather than its own scrollable `Column` the way the standalone screen had. */
private fun LazyListScope.planWizardItems(
    wizard: PlanWizardState,
    isGenerating: Boolean,
    viewModel: NutritionLibraryViewModel,
    onGenerate: () -> Unit,
) {
    item(key = "wizard-goal") {
        WizardSection(label = stringResource(R.string.nutrition_create_plan_goal)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WIZARD_GOAL_OPTIONS.forEach { (goal, labelRes) ->
                    WizardPillChip(
                        label = stringResource(labelRes),
                        selected = wizard.goal == goal,
                        onClick = { viewModel.selectWizardGoal(goal) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
    item(key = "wizard-kcal") {
        WizardSection(label = stringResource(R.string.nutrition_create_plan_kcal)) {
            WizardStepper(
                valueText = stringResource(R.string.nutrition_meal_kcal, formatVi(wizard.kcalTarget)),
                onDecrement = viewModel::decrementWizardKcal,
                onIncrement = viewModel::incrementWizardKcal,
            )
        }
    }
    item(key = "wizard-protein") {
        WizardSection(label = stringResource(R.string.nutrition_create_plan_protein)) {
            WizardStepper(
                valueText = stringResource(R.string.nutrition_recipe_grams, wizard.proteinTargetG),
                onDecrement = viewModel::decrementWizardProtein,
                onIncrement = viewModel::incrementWizardProtein,
            )
        }
    }
    item(key = "wizard-meals") {
        WizardSection(label = stringResource(R.string.nutrition_create_plan_meals_per_day)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (NutritionLibraryViewModel.MIN_MEALS_PER_DAY..NutritionLibraryViewModel.MAX_MEALS_PER_DAY).forEach { count ->
                    WizardPillChip(
                        label = formatVi(count),
                        selected = wizard.mealsPerDay == count,
                        onClick = { viewModel.selectWizardMealsPerDay(count) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
    item(key = "wizard-cook-time") {
        WizardSection(label = stringResource(R.string.nutrition_create_plan_cook_time)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NutritionLibraryViewModel.COOK_TIME_OPTIONS.forEach { minutes ->
                    WizardPillChip(
                        label = if (minutes == null) {
                            stringResource(R.string.nutrition_create_plan_cook_time_unlimited)
                        } else {
                            stringResource(R.string.nutrition_create_plan_cook_time_option, minutes)
                        },
                        selected = wizard.cookTimeCeilingMinutes == minutes,
                        onClick = { viewModel.selectWizardCookTimeCeiling(minutes) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
    item(key = "wizard-generate") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .alpha(if (isGenerating) 0.6f else 1f)
                .clip(HrShapes.ButtonCta)
                .background(HrColors.Accent)
                .clickable(enabled = !isGenerating, onClick = onGenerate)
                .padding(vertical = HrDimens.CtaPaddingVertical),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.nutrition_create_plan_generate), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = HrColors.OnAccent, textAlign = TextAlign.Center)
        }
    }
}

private val WIZARD_GOAL_OPTIONS = listOf(
    NutritionGoal.CUT to R.string.nutrition_create_plan_goal_cut,
    NutritionGoal.MAINTAIN to R.string.nutrition_create_plan_goal_maintain,
    NutritionGoal.BULK to R.string.nutrition_create_plan_goal_bulk,
)

/** Review finding (Gate 5b-ii) — [ActivePlanManageCard] must NOT reuse [goalHeaderLabelRes]. That
 * helper was written for the Món ăn tab's "GỢI Ý CHO MỤC TIÊU" eyebrow, using the onboarding
 * goal vocabulary (`goal_muscle_gain_title`/`goal_fat_loss_title`/`onboarding_goal_maintain_pill`)
 * — a plan's own summary instead reflects a choice the wizard itself offered
 * ([WIZARD_GOAL_OPTIONS]'s "Tăng cân"/"Giảm mỡ"/"Duy trì"), a different string set entirely. Using
 * the wrong one meant a plan generated as "Tăng cân" (Bulk) came back labeled "Tăng cơ" (Build
 * muscle) on return to this tab — not synonyms in Vietnamese. */
private fun wizardGoalLabelRes(goal: NutritionGoal): Int =
    WIZARD_GOAL_OPTIONS.first { (option, _) -> option == goal }.second

@Composable
private fun WizardSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = label, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp, color = HrColors.TextLow)
        content()
    }
}

@Composable
private fun WizardStepper(valueText: String, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, PillShape),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        WizardStepperButton(symbol = "−", onClick = onDecrement)
        Text(text = valueText, fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = HrColors.TextHi)
        WizardStepperButton(symbol = "+", onClick = onIncrement)
    }
}

@Composable
private fun WizardStepperButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(Dimens.MinTouchTarget)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = HrColors.Accent)
    }
}

@Composable
private fun WizardPillChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = Dimens.MinTouchTarget)
            .clip(PillShape)
            .background(if (selected) HrColors.Accent else HrColors.Surface)
            .border(1.dp, if (selected) HrColors.Accent else HrColors.Border, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = HrBody,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = if (selected) HrColors.OnAccent else HrColors.TextMid,
            textAlign = TextAlign.Center,
        )
    }
}
