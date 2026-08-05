package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fitviet.app.data.local.entity.ProgramDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDayDao {
    @Query("SELECT * FROM program_days WHERE programId = :programId ORDER BY dayOfWeek")
    fun observeForProgram(programId: Long): Flow<List<ProgramDayEntity>>

    @Query("SELECT COUNT(*) FROM program_days WHERE programId = :programId")
    suspend fun countForProgram(programId: Long): Int

    @Insert
    suspend fun insert(day: ProgramDayEntity): Long
}
