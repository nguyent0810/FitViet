package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One calendar-weekday slot in a program's fixed weekly schedule (2b). */
@Entity(
    tableName = "program_days",
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("programId"), Index(value = ["programId", "dayOfWeek"], unique = true)],
)
data class ProgramDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programId: Long,
    /** ISO day-of-week value: 1=Monday..7=Sunday (matches [java.time.DayOfWeek.getValue]). */
    val dayOfWeek: Int,
    val titleVi: String,
    val isRestDay: Boolean = false,
)
