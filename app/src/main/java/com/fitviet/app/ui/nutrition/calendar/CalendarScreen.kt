package com.fitviet.app.ui.nutrition.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.ui.common.HrBackChip
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.util.formatVi
import com.fitviet.app.util.shortLabelRes
import java.time.DayOfWeek

/** Redesign Gate 5d — token re-skin: legacy `Accent`/`SurfaceCard`/`CardBorder`/`Anton`/
 * `MaterialTheme.typography` tokens replaced with `HrColors`/`HrDisplay`/`HrBody`/`HrShapes`/
 * `HrDimens`, `HrBackChip` swapped in for the inline back-chip `Box`.
 *
 * Not a pure token swap — two call-site changes ride along, same reasoning `PlanScreen`'s own
 * Gate 5d doc comment gives for its own analogous changes:
 * - The week-average card's interior padding moved off the legacy `Dimens.CardPaddingLarge` (20dp)
 *   onto a raw 16dp, matching `RecipeDetailScreen`/`NutritionLibraryScreen`/`ProgramsListScreen`'s
 *   `CardRegular` cards (the 15-16dp modal value — not universal, e.g. `WorkoutStraightScreens`'s
 *   deliberately-emphasized active-set card is 18dp).
 * - The day-row `LazyColumn`'s gap moved off `Dimens.ListGapLarge` (12dp) onto a raw 10dp, matching
 *   `PlanScreen`'s own identical change and `HandbookScreen`/`HandbookMuscleGroupScreen`'s list gap
 *   (again the modal value, not universal — e.g. `RecipeDetailScreen`'s own list is 14dp). */
@Composable
fun CalendarScreen(viewModel: CalendarViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(HrColors.Bg)) {
        Row(
            modifier = Modifier.padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HrBackChip(onClick = onBack)
            Text(text = stringResource(R.string.nutrition_calendar_title), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = HrColors.TextHi)
        }

        if (!uiState.hasActivePlan) {
            Text(
                text = stringResource(R.string.nutrition_plan_empty),
                fontFamily = HrBody,
                fontSize = 14.sp,
                color = HrColors.TextLow,
                modifier = Modifier.padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 24.dp),
            )
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HrDimens.ScreenPaddingHorizontal)
                .clip(HrShapes.CardRegular)
                .background(HrColors.Surface)
                .border(1.dp, HrColors.Border, HrShapes.CardRegular)
                // 16.dp, matching RecipeDetailScreen/NutritionLibraryScreen/ProgramsListScreen's
                // CardRegular interior padding (the 15-16dp modal value) — the legacy
                // Dimens.CardPaddingLarge (20dp) reads visibly roomier than this screen's siblings.
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = stringResource(R.string.nutrition_calendar_week_average), fontFamily = HrBody, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = HrColors.TextLow)
            Text(
                text = stringResource(R.string.nutrition_meal_kcal, formatVi(uiState.weekAverageKcal)),
                fontFamily = HrDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = HrColors.Accent,
            )
        }

        Text(
            text = stringResource(R.string.nutrition_calendar_week),
            fontFamily = HrBody,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = HrColors.TextHi,
            modifier = Modifier.padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 14.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 4.dp),
            // Raw 10.dp, matching PlanScreen's own identical change (see this file's own top doc
            // comment for the fuller precedent list) — Dimens.ListGapLarge (12dp) is a
            // legacy-screen token, not an Hr-list one.
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(uiState.days, key = { it.dayId }) { day ->
                CalendarDayRow(day = day)
            }
        }
    }
}

@Composable
private fun CalendarDayRow(day: CalendarDayUiItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(if (day.isToday) HrColors.SurfaceAccent else HrColors.Surface)
            .border(
                if (day.isToday) Dimens.SelectedBorderWidth else Dimens.IdleBorderWidth,
                if (day.isToday) HrColors.Accent else HrColors.Border,
                HrShapes.CardRegular,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(DayOfWeek.of(day.dayOfWeek).shortLabelRes()),
                fontFamily = HrBody,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = HrColors.TextHi,
            )
            if (day.isToday) {
                Box(modifier = Modifier.clip(PillShape).background(HrColors.Accent).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text(text = stringResource(R.string.nutrition_calendar_today), fontFamily = HrBody, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = HrColors.OnAccent)
                }
            }
        }
        Text(
            text = stringResource(R.string.nutrition_meal_kcal, formatVi(day.totalKcal)),
            fontFamily = HrBody,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = HrColors.Accent,
        )
    }
}
