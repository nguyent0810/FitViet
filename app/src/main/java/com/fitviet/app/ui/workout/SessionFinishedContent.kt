package com.fitviet.app.ui.workout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitviet.app.R
import com.fitviet.app.domain.CaloriesCalculator
import com.fitviet.app.ui.common.pressScale
import com.fitviet.app.ui.common.rememberReducedMotion
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.ui.theme.Motion
import com.fitviet.app.util.formatMinutesSeconds
import com.fitviet.app.util.formatVi
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val COUNT_UP_DURATION_MS = 1800
private const val TILE_STAGGER_MS = 60L
private const val CONFETTI_PARTICLE_COUNT = 140

// Falling-rain duration for the clock loop. Must cover every particle's worst-case lifetime
// (max startDelayMs 1200 + max fallDurationMs 3800 = 5000ms below), not just the nominal "~4.5s"
// from the brief — stopping the clock short of that leaves slow-drawn particles frozen mid-fall
// at whatever alpha they had when the loop exited, instead of finishing their fade-out.
private const val CONFETTI_MAX_START_DELAY_MS = 1200f
private const val CONFETTI_MAX_FALL_DURATION_MS = 3800f
private const val CONFETTI_DURATION_MS = CONFETTI_MAX_START_DELAY_MS + CONFETTI_MAX_FALL_DURATION_MS + 100f

private data class ConfettiParticle(
    val startXFraction: Float,
    val fallDurationMs: Float,
    val startDelayMs: Float,
    val driftAmplitudePx: Float,
    val rotationDegreesPerMs: Float,
    val sizePx: Float,
    val colorIndex: Int,
)

/**
 * Premium-pass Moment 3 (Gate A4, direction 1a) — session-finished summary with synced count-up
 * tiles and a falling-rain confetti backdrop. Signature unchanged from before that gate.
 *
 * Redesign Gate 4b — same deliberate-deviation call as [RestContent]'s own doc: the mock's own
 * finished screen (design HTML lines ~318-345, inside the interactive prototype) is a flat "XONG!"
 * with no confetti, but the Premium Moments handoff locks this mechanic by name ("2f Finished —
 * chốt theo 1a: confetti rơi + count-up đồng bộ") and the redesign's own motion clause protects
 * *existing* motion from being removed (README line 147). Kept, re-skinned to Hr tokens. The
 * `sessionShared` filled "Đã chia sẻ ✓" state on [ShareToCommunityButton] is a third retained
 * non-mock behavior for the same reason — the mock shows the share button unconditionally, this
 * app's own share-guard predates the mock and stays.
 */
