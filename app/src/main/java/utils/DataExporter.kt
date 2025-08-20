package com.example.site_survey.utils

import android.content.Context
import com.example.site_survey.data.MeasurementPoint
import java.io.File

object DataExporter {
    fun exportCsv(context: Context, data: List<MeasurementPoint>): File {
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(dir, "wifisurvey_${System.currentTimeMillis()}.csv")

        file.printWriter().use { out ->
            out.println("timestamp,latitude,longitude,ssid,bssid,rssi,frequencyMHz,channel")
            data.forEach { mp ->
                mp.wifiList.forEach { w ->
                    out.println("${mp.timestamp},${mp.latitude},${mp.longitude},\"${w.ssid}\",${w.bssid},${w.rssi},${w.frequencyMHz},${w.channel}")
                }
            }
        }
        return file
    }
}