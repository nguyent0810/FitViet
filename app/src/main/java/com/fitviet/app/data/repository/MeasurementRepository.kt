package com.fitviet.app.data.repository

import com.fitviet.app.data.local.dao.MeasurementDao
import com.fitviet.app.data.local.entity.MeasurementEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class MeasurementsData(val latest: MeasurementEntity?, val previous: MeasurementEntity?)

class MeasurementRepository(private val measurementDao: MeasurementDao) {
    /** Latest two check-ins (most recent first) so the UI can show a delta against the prior one. */
    fun observeLatestTwo(): Flow<MeasurementsData> =
        measurementDao.observeAll().map { all ->
            MeasurementsData(latest = all.getOrNull(0), previous = all.getOrNull(1))
        }

    suspend fun addCheckIn(measurement: MeasurementEntity) {
        measurementDao.insert(measurement)
    }
}
