package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.PersonalBestRow
import com.fitviet.app.data.local.dao.SetLogDao
import com.fitviet.app.data.local.dao.WorkoutSessionDao
import com.fitviet.app.data.local.entity.WorkoutSessionEntity
import com.fitviet.app.domain.CompletedSession
import com.fitviet.app.domain.CompletedSet
import com.fitviet.app.domain.DashboardStatsCalculator
import com.fitviet.app.domain.DayVolume
import com.fitviet.app.domain.DiaryStatsCalculator
import com.fitviet.app.domain.MovementTypeDistribution
import com.fitviet.app.domain.MuscleGroupWorkload
import com.fitviet.app.domain.WeekVolume
import com.fitviet.app.domain.WorkoutCompositionCalculator
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

private const val PERSONAL_BEST_LIMIT = 3

/** How many trailing weeks (including the current one) feed #8/#9's charts — same window as
 * [DiaryStatsCalculator.lastNWeeks]'s default, so the bar chart and these two cards agree on
 * "recent". */
private const val COMPOSITION_WINDOW_WEEKS = 4L

data class DiaryData(
    /** All completed sessions, most recent first — not capped, so the day-strip hint can find a
     * match even for a day outside whatever slice the UI chooses to *display* as "recent". */
    val completedSessions: List<WorkoutSessionEntity>,
    val personalBests: List<PersonalBestRow>,
    val last7Days: List<DayVolume>,
    val last4Weeks: List<WeekVolume>,
    /** Feature #8 — always 6 entries (one per [com.fitviet.app.domain.MuscleGroup]), trailing
     * [COMPOSITION_WINDOW_WEEKS]-week window. */
    val muscleGroupWorkload: List<MuscleGroupWorkload>,
    /** Feature #9 — compound-vs-isolation set counts, same window as [muscleGroupWorkload]. */
    val movementTypeDistribution: MovementTypeDistribution,
)

class DiaryRepository(
    private val workoutSessionDao: WorkoutSessionDao,
    private val setLogDao: SetLogDao,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<DiaryData> {
        val zone = ZoneId.systemDefault()

        // Re-derives "today" across midnight — see dayTicker() and Gate 3 PROGRESS.md.
        return dayTicker(zone).flatMapLatest { today ->
            combine(
                workoutSessionDao.observeCompleted(),
                setLogDao.observePersonalBests(PERSONAL_BEST_LIMIT),
                setLogDao.observeCompletedSetBreakdown(),
            ) { sessions, personalBests, setBreakdown ->
                val completedSessions = sessions.mapNotNull { session ->
                    val completedAt = session.completedAt ?: return@mapNotNull null
                    CompletedSession(
                        date = Instant.ofEpochMilli(completedAt).atZone(zone).toLocalDate(),
                        volumeKg = session.totalVolumeKg,
                    )
                }
                val completedSets = setBreakdown.map { row ->
                    CompletedSet(
                        date = Instant.ofEpochMilli(row.completedAt).atZone(zone).toLocalDate(),
                        muscleGroupCode = row.muscleGroupCode,
                        movementType = row.movementType,
                        volumeKg = row.weightKg * row.reps,
                    )
                }
                val windowStart = today.with(DayOfWeek.MONDAY).minusWeeks(COMPOSITION_WINDOW_WEEKS - 1)
                DiaryData(
                    completedSessions = sessions.sortedByDescending { it.completedAt ?: 0L },
                    personalBests = personalBests,
                    last7Days = DashboardStatsCalculator.compute(completedSessions, today).last7Days,
                    last4Weeks = DiaryStatsCalculator.lastNWeeks(completedSessions, today),
                    muscleGroupWorkload = WorkoutCompositionCalculator.muscleGroupWorkload(completedSets, windowStart),
                    movementTypeDistribution = WorkoutCompositionCalculator.movementTypeDistribution(completedSets, windowStart),
                )
            }
        }
    }
}
