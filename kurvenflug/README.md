# Kurvenflug – Mathe & Physik im All (Klasse 5–10)

Du steuerst eine Raumsonde – die Flugbahn ist eine Formel, die du selbst einstellst. Das Rechnen ist die Spielsteuerung
selbst, kein Quiz zwischen zwei Spielphasen (Recherche: `../docs/RECHERCHE.md`, weitere Ideen: `../docs/APP-IDEEN.md`).

Inhalte (82 Level in 14 Kapiteln, 7 Boss-Kämpfe, Tagesmission, Auffrischen mit Lernstand, Duell, Asteroidensturm, Abzeichen,
Baukasten mit Level-Codes, Klassen-Challenge, Kurve ziehen, Formel-Detektiv, Planeten-Lexikon, Werkstatt): Koordinaten (Kl. 5–6), Zuordnungen/Prozent (Kl. 6–7), Geraden (Kl. 7–8), Pythagoras (Kl. 8–9), Parabeln (Kl. 9), Logarithmus (Kl. 10),
Weg-Zeit und Laser-Optik (Physik 7), Energieerhaltung (Physik 8–9),
Rendezvous/Gleichungssysteme (Kl. 8), schräger Wurf auf Erde/Mond/Mars (Physik), Sinus & Exponentialfunktion (Kl. 10), Gravitation/Swing-by (Physik Kl. 10), Trigonometrie sin/cos/tan (Kl. 9–10).

## So funktioniert Kurvenflug
- Die Sonde fliegt auf der eingestellten Bahn: `(x | y)`, `y = m·x + b`, `y = a·(x − d)² + e` oder Wurf mit `α`, `v`, `g`.
- Regler mit −/+ und Schieber, die Formel zeigt live die Zahlen – in derselben Farbe wie der Regler.
- Nur der Anfang der Bahn ist sichtbar: wer rechnet, trifft beim ersten Versuch (3 ⭐). „Ganze Bahn“ hilft, gibt aber höchstens 2 ⭐.
- Volt gibt nach Fehlversuchen konkrete Rückmeldung mit Zahlen, ab dem zweiten Fehlversuch einen Rechentipp.
- Treffer zählen bei Funktionskurven nur, wenn y an der Stelle der Station stimmt; nach einem Fehlversuch zeigt ein
  gelber Pfeil den Abstand („2 zu tief“). Ein Test stellt sicher, dass nur wenige Reglerstellungen treffen (Raten reicht nicht).
- Ab dem 3. Fehlversuch: Rechenweg mit Volt – jedes Zwischenergebnis antippen (42 Level, jeder Rechenweg wird gegen
  die Lösung geprüft). Beim ersten Mal je Bahnart rechnet Volt den ersten Schritt vor, danach das Kind allein.
- Lernstand: je Thema „Neu“, „Übt noch“, „Sicher“; geschaffte Themen kommen nach 2 bzw. 7 Tagen als „Auffrischen“ zurück.
- Alle Level werden in Tests mit ihrer Lösung durchgeflogen (lösbar, Lösung liegt auf dem Regler-Raster, Startwerte lösen nicht,
  alle Energiezellen erreichbar). Die Wurfweite wird gegen `v²·sin(2α)/g` geprüft.

## Bildschirme
| | | |
|---|---|---|
| ![](screenshots/01_start.png) | ![](screenshots/03_volt_erklaert.png) | ![](screenshots/04_koordinaten.png) |
| ![](screenshots/05_gerade_zielen.png) | ![](screenshots/06_gerade_daneben.png) | ![](screenshots/07_gerade_flug.png) |
| ![](screenshots/08_parabel_geschafft.png) | ![](screenshots/09_wurf_berg.png) | ![](screenshots/10_wurf_mond.png) |
| ![](screenshots/12_sinus.png) | ![](screenshots/13_zerfall.png) | ![](screenshots/14_schwerkraft_schleuder.png) |
| ![](screenshots/15_schleuder_ganze_bahn.png) | ![](screenshots/16_tagesmission.png) | ![](screenshots/17_rendezvous.png) |
| ![](screenshots/18_rendezvous_daneben.png) | ![](screenshots/19_boss.png) | ![](screenshots/20_asteroidensturm.png) |
| ![](screenshots/21_duell.png) | ![](screenshots/22_duell_zu_zweit.png) | ![](screenshots/23_abzeichen.png) |
| ![](screenshots/24_kurve_ziehen.png) | ![](screenshots/25_baukasten.png) | ![](screenshots/26_baukasten_start.png) |
| ![](screenshots/27_klassen_challenge.png) | ![](screenshots/28_formel_detektiv.png) | ![](screenshots/29_antiproportional.png) |
| ![](screenshots/30_pythagoras.png) | ![](screenshots/31_laser.png) | ![](screenshots/32_energie_rampe.png) |
| ![](screenshots/33_logarithmus.png) | ![](screenshots/34_weg_zeit.png) | ![](screenshots/35_lexikon.png) |
| ![](screenshots/36_werkstatt.png) | ![](screenshots/37_laser_zielen.png) | ![](screenshots/38_abstand_pfeil.png) |
| ![](screenshots/39_trig_laser.png) | ![](screenshots/40_trig_funk.png) | ![](screenshots/41_lernstand.png) |
| ![](screenshots/42_rechenweg.png) | ![](screenshots/11_einstellungen.png) | |

## Aufbau
- `core/model` – reine Kotlin-Logik: Flugbahnen, Wurfphysik, Gravitation (Leapfrog), Kollisionen, Level, Tagesmission-Generator,
  Musik-Komposition, Rückmeldungen, Update-Prüfung (Unit-Tests; Tagesmission für 3 Jahre auf Lösbarkeit geprüft)
- `core/data` – Fortschritt (Room, Bestwerte bleiben), Einstellungen
- `app` – Compose-Oberfläche, Volt, im Code erzeugte Geräusche, Updater

## Bauen & Ausliefern
```
export ANDROID_HOME=/opt/android-sdk
./gradlew test            # Tests + Bildschirmfotos (screenshots/)
./gradlew assembleRelease
```
Ausliefern: `versionCode`/`versionName` in `app/build.gradle.kts` erhöhen, Abschnitt in `CHANGELOG.md`, pushen.
GitHub baut, signiert (gleicher Schlüssel wie der Wissensgarten) und veröffentlicht `kurvenflug-v<Version>` unter Releases.
Die App findet die neue Version selbst (Einstellungen → App-Update).
