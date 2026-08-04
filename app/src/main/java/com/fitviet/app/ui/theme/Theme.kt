package com.fitviet.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// v1 ships a single dark theme per design tokens — no light variant yet.
private val FitVietColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = AccentSurfaceSelected,
    onPrimaryContainer = TextPrimary,
    secondary = Accent,
    onSecondary = OnAccent,
    background = BackgroundPage,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = DeepSurface1,
    onSurfaceVariant = TextMuted,
    outline = CardBorder,
    outlineVariant = DotBorderIdle,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun FitVietTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FitVietColorScheme,
        typography = FitVietTypography,
        shapes = FitVietShapes,
        content = content,
    )
}
