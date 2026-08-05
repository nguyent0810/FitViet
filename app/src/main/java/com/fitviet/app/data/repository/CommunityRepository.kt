package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.CommunityPostDao
import com.fitviet.app.data.local.entity.CommunityPostEntity
import kotlinx.coroutines.flow.Flow

class CommunityRepository(private val dao: CommunityPostDao) {
    fun observe(): Flow<List<CommunityPostEntity>> = dao.observeAll()

    suspend fun toggleLike(post: CommunityPostEntity) = dao.setLiked(post.id, !post.likedByUser)
}
