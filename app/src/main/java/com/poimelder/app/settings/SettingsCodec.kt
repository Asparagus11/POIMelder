package com.poimelder.app.settings

/**
 * Wandelt [AppSettings] in eine flache String-Map und zurück. Reine Logik ohne
 * Android-Abhängigkeiten, damit sie in JVM-Unit-Tests geprüft werden kann. Fehlende
 * oder kaputte Werte fallen auf die Defaults zurück.
 */
object SettingsCodec {

    const val KEY_RADIUS = "radius_m"
    const val KEY_CATEGORIES = "categories"
    const val KEY_CUSTOM_TAGS = "custom_tags"
    const val KEY_ALERT_MODE = "alert_mode"
    const val KEY_REPEAT_INTERVAL = "repeat_interval_min"
    const val KEY_TTS = "tts_enabled"
    const val KEY_NOTIF = "notif_enabled"
    const val KEY_KEEP_SCREEN_ON_CHARGING = "keep_screen_on_charging"
    const val KEY_KEEP_SCREEN_ON_BATTERY = "keep_screen_on_battery"
    const val KEY_REQUERY_DIST = "requery_dist_m"
    const val KEY_REQUERY_MIN_INTERVAL = "requery_min_interval_s"

    private const val LIST_SEP = "\n"

    fun toMap(s: AppSettings): Map<String, String> = mapOf(
        KEY_RADIUS to s.radiusMeters.toString(),
        KEY_CATEGORIES to s.selectedCategoryIds.joinToString(LIST_SEP),
        KEY_CUSTOM_TAGS to s.customTags.joinToString(LIST_SEP),
        KEY_ALERT_MODE to s.alertMode.name,
        KEY_REPEAT_INTERVAL to s.repeatIntervalMinutes.toString(),
        KEY_TTS to s.ttsEnabled.toString(),
        KEY_NOTIF to s.notificationEnabled.toString(),
        KEY_KEEP_SCREEN_ON_CHARGING to s.keepScreenOnCharging.toString(),
        KEY_KEEP_SCREEN_ON_BATTERY to s.keepScreenOnBattery.toString(),
        KEY_REQUERY_DIST to s.requeryDistanceMeters.toString(),
        KEY_REQUERY_MIN_INTERVAL to s.requeryMinIntervalSec.toString(),
    )

    fun fromMap(map: Map<String, String?>): AppSettings {
        val def = AppSettings()
        return AppSettings(
            radiusMeters = map.int(KEY_RADIUS, def.radiusMeters),
            selectedCategoryIds = map.list(KEY_CATEGORIES)?.toSet() ?: def.selectedCategoryIds,
            customTags = map.list(KEY_CUSTOM_TAGS) ?: def.customTags,
            alertMode = runCatching { AlertMode.valueOf(map[KEY_ALERT_MODE] ?: "") }
                .getOrDefault(def.alertMode),
            repeatIntervalMinutes = map.int(KEY_REPEAT_INTERVAL, def.repeatIntervalMinutes),
            ttsEnabled = map.bool(KEY_TTS, def.ttsEnabled),
            notificationEnabled = map.bool(KEY_NOTIF, def.notificationEnabled),
            keepScreenOnCharging = map.bool(KEY_KEEP_SCREEN_ON_CHARGING, def.keepScreenOnCharging),
            keepScreenOnBattery = map.bool(KEY_KEEP_SCREEN_ON_BATTERY, def.keepScreenOnBattery),
            requeryDistanceMeters = map.int(KEY_REQUERY_DIST, def.requeryDistanceMeters),
            requeryMinIntervalSec = map.int(KEY_REQUERY_MIN_INTERVAL, def.requeryMinIntervalSec),
        )
    }

    private fun Map<String, String?>.int(key: String, def: Int): Int =
        this[key]?.toIntOrNull() ?: def

    private fun Map<String, String?>.bool(key: String, def: Boolean): Boolean =
        this[key]?.toBooleanStrictOrNull() ?: def

    /** null wenn Key fehlt; sonst nicht-leere, getrimmte Einträge. */
    private fun Map<String, String?>.list(key: String): List<String>? {
        val raw = this[key] ?: return null
        return raw.split(LIST_SEP).map { it.trim() }.filter { it.isNotEmpty() }
    }
}
