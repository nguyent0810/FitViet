package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.ExerciseDao
import com.fitviet.app.data.local.dao.SetLogDao
import com.fitviet.app.data.local.dao.WorkoutSessionDao
import com.fitviet.app.data.local.entity.SetLogEntity
import com.fitviet.app.data.local.entity.WorkoutSessionEntity
import com.fitviet.app.domain.DashboardStatsCalculator
import com.fitviet.app.domain.ExerciseHistoryCalculator
import com.fitviet.app.domain.ExerciseHistoryEntry
import com.fitviet.app.domain.LoggedSetPoint
import com.fitviet.app.domain.PersonalRecordCalculator
import com.fitviet.app.domain.SessionPersonalRecord
import com.fitviet.app.ui.workout.LoggedSet
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface WorkoutRepository {
    /** [monthlyPlanDayId], when non-null, is the authoritative "Hit & Run" (Gate 63+) regenerate-
     * lock signal — see [com.fitviet.app.data.repository.MonthlyPlanRepository]'s lock rule. */
    suspend fun startSession(dayLabel: String, startedAtMillis: Long, monthlyPlanDayId: Long? = null): Long

    /** Persisted as each set completes rather than batched at the end, so a killed process doesn't lose logged sets. */
    suspend fun logSet(sessionId: Long, set: LoggedSet)

    suspend fun completeSession(sessionId: Long, completedAtMillis: Long, totalVolumeKg: Double, durationSeconds: Int)

    /** The heaviest weight ever logged for this exercise across completed sets (any session), or
     * null if it's never been logged — the "Recommended weight" shown on a program-day workout
     * (Gate 24) is seeded from this, since it's the closest real signal to "what should I lift"
     * this app already tracks. */
    suspend fun getRecommendedWeight(exerciseId: Long): Double?

    /** Feature #4 (Gate 40) — reuses [DashboardStatsCalculator]'s streak rule so the number shown
     * on the "share this workout" action always matches Dashboard's own streak, rather than a
     * second, potentially-diverging definition. One-shot (not observed) since it's only read once,
     * right after [completeSession] lands, at session-finish time. */
    suspend fun getCurrentStreakDays(today: LocalDate): Int

    /** Feature #10 (Gate 46) — per-exercise progress history, newest first, one entry per date
     * logged (that date's heaviest set). Backs Exercise Detail's "Tiến bộ" tab. */
    fun observeHistoryForExercise(exerciseId: Long): Flow<List<ExerciseHistoryEntry>>

    /** Redesign Gate 6d — did this just-finished session set a genuine new all-time best for any
     * exercise it trained (per [PersonalRecordCalculator.isNewRecord])? One-shot, called right
     * after [completeSession] lands, same "read once at session-finish time" pattern
     * [getCurrentStreakDays] already established. When more than one exercise PR'd in the same
     * session, the heaviest one wins — the mock's own result card has room for exactly one badge,
     * and the heaviest lift is the one worth leading with. Null when nothing in this session beat
     * its own prior best. */
    suspend fun findSessionPersonalRecord(sessionId: Long): SessionPersonalRecord?
}

class RoomWorkoutRepository(
    private val workoutSessionDao: WorkoutSessionDao,
    private val setLogDao: SetLogDao,
    private val exerciseDao: ExerciseDao,
) : WorkoutRepository {
    override suspend fun startSession(dayLabel: String, startedAtMillis: Long, monthlyPlanDayId: Long?): Long =
        workoutSessionDao.insert(WorkoutSessionEntity(dayLabel = dayLabel, startedAt = startedAtMillis, monthlyPlanDayId = monthlyPlanDayId))

    override suspend fun getRecommendedWeight(exerciseId: Long): Double? = setLogDao.getPersonalBest(exerciseId)?.weightKg

    override suspend fun getCurrentStreakDays(today: LocalDate): Int {
        val zone = ZoneId.systemDefault()
        val trainedDates = workoutSessionDao.observeCompleted().first()
            .mapNotNull { session -> session.completedAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } }
            .toSet()
        return DashboardStatsCalculator.currentStreak(trainedDates, today)
    }

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

    override fun observeHistoryForExercise(exerciseId: Long): Flow<List<ExerciseHistoryEntry>> {
        val zone = ZoneId.systemDefault()
        return setLogDao.observeHistoryForExercise(exerciseId).map { rows ->
            val points = rows.map { row ->
                LoggedSetPoint(
                    date = Instant.ofEpochMilli(row.completedAt).atZone(zone).toLocalDate(),
                    weightKg = row.weightKg,
                    reps = row.reps,
                )
            }
            ExerciseHistoryCalculator.bestSetPerDate(points)
        }
    }

    override suspend fun findSessionPersonalRecord(sessionId: Long): SessionPersonalRecord? {
        val sessionMaxByExercise = setLogDao.getSetsForSessionOnce(sessionId)
            .groupBy { it.exerciseId }
            .mapValues { (_, sets) -> sets.maxOf { it.weightKg } }
        var best: SessionPersonalRecord? = null
        for ((exerciseId, weightKg) in sessionMaxByExercise) {
            val priorBest = setLogDao.getPersonalBestExcludingSession(exerciseId, sessionId)?.weightKg
            if (!PersonalRecordCalculator.isNewRecord(weightKg, priorBest)) continue
            if (best == null || weightKg > best.weightKg) {
                val exerciseName = exerciseDao.getById(exerciseId)?.nameVi ?: continue
                best = SessionPersonalRecord(exerciseId, exerciseName, weightKg)
            }
        }
        return best
    }
}
