package com.poimelder.app.proximity

import com.poimelder.app.model.Poi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnouncementTest {

    private fun nearby(dist: Double, bearing: Double, compass: String) =
        NearbyPoi(Poi("id", "Wallfahrtskirche XY", 1.0, 2.0, "worship"), dist, bearing, compass)

    @Test
    fun relativeDirection_sectors() {
        assertEquals("voraus", Announcement.relativeDirection(0.0, 0.0))
        assertEquals("rechts", Announcement.relativeDirection(90.0, 0.0))
        assertEquals("links", Announcement.relativeDirection(270.0, 0.0))
        assertEquals("hinter dir", Announcement.relativeDirection(180.0, 0.0))
        // relativ zur Fahrtrichtung Ost (90°)
        assertEquals("voraus", Announcement.relativeDirection(90.0, 90.0))
        assertEquals("rechts", Announcement.relativeDirection(180.0, 90.0))
    }

    @Test
    fun build_withHeading_usesRelativeDirection() {
        val text = Announcement.build(nearby(3000.0, 90.0, "O"), headingDeg = 0.0)
        assertTrue(text, text.contains("rechts"))
        assertTrue(text, text.contains("Wallfahrtskirche XY"))
        assertTrue(text, text.contains("km"))
    }

    @Test
    fun build_withoutHeading_usesCompass() {
        val text = Announcement.build(nearby(800.0, 180.0, "S"), headingDeg = null)
        assertTrue(text, text.contains("Süden"))
        assertTrue(text, text.contains("800 m"))
    }

    @Test
    fun build_withOthers_appendsHint() {
        val text = Announcement.build(nearby(500.0, 0.0, "N"), headingDeg = 0.0, othersAtSimilarDistance = 2)
        assertTrue(text, text.contains("2 weitere"))
    }

    @Test
    fun formatDistance_roundsSensibly() {
        assertEquals("300 m", Announcement.formatDistance(305.0))
        assertEquals("0 m", Announcement.formatDistance(10.0))
        assertTrue(Announcement.formatDistance(2000.0).contains("km"))
        assertTrue(Announcement.formatDistance(15000.0).contains("km"))
    }
}

class RequeryPolicyTest {

    private val origin = RequeryPolicy.LastQuery(0.0, 0.0, timeMs = 0L)

    @Test
    fun firstQuery_whenNoPrevious() {
        assertTrue(RequeryPolicy.shouldRequery(null, 0.0, 0.0, 1000L, 500, 60))
    }

    @Test
    fun notEnoughMovement_returnsFalse() {
        // ~100 m Bewegung, Schwelle 200 m
        assertFalse(
            RequeryPolicy.shouldRequery(origin, 0.0009, 0.0, 999_000L, 200, 60),
        )
    }

    @Test
    fun enoughMovementAndTime_returnsTrue() {
        // ~300 m Bewegung, Schwelle 200 m, 120 s vergangen
        assertTrue(
            RequeryPolicy.shouldRequery(origin, 0.0027, 0.0, 120_000L, 200, 60),
        )
    }

    @Test
    fun enoughMovementButTooSoon_returnsFalse() {
        // ~300 m Bewegung, aber erst 10 s vergangen (min 60 s)
        assertFalse(
            RequeryPolicy.shouldRequery(origin, 0.0027, 0.0, 10_000L, 200, 60),
        )
    }
}
