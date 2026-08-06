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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentBorder
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.MacroBarCarb
import com.fitviet.app.ui.theme.MacroBarFat
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary

@Composable
fun ExerciseDetailScreen(viewModel: ExerciseDetailViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
            Text(text = stringResource(R.string.exercise_back), style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
        } else {
            uiState.exercise?.let { exercise ->
                ExerciseDetailContent(exercise = exercise, isAdded = uiState.isAdded, onToggleAdded = viewModel::toggleAdded)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseDetailContent(exercise: ExerciseEntity, isAdded: Boolean, onToggleAdded: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPaddingHorizontal),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ExerciseMediaBox(exercise = exercise)

            Column {
                Text(text = exercise.nameVi, style = MaterialTheme.typography.headlineMedium)
                Text(text = exercise.nameEn, style = MaterialTheme.typography.bodySmall, color = TextFaint)
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MuscleChip(text = exercise.primaryMuscle, highlighted = true)
                exercise.secondaryMuscles.forEach { MuscleChip(text = it, highlighted = false) }
                MuscleChip(text = exercise.equipment, highlighted = false)
            }

            MuscleInvolvementCard(exercise = exercise)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(SurfaceCard)
                    .border(1.dp, CardBorder, MaterialTheme.shapes.large)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = stringResource(R.string.exercise_instructions_title), style = MaterialTheme.typography.titleSmall)
                exercise.instructions.forEachIndexed { index, step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = Anton),
                            color = Accent,
                        )
                        Text(text = step, style = MaterialTheme.typography.bodyMedium, color = TextMuted, modifier = Modifier.weight(1f))
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp)
                .clip(MaterialTheme.shapes.large)
                .background(if (isAdded) Color.Transparent else Accent)
                .border(Dimens.SelectedBorderWidth, Accent, MaterialTheme.shapes.large)
                .clickable(onClick = onToggleAdded)
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(if (isAdded) R.string.exercise_added else R.string.exercise_add_to_workout),
                style = MaterialTheme.typography.titleMedium,
                color = if (isAdded) Accent else OnAccent,
            )
        }
    }
}

@Composable
private fun MuscleChip(text: String, highlighted: Boolean) {
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(if (highlighted) AccentSurfaceSelected else SurfaceCard)
            .border(1.dp, if (highlighted) AccentBorder else CardBorder, PillShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (highlighted) Accent else TextMuted,
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
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.exercise_involvement_title), style = MaterialTheme.typography.titleSmall)
        muscleLabels.forEachIndexed { index, label ->
            val percent = exercise.involvementPercents.getOrElse(index) { 0 }
            InvolvementRow(label = label, percent = percent, color = involvementBarColor(index))
        }
        Text(
            text = stringResource(R.string.exercise_involvement_caption),
            style = MaterialTheme.typography.labelSmall,
            color = TextFaint,
        )
    }
}

@Composable
private fun InvolvementRow(label: String, percent: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(96.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(CardBorder),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(color),
            )
        }
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = Anton),
            color = color,
            textAlign = TextAlign.End,
            modifier = Modifier.width(44.dp),
        )
    }
}

/** Descending Accent→[MacroBarCarb]→[MacroBarFat], per the plan — this app's existing macro-bar
 * palette, reused rather than introducing new hues (matches the single-accent-green convention
 * established since Gate 35's avatar colors). No seeded exercise (post Gate-44's `FUNCTIONAL`
 * exclusion) has more than 5 displayed muscles, but a 4th+ tier still degrades sensibly by fading
 * [MacroBarFat] further rather than repeating a color or crashing on an unmapped index. */
private fun involvementBarColor(index: Int): Color = when (index) {
    0 -> Accent
    1 -> MacroBarCarb
    2 -> MacroBarFat
    else -> MacroBarFat.copy(alpha = (1f - 0.15f * (index - 2)).coerceAtLeast(0.4f))
}

@Composable
private fun SuggestedTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.medium)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = Anton), color = TextPrimary)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
    }
}
