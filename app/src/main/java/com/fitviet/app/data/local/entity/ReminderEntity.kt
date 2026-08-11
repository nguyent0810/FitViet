package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A workout-reminder row (feature #5, Gate 38). **Persists and renders state only** — there is no
 * `WorkManager`/notification-channel scheduling anywhere in this app yet, so nothing here ever
 * actually fires a notification. This is a deliberate, disclosed scope boundary (see PROGRESS.md),
 * not an oversight: building a real background scheduler is a distinct gate of its own.
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int = DEFAULT_HOUR,
    val minute: Int = DEFAULT_MINUTE,
    /** Optional free-text tying this reminder to a specific workout day (e.g. "Đẩy 1 · Ngực ưu
     * tiên + vai + tay sau"), shown under the time/days row per the mockup. Blank (not null) means
     * "no label set" — this feature has no real link to a program day yet (Gate 38's scope), so the
     * user just types whatever they want here; it's purely descriptive, never resolved against
     * program data. */
    val label: String = "",
    /** ISO weekday values (1=Monday..7=Sunday), always stored sorted ascending. Empty is a
     * genuinely inert reminder (no day selected yet) rather than an implied "every day" — matches
     * this app's existing "explicit over inferred" convention. */
    val daysOfWeek: List<Int> = emptyList(),
    val enabled: Boolean = true,
    /** Non-null while this reminder is snoozed for its next occurrence. Toggled manually by the
     * same "Hoãn" action that sets it — with no scheduler to evaluate dates against, nothing would
     * ever clear this automatically, so it isn't pretended to. */
    val snoozedUntilEpochDay: Long? = null,
) {
    companion object {
        const val DEFAULT_HOUR = 7
        const val DEFAULT_MINUTE = 0
    }
}
