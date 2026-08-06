package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.entity.ProgramDayEntity
import com.fitviet.app.data.local.entity.ProgramExerciseEntity
import java.time.DayOfWeek

/** One exercise target within a [ProgramScheduleDay], joined to its real [ExerciseEntity]. */
data class ProgramScheduleExercise(
    val exerciseId: Long,
    val nameVi: String,
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    /** See [com.fitviet.app.data.local.entity.ProgramExerciseEntity.supersetGroup]. */
    val supersetGroup: String? = null,
)

/** One calendar-weekday row of a program's real weekly schedule (2b), replacing the old static
 * one-size-fits-all reference week (see PROGRESS.md, Gate 15). */
data class ProgramScheduleDay(
    val dayOfWeek: DayOfWeek,
    val titleVi: String,
    val isRestDay: Boolean,
    val exercises: List<ProgramScheduleExercise>,
)

/**
 * Joins a program's raw day/exercise-target rows against the exercise catalog into ordered,
 * display-ready [ProgramScheduleDay]s. Pure and testable: no Room/Compose dependency, mirrors the
 * [WeightHistoryCalculator]/[WorkoutCalendarCalculator] pattern of keeping join/grouping logic out
 * of the repository's reactive `combine {}` lambda.
 */
object ProgramScheduleCalculator {
    fun build(
        days: List<ProgramDayEntity>,
        programExercises: List<ProgramExerciseEntity>,
        exercises: List<ExerciseEntity>,
    ): List<ProgramScheduleDay> {
        val exercisesById = exercises.associateBy { it.id }
        val exercisesByDay = programExercises.groupBy { it.programDayId }
        return days.sortedBy { it.dayOfWeek }.map { day ->
            val dayExercises = exercisesByDay[day.id].orEmpty()
                .sortedBy { it.orderIndex }
                .mapNotNull { programExercise ->
                    val exercise = exercisesById[programExercise.exerciseId] ?: return@mapNotNull null
                    ProgramScheduleExercise(
                        exerciseId = exercise.id,
                        nameVi = exercise.nameVi,
                        targetSets = programExercise.targetSets,
                        targetRepsMin = programExercise.targetRepsMin,
                        targetRepsMax = programExercise.targetRepsMax,
                        supersetGroup = programExercise.supersetGroup,
                    )
                }
            ProgramScheduleDay(
                dayOfWeek = DayOfWeek.of(day.dayOfWeek),
                titleVi = day.titleVi,
                isRestDay = day.isRestDay,
                exercises = dayExercises,
            )
        }
    }
}
