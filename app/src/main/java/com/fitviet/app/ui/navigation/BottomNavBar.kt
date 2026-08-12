package com.fitviet.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitviet.app.R
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.DeepSurface1
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.TextMuted

private data class NavItem(val destination: FitVietDestination, val labelRes: Int)

private val NAV_ITEMS_LEFT = listOf(
    NavItem(FitVietDestination.Home, R.string.nav_home),
    NavItem(FitVietDestination.Programs, R.string.nav_programs),
)
private val NAV_ITEMS_RIGHT = listOf(
    NavItem(FitVietDestination.Nutrition, R.string.nav_nutrition),
    NavItem(FitVietDestination.Community, R.string.nav_community),
    NavItem(FitVietDestination.Handbook, R.string.nav_handbook),
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (FitVietDestination) -> Unit,
    onFabClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().background(DeepSurface1)) {
        // Prototype shows a 1px top divider only, not a full border.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CardBorder),
        )
        // Gate 25 added a 5th regular item (Handbook), so the two sides no longer have equal
        // counts (2 left, 3 right) — an odd total that provably can't satisfy both "every item is
        // the same width" AND "the FAB sits at the bar's exact mathematical center" at once (2
        // items and 3 items can only occupy equal-width halves if their own per-item widths
        // differ, and can only have equal per-item widths if the two halves' total widths differ).
        // A prior attempt tried decoupling the FAB into a `Box`-overlay pinned to `Alignment
        // .Center`, independent of a `Spacer` left in the item `Row` — review confirmed the FAB
        // itself does land at the true center that way, but the `Row`'s own internal gap does NOT
        // (same ~30dp asymmetry as ever, just moved from "the FAB" to "the gap"), so the
        // true-centered FAB ends up physically overlapping ~16-18dp of the 3rd nav item's touch
        // target — since the FAB is composed last (topmost hit-test layer) and is clickable, THAT
        // failure mode can silently steal taps meant for a real nav destination. A cosmetically
        // off-center FAB is a minor visual nitpick; a nav item that intermittently swallows taps is
        // a real usability regression — so this reverts to weighting the two side-`Row`s by their
        // own item count (2f left / 3f right), which review already confirmed gives every item a
        // genuinely equal share of the bar's width with NO overlap risk (the FAB is a plain,
        // non-overlapping sibling sitting in the natural gap between the two `Row`s, not a floating
        // overlay). The tradeoff: the FAB itself sits ~25-35dp left of the bar's exact mathematical
        // center. That asymmetry is real and intentionally accepted, not an oversight — there is no
        // layout that avoids it without either changing the FAB's visual design (e.g. a fully
        // above-the-bar docked FAB with no embedded overlap at all) or reducing the item count back
        // to an even split, neither of which is this gate's scope.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(modifier = Modifier.weight(NAV_ITEMS_LEFT.size.toFloat()), horizontalArrangement = Arrangement.SpaceAround) {
                NAV_ITEMS_LEFT.forEach { item ->
                    NavItemView(item = item, selected = item.destination.route == currentRoute, onClick = { onNavigate(item.destination) })
                }
            }
            WorkoutFab(onClick = onFabClick)
            Row(modifier = Modifier.weight(NAV_ITEMS_RIGHT.size.toFloat()), horizontalArrangement = Arrangement.SpaceAround) {
                NAV_ITEMS_RIGHT.forEach { item ->
                    NavItemView(item = item, selected = item.destination.route == currentRoute, onClick = { onNavigate(item.destination) })
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavItemView(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            // Gate 25's Handbook tab made the right side hold 3 items instead of 2 — weight(1f)
            // caps each item to an equal share of its side's width so a long Vietnamese label
            // (e.g. "Dinh dưỡng") can't push its neighbors into clipping/overlap on narrow screens;
            // sizeIn's minHeight still guarantees the touch-target floor.
            .weight(1f)
            .sizeIn(minHeight = Dimens.MinTouchTarget)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(color = if (selected) Accent else Color.Transparent, shape = CircleShape)
        )
        Text(
            text = stringResource(item.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Accent else TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WorkoutFab(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .offset(y = (-16).dp)
            .size(Dimens.FabSize)
            .clip(CircleShape)
            .background(color = Accent, shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.nav_workout_fab),
            style = MaterialTheme.typography.labelSmall,
            color = OnAccent,
        )
    }
}
