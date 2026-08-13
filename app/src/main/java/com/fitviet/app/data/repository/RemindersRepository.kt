package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.ReminderDao
import com.fitviet.app.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

/** Backs `ui/reminders/RemindersScreen.kt` (feature #5, Gate 38). Thin wrapper — see
 * [ReminderEntity]'s doc comment for this feature's "persists/renders only, nothing fires yet"
 * scope boundary. */
class RemindersRepository(private val reminderDao: ReminderDao) {
    fun observeAll(): Flow<List<ReminderEntity>> = reminderDao.observeAll()

    /** Mon-Fri is a friendlier starting point than [ReminderEntity]'s own no-days-selected default
     * — a freshly-added reminder that already looks inert (no days highlighted) would read as
     * broken on first touch. */
    suspend fun addReminder() = reminderDao.insert(ReminderEntity(daysOfWeek = DEFAULT_NEW_REMINDER_DAYS))

    /** Re-reads the current row by [id] before applying [transform] — a stale [ReminderEntity]
     * snapshot captured by a Compose closure (this card has several independently-tappable
     * controls: day pills, the enabled pill, snooze, time sheet) must never overwrite a sibling
     * field a concurrent tap already changed. No-ops if the row was deleted concurrently. Same
     * re-read-at-write-time shape as [ProfileRepository]'s `updateSettings`/
     * [OnboardingRepository]'s `updateSelectedLevel`. */
    suspend fun update(id: Long, transform: (ReminderEntity) -> ReminderEntity) {
        val current = reminderDao.getById(id) ?: return
        reminderDao.update(transform(current))
    }

    suspend fun delete(reminder: ReminderEntity) = reminderDao.delete(reminder)

    private companion object {
        val DEFAULT_NEW_REMINDER_DAYS = listOf(1, 2, 3, 4, 5)
    }
}
