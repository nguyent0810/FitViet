package com.fitviet.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.fitviet.app.ui.onboarding.LevelChip
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextMuted

/**
 * Gate 10: a "how much time do you have" step before the session starts — not part of the original
 * 12-screen spec, so styled to match the rest of the app rather than invented from scratch: same
 * centered full-screen layout as [SessionFinishedContent] (the app's other "decision moment" screen)
 * and the same [LevelChip] 3-option row already used for onboarding's level selector.
 *
 * [onExit] leaves for home with no confirmation, unlike [WorkoutHeader]'s exit button — this phase
 * runs before [WorkoutViewModel.selectDuration] creates the session row, so there's no in-progress
 * workout to lose (see [WorkoutViewModel.loadExercisesAndShowDurationPicker]). Reached only via the
 * bottom-nav FAB's programId-less entry (a programId session skips straight past this phase), which
 * previously had no in-app way back besides the OS back gesture.
 */
@Composable
fun WorkoutDurationPickerContent(onSelect: (minutes: Int?) -> Unit, onExit: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.ScreenPaddingHorizontal),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.workout_duration_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.workout_duration_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 28.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LevelChip(
                    label = stringResource(R.string.workout_duration_30),
                    selected = false,
                    onClick = { onSelect(30) },
                    modifier = Modifier.weight(1f),
                )
                LevelChip(
                    label = stringResource(R.string.workout_duration_60),
                    selected = false,
                    onClick = { onSelect(60) },
                    modifier = Modifier.weight(1f),
                )
                LevelChip(
                    label = stringResource(R.string.workout_duration_unlimited),
                    selected = false,
                    onClick = { onSelect(null) },
                    modifier = Modifier.weight(1f),
                )
            }
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
