package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Values for [CommunityPostEntity.postType] — matches the prototype's `type` field on 1h. */
object CommunityPostType {
    const val SHARE = 0
    const val QA = 1
    const val PROGRESS = 2
    /** Feature #4 (Gate 40) — a real post created from a finished [com.fitviet.app.ui.workout.WorkoutViewModel]
     * session, distinct from [SHARE]'s generic seeded freeform posts. Populates the 4 stat columns
     * below (`dayLabel`/`durationSeconds`/`totalVolumeKg`/`streakDays`) directly; `category` is
     * populated by the share composer (Gate 6c), while [badgeText] (Gate 6d) is populated by the
     * session-completion path itself — `WorkoutRepository.findSessionPersonalRecord`, not anything
     * the user chooses in the composer. */
    const val WORKOUT_SHARE = 3
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
    /** [WORKOUT_SHARE]-only columns below, null for every other [postType]. */
    val dayLabel: String? = null,
    val durationSeconds: Int? = null,
    val totalVolumeKg: Double? = null,
    val streakDays: Int? = null,
    /** Redesign Gate 6b/6c — the share composer's category pill (Tiến bộ/Hỏi đáp/Chia sẻ, i.e.
     * [CommunityPostType.PROGRESS]/[CommunityPostType.QA]/[CommunityPostType.SHARE]), tagged onto
     * a [WORKOUT_SHARE] post without changing its own [postType] — [postType] stays [WORKOUT_SHARE]
     * so the post keeps its stat-grid rendering (`WorkoutSharePostCard`, an app extension beyond
     * the mock's own plain-text feed), while [category] alone drives `CommunityFilter.byTab`'s
     * PROGRESS/QA tab membership. Every share created via `ShareComposerOverlay` (Gate 6c) now
     * carries a non-null value, defaulting to [CommunityPostType.PROGRESS]; this column stays
     * nullable because a [WORKOUT_SHARE] post with no category (any future code path that omits
     * it) shows only under "Mới nhất", matching a [WORKOUT_SHARE] post's pre-Gate-6b behavior. */
    val category: Int? = null,
)
