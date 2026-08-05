package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fitviet.app.data.local.entity.SetLogEntity
import kotlinx.coroutines.flow.Flow

data class PersonalBestRow(val exerciseId: Long, val nameVi: String, val maxWeightKg: Double)

/** One completed set joined to its exercise's stable classification codes — feeds
 * [com.fitviet.app.domain.WorkoutCompositionCalculator] (features #8/#9). */
data class SetBreakdownRow(
    val muscleGroupCode: String,
    val movementType: String,
    val weightKg: Double,
    val reps: Int,
    val completedAt: Long,
)

@Dao
interface SetLogDao {
    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY exerciseOrder, setIndex")
    fun observeForSession(sessionId: Long): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM set_logs WHERE exerciseId = :exerciseId AND isDone = 1 ORDER BY weightKg DESC LIMIT 1")
    suspend fun getPersonalBest(exerciseId: Long): SetLogEntity?

    @Query(
        """
        SELECT e.id AS exerciseId, e.nameVi AS nameVi, MAX(s.weightKg) AS maxWeightKg
        FROM set_logs s
        INNER JOIN exercises e ON e.id = s.exerciseId
        INNER JOIN workout_sessions w ON w.id = s.sessionId
        WHERE s.isDone = 1 AND w.completedAt IS NOT NULL
        GROUP BY s.exerciseId
        ORDER BY maxWeightKg DESC
        LIMIT :limit
        """,
    )
    fun observePersonalBests(limit: Int): Flow<List<PersonalBestRow>>

    @Query(
        """
        SELECT e.muscleGroupCode AS muscleGroupCode, e.movementType AS movementType,
               s.weightKg AS weightKg, s.reps AS reps, w.completedAt AS completedAt
        FROM set_logs s
        INNER JOIN exercises e ON e.id = s.exerciseId
        INNER JOIN workout_sessions w ON w.id = s.sessionId
        WHERE s.isDone = 1 AND w.completedAt IS NOT NULL
        """,
    )
    fun observeCompletedSetBreakdown(): Flow<List<SetBreakdownRow>>

    @Insert
    suspend fun insert(setLog: SetLogEntity): Long

    @Insert
    suspend fun insertAll(setLogs: List<SetLogEntity>)

    @Update
    suspend fun update(setLog: SetLogEntity)
}
