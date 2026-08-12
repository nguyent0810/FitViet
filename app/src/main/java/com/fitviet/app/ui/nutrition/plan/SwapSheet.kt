package com.fitviet.app.ui.nutrition.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fitviet.app.R
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.formatVi
import kotlin.math.abs

/**
 * Gate C8 — driven entirely by [state] + callbacks (same dumb-sheet idiom as
 * [com.fitviet.app.ui.profile.UpdateMeasurementSheet]/[com.fitviet.app.ui.workout.TechniquePickerSheet]),
 * owned by [PlanViewModel]. Shows one candidate at a time from the already-fetched
 * [SwapUiState.alternatives] pool; tapping the candidate card applies it directly (no separate
 * confirm step), "Gợi ý khác ↻" cycles to the next candidate in the pool.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapSheet(
    state: SwapUiState,
    onReroll: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = stringResource(R.string.nutrition_swap_title), style = MaterialTheme.typography.titleMedium)

            val current = state.current
            if (current == null) {
                // Blank while the fetch is still in flight — showing the "no suggestions" copy
                // here would be misleading for a normal-latency load, not just a genuine empty
                // result (both start as an empty `alternatives` list).
                if (!state.isLoadingAlternatives) {
                    Text(
                        text = stringResource(R.string.nutrition_swap_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                }
            } else {
                val deltaKcal = current.kcal - state.originalKcal
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(SurfaceCard)
                        .border(1.dp, CardBorder, MaterialTheme.shapes.large)
                        .clickable(enabled = !state.isApplying, onClick = onApply)
                        .padding(Dimens.CardPaddingLarge),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(text = current.recipeName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        text = stringResource(
                            if (deltaKcal < 0) R.string.nutrition_swap_kcal_delta_less else R.string.nutrition_swap_kcal_delta_more,
                            formatVi(abs(deltaKcal)),
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (deltaKcal < 0) Accent else TextMuted,
                    )
                }
                if (state.alternatives.size > 1) {
                    Text(
                        text = stringResource(R.string.nutrition_swap_reroll),
                        style = MaterialTheme.typography.labelLarge,
                        color = Accent,
                        modifier = Modifier
                            .clickable(onClick = onReroll)
                            .padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}
