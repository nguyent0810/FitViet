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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import com.fitviet.app.ui.exercise.ExerciseMediaBox
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentBorder
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.DeepSurface1
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.OnAccent
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
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundPage)) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
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
            Text(
                text = uiState.dayTitleVi.ifBlank { stringResource(R.string.workout_preview_title) },
                style = MaterialTheme.typography.headlineMedium,
            )
            if (uiState.showSupersetHint) {
                SupersetHintCard(onDismiss = viewModel::dismissSupersetHint)
            }
            if (uiState.groupings.isEmpty()) {
                Text(text = stringResource(R.string.workout_preview_empty), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.groupings.forEach { grouping ->
                        when (grouping) {
                            is ResolvedGrouping.Solo -> PreviewExerciseCard(item = grouping.item)
                            is ResolvedGrouping.Paired -> PreviewSupersetCard(first = grouping.first, second = grouping.second)
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
private fun PreviewExerciseCard(item: ProgramDayWorkoutItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large),
    ) {
        PreviewExerciseContent(item = item, badge = null)
    }
}

/** A superset pair (Gate 48) — renders exactly the grouping
 * [ProgramDayWorkoutPlanner.resolveGroupings] resolved, in the same
 * `DeepSurface1`/`AccentBorder` "group card" the plan specifies, with each exercise inside its own
 * normal card labelled A1/A2 (reusing the live session's [R.string.superset_badge_a1]/`_a2`/
 * [R.string.superset_badge]/[R.string.superset_no_rest_note] strings, so the preview and the live
 * session describe the same pairing the same way). Ungrouped exercises never reach this composable
 * — they render as a plain [PreviewExerciseCard], unchanged from before this gate. */
@Composable
private fun PreviewSupersetCard(first: ProgramDayWorkoutItem, second: ProgramDayWorkoutItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(DeepSurface1)
            .border(1.dp, AccentBorder, MaterialTheme.shapes.large)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(Accent)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(text = stringResource(R.string.superset_badge), style = MaterialTheme.typography.labelSmall, color = OnAccent)
        }
        SupersetExerciseCard(item = first, badge = stringResource(R.string.superset_badge_a1))
        SupersetConnector()
        SupersetExerciseCard(item = second, badge = stringResource(R.string.superset_badge_a2))
    }
}

@Composable
private fun SupersetExerciseCard(item: ProgramDayWorkoutItem, badge: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.medium),
    ) {
        PreviewExerciseContent(item = item, badge = badge)
    }
}

/** The 2dp Accent connector line + "không nghỉ" divider between a superset pair's two exercise
 * cards. */
@Composable
private fun SupersetConnector() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(modifier = Modifier.width(2.dp).height(12.dp).background(Accent))
        Text(text = stringResource(R.string.superset_no_rest_note), style = MaterialTheme.typography.labelSmall, color = TextFaint)
        Box(modifier = Modifier.width(2.dp).height(12.dp).background(Accent))
    }
}

/** Media + name + sets/reps/weight summary shared by both a plain [PreviewExerciseCard] and a
 * [PreviewSupersetCard]'s two halves — [badge] is the 26dp A1/A2 tile when this exercise is part of
 * a superset pair, `null` for a standalone exercise. */
@Composable
private fun PreviewExerciseContent(item: ProgramDayWorkoutItem, badge: String?) {
    ExerciseMediaBox(exercise = item.exercise, height = 140.dp)
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (badge != null) {
            Box(
                modifier = Modifier.size(26.dp).clip(MaterialTheme.shapes.extraSmall).background(Accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = badge, style = MaterialTheme.typography.labelMedium.copy(fontFamily = Anton), color = OnAccent)
            }
        }
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
    }
}

/** First-run superset explainer (Gate 48) — an inline card in the scroll content, not a floating
 * tooltip, per the plan. Dismissing persists [SettingsEntity.hasSeenSupersetHint][
 * com.fitviet.app.data.local.entity.SettingsEntity.hasSeenSupersetHint] so it doesn't show again. */
@Composable
private fun SupersetHintCard(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(AccentSurfaceSelected)
            .border(1.dp, AccentBorder, MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = stringResource(R.string.workout_preview_superset_hint_title), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        Text(text = stringResource(R.string.workout_preview_superset_hint_body), style = MaterialTheme.typography.bodySmall, color = TextBody)
        Text(
            text = stringResource(R.string.workout_preview_superset_hint_dismiss),
            style = MaterialTheme.typography.labelLarge,
            color = Accent,
            modifier = Modifier.align(Alignment.End).clickable(onClick = onDismiss),
        )
    }
}
