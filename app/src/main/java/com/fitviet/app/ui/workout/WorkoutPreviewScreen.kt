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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted
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
            if (uiState.items.isEmpty()) {
                Text(text = stringResource(R.string.workout_preview_empty), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.items.forEach { item -> PreviewExerciseCard(item = item) }
                }
            }
        }

        if (uiState.items.isNotEmpty()) {
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
        ExerciseMediaBox(exercise = item.exercise, height = 140.dp)
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
