package com.fitviet.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitviet.app.R
import com.fitviet.app.ui.common.HrBackChip
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay

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
        NoSessionReason.ALREADY_COMPLETED -> R.string.dashboard_already_completed_title to R.string.dashboard_already_completed_meta
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = HrDimens.ScreenPaddingHorizontal),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(titleRes),
                fontFamily = HrDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = HrColors.TextHi,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(metaRes),
                fontFamily = HrBody,
                fontSize = 14.sp,
                color = HrColors.TextLow,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        HrBackChip(
            onClick = onExit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 16.dp),
        )
    }
}
