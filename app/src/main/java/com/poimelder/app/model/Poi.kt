package com.poimelder.app.model

/**
 * Ein Point of Interest aus OpenStreetMap.
 *
 * @param id      OSM-Element-ID inkl. Typ, z.B. "node/123".
 * @param name    Anzeigename (aus tag `name`), oder ein aussagekräftiger Fallback.
 * @param lat     Breitengrad (bei ways/relations der Center-Punkt).
 * @param lon     Längengrad.
 * @param categoryId  ID der Kategorie, unter der dieser POI gefunden wurde.
 * @param kind    Konkrete Typbezeichnung (z.B. „Burg/Schloss", „Kloster"), falls ableitbar.
 * @param extras  Ausgewählte weitere OSM-Tags (Öffnungszeiten, Website, Beschreibung …).
 */
data class Poi(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val categoryId: String,
    val kind: String? = null,
    val extras: Map<String, String> = emptyMap(),
) {
    /** Kurze, menschenlesbare Detailzeile aus den wichtigsten Extra-Tags. */
    fun detailLine(): String? {
        val parts = mutableListOf<String>()
        extras["description"]?.let { parts.add(it) }
        extras["opening_hours"]?.let { parts.add("Öffnungszeiten: $it") }
        extras["website"]?.let { parts.add(it) }
        extras["phone"]?.let { parts.add("Tel. $it") }
        return parts.take(2).joinToString(" · ").ifBlank { null }
    }
}
