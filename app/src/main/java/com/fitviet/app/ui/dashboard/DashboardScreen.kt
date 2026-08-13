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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.MonthlyPlanDayEntity
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.domain.DayVolume
import com.fitviet.app.domain.MuscleGroupWorkload
import com.fitviet.app.domain.NextTraining
import com.fitviet.app.domain.ProgramProgress
import com.fitviet.app.domain.Recommendation
import com.fitviet.app.domain.StatsRange
import com.fitviet.app.domain.TodayMonthlyPlanCard
import com.fitviet.app.ui.common.RangePills
import com.fitviet.app.ui.common.entranceFade
import com.fitviet.app.ui.common.pressScale
import com.fitviet.app.ui.common.tiltOnDrag
import com.fitviet.app.ui.profile.MonogramAvatar
import com.fitviet.app.ui.profile.avatarInitial
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
import com.fitviet.app.ui.theme.premiumShadow
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextBody
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.formatCompactKg
import com.fitviet.app.util.formatVi
import com.fitviet.app.util.isoWeekNumber
import com.fitviet.app.util.labelRes
import com.fitviet.app.util.longLabelRes
import com.fitviet.app.util.shortLabelRes
import java.time.LocalDate

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onStartWorkout: (programId: Long) -> Unit,
    onViewSchedule: (programId: Long) -> Unit,
    onBrowsePrograms: () -> Unit,
    onOpenDiary: () -> Unit,
    onOpenProfile: () -> Unit,
    // "Hit & Run" (Gate 63+) — starts today's monthly-plan-day session directly, bypassing
    // WorkoutPreview (unlike the hand-authored-program path above). A deliberate Phase 5 scope
    // decision, not an oversight: retrofitting the preview screen for monthly-plan days is
    // deferred to a later gate, and going straight to the live session is arguably closer to the
    // feature's own "1-2 taps, no re-choosing" goal anyway. See the "Hit & Run" plan's Phase 9 note
    // for where a skip-preview toggle would apply once a monthly-plan preview path exists.
    onStartMonthlyPlanDay: (dayId: Long) -> Unit,
    // "Hit & Run" (Gate 63+) — the empty-state CTA shown when there's no active monthly plan yet
    // (see the call site below, gated on the same `monthlyPlanCard == null` check as the hero-card
    // branch). Deferred out of Phase 5 into this phase since Quick Generate's own destination
    // didn't exist yet — see that gate's plan note.
    onGenerateMonthlyPlan: () -> Unit,
    // "Hit & Run" (Gate 63+) Regenerate UI — the Today card's link into the plan's simple day
    // list, only rendered once a plan is active (there's nothing to view otherwise).
    onViewMonthlyPlan: () -> Unit,
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
        GreetingHeader(today = today, displayName = uiState.displayName, avatarId = uiState.avatarId, onAvatarClick = onOpenProfile)
        // "Hit & Run" (Gate 63+) — an active monthly plan's Today card takes over the hero slot
        // entirely; the hand-authored-program hero card below is unchanged for everyone else.
        // "Hit & Run" redesign (Gate 1c) — TodayMonthlyPlanCard is a total 5-case type now
        // (Training/RestDay/Unavailable/NoPlan/PlanFinished); only NoPlan falls back to the old
        // hand-authored-program hero card below — every other case, including a plan that just
        // finished, still shows the monthly-plan hero (with its own no-action copy).
        val monthlyPlanCard = uiState.todayMonthlyPlanCard
        if (monthlyPlanCard != TodayMonthlyPlanCard.NoPlan) {
            MonthlyPlanHeroCard(
                card = monthlyPlanCard,
                onStart = { (monthlyPlanCard as? TodayMonthlyPlanCard.Training)?.let { onStartMonthlyPlanDay(it.dayId) } },
                onViewPlan = onViewMonthlyPlan,
            )
        } else {
            HeroCard(
                program = uiState.featuredProgram,
                nextTraining = uiState.nextTraining,
                programProgress = uiState.programProgress,
                // WorkoutPreview only ever resolves *today*'s schedule (see its own doc comment), but
                // this card's "Tiếp theo: <day>" label can name a later day when today is a rest day —
                // routing that case into WorkoutPreview would land on an empty "no exercises today"
                // screen despite the button naming a specific upcoming day. Route those to the weekly
                // schedule instead, which already highlights the correct upcoming day.
                onStart = {
                    val program = uiState.featuredProgram
                    when {
                        program == null -> onBrowsePrograms()
                        uiState.nextTraining?.isToday == false -> onViewSchedule(program.id)
                        else -> onStartWorkout(program.id)
                    }
                },
            )
            // "Hit & Run" (Gate 63+) empty state — only shown once, right below the existing hero
            // card, when there's no active monthly plan yet; disappears for good once one exists
            // (the branch above then takes over the hero slot instead).
            GenerateMonthlyPlanCard(onClick = onGenerateMonthlyPlan)
        }
        if (uiState.showRecommendationCard) {
            uiState.recommendation?.let { RecommendationCard(recommendation = it) }
        }
        StatTilesRow(
            streakDays = uiState.stats.streakDays,
            sessionsThisWeek = uiState.stats.sessionsThisWeek,
            volumeThisWeekKg = uiState.stats.volumeThisWeekKg,
        )
        // Feature #7 (Gate 43) — sits above both cards per the plan; only WeeklyVolumeCard's
        // series actually changes with the range. MuscleBalanceCard stays a "this week" snapshot
        // (it has no series concept, and the plan's range-bucketed-series wording is specific to
        // the volume chart) — see PROGRESS.md's Gate 43 scope-decision note for the full reasoning.
        // The "THỐNG KÊ" section-header label (mockup's `StatsRangeRow`) was missing entirely until
        // this audit pass — the pills themselves were correct, but had nothing labeling the section.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.dashboard_stats_section_label),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.08.em),
                color = TextFaint,
            )
            RangePills(options = DASHBOARD_RANGES, selected = uiState.selectedRange, onSelect = viewModel::selectRange)
        }
        if (uiState.showMuscleBalanceCard) {
            MuscleBalanceCard(workload = uiState.muscleGroupWorkloadThisWeek)
        }
        WeeklyVolumeCard(
            range = uiState.selectedRange,
            series = uiState.rangeSeries,
            selectedIndex = uiState.selectedDayIndex,
            onSelectDay = viewModel::selectDay,
            onOpenDiary = onOpenDiary,
        )
        if (uiState.showNutritionCard) {
            NutritionCard(kcalToday = uiState.kcalToday, kcalGoal = uiState.kcalGoal)
        }
    }

    // "Hit & Run" (Gate 63+) adaptive scheduling — see DashboardViewModel's `missedDay` doc for
    // why this is a one-shot-per-load check, not a continuously re-triggering prompt.
    // Gate D4 — a fresh streak milestone takes priority over the missed-day prompt when both are
    // non-null at once (rare, but both are independently derived so nothing rules it out): showing
    // both at the same time would stack two modal windows with competing back behavior. The missed
    // day isn't lost, just deferred — it's untouched here, so it reappears the moment the milestone
    // overlay above it is dismissed and this recomposes.
    val streakMilestone = uiState.streakMilestone
    if (streakMilestone != null) {
        StreakMilestoneOverlay(
            streakDays = uiState.stats.streakDays,
            milestone = streakMilestone,
            onDismiss = viewModel::dismissStreakMilestone,
        )
    } else {
        uiState.missedDay?.let { missed ->
            MissedDayDialog(
                missedDay = missed,
                onPushToday = viewModel::pushMissedDayToToday,
                onSkip = viewModel::skipMissedDay,
                onViewPlan = {
                    viewModel.dismissMissedDayToViewPlan()
                    onViewMonthlyPlan()
                },
            )
        }
    }
}

