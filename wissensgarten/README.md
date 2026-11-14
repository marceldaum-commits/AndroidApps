# Wissensgarten 🌱

Lernkarten als gemütliches Garten-Spiel – für die ganze Familie, von Klasse 1 bis zum Erwachsenen.
Jede Karte ist eine Pflanze: Sie wächst mit dem Wissen und welkt nach der Vergessenskurve.
Gießen = Wiederholen zum richtigen Zeitpunkt.

**Neu in 0.8.0**

| Advent im Garten | Brummi beibringen | Kipp-Spiel | Abenteuer-Geschichte | Kreuzungs-Blume | Kreuzungen im Album |
| --- | --- | --- | --- | --- | --- |
| ![](screenshots/27_garten_advent.png) | ![](screenshots/25_brummi_beibringen.png) | ![](screenshots/26_kipp_spiel.png) | ![](screenshots/28_abenteuer_geschichte.png) | ![](screenshots/29_kreuzung.png) | ![](screenshots/30_album_kreuzungen.png) |

| Familienbaum | Merkbild malen | Klassen-Code (QR) | Klassen-Beet | Arbeitsblatt-Foto | Widget (Tag / Party) |
| --- | --- | --- | --- | --- | --- |
| ![](screenshots/31_familienbaum.png) | ![](screenshots/32_merkbild_malen.png) | ![](screenshots/33_klassen_code.png) | ![](screenshots/34_klassen_beet.png) | ![](screenshots/35_arbeitsblatt.png) | ![](screenshots/36_widget_garten.png) |

| Erklär-Geschichte | Abenteuerpfad | 3D-Truhe | Truhe offen | Sammelalbum | Garten mit Freunden |
| --- | --- | --- | --- | --- | --- |
| ![](screenshots/21_erklaerung.png) | ![](screenshots/22_abenteuerpfad.png) | ![](screenshots/23a_truhe_zu.png) | ![](screenshots/23_truhe.png) | ![](screenshots/24_album.png) | ![](screenshots/04f_garten_freunde.png) |

| Entdecken | Merkbild + Gießen | Ballon-Jagd | Buchstabensalat | Stimmt's? | Zuordnen-Bonus | Brummi |
| --- | --- | --- | --- | --- | --- | --- |
| ![](screenshots/06_entdecken.png) | ![](screenshots/06b_merkbild_gegossen.png) | ![](screenshots/16_ballons.png) | ![](screenshots/17_buchstabensalat.png) | ![](screenshots/18_stimmts.png) | ![](screenshots/19_zuordnen.png) | ![](screenshots/20_brummi.png) |

| Duell | Duell-Frage | Duell-Ergebnis |
| --- | --- | --- |
| ![](screenshots/13_duell_start.png) | ![](screenshots/14_duell_frage.png) | ![](screenshots/15_duell_ergebnis.png) |

| Geschmückter Garten | … bei Nacht | Laden: Pflanzen | Laden: Deko |
| --- | --- | --- | --- |
| ![](screenshots/04d_garten_geschmueckt.png) | ![](screenshots/04e_garten_geschmueckt_nacht.png) | ![](screenshots/11_laden_pflanzen.png) | ![](screenshots/12_laden_deko.png) |

| Garten (Tag) | Abend | Nacht | Runde fertig |
| --- | --- | --- | --- |
| ![](screenshots/04_garten.png) | ![](screenshots/04c_garten_abend.png) | ![](screenshots/04b_garten_nacht.png) | ![](screenshots/10_runde_fertig.png) |

| Willkommen | Beete wählen | Anleitung | Beet |
| --- | --- | --- | --- |
| ![](screenshots/01_willkommen.png) | ![](screenshots/02_beete_waehlen.png) | ![](screenshots/03_anleitung.png) | ![](screenshots/05_beet.png) |

| Grundschule | Samenhandlung | Familie |
| --- | --- | --- |
| ![](screenshots/07_giessen_grundschule.png) | ![](screenshots/08_samenhandlung.png) | ![](screenshots/09_familie.png) |

## Funktionen

- **Erlebnis-Lernen statt Karteikarten** – jede Karte hat ein animiertes **Merkbild** (1–3 Emojis als Eselsbrücke),
  neue Karten werden erst **entdeckt** und dann geprüft. Die Runde mischt automatisch **5 Spielarten**:
  🎈 Ballon-Jagd, 🔤 Buchstabensalat, 👆 Stimmt's? (wischen), Auswahl und Aufdecken; alle 6 Karten ein 🧩 Zuordnen-Bonus.
  Auswahl der Spielart: `core/model/Games.kt`, Spiele: `app/ui/play/`.
- **Erklär-Geschichten**: Zu jedem der 43 Pakete erzählt Brummi beim ersten Öffnen eine animierte Geschichte
  (3–5 Szenen mit Bildern, Haken, Eselsbrücke) – jederzeit wieder abspielbar (`app/ui/adventure/IntroScreen.kt`).
