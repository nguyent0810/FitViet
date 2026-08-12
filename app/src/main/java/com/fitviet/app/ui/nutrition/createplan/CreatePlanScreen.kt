package com.fitviet.app.ui.nutrition.createplan

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.fitviet.app.domain.NutritionGoal
import com.fitviet.app.ui.onboarding.OnboardingPrimaryButton
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.formatVi
import kotlinx.coroutines.launch

@Composable
fun CreatePlanScreen(viewModel: CreatePlanViewModel, onBack: () -> Unit, onPlanGenerated: () -> Unit) {
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
            Text(text = stringResource(R.string.nutrition_create_plan_title), style = MaterialTheme.typography.headlineMedium)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPaddingHorizontal),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionGapLarge),
        ) {
            WizardSection(label = stringResource(R.string.nutrition_create_plan_goal)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GOAL_OPTIONS.forEach { (goal, labelRes) ->
                        PillChip(
                            label = stringResource(labelRes),
                            selected = uiState.goal == goal,
                            onClick = { viewModel.selectGoal(goal) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            WizardSection(label = stringResource(R.string.nutrition_create_plan_kcal)) {
                Stepper(
                    valueText = stringResource(R.string.nutrition_meal_kcal, formatVi(uiState.kcalTarget)),
                    onDecrement = viewModel::decrementKcal,
                    onIncrement = viewModel::incrementKcal,
                )
            }

            WizardSection(label = stringResource(R.string.nutrition_create_plan_protein)) {
                Stepper(
                    valueText = stringResource(R.string.nutrition_recipe_grams, uiState.proteinTargetG),
                    onDecrement = viewModel::decrementProtein,
                    onIncrement = viewModel::incrementProtein,
                )
            }

            WizardSection(label = stringResource(R.string.nutrition_create_plan_meals_per_day)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (CreatePlanViewModel.MIN_MEALS_PER_DAY..CreatePlanViewModel.MAX_MEALS_PER_DAY).forEach { count ->
                        PillChip(
                            label = formatVi(count),
                            selected = uiState.mealsPerDay == count,
                            onClick = { viewModel.selectMealsPerDay(count) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            WizardSection(label = stringResource(R.string.nutrition_create_plan_cook_time)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CreatePlanViewModel.COOK_TIME_OPTIONS.forEach { minutes ->
                        PillChip(
                            label = if (minutes == null) {
                                stringResource(R.string.nutrition_create_plan_cook_time_unlimited)
                            } else {
                                stringResource(R.string.nutrition_create_plan_cook_time_option, minutes)
                            },
                            selected = uiState.cookTimeCeilingMinutes == minutes,
                            onClick = { viewModel.selectCookTimeCeiling(minutes) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        OnboardingPrimaryButton(
            text = stringResource(R.string.nutrition_create_plan_generate),
            onClick = {
                coroutineScope.launch {
                    if (viewModel.generate()) onPlanGenerated()
                }
            },
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 14.dp),
        )
    }
}

@Composable
private fun WizardSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = TextMuted)
        content()
    }
}

@Composable
private fun Stepper(valueText: String, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, PillShape)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StepperButton(symbol = "−", onClick = onDecrement)
        Text(text = valueText, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        StepperButton(symbol = "+", onClick = onIncrement)
    }
}

@Composable
private fun StepperButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, style = MaterialTheme.typography.titleMedium, color = Accent)
    }
}

@Composable
private fun PillChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(if (selected) Accent else SurfaceCard)
            .border(1.dp, if (selected) Accent else CardBorder, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) OnAccent else TextMuted,
        )
    }
}

private val GOAL_OPTIONS = listOf(
    NutritionGoal.CUT to R.string.nutrition_create_plan_goal_cut,
    NutritionGoal.MAINTAIN to R.string.nutrition_create_plan_goal_maintain,
    NutritionGoal.BULK to R.string.nutrition_create_plan_goal_bulk,
)
