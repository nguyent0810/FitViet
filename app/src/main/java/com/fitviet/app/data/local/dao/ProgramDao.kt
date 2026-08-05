package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fitviet.app.data.local.entity.ProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {
    @Query("SELECT * FROM programs ORDER BY id")
    fun observeAll(): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE id = :id")
    suspend fun getById(id: Long): ProgramEntity?

    @Query("SELECT * FROM programs ORDER BY id")
    suspend fun getAllOnce(): List<ProgramEntity>

    @Query("SELECT COUNT(*) FROM programs")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programs: List<ProgramEntity>)

    @Insert
    suspend fun insert(program: ProgramEntity): Long
}
