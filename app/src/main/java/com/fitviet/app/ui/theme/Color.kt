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

/** "Hit & Run" redesign (UI Handoff/Hit and Run app redesign) — new lime-on-near-black palette,
 * added alongside the existing green palette above rather than replacing it (Gate 1a). Each
 * screen swaps from the old tokens to these in its own redesign gate; the old tokens are only
 * deleted once nothing references them (final nav-consolidation gate). [Danger]/[DangerBorder]
 * above already match this palette's own danger spec (#E5484D/#4A2A2C) exactly, so there's no
 * `HrColors.Danger` — reuse the existing one. */
object HrColors {
    val Bg = Color(0xFF0C0E08)
    val BgDeep = Color(0xFF0A0C06)
    val Surface = Color(0xFF12150C)
    val SurfaceInput = Color(0xFF0F120A)
    val SurfaceAccent = Color(0xFF161B08)

    // Today card / hero card gradient — 150deg linear, matches [HeroGradientStart]/[HeroGradientEnd]'s
    // existing "store the two endpoints, build the Brush at the call site" convention.
    val GradientCardStart = Color(0xFF1B2408)
    val GradientCardEnd = Color(0xFF10140A)

    val Border = Color(0xFF202616)
    val BorderSoft = Color(0xFF2A3020)
    val BorderAccentDim = Color(0xFF4A5A28)
    val BorderGradient = Color(0xFF2E3813)

    val Accent = Color(0xFFB7F542)
    val AccentHover = Color(0xFFCDFF6B)
    val OnAccent = Color(0xFF141A05)

    val BtnCircle = Color(0xFF1B2110)
    val BtnCircleHover = Color(0xFF242C14)
    val BarDim = Color(0xFF242C14)

    val TextHi = Color(0xFFF1F4E8)
    val TextMid = Color(0xFFC6CCB8)
    val TextLow = Color(0xFF8F977F)
    val TextFaint = Color(0xFF5F6650)

    // Nutrition macro bars (Gate 5a) — the ONE other deliberate exception to this palette's
    // single-accent-hue rule (see [Danger] above for the first): the design doc gives these two
    // literal hex values for carb/fat specifically, progressively lighter tints of the same lime
    // rather than a real second hue, so they're real tokens here rather than [Accent]/[AccentHover]
    // reuse — unlike e.g. `RestContent.kt`'s ring, which had no such doc-specified second color and
    // stood in with [AccentHover]. Protein reuses [Accent] itself — the doc's own value for it
    // (#B7F542) is byte-for-byte [Accent], so no separate token.
    val MacroCarb = Color(0xFFD3F58E)
    val MacroFat = Color(0xFFEDFBCF)
}
