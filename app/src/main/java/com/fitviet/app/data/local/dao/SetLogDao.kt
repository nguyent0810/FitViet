package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fitviet.app.data.local.entity.SetLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SetLogDao {
    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY exerciseOrder, setIndex")
    fun observeForSession(sessionId: Long): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM set_logs WHERE exerciseId = :exerciseId AND isDone = 1 ORDER BY weightKg DESC LIMIT 1")
    suspend fun getPersonalBest(exerciseId: Long): SetLogEntity?

    @Insert
    suspend fun insert(setLog: SetLogEntity): Long

    @Insert
    suspend fun insertAll(setLogs: List<SetLogEntity>)

    @Update
    suspend fun update(setLog: SetLogEntity)
}
