package com.poimelder.app.geo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoUtilsTest {

    @Test
    fun haversine_samepoint_iszero() {
        assertEquals(0.0, GeoUtils.haversineMeters(52.0, 13.0, 52.0, 13.0), 0.001)
    }

    @Test
    fun haversine_berlin_to_hamburg() {
        // Berlin (52.5200, 13.4050) -> Hamburg (53.5511, 9.9937): ~255 km
        val d = GeoUtils.haversineMeters(52.5200, 13.4050, 53.5511, 9.9937)
        assertTrue("Distanz war $d", d in 250_000.0..262_000.0)
    }

    @Test
    fun haversine_oneDegreeLat_isAbout111km() {
        val d = GeoUtils.haversineMeters(0.0, 0.0, 1.0, 0.0)
        assertTrue("Distanz war $d", d in 110_000.0..112_000.0)
    }

    @Test
    fun bearing_north() {
        val b = GeoUtils.bearing(0.0, 0.0, 1.0, 0.0)
        assertEquals(0.0, b, 1.0)
        assertEquals("N", GeoUtils.compassDirection(b))
    }

    @Test
    fun bearing_east() {
        val b = GeoUtils.bearing(0.0, 0.0, 0.0, 1.0)
        assertEquals(90.0, b, 1.0)
        assertEquals("O", GeoUtils.compassDirection(b))
    }

    @Test
    fun compass_sectors() {
        assertEquals("N", GeoUtils.compassDirection(0.0))
        assertEquals("NO", GeoUtils.compassDirection(45.0))
        assertEquals("O", GeoUtils.compassDirection(90.0))
        assertEquals("S", GeoUtils.compassDirection(180.0))
        assertEquals("W", GeoUtils.compassDirection(270.0))
        assertEquals("N", GeoUtils.compassDirection(360.0))
        assertEquals("NW", GeoUtils.compassDirection(315.0))
    }
}
