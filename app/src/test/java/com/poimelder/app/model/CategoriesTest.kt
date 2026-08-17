package com.poimelder.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoriesTest {

    @Test
    fun standardSet_hasExpectedMappings() {
        val fuel = Categories.standardById("fuel")!!
        assertEquals(listOf(OsmTag("amenity", "fuel")), fuel.tags)

        val food = Categories.standardById("food")!!
        assertTrue(food.tags.contains(OsmTag("amenity", "restaurant")))
        assertTrue(food.tags.contains(OsmTag("amenity", "cafe")))

        assertEquals(OsmTag("shop", "supermarket"), Categories.standardById("supermarket")!!.tags.single())
        assertEquals(OsmTag("amenity", "drinking_water"), Categories.standardById("water")!!.tags.single())
    }

    @Test
    fun overpassFilter_format() {
        assertEquals("[\"amenity\"=\"fuel\"]", OsmTag("amenity", "fuel").toOverpassFilter())
    }

    @Test
    fun parseCustomTag_valid() {
        assertEquals(OsmTag("amenity", "pharmacy"), Categories.parseCustomTag("amenity=pharmacy"))
        assertEquals(OsmTag("shop", "bakery"), Categories.parseCustomTag("  shop = bakery "))
    }

    @Test
    fun parseCustomTag_invalid() {
        assertNull(Categories.parseCustomTag("nokey"))
        assertNull(Categories.parseCustomTag("=value"))
        assertNull(Categories.parseCustomTag("key="))
        assertNull(Categories.parseCustomTag("key=va lue"))
        assertNull(Categories.parseCustomTag(""))
    }

    @Test
    fun customCategory_isMarkedCustom() {
        val c = Categories.customCategory("amenity=hospital")!!
        assertTrue(c.custom)
        assertEquals("custom:amenity=hospital", c.id)
        assertEquals(listOf(OsmTag("amenity", "hospital")), c.tags)
    }
}
