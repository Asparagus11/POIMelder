package com.poimelder.app.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.poimelder.app.model.Categories
import com.poimelder.app.settings.AlertMode
import com.poimelder.app.settings.SettingsRepository
import com.poimelder.app.tts.TtsManager

private fun appVersionName(context: Context): String =
    runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    }.getOrDefault("?")

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repo = remember { SettingsRepository.get(context) }
    val settings by repo.settings.collectAsState()
    val version = remember { appVersionName(context) }
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Einstellungen", style = MaterialTheme.typography.headlineMedium)

        var showHelp by remember { mutableStateOf(false) }
        Button(onClick = { showHelp = true }) { Text("Hilfe & Bedienung") }
        if (showHelp) HelpDialog(onDismiss = { showHelp = false })

        CrashCard(context)

        // --- Radius ---
        Text("Umkreis: ${settings.radiusMeters / 1000.0} km", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = settings.radiusMeters.toFloat(),
            onValueChange = { v ->
                repo.update { it.copy(radiusMeters = (v / 500).toInt() * 500) }
            },
            valueRange = 500f..20000f,
        )

        Divider()

        // --- Kategorien ---
        Text("Kategorien", style = MaterialTheme.typography.titleMedium)
        Categories.STANDARD.forEach { cat ->
            val checked = settings.selectedCategoryIds.contains(cat.id)
            SwitchRow(cat.label, checked) { on ->
                repo.update {
                    val next = it.selectedCategoryIds.toMutableSet()
                    if (on) next.add(cat.id) else next.remove(cat.id)
                    it.copy(selectedCategoryIds = next)
                }
            }
        }

        // --- Custom-Tags ---
        Text("Eigene OSM-Tags (key=value)", style = MaterialTheme.typography.titleMedium)
        var newTag by remember { mutableStateOf("") }
        var tagError by remember { mutableStateOf<String?>(null) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newTag,
                onValueChange = { newTag = it; tagError = null },
                label = { Text("z.B. amenity=pharmacy") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    val parsed = Categories.customCategory(newTag)
                    if (parsed == null) {
                        tagError = "Ungültig – Format key=value"
                    } else {
                        repo.update {
                            if (it.customTags.contains(newTag.trim())) it
                            else it.copy(customTags = it.customTags + newTag.trim())
                        }
                        newTag = ""
                    }
                },
                modifier = Modifier.padding(start = 8.dp),
            ) { Text("+") }
        }
        tagError?.let { Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall) }
        settings.customTags.forEach { tag ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tag)
                TextButton(onClick = {
                    repo.update { it.copy(customTags = it.customTags - tag) }
                }) { Text("Entfernen") }
            }
        }

        Divider()

        // --- Meldemodus ---
        Text("Meldemodus", style = MaterialTheme.typography.titleMedium)
        RadioRow("Nur bei Annäherung (Enter/Exit)", settings.alertMode == AlertMode.ENTER_EXIT) {
            repo.update { it.copy(alertMode = AlertMode.ENTER_EXIT) }
        }
        RadioRow("Periodisch wiederholen", settings.alertMode == AlertMode.PERIODIC) {
            repo.update { it.copy(alertMode = AlertMode.PERIODIC) }
        }
        if (settings.alertMode == AlertMode.PERIODIC) {
            IntField(
                label = "Wiederholung alle (Minuten)",
                value = settings.repeatIntervalMinutes,
            ) { v -> repo.update { it.copy(repeatIntervalMinutes = v.coerceAtLeast(1)) } }
        }

        Divider()

        // --- Ausgabe ---
        SwitchRow("Sprachansage (TTS)", settings.ttsEnabled) { on ->
            repo.update { it.copy(ttsEnabled = on) }
        }
        Text(
            "Sprachausgabe nutzt automatisch die in SherpaTTS (F-Droid) eingestellte " +
                "deutsche Stimme (z.B. de-css10 oder Thorsten). Ist die Thorsten-Stimme " +
                "installiert, wird sie bevorzugt. Ohne SherpaTTS spricht die System-Standardstimme.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TtsTestRow()
        SwitchRow("Benachrichtigung", settings.notificationEnabled) { on ->
            repo.update { it.copy(notificationEnabled = on) }
        }

        Divider()

        // --- Bildschirm / Always-On ---
        Text("Bildschirm anlassen (Always-On)", style = MaterialTheme.typography.titleMedium)
        Text(
            "Verhindert, dass sich der Bildschirm ausschaltet, solange POIMelder im " +
                "Vordergrund ist. Getrennt einstellbar für Kabel- und Akkubetrieb.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SwitchRow("Am Kabel (Laden)", settings.keepScreenOnCharging) { on ->
            repo.update { it.copy(keepScreenOnCharging = on) }
        }
        SwitchRow("Im Akkubetrieb", settings.keepScreenOnBattery) { on ->
            repo.update { it.copy(keepScreenOnBattery = on) }
        }

        Divider()

        // --- Requery ---
        Text("Neuabfrage (Overpass)", style = MaterialTheme.typography.titleMedium)
        Text(
            "Effektive Bewegungsschwelle: ${settings.effectiveRequeryDistanceMeters()} m",
            style = MaterialTheme.typography.bodySmall,
        )
        IntField(
            label = "Bewegungsschwelle in m (0 = halber Radius)",
            value = settings.requeryDistanceMeters,
        ) { v -> repo.update { it.copy(requeryDistanceMeters = v.coerceAtLeast(0)) } }
        IntField(
            label = "Mindest-Zeitabstand (Sekunden)",
            value = settings.requeryMinIntervalSec,
        ) { v -> repo.update { it.copy(requeryMinIntervalSec = v.coerceAtLeast(0)) } }

        Divider()

        Text("POIMelder Version $version", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun IntField(label: String, value: Int, onValue: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { new ->
            text = new.filter { it.isDigit() }
            text.toIntOrNull()?.let(onValue)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CrashCard(context: Context) {
    var lastCrash by remember {
        mutableStateOf(
            context.getSharedPreferences("crash_log", Context.MODE_PRIVATE)
                .getString("last_crash", null),
        )
    }
    val crash = lastCrash ?: return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Letzter Absturz", style = MaterialTheme.typography.titleMedium)
            Text(crash, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = {
                context.getSharedPreferences("crash_log", Context.MODE_PRIVATE)
                    .edit().remove("last_crash").apply()
                lastCrash = null
            }) { Text("Löschen") }
        }
    }
}

@Composable
private fun TtsTestRow() {
    val context = LocalContext.current
    var info by remember { mutableStateOf<String?>(null) }
    val tts = remember { TtsManager(context) }
    DisposableEffect(Unit) {
        onDispose { tts.shutdown() }
    }
    Button(onClick = {
        tts.speak(
            "POIMelder Sprachtest. In drei Kilometern rechts voraus befindet sich " +
                "die Wallfahrtskirche.",
        )
        val engine = tts.engineName() ?: "unbekannt"
        val voice = tts.currentVoiceName() ?: "Standard"
        val sherpa = if (tts.usesSherpa()) "SherpaTTS aktiv" else "SherpaTTS NICHT genutzt"
        info = "$sherpa\nEngine: $engine\nStimme: $voice"
    }) {
        Text("TTS testen")
    }
    info?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
