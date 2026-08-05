package com.fitviet.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitviet.app.R
import com.fitviet.app.ui.onboarding.LevelChip
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.TextMuted

/**
 * Gate 10: a "how much time do you have" step before the session starts — not part of the original
 * 12-screen spec, so styled to match the rest of the app rather than invented from scratch: same
 * centered full-screen layout as [SessionFinishedContent] (the app's other "decision moment" screen)
 * and the same [LevelChip] 3-option row already used for onboarding's level selector.
 */
@Composable
fun WorkoutDurationPickerContent(onSelect: (minutes: Int?) -> Unit) {
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
}
