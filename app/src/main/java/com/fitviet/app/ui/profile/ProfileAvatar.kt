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
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.DeepSurface1
import com.fitviet.app.ui.theme.DeepSurface2

/**
 * FitViet has no photo-upload feature, so "choosing an avatar" (feature #1, Gate 35) means picking
 * a shape × background-tint combination from the app's own existing dark palette — deliberately not
 * introducing new hues, to stay visually consistent with the rest of the app. Persisted as
 * [com.fitviet.app.data.local.entity.SettingsEntity.avatarId], a stable `Int` index into [entries]
 * (never the enum itself — Room only ever stores primitives here).
 */
enum class AvatarStyle(val shape: Shape, val background: Color) {
    CIRCLE_NEUTRAL(CircleShape, DeepSurface2),
    CIRCLE_ACCENT(CircleShape, AccentSurfaceSelected),
    CIRCLE_DEEP(CircleShape, DeepSurface1),
    SQUARE_NEUTRAL(RoundedCornerShape(14.dp), DeepSurface2),
    SQUARE_ACCENT(RoundedCornerShape(14.dp), AccentSurfaceSelected),
    SQUARE_DEEP(RoundedCornerShape(14.dp), DeepSurface1),
    ;

    companion object {
        val Default = CIRCLE_NEUTRAL

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
            .let { if (borderWidth > 0.dp) it.border(borderWidth, Accent, avatarStyle.shape) else it },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initial, style = style, color = Accent)
    }
}
