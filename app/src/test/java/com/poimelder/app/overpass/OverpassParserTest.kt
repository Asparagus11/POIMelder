package com.poimelder.app.overpass

import com.poimelder.app.model.Categories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverpassParserTest {

    private val sampleJson = """
        {
          "version": 0.6,
          "elements": [
            {
              "type": "node",
              "id": 1001,
              "lat": 52.5001,
              "lon": 13.4001,
              "tags": { "amenity": "fuel", "name": "Aral Mitte" }
            },
            {
              "type": "way",
              "id": 2002,
              "center": { "lat": 52.5100, "lon": 13.4200 },
              "tags": { "shop": "supermarket", "name": "REWE City" }
            },
            {
              "type": "relation",
              "id": 3003,
              "center": { "lat": 52.4900, "lon": 13.3900 },
              "tags": { "tourism": "attraction" }
            },
            {
              "type": "node",
              "id": 4004,
              "lat": 52.50,
              "lon": 13.40,
              "tags": { "highway": "traffic_signals" }
            }
          ]
        }
    """.trimIndent()

    private val categories = listOf(
        Categories.standardById("fuel")!!,
        Categories.standardById("supermarket")!!,
        Categories.standardById("sights")!!,
    )

    @Test
    fun parse_extractsMatchingElementsWithCorrectCoords() {
        val pois = OverpassParser.parse(sampleJson, categories)
        // 3 passende Elemente; traffic_signals wird verworfen
        assertEquals(3, pois.size)

        val fuel = pois.first { it.id == "node/1001" }
        assertEquals("Aral Mitte", fuel.name)
        assertEquals("fuel", fuel.categoryId)
        assertEquals(52.5001, fuel.lat, 1e-9)
        assertEquals(13.4001, fuel.lon, 1e-9)

        val market = pois.first { it.id == "way/2002" }
        assertEquals("supermarket", market.categoryId)
        assertEquals(52.5100, market.lat, 1e-9) // Center verwendet

        val sight = pois.first { it.id == "relation/3003" }
        assertEquals("sights", sight.categoryId)
        // Kein name-Tag -> konkreter Typ aus tourism=attraction
        assertEquals("Attraktion", sight.name)
        assertEquals("Attraktion", sight.kind)
    }

    @Test
    fun parse_ignoresNonMatchingTags() {
        val pois = OverpassParser.parse(sampleJson, categories)
        assertNull(pois.firstOrNull { it.id == "node/4004" })
    }

    @Test
    fun parse_emptyOrBrokenJson_returnsEmpty() {
        assertTrue(OverpassParser.parse("", categories).isEmpty())
        assertTrue(OverpassParser.parse("not json", categories).isEmpty())
        assertTrue(OverpassParser.parse("""{"elements":[]}""", categories).isEmpty())
    }

    @Test
    fun parse_customCategory_matches() {
        val json = """
            {"elements":[
              {"type":"node","id":9,"lat":1.0,"lon":2.0,"tags":{"amenity":"pharmacy","name":"Apo"}}
            ]}
        """.trimIndent()
        val custom = Categories.customCategory("amenity=pharmacy")!!
        val pois = OverpassParser.parse(json, listOf(custom))
        assertEquals(1, pois.size)
        assertNotNull(pois.first())
        assertEquals("custom:amenity=pharmacy", pois.first().categoryId)
        assertEquals("Apo", pois.first().name)
    }
}
