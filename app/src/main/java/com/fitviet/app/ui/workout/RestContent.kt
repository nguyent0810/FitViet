package com.fitviet.app.ui.workout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitviet.app.R
import com.fitviet.app.ui.common.pressScale
import com.fitviet.app.ui.common.rememberReducedMotion
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.ui.theme.Motion

private val RingSize = 260.dp
private val RingStrokeWidth = 10.dp
private const val PULSE_THRESHOLD_SECONDS = 4

/**
 * Premium-pass Moment 2 (Gate A3, direction 1a) — shared full-screen rest countdown used by both
 * the straight-set rest phase and the superset rest phase. A 260dp progress ring (sweep gradient
 * stroke, breathing glow, last-5s pulse+haptic) rather than the old plain-text countdown; signature
 * unchanged from before that gate, so both call sites in [WorkoutScreen] keep working unmodified.
 *
 * Redesign Gate 4b — the Hit & Run mock's own rest screen (design HTML lines 288-316, inside its
 * interactive prototype, not just the static comparison — this genuinely is the mock's authoritative
 * spec) is flatter: no ring, just a bare "NGHỈ" label and lime countdown numeral. This gate
 * deliberately keeps the ring/glow/pulse mechanic rather than deleting it, on two grounds: the
 * redesign handoff's own motion clause (README line 147) says to keep *existing* motion patterns
 * and explicitly caps only *new* animation ("không thêm animation mới ngoài toast slide-up"), and
 * the Premium Moments handoff that shipped this mechanic locks it by name ("2e Rest — chốt theo
 * 1a"). Deleting an already-shipped, already-reviewed motion pattern to match a static screenshot
 * would undo that earlier decision, not correct a bug. What DOES change here is every color/type
 * token — legacy `Accent`/`Anton`/etc. swapped for `HrColors`/`HrDisplay`/etc. — so the retained
 * ring reads as part of the new visual language rather than the old one.
 *
 * The mock's own 110sp countdown spec doesn't fit inside a 260dp ring with a 10dp stroke at the
 * conventional "`M:SS`" width — [restCountdownLabel] adopts the mock's own leading-zero-free
 * "`:SS`" format below 60s instead. Review finding (Gate 4b) — measured against the actual shipped
 * font (Bricolage Grotesque 800), every label up to "9:59" clears the ring's inner chord at the
 * full 110sp, so no dynamic shrink is needed (an earlier draft shrank past 60s remaining, which
 * misfired on every rest's own starting value of 60 and bought nothing real values ever needed).
 */
