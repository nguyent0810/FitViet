package com.fitviet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fitviet.app.data.local.entity.SplitTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitTemplateDao {
    @Query("SELECT * FROM split_templates ORDER BY id")
    fun observeAll(): Flow<List<SplitTemplateEntity>>

    @Query("SELECT * FROM split_templates WHERE id = :id")
    suspend fun getById(id: Long): SplitTemplateEntity?

    @Insert
    suspend fun insert(template: SplitTemplateEntity): Long
}
