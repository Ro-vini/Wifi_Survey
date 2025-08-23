package com.example.site_survey.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// importa as entidades e DAOs
import com.example.site_survey.data.MeasurementEntity
import com.example.site_survey.data.MeasurementDao

@Database(entities = [MeasurementEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wifisurvey.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}