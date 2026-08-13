package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.CommunityPostEntity
import com.fitviet.app.data.local.entity.CommunityPostType
import org.junit.Assert.assertEquals
import org.junit.Test

class CommunityFilterTest {

    private fun post(type: Int, category: Int? = null) = CommunityPostEntity(
        authorInitial = "X",
        authorName = "Test",
        timeLabel = "now",
        postType = type,
        bodyText = "body",
        baseLikeCount = 0,
        commentCount = 0,
        category = category,
    )

    private val posts = listOf(
        post(CommunityPostType.SHARE),
        post(CommunityPostType.QA),
        post(CommunityPostType.PROGRESS),
        post(CommunityPostType.WORKOUT_SHARE),
    )

    @Test
    fun `tab 0 (Moi nhat) returns every post, including ones with no dedicated tab`() {
        assertEquals(4, CommunityFilter.byTab(posts, tab = 0).size)
    }

    @Test
    fun `an untagged workout-share post has no dedicated tab, same as a generic share post`() {
        assertEquals(0, CommunityFilter.byTab(posts, tab = CommunityPostType.QA).count { it.postType == CommunityPostType.WORKOUT_SHARE })
        assertEquals(0, CommunityFilter.byTab(posts, tab = CommunityPostType.PROGRESS).count { it.postType == CommunityPostType.WORKOUT_SHARE })
    }

    @Test
    fun `a category-tagged workout-share post (Gate 6b) shows under its tagged tab, keeping its own postType`() {
        val tagged = posts + post(CommunityPostType.WORKOUT_SHARE, category = CommunityPostType.PROGRESS)

        val result = CommunityFilter.byTab(tagged, tab = CommunityPostType.PROGRESS)

        assertEquals(2, result.size)
        assertEquals(setOf(CommunityPostType.PROGRESS, CommunityPostType.WORKOUT_SHARE), result.map { it.postType }.toSet())
        assertEquals(0, CommunityFilter.byTab(tagged, tab = CommunityPostType.QA).count { it.postType == CommunityPostType.WORKOUT_SHARE })
    }

    @Test
    fun `category is only honored for WORKOUT_SHARE posts, not any other postType`() {
        // A non-WORKOUT_SHARE post's own category (if it ever had one) must not leak it into a tab
        // its real postType doesn't belong to — the postType == WORKOUT_SHARE guard is what
        // enforces that, not just the category comparison alone.
        val stray = posts + post(CommunityPostType.SHARE, category = CommunityPostType.QA)

        val result = CommunityFilter.byTab(stray, tab = CommunityPostType.QA)

        assertEquals(1, result.size)
        assertEquals(CommunityPostType.QA, result.single().postType)
    }

    @Test
    fun `a category = SHARE workout-share post only ever surfaces under Moi nhat`() {
        // SHARE == 0, the same index as the "Moi nhat" tab, but tab 0 always takes the unconditional
        // `posts` branch before the category clause runs — so a SHARE-tagged share can never match
        // via `it.category == tab` (there is no dedicated "Chia sẻ" tab to match into either way).
        val tagged = posts + post(CommunityPostType.WORKOUT_SHARE, category = CommunityPostType.SHARE)

        assertEquals(5, CommunityFilter.byTab(tagged, tab = 0).size)
        assertEquals(0, CommunityFilter.byTab(tagged, tab = CommunityPostType.QA).count { it.category == CommunityPostType.SHARE })
        assertEquals(0, CommunityFilter.byTab(tagged, tab = CommunityPostType.PROGRESS).count { it.category == CommunityPostType.SHARE })
    }

    @Test
    fun `Hoi dap tab returns only QA posts`() {
        val result = CommunityFilter.byTab(posts, tab = CommunityPostType.QA)

        assertEquals(1, result.size)
        assertEquals(CommunityPostType.QA, result.single().postType)
    }

    @Test
    fun `Tien bo tab returns only progress posts`() {
        val result = CommunityFilter.byTab(posts, tab = CommunityPostType.PROGRESS)

        assertEquals(1, result.size)
        assertEquals(CommunityPostType.PROGRESS, result.single().postType)
    }
}
