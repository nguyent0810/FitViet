package com.fitviet.app.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fitviet.app.R
import com.fitviet.app.ui.common.rememberReducedMotion
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentBorder
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HeroGradientEnd
import com.fitviet.app.ui.theme.HeroGradientStart
import com.fitviet.app.ui.theme.MacroBarCarb
import com.fitviet.app.ui.theme.MacroBarFat
import com.fitviet.app.ui.theme.Motion
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.TextBody
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val BURST_PARTICLE_COUNT = 60
private const val BURST_DURATION_MS = 1100f

private data class BurstParticle(
    val angleRadians: Float,
    val travelPx: Float,
    val startDelayMs: Float,
    val sizePx: Float,
    val colorIndex: Int,
)

/**
 * Gate D4 — a full-screen "kinetic" celebration moment (per the Premium Moments pass-2 direction
 * 1b treatment: single burst, full-bleed gradient — distinct from [com.fitviet.app.ui.workout
 * .SessionFinishedContent]'s falling-rain confetti, which stays direction 1a per that gate's own
 * per-screen assignment). Shown by [DashboardScreen] whenever [DashboardUiState.streakMilestone]
 * is non-null; [onDismiss] persists the dismissal so it doesn't reopen on the next recomposition.
 *
 * A real [Dialog] (not an inline overlay `Box`) so it draws above the system status/nav chrome and
 * intercepts back-press via [onDismissRequest][DialogProperties] the same way it intercepts the
 * "Tuyệt vời!" tap — a milestone congratulation is a light celebratory moment, not a decision the
 * user must explicitly resolve like [MissedDayDialog], so both exits are equally valid.
 */
@Composable
fun StreakMilestoneOverlay(streakDays: Int, milestone: Int, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val reducedMotion = rememberReducedMotion()
        val haptics = LocalHapticFeedback.current

        LaunchedEffect(milestone) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }

        val badgeScale = remember(milestone) { Animatable(0f) }
        LaunchedEffect(milestone, reducedMotion) {
            if (reducedMotion) badgeScale.snapTo(1f) else badgeScale.animateTo(1f, Motion.SpringBouncy)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd))),
        ) {
            if (!reducedMotion) {
                BurstOverlay(modifier = Modifier.fillMaxSize())
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Dimens.ScreenPaddingHorizontal),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .scale(badgeScale.value)
                        .clip(MaterialTheme.shapes.large)
                        .background(Accent)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.streak_milestone_badge, milestone),
                        style = MaterialTheme.typography.labelLarge,
                        color = OnAccent,
                    )
                }
                Text(
                    text = stringResource(R.string.streak_milestone_headline, streakDays),
                    style = MaterialTheme.typography.headlineLarge.copy(fontFamily = Anton, fontSize = 48.sp, lineHeight = 54.sp),
                    color = Accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Text(
                    text = stringResource(R.string.streak_milestone_subtitle, streakDays),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextBody,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(Accent)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(R.string.streak_milestone_cta), style = MaterialTheme.typography.titleMedium, color = OnAccent)
                }
            }
        }
    }
}

/** A single radial burst from screen-center, not a looping/continuous effect — same
 * one-shared-clock-drives-every-particle idiom as [com.fitviet.app.ui.workout
 * .SessionFinishedContent]'s `ConfettiOverlay`, but particles travel outward along a fixed angle
 * instead of falling, matching this screen's distinct "burst" signature. */
@Composable
private fun BurstOverlay(modifier: Modifier = Modifier) {
    val colors = listOf(Accent, MacroBarCarb, MacroBarFat, AccentBorder, OnAccent)
    val particles = remember {
        val random = Random(seed = 7)
        List(BURST_PARTICLE_COUNT) {
            BurstParticle(
                angleRadians = random.nextFloat() * (Math.PI.toFloat() * 2f),
                travelPx = 260f + random.nextFloat() * 260f,
                startDelayMs = random.nextFloat() * 150f,
                sizePx = 6f + random.nextFloat() * 6f,
                colorIndex = random.nextInt(colors.size),
            )
        }
    }

    var elapsedMs by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (elapsedMs < BURST_DURATION_MS) {
            val now = withFrameNanos { it }
            elapsedMs = (now - startNanos) / 1_000_000f
        }
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        particles.forEach { particle ->
            val localElapsed = elapsedMs - particle.startDelayMs
            if (localElapsed < 0f) return@forEach
            val progress = (localElapsed / (BURST_DURATION_MS - particle.startDelayMs)).coerceIn(0f, 1f)
            if (progress >= 1f) return@forEach

            // Eased outward travel (fast start, slow finish) reads as a genuine "burst", not a
            // constant-velocity drift.
            val eased = 1f - (1f - progress) * (1f - progress)
            val distance = particle.travelPx * eased
            val x = center.x + cos(particle.angleRadians) * distance
            val y = center.y + sin(particle.angleRadians) * distance
            val alpha = (1f - progress).coerceIn(0f, 1f)

            drawRect(
                color = colors[particle.colorIndex].copy(alpha = alpha),
                topLeft = Offset(x - particle.sizePx / 2f, y - particle.sizePx / 2f),
                size = Size(particle.sizePx, particle.sizePx),
            )
        }
    }
}
