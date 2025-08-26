package com.example.site_survey

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.lifecycleScope
import com.example.site_survey.data.AppDatabase
import com.example.site_survey.data.MeasurementEntity
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapActivity : AppCompatActivity() {

    private lateinit var map: MapView

    // Receiver para receber novas medições em tempo real
    private val measurementReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "NEW_MEASUREMENT") {
                val lat = intent.getDoubleExtra("lat", 0.0)
                val lon = intent.getDoubleExtra("lon", 0.0)
                val rssi = intent.getIntExtra("rssi", -100)
                addHeatCircle(lat, lon, rssi)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx = applicationContext
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_map)

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setUseDataConnection(true)
        map.setMultiTouchControls(true)

        // Bolinha azul + seguir localização
        val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()
        map.overlays.add(myLocationOverlay)

        // Centraliza e aplica zoom no primeiro fix real do GPS
        myLocationOverlay.runOnFirstFix {
            runOnUiThread {
                myLocationOverlay.myLocation?.let { loc ->
                    val p = GeoPoint(loc.latitude, loc.longitude)
                    map.controller.setZoom(18.0)
                    map.controller.setCenter(p)
                }
            }
        }

        // Registrar o receiver (API 33+ precisa flag)
        val filter = IntentFilter("NEW_MEASUREMENT")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(measurementReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(measurementReceiver, filter)
        }

        // Carrega medições do banco e desenha (círculos + marcadores)
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(this@MapActivity).measurementDao()
            val data: List<MeasurementEntity> = dao.getAll()

            if (data.isNotEmpty()) {
                val start = data.first()
                map.controller.setZoom(18.0)
                map.controller.setCenter(GeoPoint(start.latitude, start.longitude))
            }

            // 1) Círculos de "calor" (semi-transparente)
            data.forEach { point ->
                val circle = Polygon(map)
                circle.points = Polygon.pointsAsCircle(
                    GeoPoint(point.latitude, point.longitude),
                    0.5 // raio em metros
                )
                val fillColor = when {
                    point.rssi >= -50 -> 0x55FF0000  // vermelho
                    point.rssi >= -70 -> 0x55FFFF00  // amarelo
                    else -> 0x5500FF00               // verde
                }
                circle.fillColor = fillColor
                circle.strokeColor = Color.TRANSPARENT
                map.overlays.add(circle)
            }

            // 2) Marcadores por cima
            data.forEach { point ->
                val marker = Marker(map)
                marker.position = GeoPoint(point.latitude, point.longitude)
                marker.title = "${point.ssid} (${point.rssi} dBm)"
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                val color = when {
                    point.rssi >= -50 -> Color.GREEN
                    point.rssi >= -70 -> Color.YELLOW
                    else -> Color.RED
                }
                marker.icon = createColoredMarker(marker, color)
                map.overlays.add(marker)
            }

            map.invalidate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(measurementReceiver)
    }

    // Tinge o ícone do marcador
    private fun createColoredMarker(marker: Marker, color: Int) = marker.icon?.let {
        val wrapped = DrawableCompat.wrap(it.mutate())
        DrawableCompat.setTint(wrapped, color)
        wrapped
    }

    // Desenha um círculo semi-transparente conforme o RSSI
    private fun addHeatCircle(lat: Double, lon: Double, rssi: Int) {
        val circle = Polygon(map)
        circle.points = Polygon.pointsAsCircle(
            GeoPoint(lat, lon),
            25.0
        )
        val fillColor = when {
            rssi >= -50 -> 0x55FF0000
            rssi >= -70 -> 0x55FFFF00
            else -> 0x5500FF00
        }
        circle.fillColor = fillColor
        circle.strokeColor = Color.TRANSPARENT
        map.overlays.add(circle)
        map.invalidate()
    }
}
