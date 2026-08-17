package com.poimelder.app.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.poimelder.app.MainActivity
import com.poimelder.app.R
import com.poimelder.app.history.SeenRepository
import com.poimelder.app.overpass.OverpassClient
import com.poimelder.app.proximity.Announcement
import com.poimelder.app.proximity.ProximityEngine
import com.poimelder.app.proximity.ProximityResult
import com.poimelder.app.proximity.RequeryPolicy
import com.poimelder.app.settings.SettingsRepository
import com.poimelder.app.tts.TtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground-Service: verfolgt den Standort dauerhaft, berechnet nahe POIs (Enter/Exit)
 * und fragt bei ausreichender Bewegung automatisch neue POIs von Overpass ab (Task 7).
 */
class LocationService : Service() {

    private var locationManager: LocationManager? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val overpass = OverpassClient()
    private val engine = ProximityEngine()
    private val settings by lazy { SettingsRepository.get(this) }
    private var tts: TtsManager? = null

    @Volatile private var lastQuery: RequeryPolicy.LastQuery? = null
    @Volatile private var querying: Boolean = false
    @Volatile private var lastAnnouncedPrimaryId: String? = null
    @Volatile private var lastAnnounceTimeMs: Long = 0L

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            handleLocation(location)
        }

        override fun onProviderDisabled(provider: String) {}
        override fun onProviderEnabled(provider: String) {}

        @Deprecated("Android API")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        tts = TtsManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, buildNotification("Warte auf Standort …"))
        ServiceState.setRunning(true)
        startLocationUpdates()
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        locationManager = lm
        if (!hasLocationPermission()) return
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
            if (runCatching { lm.isProviderEnabled(provider) }.getOrDefault(false)) {
                runCatching {
                    lm.requestLocationUpdates(
                        provider, MIN_TIME_MS, MIN_DISTANCE_M, listener, Looper.getMainLooper(),
                    )
                }
            }
        }
    }

    private fun handleLocation(location: Location) {
        ServiceState.updateLocation(location)
        val s = settings.current()
        val heading = headingOf(location)
        val result = engine.onLocationUpdate(location.latitude, location.longitude, s.radiusMeters, heading)
        publish(result, heading)
        updateNotification(notificationText(result, heading))
        maybeRequery(location, s)
    }

    private fun publish(result: ProximityResult, heading: Double?) {
        SeenRepository.get(this).markSeen(result.nearby.map { it.poi.id })
        val announcement = result.primary?.let {
            Announcement.build(it, heading, result.othersAtSimilarDistance)
        }
        ServiceState.publishProximity(
            nearby = result.nearby,
            primary = result.primary,
            othersAtSimilarDistance = result.othersAtSimilarDistance,
            announcement = announcement,
        )
        maybeSpeak(result.primary?.poi?.id, announcement)
    }

    /** Spricht die Ansage bei neuem primärem POI bzw. periodisch (Meldemodus). */
    private fun maybeSpeak(primaryId: String?, announcement: String?) {
        val s = settings.current()
        if (primaryId == null) {
            lastAnnouncedPrimaryId = null
            return
        }
        if (!s.ttsEnabled || announcement == null) return
        val now = System.currentTimeMillis()
        val isNew = primaryId != lastAnnouncedPrimaryId
        val periodicDue = s.alertMode == com.poimelder.app.settings.AlertMode.PERIODIC &&
            (now - lastAnnounceTimeMs) >= s.repeatIntervalMinutes * 60_000L
        if (isNew || periodicDue) {
            tts?.speak(announcement)
            lastAnnouncedPrimaryId = primaryId
            lastAnnounceTimeMs = now
        }
    }

    private fun maybeRequery(location: Location, s: com.poimelder.app.settings.AppSettings) {
        if (querying) return
        val threshold = s.effectiveRequeryDistanceMeters()
        val now = System.currentTimeMillis()
        if (!RequeryPolicy.shouldRequery(
                lastQuery, location.latitude, location.longitude, now,
                threshold, s.requeryMinIntervalSec,
            )
        ) {
            return
        }
        querying = true
        lastQuery = RequeryPolicy.LastQuery(location.latitude, location.longitude, now)
        serviceScope.launch {
            try {
                val categories = s.activeCategories()
                if (categories.isEmpty()) {
                    ServiceState.setStatus("Keine Kategorie gewählt (siehe Einstellungen).")
                    return@launch
                }
                ServiceState.setStatus("Lade POIs …")
                val list = overpass.fetchPois(location.latitude, location.longitude, s.radiusMeters, categories)
                engine.updatePois(list)
                ServiceState.setRegion(
                    LocationProvider.regionLabel(this@LocationService, location.latitude, location.longitude),
                )
                val cur = ServiceState.location.value ?: location
                val heading = headingOf(cur)
                val result = engine.onLocationUpdate(cur.latitude, cur.longitude, s.radiusMeters, heading)
                publish(result, heading)
                updateNotification(notificationText(result, heading))
                ServiceState.setStatus("${list.size} POIs geladen.")
            } catch (e: Exception) {
                ServiceState.setStatus("Overpass-Fehler: ${e.message}")
            } finally {
                querying = false
            }
        }
    }

    private fun notificationText(result: ProximityResult, heading: Double?): String {
        if (!settings.current().notificationEnabled) {
            return "Melder läuft – Benachrichtigungen aus"
        }
        val primary = result.primary ?: return "Suche POIs in der Nähe …"
        return Announcement.build(primary, heading, result.othersAtSimilarDistance)
    }

    private fun headingOf(location: Location): Double? =
        if (location.hasBearing() && location.hasSpeed() && location.speed > 1.0f) {
            location.bearing.toDouble()
        } else {
            null
        }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        runCatching { locationManager?.removeUpdates(listener) }
        serviceScope.cancel()
        tts?.shutdown()
        tts = null
        ServiceState.setRunning(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "POIMelder Standort", NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Dauerhafte Standortverfolgung im Hintergrund" }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, LocationService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("POIMelder aktiv")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, "Stoppen", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    companion object {
        private const val CHANNEL_ID = "poimelder_service"
        private const val NOTIF_ID = 1001
        private const val MIN_TIME_MS = 5_000L
        private const val MIN_DISTANCE_M = 10f
        const val ACTION_STOP = "com.poimelder.app.action.STOP"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, LocationService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationService::class.java))
        }
    }
}
