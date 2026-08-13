package com.fitviet.app.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.domain.ExerciseHistoryEntry
import com.fitviet.app.ui.common.HrBackChip
import com.fitviet.app.ui.common.LoadingSkeleton
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.util.formatWeight

/** Redesign Gate 3d — migrated to Hr tokens, same pass as [com.fitviet.app.ui.handbook.HandbookScreen]/
 * [com.fitviet.app.ui.handbook.HandbookMuscleGroupScreen]. [ExerciseMediaBox] (the photo/gif box)
 * is deliberately left untouched — per its own doc it's shared with the live-session logging screen,
 * which isn't re-skinned until Phase 4, so changing it here would show the new look on an otherwise
 * still-legacy screen. The multi-hue [InvolvementRow] bar palette (old `MacroBarCarb`/`MacroBarFat`)
 * has no Hr equivalent — the Hr palette is single-accent by design (see `HrColors`'s own doc) — so
 * tiers beyond the first now fade [HrColors.Accent]'s own alpha instead of switching hue, extending
 * the pre-Gate-3d code's existing 4th-tier-and-beyond fallback to every tier rather than introducing
 * a new pattern.
 */
@Composable
fun ExerciseDetailScreen(viewModel: ExerciseDetailViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(HrColors.Bg)) {
        Row(
            modifier = Modifier.padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HrBackChip(onClick = onBack)
            Text(text = stringResource(R.string.exercise_back), fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextLow)
        }

        if (uiState.isLoading) {
            LoadingSkeleton(modifier = Modifier.padding(horizontal = HrDimens.ScreenPaddingHorizontal))
        } else {
            uiState.exercise?.let { exercise ->
                ExerciseDetailContent(
                    exercise = exercise,
                    history = uiState.history,
                    isAdded = uiState.isAdded,
                    onToggleAdded = viewModel::toggleAdded,
                )
            }
        }
    }
}

private enum class ExerciseDetailTab { HOW_TO, MUSCLES, PROGRESS }

@Composable
private fun ExerciseDetailTab.label(): String = stringResource(
    when (this) {
        ExerciseDetailTab.HOW_TO -> R.string.exercise_tab_howto
        ExerciseDetailTab.MUSCLES -> R.string.exercise_tab_muscles
        ExerciseDetailTab.PROGRESS -> R.string.exercise_tab_progress
    },
)

@Composable
private fun ExerciseDetailContent(
    exercise: ExerciseEntity,
    history: List<ExerciseHistoryEntry>,
    isAdded: Boolean,
    onToggleAdded: () -> Unit,
) {
    // Feature #10 (Gate 46) — plain underline tabs, not Material TabRow/pills, per the plan.
    // rememberSaveable (not remember) so the selected tab survives a config change, e.g. rotation.
    var selectedTab by rememberSaveable { mutableStateOf(ExerciseDetailTab.HOW_TO) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HrDimens.ScreenPaddingHorizontal),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ExerciseMediaBox(exercise = exercise)

            Column {
                Text(text = exercise.nameVi, fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = HrColors.TextHi)
                Text(text = exercise.nameEn, fontFamily = HrBody, fontSize = 12.sp, color = HrColors.TextFaint)
            }

            ExerciseDetailTabRow(selected = selectedTab, onSelect = { selectedTab = it })

            when (selectedTab) {
                ExerciseDetailTab.HOW_TO -> HowToTabContent(exercise)
                ExerciseDetailTab.MUSCLES -> MusclesTabContent(exercise)
                ExerciseDetailTab.PROGRESS -> ProgressTabContent(history)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 16.dp)
                .clip(HrShapes.ButtonCta)
                .background(if (isAdded) Color.Transparent else HrColors.Accent)
                .border(Dimens.SelectedBorderWidth, HrColors.Accent, HrShapes.ButtonCta)
                .clickable(onClick = onToggleAdded)
                .padding(vertical = HrDimens.CtaPaddingVertical),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(if (isAdded) R.string.exercise_added else R.string.exercise_add_to_workout),
                fontFamily = HrBody,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isAdded) HrColors.Accent else HrColors.OnAccent,
            )
        }
    }
}

