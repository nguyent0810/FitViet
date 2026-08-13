package com.fitviet.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.repository.ExerciseRepository
import com.fitviet.app.data.repository.ProgramRepository
import com.fitviet.app.data.repository.WorkoutRepository
import com.fitviet.app.domain.NextTrainingCalculator
import java.time.LocalDate
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WorkoutPreviewUiState(
    val isLoading: Boolean = true,
    val dayTitleVi: String = "",
    /** Already resolved into solo/paired groupings (Gate 48) — the same result
     * [ProgramDayWorkoutPlanner.buildBlocks] would turn into session blocks, so this screen renders
     * exactly the pairing the live session will use rather than re-deriving it. */
    val groupings: List<ResolvedGrouping> = emptyList(),
    val showSupersetHint: Boolean = false,
    /** [ProgramDayWorkoutPlanner.estimateDurationMinutes] over [groupings] — 0 when there's nothing
     * to estimate yet (still loading, or an empty day). */
    val estimatedDurationMinutes: Int = 0,
)

/** Backs the "Xem trước" (preview) screen for a sample program (Gate 24, demoted to an optional
 * read-only link by redesign Gate 2b — see `ProgramsListScreen`'s own doc). Resolves the nearest
 * upcoming training day in the program's schedule (today if it's a training day, otherwise the
 * next non-rest day — [NextTrainingCalculator.findNext]'s same lookahead the old hero card used)
 * rather than strictly today: since Gate 2b retired the Weekly Schedule day-list this screen used
 * to be reachable from, the only remaining entry point is a program card's "Xem trước" link, which
 * always means "show me what this program actually looks like," not "show me today specifically" —
 * strictly-today would land on an empty screen on any of a program's rest days (most seed programs
 * rest more than half the week). Purely a look, never a "start" action — see `ProgramsListScreen`'s
 * doc for why generation is the card's own tap, not this screen's job. */
class WorkoutPreviewViewModel(
    private val programId: Long,
    private val programRepository: ProgramRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val databaseReady: Deferred<Unit>,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutPreviewUiState())
    val uiState: StateFlow<WorkoutPreviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            databaseReady.await()
            val schedule = programRepository.observeSchedule(programId).first()
            val nextTraining = NextTrainingCalculator.findNext(schedule, LocalDate.now().dayOfWeek)
            val resolved = nextTraining?.let {
                ProgramDayWorkoutPlanner.resolveDay(programId, it.day.dayOfWeek, programRepository, exerciseRepository, workoutRepository)
            }
            val groupings = ProgramDayWorkoutPlanner.resolveGroupings(resolved?.items.orEmpty())
            val hasSeenHint = programRepository.observeHasSeenSupersetHint().first()
            _uiState.value = WorkoutPreviewUiState(
                isLoading = false,
                dayTitleVi = resolved?.titleVi.orEmpty(),
                groupings = groupings,
                // No point showing the explainer on a day with no superset to explain, even if the
                // user hasn't dismissed it yet.
                showSupersetHint = !hasSeenHint && groupings.any { it is ResolvedGrouping.Paired },
                estimatedDurationMinutes = if (groupings.isEmpty()) 0 else ProgramDayWorkoutPlanner.estimateDurationMinutes(groupings),
            )
        }
    }

    fun dismissSupersetHint() {
        _uiState.value = _uiState.value.copy(showSupersetHint = false)
        // NonCancellable: without it, backing out of this screen right after tapping dismiss
        // would cancel viewModelScope mid-write, losing the persisted flag even though the
        // in-memory flip above already happened — the hint would silently reappear next visit.
        viewModelScope.launch { withContext(NonCancellable) { programRepository.dismissSupersetHint() } }
    }

    class Factory(
        private val programId: Long,
        private val programRepository: ProgramRepository,
        private val exerciseRepository: ExerciseRepository,
        private val workoutRepository: WorkoutRepository,
        private val databaseReady: Deferred<Unit>,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WorkoutPreviewViewModel(programId, programRepository, exerciseRepository, workoutRepository, databaseReady) as T
    }
}
