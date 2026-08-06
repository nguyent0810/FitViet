package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.CommunityPostDao
import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.entity.CommunityPostEntity
import com.fitviet.app.data.local.entity.CommunityPostType
import com.fitviet.app.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

class CommunityRepository(
    private val dao: CommunityPostDao,
    private val settingsDao: SettingsDao,
) {
    fun observe(): Flow<List<CommunityPostEntity>> = dao.observeAll()

    suspend fun toggleLike(post: CommunityPostEntity) = dao.setLiked(post.id, !post.likedByUser)

    /** Feature #4 (Gate 40) — the only real single-post creation path this repository has; every
     * other post comes from the one-time seeder's [CommunityPostDao.insertAll]. Author identity is
     * read from the persisted profile (Gate 35), not a placeholder, so a shared post looks like it
     * actually came from the signed-in user. */
    suspend fun shareWorkout(
        programTitle: String?,
        dayLabel: String,
        durationSeconds: Int,
        totalVolumeKg: Double,
        streakDays: Int,
    ): Long {
        val settings = settingsDao.get() ?: SettingsEntity()
        val post = CommunityPostEntity(
            authorInitial = settings.displayName.trim().firstOrNull()?.uppercase() ?: "?",
            authorName = settings.displayName,
            timeLabel = "Vừa xong",
            postType = CommunityPostType.WORKOUT_SHARE,
            bodyText = "Vừa hoàn thành buổi tập \"$dayLabel\"!",
            baseLikeCount = 0,
            commentCount = 0,
            programTitle = programTitle,
            dayLabel = dayLabel,
            durationSeconds = durationSeconds,
            totalVolumeKg = totalVolumeKg,
            streakDays = streakDays,
        )
        return dao.insert(post)
    }
}
