package com.fitviet.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fitviet.app.R
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.TextMuted

/** Stub for screens built in later gates (Dashboard, Programs, Workout, Nutrition, Community). */
@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPage),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$title — ${stringResource(R.string.placeholder_coming_soon)}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
    }
}
