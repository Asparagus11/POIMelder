package com.poimelder.app.geo

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geografische Hilfsfunktionen. Reines Kotlin, damit sie in JVM-Unit-Tests laufen.
 */
object GeoUtils {

    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Entfernung zwischen zwei Punkten in Metern (Haversine). */
    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(rLat1) * cos(rLat2) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }

    /** Anfangspeilung (Kurs) von Punkt 1 nach Punkt 2 in Grad [0,360). */
    fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(rLat2)
        val x = cos(rLat1) * sin(rLat2) - sin(rLat1) * cos(rLat2) * cos(dLon)
        val deg = Math.toDegrees(atan2(y, x))
        return (deg + 360.0) % 360.0
    }

    private val COMPASS = listOf("N", "NO", "O", "SO", "S", "SW", "W", "NW")

    /** Deutsche 8-Sektoren-Himmelsrichtung für eine Peilung in Grad. */
    fun compassDirection(bearingDeg: Double): String {
        val normalized = (bearingDeg % 360.0 + 360.0) % 360.0
        val index = (normalized / 45.0).roundToInt() % 8
        return COMPASS[index]
    }

    /** Himmelsrichtung direkt aus zwei Koordinaten. */
    fun compassDirection(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String =
        compassDirection(bearing(lat1, lon1, lat2, lon2))
}