@Composable
private fun GreetingHeader(today: LocalDate, displayName: String, avatarId: Int, onAvatarClick: () -> Unit) {
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
                text = stringResource(R.string.dashboard_greeting, displayName),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        MonogramAvatar(
            initial = avatarInitial(displayName),
            avatarId = avatarId,
            size = 42.dp,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.clickable(onClick = onAvatarClick),
        )
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
            .entranceFade()
            // Gate D1 rollout — this is Dashboard's own hero card, the exact surface
            // premiumShadow's Gate A1 doc comment names first, yet it never actually got the
            // shadow applied until now.
            .premiumShadow(radius = 18.dp, accentBloom = true)
            .tiltOnDrag()
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
                .pressScale(onClick = onStart)
                .clip(MaterialTheme.shapes.small)
                .background(Accent)
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

/** "Hit & Run" (Gate 63+) counterpart to [HeroCard] — same gradient/layout shell, sourced from the
 * active monthly plan's Today row instead of a hand-authored [ProgramEntity]. Shown instead of
 * [HeroCard] (not alongside it) whenever a monthly plan is active; see [DashboardScreen]'s call
 * site for the precedence rule. */
@Composable
private fun MonthlyPlanHeroCard(card: TodayMonthlyPlanCard, onStart: () -> Unit, onViewPlan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tiltOnDrag()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd), start = Offset(0f, 0f)))
            .padding(Dimens.CardPaddingLarge),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGapSmall),
    ) {
        Column {
            Text(
                text = stringResource(R.string.dashboard_hero_label),
                style = MaterialTheme.typography.labelLarge,
                color = Accent,
            )
            when (card) {
                is TodayMonthlyPlanCard.Training -> {
                    Text(
                        text = card.sessionType,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        text = stringResource(R.string.dashboard_hero_meta_monthly_plan, card.exerciseCount, card.estimatedDurationMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                TodayMonthlyPlanCard.RestDay -> {
                    Text(
                        text = stringResource(R.string.dashboard_rest_day_title),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        text = stringResource(R.string.dashboard_rest_day_meta),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                // "Hit & Run" redesign (Gate 1c) — Unavailable/PlanFinished are new cases; NoPlan
                // is handled here only defensively (the caller routes NoPlan to the old HeroCard
                // branch instead, this composable should never actually receive it).
                is TodayMonthlyPlanCard.Unavailable -> {
                    Text(
                        text = stringResource(R.string.dashboard_unavailable_title),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        text = stringResource(R.string.dashboard_unavailable_meta),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                TodayMonthlyPlanCard.PlanFinished, TodayMonthlyPlanCard.NoPlan -> {
                    Text(
                        text = stringResource(R.string.dashboard_plan_finished_title),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        text = stringResource(R.string.dashboard_plan_finished_meta),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        if (card is TodayMonthlyPlanCard.Training) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale(onClick = onStart)
                    .clip(MaterialTheme.shapes.small)
                    .background(Accent)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.dashboard_start_workout_today),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnAccent,
                )
            }
        }
        Text(
            text = stringResource(R.string.dashboard_view_monthly_plan),
            style = MaterialTheme.typography.labelMedium,
            color = Accent,
            modifier = Modifier.clickable(onClick = onViewPlan),
        )
    }
}

/** "Hit & Run" (Gate 63+) empty-state CTA — a compact, distinct card (not another gradient hero)
 * since it sits directly below [HeroCard], which already owns the "big gradient block" visual
 * weight on this screen. */
@Composable
private fun GenerateMonthlyPlanCard(onClick: () -> Unit) {
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
            Text(text = stringResource(R.string.dashboard_generate_cta_title), style = MaterialTheme.typography.labelLarge, color = Accent)
            Text(
                text = stringResource(R.string.dashboard_generate_cta_body),
                style = MaterialTheme.typography.bodySmall,
                color = TextBody,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(text = stringResource(R.string.dashboard_generate_cta_button), style = MaterialTheme.typography.labelLarge, color = Accent)
    }
}

/** "Hit & Run" (Gate 63+) adaptive scheduling — the 3-way missed-day prompt (see the "Hit & Run"
 * plan's adaptive-scheduling section: push-to-today / skip-and-continue / view monthly plan).
 * Implicit dismissal (back gesture / tap outside) is deliberately disabled because each explicit
 * option has a meaningfully different outcome; all three options dismiss the prompt themselves. */
@Composable
private fun MissedDayDialog(missedDay: MonthlyPlanDayEntity, onPushToday: () -> Unit, onSkip: () -> Unit, onViewPlan: () -> Unit) {
    val date = LocalDate.ofEpochDay(missedDay.effectiveEpochDay)
    AlertDialog(
        // A no-op, not onSkip: skipping is a real, persistent mutation (status -> SKIPPED) and
        // shouldn't fire from an implicit back-press/outside-tap — the user must pick one of the
        // 3 explicit options below. See Gate 8 review: an earlier version wired this to onSkip.
        onDismissRequest = {},
        title = { Text(text = stringResource(R.string.dashboard_missed_day_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(
                        R.string.dashboard_missed_day_body,
                        stringResource(date.dayOfWeek.longLabelRes()),
                        missedDay.sessionType.orEmpty(),
                    ),
                )
                Text(
                    text = stringResource(R.string.dashboard_missed_day_view_plan),
                    style = MaterialTheme.typography.labelLarge,
                    color = Accent,
                    modifier = Modifier.clickable(onClick = onViewPlan),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onPushToday) {
                Text(text = stringResource(R.string.dashboard_missed_day_push_today))
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text(text = stringResource(R.string.dashboard_missed_day_skip))
            }
        },
    )
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

private val DASHBOARD_RANGES = listOf(
    StatsRange.WEEK to R.string.dashboard_range_week,
    StatsRange.MONTH to R.string.dashboard_range_month,
    StatsRange.ALL to R.string.dashboard_range_all,
)

/** Feature #7 (Gate 43) — [range]'s bucket label: per-day for WEEK (unchanged from before this
 * gate), per-week-number for MONTH/ALL, matching Diary's own week-bucket chart's convention
 * (`R.string.diary_week_label` + [isoWeekNumber]) rather than inventing a new label style. */
@Composable
private fun barLabel(range: StatsRange, entry: DayVolume): String = if (range == StatsRange.WEEK) {
    stringResource(entry.date.dayOfWeek.shortLabelRes())
} else {
    stringResource(R.string.diary_week_label, entry.date.isoWeekNumber())
}

@Composable
private fun WeeklyVolumeCard(
    range: StatsRange,
    series: List<DayVolume>,
    selectedIndex: Int,
    onSelectDay: (Int) -> Unit,
    onOpenDiary: () -> Unit,
) {
    val selected = series.getOrNull(selectedIndex)
    val maxVolume = series.maxOfOrNull { it.volumeKg }?.takeIf { it > 0 } ?: 1.0
    val titleRes = when (range) {
        StatsRange.WEEK -> R.string.dashboard_weekly_volume_title
        StatsRange.MONTH -> R.string.dashboard_volume_title_month
        StatsRange.ALL -> R.string.dashboard_volume_title_all
    }

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
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
            )
            if (selected != null) {
                val restLabel = stringResource(R.string.dashboard_bar_rest)
                val valueLabel = if (selected.volumeKg <= 0.0) restLabel else "${formatVi(selected.volumeKg)} kg"
                Text(
                    text = "${barLabel(range, selected)} · $valueLabel",
                    style = MaterialTheme.typography.titleSmall,
                    color = Accent,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            series.forEachIndexed { index, entry ->
                val fraction = if (entry.volumeKg <= 0.0) 0.08f else (entry.volumeKg / maxVolume).toFloat().coerceIn(0.15f, 1f)
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
            series.forEach { entry ->
                Text(
                    text = barLabel(range, entry),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
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
