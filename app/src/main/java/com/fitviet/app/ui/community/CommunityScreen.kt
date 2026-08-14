package com.fitviet.app.ui.community

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.CommunityPostEntity
import com.fitviet.app.data.local.entity.CommunityPostType
import com.fitviet.app.ui.theme.HrBody
import com.fitviet.app.ui.theme.HrColors
import com.fitviet.app.ui.theme.HrDimens
import com.fitviet.app.ui.theme.HrDisplay
import com.fitviet.app.ui.theme.HrShapes
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.ui.workout.HrSummaryTile
import com.fitviet.app.util.formatMinutesSeconds
import com.fitviet.app.util.formatVi

private data class CommunityTab(val tab: Int, @StringRes val labelRes: Int)

private val COMMUNITY_TABS = listOf(
    CommunityTab(0, R.string.community_tab_latest),
    CommunityTab(CommunityPostType.QA, R.string.community_tab_qa),
    CommunityTab(CommunityPostType.PROGRESS, R.string.community_tab_progress),
)

/**
 * Redesign Gate 6a — token re-skin against the mock's own literal markup (`FitViet Redesign Hit &
 * Run.dc.html` lines ~183-207): legacy `Accent`/`SurfaceCard`/`CardBorder`/`MaterialTheme.typography`
 * tokens replaced with `HrColors`/`HrBody`/`HrDisplay`/`HrShapes`/`HrDimens`, plus dimension
 * corrections to match the mock exactly (30sp title, 42dp avatar with a `SurfaceAccent`/
 * `BorderAccentDim` ring, 17dp post-card padding). The post card's own 17dp radius (`HrShapes.CardRegular`)
 * is a deliberate consistency choice, not a mock-literal one — the mock's own 18dp and this
 * screen's prior `MaterialTheme.shapes.large` (=18dp) were already exact; 17dp trades that literal
 * match for matching every other Hr-token card on this screen family.
 *
 * One structural change: the mock's title row has no right-side element at all — the passive
 * "no compose button, post from the finished screen" pointer instead lives as a centered footer
 * caption below the post list (mock's own copy: "Đăng bài từ màn hoàn thành buổi tập — không có
 * nút đăng trống"), so it moved there instead of staying in the header. `community_add_post`
 * (former header label) is deleted — see `community_footer_note` below.
 *
 * `WorkoutSharePostCard`'s dead `post.programTitle != null` branch is deleted here (the column
 * itself, `programTitle`, was write-only since Gate 2b and was dropped entirely in Gate 6b), and
 * its stat tiles moved from the legacy `SummaryTile` (now deleted, this was its last caller) to
 * [HrSummaryTile] (see that composable's own doc for the updated ownership note).
 */
@Composable
fun CommunityScreen(viewModel: CommunityViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HrColors.Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HrDimens.ScreenPaddingHorizontal, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(text = stringResource(R.string.community_title), fontFamily = HrDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, color = HrColors.TextHi)
        TabRow(selectedTab = uiState.selectedTab, onSelect = viewModel::selectTab)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            uiState.posts.forEach { post ->
                if (post.postType == CommunityPostType.WORKOUT_SHARE) {
                    WorkoutSharePostCard(post = post, onLikeClick = { viewModel.toggleLike(post) })
                } else {
                    PostCard(post = post, onLikeClick = { viewModel.toggleLike(post) })
                }
            }
        }
        Text(
            text = stringResource(R.string.community_footer_note),
            fontFamily = HrBody,
            fontSize = 11.sp,
            color = HrColors.TextFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TabRow(selectedTab: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        COMMUNITY_TABS.forEach { tab ->
            val selected = tab.tab == selectedTab
            Box(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clip(PillShape)
                    .background(if (selected) HrColors.Accent else Color.Transparent)
                    .border(1.dp, if (selected) HrColors.Accent else HrColors.Border, PillShape)
                    .clickable { onSelect(tab.tab) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(tab.labelRes),
                    fontFamily = HrBody,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (selected) HrColors.OnAccent else HrColors.TextMid,
                )
            }
        }
    }
}

@Composable
private fun PostCard(post: CommunityPostEntity, onLikeClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .padding(17.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PostAuthorHeader(post)
        Text(text = post.bodyText, fontFamily = HrBody, fontSize = 14.sp, lineHeight = 21.sp, color = HrColors.TextMid)
        post.badgeText?.let { badge ->
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .border(1.dp, HrColors.BorderAccentDim, PillShape)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(text = badge, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HrColors.Accent)
            }
        }
        PostLikeCommentRow(post = post, onLikeClick = onLikeClick)
    }
}