/** Left-aligned, natural-width tabs with an 8dp gap (mockup — not stretched/centered across the
 * full row) over a 1dp [HrColors.Border] rail running the whole row's width, so unselected tabs
 * still sit on a visible baseline instead of floating with nothing under them; each tab's 2dp
 * [HrColors.Accent] underline sits directly on top of that rail when selected.
 *
 * Gate E6 — the tab's own vertical padding (was 10dp top+bottom around the label, plus another
 * 6dp before the underline) combined with the parent content Column's blanket 16dp inter-section
 * spacing above and below this whole row read as excess empty space around the tab strip — user
 * feedback specifically named "the 'Cách tập' text and the line under it" as the deadspace source.
 * Tightened the underline's top padding to 4dp; the tab's own vertical padding stays 10dp so the
 * tap target holds at [Dimens.MinTouchTarget] (44dp = 10 + 18sp line height + 4 + 2dp underline +
 * 10) rather than dropping below it.
 *
 * Also fixes the actual root cause the padding tweak alone didn't: each tab's underline [Box] used
 * `fillMaxWidth()` inside an unweighted [Row] child, which measures every non-weighted child with
 * the *same* full incoming max-width constraint rather than shrinking per sibling — so each tab's
 * [Column] sized itself to the underline's full-row-width Box, not its own [Text]'s intrinsic
 * width. The first tab (HOW_TO/"Cách tập") ended up claiming the entire row, pushing MUSCLES/PROGRESS
 * off-screen with nothing to scroll them into view, and its now-full-width underline plus the tall
 * blank band below is exactly what read as "the text and the line under it, too much empty space."
 * `Modifier.width(IntrinsicSize.Max)` on each tab's Column makes it size to its own content's max
 * intrinsic width (the [Text] label) instead of the underline's greedy fillMaxWidth. */
@Composable
private fun ExerciseDetailTabRow(selected: ExerciseDetailTab, onSelect: (ExerciseDetailTab) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExerciseDetailTab.entries.forEach { tab ->
                val isSelected = tab == selected
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .clickable { onSelect(tab) }
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = tab.label(),
                        fontFamily = HrBody,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        // Explicit, matching the doc comment's own "44dp = 10 + 18sp line height +
                        // 4 + 2dp underline + 10" math below — HrBody's tight metrics default to
                        // ~14sp here (Archivo's typo line-gap is unusually small), which would
                        // silently drop the tap target to ~40dp without this.
                        lineHeight = 18.sp,
                        color = if (isSelected) HrColors.Accent else HrColors.TextLow,
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (isSelected) HrColors.Accent else Color.Transparent),
                    )
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(HrColors.Border))
    }
}

@Composable
private fun HowToTabContent(exercise: ExerciseEntity) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MuscleChip(text = exercise.equipment, highlighted = false)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(HrShapes.CardRegular)
                .background(HrColors.Surface)
                .border(1.dp, HrColors.Border, HrShapes.CardRegular)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringResource(R.string.exercise_instructions_title), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HrColors.TextHi)
            exercise.instructions.forEachIndexed { index, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = (index + 1).toString(),
                        fontFamily = HrDisplay,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = HrColors.Accent,
                    )
                    Text(
                        text = step,
                        fontFamily = HrBody,
                        fontSize = 13.sp,
                        // Matches Gate 2a's own precedent (OnboardingScreen.kt's wrapped HrBody
                        // card copy) — without it, Vietnamese diacritics on wrapped lines collide
                        // with the line above's descenders (Archivo's tight metrics default to
                        // ~14sp here, well under the glyphs' own ascent+descent).
                        lineHeight = 20.sp,
                        color = HrColors.TextLow,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SuggestedTile(
                value = stringResource(R.string.exercise_sets_range, exercise.suggestedSetsMin, exercise.suggestedSetsMax),
                label = stringResource(R.string.exercise_tile_sets),
                modifier = Modifier.weight(1f),
            )
            SuggestedTile(
                value = stringResource(R.string.exercise_sets_range, exercise.suggestedRepsMin, exercise.suggestedRepsMax),
                label = stringResource(R.string.exercise_tile_reps),
                modifier = Modifier.weight(1f),
            )
            SuggestedTile(
                value = stringResource(R.string.exercise_rest_seconds, exercise.suggestedRestSeconds),
                label = stringResource(R.string.exercise_tile_rest),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Feature #10 (Gate 46) — muscle chips render unconditionally (they only ever depend on
 * [ExerciseEntity.primaryMuscle]/`secondaryMuscles`, always present), so this tab is never blank
 * even for exercises where Gate 44's `involvementPercents` is empty (cardio/stretching/functional
 * movements) — the chips are the "falls back to existing primary/secondary muscle text" the plan
 * asks for, and [MuscleInvolvementCard] simply adds nothing below them in that case. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MusclesTabContent(exercise: ExerciseEntity) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MuscleChip(text = exercise.primaryMuscle, highlighted = true)
            exercise.secondaryMuscles.forEach { MuscleChip(text = it, highlighted = false) }
        }
        MuscleInvolvementCard(exercise = exercise)
    }
}

/** Feature #10 (Gate 46) — [history] is already reduced to one entry per date (that date's
 * heaviest set, newest first) by [com.fitviet.app.domain.ExerciseHistoryCalculator]; this just
 * renders it as a plain date/value row list, matching this app's existing history-list convention
 * (`MeasurementHistoryRow` in `ui/profile/MeasurementHistorySheet.kt`) rather than a chart — no
 * chart library or hand-rolled `Canvas` drawing exists anywhere else in this codebase for a
 * time-series like this one. */
