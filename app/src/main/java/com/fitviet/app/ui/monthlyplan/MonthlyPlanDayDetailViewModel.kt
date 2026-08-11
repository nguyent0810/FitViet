package com.fitviet.app.ui.monthlyplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.repository.ExerciseRepository
import com.fitviet.app.data.repository.MonthlyPlanRepository
import com.fitviet.app.data.repository.RegenerateResult
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MonthlyPlanDayExerciseUi(
    val monthlyPlanExerciseId: Long,
    val exercise: ExerciseEntity,
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val targetWeightKg: Double?,
)

data class MonthlyPlanDayDetailUiState(
    val isLoading: Boolean = true,
    val date: LocalDate = LocalDate.now(),
    val dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val sessionType: String? = null,
    val isRestDay: Boolean = false,
    val isLocked: Boolean = false,
    val exercises: List<MonthlyPlanDayExerciseUi> = emptyList(),
    val isRegeneratingDay: Boolean = false,
    val swappingExerciseIds: Set<Long> = emptySet(),
    val lockedActionMessage: Boolean = false,
)

/** "Hit & Run" (Gate 63+) Regenerate UI — one day's exercises, reached by tapping a day row on
 * [MonthlyPlanDetailScreen]. Hosts the day-level and per-exercise regenerate actions the plan's
 * scope calls for (regenerate day, swap one exercise, swap avoiding an exercise's own equipment).
 * [dayId] is fixed for this ViewModel's lifetime — a different day is a different screen instance
 * (matches [com.fitviet.app.ui.workout.MonthlyPlanDayWorkoutPlanner]'s own "one specific day"
 * framing), so there's no dayId-switching to handle here. */
class MonthlyPlanDayDetailViewModel(
    private val dayId: Long,
    private val repository: MonthlyPlanRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val catalogById = MutableStateFlow<Map<Long, ExerciseEntity>?>(null)

    init {
        viewModelScope.launch {
            catalogById.value = exerciseRepository.getAll().associateBy { it.id }
        }
    }

    private val actionState = MutableStateFlow(ActionState())
    private data class ActionState(
        val isRegeneratingDay: Boolean = false,
        val swappingExerciseIds: Set<Long> = emptySet(),
        val lockedActionMessage: Boolean = false,
    )

    val uiState: StateFlow<MonthlyPlanDayDetailUiState> = combine(
        repository.observeDay(dayId),
        repository.observeExercisesForDay(dayId),
        repository.observeIsDayLocked(dayId),
        catalogById,
        actionState,
    ) { day, planExercises, isLocked, catalog, action ->
        if (day == null || catalog == null) {
            MonthlyPlanDayDetailUiState(isLoading = true)
        } else {
            MonthlyPlanDayDetailUiState(
                isLoading = false,
                date = LocalDate.ofEpochDay(day.effectiveEpochDay),
                dayOfWeek = DayOfWeek.of(day.dayOfWeek),
                sessionType = day.sessionType,
                isRestDay = day.isRestDay,
                isLocked = isLocked,
                exercises = planExercises.mapNotNull { pe ->
                    catalog[pe.exerciseId]?.let { exercise ->
                        MonthlyPlanDayExerciseUi(
                            monthlyPlanExerciseId = pe.id,
                            exercise = exercise,
                            targetSets = pe.targetSets,
                            targetRepsMin = pe.targetRepsMin,
                            targetRepsMax = pe.targetRepsMax,
                            targetWeightKg = pe.targetWeightKg,
                        )
                    }
                },
                isRegeneratingDay = action.isRegeneratingDay,
                swappingExerciseIds = action.swappingExerciseIds,
                lockedActionMessage = action.lockedActionMessage,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlyPlanDayDetailUiState())

    fun dismissLockedMessage() {
        actionState.value = actionState.value.copy(lockedActionMessage = false)
    }

    fun regenerateDay() {
        if (actionState.value.isRegeneratingDay) return
        actionState.value = actionState.value.copy(isRegeneratingDay = true)
        viewModelScope.launch {
            val result = repository.regenerateDay(dayId, LocalDate.now())
            actionState.value = actionState.value.copy(
                isRegeneratingDay = false,
                lockedActionMessage = result == RegenerateResult.Locked,
            )
        }
    }

    fun swapExercise(monthlyPlanExerciseId: Long, avoidEquipment: String? = null) {
        if (monthlyPlanExerciseId in actionState.value.swappingExerciseIds) return
        actionState.value = actionState.value.copy(swappingExerciseIds = actionState.value.swappingExerciseIds + monthlyPlanExerciseId)
        viewModelScope.launch {
            val result = repository.swapExercise(monthlyPlanExerciseId, avoidEquipment)
            actionState.value = actionState.value.copy(
                swappingExerciseIds = actionState.value.swappingExerciseIds - monthlyPlanExerciseId,
                lockedActionMessage = result == RegenerateResult.Locked,
            )
        }
    }

    class Factory(
        private val dayId: Long,
        private val repository: MonthlyPlanRepository,
        private val exerciseRepository: ExerciseRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MonthlyPlanDayDetailViewModel(dayId, repository, exerciseRepository) as T
    }
}
