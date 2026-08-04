package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A logged (or in-progress) workout session — the 1e flow produces one of these on completion. */
@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("programId")],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Null for ad-hoc workouts not tied to a program day. */
    val programId: Long? = null,
    val dayLabel: String,
    val startedAt: Long,
    val completedAt: Long? = null,
    val totalVolumeKg: Double = 0.0,
    val durationSeconds: Int = 0,
)
