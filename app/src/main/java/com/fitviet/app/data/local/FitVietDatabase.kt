package com.fitviet.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fitviet.app.data.local.dao.ExerciseDao
import com.fitviet.app.data.local.dao.MealDao
import com.fitviet.app.data.local.dao.MeasurementDao
import com.fitviet.app.data.local.dao.ProgramDao
import com.fitviet.app.data.local.dao.SetLogDao
import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.dao.WorkoutSessionDao
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.entity.MealEntity
import com.fitviet.app.data.local.entity.MeasurementEntity
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.data.local.entity.SetLogEntity
import com.fitviet.app.data.local.entity.SettingsEntity
import com.fitviet.app.data.local.entity.WorkoutSessionEntity

@Database(
    entities = [
        ProgramEntity::class,
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        SetLogEntity::class,
        MealEntity::class,
        MeasurementEntity::class,
        SettingsEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class FitVietDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun setLogDao(): SetLogDao
    abstract fun mealDao(): MealDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile private var instance: FitVietDatabase? = null

        fun getInstance(context: Context): FitVietDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FitVietDatabase::class.java,
                    "fitviet.db",
                ).build().also { instance = it }
            }
    }
}