@Composable
fun RestContent(
    title: String,
    secondsRemaining: Int,
    nextLabel: String,
    onAddRest: () -> Unit,
    onSkipRest: () -> Unit,
    countdownFontSize: TextUnit = 110.sp,
) {
    val reducedMotion = rememberReducedMotion()
    val haptics = LocalHapticFeedback.current

    // "Total" for the ring's progress fraction, re-based whenever secondsRemaining jumps upward
    // (the only way it can: onAddRest / "+15 giây"). Extending by the exact observed delta —
    // rather than only bumping up to a new peak — keeps the fraction honest even when a tap
    // doesn't push the value past its original starting point (e.g. 60 -> ticks down to 45 ->
    // +15 -> 60 again: total becomes 75, not 60, so the ring reads 60/75 rather than a
    // misleading 60/60).
    var maxSecondsSeen by remember { mutableIntStateOf(secondsRemaining.coerceAtLeast(1)) }
    var previousSecondsRemaining by remember { mutableIntStateOf(secondsRemaining) }
    if (secondsRemaining > previousSecondsRemaining) {
        maxSecondsSeen += (secondsRemaining - previousSecondsRemaining)
    }
    previousSecondsRemaining = secondsRemaining
    val progress by animateFloatAsState(
        targetValue = (secondsRemaining.toFloat() / maxSecondsSeen).coerceIn(0f, 1f),
        animationSpec = Motion.SpringGentle,
        label = "restProgress",
    )

    val glowScale = remember { Animatable(1f) }
    LaunchedEffect(reducedMotion) {
        if (!reducedMotion) {
            glowScale.animateTo(
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(tween(2400), repeatMode = RepeatMode.Reverse),
            )
        }
    }

    val pulseScale = remember { Animatable(1f) }
    LaunchedEffect(secondsRemaining) {
        if (secondsRemaining in 0 until PULSE_THRESHOLD_SECONDS + 1) {
            // HapticFeedbackType.Confirm isn't available in this project's Compose BOM
            // (2024.12.01 / Compose UI 1.7.6 only exposes LongPress/TextHandleMove) — LongPress
            // for every tick, including zero, is the closest available "tick" feel.
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (!reducedMotion) {
                pulseScale.snapTo(1f)
                pulseScale.animateTo(1.06f, Motion.SpringSnappy)
                pulseScale.animateTo(1f, Motion.SpringSnappy)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HrColors.BgDeep)
            .padding(horizontal = HrDimens.ScreenPaddingHorizontal),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 2.sp, color = HrColors.TextLow)
        Box(modifier = Modifier.padding(top = 16.dp).size(RingSize), contentAlignment = Alignment.Center) {
            if (!reducedMotion) {
                Box(
                    modifier = Modifier
                        .scale(glowScale.value)
                        .size(RingSize)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(HrColors.Accent.copy(alpha = 0.12f), HrColors.Accent.copy(alpha = 0f)))),
                )
            }
            Canvas(modifier = Modifier.size(RingSize)) {
                val strokeWidthPx = RingStrokeWidth.toPx()
                drawArc(
                    color = HrColors.BarDim,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                )
                drawArc(
                    // AccentHover — a lighter shade of the same lime, not a second hue — stands in
                    // for the old palette's separate MacroBarCarb mid-stop; HrColors has no
                    // multi-hue chart palette to draw a real second color from (see HrColors' own
                    // doc). Same reasoning as Gate 3d's involvementBarColor.
                    brush = Brush.sweepGradient(listOf(HrColors.Accent, HrColors.AccentHover, HrColors.Accent)),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                )
            }
            Text(
                text = restCountdownLabel(secondsRemaining),
                fontFamily = HrDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = countdownFontSize,
                color = HrColors.Accent,
                modifier = Modifier.scale(pulseScale.value),
            )
        }
        if (nextLabel.isNotEmpty()) {
            Text(text = nextLabel, fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextMid, modifier = Modifier.padding(top = 8.dp))
        }
        Row(modifier = Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .heightIn(min = Dimens.MinTouchTarget)
                    .pressScale(onClick = onAddRest)
                    .clip(HrShapes.CardSmall)
                    .border(1.5.dp, HrColors.BorderAccentDim, HrShapes.CardSmall)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.workout_add_rest), fontFamily = HrBody, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = HrColors.Accent)
            }
            Box(
                modifier = Modifier
                    .heightIn(min = Dimens.MinTouchTarget)
                    .pressScale(onClick = onSkipRest)
                    .clip(HrShapes.CardSmall)
                    .background(HrColors.Accent)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.workout_skip_rest), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HrColors.OnAccent)
            }
        }
    }
}

/** The mock's own countdown format (design HTML: literal `":59"`) — no leading zero digit below a
 * minute, `M:SS` at/above it. Deliberately NOT [com.fitviet.app.util.formatMinutesSeconds]: that
 * shared helper backs the session header's elapsed-time display and the finished screen's own
 * "Thời gian" tile, both of which want the conventional `0:59`/`1:05` form — this is a
 * countdown-ring-specific display format, not a general duration formatter. Also deliberately not
 * a port of the mock's own JS format function, which is buggy above 59 seconds (`':' +
 * padStart(2)` on a value like 75 yields `":75"`) — `+15 giây` (README line 143) makes values over
 * a minute a real, reachable case here, unlike the mock's own fixed demo data. */
private fun restCountdownLabel(secondsRemaining: Int): String {
    val clamped = secondsRemaining.coerceAtLeast(0)
    val minutes = clamped / 60
    val seconds = (clamped % 60).toString().padStart(2, '0')
    return if (minutes > 0) "$minutes:$seconds" else ":$seconds"
}
