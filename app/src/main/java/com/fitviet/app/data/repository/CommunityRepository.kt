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
     * actually came from the signed-in user.
     *
     * Redesign Gate 6b — [userText]/[category] are the share composer's own inputs (Gate 6c wires
     * the actual composer; until then every call site omits both, so the fallback static copy below
     * is what every current share gets). [category] omitted means a post visible only under "Mới
     * nhất" per [CommunityPostEntity.category]'s own doc. [userText] falls back to the static copy
     * when null or blank rather than posting an empty body — no `$dayLabel` in that fallback despite
     * `WorkoutSharePostCard` rendering `bodyText` right above the stat panel's own day-label line
     * (Gate 6b) — repeating it there would read like the same fact stated twice. */
    suspend fun shareWorkout(
        dayLabel: String,
        durationSeconds: Int,
        totalVolumeKg: Double,
        streakDays: Int,
        userText: String? = null,
        category: Int? = null,
    ): Long {
        val settings = settingsDao.get() ?: SettingsEntity()
        val post = CommunityPostEntity(
            authorInitial = settings.displayName.trim().firstOrNull()?.uppercase() ?: "?",
            authorName = settings.displayName,
            timeLabel = "Vừa xong",
            postType = CommunityPostType.WORKOUT_SHARE,
            bodyText = userText?.trim()?.takeIf { it.isNotEmpty() } ?: "Vừa hoàn thành buổi tập!",
            baseLikeCount = 0,
            commentCount = 0,
            dayLabel = dayLabel,
            durationSeconds = durationSeconds,
            totalVolumeKg = totalVolumeKg,
            streakDays = streakDays,
            category = category,
        )
        return dao.insert(post)
    }
}
