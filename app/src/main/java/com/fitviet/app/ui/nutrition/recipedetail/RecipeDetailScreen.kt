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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.repository.RecipeDetail
import com.fitviet.app.data.repository.RecipeIngredientDetail
import com.fitviet.app.domain.NutritionTotals
import com.fitviet.app.ui.common.HrBackChip
import com.fitviet.app.ui.common.LoadingSkeleton
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.util.formatVi
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** The mock's own literal two Vietnamese slot values (design_handoff_hit_and_run_redesign,
 * `FitViet Redesign Hit & Run.dc.html` lines ~847-848) — data keys stored verbatim in
 * [com.fitviet.app.data.local.entity.MealEntity.slot] regardless of locale, same as
 * `NutritionRepository`'s own private `ADDED_MEAL_SLOT` constant; never localized, unlike the two
 * buttons' own display labels below. */
private const val SLOT_SNACK = "Bữa phụ"
private const val SLOT_LUNCH = "Bữa trưa"

/**
 * Redesign Gate 5c — Recipe Detail rebuilt per the mock's own literal markup (§11 in the handoff
 * README; `FitViet Redesign Hit & Run.dc.html` lines ~823-849). Actual composed order: breadcrumb
 * + title → 4-stat grid (kcal/protein/carb/fat) → servings card → variant pills (if the recipe has
 * more than one) → ingredient rows → instruction steps → "fits your goal" line → two slot-aware
 * CTAs pinned to the bottom.
 *
 * Two deliberate deviations from the literal mock, both from the Phase 5 plan-check's Judgment
 * Call D ("keep instructions, variants, and scaled ingredient rows... re-skinned, above the mock's
 * own 'fits your goal' block" — dropping them would have been a real capability regression, not
 * just a copy simplification, per the Gate 3c `GenerateSheet` precedent this call cites):
 * - The mock collapses all ingredients into one static prose line ("Thành phần chính"). This screen
 *   keeps the pre-existing per-ingredient row list instead — scrollable, and each row's grams
 *   scale live with the servings stepper, which a static sentence can't do.
 * - The mock has no instructions section, no variant pills, and no favorite toggle at all (a
 *   simpler screen than what shipped in Part C). Both are kept, re-skinned: variant pills sit
 *   directly under the servings card (above the ingredient block), and instructions are inserted
 *   between the ingredient block and the mock's own "fits your goal" line. The favorite toggle IS
 *   dropped here — see [RecipeDetailViewModel]'s own doc for why (confirmed write-only, not a
 *   capability regression the way losing instructions/variants would have been).
 *
 * The mock's own descriptive subtitle under the title ("1 tô lớn · 650g") has no backing field on
 * [RecipeDetail] — inventing recipe-specific serving text isn't this gate's call to make, so that
 * slot instead shows the real prep/cook time data the pre-Gate-5c screen already had, rather than
 * being left empty or fabricated. Also mock-compliant, not a deviation: the pre-Gate-5c screen's
 * own decorative gradient cover image (no real food photography, no backing asset) is dropped
 * entirely, matching the mock (which has none) and this library's own `RecipeRow`
 * (`NutritionLibraryScreen.kt`), which is likewise a flat surface color with no gradient/photo.
 *
 * Known deferral, not a gap filled: the handoff README (§ toast spec, "Toast (pill lime, đáy màn,
 * 2.2s): sau tạo plan, thêm món, chia sẻ") calls for a toast on this exact action ("thêm món" = add
 * a dish). No toast/snackbar component exists anywhere in this codebase yet, so both CTAs below
 * navigate back on success instead — a real deviation from the spec'd interaction, tracked here as
 * a known deferral rather than presented as if it satisfies the spec.
 */
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
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(HrColors.Bg)) {
        Row(
            modifier = Modifier.padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HrBackChip(onClick = onBack)
            Text(text = stringResource(R.string.nutrition_recipe_breadcrumb), fontFamily = HrBody, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = HrColors.TextMid)
        }

        // Hoisted out of the `if (detail != null)` guard below AND out of the LazyColumn (Gate 5c
        // review fix — HIGH): the shared-element target must exist on frame 1 of the enter
        // transition to match the library row's already-real source key, or the transition starts
        // mid-flight instead of from the source's position. It must also stay composed while the
        // rest of the screen scrolls, or the pop transition back to the library has no target at
        // all. `detail?.nameVi.orEmpty()` renders the real title once the async `getRecipeDetail`
        // load resolves; before that it's an empty placeholder, not a missing element.
        Column(
            modifier = Modifier.padding(horizontal = HrDimens.ScreenPaddingHorizontal),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            with(sharedTransitionScope) {
                Text(
                    text = detail?.nameVi.orEmpty(),
                    fontFamily = HrDisplay,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 27.sp,
                    color = HrColors.TextHi,
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(key = "recipe-title-$recipeId"),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                )
            }
            if (detail != null) {
                // The mock's own subtitle slot here shows recipe-specific serving text
                // ("1 tô lớn · 650g") with no backing field on RecipeDetail — this fills it
                // with the real prep/cook time data instead, preserved rather than dropped
                // per Judgment Call D's capability-preservation reasoning.
                Text(
                    text = stringResource(R.string.nutrition_recipe_prep_cook, detail.prepTimeMinutes, detail.cookTimeMinutes),
                    fontFamily = HrBody,
                    fontSize = 13.sp,
                    color = HrColors.TextLow,
                )
            }
        }

        if (uiState.isLoading) {
            LoadingSkeleton(modifier = Modifier.weight(1f).padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 14.dp))
        } else if (detail != null) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = HrDimens.ScreenPaddingHorizontal),
                contentPadding = PaddingValues(top = 14.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "stats") { StatGrid(totals = uiState.totals) }
                item(key = "servings") {
                    ServingsCard(
                        servings = uiState.servings,
                        onIncrement = viewModel::incrementServings,
                        onDecrement = viewModel::decrementServings,
                    )
                }
                if (detail.variants.size > 1) {
                    item(key = "variants") {
                        VariantPills(
                            variants = detail.variants.map { it.code },
                            selectedVariantCode = uiState.selectedVariantCode,
                            onSelectVariant = viewModel::selectVariant,
                        )
                    }
                }
                item(key = "ingredients-header") { SectionHeader(text = stringResource(R.string.nutrition_ingredients_title)) }
                items(detail.ingredients, key = { "ingredient-${it.foodId}" }) { ingredient ->
                    IngredientRow(ingredient = ingredient, servings = uiState.servings, baseServings = detail.baseServings)
                }
                item(key = "instructions-header") { SectionHeader(text = stringResource(R.string.nutrition_instructions_title)) }
                itemsIndexed(detail.instructions, key = { index, _ -> "instruction-$index" }) { index, step ->
                    InstructionRow(stepNumber = index + 1, text = step)
                }
                item(key = "fits-goal") {
                    Text(
                        text = stringResource(R.string.nutrition_recipe_fits_goal, formatVi(uiState.kcalRemainingToday)),
                        fontFamily = HrBody,
                        fontSize = 11.sp,
                        color = HrColors.TextFaint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
            }

            AddToMealRow(
                pending = uiState.isAdding,
                onAddSnack = {
                    coroutineScope.launch { if (viewModel.addToMeal(SLOT_SNACK)) onBack() }
                },
                onAddLunch = {
                    coroutineScope.launch { if (viewModel.addToMeal(SLOT_LUNCH)) onBack() }
                },
            )
        }
    }
}

