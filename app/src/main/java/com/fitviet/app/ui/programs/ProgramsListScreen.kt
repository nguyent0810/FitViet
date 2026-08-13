package com.fitviet.app.ui.programs

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.data.repository.ImportProgramResult
import com.fitviet.app.domain.ProgramDifficulty
import com.fitviet.app.domain.SplitTemplate
import com.fitviet.app.domain.TodayMonthlyPlanCard
import com.fitviet.app.ui.common.pressScale
import com.fitviet.app.ui.common.tiltOnDrag
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.util.shortLabelRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Redesign Phase 3a — the "Kế hoạch" tab, rebuilt against the mock's PLAN TAB block (design HTML
 * lines 114-145) on top of Gate 2b's read-only generation-input browser. Migrated to
 * [HrColors]/[HrDisplay]/[HrBody]/[HrShapes]/[HrDimens], same per-screen pattern Gate 2a/2c started.
 *
 * Per the Phase 3 plan-check with the reviewer: [com.fitviet.app.ui.monthlyplan.MonthlyPlanDetailScreen]
 * is kept (not deleted) as the full-month drill-in reached via [onViewFullMonth] — the mock's own
 * Plan tab only shows today + 2 upcoming days, and that screen is still the only surface for
 * per-week regenerate. The mock's literal "chạm giữ" (long-press) hint is deliberately not built —
 * the actions it implies already live behind a plain tap on `MonthlyPlanDayDetailScreen` (an
 * existing convention from an earlier gate), so the hint copy says "chạm" instead.
 */
