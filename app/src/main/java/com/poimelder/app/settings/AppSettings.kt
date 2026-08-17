package com.poimelder.app.settings

import com.poimelder.app.model.Categories
import com.poimelder.app.model.Category

/** Meldemodus: bei Ein-/Austritt oder periodisch wiederholend. */
enum class AlertMode { ENTER_EXIT, PERIODIC }

/**
 * Alle persistenten Einstellungen der App. Reines Datenmodell (JVM-testbar).
 */
data class AppSettings(
    val radiusMeters: Int = 5000,
    val selectedCategoryIds: Set<String> = setOf("fuel", "supermarket"),
    val customTags: List<String> = emptyList(),
    val alertMode: AlertMode = AlertMode.ENTER_EXIT,
    val repeatIntervalMinutes: Int = 10,
    val ttsEnabled: Boolean = true,
    val notificationEnabled: Boolean = true,
    /** Bildschirm dauerhaft an (Always-On), während das Gerät am Kabel lädt. */
    val keepScreenOnCharging: Boolean = true,
    /** Bildschirm dauerhaft an (Always-On) im Akkubetrieb (ohne Kabel). */
    val keepScreenOnBattery: Boolean = false,
    /** Bewegungsschwelle für Overpass-Neuabfrage. 0 = automatisch (halber Radius). */
    val requeryDistanceMeters: Int = 0,
    val requeryMinIntervalSec: Int = 60,
) {
    /** Effektive Requery-Distanz: expliziter Wert, sonst halber Radius. */
    fun effectiveRequeryDistanceMeters(): Int =
        if (requeryDistanceMeters > 0) requeryDistanceMeters else radiusMeters / 2

    /** Baut die Liste aktiver Kategorien aus Standardauswahl + Custom-Tags. */
    fun activeCategories(): List<Category> {
        val standard = selectedCategoryIds.mapNotNull { Categories.standardById(it) }
        val custom = customTags.mapNotNull { Categories.customCategory(it) }
        return standard + custom
    }
}
