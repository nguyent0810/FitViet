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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
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
import com.fitviet.app.domain.ProgramDifficulty
import com.fitviet.app.ui.common.pressScale
import com.fitviet.app.ui.common.tiltOnDrag
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentBorder
import com.fitviet.app.ui.theme.AccentBorderAlt
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.ChartBarIdle
import com.fitviet.app.ui.theme.DeepSurface1
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HeroGradientEnd
import com.fitviet.app.ui.theme.HeroGradientStart
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.ui.theme.premiumShadow
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
    // "Hit & Run" (Gate 63+) — the header card below the title row, per the plan's Quick Generate
    // entry-point list (Dashboard's empty state + this screen's own header, both reachable
    // independent of whether the user has ever generated a plan before).
    onGenerateMonthlyPlan: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importMessage by remember { mutableStateOf<String?>(null) }
    val readErrorText = stringResource(R.string.programs_import_read_error)
    val invalidFormatText = stringResource(R.string.programs_import_invalid)
    val failedText = stringResource(R.string.programs_import_failed)
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
                    ImportProgramResult.Failed -> failedText
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
        item {
            GenerateMonthlyPlanHeaderCard(onClick = onGenerateMonthlyPlan)
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

/** "Hit & Run" (Gate 63+) entry point — same accent-tinted-card treatment as
 * [com.fitviet.app.ui.dashboard.DashboardScreen]'s own Quick Generate CTA, so the feature reads
 * consistently wherever it's offered. */
@Composable
private fun GenerateMonthlyPlanHeaderCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(onClick = onClick)
            .clip(MaterialTheme.shapes.medium)
            .background(AccentSurfaceSelected)
            .border(1.dp, AccentBorder, MaterialTheme.shapes.medium)
            .padding(Dimens.CardPaddingSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(text = stringResource(R.string.programs_generate_cta_title), style = MaterialTheme.typography.labelLarge, color = Accent)
            Text(
                text = stringResource(R.string.programs_generate_cta_body),
                style = MaterialTheme.typography.bodySmall,
                color = TextBody,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(text = stringResource(R.string.programs_generate_cta_button), style = MaterialTheme.typography.labelLarge, color = Accent)
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

/**
 * Programs have no bundled cover photo (no backend, no image downloads) — this draws a deterministic
 * gradient + glyph "cover" per program instead of the old placeholder text box that just printed the
 * raw [ProgramEntity.imageAsset] filename (e.g. "ảnh: fullbody-8-tuan.jpg") with nothing behind it.
 * Gradient and glyph are picked from [program.titleVi]'s hash, so the same program always renders the
 * same cover, and glyph choice reads [program.tags] to hint at the program's focus (fat loss vs.
 * strength) rather than being purely decorative.
 */
private val PROGRAM_COVER_GRADIENTS = listOf(
    HeroGradientStart to HeroGradientEnd,
    Color(0xFF1B2A20) to Color(0xFF0E1712),
    Color(0xFF16241C) to Color(0xFF0B120D),
    AccentSurfaceSelected to DeepSurface1,
)

private enum class ProgramCoverGlyph { FLAME, BARBELL, CHART }

private fun glyphFor(program: ProgramEntity): ProgramCoverGlyph = when {
    program.tags.contains("Giảm mỡ") -> ProgramCoverGlyph.FLAME
    program.tags.contains("Tăng cơ") -> ProgramCoverGlyph.BARBELL
    else -> ProgramCoverGlyph.CHART
}

@Composable
private fun ProgramCoverArt(program: ProgramEntity, modifier: Modifier = Modifier) {
    val gradient = remember(program.titleVi) {
        val index = (program.titleVi.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }) % PROGRAM_COVER_GRADIENTS.size
        PROGRAM_COVER_GRADIENTS[index]
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(gradient.first, gradient.second))),
        contentAlignment = Alignment.Center,
    ) {
        ProgramCoverIcon(glyph = glyphFor(program))
    }
}

