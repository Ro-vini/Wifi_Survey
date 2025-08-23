package com.example.site_survey.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MeasurementDao {
    @Insert
    suspend fun insertAll(measurements: List<MeasurementEntity>)

    @Query("SELECT * FROM measurements")
    suspend fun getAll(): List<MeasurementEntity>
}