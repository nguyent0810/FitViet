package com.fitviet.app.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fitviet.app.R
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentBorder
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.ChartBarIdle
import com.fitviet.app.ui.theme.DeepSurface1
import com.fitviet.app.ui.theme.FitVietTheme
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary

/**
 * Why a feature is locked. FitViet has no paid tier today — [REQUIRES_UPGRADE] exists as a
 * reusable primitive for if/when one does. Deliberately not wired to the Donate flow anywhere in
 * this app (see PROGRESS.md, Gate 36): pointing a locked item there would conflate voluntary
 * support with a product entitlement neither this enum nor the rest of the app actually implements.
 */
enum class LockReason(@StringRes val labelRes: Int) {
    NOT_YET_UNLOCKED(R.string.locked_item_not_yet_unlocked),
    REQUIRES_UPGRADE(R.string.locked_item_requires_upgrade),
}

/**
 * A row for a feature that isn't available yet. Ships with no caller as of Gate 36 — a reusable
 * primitive for a future locked feature, not wired to any real gate or to Donate. Both [LockReason]
 * variants are verified together in [LockedListItemPreview] since there's no real call site yet to
 * exercise them from.
 *
 * The two reasons read as genuinely different states, not a shared "disabled" look with different
 * text: [LockReason.NOT_YET_UNLOCKED] is inert (dashed border, muted waiting-ring, no chevron,
 * never clickable even if [onClick] is supplied); [LockReason.REQUIRES_UPGRADE] reads as an
 * actionable upsell (accent-tinted card, diamond icon, trailing chevron, clickable). [reasonText]
 * has no default so a bare padlock row with no explanation is impossible to construct.
 */
@Composable
fun LockedListItem(
    title: String,
    reason: LockReason,
    reasonText: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    when (reason) {
        LockReason.NOT_YET_UNLOCKED -> Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(DeepSurface1)
                .dashedBorder(width = 1.dp, color = CardBorder, shape = MaterialTheme.shapes.medium)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WaitingRing(modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                Text(
                    text = reasonText,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextFaint,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        LockReason.REQUIRES_UPGRADE -> Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(AccentSurfaceSelected)
                .border(1.5.dp, AccentBorder, MaterialTheme.shapes.medium)
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .rotate(45f)
                    .background(Accent, MaterialTheme.shapes.extraSmall),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text(
                    text = reasonText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(text = "›", style = MaterialTheme.typography.titleMedium, color = TextMuted)
        }
    }
}

/** The muted "waiting" indicator for [LockReason.NOT_YET_UNLOCKED] — a static 90° accent-muted
 * sweep over an idle track, echoing this app's other progress-ring drawing (nutrition's kcal ring)
 * without implying real progress (it never animates or updates). */
@Composable
private fun WaitingRing(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 2.dp.toPx())
        drawArc(color = ChartBarIdle, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = stroke)
        drawArc(color = TextMuted, startAngle = -90f, sweepAngle = 90f, useCenter = false, style = stroke)
    }
}

/** Compose has no built-in dashed [Modifier.border] — draws one directly via a dashed
 * [PathEffect]. Scoped to this file (private, not shared) since [LockedListItem] is itself
 * unused-by-design (Gate 36's own doc) — `ui/reminders/RemindersScreen.kt`'s own "+ Thêm giờ nhắc"
 * add-row (Gate 7b) needed the same dashed-stroke idiom for a screen that's actually live, and
 * copied this rather than extracting a shared utility out of a component nothing wires up yet. */
private fun Modifier.dashedBorder(width: Dp, color: Color, shape: Shape): Modifier = drawWithContent {
    drawContent()
    val outline = shape.createOutline(size, layoutDirection, this)
    drawOutline(
        outline = outline,
        color = color,
        style = Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()), 0f),
        ),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0D100E)
@Composable
private fun LockedListItemPreview() {
    FitVietTheme {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(16.dp)) {
            LockedListItem(
                title = "Đồng bộ nhiều thiết bị",
                reason = LockReason.NOT_YET_UNLOCKED,
                reasonText = "Tính năng sắp ra mắt",
            )
            LockedListItem(
                title = "Giáo án nâng cao",
                reason = LockReason.REQUIRES_UPGRADE,
                reasonText = "Mở khoá với gói nâng cấp",
                onClick = {},
            )
        }
    }
}
