package com.fitviet.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitviet.app.R
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentBorder
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.util.formatMinutesSeconds

/** Shared full-screen countdown used by both the straight-set rest phase and the superset rest phase. */
@Composable
fun RestContent(
    title: String,
    secondsRemaining: Int,
    nextLabel: String,
    onAddRest: () -> Unit,
    onSkipRest: () -> Unit,
    countdownFontSize: androidx.compose.ui.unit.TextUnit = 96.sp,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenPaddingHorizontal),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.labelLarge, color = TextMuted)
        Text(
            text = formatMinutesSeconds(secondsRemaining),
            style = MaterialTheme.typography.headlineLarge.copy(fontFamily = Anton, fontSize = countdownFontSize),
            color = Accent,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (nextLabel.isNotEmpty()) {
            Text(text = nextLabel, style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.padding(top = 8.dp))
        }
        Row(modifier = Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .border(1.dp, AccentBorder, MaterialTheme.shapes.small)
                    .clickable(onClick = onAddRest)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Text(text = stringResource(R.string.workout_add_rest), style = MaterialTheme.typography.titleSmall, color = Accent)
            }
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(Accent)
                    .clickable(onClick = onSkipRest)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Text(text = stringResource(R.string.workout_skip_rest), style = MaterialTheme.typography.titleMedium, color = OnAccent)
            }
        }
    }
}
