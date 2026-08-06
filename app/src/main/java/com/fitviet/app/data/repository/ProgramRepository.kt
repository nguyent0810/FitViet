package com.fitviet.app.data.repository

import androidx.room.withTransaction
import com.fitviet.app.data.local.FitVietDatabase
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
import kotlinx.coroutines.CancellationException
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
    /** An otherwise-valid file failed partway through the database write (e.g. a disk/storage
     * error) — the transaction guarantees nothing was left half-inserted, but the user still needs
     * to be told the import didn't happen rather than the app silently crashing. */
    data object Failed : ImportProgramResult
}

interface ProgramRepository {
    fun observeAll(): Flow<List<ProgramEntity>>

    suspend fun getById(id: Long): ProgramEntity?

    /** Empty until [com.fitviet.app.data.local.seed.DatabaseSeeder] has backfilled this program's
     * schedule rows — the UI falls back to an empty-state message in that case. */
    fun observeSchedule(programId: Long): Flow<List<ProgramScheduleDay>>

    fun observeActiveProgramId(): Flow<Long?>

    suspend fun setActiveProgram(programId: Long)

    /** Serializes a program's real weekly schedule to a shareable JSON string (feature #1). Null
     * only if the program itself doesn't exist — a program with no schedule rows yet (not seeded,
     * or a zero-day program from [importProgram]) still exports as a valid empty-days JSON rather
     * than being a permanent dead end that can never be shared. */
    suspend fun exportProgram(programId: Long): String?

    /** Parses a shared/picked file's text as a FitViet program export and inserts it as a brand
     * new program (feature #1) — never overwrites an existing program, since there's no reliable
     * cross-install identity to match against. Exercises are resolved by name against this
     * device's own library; unmatched ones are dropped from their day (see [ImportProgramResult]). */
    suspend fun importProgram(json: String): ImportProgramResult
}

class RoomProgramRepository(
    private val database: FitVietDatabase,
    private val programDao: ProgramDao,
    private val programDayDao: ProgramDayDao,
    private val programExerciseDao: ProgramExerciseDao,
    private val exerciseDao: ExerciseDao,
    private val settingsDao: SettingsDao,
) : ProgramRepository {
    override fun observeAll(): Flow<List<ProgramEntity>> = programDao.observeAll()

    override suspend fun getById(id: Long): ProgramEntity? = programDao.getById(id)

    override fun observeSchedule(programId: Long): Flow<List<ProgramScheduleDay>> = combine(
        programDayDao.observeForProgram(programId),
        programExerciseDao.observeForProgram(programId),
        exerciseDao.observeAll(),
        ProgramScheduleCalculator::build,
    )

    override fun observeActiveProgramId(): Flow<Long?> = settingsDao.observe().map { it?.activeProgramId }

    override suspend fun setActiveProgram(programId: Long) {
        val current = settingsDao.get() ?: SettingsEntity()
        settingsDao.upsert(current.copy(activeProgramId = programId))
    }

    override suspend fun exportProgram(programId: Long): String? {
        val program = programDao.getById(programId) ?: return null
        val schedule = observeSchedule(programId).first()
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

    override suspend fun importProgram(json: String): ImportProgramResult {
        val data = ProgramTransfer.decode(json) ?: return ImportProgramResult.InvalidFormat
        val skipped = mutableListOf<String>()
        // All-or-nothing: a decode()-validated file can still fail partway through (e.g. a Room
        // constraint this method doesn't itself check, or a disk/storage error) — the transaction
        // guarantees no orphaned half-imported program, and the try/catch converts that failure
        // into a result the UI can show instead of an uncaught exception crashing the coroutine
        // that launched this suspend call.
        val programId = try {
            importTransaction(data, skipped)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return ImportProgramResult.Failed
        }
        return ImportProgramResult.Success(programId, data.titleVi, skipped.distinct())
    }

    private suspend fun importTransaction(data: ProgramTransferData, skipped: MutableList<String>): Long =
        database.withTransaction {
            val exercisesByName = exerciseDao.getAllOnce().associateBy { it.nameVi }
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
                val resolvedExercises = day.exercises.mapNotNull { transferExercise ->
                    val exercise = exercisesByName[transferExercise.nameVi]
                    if (exercise == null) {
                        skipped += transferExercise.nameVi
                        null
                    } else {
                        transferExercise to exercise
                    }
                }
                // A training day whose every exercise failed name-resolution has nothing left to
                // schedule — drop the whole day rather than insert an empty training day. Keeps
                // "every non-rest day has >=1 exercise" true for any program this import can
                // produce, matching what ProgramTransfer.decode() requires of a re-exported file.
                if (day.isRestDay || resolvedExercises.isNotEmpty()) {
                    val dayId = programDayDao.insert(
                        ProgramDayEntity(
                            programId = programId,
                            dayOfWeek = day.dayOfWeek,
                            titleVi = day.titleVi,
                            isRestDay = day.isRestDay,
                        ),
                    )
                    if (resolvedExercises.isNotEmpty()) {
                        programExerciseDao.insertAll(
                            resolvedExercises.mapIndexed { index, (transferExercise, exercise) ->
                                ProgramExerciseEntity(
                                    programDayId = dayId,
                                    exerciseId = exercise.id,
                                    orderIndex = index,
                                    targetSets = transferExercise.targetSets,
                                    targetRepsMin = transferExercise.targetRepsMin,
                                    targetRepsMax = transferExercise.targetRepsMax,
                                )
                            },
                        )
                    }
                }
            }
            programId
        }
}
