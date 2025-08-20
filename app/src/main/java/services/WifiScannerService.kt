package com.example.site_survey.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import com.example.site_survey.data.WifiScanResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class WifiScannerService(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    // Emite uma lista de WifiScanResult toda vez que um scan completar
    fun scanFlow() = callbackFlow<List<WifiScanResult>> {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val results: List<ScanResult> = wifiManager.scanResults ?: emptyList()
                trySend(results.map { it.toModel() })
            }
        }
        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(receiver, filter)

        // dispara o primeiro scan
        wifiManager.startScan()

        awaitClose { context.unregisterReceiver(receiver) }
    }

    fun requestScan(): Boolean = wifiManager.startScan()

    private fun ScanResult.toModel(): WifiScanResult {
        val freq = this.frequency
        return WifiScanResult(
            ssid = this.SSID ?: "",
            bssid = this.BSSID ?: "",
            rssi = this.level,
            frequencyMHz = freq,
            channel = frequencyToChannel(freq)
        )
    }

    private fun frequencyToChannel(freqMHz: Int): Int {
        return when {
            freqMHz in 2412..2484 -> (freqMHz - 2407) / 5 // 2.4 GHz
            freqMHz in 5170..5905 -> (freqMHz - 5000) / 5 // 5 GHz (aprox)
            else -> -1
        }
    }
}