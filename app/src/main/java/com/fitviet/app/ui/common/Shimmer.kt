package com.fitviet.app.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.SurfaceCard

/**
 * Premium-pass skeleton shimmer (Gate D2) — a moving diagonal highlight sweep over a neutral
 * placeholder shape, replacing bare [androidx.compose.material3.CircularProgressIndicator]
 * full-screen loading states with a skeleton that hints at the content's real shape while it
 * loads. No-ops (a static two-tone fill, no sweep) under [rememberReducedMotion].
 */
@Composable
fun Modifier.shimmer(): Modifier {
    val reducedMotion = rememberReducedMotion()
    if (reducedMotion) return this.drawWithCache { onDrawBehind { drawRect(SurfaceCard) } }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateFraction by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1200, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "shimmerTranslate",
    )
    return this.drawWithCache {
        val sweepWidth = size.width * 0.6f
        val startX = size.width * translateFraction - sweepWidth
        val brush = Brush.linearGradient(
            colors = listOf(SurfaceCard, CardBorder, SurfaceCard),
            start = Offset(startX, 0f),
            end = Offset(startX + sweepWidth, size.height),
        )
        onDrawBehind { drawRect(brush) }
    }
}

/** One shimmering placeholder rectangle — the building block [LoadingSkeleton] composes into a
 * generic full-screen skeleton. */
@Composable
fun ShimmerBlock(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(8.dp)) {
    Box(modifier = modifier.clip(shape).shimmer())
}

/**
 * A generic full-screen loading skeleton (image-shaped block + a few varying-width text-line
 * blocks) — deliberately generic rather than per-screen-exact, since the goal is "this is
 * loading, here's roughly what's coming" rather than a pixel-perfect preview of final content.
 * Drop-in replacement for `Box(fillMaxSize, center) { CircularProgressIndicator(...) }` at the
 * three full-screen loading sites this gate rolls it out to.
 */
@Composable
fun LoadingSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ShimmerBlock(modifier = Modifier.fillMaxWidth().height(160.dp), shape = RoundedCornerShape(18.dp))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.7f).height(20.dp))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp))
        ShimmerBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.85f).height(14.dp))
    }
}
