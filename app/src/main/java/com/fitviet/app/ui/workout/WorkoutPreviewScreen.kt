package com.fitviet.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.ui.common.LoadingSkeleton
import com.fitviet.app.ui.exercise.ExerciseMediaBox
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentBorder
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.DeepSurface1
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextBody
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.formatWeight

/** The "day exercise list" screen (Gate 24) shown after tapping today's row on the Weekly Schedule
 * screen — lists what today's session holds before the user commits to starting it. */
@Composable
fun WorkoutPreviewScreen(
    viewModel: WorkoutPreviewViewModel,
    onBack: () -> Unit,
    onBeginWorkout: () -> Unit,
    // Gate E4 — tapping an exercise card opens its "cách tập" (how-to) detail, so this overview
    // isn't a dead-end that only offers committing to the whole session.
    onExerciseClick: (exerciseId: Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundPage)) {
        if (uiState.isLoading) {
            LoadingSkeleton(modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp))
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BackRow(onBack = onBack)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = uiState.dayTitleVi.ifBlank { stringResource(R.string.workout_preview_title) },
                    style = MaterialTheme.typography.headlineMedium,
                )
                // Uses the same per-rep/rest assumptions ProgramDayWorkoutPlanner.estimateDurationMinutes
                // relies on, so committing to "Bắt đầu tập" isn't a blind guess — the one thing this
                // screen's own purpose (reduce pre-workout commitment anxiety) most needed and didn't have.
                if (uiState.estimatedDurationMinutes > 0) {
                    Text(
                        text = stringResource(R.string.workout_preview_estimated_duration, uiState.estimatedDurationMinutes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextFaint,
                    )
                }
            }
            if (uiState.showSupersetHint) {
                SupersetHintCard(onDismiss = viewModel::dismissSupersetHint)
            }
            if (uiState.groupings.isEmpty()) {
                Text(text = stringResource(R.string.workout_preview_empty), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.groupings.forEach { grouping ->
                        when (grouping) {
                            is ResolvedGrouping.Solo -> PreviewExerciseCard(item = grouping.item, onClick = { onExerciseClick(grouping.item.exercise.id) })
                            is ResolvedGrouping.Paired -> PreviewSupersetCard(first = grouping.first, second = grouping.second, onExerciseClick = onExerciseClick)
                        }
                    }
                }
            }
        }

        if (uiState.groupings.isNotEmpty()) {
            Box(modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp)) {
                PrimaryActionButton(text = stringResource(R.string.dashboard_start_workout), onClick = onBeginWorkout)
            }
        }
    }
}

@Composable
private fun BackRow(onBack: () -> Unit) {
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
}

@Composable
private fun PreviewExerciseCard(item: ProgramDayWorkoutItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
        PreviewExerciseContent(item = item, badge = null)
    }
}

/** A superset pair (Gate 48) — renders exactly the grouping
 * [ProgramDayWorkoutPlanner.resolveGroupings] resolved: a round-letter badge + connector line in a
 * gutter to the left of the group card (mirrors the mockup's external gutter, not an inline pill),
 * a header naming the round count ([ProgramDayWorkoutPlanner.supersetRounds] — the same figure the
 * live session's block plan uses) and the between-round rest ([DEFAULT_REST_SECONDS], the same
 * constant the live session's rest timer counts down from), then each exercise inside its own
 * normal card labelled A1/A2 (reusing the live session's [R.string.superset_badge_a1]/`_a2`/
 * [R.string.superset_no_rest_note] strings, so the preview and the live session describe the same
 * pairing the same way). Ungrouped exercises never reach this composable — they render as a plain
 * [PreviewExerciseCard], unchanged from before this gate. */
