package com.poimelder.app.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsCodecTest {

    @Test
    fun roundTrip_preservesAllFields() {
        val original = AppSettings(
            radiusMeters = 8000,
            selectedCategoryIds = setOf("fuel", "toilets"),
            customTags = listOf("amenity=pharmacy", "shop=bakery"),
            alertMode = AlertMode.PERIODIC,
            repeatIntervalMinutes = 15,
            ttsEnabled = false,
            notificationEnabled = false,
            keepScreenOnCharging = false,
            keepScreenOnBattery = true,
            requeryDistanceMeters = 3000,
            requeryMinIntervalSec = 120,
        )
        val restored = SettingsCodec.fromMap(SettingsCodec.toMap(original))
        assertEquals(original, restored)
    }

    @Test
    fun fromEmptyMap_returnsDefaults() {
        val restored = SettingsCodec.fromMap(emptyMap())
        assertEquals(AppSettings(), restored)
    }

    @Test
    fun fromMap_invalidValues_fallBackToDefaults() {
        val map = mapOf(
            SettingsCodec.KEY_RADIUS to "abc",
            SettingsCodec.KEY_TTS to "notabool",
            SettingsCodec.KEY_ALERT_MODE to "BOGUS",
        )
        val restored = SettingsCodec.fromMap(map)
        val def = AppSettings()
        assertEquals(def.radiusMeters, restored.radiusMeters)
        assertEquals(def.ttsEnabled, restored.ttsEnabled)
        assertEquals(def.alertMode, restored.alertMode)
    }

    @Test
    fun customTags_emptyEntriesAreFiltered() {
        val map = mapOf(SettingsCodec.KEY_CUSTOM_TAGS to "amenity=pharmacy\n\n  \nshop=bakery")
        val restored = SettingsCodec.fromMap(map)
        assertEquals(listOf("amenity=pharmacy", "shop=bakery"), restored.customTags)
    }

    @Test
    fun effectiveRequeryDistance_defaultsToHalfRadius() {
        val s = AppSettings(radiusMeters = 6000, requeryDistanceMeters = 0)
        assertEquals(3000, s.effectiveRequeryDistanceMeters())

        val explicit = s.copy(requeryDistanceMeters = 1500)
        assertEquals(1500, explicit.effectiveRequeryDistanceMeters())
    }

    @Test
    fun activeCategories_combinesStandardAndCustom() {
        val s = AppSettings(
            selectedCategoryIds = setOf("fuel"),
            customTags = listOf("amenity=pharmacy", "invalid"),
        )
        val active = s.activeCategories()
        // fuel + pharmacy (invalid wird verworfen)
        assertEquals(2, active.size)
        assertTrue(active.any { it.id == "fuel" })
        assertTrue(active.any { it.id == "custom:amenity=pharmacy" })
    }
}
