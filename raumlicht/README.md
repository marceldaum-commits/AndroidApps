# Raumlicht – analytische Geometrie als Lichtshow (Oberstufe, ab Klasse 11)

Du bist Lichtdesigner:in. Laser sind Geraden, Glasscheiben und Leinwände sind Ebenen, die Drohne **Lux** fliegt deine Vektoren.
Die Bühne ist echtes 3D: drehen, zoomen, Punkte und Vektoren direkt im Raum ziehen. Das Rechnen ist die Spielhandlung selbst –
man sieht nur den Anfang eines Strahls, den Rest verrät die Rechnung (Recherche: `../docs/RECHERCHE.md`).

**75 Gigs in 14 Kapiteln**, jedes endet mit einem Abi-Check. Dazu Abi-Simulation, Schritt für Schritt, Jam-Session (endlos erzeugte Aufgaben), Tagesgig,
Lage-Check, Duell, Zu zweit, Klasse, Klausur-Planer, Lernstand, Zweite Chance, Baukasten mit 3D-Rechner, Lux-Werkstatt, Spickzettel und Abzeichen:

| Kapitel | Thema | Spielarten |
|---|---|---|
| 1 Soundcheck | Punkte im Raum | Koordinaten ablesen, Mittelpunkt, Spiegelung an Koordinatenebene, Pyramidenspitze |
| 2 Drohnen-Choreo | Vektoren | Verbindungsvektor, Vektorkette, Vielfaches, Parallelogramm, Schwerpunkt |
| 3 Erster Laser | Geraden | Richtungsvektor, Parameter t, Punktprobe, Spurpunkt |
| 4 Strahlen kreuzen | Lagebeziehungen | Schnittpunkt, parallel, windschief reparieren |
| 5 Glas & Leinwand | Ebenen | Durchstoßpunkt, Koordinatenform bauen, Normalenvektor, Schattenpunkt |
| 6 Headliner | Skalarprodukt · Abstand · Spiegelung | orthogonal, Lotfußpunkt, Spiegelpunkt, Abstand Punkt–Ebene, Reflexion über Bande |
| 7 Im richtigen Winkel | Winkel | Vektor–Vektor, Gerade–Ebene, Ebene–Ebene (15°-Schritte, Winkelbogen in 3D) |
| 8 Auf Abstand | Abstände | Lotfußpunkt auf Gerade, Punkt–Gerade, parallele Ebenen, windschiefe Geraden |
| 9 Discokugel | Kugeln | Radius, Mittelpunkt, Gerade trifft Kugel, Tangentialebene, Schnittkreis, Berührung |
| 10 Zugabe | Kreuzprodukt | Parallelogramm- und Dreiecksfläche, Pyramidenvolumen, Schnittgerade |
| 11 Flugplan | Bewegung mit Zeit | Ort nach t Sekunden, Tempo, Treffpunkt, kleinster Abstand zweier Drohnen |
| 12 Lichtskulpturen | Analysis in 3D | Rotationskörper: Zylinder, Kegel, Vase, Kugel, Trichter (V = π·∫f²) |
| 13 Lichtlotterie | Stochastik | Laplace, Pfadregel mit/ohne Zurücklegen, Erwartungswert, Gegenereignis |
| 14 Achterbahn | Analysis | Nullstelle, Hoch-/Tiefpunkt, Wendestelle, Fläche unter der Kurve (Lux fährt mit, Tangente live) |

**Weitere Spielarten**
- 🔎 **Fehler-Analyse** – typische Fehler (A − B statt B − A, nicht gemittelt, Kosinus statt Sinus, ⅓ vergessen …) werden gezielt benannt (`mistakes` in `Families`).
- 🧩 **Schritt für Schritt** – vier Verfahren in Teilschritten (`Guides`). 📊 **Lernstand** (`Learn`). 🗓️ **Klausur-Planer** (`Planner`, auch im Widget).
- 🏫 **Klasse** – Paket-Code `RLK-…` (`ClassCode`). 👥 **Zu zweit** (`Match`). 🧮 **3D-Rechner** im Baukasten (`Show.facts`).
- 🎢 **Achterbahn fahren** – Fahrersicht auf dem Graphen mit Live-Cockpit für f, f′ und f″ (`Coaster`, `RideScreen`, Szenen-Objekt `track`, `RL.ride`).
- 📐 **Querformat/Tablet** – Bühne links, Regler rechts (`LevelScreen`, ab Breite > 1,15 × Höhe).
- 🎓 **Abi-Simulation** – drei zusammenhängende Aufgaben mit je vier Teilaufgaben und Bewertungseinheiten (`Exams`); freiwillig auf Zeit (45 s je BE).
- 🧭 **Lage-Check** – acht Runden Lagebeziehung zweier Geraden erkennen; die Bühne zeigt die Geraden erst nach der Antwort.
- ⚔️ **Duell** – Code wie `RL-4A1B2X` (Thema + Seed + Prüfziffer) steht für dieselben fünf Aufgaben, ohne Server.
- 📱 **Widget** – Tagesgig auf dem Startbildschirm (`app/widget/DailyWidget`), ein Tipp öffnet die Aufgabe.
- 🔁 **Zweite Chance** – alle Gigs mit 1–2 Sternen noch einmal. 🎨 **Lux-Werkstatt** – Leuchtfarben durch Sterne.
- 🎇 **Show-Baukasten** – Laser (Stütz- und Richtungsvektor), Scheiben (Koordinatenform) und Kugeln frei setzen; Code `RLS-…` zum Teilen.
- 🎧 **Jam-Session** – `Gen` erzeugt aus einem Seed Aufgaben mit ganzzahliger Lösung (Mittelpunkt, Vektor, Zielen, Parameter, Spurpunkt,
  Schnittpunkt, Durchstoßpunkt). Nach Thema oder gemischt; eine Serie zählt Treffer beim ersten Versuch.
