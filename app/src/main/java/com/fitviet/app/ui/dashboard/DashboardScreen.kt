package com.fitviet.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.domain.DayVolume
import com.fitviet.app.domain.MuscleGroupWorkload
import com.fitviet.app.domain.NextTraining
import com.fitviet.app.domain.ProgramProgress
import com.fitviet.app.domain.Recommendation
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentBorder
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.ChartBarIdle
import com.fitviet.app.ui.theme.DeepSurface2
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HeroGradientEnd
import com.fitviet.app.ui.theme.HeroGradientStart
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextBody
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.formatCompactKg
import com.fitviet.app.util.formatVi
import com.fitviet.app.util.labelRes
import com.fitviet.app.util.longLabelRes
import com.fitviet.app.util.shortLabelRes
import java.time.LocalDate

// Placeholder until profile/settings (Gate 6) has a real, editable display name.
private const val PLACEHOLDER_USER_NAME = "Minh"

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onStartWorkout: () -> Unit,
    onBrowsePrograms: () -> Unit,
    onOpenDiary: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Not `remember`ed: recomputed each recomposition (including the ones the repository's
    // midnight tick triggers) so the greeting date doesn't freeze at whatever day this opened on.
    val today = LocalDate.now()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPage)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGapLarge),
    ) {
        GreetingHeader(today = today, onAvatarClick = onOpenProfile)
        HeroCard(
            program = uiState.featuredProgram,
            nextTraining = uiState.nextTraining,
            programProgress = uiState.programProgress,
            onStart = if (uiState.featuredProgram != null) onStartWorkout else onBrowsePrograms,
        )
        if (uiState.showRecommendationCard) {
            uiState.recommendation?.let { RecommendationCard(recommendation = it) }
        }
        StatTilesRow(
            streakDays = uiState.stats.streakDays,
            sessionsThisWeek = uiState.stats.sessionsThisWeek,
            volumeThisWeekKg = uiState.stats.volumeThisWeekKg,
        )
        if (uiState.showMuscleBalanceCard) {
            MuscleBalanceCard(workload = uiState.muscleGroupWorkloadThisWeek)
        }
        WeeklyVolumeCard(
            last7Days = uiState.stats.last7Days,
            selectedIndex = uiState.selectedDayIndex,
            onSelectDay = viewModel::selectDay,
            onOpenDiary = onOpenDiary,
        )
        if (uiState.showNutritionCard) {
            NutritionCard(kcalToday = uiState.kcalToday, kcalGoal = uiState.kcalGoal)
        }
    }
}

@Composable
private fun GreetingHeader(today: LocalDate, onAvatarClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = stringResource(
                    R.string.dashboard_date_line,
                    stringResource(today.dayOfWeek.longLabelRes()),
                    today.dayOfMonth,
                    today.monthValue,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            Text(
                text = stringResource(R.string.dashboard_greeting, PLACEHOLDER_USER_NAME),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color = DeepSurface2, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = PLACEHOLDER_USER_NAME.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    color = Accent,
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    program: ProgramEntity?,
    nextTraining: NextTraining?,
    programProgress: ProgramProgress?,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd), start = Offset(0f, 0f)))
            .padding(Dimens.CardPaddingLarge),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGapSmall),
    ) {
        Column {
            Text(
                text = if (nextTraining != null && !nextTraining.isToday) {
                    stringResource(R.string.dashboard_hero_label_next, stringResource(nextTraining.day.dayOfWeek.shortLabelRes()))
                } else {
                    stringResource(R.string.dashboard_hero_label)
                },
                style = MaterialTheme.typography.labelLarge,
                color = Accent,
            )
            Text(
                text = program?.titleVi ?: stringResource(R.string.dashboard_no_program_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                // Feature #3: the real next scheduled day + exercise count (Gate 15's per-day
                // assignment) when the active program's schedule resolves one; falls back to the
                // program's general weekly cadence/level/equipment otherwise (schedule not yet
                // seeded, or genuinely no active program).
                text = when {
                    nextTraining != null -> stringResource(
                        R.string.dashboard_hero_meta_schedule,
                        nextTraining.day.titleVi,
                        nextTraining.day.exercises.size,
                    )
                    program != null -> stringResource(R.string.dashboard_hero_meta, program.sessionsPerWeek, program.level, program.equipment)
                    else -> stringResource(R.string.dashboard_no_program_meta)
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (programProgress != null) {
            ProgramProgressBar(progress = programProgress)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(Accent)
                .clickable(onClick = onStart)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(if (program != null) R.string.dashboard_start_workout else R.string.dashboard_browse_programs),
                style = MaterialTheme.typography.titleMedium,
                color = OnAccent,
            )
        }
    }
}

@Composable
private fun ProgramProgressBar(progress: ProgramProgress) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.dashboard_progress_label, progress.completedThisWeek, progress.targetPerWeek),
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(AccentBorder),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.fraction)
                    .background(Accent),
            )
        }
    }
}