- **Abenteuerpfad**: Jedes Beet ist ein Weg mit Stationen à 5 Karten; jede dritte Station hat eine Schatztruhe.
- **Gartenfreunde**: 24 Sammelfiguren (normal, selten, legendär) – nur durch Lernfortschritt, feste Reihenfolge,
  die nächste ist sichtbar (kein Glücksspiel). Die Schatztruhe ist echtes 3D (eigener Mini-Renderer
  `app/ui/adventure/Chest3D.kt`: Perspektive, Licht/Schatten, Deckel klappt am Scharnier auf). Sie spazieren durch den Garten; Album im Laden.
- **Brummi die Hummel** begleitet jede Runde und den Garten: freut sich, tröstet, denkt mit, schläft nachts, gibt Tipps
  (`app/ui/fx/Mascot.kt`).

- **Brummi spricht und hört zu** (0.8): liest Fragen und Antworten vor (Android-Sprachausgabe, Sprache passend zum Fach;
  „Brummi liest vor“: automatisch bis Klasse 4 / an / aus). Neue Spielart 🎤 *Sag's laut* per Spracherkennung.
- **Neue Spielarten**: 🎓 *Brummi beibringen* (Brummi rät, du korrigierst – Protégé-Effekt), 📱 *Kipp-Spiel*
  (Handy neigen, Antwort einfangen). **Gute-Nacht-Runde**: 5 ruhige Karten vor dem Schlafen, ohne Zeitdruck.
- **Jahreszeiten & Feste**: Hügel, Schnee, Blätter und Blüten je nach Jahreszeit; zu Ostern, Halloween, Advent und
  Silvester versteckt der Garten Überraschungen zum Antippen.
- **Brummis Abenteuer**: 10 Kapitel „Brummi und die Wissensblume“, freigeschaltet durch gewachsene Pflanzen.
- **Kreuzungs-Blumen**: Mischrunde aus zwei Fächern (Interleaving); ab 75 % richtig blüht eine eigene Mischblüte
  (z. B. „Zahlennotenveilchen“), gesammelt im Album.
- **Familienbaum**: gemeinsames Wochenziel der Familie statt Wettkampf; ist es geschafft, bekommt jeder +20 💧.
- **Merkbild selbst malen**: nach jeder Karte „✏️ Selbst malen“ – das eigene Bild ersetzt die Emojis (Drawing Effect).
  Bilder bleiben als PNG auf dem Gerät.
- **Klassen-Code**: eigenes Beet als QR-Code am Beamer zeigen oder als Link teilen; die Klasse scannt (Google Code Scanner,
  ohne Kamera-Berechtigung) und hat sofort das Beet. Alles steckt im Code – kein Server, keine Schülerdaten
  (`core/model/ClassCode.kt`, Ausweich-Seite `web/klasse/`).
- **Foto vom Arbeitsblatt → Beet**: Vokabelliste fotografieren, Texterkennung läuft auf dem Gerät (ML Kit), „Wort – Bedeutung“
  und zweispaltige Listen werden zu Karten, passende Emojis kommen aus einem deutschen Emoji-Wörterbuch.
- **Lebendes Widget**: Mini-Garten mit Tageszeit-Himmel (durstige Beete lassen die Köpfe hängen) und – ab 3×2 Feldern –
  eine Quizfrage mit zwei Antwortknöpfen direkt auf dem Startbildschirm.

- **Einrichtung**: Wer lernt (selbst / Kind), Name + Tier-Avatar, Klassenstufe, dann **beliebige Beete** wählen –
  die Stufe hebt nur passende Pakete hervor. Eigene Beete jederzeit möglich.
- **Interaktive Anleitung** mit Zeitraffer: Man sieht, wie Wissen welkt und Gießen hilft.
- **43 Wissenspakete, ~1500 Karten** von Grundschule bis Erwachsene (Mathe, Deutsch, Englisch, Französisch, Latein,
  Spanisch, Bio, Chemie, Physik, Informatik, Geschichte, Erdkunde, Politik, Wirtschaft, Musik, Allgemeinwissen, Erste Hilfe).
- **Auswahl-Aufgaben** (automatisch bewertet) und klassische Karten mit Selbsteinschätzung; Grundschüler bekommen nur
  „Wusste ich / Wusste ich nicht“.
- **Familie**: mehrere Profile auf einem Gerät, Wochen-Überblick für Eltern.
- **Samenhandlung**: alle Pakete nach Stufe und Fach filtern und pflanzen.
- **Widget** für den Startbildschirm, helles Design als Standard (Dunkel optional).
- **Spielgefühl**: Himmel nach Tageszeit (Morgen, Tag, Abend, Sternennacht mit Glühwürmchen), Wolken, Schmetterlinge,
  Pflanzen wiegen sich im Wind. Gießkanne, Wasser-, Funken-, Blätter- und Konfetti-Partikel, „Aufgestiegen!“-Banner,
  Serien-Bonus (🔥 ab 5 richtigen ×2, ab 10 ×3 Tautropfen), Wackeln bei falschen Antworten, federnde Knöpfe.