@Composable
fun ProgramsListScreen(
    viewModel: ProgramsViewModel,
    // Redesign Gate 2b — tapping a program card's own doc (see [ProgramsViewModel
    // .generateFromProgram]): a confirm dialog gates the call when a plan is already active, but
    // once confirmed (or when there's nothing to lose), this is the trigger — the caller does the
    // actual generate()+navigate()+error-Toast, same "screen signals, nav host performs" split as
    // onboarding's `onSubmit`/Quick Generate's `onGenerateClick`.
    onGenerateFromProgram: (ProgramEntity) -> Unit,
    // The optional, demoted "Xem trước" link (see `WorkoutPreviewViewModel`'s own doc) — a
    // read-only look at the program, never a "start" action.
    onPreviewProgram: (ProgramEntity) -> Unit,
    onExerciseClick: (ExerciseEntity) -> Unit,
    // "Hit & Run" (Gate 63+) — the header card below the title row, per the plan's Quick Generate
    // entry-point list (Dashboard's empty state + this screen's own header, both reachable
    // independent of whether the user has ever generated a plan before). Redesign Phase 3a also
    // reuses this for the plan progress card's "Tạo lại ⟳" link (see the mock's own `openGenerate`
    // binding on that link — it reopens generation, not a bare regenerate-in-place).
    onGenerateMonthlyPlan: () -> Unit,
    // Redesign Phase 3a — the plan card's "TẬP ›" CTA, same action Dashboard's Today card uses.
    onStartMonthlyPlanDay: (dayId: Long) -> Unit,
    // Redesign Phase 3a — tapping the today card (when not startable) or an upcoming-day row opens
    // the existing Regenerate-UI day-detail screen.
    onOpenMonthlyPlanDay: (dayId: Long) -> Unit,
    // Redesign Phase 3a — "Xem cả tháng" drill-in to the full week/day list.
    onViewFullMonth: () -> Unit,
    // Redesign Phase 3a — "Thư viện bài tập" entry point, replacing Handbook's own bottom-nav tab.
    onOpenExerciseLibrary: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importMessage by remember { mutableStateOf<String?>(null) }
    // Redesign Gate 2b — non-null while the "replace your active plan?" confirm dialog for this
    // program is showing (see [ProgramCard]'s own tap handling below).
    var pendingReplaceConfirm by remember { mutableStateOf<ProgramEntity?>(null) }
    // Redesign Phase 3a — same confirm gate as [pendingReplaceConfirm], for the plan progress
    // card's "Tạo lại ⟳" link (regenerating also replaces the active plan, via [onGenerateMonthlyPlan]
    // rather than a specific program).
    var pendingRegenerateConfirm by remember { mutableStateOf(false) }
    val readErrorText = stringResource(R.string.programs_import_read_error)
    val invalidFormatText = stringResource(R.string.programs_import_invalid)
    val failedText = stringResource(R.string.programs_import_failed)
    val exportChooserTitle = stringResource(R.string.programs_export_chooser_title)
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
            .background(HrColors.Bg)
            .padding(horizontal = HrDimens.ScreenPaddingHorizontal),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(HrDimens.CardGapVertical),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.programs_title), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, color = HrColors.TextHi)
                ImportButton(onClick = { importLauncher.launch("*/*") })
            }
        }

        val activePlan = uiState.activePlan
        if (activePlan != null) {
            item {
                PlanProgressCard(plan = activePlan, onRegenerateClick = { pendingRegenerateConfirm = true })
            }
            item {
                PlanTodayCard(
                    card = activePlan.todayCard,
                    dayOfPlan = activePlan.dayOfPlan,
                    onStart = onStartMonthlyPlanDay,
                    onOpenDay = onOpenMonthlyPlanDay,
                )
            }
            items(activePlan.upcomingDays, key = { "upcoming-${it.dayId}" }) { day ->
                UpcomingDayRow(day = day, onClick = { onOpenMonthlyPlanDay(day.dayId) })
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Tied to upcomingDays specifically (not activePlan) — the hint only makes
                    // sense when there are rows below it to act on.
                    if (activePlan.upcomingDays.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.programs_upcoming_hint),
                            fontFamily = HrBody,
                            fontSize = 12.sp,
                            color = HrColors.TextFaint,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    // Tied to activePlan alone, not upcomingDays — on the plan's last day
                    // upcomingDays is empty, but the full month (and its per-week regenerate,
                    // which nothing else on this tab offers) must stay reachable.
                    Text(
                        text = stringResource(R.string.programs_view_full_month),
                        fontFamily = HrBody,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = HrColors.Accent,
                        modifier = Modifier
                            .heightIn(min = Dimens.MinTouchTarget)
                            .clickable(onClick = onViewFullMonth)
                            .padding(vertical = 12.dp),
                    )
                }
            }
        }

        item {
            // Same replace-confirm guard as PlanProgressCard's "Tạo lại ⟳" — both land on the same
            // QuickGenerate wizard, whose own final CTA commits immediately with no guard of its
            // own, so guarding only one of the two entry points into it would leave the other a
            // silent, unguarded way to replace the active plan.
            GenerateMonthlyPlanHeaderCard(
                onClick = { if (uiState.hasActivePlan) pendingRegenerateConfirm = true else onGenerateMonthlyPlan() },
            )
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
                    fontFamily = HrBody,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = HrColors.TextLow,
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
                    fontFamily = HrBody,
                    fontSize = 14.sp,
                    color = HrColors.TextLow,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        } else {
            items(uiState.visiblePrograms, key = { it.id }) { program ->
                ProgramCard(
                    program = program,
                    isGenerating = uiState.isGenerating,
                    onClick = {
                        if (uiState.hasActivePlan) {
                            pendingReplaceConfirm = program
                        } else {
                            onGenerateFromProgram(program)
                        }
                    },
                    onPreviewClick = { onPreviewProgram(program) },
                    onExportClick = {
                        scope.launch {
                            val json = viewModel.exportProgram(program.id) ?: return@launch
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, json)
                                putExtra(Intent.EXTRA_SUBJECT, program.titleVi)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, exportChooserTitle))
                        }
                    },
                )
            }
        }
        item {
            LibraryEntryRow(onClick = onOpenExerciseLibrary)
        }
    }

    // Redesign Gate 2b — generating from a program card replaces the currently-active plan
    // ([MonthlyPlanRepository.generate] marks it SUPERSEDED), so a tap in this browse list needs
    // an explicit confirm the same way a QuickGenerate-style dedicated screen doesn't (that
    // screen's whole purpose already signals "generate," this list's doesn't).
    pendingReplaceConfirm?.let { program ->
        AlertDialog(
            onDismissRequest = { pendingReplaceConfirm = null },
            title = { Text(text = stringResource(R.string.programs_replace_plan_title)) },
            text = { Text(text = stringResource(R.string.programs_replace_plan_body, program.titleVi)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingReplaceConfirm = null
                    onGenerateFromProgram(program)
                }) {
                    Text(text = stringResource(R.string.programs_replace_plan_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingReplaceConfirm = null }) {
                    Text(text = stringResource(R.string.profile_history_delete_confirm_cancel))
                }
            },
        )
    }

    if (pendingRegenerateConfirm) {
        AlertDialog(
            onDismissRequest = { pendingRegenerateConfirm = false },
            title = { Text(text = stringResource(R.string.programs_replace_plan_title)) },
            text = { Text(text = stringResource(R.string.programs_regenerate_body)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingRegenerateConfirm = false
                    onGenerateMonthlyPlan()
                }) {
                    Text(text = stringResource(R.string.programs_replace_plan_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRegenerateConfirm = false }) {
                    Text(text = stringResource(R.string.profile_history_delete_confirm_cancel))
                }
            },
        )
    }
}

