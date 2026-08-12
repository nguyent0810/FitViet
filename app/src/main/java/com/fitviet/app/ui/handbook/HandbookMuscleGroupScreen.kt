package com.fitviet.app.ui.handbook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.domain.ExerciseDifficulty
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.labelRes

/** Gate E5 — the exercises inside one muscle group; each row carries the exercise's own level
 * badge (the thing the flat by-level list used to convey via its section header, now shown per
 * row since exercises no longer group by level at the top level). */
@Composable
fun HandbookMuscleGroupScreen(
    viewModel: HandbookMuscleGroupViewModel,
    onBack: () -> Unit,
    onExerciseClick: (ExerciseEntity) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundPage)) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BackRow(onBack = onBack)
            Text(text = stringResource(uiState.group.labelRes()), style = MaterialTheme.typography.headlineMedium)
        }
        if (uiState.exercises.isEmpty()) {
            Text(
                text = stringResource(R.string.handbook_exercises_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, top = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = Dimens.ScreenPaddingHorizontal),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.exercises, key = { it.id }) { exercise ->
                    ExerciseRow(exercise = exercise, onClick = { onExerciseClick(exercise) })
                }
            }
        }
    }
}

@Composable
private fun BackRow(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
        Text(text = stringResource(R.string.handbook_title), style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}

@Composable
private fun ExerciseRow(exercise: ExerciseEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(text = exercise.nameVi, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(text = exercise.nameEn, style = MaterialTheme.typography.labelSmall, color = TextFaint)
        }
        DifficultyBadge(code = exercise.difficultyCode)
    }
}

/** A small pill naming the exercise's [ExerciseEntity.difficultyCode] — the per-row equivalent of
 * what the old by-level grouping's section header used to convey. Falls back to rendering nothing
 * for a code that doesn't match a known [ExerciseDifficulty] (shouldn't happen for seed data, but
 * a malformed code silently disappearing is safer than crashing on `valueOf`). */
@Composable
private fun DifficultyBadge(code: String) {
    val level = ExerciseDifficulty.entries.find { it.name == code } ?: return
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(AccentSurfaceSelected)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(text = stringResource(level.labelRes()), style = MaterialTheme.typography.labelSmall, color = Accent)
    }
}
