package com.example.site_survey.data

data class WifiScanResult(
    val ssid: String,
    val bssid: String,
    val rssi: Int,          // level em dBm (ScanResult.level)
    val frequencyMHz: Int,  // ScanResult.frequency
    val channel: Int
)