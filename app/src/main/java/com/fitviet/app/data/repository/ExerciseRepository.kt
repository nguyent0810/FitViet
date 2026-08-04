package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.ExerciseDao
import com.fitviet.app.data.local.entity.ExerciseEntity

interface ExerciseRepository {
    suspend fun getAll(): List<ExerciseEntity>
    suspend fun getById(id: Long): ExerciseEntity?
}

class RoomExerciseRepository(private val exerciseDao: ExerciseDao) : ExerciseRepository {
    override suspend fun getAll(): List<ExerciseEntity> = exerciseDao.getAllOnce()
    override suspend fun getById(id: Long): ExerciseEntity? = exerciseDao.getById(id)
}
