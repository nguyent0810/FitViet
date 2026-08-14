package com.fitviet.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// v1 ships a single dark theme per design tokens — no light variant yet.
//
// Phase 8 checkpoint (Gates 8a-8e): every live screen in the app now styles itself explicitly via
// HrColors/HrShapes/HrDisplay/HrBody rather than reading MaterialTheme.colorScheme/.shapes/.typography
// — confirmed by an app-wide sweep finding zero remaining call sites outside `ui/common/LockedListItem.kt`
// and `ui/onboarding/OnboardingComponents.kt` (both confirmed dead code, no live call sites). Despite
// that, FitVietTheme below is NOT dead code and must not be removed or simplified: it wraps the whole
// app (see MainActivity) in a real MaterialTheme, which every *unstyled* Material3 component — AlertDialog
// and ModalBottomSheet's own default container color, Text's default LocalTextStyle, etc. — still reads
// from ambiently, with no explicit call site to grep for. So the legacy Color.kt/Shape.kt/Type.kt tokens
// this scheme is built from stay defined permanently, not as leftover cleanup debt.
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
