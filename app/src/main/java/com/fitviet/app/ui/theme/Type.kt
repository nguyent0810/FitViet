package com.fitviet.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
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
