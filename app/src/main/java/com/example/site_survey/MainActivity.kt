package com.example.site_survey

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.site_survey.data.MeasurementPoint
import com.example.site_survey.databinding.ActivityMainBinding
import com.example.site_survey.services.GpsLocationService
import com.example.site_survey.services.WifiScannerService
import com.example.site_survey.utils.DataExporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.content.Intent

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var wifiScanner: WifiScannerService
    private lateinit var gpsService: GpsLocationService

    private val measurements = mutableListOf<MeasurementPoint>()
    private var collectingJob: Job? = null

    private val latestLat = MutableStateFlow<Double?>(null)
    private val latestLon = MutableStateFlow<Double?>(null)
    private val latestWifi = MutableStateFlow(emptyList<com.example.site_survey.data.WifiScanResult>())

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val allGranted = granted.all { it.value }
        if (allGranted) {
            startCollecting()
        } else {
            Toast.makeText(this, "Permissões necessárias não concedidas.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }

        wifiScanner = WifiScannerService(this)
        gpsService = GpsLocationService(this)

        binding.btnStart.setOnClickListener { checkAndRequestPermissionsThenStart() }
        binding.btnStop.setOnClickListener { stopCollecting() }
        binding.btnExport.setOnClickListener {
            val file = DataExporter.exportCsv(this, measurements)
            Toast.makeText(this, "Exportado: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
        binding.btnOpenMap.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkAndRequestPermissionsThenStart() {
        val needed = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }.toTypedArray()

        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(needed)
        } else {
            startCollecting()
        }
    }

    private fun startCollecting() {
        if (collectingJob != null) return

        binding.tvStatus.text = "Status: coletando…"

        // Flow de localização
        val locJob = lifecycleScope.launch {
            gpsService.locationFlow().collectLatest { loc ->
                latestLat.value = loc.latitude
                latestLon.value = loc.longitude
            }
        }

        // Flow de Wi-Fi (a cada scan completo)
        val wifiJob = lifecycleScope.launch {
            wifiScanner.scanFlow().collectLatest { list ->
                latestWifi.value = list
                // opcional: disparar novo scan com intervalo
                lifecycleScope.launch {
                    delay(3000L)
                    wifiScanner.requestScan()
                }
            }
        }

        // Consolida medições (quando houver lat/lon + wifi)
        val collectorJob = lifecycleScope.launch {
            while (true) {
                val lat = latestLat.value
                val lon = latestLon.value
                val wifi = latestWifi.value
                if (lat != null && lon != null && wifi.isNotEmpty()) {
                    val point = MeasurementPoint(
                        timestamp = System.currentTimeMillis(),
                        latitude = lat,
                        longitude = lon,
                        wifiList = wifi
                    )
                    measurements += point
                    binding.tvCount.text = "Medições: ${measurements.size}"

                    // 🔹 Envia broadcast para o MapActivity desenhar em tempo real
                    val intent = Intent("NEW_MEASUREMENT")
                    intent.putExtra("lat", lat)
                    intent.putExtra("lon", lon)
                    intent.putExtra("rssi", wifi.first().rssi) // exemplo: pega o primeiro AP
                    sendBroadcast(intent)
                }
                delay(2000L)
            }
        }

        collectingJob = lifecycleScope.launch {
            // supervisiona os três
            listOf(locJob, wifiJob, collectorJob).forEach { it.join() }
        }
    }

    private fun stopCollecting() {
        collectingJob?.cancel()
        collectingJob = null
        binding.tvStatus.text = "Status: parado"
        Toast.makeText(this, "Coleta interrompida.", Toast.LENGTH_SHORT).show()
    }

}