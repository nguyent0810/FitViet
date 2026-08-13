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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.domain.ExerciseDifficulty
import com.fitviet.app.ui.common.HrBackChip
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.util.labelRes

/** Gate E5 — the exercises inside one muscle group; each row carries the exercise's own level
 * badge (the thing the flat by-level list used to convey via its section header, now shown per
 * row since exercises no longer group by level at the top level). Redesign Gate 3d — migrated to
 * Hr tokens, same pass as [HandbookScreen]. */
@Composable
fun HandbookMuscleGroupScreen(
    viewModel: HandbookMuscleGroupViewModel,
    onBack: () -> Unit,
    onExerciseClick: (ExerciseEntity) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(HrColors.Bg)) {
        Column(
            modifier = Modifier.padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BackRow(onBack = onBack)
            Text(text = stringResource(uiState.group.labelRes()), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = HrColors.TextHi)
        }
        if (uiState.exercises.isEmpty()) {
            Text(
                text = stringResource(R.string.handbook_exercises_empty),
                fontFamily = HrBody,
                fontSize = 14.sp,
                // Explicit lineHeight — see HandbookScreen's EmptyText doc (same review finding).
                lineHeight = 20.sp,
                color = HrColors.TextLow,
                modifier = Modifier.padding(horizontal = HrDimens.ScreenPaddingHorizontal).padding(top = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = HrDimens.ScreenPaddingHorizontal),
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
        HrBackChip(onClick = onBack)
        Text(text = stringResource(R.string.handbook_title), fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextLow)
    }
}

@Composable
private fun ExerciseRow(exercise: ExerciseEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(text = exercise.nameVi, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HrColors.TextHi)
            Text(text = exercise.nameEn, fontFamily = HrBody, fontSize = 11.sp, color = HrColors.TextFaint)
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
            .clip(HrShapes.CardSmall)
            .background(HrColors.SurfaceAccent)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(text = stringResource(level.labelRes()), fontFamily = HrBody, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = HrColors.Accent)
    }
}
