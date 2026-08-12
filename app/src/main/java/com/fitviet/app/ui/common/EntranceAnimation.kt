package com.fitviet.app.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Premium-pass entrance animation (Gate D1) — a brief fade + rise-in for hero-style surfaces the
 * first time they compose, the third named primitive alongside [pressScale] and
 * [com.fitviet.app.ui.theme.premiumShadow]. Applied SELECTIVELY to hero/PR/donate-style surfaces
 * per those two primitives' own established scope (Gate A2's doc comment: "applied only to...
 * nothing else") — not to every element on a screen. No-ops under [rememberReducedMotion], same
 * as every other purely decorative animation in this pass.
 */
@Composable
fun Modifier.entranceFade(riseBy: Dp = 12.dp): Modifier {
    val reducedMotion = rememberReducedMotion()
    if (reducedMotion) return this

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 360))
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * riseBy.toPx()
    }
}
