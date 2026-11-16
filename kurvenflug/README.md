# Kurvenflug – Mathe & Physik im All (Klasse 5–10)

Du steuerst eine Raumsonde – die Flugbahn ist eine Formel, die du selbst einstellst. Das Rechnen ist die Spielsteuerung
selbst, kein Quiz zwischen zwei Spielphasen (Recherche: `../docs/RECHERCHE.md`, weitere Ideen: `../docs/APP-IDEEN.md`).

Inhalte (107 Level in 18 Kapiteln, 7 Boss-Kämpfe, Tagesmission, Auffrischen mit Lernstand, Duell, Asteroidensturm, Abzeichen,
Baukasten mit Level-Codes, Klassen-Challenge, Kurve ziehen, Formel-Detektiv, Planeten-Lexikon, Werkstatt): Koordinaten, Brüche & Kommazahlen am Zahlenstrahl, Winkel mit Geodreieck (Kl. 5–6), Zuordnungen/Prozent (Kl. 6–7), Geraden (Kl. 7–8), Pythagoras (Kl. 8–9), Parabeln (Kl. 9), Logarithmus (Kl. 10),
Weg-Zeit und Laser-Optik (Physik 7), Energieerhaltung (Physik 8–9),
Rendezvous/Gleichungssysteme (Kl. 8), schräger Wurf auf Erde/Mond/Mars (Physik), Sinus & Exponentialfunktion (Kl. 10), Gravitation/Swing-by (Physik Kl. 10), Trigonometrie sin/cos/tan (Kl. 9–10), Nullstellen und quadratische Ergänzung (Kl. 9), Lichtbrechung nach Snellius mit Totalreflexion (Physik 9).

## Aufbau der Startseite
- Vier Reiter unten: 🚀 **Heute** („Weiter, wo du warst“, Tagesmission, Auffrischen), 🗺️ **Galaxie** (Kapitel nach Klasse,
  eingeklappt mit Fortschrittsring, Filter Mathe/Physik), 🎮 **Spiele** (Duell, Sturm, Detektiv, Baukasten), 🎒 **Ich**
  (Lernstand, Abzeichen, Lexikon, Werkstatt, Klasse, Einstellungen).
- Startbildschirm-Widget (Jetpack Glance): Tagesmission, „Weiter“ und fälliges Auffrischen, jeder Knopf springt direkt ins
  Level (`kurvenflug://level/<id>`). Klein (2×2) nur Sterne und „Weiter“. Aktualisiert sich nach jedem Level und alle 30 Minuten.
- Story „Die Reise der Aurora“: Volts Mutterschiff ist zerbrochen, auf jedem Planeten (Kapitel) treibt ein Bauteil. Ein ganz
  geschafftes Kapitel bringt es zurück (Feier + Hangar, in dem das Schiff aus 18 Teilen zusammenwächst). Die Galaxie ist eine
  Sternenkarte mit Pfad, Volts Rakete schwebt beim aktuellen Planeten; Umschalten auf Liste möglich.
- Volt lebt: plappert in Roboter-Sprache (im Code erzeugt, abschaltbar), schläft abends, freut sich nach einer Pause,
  tanzt bei 3 ⭐. Überraschungs-Sticker (24, drei Seltenheiten, 10 % Glitzer) nach geschafften Leveln; Fotomodus teilt die
  Flugbahn als Bild (nur das Bild, keine Daten).
- Mehrere Spieler auf einem Gerät (bis 6, Spitzname + Bild), je eigener Spielstand; Eltern-Ansicht mit Wochenübersicht
  (Tage, Minuten, Themen, wo Hilfe gut täte) aus dem Durchgangs-Protokoll; Klassen-Challenge mit Ergebnis-Codes (KFR1) und
  Bestenliste mit Prüfcode – alles ohne Server. Datenbank v2 (Profil-Spalte, Tabelle `plays`) mit Migration und Migrationstest.
- Staffel 2: Gold-Teile (Kapitel komplett mit 3 ⭐, Goldglanz + Warp-Antrieb) und 8 Rettungsmissionen mit je 5 gemischten
  Etappen (gemischtes Üben); gerettete Freunde bilden die Crew und sprechen täglich wechselnd auf der Startseite.
- Andromeda (nach fertiger Aurora): endlose, aus der Planetennummer erzeugte Galaxie, je Planet 3 schwere Level;
  die ersten 18 Planeten bauen das zweite Schiff „Nova“.
- Raumstation: 6 Räume mit Sternenstaub (= alle gesammelten Sterne) freischalten, mit Stickern dekorieren, Crew wohnt dort.
  Gast-Planeten zu Halloween, Advent, Ostern (Gaußsche Osterformel) und Sommerferien mit Themen-Sticker – jedes Jahr wieder.