@Composable
private fun ImportButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = Dimens.MinTouchTarget)
            .clip(HrShapes.CardSmall)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardSmall)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(R.string.programs_import_button), fontFamily = HrBody, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = HrColors.TextLow)
    }
}

@Composable
private fun ImportMessageCard(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardSmall)
            .background(HrColors.SurfaceAccent)
            .border(1.dp, HrColors.BorderAccentDim, HrShapes.CardSmall)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = message, fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextMid, modifier = Modifier.weight(1f))
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
            .clip(HrShapes.CardRegular)
            .background(HrColors.SurfaceAccent)
            .border(1.dp, HrColors.BorderAccentDim, HrShapes.CardRegular)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(text = stringResource(R.string.programs_generate_cta_title), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HrColors.Accent)
            Text(
                text = stringResource(R.string.programs_generate_cta_body),
                fontFamily = HrBody,
                fontSize = 12.sp,
                color = HrColors.TextMid,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(text = stringResource(R.string.programs_generate_cta_button), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HrColors.Accent)
    }
}

/** Redesign Phase 3a — mock lines 119-126: the active plan's month/split/week label, progress bar,
 * and "X/Y buổi hoàn thành" line. The month number is the plan's own creation month (real data,
 * not the mock's literal "8") — this app has no separate "plan name," so "created in month N"
 * is the only real value available for the mock's "PLAN THÁNG N" label. */
@Composable
private fun PlanProgressCard(plan: ActivePlanUi, onRegenerateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardHero)
            .background(Brush.linearGradient(listOf(HrColors.GradientCardStart, HrColors.GradientCardEnd), start = Offset(0f, 0f)))
            .border(1.dp, HrColors.BorderGradient, HrShapes.CardHero)
            .padding(18.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(
                    R.string.programs_plan_label,
                    plan.creationMonth,
                    stringResource(splitTemplateLabelRes(plan.splitTemplate)),
                    plan.progress.currentWeekNumber ?: 1,
                    plan.totalWeeks,
                ),
                fontFamily = HrBody,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = HrColors.Accent,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            Text(
                text = stringResource(R.string.programs_plan_regenerate_link),
                fontFamily = HrBody,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = HrColors.TextLow,
                modifier = Modifier
                    .heightIn(min = Dimens.MinTouchTarget)
                    .clickable(onClick = onRegenerateClick)
                    .padding(vertical = 12.dp),
            )
        }
        val fraction = if (plan.progress.totalSessions > 0) plan.progress.completedSessions.toFloat() / plan.progress.totalSessions else 0f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp)
                .height(6.dp)
                .clip(PillShape)
                .background(HrColors.Border),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(PillShape)
                    .background(HrColors.Accent),
            )
        }
        Text(
            text = stringResource(R.string.programs_plan_sessions_completed, plan.progress.completedSessions, plan.progress.totalSessions),
            fontFamily = HrBody,
            fontSize = 12.sp,
            color = HrColors.TextLow,
        )
    }
}

private fun splitTemplateLabelRes(template: SplitTemplate): Int = when (template) {
    SplitTemplate.PPL -> R.string.split_ppl_title
    SplitTemplate.UPPER_LOWER -> R.string.split_upper_lower_title
    SplitTemplate.FULL_BODY -> R.string.split_fullbody_title
    SplitTemplate.BRO_SPLIT -> R.string.split_bro_title
    SplitTemplate.PHUL -> R.string.split_phul_title
    // Defensive only — QuickGenerate's own picker excludes CUSTOM and ProgramTransfer.decode
    // rejects it on import, so no live path currently produces a plan with this split.
    SplitTemplate.CUSTOM -> R.string.split_custom_title
}

/** Redesign Phase 3a — mock lines 127-130: the highlighted "today" row, condensed vs. Dashboard's
 * full hero [com.fitviet.app.ui.dashboard.DashboardScreen] `TodayCard` (this tab's job is the
 * month overview, not a second copy of Dashboard's own hero). Covers all 6
 * [TodayMonthlyPlanCard] cases so this card never contradicts Dashboard's — RestDay/PlanFinished/
 * NoPlan show status text with no CTA and no tap target; Unavailable/Completed show status text
 * with a tap-to-view-day target (no CTA, matching Dashboard's own no-restart-CTA-for-Completed
 * fix from the Phase 2 checkpoint); Training shows the real session + a "TẬP ›" CTA. */