// Index-matched to the recommendation_tip_1..8 string resources — must stay in sync with
// RecommendationCalculator.GENERIC_TIP_COUNT.
private val GENERIC_TIP_RES_IDS = listOf(
    R.string.recommendation_tip_1,
    R.string.recommendation_tip_2,
    R.string.recommendation_tip_3,
    R.string.recommendation_tip_4,
    R.string.recommendation_tip_5,
    R.string.recommendation_tip_6,
    R.string.recommendation_tip_7,
    R.string.recommendation_tip_8,
)

@Composable
private fun RecommendationCard(recommendation: Recommendation) {
    val text = when (recommendation) {
        is Recommendation.ComeBackReminder -> stringResource(R.string.recommendation_come_back)
        is Recommendation.StreakPraise -> stringResource(R.string.recommendation_streak_praise, recommendation.streakDays)
        is Recommendation.MeasurementReminder -> stringResource(R.string.recommendation_measurement_reminder)
        is Recommendation.GenericTip -> stringResource(GENERIC_TIP_RES_IDS[recommendation.tipIndex])
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(AccentSurfaceSelected)
            .border(1.dp, AccentBorder, MaterialTheme.shapes.medium)
            .padding(Dimens.CardPaddingSmall),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = stringResource(R.string.recommendation_title), style = MaterialTheme.typography.labelLarge, color = Accent)
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextBody)
    }
}

@Composable
private fun MuscleBalanceCard(workload: List<MuscleGroupWorkload>) {
    val maxSets = workload.maxOfOrNull { it.setCount } ?: 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .padding(Dimens.CardPaddingSmall),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = stringResource(R.string.dashboard_muscle_balance_title), style = MaterialTheme.typography.titleSmall)
        if (maxSets <= 0) {
            Text(text = stringResource(R.string.dashboard_muscle_balance_empty), style = MaterialTheme.typography.bodySmall, color = TextMuted)
        } else {
            workload.forEach { entry ->
                val fraction = if (maxSets <= 0) 0f else entry.setCount.toFloat() / maxSets
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(entry.muscleGroup.labelRes()),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextBody,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(80.dp),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(AccentBorder),
                    ) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(Accent))
                    }
                    Text(
                        text = stringResource(R.string.dashboard_muscle_balance_sets, entry.setCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTilesRow(streakDays: Int, sessionsThisWeek: Int, volumeThisWeekKg: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(value = streakDays.toString(), label = stringResource(R.string.dashboard_stat_streak), modifier = Modifier.weight(1f), accentValue = true)
        StatTile(value = sessionsThisWeek.toString(), label = stringResource(R.string.dashboard_stat_sessions_this_week), modifier = Modifier.weight(1f))
        StatTile(value = formatCompactKg(volumeThisWeekKg), label = stringResource(R.string.dashboard_stat_volume_this_week), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier, accentValue: Boolean = false) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = Anton),
            color = if (accentValue) Accent else TextPrimary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun WeeklyVolumeCard(
    last7Days: List<DayVolume>,
    selectedIndex: Int,
    onSelectDay: (Int) -> Unit,
    onOpenDiary: () -> Unit,
) {
    val selected = last7Days.getOrNull(selectedIndex)
    val maxVolume = last7Days.maxOfOrNull { it.volumeKg }?.takeIf { it > 0 } ?: 1.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .padding(Dimens.CardPaddingSmall),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDiary),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(R.string.dashboard_weekly_volume_title), style = MaterialTheme.typography.titleSmall)
            if (selected != null) {
                val dayLabel = stringResource(selected.date.dayOfWeek.shortLabelRes())
                val restLabel = stringResource(R.string.dashboard_bar_rest)
                val valueLabel = if (selected.volumeKg <= 0.0) restLabel else "${formatVi(selected.volumeKg)} kg"
                Text(text = "$dayLabel · $valueLabel", style = MaterialTheme.typography.titleSmall, color = Accent)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            last7Days.forEachIndexed { index, day ->
                val fraction = if (day.volumeKg <= 0.0) 0.08f else (day.volumeKg / maxVolume).toFloat().coerceIn(0.15f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .fillMaxHeight(fraction)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(if (index == selectedIndex) Accent else ChartBarIdle)
                        .clickable { onSelectDay(index) },
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            last7Days.forEach { day ->
                Text(
                    text = stringResource(day.date.dayOfWeek.shortLabelRes()),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NutritionCard(kcalToday: Int, kcalGoal: Int) {
    val progress = (kcalToday.toFloat() / kcalGoal.toFloat()).coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .padding(Dimens.CardPaddingSmall),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(R.string.dashboard_nutrition_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.dashboard_nutrition_progress, formatVi(kcalToday), formatVi(kcalGoal)),
                style = MaterialTheme.typography.labelLarge,
                color = Accent,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(DeepSurface2),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(Accent),
            )
        }
    }
}
