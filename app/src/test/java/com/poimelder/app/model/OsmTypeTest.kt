package com.poimelder.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OsmTypeTest {

    @Test
    fun historic_mappedToGerman() {
        assertEquals("Burg/Schloss", OsmType.describe(mapOf("historic" to "castle")))
        assertEquals("Denkmal", OsmType.describe(mapOf("historic" to "monument")))
        assertEquals("Bildstock", OsmType.describe(mapOf("historic" to "wayside_shrine")))
        assertEquals("Kloster", OsmType.describe(mapOf("historic" to "monastery")))
    }

    @Test
    fun historic_unknownValue_prettified() {
        assertEquals("Aqueduct", OsmType.describe(mapOf("historic" to "aqueduct")))
        // "yes" ist zu unspezifisch -> generische Bezeichnung
        assertEquals("Historische Stätte", OsmType.describe(mapOf("historic" to "yes")))
    }

    @Test
    fun tourism_mappedToGerman() {
        assertEquals("Aussichtspunkt", OsmType.describe(mapOf("tourism" to "viewpoint")))
        assertEquals("Museum", OsmType.describe(mapOf("tourism" to "museum")))
    }

    @Test
    fun placeOfWorship_usesReligion() {
        assertEquals("Kirche", OsmType.describe(mapOf("amenity" to "place_of_worship", "religion" to "christian")))
        assertEquals("Kapelle", OsmType.describe(mapOf("amenity" to "place_of_worship", "building" to "chapel")))
        assertEquals("Kirche", OsmType.describe(mapOf("amenity" to "place_of_worship")))
    }

    @Test
    fun historic_takesPrecedenceOverTourism() {
        val tags = mapOf("historic" to "castle", "tourism" to "attraction")
        assertEquals("Burg/Schloss", OsmType.describe(tags))
    }

    @Test
    fun nothingSpecific_returnsNull() {
        assertNull(OsmType.describe(mapOf("amenity" to "fuel")))
        assertNull(OsmType.describe(emptyMap()))
    }
}
