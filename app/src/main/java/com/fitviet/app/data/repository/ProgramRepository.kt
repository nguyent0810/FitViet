package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.ProgramDao
import com.fitviet.app.data.local.entity.ProgramEntity
import kotlinx.coroutines.flow.Flow

class ProgramRepository(private val programDao: ProgramDao) {
    fun observeAll(): Flow<List<ProgramEntity>> = programDao.observeAll()

    suspend fun getById(id: Long): ProgramEntity? = programDao.getById(id)
}
