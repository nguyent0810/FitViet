package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A body-measurement check-in (the 4 tiles on 1i: Cân nặng/Ngực/Eo/Tay). One row per update. */
@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val weightKg: Double? = null,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val armCm: Double? = null,
)
