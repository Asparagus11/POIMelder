package com.poimelder.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.poimelder.app.model.Poi

/** Ein aufbereiteter Info-Eintrag; [url] gesetzt = klickbar (Browser/Telefon). */
data class InfoItem(val label: String, val value: String, val url: String?)

/** Baut die anzeigbaren Zusatzinfos aus den OSM-Extra-Tags eines POI. */
fun buildInfoItems(poi: Poi): List<InfoItem> {
    val e = poi.extras
    val items = mutableListOf<InfoItem>()
    e["description"]?.let { items += InfoItem("Beschreibung", it, null) }
    e["opening_hours"]?.let { items += InfoItem("Öffnungszeiten", it, null) }
    e["cuisine"]?.let { items += InfoItem("Küche", prettyValue(it), null) }
    e["religion"]?.let { items += InfoItem("Religion", prettyValue(it), null) }
    e["denomination"]?.let { items += InfoItem("Konfession", prettyValue(it), null) }
    e["operator"]?.let { items += InfoItem("Betreiber", it, null) }
    e["ele"]?.let { items += InfoItem("Höhe", "$it m", null) }
    e["website"]?.let { items += InfoItem("Website", it, normalizeUrl(it)) }
    e["phone"]?.let { items += InfoItem("Telefon", it, "tel:" + it.replace(" ", "")) }
    e["wikipedia"]?.let { items += InfoItem("Wikipedia", it, wikipediaUrl(it)) }
    e["wikidata"]?.let { items += InfoItem("Wikidata", it, "https://www.wikidata.org/wiki/$it") }
    return items
}

fun poiHasInfo(poi: Poi): Boolean = buildInfoItems(poi).isNotEmpty()

private fun prettyValue(raw: String): String =
    raw.split(";").joinToString(", ") { it.trim().replace('_', ' ') }

private fun normalizeUrl(raw: String): String =
    if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"

private fun wikipediaUrl(raw: String): String {
    // Format oft "de:Artikelname"
    val parts = raw.split(":", limit = 2)
    return if (parts.size == 2) {
        val lang = parts[0]
        val article = Uri.encode(parts[1].replace(' ', '_'))
        "https://$lang.wikipedia.org/wiki/$article"
    } else {
        "https://de.wikipedia.org/wiki/${Uri.encode(raw.replace(' ', '_'))}"
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

/** Kleines, klickbares Info-Symbol. */
@Composable
fun InfoBadge(onClick: () -> Unit, tint: Color = Color.Unspecified) {
    Text(
        text = "ⓘ",
        style = MaterialTheme.typography.titleLarge,
        color = tint,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(start = 8.dp),
    )
}

/** Overlay mit allen Zusatzinfos zu einem POI. */
@Composable
fun PoiInfoDialog(poi: Poi, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val items = remember(poi.id) { buildInfoItems(poi) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schließen") } },
        title = { Text(poi.name) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                poi.kind?.let {
                    Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                if (items.isEmpty()) {
                    Text("Keine weiteren Infos in OpenStreetMap hinterlegt.")
                }
                items.forEach { item ->
                    Column {
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (item.url != null) {
                            Text(
                                item.value,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable { openUrl(context, item.url) },
                            )
                        } else {
                            Text(item.value, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
    )
}
