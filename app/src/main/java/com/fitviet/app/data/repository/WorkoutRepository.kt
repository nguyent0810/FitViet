package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.SetLogDao
import com.fitviet.app.data.local.dao.WorkoutSessionDao
import com.fitviet.app.data.local.entity.SetLogEntity
import com.fitviet.app.data.local.entity.WorkoutSessionEntity
import com.fitviet.app.ui.workout.LoggedSet

class WorkoutRepository(
    private val workoutSessionDao: WorkoutSessionDao,
    private val setLogDao: SetLogDao,
) {
    suspend fun startSession(dayLabel: String, startedAtMillis: Long): Long =
        workoutSessionDao.insert(WorkoutSessionEntity(dayLabel = dayLabel, startedAt = startedAtMillis))

    /** Persisted as each set completes rather than batched at the end, so a killed process doesn't lose logged sets. */
    suspend fun logSet(sessionId: Long, set: LoggedSet) {
        setLogDao.insert(
            SetLogEntity(
                sessionId = sessionId,
                exerciseId = set.exerciseId,
                exerciseOrder = set.exerciseOrder,
                setIndex = set.setIndex,
                weightKg = set.weightKg,
                reps = set.reps,
                isDone = true,
            ),
        )
    }

    suspend fun completeSession(sessionId: Long, completedAtMillis: Long, totalVolumeKg: Double, durationSeconds: Int) {
        val session = workoutSessionDao.getById(sessionId) ?: return
        workoutSessionDao.update(
            session.copy(completedAt = completedAtMillis, totalVolumeKg = totalVolumeKg, durationSeconds = durationSeconds),
        )
    }
}
