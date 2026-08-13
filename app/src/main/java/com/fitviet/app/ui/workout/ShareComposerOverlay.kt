package com.fitviet.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.CommunityPostType
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.util.formatMinutesSeconds
import com.fitviet.app.util.formatVi
import java.time.LocalDate

/**
 * Redesign Gate 6c — the share composer, per the mock's own literal markup (`FitViet Redesign Hit
 * & Run.dc.html` lines ~721-748, "SHARE COMPOSER"). A real full-screen [Dialog] (not an inline
 * overlay `Box`), matching [com.fitviet.app.ui.dashboard.StreakMilestoneOverlay]'s own established
 * reasoning: draws above system chrome, and `onDismissRequest` intercepts back-press the same way
 * it intercepts the "Huỷ" tap — no manual `BackHandler` needed, both exits are equally valid since
 * nothing is posted until "Đăng bài →" is actually tapped. Named `*Overlay` rather than `*Screen`
 * for the same reason `StreakMilestoneOverlay` is: it isn't a `FitVietDestination` nav target.
 *
 * Unlike that overlay's vertically-centered content, this composer's content is top- and bottom-
 * anchored, so it needs its own [Modifier.safeDrawingPadding] — a [Dialog] with
 * `decorFitsSystemWindows = false` draws outside the app's normal Scaffold-inset handling, and on
 * targetSdk 35 the window is sized to the full display rather than the inset-safe area. No separate
 * `imePadding()`: `WindowInsets.safeDrawing` is itself `systemBars ∪ ime ∪ displayCutout`, and
 * [Modifier.safeDrawingPadding] consumes all three, so a chained `imePadding()` would resolve to
 * `ime.exclude(safeDrawing)` — zero on every edge, a no-op.
 *
 * The content column both scrolls (growing multi-line input, large font scale) and keeps the mock's
 * own `flex:1` pin-the-CTA-to-the-bottom spacer. Those coexist: [Modifier.verticalScroll] only
 * raises the child's `maxHeight` to infinity, it leaves `minHeight` alone, and [Modifier.fillMaxSize]
 * has already pinned `minHeight` to the full (bounded) height this [Dialog] measures its content
 * with. A [Column]'s measure policy falls back to `minHeight` as its weight target once `maxHeight`
 * is infinite, so `Modifier.weight` still resolves to "whatever is left over" — the CTA sits at the
 * bottom while the content fits, and the spacer collapses to zero (its leftover space clamps at 0)
 * once the content overflows and scrolling takes over.
 *
 * The auto-generated result card's 3-stat grid (Set/Kg/Phút) is the mock's own literal choice —
 * deliberately NOT the same 3 stats `CommunityScreen`'s `WorkoutSharePostCard` shows in the feed
 * (Thời gian/Tổng kg/Chuỗi). This screen is a live preview of the session just finished (where "how
 * many sets did I do" is the interesting number); the feed card is a summary for other users
 * (where "how long, how much volume, what streak" reads better). Both are real data from the same
 * [WorkoutUiState], just different facets of it — not a bug, not meant to match.
 *
 * No PR badge yet — the mock's own card has one ("PR MỚI · KÉO XÔ 25 KG"), but no real PR-detection
 * data reaches this ViewModel yet (see [WorkoutUiState.sessionStreakDays]'s neighbors — there's no
 * equivalent `sessionPr` field). Gate 6d is what surfaces `domain/PersonalRecordCalculator.kt`'s own
 * existing PR computation up to here; fabricating a badge with no real data behind it isn't this
 * gate's call to make.
 *
 * The "Chia sẻ" category pill (`CommunityPostType.SHARE` = 0) is a deliberate no-op tag: tab 0
 * ("Mới nhất") always short-circuits to "all posts" in `CommunityFilter.byTab` before the category
 * clause runs, so a SHARE-tagged post can never land under a specific tab any differently than an
 * untagged one — matching the mock, which has no dedicated "Chia sẻ" feed tab.
 */
