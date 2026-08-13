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
// Redesign Phase 3b — Handbook dropped from here (see FitVietDestination.BOTTOM_NAV_ROUTES' own
// doc): reachable via the Kế hoạch tab's "Thư viện bài tập" row instead, per the mock's own "6 mục
// → 4 tab + TẬP" nav consolidation. Back to 2/2 with NAV_ITEMS_LEFT, which incidentally resolves
// the centering compromise the old 2-vs-3 split forced (see the Row below).
private val NAV_ITEMS_RIGHT = listOf(
    NavItem(FitVietDestination.Nutrition, R.string.nav_nutrition),
    NavItem(FitVietDestination.Community, R.string.nav_community),
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
        // Redesign Phase 3b restored the 2 left / 2 right split (Handbook's removal, see
        // NAV_ITEMS_RIGHT's own doc) that Gate 25's 5th item had broken — weighting each side by
        // its own item count now gives every item an equal share AND puts the FAB at the bar's
        // true mathematical center, with no overlap risk. (Gate 25 through Phase 3a ran with an
        // asymmetric 2 left / 3 right split and an off-center FAB as a deliberate, documented
        // trade-off rather than risk the FAB's hit-test layer stealing taps from a nav item — that
        // trade-off is moot now that the counts match again.)
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
            // weight(1f) caps each item to an equal share of its side's width so a long
            // Vietnamese label (e.g. "Dinh dưỡng") can't push its neighbors into clipping/overlap
            // on narrow screens; sizeIn's minHeight still guarantees the touch-target floor.
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
