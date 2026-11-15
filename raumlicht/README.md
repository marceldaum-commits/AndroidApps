# Raumlicht – analytische Geometrie als Lichtshow (Oberstufe, ab Klasse 11)

Du bist Lichtdesigner:in. Laser sind Geraden, Glasscheiben und Leinwände sind Ebenen, die Drohne **Lux** fliegt deine Vektoren.
Die Bühne ist echtes 3D: drehen, zoomen, Punkte und Vektoren direkt im Raum ziehen. Das Rechnen ist die Spielhandlung selbst –
man sieht nur den Anfang eines Strahls, den Rest verrät die Rechnung (Recherche: `../docs/RECHERCHE.md`).

**55 Gigs in 10 Kapiteln**, jedes endet mit einem Abi-Check. Dazu Jam-Session (endlos erzeugte Aufgaben), Tagesgig, Spickzettel und Abzeichen:

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

**Weitere Spielarten**
- 🎧 **Jam-Session** – `Gen` erzeugt aus einem Seed Aufgaben mit ganzzahliger Lösung (Mittelpunkt, Vektor, Zielen, Parameter, Spurpunkt,
  Schnittpunkt, Durchstoßpunkt). Nach Thema oder gemischt; eine Serie zählt Treffer beim ersten Versuch.
- 📅 **Tagesgig** – jeden Tag eine Aufgabe (Themen wechseln reihum), für drei Jahre im Test auf Lösbarkeit geprüft.
- 📓 **Spickzettel** – Formelsammlung. 🏅 **Abzeichen** – 14 Auszeichnungen für Fortschritt.

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
| ![](screenshots/26_spickzettel.png) | ![](screenshots/27_abzeichen.png) | ![](screenshots/15_einstellungen.png) |

## Aufbau
- `core/model` – reine Kotlin-Logik: Vektorrechnung (`Geo`), Spielarten (`Families`), alle Level (`Levels`, `MoreLevels`), Aufgaben-Generator,
  Tagesgig, Abzeichen und Spickzettel (`Gen.kt`), Bühnen-Beschreibung (`Stage`),
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

Ausliefern: `versionCode`/`versionName` in `app/build.gradle.kts` erhöhen, Abschnitt in `CHANGELOG.md`, pushen.
GitHub baut, signiert (gleicher Schlüssel wie der Wissensgarten) und veröffentlicht `raumlicht-v<Version>` unter Releases.
Die App findet die neue Version selbst (Einstellungen → App-Update).
