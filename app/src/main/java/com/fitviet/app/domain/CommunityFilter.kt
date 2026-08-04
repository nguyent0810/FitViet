package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.CommunityPostEntity

/**
 * Filters the 1h feed for the selected tab. Tab index 0 ("Mới nhất") matches every post — including
 * ones like "Chia sẻ" that have no dedicated tab of their own — tabs 1/2 ("Hỏi đáp"/"Tiến bộ") match
 * that exact [CommunityPostEntity.postType]. Matches the prototype's `commTab === 0 || p.type === commTab`.
 */
object CommunityFilter {
    fun byTab(posts: List<CommunityPostEntity>, tab: Int): List<CommunityPostEntity> =
        if (tab == 0) posts else posts.filter { it.postType == tab }
}
