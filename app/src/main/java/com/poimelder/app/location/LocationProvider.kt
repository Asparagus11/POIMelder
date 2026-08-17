package com.poimelder.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Dünner Wrapper um den Android-Framework-LocationManager (bewusst kein
 * Play-Services-Fused, siehe spec). Liefert den aktuellen Standort und eine
 * lesbare Regionsbezeichnung (Reverse-Geocoding).
 */
object LocationProvider {

    fun hasPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Aktueller Standort: zunächst ein hinreichend frischer Last-Known-Fix, sonst ein
     * einmaliges Update (mit Timeout). Gibt null zurück, wenn keine Berechtigung/kein Fix.
     */
    @SuppressLint("MissingPermission")
    suspend fun current(context: Context, timeoutMs: Long = 12_000): Location? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        val lastKnown = providers
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }

        val fresh = lastKnown != null &&
            (System.currentTimeMillis() - lastKnown.time) < 2 * 60_000L
        if (fresh) return lastKnown

        val single = withTimeoutOrNull(timeoutMs) { requestSingle(lm) }
        return single ?: lastKnown
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingle(lm: LocationManager): Location? =
        suspendCancellableCoroutine { cont ->
            val provider = when {
                runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) ->
                    LocationManager.GPS_PROVIDER
                runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) ->
                    LocationManager.NETWORK_PROVIDER
                else -> {
                    if (cont.isActive) cont.resume(null)
                    return@suspendCancellableCoroutine
                }
            }
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    runCatching { lm.removeUpdates(this) }
                    if (cont.isActive) cont.resume(location)
                }

                override fun onProviderDisabled(provider: String) {}
                override fun onProviderEnabled(provider: String) {}

                @Deprecated("Android API")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }
            // LocationManager-Aufrufe brauchen einen Looper – Main-Looper verwenden.
            Handler(Looper.getMainLooper()).post {
                runCatching {
                    lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                }.onFailure {
                    if (cont.isActive) cont.resume(null)
                }
            }
            cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
        }

    /** Lesbare Regionsbezeichnung (z.B. „München, Bayern"), oder null. */
    suspend fun regionLabel(context: Context, lat: Double, lon: Double): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocation(lat, lon, 1)
                val address = results?.firstOrNull() ?: return@withContext null
                listOfNotNull(
                    address.locality ?: address.subAdminArea,
                    address.adminArea,
                    address.countryName,
                ).distinct().take(2).joinToString(", ").ifBlank { null }
            }.getOrNull()
        }
}
