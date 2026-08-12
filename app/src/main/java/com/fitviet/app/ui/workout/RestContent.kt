package com.fitviet.app.ui.workout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitviet.app.R
import com.fitviet.app.ui.common.rememberReducedMotion
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentBorder
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.ChartBarIdle
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.MacroBarCarb
import com.fitviet.app.ui.theme.Motion
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.util.formatMinutesSeconds

private val RingSize = 260.dp
private val RingStrokeWidth = 10.dp
private const val PULSE_THRESHOLD_SECONDS = 3

/**
 * Premium-pass Moment 2 (Gate A3, direction 1a) — shared full-screen rest countdown used by both
 * the straight-set rest phase and the superset rest phase. Redesigned as a 260dp progress ring
 * (sweep gradient stroke, breathing glow, last-5s pulse+haptic) replacing the old plain-text
 * countdown; signature unchanged from before this gate, so both call sites in [WorkoutScreen]
 * keep working without modification.
 *
 * Progress is tracked against a running total that starts at the first [secondsRemaining] value
 * seen and grows by the exact delta every time the value jumps upward (the only way that
 * happens: the user taps "+15 giây", `onAddRest`) — re-basing the ring's total on the fly means
 * the arc always reflects "how far through the current, possibly-extended rest window" rather
 * than assuming a fixed 60s default.
 */
@Composable
fun RestContent(
    title: String,
    secondsRemaining: Int,
    nextLabel: String,
    onAddRest: () -> Unit,
    onSkipRest: () -> Unit,
    countdownFontSize: androidx.compose.ui.unit.TextUnit = 96.sp,
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
            .padding(horizontal = Dimens.ScreenPaddingHorizontal),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.labelLarge, color = TextMuted)
        Box(modifier = Modifier.padding(top = 16.dp).size(RingSize), contentAlignment = Alignment.Center) {
            if (!reducedMotion) {
                Box(
                    modifier = Modifier
                        .scale(glowScale.value)
                        .size(RingSize)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(Brush.radialGradient(listOf(Accent.copy(alpha = 0.15f), Accent.copy(alpha = 0f)))),
                )
            }
            Canvas(modifier = Modifier.size(RingSize)) {
                val strokeWidthPx = RingStrokeWidth.toPx()
                drawArc(
                    color = ChartBarIdle,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                )
                drawArc(
                    brush = Brush.sweepGradient(listOf(Accent, MacroBarCarb, Accent)),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                )
            }
            Text(
                text = formatMinutesSeconds(secondsRemaining),
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = Anton, fontSize = countdownFontSize),
                color = Accent,
                modifier = Modifier.scale(pulseScale.value),
            )
        }
        if (nextLabel.isNotEmpty()) {
            Text(text = nextLabel, style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.padding(top = 8.dp))
        }
        Row(modifier = Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .border(1.dp, AccentBorder, MaterialTheme.shapes.small)
                    .clickable(onClick = onAddRest)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Text(text = stringResource(R.string.workout_add_rest), style = MaterialTheme.typography.titleSmall, color = Accent)
            }
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(Accent)
                    .clickable(onClick = onSkipRest)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Text(text = stringResource(R.string.workout_skip_rest), style = MaterialTheme.typography.titleMedium, color = OnAccent)
            }
        }
    }
}
