package com.poimelder.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.poimelder.app.settings.SettingsRepository
import com.poimelder.app.ui.HomeScreen
import com.poimelder.app.ui.MapScreen
import com.poimelder.app.ui.SettingsScreen
import com.poimelder.app.ui.theme.POIMelderTheme

/**
 * Persistiert einen unbehandelten Crash (siehe Skill android-stability), damit er
 * beim nächsten Start sichtbar gemacht werden kann.
 */
private fun installCrashHandler(context: Context) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        try {
            context.getSharedPreferences("crash_log", Context.MODE_PRIVATE).edit()
                .putString(
                    "last_crash",
                    "${throwable.javaClass.simpleName}: ${throwable.message}\n" +
                        throwable.stackTrace.take(5).joinToString("\n"),
                )
                .apply()
            Thread.sleep(1500)
        } catch (_: Throwable) {
            // niemals im Crash-Handler erneut crashen
        }
        previous?.uncaughtException(thread, throwable)
            ?: run {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashHandler(applicationContext)
        setContent {
            POIMelderTheme {
                KeepScreenOnController()
                AppRoot()
            }
        }
    }
}

/**
 * Steuert das Flag [WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON] abhängig davon,
 * ob das Gerät gerade lädt (Kabel) oder im Akkubetrieb ist – jeweils separat über die
 * Einstellungen ein-/ausschaltbar. Das Flag gilt nur, solange die App im Vordergrund
 * ist; wird die App verlassen, entfernt Android das Flag automatisch mit dem Fenster.
 */
@Composable
private fun KeepScreenOnController() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity ?: return
    val repo = remember { SettingsRepository.get(context) }
    val settings by repo.settings.collectAsState()

    // Ladezustand beobachten: initial aus dem Sticky-Battery-Intent, danach über
    // ACTION_POWER_CONNECTED / ACTION_POWER_DISCONNECTED aktualisiert.
    var charging by remember { mutableStateOf(isCurrentlyCharging(context)) }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_POWER_CONNECTED -> charging = true
                    Intent.ACTION_POWER_DISCONNECTED -> charging = false
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(receiver, filter)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    val keepOn = if (charging) settings.keepScreenOnCharging else settings.keepScreenOnBattery
    DisposableEffect(keepOn) {
        if (keepOn) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

/** Liest den aktuellen Ladezustand aus dem Sticky-ACTION_BATTERY_CHANGED-Intent. */
private fun isCurrentlyCharging(context: Context): Boolean {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        ?: return false
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    return status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
}

private sealed class Dest(val route: String, val label: String, val symbol: String) {    data object Map : Dest("map", "Karte", "🗺")
    data object List : Dest("list", "Liste", "☰")
    data object Settings : Dest("settings", "Einstellungen", "⚙")
}

@Composable
private fun AppRoot() {
    val navController = rememberNavController()
    val items = listOf(Dest.Map, Dest.List, Dest.Settings)
    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            NavigationBar {
                items.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = {
                            if (currentRoute != dest.route) {
                                navController.navigate(dest.route) {
                                    popUpTo(Dest.Map.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Text(dest.symbol) },
                        label = { Text(dest.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Dest.List.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Dest.Map.route) { MapScreen() }
            composable(Dest.List.route) { HomeScreen() }
            composable(Dest.Settings.route) { SettingsScreen() }
        }
    }
}
