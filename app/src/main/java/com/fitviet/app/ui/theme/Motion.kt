package com.fitviet.app.ui.theme

import androidx.compose.animation.core.spring

/**
 * Premium-pass motion tokens (Gate A1) — three named springs shared by every state-transition
 * animation the Premium Moments build prompt introduces (selection, press-scale, tilt release,
 * ring updates, celebration overshoot). Centralized here so every screen tunes together rather
 * than hand-picking damping/stiffness per call site.
 */
object Motion {
    /** State transitions: selection, button press-scale, set-row change. */
    val SpringSnappy = spring<Float>(dampingRatio = 0.55f, stiffness = 400f)

    /** Card tilt release, ring value updates. */
    val SpringGentle = spring<Float>(dampingRatio = 0.75f, stiffness = 200f)

    /** Celebration overshoot: count-up tiles popping in. */
    val SpringBouncy = spring<Float>(dampingRatio = 0.45f, stiffness = 180f)
}
