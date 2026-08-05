package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fitviet.app.data.local.entity.ProgramExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramExerciseDao {
    @Query(
        """
        SELECT pe.* FROM program_exercises pe
        INNER JOIN program_days pd ON pd.id = pe.programDayId
        WHERE pd.programId = :programId
        ORDER BY pe.orderIndex
        """,
    )
    fun observeForProgram(programId: Long): Flow<List<ProgramExerciseEntity>>

    @Insert
    suspend fun insertAll(exercises: List<ProgramExerciseEntity>)
}
