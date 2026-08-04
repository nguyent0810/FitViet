package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.ExerciseDao
import com.fitviet.app.data.local.entity.ExerciseEntity

class ExerciseRepository(private val exerciseDao: ExerciseDao) {
    suspend fun getAll(): List<ExerciseEntity> = exerciseDao.getAllOnce()
}
