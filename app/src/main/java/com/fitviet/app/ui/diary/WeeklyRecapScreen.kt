package com.fitviet.app.ui.diary

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawLayer
import androidx.compose.ui.graphics.layer.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.dao.PersonalBestRow
import com.fitviet.app.domain.MuscleGroupWorkload
import com.fitviet.app.ui.common.entranceFade
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.Anton
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.DeepSurface2
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HeroGradientEnd
import com.fitviet.app.ui.theme.HeroGradientStart
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextBody
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.ui.theme.TextPrimary
import com.fitviet.app.ui.theme.premiumShadow
import com.fitviet.app.util.formatVi
import com.fitviet.app.util.labelRes
import com.fitviet.app.util.saveBitmapForSharing
import com.fitviet.app.util.shareImageIntent
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WeeklyRecapScreen(viewModel: WeeklyRecapViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    var isSharing by remember { mutableStateOf(false) }
    val shareChooserTitle = stringResource(R.string.weekly_recap_share_chooser_title)
    val shareFailedMessage = stringResource(R.string.weekly_recap_share_failed)

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
            Text(text = stringResource(R.string.weekly_recap_title), style = MaterialTheme.typography.headlineMedium)
        }

        if (!uiState.hasData) {
            Text(
                text = stringResource(R.string.weekly_recap_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 24.dp),
            )
            return@Column
        }

        Column(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionGapLarge),
        ) {
            // Only the card itself (not the header/share-button chrome around it) is captured —
            // record() runs on every draw pass, toImageBitmap() is only actually invoked when the
            // user taps Share, so this costs nothing while the screen just sits on-screen.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .entranceFade()
                    .drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(graphicsLayer)
                    },
            ) {
                RecapCard(
                    weekStart = uiState.weekStart,
                    weekEnd = uiState.weekEnd,
                    sessions = uiState.sessionsThisWeek,
                    volumeKg = uiState.volumeThisWeekKg,
                    muscleGroupWorkload = uiState.muscleGroupWorkload,
                    topPersonalBests = uiState.topPersonalBests,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(Accent)
                    .clickable(enabled = !isSharing) {
                        isSharing = true
                        coroutineScope.launch {
                            try {
                                val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                val uri = withContext(Dispatchers.IO) {
                                    saveBitmapForSharing(context, bitmap, "weekly_recap.png")
                                }
                                context.startActivity(shareImageIntent(uri, shareChooserTitle))
                            } catch (e: Exception) {
                                Toast.makeText(context, shareFailedMessage, Toast.LENGTH_SHORT).show()
                            } finally {
                                isSharing = false
                            }
                        }
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.weekly_recap_share), style = MaterialTheme.typography.titleMedium, color = OnAccent)
            }
        }
    }
}

@Composable
private fun RecapCard(
    weekStart: LocalDate?,
    weekEnd: LocalDate?,
    sessions: Int,
    volumeKg: Double,
    muscleGroupWorkload: List<MuscleGroupWorkload>,
    topPersonalBests: List<PersonalBestRow>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .premiumShadow(radius = 18.dp, accentBloom = true)
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd)))
            .border(1.dp, CardBorder, MaterialTheme.shapes.large)
            .padding(Dimens.CardPaddingLarge),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGapSmall),
    ) {
        Text(text = stringResource(R.string.weekly_recap_title), style = MaterialTheme.typography.titleMedium)
        if (weekStart != null && weekEnd != null) {
            Text(
                text = stringResource(R.string.weekly_recap_date_range, shortDateLabel(weekStart), shortDateLabel(weekEnd)),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ListGapLarge)) {
            RecapStatTile(label = stringResource(R.string.weekly_recap_sessions), value = formatVi(sessions), modifier = Modifier.weight(1f))
            RecapStatTile(
                label = stringResource(R.string.weekly_recap_volume),
                value = "${formatVi(volumeKg)} kg",
                modifier = Modifier.weight(1f),
            )
        }

        if (muscleGroupWorkload.any { it.volumeKg > 0.0 }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.weekly_recap_muscle_balance), style = MaterialTheme.typography.labelLarge, color = TextMuted)
                val maxVolume = muscleGroupWorkload.maxOf { it.volumeKg }.takeIf { it > 0 } ?: 1.0
                muscleGroupWorkload.forEach { entry ->
                    val fraction = (entry.volumeKg / maxVolume).toFloat().coerceIn(0f, 1f)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(entry.muscleGroup.labelRes()),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextBody,
                            modifier = Modifier.weight(0.6f),
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(DeepSurface2),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(6.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(Accent),
                            )
                        }
                    }
                }
            }
        }

        if (topPersonalBests.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.weekly_recap_personal_bests), style = MaterialTheme.typography.labelLarge, color = TextMuted)
                topPersonalBests.forEach { pr ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = pr.nameVi, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        Text(
                            text = "${formatVi(pr.maxWeightKg)} kg",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = Anton),
                            color = Accent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecapStatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(AccentSurfaceSelected)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontFamily = Anton), color = Accent)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}

private fun shortDateLabel(date: LocalDate): String = "${date.dayOfMonth}/${date.monthValue}"
