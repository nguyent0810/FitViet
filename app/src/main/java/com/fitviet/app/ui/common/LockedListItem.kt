package com.fitviet.app.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fitviet.app.R
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.FitVietTheme
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted

/**
 * Why a feature is locked. FitViet has no paid tier today — [REQUIRES_UPGRADE] exists as a
 * reusable primitive for if/when one does. Deliberately not wired to the Donate flow anywhere in
 * this app (see PROGRESS.md, Gate 36): pointing a locked item there would conflate voluntary
 * support with a product entitlement neither this enum nor the rest of the app actually implements.
 */
enum class LockReason(@StringRes val labelRes: Int) {
    REQUIRES_UPGRADE(R.string.locked_item_requires_upgrade),
    COMING_SOON(R.string.locked_item_coming_soon),
}

/**
 * A disabled-looking list row for a feature that isn't available yet. Ships with no caller as of
 * Gate 36 — a reusable primitive for a future locked feature, not wired to any real gate or to
 * Donate. Both [LockReason] variants are verified together in [LockedListItemPreview] since
 * there's no real call site yet to exercise them from.
 */
@Composable
fun LockedListItem(title: String, subtitle: String, reason: LockReason, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = "🔒 $title", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextFaint,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier = Modifier
                .clip(PillShape)
                .border(1.dp, CardBorder, PillShape)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(text = stringResource(reason.labelRes), style = MaterialTheme.typography.labelSmall, color = TextFaint)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D100E)
@Composable
private fun LockedListItemPreview() {
    FitVietTheme {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(16.dp)) {
            LockedListItem(
                title = "Giáo án nâng cao",
                subtitle = "Mở khoá với gói nâng cấp",
                reason = LockReason.REQUIRES_UPGRADE,
            )
            LockedListItem(
                title = "Đồng bộ nhiều thiết bị",
                subtitle = "Tính năng sắp ra mắt",
                reason = LockReason.COMING_SOON,
            )
        }
    }
}