@Composable
fun SessionFinishedContent(uiState: WorkoutUiState, onBackToHome: () -> Unit, onShare: () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        // HapticFeedbackType.Confirm isn't available in this project's Compose BOM (2024.12.01 /
        // Compose UI 1.7.6 only exposes LongPress/TextHandleMove) — LongPress is the closest
        // available "meaningful moment" feel.
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val countUp = remember { Animatable(0f) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            countUp.snapTo(1f)
        } else {
            countUp.animateTo(1f, tween(COUNT_UP_DURATION_MS, easing = FastOutSlowInEasing))
        }
    }

    val tileScales = remember { List(4) { Animatable(0f) } }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            tileScales.forEach { it.snapTo(1f) }
        } else {
            tileScales.forEachIndexed { index, anim ->
                launch {
                    kotlinx.coroutines.delay(index * TILE_STAGGER_MS)
                    anim.animateTo(1f, Motion.SpringBouncy)
                }
            }
        }
    }

    val t = countUp.value
    val kcalTotal = CaloriesCalculator.estimateKcal(uiState.sessionElapsedSeconds)

    Box(modifier = Modifier.fillMaxSize().background(HrColors.BgDeep)) {
        // Confetti must stay a sibling of (not painted over by) the Column below — see this file's
        // own review history: an earlier draft put the background on the Column instead, which
        // silently hid every particle since the Column is the later, opaque sibling in this Box.
        if (!reducedMotion) {
            ConfettiOverlay(modifier = Modifier.fillMaxSize())
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = HrDimens.ScreenPaddingHorizontal),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.workout_session_finished_title),
                fontFamily = HrDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 58.sp,
                color = HrColors.Accent,
                textAlign = TextAlign.Center,
            )
            Text(
                text = sessionFinishedSubtitle(uiState),
                fontFamily = HrBody,
                fontSize = 15.sp,
                color = HrColors.TextMid,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HrSummaryTile(
                    value = (uiState.sessionTotalSets * t).roundToInt().toString(),
                    label = stringResource(R.string.workout_stat_sets),
                    accent = true,
                    modifier = Modifier.weight(1f).scale(tileScales[0].value),
                )
                HrSummaryTile(
                    value = formatVi(uiState.sessionTotalVolumeKg * t),
                    label = stringResource(R.string.workout_stat_volume),
                    modifier = Modifier.weight(1f).scale(tileScales[1].value),
                )
                HrSummaryTile(
                    value = formatMinutesSeconds((uiState.sessionElapsedSeconds * t).roundToInt()),
                    label = stringResource(R.string.workout_stat_time),
                    modifier = Modifier.weight(1f).scale(tileScales[2].value),
                )
                HrSummaryTile(
                    // Feature #10 — a rough estimate, not a precise measurement; see CaloriesCalculator's doc comment.
                    value = (kcalTotal * t).roundToInt().toString(),
                    label = stringResource(R.string.workout_stat_kcal),
                    modifier = Modifier.weight(1f).scale(tileScales[3].value),
                )
            }
            Text(
                text = stringResource(R.string.workout_session_finished_note),
                fontFamily = HrBody,
                fontSize = 12.sp,
                color = HrColors.TextLow,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            ShareToCommunityButton(
                shared = uiState.sessionShared,
                onClick = onShare,
                modifier = Modifier.padding(top = 20.dp),
            )
            HrPrimaryActionButton(
                text = stringResource(R.string.workout_back_to_home),
                onClick = onBackToHome,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/** "Buổi N/M · chuỗi X ngày liên tiếp" when [WorkoutUiState.sessionNumberInPlan]/`sessionTotalInPlan`
 * resolved a plan row (see [WorkoutViewModel.readSessionPlanProgress]'s own doc for when they
 * don't); streak-only otherwise — there's always a streak stat to show (0 on a first-ever session),
 * so this never has a "nothing to say" case, unlike a true null-when-empty helper. */
@Composable
private fun sessionFinishedSubtitle(uiState: WorkoutUiState): String {
    val number = uiState.sessionNumberInPlan
    val total = uiState.sessionTotalInPlan
    return if (number != null && total != null) {
        stringResource(R.string.workout_session_finished_subtitle_with_plan, number, total, uiState.sessionStreakDays)
    } else {
        stringResource(R.string.workout_session_finished_subtitle_streak_only, uiState.sessionStreakDays)
    }
}

/** One [withFrameNanos] clock drives every particle's position — particles are plain data
 * (no per-particle composition state), positions are a pure function of elapsed time. */
@Composable
private fun ConfettiOverlay(modifier: Modifier = Modifier) {
    // HrColors has no multi-hue chart/macro palette (single-accent by design, see its own doc) —
    // variety here comes from alpha/lightness variants of the one accent plus a couple of neutral
    // text tokens, not a second or third hue, same reasoning as RestContent's ring gradient.
    // Review finding (Gate 4b) — Color.copy(alpha=) REPLACES alpha, it doesn't multiply, so a
    // color's own base alpha has to be tracked per-particle and combined with the fall-fade alpha
    // at draw time (see the multiply below), not baked into this list via a second .copy() here.
    val colors = listOf(HrColors.Accent, HrColors.AccentHover, HrColors.Accent, HrColors.TextHi, HrColors.BorderAccentDim)
    val baseAlphas = listOf(1f, 1f, 0.7f, 1f, 1f)
    val particles = remember {
        val random = Random(seed = 42)
        List(CONFETTI_PARTICLE_COUNT) {
            ConfettiParticle(
                startXFraction = random.nextFloat(),
                fallDurationMs = 2200f + random.nextFloat() * 1600f,
                startDelayMs = random.nextFloat() * 1200f,
                driftAmplitudePx = 20f + random.nextFloat() * 40f,
                rotationDegreesPerMs = (random.nextFloat() - 0.5f) * 0.6f,
                sizePx = 6f + random.nextFloat() * 6f,
                colorIndex = random.nextInt(colors.size),
            )
        }
    }

    var elapsedMs by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (elapsedMs < CONFETTI_DURATION_MS) {
            val now = withFrameNanos { it }
            elapsedMs = (now - startNanos) / 1_000_000f
        }
    }

    Canvas(modifier = modifier) {
        particles.forEach { particle ->
            val localElapsed = elapsedMs - particle.startDelayMs
            if (localElapsed < 0f) return@forEach
            val fallProgress = (localElapsed / particle.fallDurationMs).coerceIn(0f, 1f)
            if (fallProgress >= 1f) return@forEach

            val y = fallProgress * (size.height + particle.sizePx * 2f) - particle.sizePx
            val drift = sin(fallProgress * Math.PI.toFloat() * 2f) * particle.driftAmplitudePx
            val x = particle.startXFraction * size.width + drift
            val alpha = (1f - fallProgress).coerceIn(0f, 1f)

            rotate(degrees = localElapsed * particle.rotationDegreesPerMs, pivot = Offset(x, y)) {
                drawRect(
                    color = colors[particle.colorIndex].copy(alpha = alpha * baseAlphas[particle.colorIndex]),
                    topLeft = Offset(x - particle.sizePx / 2f, y - particle.sizePx / 2f),
                    size = androidx.compose.ui.geometry.Size(particle.sizePx, particle.sizePx),
                )
            }
        }
    }
}

/** Outlined while unshared (matches the app's established secondary-action style, e.g.
 * `RemindersScreen`'s "+ Thêm nhắc nhở"); once shared, flips to a filled/inert confirmation state
 * with no click handler — [WorkoutViewModel.shareToCommunity] already guards against a repeat
 * share, so there's nothing left for a second tap to do. */
@Composable
private fun ShareToCommunityButton(shared: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (shared) Modifier else Modifier.pressScale(onClick = onClick))
            .clip(HrShapes.ButtonCta)
            .background(if (shared) HrColors.SurfaceAccent else Color.Transparent)
            .border(Dimens.SelectedBorderWidth, HrColors.BorderAccentDim, HrShapes.ButtonCta)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(if (shared) R.string.workout_share_button_done else R.string.workout_share_button),
            fontFamily = HrBody,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = HrColors.Accent,
        )
    }
}

/** Hr-token stat tile, per the mock's own dims (20sp value/11sp label vs. the legacy [SummaryTile]'s
 * 22sp/12sp). Kept local to this file rather than exported — nothing else needs an Hr-token stat
 * tile yet; [SummaryTile] itself stays legacy-token permanently for `CommunityScreen`'s own reuse,
 * see that composable's own doc. [accent] mirrors [SummaryTile]'s own parameter — the mock gives
 * only the "Set" tile the lime value color, matching `SupersetScreens.kt`'s existing `accent = true`
 * on the same stat. */
@Composable
private fun HrSummaryTile(value: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(
        modifier = modifier
            .clip(HrShapes.CardSmall)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardSmall)
            .padding(horizontal = 10.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value, fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = if (accent) HrColors.Accent else HrColors.TextHi)
        Text(text = label, fontFamily = HrBody, fontSize = 11.sp, color = HrColors.TextLow, modifier = Modifier.padding(top = 2.dp))
    }
}
