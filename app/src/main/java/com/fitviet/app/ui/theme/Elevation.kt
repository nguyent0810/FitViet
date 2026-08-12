package com.fitviet.app.ui.theme

import android.graphics.BlurMaskFilter
import android.graphics.Paint as FrameworkPaint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ShadowAmbient = Color(0xB3000000)
private val ShadowAccentBloom = Color(0x1F52E077)

/**
 * Premium-pass multi-layer shadow (Gate A1) — replaces Material's flat elevation with two
 * stacked soft-blurred layers (ambient black + an optional faint accent bloom for hero/PR/donate
 * surfaces), matching the Premium Moments build prompt exactly. Draws behind the composable via
 * a native [BlurMaskFilter] rather than Material elevation, so it composes with any background.
 *
 * [radius] is the card's own corner radius (the shadow follows the card's rounded shape, it is
 * not the blur amount).
 *
 * Known caveat (flagged by review): [BlurMaskFilter] is documented as unsupported on a
 * View-hierarchy hardware layer (`LAYER_TYPE_HARDWARE`); Compose's own draw calls are Skia
 * software-rasterized per primitive and render this correctly in practice (the same idiom used
 * by several published Compose shadow libraries), but this must still be confirmed visually on a
 * real device the first time a call site actually uses this modifier (Gate A2's on-device smoke
 * check) before relying on it for the app-wide elevation rollout in Part D.
 */
fun Modifier.premiumShadow(radius: Dp, accentBloom: Boolean = false): Modifier = drawWithCache {
    val cornerRadiusPx = radius.toPx()
    val bloomOffsetPx = 8.dp.toPx()
    val ambientOffsetPx = 16.dp.toPx()

    val bloomPaint = if (accentBloom) {
        FrameworkPaint().apply {
            isAntiAlias = true
            color = ShadowAccentBloom.toArgb()
            maskFilter = BlurMaskFilter(48.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
        }
    } else {
        null
    }
    val ambientPaint = FrameworkPaint().apply {
        isAntiAlias = true
        color = ShadowAmbient.toArgb()
        maskFilter = BlurMaskFilter(24.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
    }

    onDrawBehind {
        drawIntoCanvas { canvas ->
            if (bloomPaint != null) {
                canvas.nativeCanvas.drawRoundRect(
                    0f,
                    bloomOffsetPx,
                    size.width,
                    size.height + bloomOffsetPx,
                    cornerRadiusPx,
                    cornerRadiusPx,
                    bloomPaint,
                )
            }
            canvas.nativeCanvas.drawRoundRect(
                0f,
                ambientOffsetPx,
                size.width,
                size.height + ambientOffsetPx,
                cornerRadiusPx,
                cornerRadiusPx,
                ambientPaint,
            )
        }
    }
}