/** Feature #4b (Gate 41) — renders Gate 40's structured columns instead of freeform [CommunityPostEntity.bodyText]
 * for the stat grid, but (Gate 6b) [bodyText] itself is rendered too now — it was write-only from
 * Gate 41 through Gate 6a (this card never read it; only [PostCard] did), which meant Gate 40's own
 * static "Vừa hoàn thành buổi tập…" copy silently never appeared, and would have made Gate 6b's own
 * new user-composed text (`CommunityRepository.shareWorkout`'s `userText` param) a second write-only
 * field on top of it. Shown between the author header and the stat panel, same position [PostCard]
 * gives the equivalent content.
 *
 * Reuses [PostAuthorHeader]/[PostLikeCommentRow] (identical across every post type) and
 * [HrSummaryTile] (Gate 6a — moved off the legacy `SummaryTile`, same primitive `SessionFinishedContent`
 * uses), so a shared workout reads as a natural extension of the app's existing visual language
 * rather than a bespoke one-off card. The mock's own feed (§12) has no stat grid at all — this
 * card is the app's own Gate 41 extension beyond the mock, kept per capability preservation. */
@Composable
private fun WorkoutSharePostCard(post: CommunityPostEntity, onLikeClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HrShapes.CardRegular)
            .background(HrColors.Surface)
            .border(1.dp, HrColors.Border, HrShapes.CardRegular)
            .padding(17.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PostAuthorHeader(post)
        if (post.bodyText.isNotBlank()) {
            Text(text = post.bodyText, fontFamily = HrBody, fontSize = 14.sp, lineHeight = 21.sp, color = HrColors.TextMid)
        }
        // Gate 41's own mockup's "inset summary block" (not the Hit & Run redesign mock, which has
        // no stat grid here at all — see this file's own top doc) — visually separates "who posted"
        // (header, outside this panel) from "what they did" (this panel), rather than both sitting
        // flat as siblings in the outer card.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(HrShapes.CardSmall)
                .background(HrColors.SurfaceInput)
                .border(1.dp, HrColors.Border, HrShapes.CardSmall)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // There is no program title to show, only the day label — `programTitle` was dropped
            // from `CommunityPostEntity` entirely in Gate 6b (write-only since Gate 2b).
            Text(text = post.dayLabel.orEmpty(), fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HrColors.TextHi)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HrSummaryTile(
                    value = formatMinutesSeconds(post.durationSeconds ?: 0),
                    label = stringResource(R.string.workout_stat_time),
                    modifier = Modifier.weight(1f),
                )
                HrSummaryTile(
                    value = formatVi(post.totalVolumeKg ?: 0.0),
                    label = stringResource(R.string.workout_stat_volume),
                    modifier = Modifier.weight(1f),
                )
                HrSummaryTile(
                    value = formatVi(post.streakDays ?: 0),
                    label = stringResource(R.string.dashboard_stat_streak),
                    accent = true,
                    modifier = Modifier.weight(1f),
                )
            }
            // Redesign Gate 6d — same badge [PostCard] already renders for its own generic seeded
            // posts, now also populated by a real code path (`WorkoutRepository.findSessionPersonalRecord`)
            // for a workout-share post. Placed after the stat grid, inside the same inset panel,
            // matching `ShareComposerOverlay`'s own result card — the two cards show the same badge
            // in the same relative position, just embedded in each card's own stat layout.
            post.badgeText?.let { badge ->
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .border(1.dp, HrColors.BorderAccentDim, PillShape)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(text = badge, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HrColors.Accent)
                }
            }
        }
        PostLikeCommentRow(post = post, onLikeClick = onLikeClick)
    }
}

@Composable
private fun PostAuthorHeader(post: CommunityPostEntity) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(HrColors.SurfaceAccent)
                .border(1.dp, HrColors.BorderAccentDim, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = post.authorInitial, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HrColors.Accent)
        }
        Column {
            Text(text = post.authorName, fontFamily = HrBody, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HrColors.TextHi)
            Text(text = post.timeLabel, fontFamily = HrBody, fontSize = 12.sp, color = HrColors.TextFaint)
        }
    }
}

@Composable
private fun PostLikeCommentRow(post: CommunityPostEntity, onLikeClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        val likeCount = post.baseLikeCount + if (post.likedByUser) 1 else 0
        Box(
            modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp).clickable(onClick = onLikeClick),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = "${if (post.likedByUser) "♥" else "♡"} ${formatVi(likeCount)}",
                fontFamily = HrBody,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (post.likedByUser) HrColors.Accent else HrColors.TextLow,
            )
        }
        Text(
            text = stringResource(
                if (post.postType == CommunityPostType.QA) R.string.community_replies else R.string.community_comments,
                post.commentCount,
            ),
            fontFamily = HrBody,
            fontSize = 13.sp,
            color = HrColors.TextLow,
        )
        if (post.hasBestAnswerMarker) {
            Text(text = stringResource(R.string.community_best_answer), fontFamily = HrBody, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = HrColors.Accent)
        }
    }
}
