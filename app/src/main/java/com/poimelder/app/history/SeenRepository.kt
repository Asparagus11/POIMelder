package com.poimelder.app.history

import android.content.Context
import java.time.LocalDate

/** Sichtungsstatus eines POI für die Marker-Färbung. */
enum class SeenStatus { UNSEEN, TODAY, PREVIOUS }

/**
 * Merkt sich, an welchem Tag ein POI zum ersten Mal eingeblendet wurde, und leitet
 * daraus den Sichtungsstatus ab:
 *  - heute zuerst gesehen  -> TODAY (grün)
 *  - an einem früheren Tag -> PREVIOUS (braun, dauerhaft)
 *  - noch nie gesehen      -> UNSEEN (Standardfarbe)
 */
class SeenRepository private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Trägt neue POIs mit dem heutigen Tag ein (bestehende bleiben unverändert). */
    fun markSeen(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val today = LocalDate.now().toEpochDay()
        val editor = prefs.edit()
        var changed = false
        for (id in ids) {
            if (!prefs.contains(id)) {
                editor.putLong(id, today)
                changed = true
            }
        }
        if (changed) editor.apply()
    }

    fun statusFor(id: String, today: Long = LocalDate.now().toEpochDay()): SeenStatus {
        val day = if (prefs.contains(id)) prefs.getLong(id, today) else null
        return status(day, today)
    }

    companion object {
        private const val PREFS_NAME = "poimelder_seen"

        @Volatile
        private var instance: SeenRepository? = null

        fun get(context: Context): SeenRepository =
            instance ?: synchronized(this) {
                instance ?: SeenRepository(context).also { instance = it }
            }

        /** Reine, testbare Statuslogik. */
        fun status(firstSeenDay: Long?, today: Long): SeenStatus = when {
            firstSeenDay == null -> SeenStatus.UNSEEN
            firstSeenDay >= today -> SeenStatus.TODAY
            else -> SeenStatus.PREVIOUS
        }
    }
}
