package com.fitviet.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.fitviet.app.R
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextMuted

private const val SUGGESTED_DAYS_PER_WEEK = 6

// Prototype gaps for this screen: 16px between sections, 9px between split cards (README §2a).
private val SECTION_GAP = 16.dp
private val SPLIT_CARD_GAP = 9.dp

@Composable
fun SplitScreen(
    viewModel: OnboardingViewModel,
    onContinue: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPage),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
        ) {
            OnboardingProgressBar(totalSteps = 3, filledCount = 2)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.split_title),
                    style = MaterialTheme.typography.headlineLarge,
                )
                val subtitlePrefix = stringResource(R.string.split_subtitle_prefix)
                val subtitleDays = stringResource(R.string.split_subtitle_days, SUGGESTED_DAYS_PER_WEEK)
                Text(
                    text = buildAnnotatedString {
                        append(subtitlePrefix)
                        withStyle(SpanStyle(color = Accent, fontWeight = FontWeight.Bold)) {
                            append(subtitleDays)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(SPLIT_CARD_GAP)) {
                SPLIT_OPTIONS.forEachIndexed { index, option ->
                    SelectionCard(
                        title = stringResource(option.titleRes),
                        subtitle = stringResource(option.subRes),
                        selected = uiState.selectedSplit == index,
                        recommended = option.recommended,
                        onClick = { viewModel.selectSplit(index) },
                        horizontalPadding = 16.dp,
                        verticalPadding = 14.dp,
                    )
                }
            }

            val infoPrefix = stringResource(R.string.split_info_prefix)
            val infoBody = stringResource(R.string.split_info_body)
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Accent, fontWeight = FontWeight.Bold)) {
                        append(infoPrefix)
                    }
                    append(infoBody)
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(SurfaceCard)
                    .border(1.dp, CardBorder, MaterialTheme.shapes.small)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }

        OnboardingPrimaryButton(
            text = stringResource(R.string.split_continue),
            onClick = onContinue,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 14.dp),
        )
    }
}
