package com.fitviet.app.ui.common

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.drawWithContent
import com.fitviet.app.ui.theme.Motion
import kotlinx.coroutines.launch

/**
 * Premium-pass Moment 1 (Gate A2, direction "1a Aurora Depth") — drag-to-tilt perspective card
 * with a sheen overlay that sweeps as the card rotates. Applied only to the dashboard hero card,
 * PR card(s), and program list cards per the Premium Moments build prompt; nothing else.
 *
 * No-ops (returns [this] unmodified) under [rememberReducedMotion] so a user with system
 * animations disabled never sees the drag/tilt/sheen effect.
 */
@Composable
fun Modifier.tiltOnDrag(maxDegrees: Float = 8f): Modifier {
    val reducedMotion = rememberReducedMotion()
    if (reducedMotion) return this

    val scope = rememberCoroutineScope()
    val rx = remember { Animatable(0f) }
    val ry = remember { Animatable(0f) }
    val sheenAlpha = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current

    return this
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scope.launch { sheenAlpha.animateTo(0.9f, Motion.SpringGentle) }
                },
                onDragEnd = {
                    scope.launch { rx.animateTo(0f, Motion.SpringGentle) }
                    scope.launch { ry.animateTo(0f, Motion.SpringGentle) }
                    scope.launch { sheenAlpha.animateTo(0f, Motion.SpringGentle) }
                },
                onDragCancel = {
                    scope.launch { rx.animateTo(0f, Motion.SpringGentle) }
                    scope.launch { ry.animateTo(0f, Motion.SpringGentle) }
                    scope.launch { sheenAlpha.animateTo(0f, Motion.SpringGentle) }
                },
            ) { change, _ ->
                change.consume()
                val nx = ((change.position.x / size.width) * 2f - 1f).coerceIn(-1f, 1f)
                val ny = ((change.position.y / size.height) * 2f - 1f).coerceIn(-1f, 1f)
                scope.launch { ry.snapTo(nx * maxDegrees) }
                scope.launch { rx.snapTo(-ny * maxDegrees) }
            }
        }
        .graphicsLayer {
            rotationX = rx.value
            rotationY = ry.value
            cameraDistance = 12f * density
        }
        .drawWithContent {
            drawContent()
            if (sheenAlpha.value > 0.01f) {
                val shiftPx = (ry.value / maxDegrees) * size.width * 0.3f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.14f * sheenAlpha.value),
                            Color.Transparent,
                        ),
                        start = Offset(shiftPx - size.width * 0.3f, 0f),
                        end = Offset(shiftPx + size.width * 0.6f, size.height),
                    ),
                )
            }
        }
}
