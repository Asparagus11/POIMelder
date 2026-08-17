package com.poimelder.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.poimelder.app.history.SeenRepository
import com.poimelder.app.history.SeenStatus
import com.poimelder.app.location.ServiceState
import com.poimelder.app.settings.SettingsRepository
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

private const val COLOR_TODAY = 0xFF2E7D32.toInt()      // grün
private const val COLOR_PREVIOUS = 0xFF6D4C41.toInt()   // braun
private const val COLOR_UNSEEN = 0xFF4FC3F7.toInt()     // hellblau
private const val COLOR_ME = 0xFF1565C0.toInt()         // Standort

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val nearby by ServiceState.nearby.collectAsState()
    val location by ServiceState.location.collectAsState()
    val settingsRepo = remember { SettingsRepository.get(context) }
    val settings by settingsRepo.settings.collectAsState()
    val seen = remember { SeenRepository.get(context) }

    // osmdroid vor dem ersten MapView konfigurieren (User-Agent Pflicht wg. OSM-Regeln).
    remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        }
        true
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.0)
            // Startzentrum grob Deutschland, bis ein Standort vorliegt.
            controller.setCenter(GeoPoint(51.0, 10.0))
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { mapView },
        update = { mv ->
            mv.overlays.clear()
            val today = java.time.LocalDate.now().toEpochDay()

            location?.let { loc ->
                val center = GeoPoint(loc.latitude, loc.longitude)
                // Radius-Kreis
                val circle = Polygon(mv).apply {
                    points = Polygon.pointsAsCircle(center, settings.radiusMeters.toDouble())
                    fillPaint.color = 0x220D47A1
                    outlinePaint.color = 0x660D47A1
                    outlinePaint.strokeWidth = 3f
                }
                mv.overlays.add(circle)

                // Eigener Standort
                val me = Marker(mv).apply {
                    position = center
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = dotDrawable(context, COLOR_ME, 16)
                    title = "Standort"
                }
                mv.overlays.add(me)
                mv.controller.setCenter(center)
            }

            nearby.forEach { np ->
                val color = when (seen.statusFor(np.poi.id, today)) {
                    SeenStatus.TODAY -> COLOR_TODAY
                    SeenStatus.PREVIOUS -> COLOR_PREVIOUS
                    SeenStatus.UNSEEN -> COLOR_UNSEEN
                }
                val marker = Marker(mv).apply {
                    position = GeoPoint(np.poi.lat, np.poi.lon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = dotDrawable(context, color, 14)
                    title = np.poi.name
                    snippet = listOfNotNull(np.poi.kind, np.poi.detailLine()).joinToString("\n")
                }
                mv.overlays.add(marker)
            }
            mv.invalidate()
        },
    )
}

/** Farbiger Kreis-Marker (mit weißem Rand) in der gegebenen dp-Größe. */
private fun dotDrawable(context: Context, colorInt: Int, sizeDp: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (sizeDp * density).toInt().coerceAtLeast(8)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val r = size / 2f
    val border = size * 0.12f
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorInt }
    canvas.drawCircle(r, r, r - border, fill)
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = border
    }
    canvas.drawCircle(r, r, r - border / 2f, stroke)
    return BitmapDrawable(context.resources, bmp)
}
