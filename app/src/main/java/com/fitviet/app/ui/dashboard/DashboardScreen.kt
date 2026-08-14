package com.fitviet.app.ui.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.MonthlyPlanDayEntity
import com.fitviet.app.domain.DayVolume
import com.fitviet.app.domain.StatsRange
import com.fitviet.app.domain.TodayMonthlyPlanCard
import com.fitviet.app.domain.WeekDayCell
import com.fitviet.app.domain.WeekDayCellState
import com.fitviet.app.ui.common.pressScale
import com.fitviet.app.ui.common.tiltOnDrag
import com.fitviet.app.ui.profile.MonogramAvatar
import com.fitviet.app.ui.profile.avatarInitial
import com.fitviet.app.ui.quickgenerate.GenerateSheet
import com.fitviet.app.ui.quickgenerate.QuickGenerateViewModel
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.util.formatCompactKg
import com.fitviet.app.util.isoWeekNumber
import com.fitviet.app.util.longLabelRes
import com.fitviet.app.util.shortLabelRes
import java.time.LocalDate

/**
 * Redesign Gate 2c — full visual rebuild against mock variant "2a" (the one marked "ĐÃ CHỌN ✓" in
 * the design HTML's own side-by-side comparison, spec taken from that file's *interactive*
 * prototype block, not the simplified static comparison block — the two disagree in a few places,
 * e.g. the Today card's meta line, and the prototype's real template bindings are authoritative).
 * First real Dashboard consumer of [HrColors]/[HrDisplay]/[HrBody]/[HrShapes]/[HrDimens] — same
 * per-screen migration pattern Gate 2a's onboarding started.
 *
 * The mock drops the old `RecommendationCard`/`MuscleBalanceCard`/`NutritionCard` widgets entirely
 * — confirmed against the design HTML's own "TODAY" block, which has no equivalent element for any
 * of the three. `showRecommendationCard`/`showMuscleBalanceCard`/`showNutritionCard` (and their
 * Settings toggles) are left wired in the data layer but unrendered here rather than deleted in
 * this gate — Settings' own toggle rows are a separate screen's cleanup (README §18 still lists
 * them as a "MÀN HÔM NAY" group), out of scope for "rebuild DashboardScreen."
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenProfile: () -> Unit,
    // Redesign Gate 2c — the bottom "Nhật ký đầy đủ & số đo cơ thể" row. The mock's own HTML wires
    // this row to `openProfile` (same target as the avatar tap), on the assumption Profile already
    // has its own "Nhật ký & lịch tập đầy đủ" row through to Diary (README §14) — it doesn't yet;
    // that's Phase 7's Profile-cluster redesign, not built. Wiring this to Profile today would
    // leave Diary/WorkoutCalendar/WeeklyRecap completely unreachable (confirmed: nothing else in
    // the app navigates to them), so this stays pointed at Diary directly, same as before this
    // gate, until Phase 7 actually adds that onward link — a deliberate, temporary deviation from
    // the mock's literal binding, not an oversight.
    onOpenDiary: () -> Unit,
    // "Hit & Run" (Gate 63+) — starts today's monthly-plan-day session directly, bypassing
    // WorkoutPreview. A deliberate scope decision, not an oversight: retrofitting a preview
    // screen for monthly-plan days is deferred to a later gate, and going straight to the live
    // session is arguably closer to the feature's own "1-2 taps, no re-choosing" goal anyway.
    onStartMonthlyPlanDay: (dayId: Long) -> Unit,
    // Redesign Gate 3c — [GenerateSheet]'s own state holder, hosted locally by this screen (see
    // that file's own doc for why it's per-screen rather than a shared nav destination the old
    // full-screen "Quick Generate" was). Opened by the empty-state CTA and the Today card's
    // PlanFinished CTA (both previously navigated to that screen via a plain `onGenerateMonthlyPlan`
    // callback).
    quickGenerateViewModel: QuickGenerateViewModel,
    // Redesign Gate 3c — the sheet's own CTA tap: generate + await + navigate-to-Home + error-Toast,
    // same logic the retired Quick Generate screen's nav-host call site used to own.
    onGenerateConfirmed: () -> Unit,
    // "Hit & Run" (Gate 63+) Regenerate UI — the missed-day dialog's own "Xem lịch tháng" link.
    // Redesign Gate 2c also wires this into TodayCard's own "Xem kế hoạch tháng" link — without
    // that second wiring, MonthlyPlanDetailScreen was reachable only by way of an unresolved
    // missed day surfacing the dialog (the app's sole `navigate(...MonthlyPlanDetail...)` call
    // site was this one), stranding the whole screen the same way the Diary link above was
    // almost stranded.
    onViewMonthlyPlan: () -> Unit,
    // Redesign Gate 2c — Today card's "Chi tiết" link (labeled "Xem trước"/Preview in an earlier
    // draft, corrected — see the review that caught it: the destination screen has a regenerate
    // button and per-exercise swap actions, so "preview" overclaimed read-only-ness). Reuses the
    // existing Regenerate-UI day detail screen rather than building a new monthly-plan-aware
    // preview screen from scratch (that screen is program-day-only post-Gate-2b). Only ever called
    // with a real dayId (the link is hidden otherwise).
    onPreviewToday: (dayId: Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Not `remember`ed: recomputed each recomposition (including the ones the repository's
    // midnight tick triggers) so the greeting date doesn't freeze at whatever day this opened on.
    val today = LocalDate.now()

    // Redesign Gate 3c — local to this screen, not hoisted into DashboardViewModel: it's pure UI
    // state (which sheet is showing), the same "screen owns its own dialog visibility" pattern
    // MissedDayDialog's own null-check below already uses, just for a sheet instead of a dialog.
    var isGenerateSheetOpen by remember { mutableStateOf(false) }
    val openGenerateSheet = {
        quickGenerateViewModel.refreshPrefill()
        isGenerateSheetOpen = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HrColors.Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(HrDimens.CardGapVertical),
    ) {
        GreetingHeader(today = today, displayName = uiState.displayName, avatarId = uiState.avatarId, onAvatarClick = onOpenProfile)

        val monthlyPlanCard = uiState.todayMonthlyPlanCard
        if (monthlyPlanCard != TodayMonthlyPlanCard.NoPlan) {
            TodayCard(
                card = monthlyPlanCard,
                dayOfPlan = uiState.dayOfPlan,
                totalDaysInPlan = uiState.totalDaysInPlan,
                onStart = { (monthlyPlanCard as? TodayMonthlyPlanCard.Training)?.let { onStartMonthlyPlanDay(it.dayId) } },
                onPreview = {
                    val dayId = when (monthlyPlanCard) {
                        is TodayMonthlyPlanCard.Training -> monthlyPlanCard.dayId
                        is TodayMonthlyPlanCard.Unavailable -> monthlyPlanCard.dayId
                        is TodayMonthlyPlanCard.Completed -> monthlyPlanCard.dayId
                        else -> null
                    }
                    dayId?.let(onPreviewToday)
                },
                onViewMonthlyPlan = onViewMonthlyPlan,
                onGenerateMonthlyPlan = openGenerateSheet,
            )
        } else {
            EmptyPlanCard(onClick = openGenerateSheet)
        }

        StatTilesRow(
            streakDays = uiState.stats.streakDays,
            sessionsThisWeek = uiState.sessionsRolling7Day,
            weeklySessionTarget = uiState.weeklySessionTarget,
            volumeThisWeekKg = uiState.volumeRolling7DayKg,
            prCountThisWeek = uiState.prCountThisWeek,
        )

        WeekCard(cells = uiState.weekDayCells, todayCard = monthlyPlanCard)

        VolumeCard(
            range = uiState.selectedRange,
            series = uiState.rangeSeries,
            onSelectRange = viewModel::selectRange,
        )

        FullDiaryLinkRow(onClick = onOpenDiary)
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

    if (isGenerateSheetOpen) {
        // Closes immediately on tap rather than waiting for onGenerateConfirmed's async
        // generate()+navigate to finish — generation is a fast local DB write (no network round
        // trip), the mock's own demo shows no loading state either, and a failure still surfaces
        // via onGenerateConfirmed's error Toast after the sheet is gone. Keeping the sheet open
        // through the whole async call would need a success/failure signal threaded back from the
        // NavHost-owned onGenerateConfirmed into this screen's local sheet state, which isn't
        // worth the plumbing for an operation this fast.
        GenerateSheet(
            viewModel = quickGenerateViewModel,
            onDismiss = { isGenerateSheetOpen = false },
            onGenerateClick = {
                isGenerateSheetOpen = false
                onGenerateConfirmed()
            },
        )
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
                fontFamily = HrBody,
                fontSize = 13.sp,
                color = HrColors.TextLow,
            )
            Text(
                text = stringResource(R.string.dashboard_greeting, displayName),
                fontFamily = HrDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp,
                letterSpacing = (-0.5).sp,
                color = HrColors.TextHi,
            )
        }
        MonogramAvatar(
            initial = avatarInitial(displayName),
            avatarId = avatarId,
            size = 48.dp,
            style = TextStyle(fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp),
            modifier = Modifier.clickable(onClick = onAvatarClick),
        )
    }
}

/** The mock's Today card (variant "2a"). [onPreview] is only ever wired when the resolved card
 * carries a real day id (`Training`/`Unavailable`/`Completed`) — [TodayMonthlyPlanCard.RestDay]/
 * [TodayMonthlyPlanCard.PlanFinished] hide the link entirely rather than calling it with nothing to
 * preview. Equipment ("· Phòng gym" in the mock's meta line) isn't threaded through here —
 * [TodayMonthlyPlanCard.Training] doesn't carry it, and adding it would mean widening that type
 * just for this one label; the meta line reads "X bài · ~Y phút" without it, matching the design
 * HTML's own static (non-interactive) comparison block, which already omits it.
 *
 * Phase 2 checkpoint — the mock's own "Đổi buổi" link (bound to `goPlanTab`, a plain tab switch)
 * was dropped: post-Gate-2b the "Kế hoạch"/Programs tab it pointed at is a read-only
 * generation-input browser whose only two actions replace the whole active plan, not change
 * today's session — see [com.fitviet.app.ui.dashboard.DashboardViewModel]'s own
 * `dismissMissedDayToViewPlan` doc, which already documents that a real day-swap picker
 * ([com.fitviet.app.data.repository.MonthlyPlanRepository.swapTwoDays]) has no production caller
 * yet. "Chi tiết" already opens the one screen that actually can change today's session
 * (regenerate/swap), so the mock's two links' *destinations* survive under one label rather than
 * two mismatched ones. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodayCard(
    card: TodayMonthlyPlanCard,
    dayOfPlan: Int?,
    totalDaysInPlan: Int?,
    onStart: () -> Unit,
    onPreview: () -> Unit,
    onViewMonthlyPlan: () -> Unit,
    onGenerateMonthlyPlan: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tiltOnDrag()
            .clip(HrShapes.CardHero)
            .background(Brush.linearGradient(listOf(HrColors.GradientCardStart, HrColors.GradientCardEnd), start = Offset(0f, 0f)))
            .border(1.dp, HrColors.BorderGradient, HrShapes.CardHero)
            .padding(22.dp),
    ) {
        Text(
            text = if (dayOfPlan != null && totalDaysInPlan != null) {
                stringResource(R.string.dashboard_hero_label_with_day, dayOfPlan, totalDaysInPlan)
            } else {
                stringResource(R.string.dashboard_hero_label)
            },
            fontFamily = HrBody,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            color = HrColors.Accent,
        )
        when (card) {
            is TodayMonthlyPlanCard.Training -> {
                Text(
                    text = card.sessionType,
                    fontFamily = HrDisplay,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 38.sp,
                    lineHeight = 40.sp,
                    letterSpacing = (-0.5).sp,
                    color = HrColors.TextHi,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = stringResource(R.string.dashboard_hero_meta_monthly_plan, card.exerciseCount, card.estimatedDurationMinutes),
                    fontFamily = HrBody,
                    fontSize = 14.sp,
                    color = HrColors.TextLow,
                )
            }
            TodayMonthlyPlanCard.RestDay -> TodayCardStatusText(R.string.dashboard_rest_day_title, R.string.dashboard_rest_day_meta)
            is TodayMonthlyPlanCard.Unavailable -> TodayCardStatusText(R.string.dashboard_unavailable_title, R.string.dashboard_unavailable_meta)
            is TodayMonthlyPlanCard.Completed -> TodayCardStatusText(R.string.dashboard_already_completed_title, R.string.dashboard_already_completed_meta)
            TodayMonthlyPlanCard.PlanFinished, TodayMonthlyPlanCard.NoPlan ->
                TodayCardStatusText(R.string.dashboard_plan_finished_title, R.string.dashboard_plan_finished_meta)
        }
        // Phase 2 checkpoint fix — PlanFinished used to have no CTA at all, only the (now-removed,
        // wrong-destination — see this composable's own doc) "Đổi buổi" link as its sole onward
        // path; that made a finished 4-week plan a dead end on Dashboard. Reuses the CTA's exact
        // shell, just re-labeled/re-targeted, rather than a smaller secondary-looking button —
        // "make a new plan" deserves the same visual weight "start today's session" gets.
        val ctaLabelRes = when (card) {
            is TodayMonthlyPlanCard.Training -> R.string.dashboard_start_workout_today
            TodayMonthlyPlanCard.PlanFinished -> R.string.dashboard_generate_cta_button
            else -> null
        }
        if (ctaLabelRes != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .pressScale(onClick = if (card is TodayMonthlyPlanCard.Training) onStart else onGenerateMonthlyPlan)
                    .clip(HrShapes.ButtonCta)
                    .background(HrColors.Accent)
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(ctaLabelRes),
                    fontFamily = HrDisplay,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = HrColors.OnAccent,
                )
            }
        }
        val hasPreviewTarget = card is TodayMonthlyPlanCard.Training || card is TodayMonthlyPlanCard.Unavailable || card is TodayMonthlyPlanCard.Completed
        // FlowRow, not Row — even 2 links + 1 separator can exceed the card's inner width at large
        // font scales / narrow devices (flagged as width-fragile at the Gate 2c review with a plain
        // Row, when there were 3 links); wrapping to a second line beats an overflow clip.
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            if (hasPreviewTarget) {
                Text(
                    text = stringResource(R.string.dashboard_preview_link),
                    fontFamily = HrBody,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = HrColors.TextLow,
                    modifier = Modifier.clickable(onClick = onPreview).padding(horizontal = 9.dp, vertical = 6.dp),
                )
                Text(text = "·", fontFamily = HrBody, fontSize = 13.sp, color = HrColors.BorderSoft, modifier = Modifier.padding(vertical = 6.dp))
            }
            // Redesign Gate 2c — not in the mock's own Today card; added so
            // MonthlyPlanDetailScreen stays reachable without an unresolved missed day (see
            // [DashboardScreen]'s own `onViewMonthlyPlan` doc). Reuses the pre-existing
            // "Xem lịch tháng ›" string the old hero card's link used, now otherwise unreferenced.
            Text(
                text = stringResource(R.string.dashboard_view_monthly_plan),
                fontFamily = HrBody,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = HrColors.TextLow,
                modifier = Modifier.clickable(onClick = onViewMonthlyPlan).padding(horizontal = 9.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun TodayCardStatusText(@StringRes titleRes: Int, @StringRes metaRes: Int) {
    Text(
        text = stringResource(titleRes),
        fontFamily = HrDisplay,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 32.sp,
        color = HrColors.TextHi,
        modifier = Modifier.padding(top = 2.dp),
    )
    Text(
        text = stringResource(metaRes),
        fontFamily = HrBody,
        fontSize = 14.sp,
        color = HrColors.TextLow,
    )
}

/** "Hit & Run" (Gate 63+) empty-state CTA, shown in place of [TodayCard] when there's no active
 * plan yet — redesign Gate 2c re-skinned this against the mock's exact "planEmpty" block (same
 * gradient shell as the Today card itself, not the old accent-outline row treatment). */
