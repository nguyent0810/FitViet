package com.fitviet.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One exercise target assigned to a [ProgramDayEntity], in display order. */
@Entity(
    tableName = "program_exercises",
    foreignKeys = [
        ForeignKey(
            entity = ProgramDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["programDayId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("programDayId"), Index("exerciseId")],
)
data class ProgramExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programDayId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    /** Null = straight sets. Two rows in the same [programDayId] sharing the same non-null value
     * form a superset pair (Gate 47) — any other arrangement (a lone value, 3+ rows sharing one,
     * or two matching rows that aren't adjacent by [orderIndex]) is treated as malformed and each
     * row degrades back to straight sets rather than being dropped. Deliberately authored per
     * program day, not inferred from muscle group, since real supersets commonly pair opposing or
     * unrelated muscles. */
    val supersetGroup: String? = null,
)