- Beim ersten Start fragt Volt nach der Klasse (5–10 oder „alle“). Sie sortiert nur: „Deine Klasse“, „Zum Wiederholen“,
  „Für später“ – gesperrt wird nichts.

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
| ![](screenshots/42_rechenweg.png) | ![](screenshots/43_galaxie.png) | ![](screenshots/44_spiele.png) |
| ![](screenshots/46_ich.png) | ![](screenshots/45_klasse_waehlen.png) | ![](screenshots/11_einstellungen.png) |
| ![](screenshots/47_widget.png) | ![](screenshots/48_widget_klein.png) | ![](screenshots/49_brueche.png) |
| ![](screenshots/50_winkel.png) | ![](screenshots/51_lichtbrechung.png) | ![](screenshots/52_nullstellen.png) |
| ![](screenshots/53_sternenkarte.png) | ![](screenshots/54_hangar.png) | ![](screenshots/55_bauteil_gefunden.png) |
| ![](screenshots/56_geschichte.png) | ![](screenshots/57_sticker.png) | ![](screenshots/58_volt_launen.png) |
| ![](screenshots/59_sticker_gewonnen.png) | ![](screenshots/60_foto.png) | ![](screenshots/61_spieler.png) |
| ![](screenshots/62_eltern.png) | ![](screenshots/63_bestenliste.png) | ![](screenshots/64_hangar_gold.png) |
| ![](screenshots/65_crew.png) | ![](screenshots/66_notruf.png) | ![](screenshots/67_start_crew.png) |
| ![](screenshots/68_andromeda.png) | ![](screenshots/69_raumstation.png) | ![](screenshots/70_gastplanet.png) |
| ![](screenshots/71_start_gastplanet.png) | | |

## Aufbau
Eine Codebasis für Android **und** Browser (Kotlin Multiplatform + Compose Multiplatform):
- `core/model` – reine Kotlin-Logik für JVM und Kotlin/Wasm: Flugbahnen, Wurfphysik, Gravitation (Leapfrog), Kollisionen, Level,
  Tagesmission-Generator, Musik-Komposition, Rückmeldungen, Einstellungen, Codes (Unit-Tests; Tagesmission für 3 Jahre auf
  Lösbarkeit geprüft). `ParityTest` läuft auf JVM **und** Wasm und vergleicht einen Fingerabdruck aller Level, Bahnen und Codes.
- `core/data` – Android-Speicher: Fortschritt (Room, Bestwerte bleiben), Einstellungen (SharedPreferences)
- `shared` – alle Bildschirme (Compose), Volt, im Code erzeugte Geräusche; je Plattform: Teilen, Ton, Vibration, Zurück, Updater
- `app` – Android-Hülle: Activity, Widget, App-Update
- `web` – WebApp (Kotlin/Wasm): Startseite, `kf.js` (Web Audio, Teilen, Zurück), Manifest, Service Worker (offline),
  zugeschnittene Schriften für Emojis und Mathe-Zeichen (`web/fonts/make_fonts.py`, Noto Color Emoji / DejaVu, freie Lizenzen)

## Verstehen statt nur Schieben
Live-Rechnung beim Schieben, Rechen-Stern nach dem Treffer, danach der Lösungsweg; die Aufgabe bleibt nach Fehlversuchen sichtbar.

| Live-Rechnung | Rechen-Stern | Richtig + Lösungsweg | Aufgabe bleibt |
|---|---|---|---|
| ![Live](screenshots/72_live_rechnung.png) | ![Frage](screenshots/73_rechenstern_frage.png) | ![Richtig](screenshots/74_rechenstern_geschafft.png) | ![Aufgabe](screenshots/76_aufgabe_nach_fehlversuch.png) |

## Im Browser (WebApp)
Gleiche Version, gleiche Bildschirme. Spielstand im lokalen Speicher des Browsers, keine Server-Daten, offline spielbar,
installierbar. Am PC als Handy-Spalte in der Mitte. Test: https://kurvenflug-test.mathenachhilfe-online.com

| Handy | Level | PC |
|---|---|---|
| ![Web Start](screenshots/web-01-start.png) | ![Web Level](screenshots/web-02-level.png) | ![Web PC](screenshots/web-03-pc.png) |

## Bauen & Ausliefern
```
export ANDROID_HOME=/opt/android-sdk
./gradlew :core:model:allTests :core:data:testDebugUnitTest :app:testDebugUnitTest   # Logik (JVM + Wasm), Daten, Bildschirmfotos
./gradlew assembleRelease
./gradlew :web:webapp     # fertige WebApp nach webapp/ (index.html oben, sw.js, version.json)
```
Ausliefern: `versionCode`/`versionName` in `app/build.gradle.kts` erhöhen (gilt auch für die WebApp), Abschnitt in `CHANGELOG.md`, pushen.
WebApp: `:web:webapp` bauen, im Browser prüfen und auf den Zweig `kurvenflug-test` legen (Server holt alle 5 Minuten),
Meldung in `MELDUNG_AN_LEITSTAND.md`. Den Zweig `kurvenflug-live` setzt nur der Leitstand.
GitHub baut, signiert (gleicher Schlüssel wie der Wissensgarten) und veröffentlicht `kurvenflug-v<Version>` unter Releases.
Die App findet die neue Version selbst (Einstellungen → App-Update).