@Composable
private fun EmptyPlanCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardHero)
            .background(Brush.linearGradient(listOf(HrColors.GradientCardStart, HrColors.GradientCardEnd), start = Offset(0f, 0f)))
            .border(1.dp, HrColors.BorderGradient, HrShapes.CardHero)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.dashboard_generate_cta_title),
            fontFamily = HrDisplay,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
            lineHeight = 30.sp,
            color = HrColors.TextHi,
        )
        Text(
            text = stringResource(R.string.dashboard_generate_cta_body),
            fontFamily = HrBody,
            fontSize = 14.sp,
            color = HrColors.TextLow,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .pressScale(onClick = onClick)
                .clip(HrShapes.ButtonCta)
                .background(HrColors.Accent)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.dashboard_generate_cta_button),
                fontFamily = HrDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = HrColors.OnAccent,
            )
        }
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
                    fontFamily = HrBody,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = HrColors.Accent,
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
private fun StatTilesRow(streakDays: Int, sessionsThisWeek: Int, weeklySessionTarget: Int?, volumeThisWeekKg: Double, prCountThisWeek: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(value = streakDays.toString(), label = stringResource(R.string.dashboard_stat_streak), modifier = Modifier.weight(1f), accentValue = true)
        StatTile(
            value = if (weeklySessionTarget != null) "$sessionsThisWeek/$weeklySessionTarget" else sessionsThisWeek.toString(),
            label = stringResource(R.string.dashboard_stat_sessions_this_week),
            modifier = Modifier.weight(1f),
        )
        StatTile(value = formatCompactKg(volumeThisWeekKg), label = stringResource(R.string.dashboard_stat_volume_this_week), modifier = Modifier.weight(1f))
        StatTile(value = prCountThisWeek.toString(), label = stringResource(R.string.dashboard_stat_pr), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier, accentValue: Boolean = false) {
    Column(
        modifier = modifier
            .clip(HrShapes.CardSmall)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardSmall)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            fontFamily = HrDisplay,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = if (accentValue) HrColors.Accent else HrColors.TextHi,
        )
        Text(
            text = label,
            fontFamily = HrBody,
            fontSize = 11.sp,
            color = HrColors.TextLow,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** Redesign Gate 2c — the "Tuần này" card. [com.fitviet.app.domain.WeekDayCellCalculator]'s own
 * doc covers the trailing-7-day window; this composable only maps states to the mock's exact
 * colors/glyphs. The subtitle line (mock's `{{ weekDetail }}`, HTML line 79) is always shown, not
 * just on completion — the mock's own JS (`s.todayDone ? '...' : 'Hôm nay: ... — chưa tập'`)
 * branches on completion, it doesn't omit the line otherwise. [todayCard] supplies the real
 * today session type (for [TodayMonthlyPlanCard.Training]/[TodayMonthlyPlanCard.Unavailable])
 * rather than the mock's hardcoded "Lưng & Tay" placeholder; [TodayMonthlyPlanCard.RestDay]/
 * [TodayMonthlyPlanCard.PlanFinished]/[TodayMonthlyPlanCard.NoPlan] have no session type to name,
 * so they fall back to a generic "chưa tập" line rather than wrongly claiming today's a rest day
 * when there might be no plan at all. */
@Composable
private fun WeekCard(cells: List<WeekDayCell>, todayCard: TodayMonthlyPlanCard) {
    val todayCell = cells.lastOrNull()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(R.string.dashboard_week_card_title), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HrColors.TextHi)
            when (todayCell?.state) {
                WeekDayCellState.TODAY_DONE ->
                    Text(text = stringResource(R.string.dashboard_week_detail_done), fontFamily = HrBody, fontSize = 12.sp, color = HrColors.TextLow)
                WeekDayCellState.TODAY_PENDING -> {
                    val sessionType = when (todayCard) {
                        is TodayMonthlyPlanCard.Training -> todayCard.sessionType
                        is TodayMonthlyPlanCard.Unavailable -> todayCard.sessionType
                        else -> null
                    }
                    val subtitle = if (sessionType != null) {
                        stringResource(R.string.dashboard_week_detail_pending, sessionType)
                    } else {
                        stringResource(R.string.dashboard_week_detail_no_session)
                    }
                    Text(text = subtitle, fontFamily = HrBody, fontSize = 12.sp, color = HrColors.TextLow)
                }
                else -> Unit
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            cells.forEach { cell ->
                WeekDayCircle(cell = cell, modifier = Modifier.weight(1f))
            }
        }
    }
}

private data class WeekDayCircleStyle(val bg: Color?, val border: Color, val glyphColor: Color, val glyph: String)

@Composable
private fun WeekDayCircle(cell: WeekDayCell, modifier: Modifier = Modifier) {
    val isToday = cell.state == WeekDayCellState.TODAY_DONE || cell.state == WeekDayCellState.TODAY_PENDING
    val labelColor = if (isToday) HrColors.Accent else HrColors.TextFaint
    val style = when (cell.state) {
        WeekDayCellState.TODAY_DONE -> WeekDayCircleStyle(HrColors.Accent, HrColors.Accent, HrColors.OnAccent, "✓")
        WeekDayCellState.TODAY_PENDING -> WeekDayCircleStyle(null, HrColors.Accent, HrColors.Accent, "·")
        WeekDayCellState.DONE -> WeekDayCircleStyle(null, HrColors.BorderAccentDim, HrColors.Accent, "✓")
        WeekDayCellState.EMPTY -> WeekDayCircleStyle(null, HrColors.Border, HrColors.TextFaint, "–")
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(cell.date.dayOfWeek.shortLabelRes()),
            fontFamily = HrBody,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = labelColor,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(style.bg ?: Color.Transparent)
                .border(1.5.dp, style.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = style.glyph, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = style.glyphColor)
        }
    }
}