- **Gartenladen**: Tautropfen gegen 7 Pflanzenarten (Sonnenblume, Tulpe, Kaktus, Rose, Tanne, Kirschbaum,
  Zauberblume – pro Beet wählbar) und 9 Deko-Stücke (Zaun, Zwerg, Windrad, Bank, Vogelhaus, Teich, Laterne,
  Bienenstock, Regenbogen) tauschen. Reiter „Wissen Plus“ als Platz für spätere Bezahl-Pakete.
- **Duell per Code** (ohne Server): Paket wählen, 10 Fragen à 15 s, Punkte für Richtig + Tempo. Der Code geht per
  WhatsApp raus; wer ihn einfügt (oder die Nachricht an die App teilt), spielt dieselben Fragen und hängt sein Ergebnis an.
  Siegerpodest, Konfetti, Tautropfen. Im Code stehen nur Profilname, Tier und Punkte (`core/model/Duel.kt`).
  Der geteilte Link `https://www.mathenachhilfe-online.com/duell/#WG1-…` öffnet direkt die App (Android App Link);
  ohne App zeigt er eine Ausweich-Seite. Server-Dateien und Anleitung: [`web/`](web/README.md).
- **Gieß-Erinnerung**: täglich zur Wunschzeit, nur wenn Pflanzen Durst haben, für alle Profile in einer Nachricht
  (WorkManager, `notify/Reminders.kt`).
- **Ton**: alle Geräusche werden im Code erzeugt (`ui/fx/Synth.kt`) – keine Audiodateien, keine Lizenzen.
  Abschaltbar in den Einstellungen.

## Aufbau (Module)

| Modul | Inhalt |
| --- | --- |
| `core/model` | reine Kotlin-Logik ohne Android: Vergessenskurve (`Memory.kt`), Paketformat (`Pack.kt`), Gieß-Runden (`Session.kt`) |
| `core/data` | Room-Datenbank (Profile, Beete, Karten, Lernprotokoll), Paket-Katalog, Paket-Updates, Freischaltungen, Einstellungen |
| `content` | Wissenspakete als JSON – Format in [`content/PACK_FORMAT.md`](content/PACK_FORMAT.md) |
| `app` | Oberfläche (Compose), Navigation, Widget, Effekte und Sounds (`ui/fx`) |

### Erweitern & Updates

- **Neues Paket**: JSON-Datei in `content/src/main/assets/packs/` – kein Code nötig. `PackValidationTest` prüft das Format.
- **Paket aktualisieren**: Inhalt ändern, `version` erhöhen. Beim nächsten Start bekommen alle, die das Beet gepflanzt
  haben, neue Karten dazu, Texte werden korrigiert – der Lernfortschritt bleibt (über die Karten-`id`).
- **Datenbank ändern**: Version in `GardenDatabase` erhöhen und Migration ergänzen; Schemata liegen in `core/data/schemas`.
  `MigrationTest` baut die alte Version aus dem Schema nach und prüft, dass nichts verloren geht.
- **Neue Laden-Artikel**: in `core/model/ShopItems.kt` eintragen, zeichnen in `app/ui/common/Plant.kt` (Pflanzen)
  bzw. `app/ui/fx/Decor.kt` (Deko).
- **Bezahlversion (später)**: Pakete mit `"tier": "plus"` sind markiert. `Entitlements`/`Monetization.ENFORCED` in
  `core/data` sind die einzige Stelle, an der Google Play Billing angeschlossen werden muss. Heute ist alles frei.

## Signieren

Release-APKs werden mit dem Upload-Schlüssel signiert, wenn `keystore.properties` + `wissensgarten-upload.jks` im
Ordner `wissensgarten/` liegen (beides per `.gitignore` ausgeschlossen, Ablage: Google Drive → Wissensgarten →
„Schlüssel (geheim)“). Ohne die Dateien wird mit dem Test-Schlüssel gebaut.

## Download (automatisch)

Bei jedem Push mit neuer Version (`versionName`) baut GitHub die App, signiert sie, prüft die Signatur und
veröffentlicht `Wissensgarten-<Version>.apk` unter **Releases** – und, wenn eingerichtet, im Drive-Ordner „Wissensgarten“
(`.github/workflows/wissensgarten-release.yml`, Upload: `.github/scripts/drive_upload.py`).
Benötigte Secrets stehen im Kopf der Workflow-Datei.

## Bauen & Testen

Benötigt JDK 17+ und Android SDK (Plattform 36).

```bash
./gradlew assembleRelease     # → app/build/outputs/apk/release/app-release.apk
./gradlew test                # Logik, Datenbank, Paket-Prüfung, Bildschirmfotos (→ screenshots/)
```

## Nächste Ideen

- Echtes Online-Duell mit Konten (Server, Datenschutz-Konzept für Kinder)
- Pakete aus dem Internet nachladen; CSV-Import
- Dungeon-Modus: gewachsene Pflanzen werden Kampfkarten
- Play Store: eigener Upload-Schlüssel, Datenschutzerklärung, Billing für „Plus“
