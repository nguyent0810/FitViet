package com.fitviet.app.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.dao.PersonalBestRow
import com.fitviet.app.data.repository.DiaryRepository
import com.fitviet.app.domain.MuscleGroupWorkload
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val TOP_PERSONAL_BESTS_DISPLAY_LIMIT = 3

data class WeeklyRecapUiState(
    val hasData: Boolean = false,
    val weekStart: LocalDate? = null,
    val weekEnd: LocalDate? = null,
    val sessionsThisWeek: Int = 0,
    val volumeThisWeekKg: Double = 0.0,
    /** Same trailing-window data [com.fitviet.app.ui.diary.DiaryScreen]'s own
     * `MuscleGroupWorkloadCard` shows — NOT recomputed as a "this week only" breakdown, since no
     * per-week muscle-group aggregate exists separately from [DiaryRepository]'s own window. */
    val muscleGroupWorkload: List<MuscleGroupWorkload> = emptyList(),
    /** All-time bests, labeled "current" rather than "set this week" — [PersonalBestRow] carries
     * no date, so which ones were actually achieved in this specific week isn't derivable here. */
    val topPersonalBests: List<PersonalBestRow> = emptyList(),
)

/**
 * Gate D3 — the Weekly Recap screen reuses [DiaryRepository.observe]'s already-computed data
 * rather than adding a new query: [com.fitviet.app.domain.WeekVolume] (via `last4Weeks.last()`,
 * the same "index 3 = this week" convention [DiaryViewModel]'s own `selectedWeekIndex` default
 * already uses) for the week's total volume, and a session count derived by matching each
 * completed session's date against that week's [weekStart, weekStart+6] range.
 */
class WeeklyRecapViewModel(repository: DiaryRepository) : ViewModel() {
    val uiState: StateFlow<WeeklyRecapUiState> = repository.observe().map { data ->
        val thisWeek = data.last4Weeks.lastOrNull()
        val weekStart = thisWeek?.weekStart
        val weekEnd = weekStart?.plusDays(6)
        val zone = ZoneId.systemDefault()
        val sessionsThisWeek = if (weekStart != null && weekEnd != null) {
            data.completedSessions.count { session ->
                val completedAt = session.completedAt ?: return@count false
                val date = Instant.ofEpochMilli(completedAt).atZone(zone).toLocalDate()
                date in weekStart..weekEnd
            }
        } else {
            0
        }
        WeeklyRecapUiState(
            hasData = sessionsThisWeek > 0,
            weekStart = weekStart,
            weekEnd = weekEnd,
            sessionsThisWeek = sessionsThisWeek,
            volumeThisWeekKg = thisWeek?.volumeKg ?: 0.0,
            muscleGroupWorkload = data.muscleGroupWorkload,
            topPersonalBests = data.personalBests.take(TOP_PERSONAL_BESTS_DISPLAY_LIMIT),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeklyRecapUiState())

    class Factory(private val repository: DiaryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = WeeklyRecapViewModel(repository) as T
    }
}