/** Redesign Gate 2c — "Khối lượng" card. Only 2 pills (Tuần/4 tuần), down from the pre-redesign
 * card's 3 (week/month/all) — the mock's own volume card has exactly 2, and "all" isn't part of
 * this screen's spec (Diary keeps its own full range picker; this card's job is just a Dashboard
 * glance). Reuses [StatsRange.WEEK]/[StatsRange.MONTH] for the underlying series rather than
 * introducing a parallel enum — [StatsRange.ALL] is simply never selectable from here. Bars are
 * non-interactive (the mock's own `bars` never bind an `onClick`) — the pre-redesign card's
 * tap-a-bar-to-preview-its-value interaction is dropped along with the header text it used to
 * update; `DashboardViewModel.selectDay`/`explicitDayIndex` are left wired but now unused by any
 * caller, same "not this gate's cleanup" deferral as the widget-toggle fields above. */
@Composable
private fun VolumeCard(
    range: StatsRange,
    series: List<DayVolume>,
    onSelectRange: (StatsRange) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(R.string.dashboard_weekly_volume_title), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HrColors.TextHi)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                VolumePill(
                    label = stringResource(R.string.dashboard_volume_pill_week),
                    selected = range == StatsRange.WEEK,
                    onClick = { onSelectRange(StatsRange.WEEK) },
                )
                VolumePill(
                    label = stringResource(R.string.dashboard_volume_pill_month),
                    selected = range == StatsRange.MONTH,
                    onClick = { onSelectRange(StatsRange.MONTH) },
                )
            }
        }
        val maxVolume = series.maxOfOrNull { it.volumeKg }?.takeIf { it > 0 } ?: 1.0
        Row(
            modifier = Modifier.fillMaxWidth().height(84.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            series.forEachIndexed { index, entry ->
                val fraction = if (entry.volumeKg <= 0.0) 0.08f else (entry.volumeKg / maxVolume).toFloat().coerceIn(0.15f, 1f)
                val isLast = index == series.lastIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isLast) HrColors.Accent else HrColors.BarDim),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            series.forEachIndexed { index, entry ->
                val isLast = index == series.lastIndex
                Text(
                    text = barLabel(range, entry),
                    fontFamily = HrBody,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = if (isLast) HrColors.Accent else HrColors.TextFaint,
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
private fun VolumePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(if (selected) HrColors.Accent else Color.Transparent)
            .border(1.dp, if (selected) HrColors.Accent else HrColors.Border, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            fontFamily = HrBody,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (selected) HrColors.OnAccent else HrColors.TextLow,
        )
    }
}

/** [range]'s bucket label: per-day for WEEK, per-week-number for MONTH — same convention as
 * Diary's own week-bucket chart. */
@Composable
private fun barLabel(range: StatsRange, entry: DayVolume): String = if (range == StatsRange.WEEK) {
    stringResource(entry.date.dayOfWeek.shortLabelRes())
} else {
    stringResource(R.string.diary_week_label, entry.date.isoWeekNumber())
}

/** Redesign Gate 2c — the bottom "Nhật ký đầy đủ & số đo cơ thể" row. See [DashboardScreen]'s own
 * `onOpenDiary` doc for why this routes to Diary rather than the mock's literal `openProfile`
 * binding — Profile doesn't have an onward Diary link yet. */
@Composable
private fun FullDiaryLinkRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.dashboard_full_diary_link), fontFamily = HrBody, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = HrColors.TextMid)
        Text(text = "›", fontFamily = HrBody, fontSize = 16.sp, color = HrColors.TextFaint)
    }
}
