package com.example.site_survey.data

data class MeasurementPoint(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val wifiList: List<WifiScanResult>
)