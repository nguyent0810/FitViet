package com.fitviet.app.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.ReminderEntity
import com.fitviet.app.data.repository.RemindersRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RemindersUiState(
    val reminders: List<ReminderEntity> = emptyList(),
    /** Non-null while the "Đổi giờ" bottom sheet is open for this reminder. */
    val editingReminder: ReminderEntity? = null,
)

class RemindersViewModel(private val repository: RemindersRepository) : ViewModel() {
    private val editingReminder = MutableStateFlow<ReminderEntity?>(null)

    val uiState: StateFlow<RemindersUiState> = combine(
        repository.observeAll(), editingReminder,
    ) { reminders, editing ->
        RemindersUiState(reminders = reminders, editingReminder = editing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersUiState())

    fun addReminder() = viewModelScope.launch { repository.addReminder() }

    fun deleteReminder(reminder: ReminderEntity) = viewModelScope.launch { repository.delete(reminder) }

    /** Disabling/re-enabling is a full reset — it also clears any snooze, otherwise re-enabling
     * a snoozed reminder would silently land back on "Đã hoãn" instead of "Bật". */
    fun toggleEnabled(reminder: ReminderEntity) = viewModelScope.launch {
        repository.update(reminder.copy(enabled = !reminder.enabled, snoozedUntilEpochDay = null))
    }

    /** Snoozing/un-snoozing is a manual toggle, not date-evaluated — see [ReminderEntity]'s doc
     * comment on why nothing clears this automatically. */
    fun toggleSnooze(reminder: ReminderEntity) = viewModelScope.launch {
        val newValue = if (reminder.snoozedUntilEpochDay != null) null else LocalDate.now().toEpochDay()
        repository.update(reminder.copy(snoozedUntilEpochDay = newValue))
    }

    fun toggleDay(reminder: ReminderEntity, isoDayOfWeek: Int) = viewModelScope.launch {
        val days = if (isoDayOfWeek in reminder.daysOfWeek) {
            reminder.daysOfWeek - isoDayOfWeek
        } else {
            (reminder.daysOfWeek + isoDayOfWeek).sorted()
        }
        repository.update(reminder.copy(daysOfWeek = days))
    }

    fun openTimeSheet(reminder: ReminderEntity) {
        editingReminder.value = reminder
    }

    fun dismissTimeSheet() {
        editingReminder.value = null
    }

    fun saveTime(hour: Int, minute: Int) {
        val reminder = editingReminder.value ?: return
        viewModelScope.launch { repository.update(reminder.copy(hour = hour, minute = minute)) }
        editingReminder.value = null
    }

    class Factory(private val repository: RemindersRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RemindersViewModel(repository) as T
    }
}
