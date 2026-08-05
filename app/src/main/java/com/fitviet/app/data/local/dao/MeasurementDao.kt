package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fitviet.app.data.local.entity.MeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements ORDER BY epochDay DESC, id DESC")
    fun observeAll(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements ORDER BY epochDay DESC, id DESC LIMIT 1")
    fun observeLatest(): Flow<MeasurementEntity?>

    @Query("SELECT * FROM measurements ORDER BY epochDay DESC, id DESC LIMIT 2")
    suspend fun getLatestTwo(): List<MeasurementEntity>

    @Insert
    suspend fun insert(measurement: MeasurementEntity): Long

    @Update
    suspend fun update(measurement: MeasurementEntity)

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun deleteById(id: Long)
}
