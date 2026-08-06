package com.fitviet.app.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.SettingsEntity
import com.fitviet.app.domain.MeasurementDeltas
import com.fitviet.app.domain.WeightHistoryRange
import com.fitviet.app.domain.WeightPoint
import com.fitviet.app.ui.common.RangePills
import com.fitviet.app.ui.common.SettingsRow
import com.fitviet.app.ui.onboarding.GOAL_OPTIONS
import com.fitviet.app.ui.onboarding.LEVEL_OPTIONS
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentBorderAlt
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.DeepSurface1
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HeroGradientEnd
import com.fitviet.app.ui.theme.HeroGradientStart
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.util.formatLengthUnit
import com.fitviet.app.util.formatOneDecimal
import com.fitviet.app.util.formatWeightUnit
import com.fitviet.app.util.kgToLb
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPage)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGapLarge),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(SurfaceCard)
                        .border(1.dp, CardBorder, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "‹", style = MaterialTheme.typography.titleMedium, color = TextMuted)
                }
            }
            Text(text = stringResource(R.string.profile_back), style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        ProfileHeader(settings = uiState.settings, onAvatarClick = onEditProfile)
        MeasurementsCard(
            deltas = uiState.deltas,
            weightKg = uiState.latestMeasurement?.weightKg,
            chestCm = uiState.latestMeasurement?.chestCm,
            waistCm = uiState.latestMeasurement?.waistCm,
            armCm = uiState.latestMeasurement?.armCm,
            useImperial = uiState.settings.useImperialUnits,
            onUpdateClick = viewModel::openUpdateSheet,
            onHistoryClick = viewModel::openHistorySheet,
        )
        WeightHistoryCard(
            points = uiState.weightHistoryPoints,
            range = uiState.weightHistoryRange,
            useImperial = uiState.settings.useImperialUnits,
            onRangeSelect = viewModel::selectWeightHistoryRange,
        )
        SingleRowCard(label = stringResource(R.string.profile_settings_edit_profile), onClick = onEditProfile)
        SingleRowCard(label = stringResource(R.string.profile_settings_open_settings), onClick = onOpenSettings)
        DonateCard(donated = uiState.settings.hasDonated, onDonateClick = viewModel::toggleDonated)
    }

    if (uiState.showUpdateSheet) {
        UpdateMeasurementSheet(
            prefill = uiState.editingMeasurement ?: uiState.latestMeasurement,
            isEditing = uiState.editingMeasurement != null,
            onSave = viewModel::saveMeasurement,
            onDismiss = viewModel::dismissUpdateSheet,
        )
    }
    if (uiState.showHistorySheet) {
        MeasurementHistorySheet(
            history = uiState.measurementHistory,
            useImperial = uiState.settings.useImperialUnits,
            onEdit = viewModel::openEditSheet,
            onDelete = { viewModel.deleteMeasurement(it.id) },
            onDismiss = viewModel::dismissHistorySheet,
        )
    }
}