@Composable
fun ShareComposerOverlay(
    uiState: WorkoutUiState,
    onTextChange: (String) -> Unit,
    onSelectCategory: (Int) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(HrColors.Bg)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.share_composer_title), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HrColors.TextMid)
                Text(
                    text = stringResource(R.string.share_composer_cancel),
                    fontFamily = HrBody,
                    fontSize = 13.sp,
                    color = HrColors.TextLow,
                    modifier = Modifier.clickable(onClick = onDismiss),
                )
            }

            ResultCard(uiState = uiState)

            ComposerTextField(text = uiState.shareComposerText, onTextChange = onTextChange)

            CategoryPillRow(selected = uiState.shareComposerCategory, onSelect = onSelectCategory)

            Spacer(modifier = Modifier.weight(1f))

            HrPrimaryActionButton(text = stringResource(R.string.share_composer_submit), onClick = onSubmit)
            Text(
                text = stringResource(R.string.share_composer_footer),
                fontFamily = HrBody,
                fontSize = 11.sp,
                color = HrColors.TextFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ResultCard(uiState: WorkoutUiState) {
    val today = LocalDate.now()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(Brush.linearGradient(listOf(HrColors.GradientCardStart, HrColors.GradientCardEnd), start = Offset(0f, 0f)))
            .border(1.dp, HrColors.BorderGradient, HrShapes.CardRegular)
            .padding(18.dp),
    ) {
        Text(
            text = stringResource(R.string.share_composer_date, today.dayOfMonth, today.monthValue, today.year),
            fontFamily = HrBody,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            color = HrColors.Accent,
        )
        Text(
            text = stringResource(R.string.share_composer_workout_done, uiState.dayLabel),
            fontFamily = HrDisplay,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
            color = HrColors.TextHi,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            ResultStatTile(value = formatVi(uiState.sessionTotalSets), label = stringResource(R.string.workout_stat_sets), accent = true, modifier = Modifier.weight(1f))
            ResultStatTile(value = formatVi(uiState.sessionTotalVolumeKg), label = stringResource(R.string.share_composer_stat_kg), modifier = Modifier.weight(1f))
            ResultStatTile(value = formatMinutesSeconds(uiState.sessionElapsedSeconds), label = stringResource(R.string.share_composer_stat_minutes), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ResultStatTile(value: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(
        modifier = modifier
            .clip(HrShapes.ButtonSmall)
            .background(HrColors.Bg.copy(alpha = 0.6f))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value, fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = if (accent) HrColors.Accent else HrColors.TextHi)
        Text(text = label, fontFamily = HrBody, fontSize = 9.sp, color = HrColors.TextLow, modifier = Modifier.padding(top = 1.dp))
    }
}

@Composable
private fun ComposerTextField(text: String, onTextChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clip(HrShapes.CardSmall)
            .background(HrColors.SurfaceInput)
            .border(1.dp, HrColors.Border, HrShapes.CardSmall)
            .padding(14.dp),
    ) {
        if (text.isEmpty()) {
            Text(text = stringResource(R.string.share_composer_placeholder), fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextFaint)
        }
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            textStyle = TextStyle(fontFamily = HrBody, fontSize = 13.sp, color = HrColors.TextHi),
            cursorBrush = SolidColor(HrColors.Accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private data class CategoryPill(val category: Int, val labelRes: Int)

private val CATEGORY_PILLS = listOf(
    CategoryPill(CommunityPostType.PROGRESS, R.string.share_composer_category_progress),
    CategoryPill(CommunityPostType.QA, R.string.share_composer_category_qa),
    CategoryPill(CommunityPostType.SHARE, R.string.share_composer_category_share),
)

@Composable
private fun CategoryPillRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        CATEGORY_PILLS.forEach { pill ->
            val isSelected = pill.category == selected
            Box(
                modifier = Modifier
                    .heightIn(min = Dimens.MinTouchTarget)
                    .clip(PillShape)
                    .background(if (isSelected) HrColors.SurfaceAccent else Color.Transparent)
                    .border(1.5.dp, if (isSelected) HrColors.Accent else HrColors.BorderSoft, PillShape)
                    .clickable { onSelect(pill.category) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(pill.labelRes),
                    fontFamily = HrBody,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                    color = if (isSelected) HrColors.Accent else HrColors.TextLow,
                )
            }
        }
    }
}
