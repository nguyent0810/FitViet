package com.fitviet.app.ui.theme

import androidx.compose.ui.unit.dp

object Dimens {
    val ScreenPaddingHorizontal = 24.dp

    val CardPaddingSmall = 14.dp
    val CardPaddingLarge = 20.dp

    val ListGapSmall = 8.dp
    val ListGapLarge = 12.dp

    val SectionGapSmall = 14.dp
    val SectionGapLarge = 18.dp

    val MinTouchTarget = 44.dp
    val FabSize = 46.dp

    val SelectedBorderWidth = 1.5.dp
    val IdleBorderWidth = 1.dp
}

/** "Hit & Run" redesign (Gate 1a) — spacing additions, alongside [Dimens] above rather than
 * replacing it. Screen padding is a real, deliberate deviation from [Dimens.ScreenPaddingHorizontal]
 * (24dp) — the mock specifies 22dp — so it's its own token rather than overloading the old one;
 * [Dimens.MinTouchTarget] is reused as-is since the mock's own "hit target tối thiểu 44dp" matches
 * exactly. */
object HrDimens {
    val ScreenPaddingHorizontal = 22.dp
    val CardGapVertical = 15.dp
    val CtaHeight = 56.dp
    val CtaPaddingVertical = 17.dp
}
