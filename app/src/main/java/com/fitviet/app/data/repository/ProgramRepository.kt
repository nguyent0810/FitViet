package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.ExerciseDao
import com.fitviet.app.data.local.dao.ProgramDao
import com.fitviet.app.data.local.dao.ProgramDayDao
import com.fitviet.app.data.local.dao.ProgramExerciseDao
import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.data.local.entity.SettingsEntity
import com.fitviet.app.domain.ProgramScheduleCalculator
import com.fitviet.app.domain.ProgramScheduleDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

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
}
