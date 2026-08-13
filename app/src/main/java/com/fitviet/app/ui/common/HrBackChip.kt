package com.fitviet.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrShapes

/**
 * Redesign Gate 3d — the small "‹" back affordance every Hr-token drill-in screen needs, extracted
 * from what used to be a private copy inside [com.fitviet.app.ui.monthlyplan.MonthlyPlanDetailScreen]
 * once Gate 3d's own drill-in screens (Handbook, Handbook muscle group, Exercise detail) needed the
 * same thing a second/third/fourth time (4 call sites total after this extraction).
 *
 * The visual chip stays 34dp (the pre-existing look every one of these screens already had), but
 * the actual tap target is 44dp ([com.fitviet.app.ui.theme.Dimens.MinTouchTarget]), via an outer
 * clickable box the visual chip sits inside. A same-sized 34dp *legacy*-token back chip elsewhere
 * in this codebase was already flagged and bumped to 44dp for the same reason (see PROGRESS.md's
 * own record of that fix) — this extraction fixes it correctly the first time here rather than
 * repeating that defect across 4 new call sites at once.
 *
 * [Alignment.CenterStart], not [Alignment.Center] — every call site's own left edge is already
 * flush with the screen's [com.fitviet.app.ui.theme.HrDimens.ScreenPaddingHorizontal] (matching
 * every card/text below it on the same screen), so growing the tap target only on the trailing
 * side keeps that shared left margin intact instead of shifting the visible chip 5dp off it. The
 * extra room lands between the chip and the row's next child, which is exactly where the pre-Gate-
 * 3d 34dp chip's own `spacedBy(12.dp)` already expected slack to be absorbed — so call sites keep
 * that same plain `spacedBy(12.dp)`, no per-call-site compensation needed.
 */
@Composable
fun HrBackChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Dimens.MinTouchTarget)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(HrShapes.CardSmall)
                .background(HrColors.Surface)
                .border(1.dp, HrColors.Border, HrShapes.CardSmall),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "‹", fontFamily = HrBody, fontSize = 18.sp, color = HrColors.TextLow)
        }
    }
}