- 📅 **Tagesgig** – jeden Tag eine Aufgabe (Themen wechseln reihum), für drei Jahre im Test auf Lösbarkeit geprüft.
- 📓 **Spickzettel** – Formelsammlung. 🏅 **Abzeichen** – 14 Auszeichnungen für Fortschritt.

**Aufbau für Nutzer (seit 0.4.2):** drei Gebiete – Geometrie (Kapitel 1–11), Analysis (12, 14, 15, 17), Stochastik (13, 16). Seit 0.5.0: 89 Aufgaben in 17 Kapiteln,
Flug durch den Raum (`Flights`), Galton-Brett (`Galton`), Ableitungsbahn und Flächenfüllung (`Ride.scene`), Ohne Hilfsmittel (`Quick`), Fehler-Tagebuch (`MistakeBook`, `DiaryEntry`),
Kalender (`Calendar`), Zubehör für Lux (`Looks`, `RL.look`), zweites Widget (`LearnWidget`), Ergebnis-Karte (`ShareCard`). Kapitel heißen nach ihrem Thema;
in älteren Abschnitten dieses READMEs stehen teils noch die früheren Namen (Gig = Aufgabe, Jam-Session = Endlos üben, Backstage = Mehr, Spickzettel = Formelsammlung).

## So funktioniert Raumlicht
- Einstellen: Griff in der Bühne ziehen (Kugel = in der Bodenebene, Pfeile = entlang einer Achse) oder Zahlen mit −/+ tippen.
  Die Formel zeigt die Zahlen live, als Spaltenvektoren wie im Heft.
- „Los“: Lux fliegt bzw. der Laser feuert. Treffer beim ersten Versuch = 3 ⭐, beim zweiten 2 ⭐, danach 1 ⭐. Mit Tipp höchstens 2 ⭐.
- Lux gibt nach Fehlversuchen gezielte Rückmeldung („liegt auf g ✓, aber nicht in E ✗“), nach dem Lösen steht der Rechenweg da.
- Nichts ist gesperrt: Wer für die Klausur ein Thema braucht, springt direkt hin.

## Bildschirme
| | | |
|---|---|---|
| ![](screenshots/01_start.png) | ![](screenshots/02_lux_erklaert.png) | ![](screenshots/03_punkt.png) |
| ![](screenshots/04_daneben.png) | ![](screenshots/05_pyramide_geschafft.png) | ![](screenshots/06_vektor.png) |
| ![](screenshots/07_laser_treffer.png) | ![](screenshots/08_kreuzung.png) | ![](screenshots/09_windschief.png) |
| ![](screenshots/10_durchstosspunkt.png) | ![](screenshots/11_ebene_bauen.png) | ![](screenshots/12_normalenvektor.png) |
| ![](screenshots/13_spiegelpunkt.png) | ![](screenshots/14_ueber_bande.png) | ![](screenshots/16_winkel.png) |
| ![](screenshots/17_abstand_windschief.png) | ![](screenshots/18_discokugel.png) | ![](screenshots/19_kugel_treffer.png) |
| ![](screenshots/20_lichtkuppel.png) | ![](screenshots/21_lichtsegel.png) | ![](screenshots/22_zeltvolumen.png) |
| ![](screenshots/23_jam_session.png) | ![](screenshots/24_jam_aufgabe.png) | ![](screenshots/25_tagesgig.png) |
| ![](screenshots/26_spickzettel.png) | ![](screenshots/27_abzeichen.png) | ![](screenshots/28_bewegung.png) |
| ![](screenshots/29_rotationskoerper.png) | ![](screenshots/30_backstage.png) | ![](screenshots/31_abi_simulation.png) |
| ![](screenshots/32_abi_teilaufgabe.png) | ![](screenshots/33_lage_check.png) | ![](screenshots/34_duell.png) |
| ![](screenshots/35_lux_werkstatt.png) | ![](screenshots/36_show_baukasten.png) | ![](screenshots/37_widget.png) |
| ![](screenshots/38_stochastik.png) | ![](screenshots/39_achterbahn.png) | ![](screenshots/40_leuchtflaeche.png) |
| ![](screenshots/41_schritt_fuer_schritt.png) | ![](screenshots/42_schritt.png) | ![](screenshots/43_lernstand.png) |
| ![](screenshots/44_klausur_planer.png) | ![](screenshots/45_klasse.png) | ![](screenshots/46_zu_zweit.png) |
| ![](screenshots/47_rechner.png) | ![](screenshots/15_einstellungen.png) | |

