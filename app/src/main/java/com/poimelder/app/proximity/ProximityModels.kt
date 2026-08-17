package com.poimelder.app.proximity

import com.poimelder.app.model.Poi

/** Ein POI mit berechneter Entfernung/Richtung relativ zum aktuellen Standort. */
data class NearbyPoi(
    val poi: Poi,
    val distanceMeters: Double,
    val bearingDeg: Double,
    val compass: String,
)

/** Ergebnis eines Standort-Updates gegen die aktuelle POI-Liste. */
data class ProximityResult(
    /** Alle POIs im Radius, aufsteigend nach Entfernung. */
    val nearby: List<NearbyPoi>,
    /** POIs, die seit dem letzten Update neu in den Radius eingetreten sind. */
    val entered: List<Poi>,
    /** IDs von POIs, die seit dem letzten Update den Radius verlassen haben. */
    val exited: List<String>,
    /** Der hervorzuhebende POI (nächster, bei Gleichstand am ehesten in Fahrtrichtung). */
    val primary: NearbyPoi?,
    /** Anzahl weiterer POIs in ähnlicher Entfernung wie [primary]. */
    val othersAtSimilarDistance: Int,
)
