# Wissensgarten 🌱

Lernkarten als gemütliches Garten-Spiel – für die ganze Familie, von Klasse 1 bis zum Erwachsenen.
Jede Karte ist eine Pflanze: Sie wächst mit dem Wissen und welkt nach der Vergessenskurve.
Gießen = Wiederholen zum richtigen Zeitpunkt.

| Duell | Duell-Frage | Duell-Ergebnis |
| --- | --- | --- |
| ![](screenshots/13_duell_start.png) | ![](screenshots/14_duell_frage.png) | ![](screenshots/15_duell_ergebnis.png) |

| Geschmückter Garten | … bei Nacht | Laden: Pflanzen | Laden: Deko |
| --- | --- | --- | --- |
| ![](screenshots/04d_garten_geschmueckt.png) | ![](screenshots/04e_garten_geschmueckt_nacht.png) | ![](screenshots/11_laden_pflanzen.png) | ![](screenshots/12_laden_deko.png) |

| Garten (Tag) | Abend | Nacht | Aufgestiegen! | Runde fertig |
| --- | --- | --- | --- | --- |
| ![](screenshots/04_garten.png) | ![](screenshots/04c_garten_abend.png) | ![](screenshots/04b_garten_nacht.png) | ![](screenshots/06b_aufgestiegen.png) | ![](screenshots/10_runde_fertig.png) |

| Willkommen | Beete wählen | Anleitung | Beet | Gießen (Auswahl) |
| --- | --- | --- | --- | --- |
| ![](screenshots/01_willkommen.png) | ![](screenshots/02_beete_waehlen.png) | ![](screenshots/03_anleitung.png) | ![](screenshots/05_beet.png) | ![](screenshots/06_giessen_auswahl.png) |

| Grundschule | Samenhandlung | Familie |
| --- | --- | --- |
| ![](screenshots/07_giessen_grundschule.png) | ![](screenshots/08_samenhandlung.png) | ![](screenshots/09_familie.png) |

## Funktionen

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

## Bauen & Testen

Benötigt JDK 17+ und Android SDK (Plattform 36).

```bash
./gradlew assembleRelease     # → app/build/outputs/apk/release/app-release.apk
./gradlew test                # Logik, Datenbank, Paket-Prüfung, Bildschirmfotos (→ screenshots/)
```

## Nächste Ideen

- Echtes Online-Duell mit Konten (Server, Datenschutz-Konzept für Kinder)
- Pakete aus dem Internet nachladen; Karten-Sets teilen (Link/QR), CSV-Import
- Dungeon-Modus: gewachsene Pflanzen werden Kampfkarten
- Play Store: eigener Upload-Schlüssel, Datenschutzerklärung, Billing für „Plus“
