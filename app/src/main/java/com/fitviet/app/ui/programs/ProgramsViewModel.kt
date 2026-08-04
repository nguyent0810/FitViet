package com.fitviet.app.ui.programs

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.data.repository.ExerciseRepository
import com.fitviet.app.data.repository.ProgramRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/** Filter chip tag; null means "Tất cả" (no tag filter). */
data class ProgramFilter(@StringRes val labelRes: Int, val tag: String?)

val PROGRAM_FILTERS = listOf(
    ProgramFilter(R.string.programs_filter_all, null),
    ProgramFilter(R.string.programs_filter_muscle_gain, "Tăng cơ"),
    ProgramFilter(R.string.programs_filter_fat_loss, "Giảm mỡ"),
    ProgramFilter(R.string.programs_filter_home, "Tại nhà"),
    ProgramFilter(R.string.programs_filter_gym, "Phòng gym"),
)

data class ProgramsUiState(
    val searchQuery: String = "",
    val selectedFilterIndex: Int = 0,
    val visiblePrograms: List<ProgramEntity> = emptyList(),
    /** Only populated while actively searching — the placeholder text says "Tìm giáo án, bài tập…". */
    val matchingExercises: List<ExerciseEntity> = emptyList(),
)

class ProgramsViewModel(
    programRepository: ProgramRepository,
    exerciseRepository: ExerciseRepository,
    databaseReady: Deferred<Unit>,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedFilterIndex = MutableStateFlow(0)

    // Awaits seeding first — a one-shot read taken before the async seed transaction commits
    // would otherwise emit an empty exercise list once and never refresh (unlike the programs
    // Flow above, which is Room-observed and updates on its own).
    private val allExercises = flow {
        databaseReady.await()
        emit(exerciseRepository.getAll())
    }

    val uiState: StateFlow<ProgramsUiState> = combine(
        programRepository.observeAll(),
        searchQuery,
        selectedFilterIndex,
        allExercises,
    ) { programs, query, filterIndex, exercises ->
        val tag = PROGRAM_FILTERS[filterIndex].tag
        val visiblePrograms = programs.filter { program ->
            (tag == null || program.tags.contains(tag)) &&
                (query.isBlank() || program.titleVi.contains(query, ignoreCase = true))
        }
        val matchingExercises = if (query.isBlank()) {
            emptyList()
        } else {
            exercises.filter { it.nameVi.contains(query, ignoreCase = true) }
        }
        ProgramsUiState(
            searchQuery = query,
            selectedFilterIndex = filterIndex,
            visiblePrograms = visiblePrograms,
            matchingExercises = matchingExercises,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgramsUiState())

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onFilterSelected(index: Int) {
        selectedFilterIndex.value = index
    }

    class Factory(
        private val programRepository: ProgramRepository,
        private val exerciseRepository: ExerciseRepository,
        private val databaseReady: Deferred<Unit>,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProgramsViewModel(programRepository, exerciseRepository, databaseReady) as T
    }
}
