package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.CommunityPostEntity
import com.fitviet.app.data.local.entity.CommunityPostType

/**
 * Filters the 1h feed for the selected tab. Tab index 0 ("Mới nhất") matches every post — including
 * ones like "Chia sẻ" that have no dedicated tab of their own — tabs 1/2 ("Hỏi đáp"/"Tiến bộ") match
 * that exact [CommunityPostEntity.postType]. Matches the prototype's `commTab === 0 || p.type === commTab`.
 *
 * Redesign Gate 6b — a [CommunityPostType.WORKOUT_SHARE] post also matches tabs 1/2 when its own
 * [CommunityPostEntity.category] equals the tab. This is this app's own extension, not a mock-literal
 * one — the Hit & Run redesign mock's own community feed data has only a `cat` field and no `type`
 * at all, so it has no orthogonal postType-vs-category split to mirror (its `p3` example, `cat:'Mới
 * nhất'` on a "Chia sẻ"-labeled post, is that mock's fallback case, not precedent for a second
 * dimension). Same "app extension beyond the mock" framing `WorkoutSharePostCard`'s own doc already
 * uses for its stat grid. [postType] itself never changes for a shared workout, so it keeps that
 * card's stat-grid rendering regardless of which category tag it carries — [category] and [postType]
 * both key off the same [CommunityPostType] int space by convenience, not by any enforced contract.
 */
object CommunityFilter {
    fun byTab(posts: List<CommunityPostEntity>, tab: Int): List<CommunityPostEntity> =
        if (tab == 0) {
            posts
        } else {
            posts.filter { it.postType == tab || (it.postType == CommunityPostType.WORKOUT_SHARE && it.category == tab) }
        }
}
