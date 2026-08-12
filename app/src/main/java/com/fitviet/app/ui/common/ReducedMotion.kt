package com.fitviet.app.ui.common

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private fun readAnimatorDurationScale(resolver: android.content.ContentResolver): Boolean {
    val scale = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    return scale == 0f
}

/**
 * Premium-pass reduced-motion gate (Gate A1) — every purely decorative animation the Premium
 * Moments build prompt introduces (confetti, sheen, breathing glow, shimmer) must check this
 * before running, per the brief's "respect Settings > Remove animations" rule. Short spring
 * transitions on real state changes (press, selection) are allowed to stay regardless — this
 * only gates decoration with no functional purpose.
 *
 * Live-observes the setting via a [ContentObserver] rather than reading it once — a user can
 * toggle "Remove animations" in system Developer Options while this composition is still alive
 * (e.g. switching apps via the settings shortcut and back), and a stale one-time snapshot would
 * leave decorative animation gated on the value from when the screen first composed.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    var reducedMotion by remember(context) { mutableStateOf(readAnimatorDurationScale(context.contentResolver)) }

    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reducedMotion = readAnimatorDurationScale(context.contentResolver)
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }

    return reducedMotion
}
