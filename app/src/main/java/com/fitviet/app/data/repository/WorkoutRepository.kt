package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.SetLogDao
import com.fitviet.app.data.local.dao.WorkoutSessionDao
import com.fitviet.app.data.local.entity.SetLogEntity
import com.fitviet.app.data.local.entity.WorkoutSessionEntity
import com.fitviet.app.ui.workout.LoggedSet

interface WorkoutRepository {
    suspend fun startSession(dayLabel: String, startedAtMillis: Long): Long

    /** Persisted as each set completes rather than batched at the end, so a killed process doesn't lose logged sets. */
    suspend fun logSet(sessionId: Long, set: LoggedSet)

    suspend fun completeSession(sessionId: Long, completedAtMillis: Long, totalVolumeKg: Double, durationSeconds: Int)

    /** The heaviest weight ever logged for this exercise across completed sets (any session), or
     * null if it's never been logged — the "Recommended weight" shown on a program-day workout
     * (Gate 24) is seeded from this, since it's the closest real signal to "what should I lift"
     * this app already tracks. */
    suspend fun getRecommendedWeight(exerciseId: Long): Double?
}

class RoomWorkoutRepository(
    private val workoutSessionDao: WorkoutSessionDao,
    private val setLogDao: SetLogDao,
) : WorkoutRepository {
    override suspend fun startSession(dayLabel: String, startedAtMillis: Long): Long =
        workoutSessionDao.insert(WorkoutSessionEntity(dayLabel = dayLabel, startedAt = startedAtMillis))

    override suspend fun getRecommendedWeight(exerciseId: Long): Double? = setLogDao.getPersonalBest(exerciseId)?.weightKg

    override suspend fun logSet(sessionId: Long, set: LoggedSet) {
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

    override suspend fun completeSession(sessionId: Long, completedAtMillis: Long, totalVolumeKg: Double, durationSeconds: Int) {
        val session = workoutSessionDao.getById(sessionId) ?: return
        workoutSessionDao.update(
            session.copy(completedAt = completedAtMillis, totalVolumeKg = totalVolumeKg, durationSeconds = durationSeconds),
        )
    }
}
