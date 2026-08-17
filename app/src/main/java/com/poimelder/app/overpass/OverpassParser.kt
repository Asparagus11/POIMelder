package com.poimelder.app.overpass

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.poimelder.app.model.Category
import com.poimelder.app.model.OsmType
import com.poimelder.app.model.Poi

/**
 * Parst Overpass-JSON zu einer POI-Liste. Für ways/relations wird der Center-Punkt
 * verwendet. Die Kategorie wird durch Abgleich der Element-Tags mit den angefragten
 * Kategorien bestimmt.
 */
object OverpassParser {

    fun parse(json: String, categories: List<Category>): List<Poi> {
        val root = runCatching { JsonParser.parseString(json).asJsonObject }
            .getOrNull() ?: return emptyList()
        val elements = root.getAsJsonArray("elements") ?: return emptyList()

        val result = ArrayList<Poi>(elements.size())
        for (el in elements) {
            val obj = el as? JsonObject ?: continue
            val type = obj.get("type")?.asString ?: continue
            val id = obj.get("id")?.asString() ?: continue

            val (lat, lon) = coordinates(obj) ?: continue

            val tags = obj.getAsJsonObject("tags")
            val tagMap: Map<String, String> = tags?.entrySet()
                ?.associate { it.key to (runCatching { it.value.asString }.getOrDefault("")) }
                ?: emptyMap()

            val category = matchCategory(tagMap, categories) ?: continue
            val kind = OsmType.describe(tagMap)
            val name = tagMap["name"]
                ?: tagMap["brand"]
                ?: tagMap["operator"]
                ?: kind
                ?: category.label

            result.add(
                Poi(
                    id = "$type/$id",
                    name = name,
                    lat = lat,
                    lon = lon,
                    categoryId = category.id,
                    kind = kind,
                    extras = extractExtras(tagMap),
                ),
            )
        }
        return result
    }

    private val EXTRA_KEYS = listOf(
        "description", "opening_hours", "website", "url", "contact:website",
        "phone", "contact:phone", "wikipedia", "wikidata", "cuisine",
        "religion", "denomination", "historic", "tourism", "operator", "ele",
    )

    private fun extractExtras(tags: Map<String, String>): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (key in EXTRA_KEYS) {
            val v = tags[key]
            if (!v.isNullOrBlank()) {
                // Normalisiere Website/Telefon-Aliase auf einheitliche Keys.
                val normKey = when (key) {
                    "url", "contact:website" -> "website"
                    "contact:phone" -> "phone"
                    else -> key
                }
                out.putIfAbsent(normKey, v)
            }
        }
        return out
    }

    private fun coordinates(obj: JsonObject): Pair<Double, Double>? {
        // node: lat/lon direkt; way/relation: center.{lat,lon}
        val lat = obj.get("lat")?.asDouble
        val lon = obj.get("lon")?.asDouble
        if (lat != null && lon != null) return lat to lon
        val center = obj.getAsJsonObject("center") ?: return null
        val cLat = center.get("lat")?.asDouble ?: return null
        val cLon = center.get("lon")?.asDouble ?: return null
        return cLat to cLon
    }

    private fun matchCategory(tags: Map<String, String>, categories: List<Category>): Category? {
        for (category in categories) {
            for (tag in category.tags) {
                if (tag.matches(tags)) return category
            }
        }
        return null
    }

    private fun com.google.gson.JsonElement.asString(): String? =
        runCatching { asString }.getOrNull()
            ?: runCatching { asLong.toString() }.getOrNull()
}
