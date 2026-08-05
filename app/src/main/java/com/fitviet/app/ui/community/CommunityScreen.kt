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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.CommunityPostEntity
import com.fitviet.app.data.local.entity.CommunityPostType
import com.fitviet.app.ui.theme.Accent
import com.fitviet.app.ui.theme.AccentBorder
import com.fitviet.app.ui.theme.AccentSurfaceSelected
import com.fitviet.app.ui.theme.BackgroundPage
import com.fitviet.app.ui.theme.CardBorder
import com.fitviet.app.ui.theme.Dimens
import com.fitviet.app.ui.theme.OnAccent
import com.fitviet.app.ui.theme.PillShape
import com.fitviet.app.ui.theme.SurfaceCard
import com.fitviet.app.ui.theme.TextBody
import com.fitviet.app.ui.theme.TextFaint
import com.fitviet.app.ui.theme.TextMuted
import com.fitviet.app.util.formatVi

private data class CommunityTab(val tab: Int, @StringRes val labelRes: Int)

private val COMMUNITY_TABS = listOf(
    CommunityTab(0, R.string.community_tab_latest),
    CommunityTab(CommunityPostType.QA, R.string.community_tab_qa),
    CommunityTab(CommunityPostType.PROGRESS, R.string.community_tab_progress),
)

@Composable
fun CommunityScreen(viewModel: CommunityViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPage)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenPaddingHorizontal, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGapLarge),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(R.string.community_title), style = MaterialTheme.typography.headlineMedium)
            // Static in the prototype too (no onClick on this label) — there's no real post-creation flow.
            Text(text = stringResource(R.string.community_add_post), style = MaterialTheme.typography.labelLarge, color = Accent)
        }
        TabRow(selectedTab = uiState.selectedTab, onSelect = viewModel::selectTab)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            uiState.posts.forEach { post ->
                PostCard(post = post, onLikeClick = { viewModel.toggleLike(post) })
            }
        }
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
                    .background(if (selected) Accent else SurfaceCard)
                    .border(1.dp, if (selected) Accent else CardBorder, PillShape)
                    .clickable { onSelect(tab.tab) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(tab.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) OnAccent else TextMuted,
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
            .clip(MaterialTheme.shapes.large)
            .background(SurfaceCard)
            .border(1.dp, CardBorder, MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AccentSurfaceSelected),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = post.authorInitial, style = MaterialTheme.typography.titleSmall, color = Accent)
            }
            Column {
                Text(text = post.authorName, style = MaterialTheme.typography.titleSmall)
                Text(text = post.timeLabel, style = MaterialTheme.typography.labelMedium, color = TextFaint)
            }
        }
        Text(text = post.bodyText, style = MaterialTheme.typography.bodyMedium, color = TextBody)
        post.badgeText?.let { badge ->
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .border(1.dp, AccentBorder, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(text = badge, style = MaterialTheme.typography.labelSmall, color = Accent)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val likeCount = post.baseLikeCount + if (post.likedByUser) 1 else 0
            Box(
                modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp).clickable(onClick = onLikeClick),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "${if (post.likedByUser) "♥" else "♡"} ${formatVi(likeCount)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (post.likedByUser) Accent else TextFaint,
                )
            }
            Text(
                text = stringResource(
                    if (post.postType == CommunityPostType.QA) R.string.community_replies else R.string.community_comments,
                    post.commentCount,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = TextFaint,
            )
            if (post.hasBestAnswerMarker) {
                Text(text = stringResource(R.string.community_best_answer), style = MaterialTheme.typography.labelMedium, color = Accent)
            }
        }
    }
}
