package com.poimelder.app.location

import android.location.Location
import com.poimelder.app.proximity.NearbyPoi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-weiter, prozessglobaler Zustand des Melde-Service. Der Foreground-Service
 * schreibt hier hinein, die UI beobachtet die Flows.
 */
object ServiceState {

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    /** Alle POIs im Radius, aufsteigend nach Entfernung. */
    private val _nearby = MutableStateFlow<List<NearbyPoi>>(emptyList())
    val nearby: StateFlow<List<NearbyPoi>> = _nearby.asStateFlow()

    /** Hervorgehobener POI (nächster/„in Fahrtrichtung"). */
    private val _primary = MutableStateFlow<NearbyPoi?>(null)
    val primary: StateFlow<NearbyPoi?> = _primary.asStateFlow()

    /** Anzahl weiterer POIs in ähnlicher Entfernung wie [primary]. */
    private val _othersAtSimilarDistance = MutableStateFlow(0)
    val othersAtSimilarDistance: StateFlow<Int> = _othersAtSimilarDistance.asStateFlow()

    /** Fertiger Ansagetext für den primären POI. */
    private val _announcement = MutableStateFlow<String?>(null)
    val announcement: StateFlow<String?> = _announcement.asStateFlow()

    /** Aktuelle Regionsbezeichnung (Reverse-Geocoding). */
    private val _region = MutableStateFlow<String?>(null)
    val region: StateFlow<String?> = _region.asStateFlow()

    /** Kurzer Statustext (z.B. „12 POIs geladen"). */
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    fun updateLocation(location: Location) {
        _location.value = location
    }

    fun setRunning(running: Boolean) {
        _running.value = running
        if (!running) {
            _nearby.value = emptyList()
            _primary.value = null
            _othersAtSimilarDistance.value = 0
            _announcement.value = null
        }
    }

    fun publishProximity(
        nearby: List<NearbyPoi>,
        primary: NearbyPoi?,
        othersAtSimilarDistance: Int,
        announcement: String?,
    ) {
        _nearby.value = nearby
        _primary.value = primary
        _othersAtSimilarDistance.value = othersAtSimilarDistance
        _announcement.value = announcement
    }

    fun setRegion(region: String?) {
        _region.value = region
    }

    fun setStatus(status: String?) {
        _status.value = status
    }
}
