package com.poimelder.app.overpass

import com.poimelder.app.model.Categories
import org.junit.Assert.assertTrue
import org.junit.Test

class OverpassQueryTest {

    @Test
    fun build_containsAroundNodeWayRelation_forEachTag() {
        val fuel = Categories.standardById("fuel")!!
        val q = OverpassQuery.build(52.5, 13.405, 5000, listOf(fuel))
        assertTrue(q.contains("[out:json]"))
        assertTrue(q.contains("node[\"amenity\"=\"fuel\"](around:5000,52.500000,13.405000);"))
        assertTrue(q.contains("way[\"amenity\"=\"fuel\"](around:5000,52.500000,13.405000);"))
        assertTrue(q.contains("relation[\"amenity\"=\"fuel\"](around:5000,52.500000,13.405000);"))
        assertTrue(q.contains("out center tags;"))
    }

    @Test
    fun build_multiTagCategory_emitsAllTags() {
        val food = Categories.standardById("food")!!
        val q = OverpassQuery.build(0.0, 0.0, 1000, listOf(food))
        assertTrue(q.contains("[\"amenity\"=\"restaurant\"]"))
        assertTrue(q.contains("[\"amenity\"=\"cafe\"]"))
    }

    @Test
    fun build_usesUsLocaleForCoordinates() {
        val fuel = Categories.standardById("fuel")!!
        val q = OverpassQuery.build(48.137154, 11.576124, 2500, listOf(fuel))
        // Punkt als Dezimaltrenner, kein Komma
        assertTrue(q.contains("48.137154,11.576124"))
    }
}
