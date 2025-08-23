package com.example.site_survey

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.graphics.drawable.DrawableCompat
import com.example.site_survey.data.AppDatabase
import com.example.site_survey.data.MeasurementEntity
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.overlay.Marker

class MapActivity : AppCompatActivity() {

    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa configuração (importante para cache)
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_map)

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        val myLocationOverlay = org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay(
            org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider(this),
            map
        )
        myLocationOverlay.enableMyLocation()       // ativa GPS
        myLocationOverlay.enableFollowLocation()   // centraliza no usuário automaticamente
        map.overlays.add(myLocationOverlay)

        // 🔹 Quando pegar o primeiro fix do GPS
        myLocationOverlay.runOnFirstFix {
            runOnUiThread {
                val loc = myLocationOverlay.myLocation
                if (loc != null) {
                    val geoPoint = GeoPoint(loc.latitude, loc.longitude)
                    map.controller.setZoom(18.0) // força zoom inicial
                    map.controller.setCenter(geoPoint)
                }
            }
        }

        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(this@MapActivity).measurementDao()
            val data: List<MeasurementEntity> = dao.getAll()

            if (data.isNotEmpty()) {
                val start = data.first()
                map.controller.setZoom(18.0)
                map.controller.setCenter(GeoPoint(start.latitude, start.longitude))
            }

            // Coloca marcadores coloridos de acordo com intensidade do Wi-Fi
            data.forEach { point ->
                val marker = Marker(map)
                marker.position = GeoPoint(point.latitude, point.longitude)
                marker.title = "${point.ssid} (${point.rssi} dBm)"
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                // Define cor do marcador
                val color = when {
                    point.rssi >= -50 -> Color.GREEN
                    point.rssi >= -70 -> Color.YELLOW
                    else -> Color.RED
                }
                marker.icon = createColoredMarker(marker, color)

                map.overlays.add(marker)
            }

            map.invalidate() // redesenha
        }
    }

    // Função para criar marcador colorido
    private fun createColoredMarker(marker: Marker, color: Int) = marker.icon?.let {
        val wrapped = DrawableCompat.wrap(it)
        DrawableCompat.setTint(wrapped, color)
        wrapped
    }
}