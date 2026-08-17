package com.poimelder.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.poimelder.app.location.LocationService
import com.poimelder.app.location.ServiceState

private fun hasFineOrCoarse(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

private fun hasBackground(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

/**
 * Großer runder Start/Stop-Button (daumenfreundlich). Grün = starten, Rot = läuft/stoppen.
 * Enthält den kompletten Runtime-Permission-Flow (Standort + Hintergrund + Benachrichtigung).
 */
@Composable
fun ServiceControlFab(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val running by ServiceState.running.collectAsState()
    var showBgRationale by remember { mutableStateOf(false) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    fun startService() = LocationService.start(context)

    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            toast("Ohne Hintergrund-Ortung meldet die App evtl. nur bei offener App zuverlässig.")
        }
        startService()
    }

    fun requestBackgroundThenStart() {
        if (!hasBackground(context)) showBgRationale = true else startService()
    }

    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) requestBackgroundThenStart() else toast("Standortberechtigung wird benötigt.")
    }

    fun onStartClicked() {
        if (!hasFineOrCoarse(context)) {
            val perms = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            foregroundLauncher.launch(perms.toTypedArray())
        } else {
            requestBackgroundThenStart()
        }
    }

    FloatingActionButton(
        onClick = { if (running) LocationService.stop(context) else onStartClicked() },
        modifier = modifier.size(72.dp),
        shape = CircleShape,
        containerColor = if (running) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
    ) {
        Text(
            text = if (running) "\u25A0" else "\u25B6", // ■ / ▶
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )
    }

    if (showBgRationale) {
        AlertDialog(
            onDismissRequest = { showBgRationale = false },
            title = { Text("Hintergrund-Ortung erlauben?") },
            text = {
                Text(
                    "POIMelder verfolgt deinen Standort auch bei ausgeschaltetem Display, " +
                        "um dich während der Fahrt an POIs zu erinnern. Wähle dazu im nächsten " +
                        "Dialog \"Immer zulassen\".",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBgRationale = false
                    backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }) { Text("Weiter") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBgRationale = false
                    startService()
                }) { Text("Später") }
            },
        )
    }
}
