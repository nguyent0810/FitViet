package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fitviet.app.data.local.entity.CommunityPostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunityPostDao {
    @Query("SELECT * FROM community_posts ORDER BY id")
    fun observeAll(): Flow<List<CommunityPostEntity>>

    @Query("SELECT COUNT(*) FROM community_posts")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(posts: List<CommunityPostEntity>)

    @Insert
    suspend fun insert(post: CommunityPostEntity): Long

    @Query("UPDATE community_posts SET likedByUser = :liked WHERE id = :id")
    suspend fun setLiked(id: Long, liked: Boolean)
}