@Composable
private fun PreviewSupersetCard(first: ProgramDayWorkoutItem, second: ProgramDayWorkoutItem, onExerciseClick: (exerciseId: Long) -> Unit) {
    val groupLetter = first.supersetGroup ?: "A"
    val rounds = ProgramDayWorkoutPlanner.supersetRounds(first, second)
    // IntrinsicSize.Min, not a bare Row: this screen's content sits inside a verticalScroll
    // Column, which measures children with unbounded height. A plain Row would pass that same
    // unbounded height down to the gutter Column below, and weight() on its connector line would
    // hit Compose's "infinite height with weight" crash. IntrinsicSize.Min bounds the Row's height
    // to its tallest child's intrinsic height (the group card) before either child is weight-measured.
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.width(26.dp).padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(AccentSurfaceSelected)
                    .border(1.5.dp, Accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = groupLetter, style = MaterialTheme.typography.labelSmall.copy(fontFamily = Anton), color = Accent)
            }
            Box(modifier = Modifier.weight(1f).width(2.dp).background(Accent))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.large)
                .background(DeepSurface1)
                .border(1.5.dp, AccentBorder, MaterialTheme.shapes.large)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.superset_group_header, groupLetter, rounds),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.08.em),
                    color = Accent,
                )
                Text(
                    text = stringResource(R.string.superset_group_rest, DEFAULT_REST_SECONDS),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextFaint,
                )
            }
            SupersetExerciseCard(item = first, badge = stringResource(R.string.superset_badge_a1), onClick = { onExerciseClick(first.exercise.id) })
            SupersetConnector()
            SupersetExerciseCard(item = second, badge = stringResource(R.string.superset_badge_a2), onClick = { onExerciseClick(second.exercise.id) })
        }
    }
}

@Composable
private fun SupersetExerciseCard(item: ProgramDayWorkoutItem, badge: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Must match ExerciseMediaBox's own hardcoded `shapes.large` (inside
            // PreviewExerciseContent) — a mismatched radius here would leave the media box's own
            // corner border visible poking out past this card's border.
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
        PreviewExerciseContent(item = item, badge = badge)
    }
}

/** The "không nghỉ" divider between a superset pair's two exercise cards — flanking 1dp
 * [AccentBorder] lines, not the vertical bars used before this gate's mockup pass. */
@Composable
private fun SupersetConnector() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(AccentBorder))
        Text(text = stringResource(R.string.superset_no_rest_note), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Accent)
        Box(modifier = Modifier.weight(1f).height(1.dp).background(AccentBorder))
    }
}

/** Media + name + sets/reps/weight summary shared by both a plain [PreviewExerciseCard] and a
 * [PreviewSupersetCard]'s two halves — [badge] is the Anton-styled "A1"/"A2" label at the row's
 * trailing edge when this exercise is part of a superset pair, `null` for a standalone exercise. */
@Composable
private fun PreviewExerciseContent(item: ProgramDayWorkoutItem, badge: String?) {
    ExerciseMediaBox(exercise = item.exercise, height = 140.dp)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = if (badge != null) Arrangement.SpaceBetween else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = item.exercise.nameVi, style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(
                    R.string.workout_preview_summary,
                    item.targetSets,
                    item.targetRepsMin,
                    item.targetRepsMax,
                    formatWeight(item.recommendedWeightKg),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = TextFaint,
            )
        }
        if (badge != null) {
            Text(text = badge, style = MaterialTheme.typography.titleSmall.copy(fontFamily = Anton), color = Accent)
        }
    }
}

/** First-run superset explainer (Gate 48) — an inline card in the scroll content, not a floating
 * tooltip, per the plan. Dismissing persists [SettingsEntity.hasSeenSupersetHint][
 * com.fitviet.app.data.local.entity.SettingsEntity.hasSeenSupersetHint] so it doesn't show again.
 * Bordered in bright [Accent], not [AccentBorder] — the group card below uses the muted border, so
 * this explainer needs to read as visually distinct from the thing it explains. */
@Composable
private fun SupersetHintCard(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(AccentSurfaceSelected)
            .border(1.5.dp, Accent, MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = stringResource(R.string.workout_preview_superset_hint_title), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        Text(text = stringResource(R.string.workout_preview_superset_hint_body), style = MaterialTheme.typography.bodySmall, color = TextBody)
        Box(
            modifier = Modifier
                .clip(PillShape)
                .border(1.dp, AccentBorder, PillShape)
                .clickable(onClick = onDismiss)
                .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Text(
                text = stringResource(R.string.workout_preview_superset_hint_dismiss),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Accent,
            )
        }
    }
}
