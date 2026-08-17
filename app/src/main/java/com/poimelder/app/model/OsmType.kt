package com.poimelder.app.model

/**
 * Leitet aus OSM-Tags eine konkrete, deutschsprachige Typbezeichnung ab
 * (z.B. „Burg/Schloss", „Denkmal", „Aussichtspunkt"). Viele OSM-Objekte haben kein
 * `name`-Tag; dann ist diese Bezeichnung deutlich aussagekräftiger als das generische
 * Kategorie-Label.
 */
object OsmType {

    private val HISTORIC = mapOf(
        "castle" to "Burg/Schloss",
        "ruins" to "Ruine",
        "monument" to "Denkmal",
        "memorial" to "Gedenkstätte",
        "archaeological_site" to "Ausgrabungsstätte",
        "church" to "Historische Kirche",
        "chapel" to "Kapelle",
        "monastery" to "Kloster",
        "wayside_shrine" to "Bildstock",
        "wayside_cross" to "Wegkreuz",
        "tower" to "Turm",
        "city_gate" to "Stadttor",
        "gate" to "Tor",
        "fort" to "Festung",
        "manor" to "Gutshaus",
        "farm" to "Historischer Hof",
        "building" to "Historisches Gebäude",
        "boundary_stone" to "Grenzstein",
        "milestone" to "Meilenstein",
        "yes" to "Historische Stätte",
    )

    private val TOURISM = mapOf(
        "attraction" to "Attraktion",
        "viewpoint" to "Aussichtspunkt",
        "museum" to "Museum",
        "artwork" to "Kunstwerk",
        "gallery" to "Galerie",
        "monument" to "Denkmal",
    )

    /** Beste verfügbare Typbezeichnung, oder null wenn nichts Spezifisches erkennbar. */
    fun describe(tags: Map<String, String>): String? {
        tags["historic"]?.let { v ->
            HISTORIC[v]?.let { return it }
            if (v.isNotBlank() && v != "yes") return prettify(v)
        }
        tags["tourism"]?.let { v ->
            TOURISM[v]?.let { return it }
            if (v.isNotBlank()) return prettify(v)
        }
        if (tags["amenity"] == "place_of_worship") {
            return when {
                tags["building"] == "chapel" -> "Kapelle"
                tags["religion"] == "christian" -> "Kirche"
                tags["religion"] != null -> "Gebetsstätte"
                else -> "Kirche"
            }
        }
        return null
    }

    /** Wandelt einen rohen Tag-Wert („wayside_shrine") in lesbaren Text um. */
    private fun prettify(raw: String): String =
        raw.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
