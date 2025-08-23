package com.example.site_survey.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequencyMHz: Int,
    val channel: Int
)