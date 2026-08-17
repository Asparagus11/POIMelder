package com.poimelder.app.proximity

import com.poimelder.app.geo.GeoUtils

/**
 * Erzeugt einen deutschen Ansagetext wie
 * „In 3,0 km rechts voraus befindet sich Wallfahrtskirche XY".
 * Vorerst rein textuell (Sprachausgabe folgt in Task 8).
 */
object Announcement {

    fun build(nearby: NearbyPoi, headingDeg: Double?, othersAtSimilarDistance: Int = 0): String {
        val dist = formatDistance(nearby.distanceMeters)
        val direction = if (headingDeg != null) {
            relativeDirection(nearby.bearingDeg, headingDeg)
        } else {
            "im ${compassLong(nearby.compass)}"
        }
        val base = "In $dist $direction befindet sich ${nearby.poi.name}"
        return if (othersAtSimilarDistance > 0) {
            "$base (und $othersAtSimilarDistance weitere in ähnlicher Entfernung)"
        } else {
            base
        }
    }

    fun formatDistance(meters: Double): String = when {
        meters >= 10_000 -> "%.0f km".format(meters / 1000.0)
        meters >= 1_000 -> "%.1f km".format(meters / 1000.0)
        else -> {
            val rounded = (Math.round(meters / 50.0) * 50).toInt().coerceAtLeast(0)
            "$rounded m"
        }
    }

    /** Relative Richtung zur Fahrtrichtung (8 Sektoren). */
    fun relativeDirection(bearingDeg: Double, headingDeg: Double): String {
        var diff = (bearingDeg - headingDeg) % 360.0
        if (diff < 0) diff += 360.0
        return when {
            diff <= 22.5 || diff > 337.5 -> "voraus"
            diff <= 67.5 -> "rechts voraus"
            diff <= 112.5 -> "rechts"
            diff <= 157.5 -> "rechts hinten"
            diff <= 202.5 -> "hinter dir"
            diff <= 247.5 -> "links hinten"
            diff <= 292.5 -> "links"
            else -> "links voraus"
        }
    }

    private fun compassLong(compass: String): String = when (compass) {
        "N" -> "Norden"
        "NO" -> "Nordosten"
        "O" -> "Osten"
        "SO" -> "Südosten"
        "S" -> "Süden"
        "SW" -> "Südwesten"
        "W" -> "Westen"
        "NW" -> "Nordwesten"
        else -> compass
    }
}

/**
 * Entscheidet, ob eine neue Overpass-Abfrage fällig ist: nur wenn die Bewegung seit der
 * letzten Abfrage die Distanzschwelle überschreitet UND der Mindest-Zeitabstand vorbei ist.
 */
object RequeryPolicy {

    data class LastQuery(val lat: Double, val lon: Double, val timeMs: Long)

    fun shouldRequery(
        last: LastQuery?,
        curLat: Double,
        curLon: Double,
        nowMs: Long,
        distanceThresholdMeters: Int,
        minIntervalSec: Int,
    ): Boolean {
        if (last == null) return true
        val moved = GeoUtils.haversineMeters(last.lat, last.lon, curLat, curLon)
        val movedEnough = moved >= distanceThresholdMeters
        val timeOk = (nowMs - last.timeMs) >= minIntervalSec * 1000L
        return movedEnough && timeOk
    }
}