@Composable
private fun ProgramCoverIcon(glyph: ProgramCoverGlyph) {
    Canvas(modifier = Modifier.size(40.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.09f, cap = StrokeCap.Round)
        when (glyph) {
            ProgramCoverGlyph.BARBELL -> {
                val barY = size.height / 2f
                drawLine(Accent, Offset(size.width * 0.22f, barY), Offset(size.width * 0.78f, barY), strokeWidth = stroke.width, cap = stroke.cap)
                val plateHeight = size.height * 0.7f
                val plateWidth = size.width * 0.12f
                for (x in listOf(size.width * 0.14f, size.width * 0.86f)) {
                    drawLine(
                        Accent,
                        Offset(x, barY - plateHeight / 2f),
                        Offset(x, barY + plateHeight / 2f),
                        strokeWidth = plateWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }
            ProgramCoverGlyph.FLAME -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.05f)
                    cubicTo(
                        size.width * 0.85f, size.height * 0.4f,
                        size.width * 0.65f, size.height * 0.55f,
                        size.width * 0.5f, size.height * 0.95f,
                    )
                    cubicTo(
                        size.width * 0.35f, size.height * 0.55f,
                        size.width * 0.15f, size.height * 0.4f,
                        size.width * 0.5f, size.height * 0.05f,
                    )
                    close()
                }
                drawPath(path, Accent)
            }
            ProgramCoverGlyph.CHART -> {
                val barWidth = size.width * 0.16f
                val gap = size.width * 0.1f
                val heights = listOf(0.4f, 0.7f, 1.0f)
                heights.forEachIndexed { index, fraction ->
                    val barHeight = size.height * fraction
                    val x = size.width * 0.12f + index * (barWidth + gap)
                    drawLine(
                        Accent,
                        Offset(x, size.height),
                        Offset(x, size.height - barHeight),
                        strokeWidth = barWidth,
                        cap = StrokeCap.Square,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgramCard(program: ProgramEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Gate D1 rollout — premiumShadow, not entranceFade: this card lives inside a
            // LazyColumn's items(...), so it gets recycled/re-entered on scroll; entranceFade's
            // one-shot LaunchedEffect(Unit) would replay every time a scrolled-off card scrolls
            // back into view, which reads as a glitch, not polish. premiumShadow is a static draw
            // effect with no such replay risk. accentBloom = false — this isn't a donate/PR-style
            // hero surface, just an ambient lift to match Dashboard's hero card now having one.
            .premiumShadow(radius = 18.dp, accentBloom = false)
            .pressScale(onClick = onClick)
            .tiltOnDrag(maxDegrees = 6f)
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large),
    ) {
        ProgramCoverArt(program = program, modifier = Modifier.height(84.dp))
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ProgramTitleRow(program = program)
            DifficultyBadge(level = program.level)
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

/** Feature #8 (Gate 42) — 3 uniform 14×4dp flat bars, not stars and not an ascending-height
 * "signal strength" shape, filled up to [ProgramDifficulty.levelSteps]'s count (mockup's own exact
 * dimensions — a Gate 42 review pass drew ascending-height columns instead, fixed in this audit
 * pass). Unrated ("Mọi trình độ" or an unrecognized/imported level string, `levelSteps` returns
 * `null`) reads as visibly distinct from a *rated* card's empty bars: all-[TextMuted] for unrated,
 * vs. [ChartBarIdle] for the unfilled remainder of a rated card — collapsing both to the same
 * color would make "this program has no difficulty rating" indistinguishable from "this program is
 * rated Beginner." */
@Composable
private fun DifficultyBadge(level: String) {
    val steps = ProgramDifficulty.levelSteps(level)
    val emptyColor = if (steps == null) TextMuted else ChartBarIdle
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) { index ->
            val filled = steps != null && index < steps
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(4.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(if (filled) Accent else emptyColor),
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
