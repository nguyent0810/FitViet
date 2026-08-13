package com.fitviet.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val FitVietShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val PillShape = RoundedCornerShape(percent = 50)

/** "Hit & Run" redesign (Gate 1a) — new radii scale, added alongside [FitVietShapes]/[PillShape]
 * above rather than replacing them (same additive convention as [HrColors]/[HrDisplay]/[HrBody]).
 * README's own radii: hero/large card 24 · regular card 16-18 · small card/row 14 · CTA button 16
 * · small button 12 · pill 99 (practically identical to the existing [PillShape]'s 50%-based pill
 * for any real button height, so reused as-is rather than duplicated). */
object HrShapes {
    val CardHero = RoundedCornerShape(24.dp)
    val CardRegular = RoundedCornerShape(17.dp)
    val CardSmall = RoundedCornerShape(14.dp)
    val ButtonCta = RoundedCornerShape(16.dp)
    val ButtonSmall = RoundedCornerShape(12.dp)
}
