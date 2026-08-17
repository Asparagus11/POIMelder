package com.poimelder.app.proximity

import com.poimelder.app.geo.GeoUtils
import com.poimelder.app.model.Poi
import kotlin.math.abs

/**
 * Hält die zuletzt geladene POI-Liste und berechnet bei jedem Standort-Update, welche
 * POIs im Radius liegen, sowie Enter/Exit-Ereignisse pro POI.
 *
 * @param tieToleranceMeters POIs, deren Entfernung sich um höchstens diesen Wert vom
 *   nächstgelegenen unterscheidet, gelten als „etwa gleich weit entfernt".
 */
class ProximityEngine(
    private val tieToleranceMeters: Double = 150.0,
    private val hysteresisMeters: Double = 200.0,
) {

    private var pois: List<Poi> = emptyList()
    private val insideIds = LinkedHashSet<String>()
    private var lastPrimaryId: String? = null

    fun updatePois(newPois: List<Poi>) {
        pois = newPois
        val validIds = newPois.mapTo(HashSet()) { it.id }
        insideIds.retainAll(validIds)
        if (lastPrimaryId !in validIds) lastPrimaryId = null
    }

    fun currentPois(): List<Poi> = pois

    fun onLocationUpdate(
        lat: Double,
        lon: Double,
        radiusMeters: Int,
        headingDeg: Double? = null,
    ): ProximityResult {
        val within = pois.map { p ->
            val d = GeoUtils.haversineMeters(lat, lon, p.lat, p.lon)
            val b = GeoUtils.bearing(lat, lon, p.lat, p.lon)
            NearbyPoi(p, d, b, GeoUtils.compassDirection(b))
        }
            .filter { it.distanceMeters <= radiusMeters }
            .sortedBy { it.distanceMeters }

        val withinIds = within.mapTo(LinkedHashSet()) { it.poi.id }
        val entered = within.filter { it.poi.id !in insideIds }.map { it.poi }
        val exited = insideIds.filter { it !in withinIds }

        insideIds.clear()
        insideIds.addAll(withinIds)

        val cluster = nearestCluster(within)
        val candidate = choosePrimary(cluster, headingDeg)
        val primary = applyHysteresis(candidate, within)
        lastPrimaryId = primary?.poi?.id
        val others = (cluster.size - 1).coerceAtLeast(0)

        return ProximityResult(within, entered, exited, primary, others)
    }

    /**
     * Verhindert schnelles Hin- und Herspringen des primären POI: der bisherige primäre
     * POI bleibt, solange er nicht deutlich (> hysteresisMeters) weiter entfernt ist als
     * der neue Kandidat.
     */
    private fun applyHysteresis(candidate: NearbyPoi?, within: List<NearbyPoi>): NearbyPoi? {
        if (candidate == null) return null
        val prev = within.firstOrNull { it.poi.id == lastPrimaryId } ?: return candidate
        if (prev.poi.id == candidate.poi.id) return candidate
        return if (prev.distanceMeters <= candidate.distanceMeters + hysteresisMeters) prev else candidate
    }

    /** POIs, deren Entfernung nah am nächstgelegenen liegt (Gleichstand-Cluster). */
    private fun nearestCluster(within: List<NearbyPoi>): List<NearbyPoi> {
        if (within.isEmpty()) return emptyList()
        val nearest = within.first().distanceMeters
        return within.filter { it.distanceMeters - nearest <= tieToleranceMeters }
    }

    private fun choosePrimary(cluster: List<NearbyPoi>, headingDeg: Double?): NearbyPoi? {
        if (cluster.isEmpty()) return null
        if (cluster.size == 1 || headingDeg == null) return cluster.first()
        // Bei Gleichstand den POI wählen, der am ehesten in Fahrtrichtung liegt.
        return cluster.minByOrNull { relativeAngle(it.bearingDeg, headingDeg) } ?: cluster.first()
    }

    companion object {
        /** Betrag des Winkels zwischen Peilung und Kurs, normalisiert auf [0,180]. */
        fun relativeAngle(bearingDeg: Double, headingDeg: Double): Double {
            var diff = (bearingDeg - headingDeg) % 360.0
            if (diff < -180.0) diff += 360.0
            if (diff > 180.0) diff -= 360.0
            return abs(diff)
        }
    }
}
