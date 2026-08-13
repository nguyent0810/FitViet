@file:OptIn(ExperimentalTextApi::class)

package com.fitviet.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.fitviet.app.R

// Be Vietnam Pro: general UI text, full Vietnamese glyph support.
val BeVietnamPro = FontFamily(
    Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
    Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
    Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
    Font(R.font.be_vietnam_pro_bold, FontWeight.Bold),
    Font(R.font.be_vietnam_pro_extrabold, FontWeight.ExtraBold),
)

// Anton: display numerals — timers, PR values, big headers.
val Anton = FontFamily(
    Font(R.font.anton_regular, FontWeight.Normal),
)

// Display style for Anton numerals; size is set per-usage (26–96sp range in spec).
val DisplayNumeral = TextStyle(fontFamily = Anton, fontWeight = FontWeight.Normal)

val FitVietTypography = Typography(
    displayLarge = TextStyle(fontFamily = BeVietnamPro, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineLarge = TextStyle(fontFamily = BeVietnamPro, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 31.sp),
    headlineMedium = TextStyle(fontFamily = BeVietnamPro, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontFamily = BeVietnamPro, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = BeVietnamPro, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = BeVietnamPro, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = BeVietnamPro, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = BeVietnamPro, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = BeVietnamPro, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = BeVietnamPro, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = BeVietnamPro, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = BeVietnamPro, fontWeight = FontWeight.Medium, fontSize = 9.sp, lineHeight = 12.sp),
)

/** "Hit & Run" redesign (Gate 1a) — new type families, added alongside [BeVietnamPro]/[Anton]/
 * [FitVietTypography] above rather than replacing them; each screen swaps to these in its own
 * redesign gate, old families are deleted only once nothing references them.
 *
 * Both are bundled as their upstream *variable* fonts (Google Fonts' `google/fonts` repo no
 * longer distributes static instances for either family) rather than a single pinned static
 * weight, using [FontVariation.Settings] — supported since Compose UI 1.7 (this project is on
 * 1.7.6 via the 2024.12.01 BOM, minSdk 26), so no external font-instancing tooling is needed.
 */

/** Bricolage Grotesque — display: screen titles, session names, big stat numbers, primary CTA
 * text. README's spec calls for a single weight (800/ExtraBold); the variable font's `opsz`
 * (optical size) axis is pinned at 36 as one representative instance suited to the type scale's
 * mid-large sizes (headlines/titles) — very large display numbers (~96-110sp) and small labels
 * using this family won't get per-size optical correction, a deliberate simplification given the
 * axis can't vary per call site within one [FontFamily] entry. */
val HrDisplay = FontFamily(
    Font(
        R.font.bricolage_grotesque_variable,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800), FontVariation.opticalSizing(36.sp)),
    ),
)

/** Archivo — body: everything else. Weights 400/600/700 per the redesign's type table, each its
 * own [Font] entry against the same variable file so [FontWeight] resolution picks the right
 * instance per `TextStyle`. */
val HrBody = FontFamily(
    Font(R.font.archivo_variable, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.archivo_variable, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.archivo_variable, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)
