package com.fitviet.app.ui.nutrition.templates

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.MealPlanTemplateEntity
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import kotlinx.coroutines.launch

@Composable
fun TemplatesScreen(viewModel: TemplatesViewModel, onBack: () -> Unit, onPlanGenerated: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

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
            Text(text = stringResource(R.string.nutrition_templates_title), style = MaterialTheme.typography.headlineMedium)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.ListGapLarge),
        ) {
            items(uiState.templates, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    isMatched = template.goalCode == uiState.matchedGoalCode,
                    onUse = {
                        coroutineScope.launch {
                            if (viewModel.useTemplate(template.id)) onPlanGenerated()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun TemplateCard(template: MealPlanTemplateEntity, isMatched: Boolean, onUse: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(if (isMatched) AccentSurfaceSelected else SurfaceCard)
            .border(
                if (isMatched) Dimens.SelectedBorderWidth else Dimens.IdleBorderWidth,
                if (isMatched) Accent else CardBorder,
                MaterialTheme.shapes.large,
            )
            .padding(Dimens.CardPaddingLarge),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (isMatched) {
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(Accent)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.nutrition_template_matches_goal),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnAccent,
                )
            }
        }
        Text(text = template.nameVi, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(text = template.descriptionVi, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        Text(
            text = stringResource(
                R.string.nutrition_template_meta,
                template.kcalPerDay,
                template.proteinPerDayG,
                template.mealsPerDay,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = if (isMatched) Accent else TextMuted,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(Accent)
                .clickable(onClick = onUse)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.nutrition_template_use), style = MaterialTheme.typography.labelLarge, color = OnAccent)
        }
    }
}
