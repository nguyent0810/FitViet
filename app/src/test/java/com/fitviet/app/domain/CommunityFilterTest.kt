package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.CommunityPostEntity
import com.fitviet.app.data.local.entity.CommunityPostType
import org.junit.Assert.assertEquals
import org.junit.Test

class CommunityFilterTest {

    private fun post(type: Int) = CommunityPostEntity(
        authorInitial = "X",
        authorName = "Test",
        timeLabel = "now",
        postType = type,
        bodyText = "body",
        baseLikeCount = 0,
        commentCount = 0,
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
    fun `workout-share posts (Gate 40-41) have no dedicated tab, same as generic share posts`() {
        assertEquals(0, CommunityFilter.byTab(posts, tab = CommunityPostType.QA).count { it.postType == CommunityPostType.WORKOUT_SHARE })
        assertEquals(0, CommunityFilter.byTab(posts, tab = CommunityPostType.PROGRESS).count { it.postType == CommunityPostType.WORKOUT_SHARE })
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
