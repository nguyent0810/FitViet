package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.ExerciseDao
import com.fitviet.app.data.local.dao.ProgramDao
import com.fitviet.app.data.local.dao.ProgramDayDao
import com.fitviet.app.data.local.dao.ProgramExerciseDao
import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.entity.ProgramDayEntity
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.data.local.entity.ProgramExerciseEntity
import com.fitviet.app.data.local.entity.SettingsEntity
import com.fitviet.app.domain.ProgramScheduleCalculator
import com.fitviet.app.domain.ProgramScheduleDay
import com.fitviet.app.domain.ProgramTransfer
import com.fitviet.app.domain.ProgramTransferData
import com.fitviet.app.domain.ProgramTransferDay
import com.fitviet.app.domain.ProgramTransferExercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Result of [ProgramRepository.importProgram] (feature #1). */
sealed interface ImportProgramResult {
    /** [skippedExerciseNames] are transfer rows whose [ProgramTransferExercise.nameVi] didn't
     * match any exercise in this device's own library — that day-slot is imported without them
     * rather than failing the whole import, matching this app's existing "library-driven, missing
     * content is a documented limitation, not a crash" convention (see e.g. Gate 15's no-equipment
     * program note). */
    data class Success(val programId: Long, val titleVi: String, val skippedExerciseNames: List<String>) : ImportProgramResult
    data object InvalidFormat : ImportProgramResult
}

class ProgramRepository(
    private val programDao: ProgramDao,
    private val programDayDao: ProgramDayDao,
    private val programExerciseDao: ProgramExerciseDao,
    private val exerciseDao: ExerciseDao,
    private val settingsDao: SettingsDao,
) {
    fun observeAll(): Flow<List<ProgramEntity>> = programDao.observeAll()

    suspend fun getById(id: Long): ProgramEntity? = programDao.getById(id)

    /** Empty until [com.fitviet.app.data.local.seed.DatabaseSeeder] has backfilled this program's
     * schedule rows — the UI falls back to an empty-state message in that case. */
    fun observeSchedule(programId: Long): Flow<List<ProgramScheduleDay>> = combine(
        programDayDao.observeForProgram(programId),
        programExerciseDao.observeForProgram(programId),
        exerciseDao.observeAll(),
        ProgramScheduleCalculator::build,
    )

    fun observeActiveProgramId(): Flow<Long?> = settingsDao.observe().map { it?.activeProgramId }

    suspend fun setActiveProgram(programId: Long) {
        val current = settingsDao.get() ?: SettingsEntity()
        settingsDao.upsert(current.copy(activeProgramId = programId))
    }

    /** Serializes a program's real weekly schedule to a shareable JSON string (feature #1). Null
     * if the program doesn't exist or its schedule hasn't been seeded/built yet — same "empty
     * until backfilled" state [observeSchedule] already documents. */
    suspend fun exportProgram(programId: Long): String? {
        val program = programDao.getById(programId) ?: return null
        val schedule = observeSchedule(programId).first()
        if (schedule.isEmpty()) return null
        return ProgramTransfer.encode(
            ProgramTransferData(
                titleVi = program.titleVi,
                durationWeeks = program.durationWeeks,
                sessionsPerWeek = program.sessionsPerWeek,
                level = program.level,
                equipment = program.equipment,
                days = schedule.map { day ->
                    ProgramTransferDay(
                        dayOfWeek = day.dayOfWeek.value,
                        titleVi = day.titleVi,
                        isRestDay = day.isRestDay,
                        exercises = day.exercises.map { exercise ->
                            ProgramTransferExercise(
                                nameVi = exercise.nameVi,
                                targetSets = exercise.targetSets,
                                targetRepsMin = exercise.targetRepsMin,
                                targetRepsMax = exercise.targetRepsMax,
                            )
                        },
                    )
                },
            ),
        )
    }

    /** Parses a shared/picked file's text as a FitViet program export and inserts it as a brand
     * new program (feature #1) — never overwrites an existing program, since there's no reliable
     * cross-install identity to match against. Exercises are resolved by name against this
     * device's own library; unmatched ones are dropped from their day (see [ImportProgramResult]). */
    suspend fun importProgram(json: String): ImportProgramResult {
        val data = ProgramTransfer.decode(json) ?: return ImportProgramResult.InvalidFormat
        val exercisesByName = exerciseDao.getAllOnce().associateBy { it.nameVi }
        val skipped = mutableListOf<String>()
        val programId = programDao.insert(
            ProgramEntity(
                titleVi = data.titleVi,
                imageAsset = "nhap-giao-an.jpg",
                durationWeeks = data.durationWeeks,
                sessionsPerWeek = data.sessionsPerWeek,
                level = data.level,
                equipment = data.equipment,
                tags = emptyList(),
            ),
        )
        data.days.forEach { day ->
            val dayId = programDayDao.insert(
                ProgramDayEntity(
                    programId = programId,
                    dayOfWeek = day.dayOfWeek,
                    titleVi = day.titleVi,
                    isRestDay = day.isRestDay,
                ),
            )
            val programExercises = day.exercises.mapIndexedNotNull { index, transferExercise ->
                val exercise = exercisesByName[transferExercise.nameVi]
                if (exercise == null) {
                    skipped += transferExercise.nameVi
                    null
                } else {
                    ProgramExerciseEntity(
                        programDayId = dayId,
                        exerciseId = exercise.id,
                        orderIndex = index,
                        targetSets = transferExercise.targetSets,
                        targetRepsMin = transferExercise.targetRepsMin,
                        targetRepsMax = transferExercise.targetRepsMax,
                    )
                }
            }
            if (programExercises.isNotEmpty()) {
                programExerciseDao.insertAll(programExercises)
            }
        }
        return ImportProgramResult.Success(programId, data.titleVi, skipped.distinct())
    }
}