@Composable
private fun StatGrid(totals: NutritionTotals) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        StatTile(value = formatVi(totals.kcal), label = stringResource(R.string.nutrition_recipe_stat_kcal), accent = true, modifier = Modifier.weight(1f))
        StatTile(value = stringResource(R.string.nutrition_recipe_grams, totals.proteinG), label = stringResource(R.string.nutrition_macro_protein), modifier = Modifier.weight(1f))
        StatTile(value = stringResource(R.string.nutrition_recipe_grams, totals.carbG), label = stringResource(R.string.nutrition_macro_carb), modifier = Modifier.weight(1f))
        StatTile(value = stringResource(R.string.nutrition_recipe_grams, totals.fatG), label = stringResource(R.string.nutrition_recipe_stat_fat), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(
        modifier = modifier
            .clip(HrShapes.ButtonSmall)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.ButtonSmall)
            .padding(vertical = 11.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value, fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = if (accent) HrColors.Accent else HrColors.TextHi)
        Text(text = label, fontFamily = HrBody, fontSize = 9.sp, color = HrColors.TextLow, modifier = Modifier.padding(top = 1.dp))
    }
}

@Composable
private fun ServingsCard(servings: Int, onIncrement: () -> Unit, onDecrement: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .padding(15.dp),
        // Mock line 837 specifies margin-bottom:10px between the header row and the hint.
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.nutrition_servings_label), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HrColors.TextHi)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ServingsStepperButton(symbol = "−", onClick = onDecrement)
                Text(text = formatVi(servings), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = HrColors.TextHi)
                ServingsStepperButton(symbol = "+", onClick = onIncrement)
            }
        }
        // A third copy deviation alongside the two documented at the top of this file: the mock
        // renders the count as "1.0" with a half-portion drag hint ("Nửa tô… kéo để chỉnh"). This
        // stepper is integer-only (MIN_SERVINGS = 1, no drag gesture), so nutrition_recipe_servings_hint
        // was rewritten rather than kept mock-literal — the mock's hint describes an interaction
        // this screen doesn't have.
        Text(text = stringResource(R.string.nutrition_recipe_servings_hint), fontFamily = HrBody, fontSize = 12.sp, color = HrColors.TextLow)
    }
}

