package com.fitviet.app.ui.nutrition.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitviet.app.R
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.util.formatVi
import kotlin.math.abs

/**
 * Gate C8 — driven entirely by [state] + callbacks (same dumb-sheet idiom as
 * [com.fitviet.app.ui.profile.UpdateMeasurementSheet]/[com.fitviet.app.ui.workout.TechniquePickerSheet]),
 * owned by [PlanViewModel]. Shows one candidate at a time from the already-fetched
 * [SwapUiState.alternatives] pool; tapping the candidate card applies it directly (no separate
 * confirm step), "Gợi ý khác ↻" cycles to the next candidate in the pool.
 *
 * Redesign Gate 5d — token re-skin: legacy `Accent`/`SurfaceCard`/`CardBorder`/`MaterialTheme.typography`
 * tokens replaced with `HrColors`/`HrDisplay`/`HrBody`/`HrShapes`/`HrDimens` (the candidate card's
 * `SurfaceCard`→`HrColors.Surface` background is this same direct translation, not `SurfaceAccent`
 * — see the call-site comment below for why that distinction matters), `containerColor =
 * HrColors.Surface` matching `TechniquePickerSheet`'s own established `ModalBottomSheet` convention.
 *
 * Not a pure token swap — one call-site change rides along, same reasoning `PlanScreen`/
 * `CalendarScreen`'s own Gate 5d doc comments give for their analogous changes: the candidate
 * card's interior padding moved off the legacy `Dimens.CardPaddingLarge` (20dp) onto a raw 16dp,
 * matching `RecipeDetailScreen`/`NutritionLibraryScreen`/`ProgramsListScreen`'s `CardRegular`
 * cards (the 15-16dp modal value across the redesign).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapSheet(
    state: SwapUiState,
    onReroll: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = HrColors.Surface) {
        Column(
            modifier = Modifier.padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = stringResource(R.string.nutrition_swap_title), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = HrColors.TextHi)

            val current = state.current
            if (current == null) {
                // Blank while the fetch is still in flight — showing the "no suggestions" copy
                // here would be misleading for a normal-latency load, not just a genuine empty
                // result (both start as an empty `alternatives` list).
                if (!state.isLoadingAlternatives) {
                    Text(
                        text = stringResource(R.string.nutrition_swap_empty),
                        fontFamily = HrBody,
                        fontSize = 14.sp,
                        color = HrColors.TextLow,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                }
            } else {
                val deltaKcal = current.kcal - state.originalKcal
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(HrShapes.CardRegular)
                        // Surface, not SurfaceAccent — TechniquePickerSheet/GenerateSheet/
                        // CalendarDayRow all use SurfaceAccent for a selected/active/today state
                        // (SurfaceAccent also shows up decoratively elsewhere, e.g. RecipeDetailScreen's
                        // ingredient-initial avatar, so it isn't a strict selection-only token — but
                        // a swap candidate is neither a selection state nor that kind of decorative
                        // accent, so plain Surface is the right read here either way).
                        .background(HrColors.Surface)
                        .border(1.dp, HrColors.Border, HrShapes.CardRegular)
                        .clickable(enabled = !state.isApplying, onClick = onApply)
                        // 16.dp, matching RecipeDetailScreen/NutritionLibraryScreen/ProgramsListScreen's
                        // CardRegular interior padding (the 15-16dp modal value) — the legacy
                        // Dimens.CardPaddingLarge (20dp) is an outlier.
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(text = current.recipeName, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HrColors.TextHi)
                    Text(
                        text = stringResource(
                            if (deltaKcal < 0) R.string.nutrition_swap_kcal_delta_less else R.string.nutrition_swap_kcal_delta_more,
                            formatVi(abs(deltaKcal)),
                        ),
                        fontFamily = HrBody,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = if (deltaKcal < 0) HrColors.Accent else HrColors.TextLow,
                    )
                }
                if (state.alternatives.size > 1) {
                    Text(
                        text = stringResource(R.string.nutrition_swap_reroll),
                        fontFamily = HrBody,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = HrColors.Accent,
                        modifier = Modifier
                            .clickable(onClick = onReroll)
                            .padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}
