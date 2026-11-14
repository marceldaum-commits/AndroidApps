# Quanta – Mathe & Physik spielen (Klasse 5–10)

Eine App, drei Welten. Das Rechnen ist die Spielsteuerung selbst – kein Quiz zwischen zwei Spielphasen
(Recherche: `../docs/RECHERCHE.md`, Ideen: `../docs/APP-IDEEN.md`).

| Welt | Inhalt | Status |
|---|---|---|
| 🚀 Kurvenflug | Koordinaten, Geraden, Parabeln, schräger Wurf auf Erde/Mond/Mars | ✅ 27 Level |
| ⚙️ Werkstatt der Maschinen | Hebel, Rollen, Spiegel, Stromkreise | geplant |
| 🗡️ Formel-Dungeon | Terme und Gleichungen als Karten-Abenteuer | geplant |

## So funktioniert Kurvenflug
- Die Sonde fliegt auf der eingestellten Bahn: `(x | y)`, `y = m·x + b`, `y = a·(x − d)² + e` oder Wurf mit `α`, `v`, `g`.
- Regler mit −/+ und Schieber, die Formel zeigt live die Zahlen – in derselben Farbe wie der Regler.
- Nur der Anfang der Bahn ist sichtbar: wer rechnet, trifft beim ersten Versuch (3 ⭐). „Ganze Bahn“ hilft, gibt aber höchstens 2 ⭐.
- Volt gibt nach Fehlversuchen konkrete Rückmeldung mit Zahlen, ab dem zweiten Fehlversuch einen Rechentipp.
- Alle Level werden in Tests mit ihrer Lösung durchgeflogen (lösbar, Lösung liegt auf dem Regler-Raster, Startwerte lösen nicht,
  alle Energiezellen erreichbar). Die Wurfweite wird gegen `v²·sin(2α)/g` geprüft.

## Bildschirme
| | | |
|---|---|---|
| ![](screenshots/01_start.png) | ![](screenshots/02_kurvenflug_kapitel.png) | ![](screenshots/03_volt_erklaert.png) |
| ![](screenshots/04_koordinaten.png) | ![](screenshots/05_gerade_zielen.png) | ![](screenshots/06_gerade_daneben.png) |
| ![](screenshots/07_gerade_flug.png) | ![](screenshots/08_parabel_geschafft.png) | ![](screenshots/09_wurf_berg.png) |
| ![](screenshots/10_wurf_mond.png) | ![](screenshots/11_einstellungen.png) | |

## Aufbau
- `core/model` – reine Kotlin-Logik: Flugbahnen, Wurfphysik, Kollisionen, Level, Rückmeldungen, Update-Prüfung (Unit-Tests)
- `core/data` – Fortschritt (Room, Bestwerte bleiben), Einstellungen
- `app` – Compose-Oberfläche, Volt, im Code erzeugte Geräusche, Updater

## Bauen & Ausliefern
```
export ANDROID_HOME=/opt/android-sdk
./gradlew test            # Tests + Bildschirmfotos (screenshots/)
./gradlew assembleRelease
```
Ausliefern: `versionCode`/`versionName` in `app/build.gradle.kts` erhöhen, Abschnitt in `CHANGELOG.md`, pushen.
GitHub baut, signiert (gleicher Schlüssel wie der Wissensgarten) und veröffentlicht `quanta-v<Version>` unter Releases.
Die App findet die neue Version selbst (Einstellungen → App-Update).
