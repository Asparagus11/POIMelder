package com.poimelder.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Kurze In-App-Hilfe: Zweck und Bedienung der App (Pflicht laut Konventionen). */
@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schließen") } },
        title = { Text("Hilfe & Bedienung") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Section(
                    "Was macht POIMelder?",
                    "Die App meldet dir während der Fahrt (Rad/Auto) Points of Interest aus " +
                        "OpenStreetMap, sobald sie in deinen gewählten Umkreis kommen – per " +
                        "Benachrichtigung, optional Sprachansage, plus Liste und Karte.",
                )
                Section(
                    "Loslegen",
                    "1. In den Einstellungen Kategorien wählen (z.B. Sehenswürdigkeit, " +
                        "Kirche/Wallfahrt) und den Umkreis einstellen.\n" +
                        "2. Im Tab „Liste\" den Melder mit „Starten\" aktivieren und die " +
                        "Standortberechtigung erteilen (für den Hintergrundbetrieb „Immer zulassen\").",
                )
                Section(
                    "Liste",
                    "Zeigt die nahen POIs nach Entfernung sortiert mit Typ, Distanz und " +
                        "Himmelsrichtung. Oben wird der nächste POI groß hervorgehoben. Ein Tipp " +
                        "auf ein POI-Feld öffnet Details; das Symbol ⓘ erscheint, wenn Zusatzinfos " +
                        "(z.B. Öffnungszeiten, Website, Wikipedia) vorliegen. Links öffnen im Browser.",
                )
                Section(
                    "Karte",
                    "Zeigt deinen Standort, den Umkreis und die POIs als Marker. Farben: " +
                        "grün = heute zuerst gesehen, braun = an einem früheren Tag gesehen, " +
                        "hellblau = neu.",
                )
                Section(
                    "Automatische Aktualisierung",
                    "Während der Fahrt lädt die App neue POIs, sobald du dich weiter als die " +
                        "Bewegungsschwelle bewegt hast und der Mindest-Zeitabstand vorbei ist " +
                        "(beides in den Einstellungen). Mehr Vorwarnzeit erreichst du über einen " +
                        "größeren Umkreis.",
                )
                Section(
                    "Sprachansage (TTS)",
                    "Bei aktivem Schalter wird der nächste POI angesagt. Für die angenehme " +
                        "Offline-Stimme „Thorsten\" die App SherpaTTS (F-Droid) installieren und " +
                        "die Stimme dort laden. Mit „TTS testen\" prüfst du, welche Engine/Stimme " +
                        "genutzt wird.",
                )
                Section(
                    "Benachrichtigung",
                    "Der Schalter „Benachrichtigung\" steuert, ob in der dauerhaften " +
                        "Melder-Benachrichtigung die POI-Ansage angezeigt wird. Die Benachrichtigung " +
                        "selbst ist bei laufendem Hintergrunddienst von Android vorgeschrieben.",
                )
                Section(
                    "Bildschirm anlassen (Always-On)",
                    "In den Einstellungen kannst du getrennt für Kabel- und Akkubetrieb " +
                        "festlegen, ob der Bildschirm an bleibt, solange POIMelder im " +
                        "Vordergrund ist. Standard: am Kabel an, im Akkubetrieb aus (schont den " +
                        "Akku). Wechselt das Gerät zwischen Laden und Akku, passt sich der " +
                        "Bildschirm automatisch an.",
                )
                Section(
                    "Daten & Datenschutz",
                    "POIs kommen live über die Overpass-API, Kartenkacheln von OpenStreetMap " +
                        "(nur beim Öffnen der Karte, lokal zwischengespeichert). Es werden keine " +
                        "personenbezogenen Daten an Dritte gesendet.",
                )
            }
        },
    )
}

@Composable
private fun Section(title: String, body: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(body, style = MaterialTheme.typography.bodySmall)
    }
}