@Composable
private fun PlanTodayCard(card: TodayMonthlyPlanCard, dayOfPlan: Int?, onStart: (dayId: Long) -> Unit, onOpenDay: (dayId: Long) -> Unit) {
    val openableDayId = when (card) {
        is TodayMonthlyPlanCard.Training -> card.dayId
        is TodayMonthlyPlanCard.Unavailable -> card.dayId
        is TodayMonthlyPlanCard.Completed -> card.dayId
        else -> null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.SurfaceAccent)
            .border(1.5.dp, HrColors.Accent, HrShapes.CardRegular)
            .let { if (openableDayId != null && card !is TodayMonthlyPlanCard.Training) it.clickable { onOpenDay(openableDayId) } else it }
            .padding(15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = if (dayOfPlan != null) {
                    stringResource(R.string.programs_today_label_with_day, dayOfPlan)
                } else {
                    stringResource(R.string.programs_today_label)
                },
                fontFamily = HrBody,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = HrColors.Accent,
            )
            val titleText = when (card) {
                is TodayMonthlyPlanCard.Training -> card.sessionType
                TodayMonthlyPlanCard.RestDay -> stringResource(R.string.dashboard_rest_day_title)
                is TodayMonthlyPlanCard.Unavailable -> stringResource(R.string.dashboard_unavailable_title)
                is TodayMonthlyPlanCard.Completed -> stringResource(R.string.dashboard_already_completed_title)
                TodayMonthlyPlanCard.PlanFinished, TodayMonthlyPlanCard.NoPlan -> stringResource(R.string.dashboard_plan_finished_title)
            }
            Text(
                text = titleText,
                fontFamily = HrDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = HrColors.TextHi,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (card is TodayMonthlyPlanCard.Training) {
                Text(
                    text = stringResource(R.string.dashboard_hero_meta_monthly_plan, card.exerciseCount, card.estimatedDurationMinutes),
                    fontFamily = HrBody,
                    fontSize = 12.sp,
                    color = HrColors.TextLow,
                )
            }
        }
        if (card is TodayMonthlyPlanCard.Training) {
            Box(
                modifier = Modifier
                    .pressScale(onClick = { onStart(card.dayId) })
                    .clip(HrShapes.ButtonSmall)
                    .background(HrColors.Accent)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text(text = stringResource(R.string.programs_today_cta), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HrColors.OnAccent)
            }
        }
    }
}

@Composable
private fun UpcomingDayRow(day: UpcomingDayUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, if (day.isRestDay) HrColors.BorderSoft else HrColors.Border, HrShapes.CardRegular)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = stringResource(R.string.programs_upcoming_day_label, stringResource(day.dayOfWeek.shortLabelRes()), day.dayOfPlan),
                fontFamily = HrBody,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = HrColors.TextFaint,
            )
            Text(
                text = day.sessionType ?: stringResource(R.string.dashboard_rest_day_title),
                fontFamily = HrBody,
                fontWeight = if (day.isRestDay) FontWeight.SemiBold else FontWeight.Bold,
                fontSize = 15.sp,
                color = if (day.isRestDay) HrColors.TextLow else HrColors.TextMid,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun LibraryEntryRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardSmall)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardSmall)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(text = stringResource(R.string.programs_library_title), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HrColors.TextHi)
            Text(
                text = stringResource(R.string.programs_library_subtitle),
                fontFamily = HrBody,
                fontSize = 12.sp,
                color = HrColors.TextLow,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(text = "›", fontFamily = HrBody, fontSize = 16.sp, color = HrColors.TextFaint)
    }
}

