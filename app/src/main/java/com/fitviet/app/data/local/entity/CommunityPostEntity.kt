package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Values for [CommunityPostEntity.postType] — matches the prototype's `type` field on 1h. */
object CommunityPostType {
    const val SHARE = 0
    const val QA = 1
    const val PROGRESS = 2
}

/**
 * A post shown on the 1h Community feed. Community is the app's one "online" feature per the
 * README, but this app has no backend — posts are seeded once (as if synced before going offline)
 * and the like toggle is local-only, so the feed degrades to "read the last synced state" rather
 * than failing when offline, matching the spec's "must degrade gracefully offline" requirement.
 */
@Entity(tableName = "community_posts")
data class CommunityPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorInitial: String,
    val authorName: String,
    /** Static demo copy combining relative time + category, e.g. "2 giờ trước · Tiến bộ" — there's
     * no real post-creation flow to derive a live timestamp from (see [CommunityPostType] doc). */
    val timeLabel: String,
    val postType: Int,
    val bodyText: String,
    val badgeText: String? = null,
    val hasBestAnswerMarker: Boolean = false,
    /** Other users' likes, from seed data. Displayed count = this + 1 if [likedByUser]. */
    val baseLikeCount: Int,
    val likedByUser: Boolean = false,
    val commentCount: Int,
)
