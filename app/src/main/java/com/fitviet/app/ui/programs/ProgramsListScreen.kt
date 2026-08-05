package com.fitviet.app.ui.programs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.data.repository.ImportProgramResult
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentBorder
import com.fitviet.app.ui.theme.AccentBorderAlt
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.DeepSurface1
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextBody
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProgramsListScreen(
    viewModel: ProgramsViewModel,
    onProgramClick: (ProgramEntity) -> Unit,
    onExerciseClick: (ExerciseEntity) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importMessage by remember { mutableStateOf<String?>(null) }
    val readErrorText = stringResource(R.string.programs_import_read_error)
    val invalidFormatText = stringResource(R.string.programs_import_invalid)
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }
                    .getOrNull()
            }
            importMessage = when {
                text == null -> readErrorText
                else -> when (val result = viewModel.importProgram(text)) {
                    is ImportProgramResult.Success -> if (result.skippedExerciseNames.isEmpty()) {
                        context.getString(R.string.programs_import_success, result.titleVi)
                    } else {
                        context.getString(
                            R.string.programs_import_success_with_skipped,
                            result.titleVi,
                            result.skippedExerciseNames.size,
                        )
                    }
                    ImportProgramResult.InvalidFormat -> invalidFormatText
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenPaddingHorizontal),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.programs_title), style = MaterialTheme.typography.headlineMedium)
                ImportButton(onClick = { importLauncher.launch("*/*") })
            }
        }
        importMessage?.let { message ->
            item {
                ImportMessageCard(message = message, onDismiss = { importMessage = null })
            }
        }
        item {
            SearchField(query = uiState.searchQuery, onQueryChange = viewModel::onSearchQueryChange)
        }
        item {
            FilterChips(selectedIndex = uiState.selectedFilterIndex, onSelect = viewModel::onFilterSelected)
        }
        if (uiState.matchingExercises.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.programs_exercises_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                )
            }
            items(uiState.matchingExercises, key = { "exercise-${it.id}" }) { exercise ->
                ExerciseResultRow(exercise = exercise, onClick = { onExerciseClick(exercise) })
            }
        }
        if (uiState.visiblePrograms.isEmpty() && uiState.matchingExercises.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.programs_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        } else {
            items(uiState.visiblePrograms, key = { it.id }) { program ->
                ProgramCard(program = program, onClick = { onProgramClick(program) })
            }
        }
    }
}

@Composable
private fun ImportButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = Dimens.MinTouchTarget)
            .clip(MaterialTheme.shapes.small)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(R.string.programs_import_button), style = MaterialTheme.typography.labelLarge, color = TextMuted)
    }
}

@Composable
private fun ImportMessageCard(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(AccentSurfaceSelected)
            .border(1.dp, AccentBorder, MaterialTheme.shapes.small)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodySmall, color = TextBody, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ExerciseResultRow(exercise: ExerciseEntity, onClick: () -> Unit) {
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
        Column {
            Text(text = exercise.nameVi, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(text = exercise.primaryMuscle, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        }
        Text(text = "›", style = MaterialTheme.typography.titleMedium, color = TextMuted)
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.small)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (query.isEmpty()) {
            Text(
                text = stringResource(R.string.programs_search_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = TextFaint,
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                color = TextPrimary,
            ),
            cursorBrush = SolidColor(Accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChips(selectedIndex: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PROGRAM_FILTERS.forEachIndexed { index, filter ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(if (selected) Accent else SurfaceCard)
                    .border(1.dp, if (selected) Accent else CardBorder, PillShape)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = stringResource(filter.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) OnAccent else TextMuted,
                )
            }
        }
    }
}

@Composable
private fun ProgramCard(program: ProgramEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .background(DeepSurface1),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "ảnh: ${program.imageAsset}",
                style = MaterialTheme.typography.labelMedium,
                color = TextFaint,
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ProgramTitleRow(program = program)
            Text(
                text = stringResource(
                    R.string.programs_card_meta,
                    program.durationWeeks,
                    program.sessionsPerWeek,
                    program.level,
                    program.equipment,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun ProgramTitleRow(program: ProgramEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = program.titleVi,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .border(1.dp, AccentBorderAlt, MaterialTheme.shapes.extraSmall)
                .padding(horizontal = 7.dp, vertical = 2.dp),
        ) {
            Text(
                text = stringResource(R.string.programs_free_badge),
                style = MaterialTheme.typography.labelSmall,
                color = Accent,
            )
        }
    }
}