@Composable
private fun ExerciseResultRow(exercise: ExerciseEntity, onClick: () -> Unit) {
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
        Column {
            Text(text = exercise.nameVi, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HrColors.TextHi)
            Text(text = exercise.primaryMuscle, fontFamily = HrBody, fontSize = 12.sp, color = HrColors.TextLow)
        }
        Text(text = "›", fontFamily = HrBody, fontSize = 16.sp, color = HrColors.TextFaint)
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardSmall)
            .background(HrColors.SurfaceInput)
            .border(1.dp, HrColors.Border, HrShapes.CardSmall)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (query.isEmpty()) {
            Text(
                text = stringResource(R.string.programs_search_placeholder),
                fontFamily = HrBody,
                fontSize = 13.sp,
                color = HrColors.TextFaint,
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp, fontFamily = HrBody, color = HrColors.TextHi),
            cursorBrush = SolidColor(HrColors.Accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChips(selectedIndex: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        PROGRAM_FILTERS.forEachIndexed { index, filter ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(if (selected) HrColors.Accent else Color.Transparent)
                    .border(1.dp, if (selected) HrColors.Accent else HrColors.Border, PillShape)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = stringResource(filter.labelRes),
                    fontFamily = HrBody,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (selected) HrColors.OnAccent else HrColors.TextMid,
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
    HrColors.GradientCardStart to HrColors.GradientCardEnd,
    Color(0xFF1B2A20) to Color(0xFF0E1712),
    Color(0xFF16241C) to Color(0xFF0B120D),
    HrColors.SurfaceAccent to HrColors.BgDeep,
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
                drawLine(HrColors.Accent, Offset(size.width * 0.22f, barY), Offset(size.width * 0.78f, barY), strokeWidth = stroke.width, cap = stroke.cap)
                val plateHeight = size.height * 0.7f
                val plateWidth = size.width * 0.12f
                for (x in listOf(size.width * 0.14f, size.width * 0.86f)) {
                    drawLine(
                        HrColors.Accent,
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
                drawPath(path, HrColors.Accent)
            }
            ProgramCoverGlyph.CHART -> {
                val barWidth = size.width * 0.16f
                val gap = size.width * 0.1f
                val heights = listOf(0.4f, 0.7f, 1.0f)
                heights.forEachIndexed { index, fraction ->
                    val barHeight = size.height * fraction
                    val x = size.width * 0.12f + index * (barWidth + gap)
                    drawLine(
                        HrColors.Accent,
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

/** Redesign Gate 2b — the card's own tap ([onClick]) is now a real "commit" action (generates a
 * monthly plan from this program, see [ProgramsViewModel.generateFromProgram]), not pure
 * navigation. [onPreviewClick]/[onExportClick] are separate, smaller tap targets below the main
 * card body so they don't fall through to the card's own generate tap (Compose routes a touch to
 * whichever `clickable` is innermost at that point — a child's own modifier always wins over an
 * ancestor's, so no extra plumbing is needed to keep these independent). */
@Composable
private fun ProgramCard(
    program: ProgramEntity,
    isGenerating: Boolean,
    onClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onExportClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // No dedicated per-card loading state (isGenerating is screen-wide — only one
            // generation ever runs at a time) — dimming every card is a small, honest "something
            // is happening" signal so a multi-second generation doesn't look like a dead tap.
            .alpha(if (isGenerating) 0.6f else 1f)
            .pressScale(onClick = { if (!isGenerating) onClick() })
            .tiltOnDrag(maxDegrees = 6f)
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular),
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
                fontFamily = HrBody,
                fontSize = 12.sp,
                color = HrColors.TextLow,
            )
            // These sit right next to a destructive action (the card's own tap replaces the
            // active plan), so a near-miss must land on a genuine touch target, not a bare
            // ~18dp-tall Text — see Dimens.MinTouchTarget's own "44dp floor" doc.
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.programs_preview_button),
                    fontFamily = HrBody,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = HrColors.Accent,
                    modifier = Modifier
                        .heightIn(min = Dimens.MinTouchTarget)
                        .clickable(onClick = onPreviewClick)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                )
                Text(
                    text = stringResource(R.string.programs_export_button),
                    fontFamily = HrBody,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = HrColors.TextLow,
                    modifier = Modifier
                        .heightIn(min = Dimens.MinTouchTarget)
                        .clickable(onClick = onExportClick)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                )
            }
        }
    }
}

/** Feature #8 (Gate 42) — 3 uniform 14×4dp flat bars, not stars and not an ascending-height
 * "signal strength" shape, filled up to [ProgramDifficulty.levelSteps]'s count. Unrated ("Mọi
 * trình độ" or an unrecognized/imported level string, `levelSteps` returns `null`) uses
 * [HrColors.TextFaint] for its empty bars, distinct from a *rated* card's [HrColors.BarDim] —
 * collapsing both to the same color would make "this program has no difficulty rating"
 * indistinguishable from "this program is rated Beginner." */
@Composable
private fun DifficultyBadge(level: String) {
    val steps = ProgramDifficulty.levelSteps(level)
    val emptyColor = if (steps == null) HrColors.TextFaint else HrColors.BarDim
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) { index ->
            val filled = steps != null && index < steps
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(4.dp)
                    .clip(HrShapes.CardSmall)
                    .background(if (filled) HrColors.Accent else emptyColor),
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
            fontFamily = HrBody,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = HrColors.TextHi,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
        Box(
            modifier = Modifier
                .clip(HrShapes.CardSmall)
                .border(1.dp, HrColors.BorderAccentDim, HrShapes.CardSmall)
                .padding(horizontal = 7.dp, vertical = 2.dp),
        ) {
            Text(
                text = stringResource(R.string.programs_free_badge),
                fontFamily = HrBody,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = HrColors.Accent,
            )
        }
    }
}