@Composable
private fun ServingsStepperButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(HrColors.BtnCircle)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HrColors.Accent)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VariantPills(variants: List<String>, selectedVariantCode: String, onSelectVariant: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        variants.forEach { code ->
            val isSelected = code == selectedVariantCode
            Box(
                modifier = Modifier
                    .heightIn(min = Dimens.MinTouchTarget)
                    .clip(PillShape)
                    .background(if (isSelected) HrColors.Accent else HrColors.Surface)
                    .border(1.dp, if (isSelected) HrColors.Accent else HrColors.Border, PillShape)
                    .clickable { onSelectVariant(code) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = variantLabel(code),
                    fontFamily = HrBody,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (isSelected) HrColors.OnAccent else HrColors.TextMid,
                )
            }
        }
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
    Text(text = text, fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = HrColors.TextHi)
}

@Composable
private fun IngredientRow(ingredient: RecipeIngredientDetail, servings: Int, baseServings: Int) {
    val scaledGrams = (ingredient.gramsAtBaseServings * servings / baseServings.coerceAtLeast(1)).roundToInt()
    val showDisplayQuantity = servings == baseServings && ingredient.displayQuantity != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardSmall)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardSmall)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(HrColors.SurfaceAccent)
                .border(1.dp, HrColors.Border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = ingredient.foodNameVi.take(1), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HrColors.Accent)
        }
        Text(
            text = ingredient.foodNameVi,
            fontFamily = HrBody,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = HrColors.TextHi,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (showDisplayQuantity) ingredient.displayQuantity!! else stringResource(R.string.nutrition_recipe_grams, scaledGrams),
            fontFamily = HrBody,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = HrColors.TextLow,
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
                .background(HrColors.Surface)
                .border(1.dp, HrColors.Border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = formatVi(stepNumber), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HrColors.Accent)
        }
        Text(text = text, fontFamily = HrBody, fontSize = 13.sp, lineHeight = 19.sp, color = HrColors.TextLow, modifier = Modifier.padding(top = 3.dp))
    }
}

/** The mock's own literal footer — "Bữa phụ" outline (flex 1) / "+ Thêm vào bữa trưa" filled
 * (flex 1.6, wider — the primary action). Pinned below the scrollable content, not inside the
 * `LazyColumn`, same "scrollable body + fixed footer CTA" split `StraightLogContent` established. */
@Composable
private fun AddToMealRow(pending: Boolean, onAddSnack: () -> Unit, onAddLunch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 14.dp)
            .alpha(if (pending) 0.6f else 1f),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = Dimens.MinTouchTarget)
                .clip(HrShapes.CardSmall)
                .border(1.5.dp, HrColors.BorderAccentDim, HrShapes.CardSmall)
                .clickable(enabled = !pending, onClick = onAddSnack)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.nutrition_recipe_cta_snack), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HrColors.Accent, textAlign = TextAlign.Center)
        }
        Box(
            modifier = Modifier
                .weight(1.6f)
                .heightIn(min = Dimens.MinTouchTarget)
                .clip(HrShapes.CardSmall)
                .background(HrColors.Accent)
                .clickable(enabled = !pending, onClick = onAddLunch)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.nutrition_recipe_cta_lunch), fontFamily = HrBody, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = HrColors.OnAccent, textAlign = TextAlign.Center)
        }
    }
}
