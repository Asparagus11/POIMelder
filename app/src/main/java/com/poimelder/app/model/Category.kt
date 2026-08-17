package com.poimelder.app.model

/** Ein OSM key=value Tag-Filter. Wert "*" bedeutet: Schlüssel vorhanden (beliebiger Wert). */
data class OsmTag(val key: String, val value: String) {

    val isWildcard: Boolean get() = value == "*"

    /** Overpass-Filter-Schreibweise, z.B. ["amenity"="fuel"] bzw. ["historic"]. */
    fun toOverpassFilter(): String =
        if (isWildcard) "[\"$key\"]" else "[\"$key\"=\"$value\"]"

    /** Passt dieser Tag auf die gegebenen Element-Tags? */
    fun matches(tags: Map<String, String>): Boolean =
        if (isWildcard) tags.containsKey(key) else tags[key] == value
}

/**
 * Eine Melde-Kategorie. Bündelt einen oder mehrere OSM-Tags (ODER-verknüpft).
 *
 * @param custom true, wenn vom Nutzer über key=value definiert.
 */
data class Category(
    val id: String,
    val label: String,
    val tags: List<OsmTag>,
    val custom: Boolean = false,
)

object Categories {

    /** Festes Standard-Set gemäß Spec (Sehenswürdigkeit bewusst breit getaggt). */
    val STANDARD: List<Category> = listOf(
        Category("fuel", "Tankstelle", listOf(OsmTag("amenity", "fuel"))),
        Category(
            "food",
            "Restaurant/Café",
            listOf(OsmTag("amenity", "restaurant"), OsmTag("amenity", "cafe")),
        ),
        Category("supermarket", "Supermarkt", listOf(OsmTag("shop", "supermarket"))),
        Category(
            "sights",
            "Sehenswürdigkeit",
            listOf(
                OsmTag("tourism", "attraction"),
                OsmTag("tourism", "viewpoint"),
                OsmTag("tourism", "museum"),
                OsmTag("tourism", "artwork"),
                OsmTag("historic", "*"),
            ),
        ),
        Category(
            "worship",
            "Kirche/Wallfahrt",
            listOf(OsmTag("amenity", "place_of_worship")),
        ),
        Category("rest_area", "Rastplatz", listOf(OsmTag("highway", "rest_area"))),
        Category("toilets", "Toilette", listOf(OsmTag("amenity", "toilets"))),
        Category("water", "Trinkwasser", listOf(OsmTag("amenity", "drinking_water"))),
        Category("bicycle", "Fahrradladen", listOf(OsmTag("shop", "bicycle"))),
    )

    private val byId: Map<String, Category> = STANDARD.associateBy { it.id }

    fun standardById(id: String): Category? = byId[id]

    private val TAG_TOKEN = Regex("^[A-Za-z0-9_:-]+$")

    /**
     * Parst einen benutzerdefinierten "key=value"-Tag (value darf "*" sein).
     * Gibt null zurück, wenn das Format ungültig ist.
     */
    fun parseCustomTag(raw: String): OsmTag? {
        val trimmed = raw.trim()
        val idx = trimmed.indexOf('=')
        if (idx <= 0 || idx == trimmed.length - 1) return null
        val key = trimmed.substring(0, idx).trim()
        val value = trimmed.substring(idx + 1).trim()
        if (!TAG_TOKEN.matches(key)) return null
        if (value != "*" && !TAG_TOKEN.matches(value)) return null
        return OsmTag(key, value)
    }

    /** Baut eine Custom-Kategorie aus einem "key=value"-String, oder null bei Fehler. */
    fun customCategory(raw: String): Category? {
        val tag = parseCustomTag(raw) ?: return null
        val id = "custom:${tag.key}=${tag.value}"
        return Category(id, "${tag.key}=${tag.value}", listOf(tag), custom = true)
    }
}
