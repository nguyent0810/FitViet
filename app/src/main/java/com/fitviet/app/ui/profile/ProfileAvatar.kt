package com.fitviet.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fitviet.app.ui.theme.HrColors

/**
 * FitViet has no photo-upload feature, so "choosing an avatar" (feature #1, Gate 35) means picking
 * a shape × colour combination from the app's own existing token palette — the 3 colours already
 * used for macro/stat bars (Accent/MacroCarb/MacroFat), not new hues, to stay visually consistent
 * with the rest of the app. Per the mockup: 3 tokens × filled/outline = 6 options, shape alternates
 * circle (even id) / 16dp rounded (odd id) independently of the colour/fill pairing. Persisted as
 * [com.fitviet.app.data.local.entity.SettingsEntity.avatarId], a stable `Int` index into [entries]
 * (never the enum itself — Room only ever stores primitives here).
 *
 * Redesign Gate 7a — swapped from the legacy green palette (`Accent`/`MacroBarCarb`/`MacroBarFat`)
 * to [HrColors]' lime one (`Accent`/`MacroCarb`/`MacroFat`), preserving the exact same "3 hues from
 * the app's own current palette" design intent rather than picking new, unrelated hues — a device
 * that already had an avatar chosen keeps the same *role* (e.g. "the carb-tint one"), just
 * repainted in the new palette, same as every other Hr-migrated screen's colors did.
 */
enum class AvatarStyle(val shape: Shape, val background: Color, val border: Color?, val content: Color) {
    ACCENT_FILLED(CircleShape, HrColors.Accent, null, HrColors.OnAccent),
    ACCENT_OUTLINE(RoundedCornerShape(16.dp), Color.Transparent, HrColors.Accent, HrColors.Accent),
    CARB_FILLED(CircleShape, HrColors.MacroCarb, null, HrColors.OnAccent),
    CARB_OUTLINE(RoundedCornerShape(16.dp), Color.Transparent, HrColors.MacroCarb, HrColors.MacroCarb),
    FAT_FILLED(CircleShape, HrColors.MacroFat, null, HrColors.OnAccent),
    FAT_OUTLINE(RoundedCornerShape(16.dp), Color.Transparent, HrColors.MacroFat, HrColors.MacroFat),
    ;

    companion object {
        val Default = ACCENT_FILLED

        /** Never throws on a stale/out-of-range id (e.g. a future downgrade) — falls back to
         * [Default] instead. */
        fun fromId(id: Int): AvatarStyle = entries.getOrNull(id) ?: Default
    }
}

/** The single letter shown inside a [MonogramAvatar] for a given display name. */
fun avatarInitial(displayName: String): String = displayName.trim().firstOrNull()?.uppercase() ?: "?"

@Composable
fun MonogramAvatar(
    initial: String,
    avatarId: Int,
    size: Dp,
    style: TextStyle,
    modifier: Modifier = Modifier,
    borderWidth: Dp = 0.dp,
) {
    val avatarStyle = AvatarStyle.fromId(avatarId)
    Box(
        modifier = modifier
            .size(size)
            .clip(avatarStyle.shape)
            .background(avatarStyle.background)
            // The style's own outline border (for *_OUTLINE variants) draws first; the caller's
            // [borderWidth] selection ring (grid picker's "this one is chosen" indicator) is a
            // separate, wider stroke that overrides it when both apply — a selected outline swatch
            // should read as "selected," not blend its own thin border into the ring.
            .let { if (avatarStyle.border != null && borderWidth == 0.dp) it.border(1.5.dp, avatarStyle.border, avatarStyle.shape) else it }
            .let { if (borderWidth > 0.dp) it.border(borderWidth, HrColors.Accent, avatarStyle.shape) else it },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initial, style = style, color = avatarStyle.content)
    }
}
