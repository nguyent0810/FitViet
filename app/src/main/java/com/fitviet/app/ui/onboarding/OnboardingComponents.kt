package com.fitviet.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fitviet.app.R
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.DotBorderIdle
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary

/** Idle → accent-tinted bg + accent border + filled radio dot; matches the global selection pattern. */
@Composable
fun SelectionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    recommended: Boolean = false,
    horizontalPadding: Dp = 18.dp,
    verticalPadding: Dp = 16.dp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) AccentSurfaceSelected else SurfaceCard)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Accent else CardBorder,
                shape = MaterialTheme.shapes.medium,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                if (recommended) RecommendedBadge()
            }
            Text(text = subtitle, style = MaterialTheme.typography.labelMedium, color = TextMuted, modifier = Modifier.padding(top = 3.dp))
        }
        SelectionDot(selected = selected)
    }
}

@Composable
private fun RecommendedBadge() {
    Text(
        text = stringResource(R.string.split_recommended_badge),
        style = MaterialTheme.typography.labelSmall,
        color = OnAccent,
        modifier = Modifier
            .background(color = Accent, shape = MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun SelectionDot(selected: Boolean, modifier: Modifier = Modifier) {
    val bg = if (selected) Accent else Color.Transparent
    val border = if (selected) Color.Transparent else DotBorderIdle
    Box(
        modifier = modifier
            .size(18.dp)
            .background(color = bg, shape = CircleShape)
            .border(width = 1.5.dp, color = border, shape = CircleShape),
    )
}

/** 3-chip row used for the level selector on 1a. */
@Composable
fun LevelChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) AccentSurfaceSelected else SurfaceCard)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Accent else CardBorder,
                shape = MaterialTheme.shapes.small,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) TextPrimary else TextMuted,
        )
    }
}

/** Solid-fill pill chip — matches `ProgramsListScreen`'s `FilterChips` treatment exactly (selected =
 * Accent bg + OnAccent text, idle = SurfaceCard + CardBorder + TextMuted, true [PillShape] capsule),
 * per the plan's explicit "same chip treatment as FilterChips" instruction for the days-per-week
 * row (feature #3) — distinct from [LevelChip]'s tinted-selection-card look used for 1a's level
 * selector, which reads as "this option is selected" rather than "this filter is active." */
@Composable
fun PillChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(PillShape)
            .background(if (selected) Accent else SurfaceCard)
            .border(width = 1.dp, color = if (selected) Accent else CardBorder, shape = PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) OnAccent else TextMuted,
        )
    }
}

/** Filled accent CTA used at the bottom of every onboarding step. */
@Composable
fun OnboardingPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Accent)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, color = OnAccent)
    }
}

/** N-segment progress bar; [filledCount] segments render accent, the rest render idle. */
@Composable
fun OnboardingProgressBar(totalSteps: Int, filledCount: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(if (index < filledCount) Accent else CardBorder),
            )
        }
    }
}
