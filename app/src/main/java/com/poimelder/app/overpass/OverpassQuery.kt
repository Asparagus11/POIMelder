package com.poimelder.app.overpass

import com.poimelder.app.model.Category

/**
 * Baut Overpass-QL-Abfragen. Getrennt vom Netzwerk-Client, damit reine JVM-Tests
 * die Query-Erzeugung prüfen können.
 */
object OverpassQuery {

    /**
     * Erzeugt eine Overpass-Abfrage, die node/way/relation für alle Tags der
     * angegebenen Kategorien im Umkreis (Meter) um (lat, lon) sucht und Center-Punkte
     * ausgibt.
     */
    fun build(
        lat: Double,
        lon: Double,
        radiusMeters: Int,
        categories: List<Category>,
        timeoutSec: Int = 25,
    ): String {
        val around = "(around:$radiusMeters,${fmt(lat)},${fmt(lon)})"
        val body = StringBuilder()
        val tags = categories.flatMap { it.tags }.distinct()
        for (tag in tags) {
            val filter = tag.toOverpassFilter()
            body.append("  node$filter$around;\n")
            body.append("  way$filter$around;\n")
            body.append("  relation$filter$around;\n")
        }
        return buildString {
            append("[out:json][timeout:$timeoutSec];\n")
            append("(\n")
            append(body)
            append(");\n")
            append("out center tags;\n")
        }
    }

    /** Koordinaten mit fester Präzision, unabhängig von der Locale. */
    private fun fmt(v: Double): String = String.format(java.util.Locale.US, "%.6f", v)
}