@Composable
private fun ProfileHeader(settings: SettingsEntity, onAvatarClick: () -> Unit) {
    val goalTitle = stringResource(GOAL_OPTIONS[settings.selectedGoal].titleRes)
    val levelTitle = stringResource(LEVEL_OPTIONS[settings.selectedLevel])
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        MonogramAvatar(
            initial = avatarInitial(settings.displayName),
            avatarId = settings.avatarId,
            size = 60.dp,
            style = MaterialTheme.typography.headlineSmall,
            borderWidth = 2.dp,
            modifier = Modifier.clickable(onClick = onAvatarClick),
        )
        Column {
            Text(text = settings.displayName, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(R.string.profile_meta, levelTitle, goalTitle),
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun MeasurementsCard(
    deltas: MeasurementDeltas,
    weightKg: Double?,
    chestCm: Double?,
    waistCm: Double?,
    armCm: Double?,
    useImperial: Boolean,
    onUpdateClick: () -> Unit,
    onHistoryClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large)
            .padding(Dimens.CardPaddingLarge),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(R.string.profile_measurements_title), style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clickable(onClick = onHistoryClick)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.profile_history_button),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMuted,
                    )
                }
                Box(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clickable(onClick = onUpdateClick)
                        .padding(start = 4.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(
                        text = stringResource(R.string.profile_update_cta),
                        style = MaterialTheme.typography.labelLarge,
                        color = Accent,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MeasurementTile(
                value = weightKg,
                delta = deltas.weightKg,
                label = stringResource(R.string.profile_tile_weight_short),
                useImperial = useImperial,
                isWeight = true,
                modifier = Modifier.weight(1f),
            )
            MeasurementTile(
                value = chestCm,
                delta = deltas.chestCm,
                label = stringResource(R.string.profile_tile_chest_short),
                useImperial = useImperial,
                isWeight = false,
                modifier = Modifier.weight(1f),
            )
            MeasurementTile(
                value = waistCm,
                delta = deltas.waistCm,
                label = stringResource(R.string.profile_tile_waist_short),
                useImperial = useImperial,
                isWeight = false,
                modifier = Modifier.weight(1f),
            )
            MeasurementTile(
                value = armCm,
                delta = deltas.armCm,
                label = stringResource(R.string.profile_tile_arm_short),
                useImperial = useImperial,
                isWeight = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MeasurementTile(
    value: Double?,
    delta: Double?,
    label: String,
    useImperial: Boolean,
    isWeight: Boolean,
    modifier: Modifier = Modifier,
) {
    fun format(v: Double) = if (isWeight) formatWeightUnit(v, useImperial) else formatLengthUnit(v, useImperial)

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(DeepSurface1)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value?.let(::format) ?: "–",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = Anton),
            color = TextPrimary,
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
        if (delta != null && delta != 0.0) {
            val positive = delta > 0
            Text(
                text = "${if (positive) "+" else "−"}${format(abs(delta))}",
                style = MaterialTheme.typography.labelSmall,
                color = if (positive) Accent else TextMuted,
                fontWeight = if (positive) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

private val WEIGHT_HISTORY_RANGES = listOf(
    WeightHistoryRange.THIRTY_DAYS to R.string.profile_weight_range_30d,
    WeightHistoryRange.THREE_MONTHS to R.string.profile_weight_range_3m,
    WeightHistoryRange.ALL_TIME to R.string.profile_weight_range_all,
)

@Composable
private fun WeightHistoryCard(
    points: List<WeightPoint>,
    range: WeightHistoryRange,
    useImperial: Boolean,
    onRangeSelect: (WeightHistoryRange) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large)
            .padding(Dimens.CardPaddingLarge),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.profile_weight_history_title), style = MaterialTheme.typography.titleSmall)
        RangePills(options = WEIGHT_HISTORY_RANGES, selected = range, onSelect = onRangeSelect)
        if (points.size < 2) {
            Text(
                text = stringResource(R.string.profile_weight_history_empty),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        } else {
            WeightLineChart(points = points, useImperial = useImperial)
        }
    }
}

@Composable
private fun WeightLineChart(points: List<WeightPoint>, useImperial: Boolean) {
    val displayValues = points.map { if (useImperial) kgToLb(it.weightKg) else it.weightKg }
    val minValue = displayValues.min()
    val maxValue = displayValues.max()
    // A flat/near-flat series (e.g. two check-ins with the same weight) has no real span to map
    // onto — draw it as a flat line at mid-height rather than dividing by a fallback span, which
    // would silently place it at the bottom instead.
    val isFlat = maxValue - minValue <= 0.01

    Column {
        Row(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            Column(
                modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = formatOneDecimal(maxValue), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(text = formatOneDecimal(minValue), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
            Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val pointGapPx = 5.dp.toPx()
                val stepX = if (points.size > 1) (size.width - pointGapPx * 2) / (points.size - 1) else 0f
                val usableHeight = size.height - pointGapPx * 2
                fun yFor(value: Double): Float {
                    val fraction = if (isFlat) 0.5f else ((value - minValue) / (maxValue - minValue)).toFloat()
                    return usableHeight - (fraction * usableHeight) + pointGapPx
                }
                val path = Path()
                displayValues.forEachIndexed { index, value ->
                    val x = pointGapPx + index * stepX
                    val y = yFor(value)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path = path, color = Accent, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                displayValues.forEachIndexed { index, value ->
                    drawCircle(color = Accent, radius = 4.dp.toPx(), center = Offset(pointGapPx + index * stepX, yFor(value)))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = shortDateLabel(points.first().date), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            if (points.size > 1) {
                Text(text = shortDateLabel(points.last().date), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }
}

private fun shortDateLabel(date: LocalDate): String = "${date.dayOfMonth}/${date.monthValue}"

/** A single-row card wrapping the shared [SettingsRow] — used for the two Profile-level navigation
 * entries ("Chỉnh sửa hồ sơ ›", "Cài đặt ›") now that the old multi-row `SettingsCard`/
 * `DashboardWidgetsCard` have moved into `ui/settings/SettingsScreen.kt` (Gate 37). */
@Composable
private fun SingleRowCard(label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large),
    ) {
        SettingsRow(label = label, value = "›", onClick = onClick, showDivider = false)
    }
}

@Composable
private fun DonateCard(donated: Boolean, onDonateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd)))
            .border(1.dp, AccentBorderAlt, MaterialTheme.shapes.large)
            .padding(Dimens.CardPaddingLarge),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (donated) {
            Text(text = stringResource(R.string.profile_donated_title), style = MaterialTheme.typography.titleSmall, color = Accent)
            Text(text = stringResource(R.string.profile_donated_body), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .border(1.dp, AccentBorderAlt, MaterialTheme.shapes.small)
                    .clickable(onClick = onDonateClick)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.profile_donated_undo), style = MaterialTheme.typography.labelLarge, color = TextMuted)
            }
        } else {
            Text(text = stringResource(R.string.profile_donate_title), style = MaterialTheme.typography.titleSmall)
            Text(text = stringResource(R.string.profile_donate_body), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .border(Dimens.SelectedBorderWidth, Accent, MaterialTheme.shapes.small)
                    .clickable(onClick = onDonateClick)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.profile_donate_cta), style = MaterialTheme.typography.titleMedium, color = Accent)
            }
        }
    }
}
