package com.fitviet.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitviet.app.R
import com.fitviet.app.domain.CaloriesCalculator
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.util.formatMinutesSeconds
import com.fitviet.app.util.formatVi

@Composable
fun SessionFinishedContent(uiState: WorkoutUiState, onBackToHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenPaddingHorizontal),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.workout_session_finished_title),
            style = MaterialTheme.typography.headlineLarge.copy(fontFamily = Anton, fontSize = 52.sp, lineHeight = 58.sp),
            color = Accent,
            textAlign = TextAlign.Center,
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile(value = uiState.sessionTotalSets.toString(), label = stringResource(R.string.workout_stat_sets), accent = true, modifier = Modifier.weight(1f))
            SummaryTile(value = formatVi(uiState.sessionTotalVolumeKg), label = stringResource(R.string.workout_stat_volume), modifier = Modifier.weight(1f))
            SummaryTile(value = formatMinutesSeconds(uiState.sessionElapsedSeconds), label = stringResource(R.string.workout_stat_time), modifier = Modifier.weight(1f))
            SummaryTile(
                // Feature #10 — a rough estimate, not a precise measurement; see CaloriesCalculator's doc comment.
                value = CaloriesCalculator.estimateKcal(uiState.sessionElapsedSeconds).toString(),
                label = stringResource(R.string.workout_stat_kcal),
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = stringResource(R.string.workout_session_finished_note),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        PrimaryActionButton(
            text = stringResource(R.string.workout_back_to_home),
            onClick = onBackToHome,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}
