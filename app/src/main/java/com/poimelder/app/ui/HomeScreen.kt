package com.poimelder.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.poimelder.app.history.SeenRepository
import com.poimelder.app.location.LocationProvider
import com.poimelder.app.location.ServiceState
import com.poimelder.app.model.Categories
import com.poimelder.app.model.Poi
import com.poimelder.app.overpass.OverpassClient
import com.poimelder.app.proximity.Announcement
import com.poimelder.app.proximity.NearbyPoi
import com.poimelder.app.proximity.ProximityEngine
import com.poimelder.app.settings.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { OverpassClient() }
    val engine = remember { ProximityEngine() }
    val repo = remember { SettingsRepository.get(context) }

    val running by ServiceState.running.collectAsState()
    val location by ServiceState.location.collectAsState()
    val primary by ServiceState.primary.collectAsState()
    val announcement by ServiceState.announcement.collectAsState()
    val others by ServiceState.othersAtSimilarDistance.collectAsState()
    val nearby by ServiceState.nearby.collectAsState()
    val region by ServiceState.region.collectAsState()
    val status by ServiceState.status.collectAsState()

    var loading by remember { mutableStateOf(false) }
    var infoPoi by remember { mutableStateOf<Poi?>(null) }

    fun refreshOnce() {
        loading = true
        ServiceState.setStatus("Ermittle Standort …")
        scope.launch {
            val loc = ServiceState.location.value ?: LocationProvider.current(context)
            if (loc == null) {
                ServiceState.setStatus("Kein Standort verfügbar. GPS aktiv? Berechtigung erteilt?")
                loading = false
                return@launch
            }
            ServiceState.updateLocation(loc)
            ServiceState.setRegion(LocationProvider.regionLabel(context, loc.latitude, loc.longitude))
            val settings = repo.current()
            val categories = settings.activeCategories()
            if (categories.isEmpty()) {
                ServiceState.setStatus("Keine Kategorie gewählt (siehe Einstellungen).")
                loading = false
                return@launch
            }
            ServiceState.setStatus("Suche POIs im Umkreis von ${settings.radiusMeters / 1000.0} km …")
            runCatching {
                client.fetchPois(loc.latitude, loc.longitude, settings.radiusMeters, categories)
            }.onSuccess { list ->
                engine.updatePois(list)
                val res = engine.onLocationUpdate(loc.latitude, loc.longitude, settings.radiusMeters, null)
                SeenRepository.get(context).markSeen(res.nearby.map { it.poi.id })
                val ann = res.primary?.let { Announcement.build(it, null, res.othersAtSimilarDistance) }
                ServiceState.publishProximity(res.nearby, res.primary, res.othersAtSimilarDistance, ann)
                ServiceState.setStatus("${list.size} POIs gefunden.")
            }.onFailure {
                ServiceState.setStatus("Fehler: ${it.message}")
            }
            loading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PrimaryPoiCard(
                announcement = announcement,
                primary = primary,
                others = others,
                onInfo = { infoPoi = it },
            )

            RegionCard(
                region = region,
                origin = location?.let { it.latitude to it.longitude },
            )

            Button(
                enabled = !loading,
                onClick = { refreshOnce() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        loading -> "Lädt …"
                        running -> "Jetzt aktualisieren"
                        else -> "POIs in der Umgebung laden"
                    },
                )
            }

            status?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            HorizontalDivider()

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                items(nearby) { np -> NearbyPoiRow(np, onInfo = { infoPoi = it }) }
            }
        }

        // Großer runder Start/Stop-Button unten rechts (daumenfreundlich).
        ServiceControlFab(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }

    infoPoi?.let { poi ->
        PoiInfoDialog(poi = poi, onDismiss = { infoPoi = null })
    }
}

@Composable
private fun PrimaryPoiCard(
    announcement: String?,
    primary: NearbyPoi?,
    others: Int,
    onInfo: (Poi) -> Unit,
) {
    if (primary == null) return
    val categoryLabel = primary.poi.kind
        ?: Categories.standardById(primary.poi.categoryId)?.label
        ?: primary.poi.categoryId
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInfo(primary.poi) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Nächster POI",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                )
                if (poiHasInfo(primary.poi)) {
                    InfoBadge(onClick = { onInfo(primary.poi) }, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Text(
                announcement ?: primary.poi.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                categoryLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp),
            )
            primary.poi.detailLine()?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun NearbyPoiRow(np: NearbyPoi, onInfo: (Poi) -> Unit) {
    val categoryLabel = np.poi.kind
        ?: Categories.standardById(np.poi.categoryId)?.label
        ?: np.poi.categoryId
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInfo(np.poi) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(np.poi.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                "$categoryLabel · ${Announcement.formatDistance(np.distanceMeters)} · ${np.compass}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (poiHasInfo(np.poi)) {
            InfoBadge(onClick = { onInfo(np.poi) })
        }
    }
}

@Composable
private fun RegionCard(region: String?, origin: Pair<Double, Double>?) {
    if (region == null && origin == null) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9),
            contentColor = Color(0xFF1B1B1B),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Aktuelle Region",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF1B5E20),
            )
            Text(
                region ?: "Region wird ermittelt …",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1B1B1B),
            )
            origin?.let { (la, lo) ->
                Text(
                    "%.4f, %.4f".format(la, lo),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF424242),
                )
            }
        }
    }
}
