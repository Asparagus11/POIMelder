# POIMelder

Android-App für Radfahrer und Autofahrer. Verfolgt im Hintergrund den Standort per GPS und meldet per Notification und Sprachansage (TTS), sobald ein Point of Interest einer gewählten Kategorie in den konfigurierbaren Umkreis kommt.

## Download

📥 Die aktuelle APK findest du unter [Releases](../../releases).

## Screenshots

| | |
|---|---|
| ![Screenshot 1](screenshots/screenshot1.png) | ![Screenshot 2](screenshots/screenshot2.png) |

## Features

- POI-Daten via OpenStreetMap Overpass-API
- Foreground-Service für Hintergrund-Tracking
- Konfigurierbare Kategorien (Tankstelle, Restaurant, Supermarkt etc.) + eigene OSM-Tags
- TTS-Sprachansage bei Annäherung
- Kartenansicht (osmdroid/OpenStreetMap)
- Enter/Exit-Logik pro POI
- Display-Always-On (getrennt für Kabel/Akku einstellbar)

## Datenaustausch

Diese App nutzt aktuell keinen Cloud-Sync. Sie ist Teil einer App-Familie, deren andere Mitglieder Nextcloud (WebDAV) für den Datenaustausch nutzen. Zukünftig könnten auch andere Services wie Syncthing oder generisches WebDAV zum Einsatz kommen.

## Tech-Stack

- Kotlin
- Jetpack Compose
- Material3
- osmdroid
- Android LocationManager
- Overpass-API
- OkHttp

## Entwicklung

Diese App wurde größtenteils mit [Kiro CLI](https://kiro.dev) entwickelt.
Kiro ist vermutlich der einfachste Weg, die App weiterzuentwickeln.

### Build

```bash
./gradlew assembleDebug
```

## Unterstützung

☕ [Buy Me a Coffee](https://buymeacoffee.com/asparagus11)

## Lizenz

MIT License – siehe [LICENSE](LICENSE)

## Kontakt

Wenn du die App weiterentwickeln oder forken möchtest, freue ich mich über eine kurze Info an thomas.ad.meyer@gmail.com