@Composable
private fun ProgressTabContent(history: List<ExerciseHistoryEntry>) {
    if (history.isEmpty()) {
        Text(
            text = stringResource(R.string.exercise_progress_empty),
            fontFamily = HrBody,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = HrColors.TextLow,
            modifier = Modifier.padding(vertical = 24.dp),
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        history.forEach { entry -> ProgressHistoryRow(entry) }
    }
}

@Composable
private fun ProgressHistoryRow(entry: ExerciseHistoryEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${entry.date.dayOfMonth}/${entry.date.monthValue}/${entry.date.year}",
            fontFamily = HrBody,
            fontSize = 13.sp,
            color = HrColors.TextHi,
        )
        Text(
            text = "${formatWeight(entry.weightKg)} kg × ${stringResource(R.string.exercise_progress_reps, entry.reps)}",
            fontFamily = HrBody,
            fontSize = 13.sp,
            color = HrColors.TextLow,
        )
    }
}

@Composable
private fun MuscleChip(text: String, highlighted: Boolean) {
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(if (highlighted) HrColors.SurfaceAccent else HrColors.Surface)
            .border(1.dp, if (highlighted) HrColors.BorderAccentDim else HrColors.Border, PillShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            fontFamily = HrBody,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = if (highlighted) HrColors.Accent else HrColors.TextLow,
        )
    }
}

/** Feature #9b (Gate 45) — renders Gate 44's validated `involvementPercents`; hides cleanly (no
 * empty card, no placeholder) when the list is empty, which is the deliberate "not every exercise
 * gets a breakdown" signal from that gate, not missing data. */
@Composable
private fun MuscleInvolvementCard(exercise: ExerciseEntity) {
    if (exercise.involvementPercents.isEmpty()) return
    val muscleLabels = listOf(exercise.primaryMuscle) + exercise.secondaryMuscles

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.exercise_involvement_title), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HrColors.TextHi)
        muscleLabels.forEachIndexed { index, label ->
            val percent = exercise.involvementPercents.getOrElse(index) { 0 }
            InvolvementRow(label = label, percent = percent, color = involvementBarColor(index))
        }
        Text(
            text = stringResource(R.string.exercise_involvement_caption),
            fontFamily = HrBody,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = HrColors.TextFaint,
        )
    }
}

@Composable
private fun InvolvementRow(label: String, percent: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label,
            fontFamily = HrBody,
            fontSize = 12.sp,
            color = HrColors.TextLow,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(96.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(HrShapes.CardSmall)
                .background(HrColors.BarDim),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                    .clip(HrShapes.CardSmall)
                    .background(color),
            )
        }
        Text(
            text = "$percent%",
            fontFamily = HrDisplay,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            // Fixed, not tied to the bar's own fill color (review finding) — a faded tier-3/4 bar
            // color reads at ~2.7:1 against the card, below WCAG's 4.5:1 text minimum. The bar
            // itself already encodes the tier via its fill length/color; this number doesn't need
            // to repeat that encoding at the cost of legibility.
            color = HrColors.TextMid,
            textAlign = TextAlign.End,
            modifier = Modifier.width(44.dp),
        )
    }
}

/** Descending [HrColors.Accent] alpha, tier by tier — the Hr palette is single-accent by design
 * (see `HrColors`'s own doc), unlike the old palette's separate `MacroBarCarb`/`MacroBarFat` hues,
 * so every tier beyond the first now fades the one accent color rather than switching hue. No
 * seeded exercise (post Gate-44's `FUNCTIONAL` exclusion) has more than 5 displayed muscles, but a
 * 4th+ tier still degrades sensibly by fading further rather than repeating a color or crashing on
 * an unmapped index.
 *
 * Review finding (Gate 3d) — the first version of this fade (starting at tier 1, floored at 0.35)
 * dropped tier-3/4 bar fills to ~3:1 contrast against [HrColors.BarDim], below the 3:1 WCAG
 * non-text minimum, and several seeded exercises (`SeedData.kt`) actually reach 5 tiers of
 * near-equal-width bars where alpha is the only thing distinguishing them. A gentler slope with a
 * higher floor keeps every tier's fill legible; see [InvolvementRow]'s own doc for why the `%`
 * label text no longer reuses this color. */
private fun involvementBarColor(index: Int): Color = when (index) {
    0 -> HrColors.Accent
    else -> HrColors.Accent.copy(alpha = (1f - 0.15f * index).coerceAtLeast(0.55f))
}

@Composable
private fun SuggestedTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value, fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = HrColors.TextHi)
        Text(text = label, fontFamily = HrBody, fontSize = 12.sp, color = HrColors.TextLow, modifier = Modifier.padding(top = 2.dp))
    }
}