| ![](screenshots/49_achterbahn_fahrersicht.png) | ![](screenshots/50_achterbahn_aussen.png) | |

| ![](screenshots/52_funktion_bauen.png) | ![](screenshots/53_baumdiagramm.png) | ![](screenshots/54_vierfeldertafel.png) |
| ![](screenshots/55_extremwert.png) | ![](screenshots/56_galton_brett.png) | ![](screenshots/57_raumflug.png) |
| ![](screenshots/58_ableitung_und_flaeche.png) | ![](screenshots/59_fehler_tagebuch.png) | ![](screenshots/60_kalender.png) |
| ![](screenshots/61_ohne_hilfsmittel.png) | ![](screenshots/62_lux_fuehrt_vor.png) | ![](screenshots/63_widget_lernstand.png) |

Seit 0.6.0: eigene Funktionen in „Kurve abfahren“ (`Expr` liest den Term, `Coaster.custom` baut Bahn, Maßstab und besondere Stellen).

| ![](screenshots/64_eigene_funktion.png) | ![](screenshots/65_eigene_funktion_eingabe.png) |
|---|---|

**Bedien-Test der echten Bühne** (seit 2.3.1): `cd scene && node test/interact.mjs ../core/model/build/scenes.json` spielt Antippen, Ziehen und Wischen mit echten
Zeiger-Ereignissen im Browser durch und prüft, was die Bühne meldet. Vor jeder Auslieferung laufen lassen, wenn sich an der Bedienung der Bühne etwas ändert –
die Robolectric-Tests ersetzen die Bühne durch eine Attrappe und sehen solche Fehler nicht.

Querformat: ![](screenshots/48_querformat.png)

## Aufbau
- `core/model` – reine Kotlin-Logik: Vektorrechnung (`Geo`), Spielarten (`Families`), alle Level (`Levels`, `MoreLevels`), Aufgaben-Generator,
  Tagesgig, Abzeichen und Spickzettel (`Gen.kt`), Abi-Simulation, Lage-Check, Duell- und Show-Codes (`Modes.kt`), Bühnen-Beschreibung (`Stage`),
  Musik, Update-Prüfung. Tests spielen jedes Level mit seiner Lösung durch und rechnen die Mathematik unabhängig nach.
- `core/data` – Fortschritt (Room, Sterne sinken nie), Einstellungen
- `app` – Compose-Oberfläche, Lux, im Code erzeugte Geräusche, Updater. Die 3D-Bühne läuft in einem WebView
  (`app/src/main/assets/scene/`, komplett offline) und wird über `StageController` ferngesteuert.
- `scene` – Quelltext der 3D-Bühne (three.js): Spiegelboden, Leuchten (Bloom), Glas-Ebenen, Laser, Partikel, Lux als 3D-Modell.
  Die Bühne zeichnet nur; gerechnet und geprüft wird in `core/model`.

## Bauen & Ausliefern
```
export ANDROID_HOME=/opt/android-sdk
./gradlew test            # Tests + Bildschirmfotos (screenshots/)
./gradlew assembleRelease
```
Nach Änderungen an der 3D-Bühne (`scene/src/main.js`):
```
cd scene && npm install && npm run build     # schreibt app/src/main/assets/scene/scene.js (wird mit eingecheckt)
../gradlew -p .. :core:model:test            # schreibt core/model/build/scenes.json
node test/shot.mjs ../core/model/build/scenes.json ../app/src/test/resources/stage 384 480   # optional: Muster als 6. Angabe, z. B. '^9-'
```
Der letzte Schritt rendert die 3D-Bilder, die in den Bildschirmfoto-Tests an Stelle der Bühne stehen (Robolectric kann kein WebGL).

Ausliefern: `versionCode`/`versionName` und `VERSION_DATE` in `app/build.gradle.kts` setzen, Abschnitt mit Datum in `CHANGELOG.md`, pushen.
GitHub baut, signiert (gleicher Schlüssel wie der Wissensgarten) und veröffentlicht `raumlicht-v<Version>` unter Releases.
Die App findet die neue Version selbst (Einstellungen → App-Update).
