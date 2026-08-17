package com.poimelder.app.proximity

import com.poimelder.app.model.Poi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProximityEngineTest {

    // Am Äquator: 0.001° ≈ 111 m.
    private val poiEast = Poi("A", "Ost", 0.0, 0.001, "fuel")   // Peilung ~90°
    private val poiNorth = Poi("B", "Nord", 0.001, 0.0, "fuel") // Peilung ~0°
    private val poiFar = Poi("C", "Fern", 0.02, 0.0, "fuel")    // ~2226 m nördlich

    @Test
    fun nearby_filtersByRadiusAndSortsByDistance() {
        val engine = ProximityEngine()
        engine.updatePois(listOf(poiFar, poiEast, poiNorth))
        val res = engine.onLocationUpdate(0.0, 0.0, radiusMeters = 200)
        // C (2226 m) fällt raus, A und B bleiben
        assertEquals(listOf("A", "B").sorted(), res.nearby.map { it.poi.id }.sorted())
        assertTrue(res.nearby.first().distanceMeters <= res.nearby.last().distanceMeters)
    }

    @Test
    fun enterAndExit_transitions() {
        val engine = ProximityEngine()
        engine.updatePois(listOf(poiEast, poiNorth, poiFar))

        val first = engine.onLocationUpdate(0.0, 0.0, radiusMeters = 5000)
        assertEquals(setOf("A", "B", "C"), first.entered.map { it.id }.toSet())
        assertTrue(first.exited.isEmpty())

        val second = engine.onLocationUpdate(0.0, 0.0, radiusMeters = 200)
        assertTrue(second.entered.isEmpty())
        assertEquals(listOf("C"), second.exited)

        val third = engine.onLocationUpdate(10.0, 10.0, radiusMeters = 5000)
        assertEquals(setOf("A", "B"), third.exited.toSet())
    }

    @Test
    fun primary_tieBrokenByHeading() {
        val engineNorth = ProximityEngine()
        engineNorth.updatePois(listOf(poiEast, poiNorth))
        val headingNorth = engineNorth.onLocationUpdate(0.0, 0.0, 5000, headingDeg = 0.0)
        assertEquals("B", headingNorth.primary?.poi?.id) // Nord voraus
        assertEquals(1, headingNorth.othersAtSimilarDistance)

        val engineEast = ProximityEngine()
        engineEast.updatePois(listOf(poiEast, poiNorth))
        val headingEast = engineEast.onLocationUpdate(0.0, 0.0, 5000, headingDeg = 90.0)
        assertEquals("A", headingEast.primary?.poi?.id) // Ost voraus
    }

    @Test
    fun primary_stickyWithHysteresis() {
        // A nördlich (111 m), B südlich (133 m)
        val a = Poi("A", "A", 0.001, 0.0, "fuel")
        val b = Poi("B", "B", -0.0012, 0.0, "fuel")
        val engine = ProximityEngine(hysteresisMeters = 200.0)
        engine.updatePois(listOf(a, b))

        // Start am Ursprung: A ist näher -> primary A
        assertEquals("A", engine.onLocationUpdate(0.0, 0.0, 5000).primary?.poi?.id)

        // Etwas nach Süden: B jetzt näher, aber nur ~89 m Vorsprung < Hysterese -> bleibt A
        assertEquals("A", engine.onLocationUpdate(-0.0005, 0.0, 5000).primary?.poi?.id)

        // Deutlich weiter nach Süden: B klar näher (> Hysterese) -> wechselt zu B
        assertEquals("B", engine.onLocationUpdate(-0.002, 0.0, 5000).primary?.poi?.id)
    }

    @Test
    fun updatePois_dropsStaleInsideIds() {
        val engine = ProximityEngine()
        engine.updatePois(listOf(poiEast))
        engine.onLocationUpdate(0.0, 0.0, 5000) // A ist jetzt "inside"
        engine.updatePois(listOf(poiNorth))     // A verschwindet, B neu
        val res = engine.onLocationUpdate(0.0, 0.0, 5000)
        // B muss als Enter gemeldet werden (nicht als bereits-drinnen unterdrückt)
        assertEquals(listOf("B"), res.entered.map { it.id })
    }
}
