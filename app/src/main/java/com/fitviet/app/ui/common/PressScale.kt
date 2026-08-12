package com.fitviet.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.fitviet.app.ui.theme.Motion

/**
 * Premium-pass press feedback (Gate A2/D1) — drop-in replacement for a plain
 * `Modifier.clickable(onClick = X)` that adds a 0.97 press-scale with a snappy spring release.
 * Owns the click handling itself (via its own [MutableInteractionSource]) rather than layering a
 * second gesture detector alongside an existing `.clickable`, so it composes safely as a direct
 * swap at any call site.
 */
@Composable
fun Modifier.pressScale(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = Motion.SpringSnappy,
        label = "pressScale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
}
