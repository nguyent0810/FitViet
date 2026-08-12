package com.fitviet.app.ui.nutrition.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.formatVi
import com.fitviet.app.util.shortLabelRes
import java.time.DayOfWeek

@Composable
fun CalendarScreen(viewModel: CalendarViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundPage)) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(SurfaceCard)
                    .border(1.dp, CardBorder, MaterialTheme.shapes.small)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "‹", style = MaterialTheme.typography.titleMedium, color = TextMuted)
            }
            Text(text = stringResource(R.string.nutrition_calendar_title), style = MaterialTheme.typography.headlineMedium)
        }

        if (!uiState.hasActivePlan) {
            Text(
                text = stringResource(R.string.nutrition_plan_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 24.dp),
            )
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPaddingHorizontal)
                .clip(MaterialTheme.shapes.large)
                .background(SurfaceCard)
                .border(1.dp, CardBorder, MaterialTheme.shapes.large)
                .padding(Dimens.CardPaddingLarge),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = stringResource(R.string.nutrition_calendar_week_average), style = MaterialTheme.typography.labelLarge, color = TextMuted)
            Text(
                text = stringResource(R.string.nutrition_meal_kcal, formatVi(uiState.weekAverageKcal)),
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = Anton),
                color = Accent,
            )
        }

        Text(
            text = stringResource(R.string.nutrition_calendar_week),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 14.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.ListGapLarge),
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
            .clip(MaterialTheme.shapes.medium)
            .background(if (day.isToday) AccentSurfaceSelected else SurfaceCard)
            .border(
                if (day.isToday) Dimens.SelectedBorderWidth else Dimens.IdleBorderWidth,
                if (day.isToday) Accent else CardBorder,
                MaterialTheme.shapes.medium,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(DayOfWeek.of(day.dayOfWeek).shortLabelRes()),
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
            if (day.isToday) {
                Box(modifier = Modifier.clip(PillShape).background(Accent).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text(text = stringResource(R.string.nutrition_calendar_today), style = MaterialTheme.typography.labelSmall, color = OnAccent)
                }
            }
        }
        Text(
            text = stringResource(R.string.nutrition_meal_kcal, formatVi(day.totalKcal)),
            style = MaterialTheme.typography.titleSmall,
            color = Accent,
        )
    }
}
