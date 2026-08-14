package com.fitviet.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.PillShape

/**
 * Shared pill-row selector (Gate 43) — this exact selected/idle pill visual had already been
 * hand-copied twice (Profile's weight-history range row, Programs' filter chips) before this
 * gate needed a third; extracted here rather than adding a fourth copy for the Dashboard's
 * time-range row. Programs' filter chips stay their own `FlowRow`-based copy (a genuinely
 * different layout need — a variable-length, wrapping option list — not just a visual variant),
 * but Profile's weight-history row was migrated onto this since it's a straight structural match.
 *
 * [options] pairs each selectable value with its `@StringRes` label id.
 */
@Composable
fun <T> RangePills(
    options: List<Pair<T, Int>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, labelRes) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .heightIn(min = Dimens.MinTouchTarget)
                    .clip(PillShape)
                    .background(if (isSelected) HrColors.Accent else HrColors.Surface)
                    .border(1.dp, if (isSelected) HrColors.Accent else HrColors.Border, PillShape)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(labelRes),
                    fontFamily = HrBody,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) HrColors.OnAccent else HrColors.TextMid,
                )
            }
        }
    }
}
