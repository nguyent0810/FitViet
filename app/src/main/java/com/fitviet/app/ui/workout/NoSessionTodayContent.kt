package com.fitviet.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitviet.app.R
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextMuted

/**
 * Redesign Gate 1c — the no-arg entry point's "nothing to train today" outcome, replacing the old
 * duration picker's role as the screen a bare bottom-nav-FAB tap can land on. Reuses the exact same
 * copy Dashboard's [com.fitviet.app.domain.TodayMonthlyPlanCard] hero card already shows for each
 * [NoSessionReason] so the two never disagree about how "today" is described. Same centered layout
 * + top-start exit affordance as the picker it replaces (see [WorkoutScreen]'s header-visibility
 * guard, which hides the normal header for this phase the same way it did for the picker).
 */
@Composable
fun NoSessionTodayContent(reason: NoSessionReason, onExit: () -> Unit) {
    val (titleRes, metaRes) = when (reason) {
        NoSessionReason.REST_DAY -> R.string.dashboard_rest_day_title to R.string.dashboard_rest_day_meta
        NoSessionReason.UNAVAILABLE -> R.string.dashboard_unavailable_title to R.string.dashboard_unavailable_meta
        NoSessionReason.PLAN_FINISHED -> R.string.dashboard_plan_finished_title to R.string.dashboard_plan_finished_meta
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.ScreenPaddingHorizontal),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(metaRes),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp)
                .size(34.dp)
                .clip(MaterialTheme.shapes.small)
                .background(SurfaceCard)
                .border(1.dp, CardBorder, MaterialTheme.shapes.small)
                .clickable(onClick = onExit),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "‹", style = MaterialTheme.typography.titleMedium, color = TextMuted)
        }
    }
}
