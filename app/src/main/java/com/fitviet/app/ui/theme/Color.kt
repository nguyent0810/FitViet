package com.fitviet.app.ui.theme

import androidx.compose.ui.graphics.Color

// Backgrounds
val BackgroundPage = Color(0xFF0D100E)
val AppCanvas = Color(0xFF0A0C0B)
val SurfaceCard = Color(0xFF151A17)
val CardBorder = Color(0xFF232A25)
val DeepSurface1 = Color(0xFF0F1310)
val DeepSurface2 = Color(0xFF1B211D)

// Accent
val Accent = Color(0xFF52E077)
val OnAccent = Color(0xFF06170B)
val AccentHover = Color(0xFF6BEA8E)
val AccentSurfaceSelected = Color(0xFF132217)
val AccentBorder = Color(0xFF2E5A3C)
val AccentBorderAlt = Color(0xFF234D31)

// Hero gradient
val HeroGradientStart = Color(0xFF14291B)
val HeroGradientEnd = Color(0xFF101B14)

// Text
val TextPrimary = Color(0xFFEDF2EE)
val TextBody = Color(0xFFC4CEC7)
val TextMuted = Color(0xFF93A097)
val TextFaint = Color(0xFF7D8A80)
val TextFaintAlt = Color(0xFF5C685F)

// Charts
val ChartBarIdle = Color(0xFF243B2C)
val MacroBarProtein = Color(0xFF52E077)
val MacroBarCarb = Color(0xFF8FD9A3)
val MacroBarFat = Color(0xFFC9EDD3)

// Selection dot (unselected)
val DotBorderIdle = Color(0xFF3A443C)

// Destructive actions (Gate 37) — a muted red, the one deliberate exception to this app's
// single-accent-green palette: a destructive/danger signal is expected, established Android
// vocabulary (system dialogs, Material guidelines), not a decorative color choice like an avatar
// swatch would be. Values match the mockup's own proposed literals exactly (#E5484D/#4A2A2C) —
// Gate 37 originally shipped a close-but-not-exact approximation.
val Danger = Color(0xFFE5484D)
val DangerSurfaceSelected = Color(0xFF2A1613)
val DangerBorder = Color(0xFF4A2A2C)
