package com.poimelder.app.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

/**
 * Kapselt die Android-System-[TextToSpeech]-API. F-Droid-sicher: keine nativen
 * Blobs im App-Paket. Die Thorsten-Stimme wird genutzt, wenn eine Engine sie
 * bereitstellt – bevorzugt die freie Offline-Engine „SherpaTTS" (Piper), sonst die
 * Standard-Engine des Systems.
 *
 * Erstellung/Freigabe müssen auf dem Main-Thread erfolgen (siehe android-stability).
 */
class TtsManager(context: Context, private val onReady: (() -> Unit)? = null) {

    private var tts: TextToSpeech? = null
    @Volatile private var ready = false
    @Volatile private var usedVoice: String? = null
    @Volatile private var attemptedEngine: String? = null

    private val onInit = TextToSpeech.OnInitListener { status ->
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts
            if (engine != null) {
                runCatching { engine.language = Locale.GERMAN }
                selectPreferredVoice(engine)
                ready = true
            }
        }
        onReady?.invoke()
    }

    init {
        val appContext = context.applicationContext
        // Wenn die SherpaTTS-Engine installiert ist, diese gezielt verwenden (Thorsten/Piper).
        val enginePkg = if (isInstalled(appContext, SHERPA_ENGINE)) SHERPA_ENGINE else null
        attemptedEngine = enginePkg
        tts = runCatching {
            if (enginePkg != null) {
                TextToSpeech(appContext, onInit, enginePkg)
            } else {
                TextToSpeech(appContext, onInit)
            }
        }.getOrNull()
    }

    private fun selectPreferredVoice(engine: TextToSpeech) {
        val voices: Set<Voice> = runCatching { engine.voices }.getOrNull() ?: emptySet()
        // Thorsten nur wählen, wenn wirklich vorhanden – sonst die vom Nutzer in der
        // Engine eingestellte/installierte Stimme (z.B. de-css10) unverändert nutzen.
        val thorsten = voices.firstOrNull {
            it.name?.contains("thorsten", ignoreCase = true) == true
        }
        if (thorsten != null) {
            runCatching { engine.voice = thorsten }
            usedVoice = thorsten.name
        } else {
            // Keine Stimme erzwingen: die aktuelle Default-Stimme der Engine übernehmen.
            usedVoice = runCatching { engine.voice?.name }.getOrNull()
                ?: voices.firstOrNull { it.locale?.language == Locale.GERMAN.language }?.name
        }
    }

    /** Spricht den Text (verwirft laufende Ausgabe). No-op, wenn nicht bereit. */
    fun speak(text: String) {
        val engine = tts ?: return
        if (!ready || text.isBlank()) return
        runCatching {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "poimelder-${System.currentTimeMillis()}")
        }
    }

    fun stop() {
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
    }

    /** Name der aktuell gewählten Stimme (Diagnose/Anzeige), oder null. */
    fun currentVoiceName(): String? = usedVoice

    /** Aktuell genutzte Engine (angefragtes Paket, sonst System-Default). */
    fun engineName(): String? = attemptedEngine ?: runCatching { tts?.defaultEngine }.getOrNull()

    /** true, wenn gezielt die SherpaTTS-Engine angefragt wurde. */
    fun usesSherpa(): Boolean = attemptedEngine == SHERPA_ENGINE

    fun isReady(): Boolean = ready

    companion object {
        const val SHERPA_ENGINE = "org.woheller69.ttsengine"

        private fun isInstalled(context: Context, pkg: String): Boolean =
            runCatching {
                context.packageManager.getPackageInfo(pkg, 0)
                true
            }.getOrDefault(false)
    }
}
