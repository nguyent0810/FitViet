package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A training program card shown on the Giáo án (1c) list. All programs are free. */
@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titleVi: String,
    val imageAsset: String,
    val durationWeeks: Int,
    val sessionsPerWeek: Int,
    val level: String,
    val equipment: String,
    /** Filter chip tags, e.g. "Tăng cơ", "Phòng gym" — matches the 1c filter chip labels. */
    val tags: List<String>,
)
