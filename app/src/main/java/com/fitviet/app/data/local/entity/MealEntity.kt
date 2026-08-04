package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A logged meal entry shown on the 1g nutrition screen. */
@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Day this meal was logged on, as an epoch day (java.time.LocalDate#toEpochDay). */
    val epochDay: Long,
    val slot: String,
    val nameVi: String,
    val kcal: Int,
    val proteinG: Int,
    val carbG: Int,
    val fatG: Int,
)
