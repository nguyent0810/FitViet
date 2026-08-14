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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.dao.PersonalBestRow
import com.fitviet.app.domain.MuscleGroupWorkload
import com.fitviet.app.ui.common.entranceFade
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.ui.theme.premiumShadow
import com.fitviet.app.util.formatVi
import com.fitviet.app.util.labelRes
import com.fitviet.app.util.saveBitmapForSharing
import com.fitviet.app.util.shareImageIntent
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Redesign Gate 7d — token re-skin against the mock's `WEEKLY RECAP` section (~L795-818), deferred
 * from Gate 6a (that gate's own plan-check: this screen's OS-share-intent mechanism is a separate
 * concern from Community's in-app composer). The mock's own recap is a 4-stat grid (sessions/kg/PR
 * count/hours) plus a "So với tuần trước" week-over-week comparison card and a "Vào tuần mới →"
 * CTA; this screen keeps its existing 2-stat + muscle-balance + PR-list structure instead — adding
 * the comparison card and PR/hours stats is real new functionality (a week-over-week diff, an hours
 * formatter, wiring `PersonalRecordCalculator` into this screen's own uiState), not a token re-skin,
 * so it's deliberately left for a future gate rather than folded in silently here.
 */
@Composable
fun WeeklyRecapScreen(viewModel: WeeklyRecapViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    var isSharing by remember { mutableStateOf(false) }
    val shareChooserTitle = stringResource(R.string.weekly_recap_share_chooser_title)
    val shareFailedMessage = stringResource(R.string.weekly_recap_share_failed)

    Column(modifier = Modifier.fillMaxSize().background(HrColors.BgDeep)) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(HrShapes.ButtonSmall)
                    .background(HrColors.Surface)
                    .border(1.dp, HrColors.Border, HrShapes.ButtonSmall)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "‹", fontFamily = HrBody, fontSize = 18.sp, color = HrColors.TextMid)
            }
            Text(text = stringResource(R.string.weekly_recap_title), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = HrColors.TextHi)
        }

        if (!uiState.hasData) {
            Text(
                text = stringResource(R.string.weekly_recap_empty),
                fontFamily = HrBody,
                fontSize = 14.sp,
                color = HrColors.TextLow,
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
                        graphicsLayer.record(this, layoutDirection, IntSize(size.width.toInt(), size.height.toInt())) {
                            this@drawWithContent.drawContent()
                        }
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
                    .clip(HrShapes.ButtonCta)
                    .background(HrColors.Accent)
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
                Text(text = stringResource(R.string.weekly_recap_share), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HrColors.OnAccent)
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
            .clip(HrShapes.CardRegular)
            .background(Brush.linearGradient(listOf(HrColors.GradientCardStart, HrColors.GradientCardEnd)))
            .border(1.dp, HrColors.BorderGradient, HrShapes.CardRegular)
            .padding(Dimens.CardPaddingLarge),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGapSmall),
    ) {
        Text(text = stringResource(R.string.weekly_recap_title), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = HrColors.TextHi)
        if (weekStart != null && weekEnd != null) {
            Text(
                text = stringResource(R.string.weekly_recap_date_range, shortDateLabel(weekStart), shortDateLabel(weekEnd)),
                fontFamily = HrBody,
                fontSize = 12.sp,
                color = HrColors.TextLow,
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
                Text(text = stringResource(R.string.weekly_recap_muscle_balance), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HrColors.TextLow)
                val maxVolume = muscleGroupWorkload.maxOf { it.volumeKg }.takeIf { it > 0 } ?: 1.0
                muscleGroupWorkload.forEach { entry ->
                    val fraction = (entry.volumeKg / maxVolume).toFloat().coerceIn(0f, 1f)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(entry.muscleGroup.labelRes()),
                            fontFamily = HrBody,
                            fontSize = 11.sp,
                            color = HrColors.TextMid,
                            modifier = Modifier.weight(0.6f),
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(HrShapes.ButtonCta)
                                .background(HrColors.Border),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(6.dp)
                                    .clip(HrShapes.ButtonCta)
                                    .background(HrColors.Accent),
                            )
                        }
                    }
                }
            }
        }

        if (topPersonalBests.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.weekly_recap_personal_bests), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HrColors.TextLow)
                topPersonalBests.forEach { pr ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = pr.nameVi, fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextHi)
                        Text(
                            text = "${formatVi(pr.maxWeightKg)} kg",
                            fontFamily = HrDisplay,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = HrColors.Accent,
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
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(text = value, fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = HrColors.Accent)
        Text(text = label, fontFamily = HrBody, fontSize = 11.sp, color = HrColors.TextLow, modifier = Modifier.padding(top = 2.dp))
    }
}

private fun shortDateLabel(date: LocalDate): String = "${date.dayOfMonth}/${date.monthValue}"

/**
 * This project's Compose BOM resolves to Compose UI 1.7.6, which does not yet ship the
 * `rememberGraphicsLayer()` convenience composable (added in a later Compose UI release) —
 * confirmed by inspecting the actual `ui`/`ui-graphics` 1.7.6 jars, since an earlier review had
 * incorrectly assumed it was already present at this version. Reimplemented here from the
 * lower-level `GraphicsContext`/[LocalGraphicsContext] API, which IS public at 1.7.6 and is what
 * the real `rememberGraphicsLayer()` wraps internally in newer versions.
 */
@Composable
private fun rememberGraphicsLayer(): GraphicsLayer {
    val graphicsContext = LocalGraphicsContext.current
    val graphicsLayer = remember { graphicsContext.createGraphicsLayer() }
    DisposableEffect(graphicsLayer) {
        onDispose { graphicsContext.releaseGraphicsLayer(graphicsLayer) }
    }
    return graphicsLayer
}
