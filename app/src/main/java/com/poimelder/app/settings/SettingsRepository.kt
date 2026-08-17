package com.poimelder.app.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistiert [AppSettings] in SharedPreferences und stellt sie als [StateFlow] bereit.
 * App-weites Singleton, damit UI und Service dieselbe Quelle nutzen.
 */
class SettingsRepository private constructor(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load(): AppSettings {
        val keys = SettingsCodec.toMap(AppSettings()).keys
        val map = keys.associateWith { prefs.getString(it, null) }
        return SettingsCodec.fromMap(map)
    }

    /** Aktuellen Stand (frisch aus prefs) – nützlich für Nicht-Compose-Aufrufer. */
    fun current(): AppSettings = _settings.value

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        save(next)
        _settings.value = next
    }

    private fun save(s: AppSettings) {
        val editor = prefs.edit()
        SettingsCodec.toMap(s).forEach { (k, v) -> editor.putString(k, v) }
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "poimelder_settings"

        @Volatile
        private var instance: SettingsRepository? = null

        fun get(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context).also { instance = it }
            }
    }
}
