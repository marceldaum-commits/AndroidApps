# Meldungen an den Leitstand – Kurvenflug WebApp

## 2.5.1 – Offline in Bestqualität (Nachtrag Marcel) · Web = APK = Play
- Zwei Knöpfe: **„Offline laden – Standard (ca. X MB)“** (X aus "bytes") und **„Offline laden – Bestqualität (ca. Z MB)“**
  (Z aus "bytes_original"); jeweils anteilig um schon Gespeichertes verringert. Die gemerkte Wahl ist der gefüllte Knopf.
- Bestqualität lädt `…/nova/original/<h[0:2]>/<h>.mp3`; bei 404 die normale Datei (gespeichert als Bestqualität, damit sie
  nicht erneut versucht wird). Getrennte Ablage (Android `files/nova` und `files/nova-best`, Web Cache-Schlüssel mit/ohne
  `original/`), Abspielen: erst Bestqualität, dann Standard, dann laden. Normales Vorlesen bleibt bei der Standard-Datei.
  „Offline-Stimme löschen“ löscht beide.
- Browser-Test gegen den Server: Stand 154 551 057 / 188 464 128 Bytes gelesen; Bestqualität geladen – `original/` lieferte
  noch 404, Rückfall auf normale Datei griff, 32 gespeichert und als Bestqualität gezählt; Löschen ok.
- versionCode 48. Bildschirmfoto `screenshots/99_vorlesen.png`.

## 2.5.0 – „Alle Nova-Texte offline laden“ (Vorgabe Marcel) · Web = APK = Play
- Einstellungen → Vorlesen → Kasten „📦 Offline-Stimme“: Knopf **„Alle Nova-Texte offline laden (ca. X MB)“**, Hinweis
  „Am besten im WLAN laden“, während des Ladens „Lade … x von n“ mit Balken und **Abbrechen**, danach bzw. sobald etwas
  gespeichert ist **„Offline-Stimme löschen (Y MB)“**; komplett: „✓ Alle 1 163 Texte sind gespeichert“.
  Bildschirmfoto `screenshots/99_vorlesen.png`.
- **X** live aus `stand-<host>.json` → "bytes", minus was schon gespeichert ist (Web: Host der Seite, auf localhost der Live-Host;
  Android: kurvenflug.mathenachhilfe-online.com). Wird bei jedem Öffnen der Einstellungen neu gelesen.
- **Liste:** eigene `vorlesen/texte.json` (Web: von der Seite; Android: dieselbe Datei als Asset in der APK), Dateinamen über
  `Nova.hash`. Nur fehlende Dateien, **4 parallel**, 404 still übersprungen, Laufzeit-Texte (/jetzt) nicht dabei.
  Nach Abbruch geht es beim nächsten Tippen mit dem Rest weiter.
- **Speicher:** Android `files/nova` (App-Speicher, nicht Cache, keine Obergrenze; schon zwischengespeicherte Ansagen werden
  übernommen statt neu geladen), Web Cache Storage „kf-nova“. Abspielen schaut zuerst dort nach.
- **Browser-Test (gegen den echten Server):** Größe 188 464 128 Bytes gelesen; nach 4 s abgebrochen → 47 gespeichert (6 MB);
  zweiter Lauf startete mit „0/1116“ (nur Fehlende); Löschen → 0. Android gebaut, nicht auf Gerät getestet.
- versionCode 47, keine neue Dateiendung.

## 2.4.2 – Auftrag „Play-Store-fertig“ (Routine 17.11. 15:00) geprüft
Hinweis: Der Auftrag kam nach deiner Nachricht „Play erst später, keine Zeit in Play-Material“ – ich habe deshalb nur
geprüft und Lücken geschlossen, nichts neu aufgebaut. Stand:
| Punkt | Stand |
|---|---|
| Kein Deinstallieren GitHub-APK ↔ Play | ✓ Play App Signing mit **eigenem** Schlüssel (WG_*, 9ece4812…): Anleitung Abschnitt 2 (PEPK über den Workflow „Kurvenflug: Signaturschlüssel für Google Play“, Alternative `java -jar pepk.jar`). **Kein separater Upload-Schlüssel nötig** – derselbe Schlüssel signiert die Uploads; der Release-Workflow prüft den Fingerabdruck des AAB. |
| Play-Variante ohne eigenen Download/Installations-Recht | ✓ `SELF_UPDATE=false`, REQUEST_INSTALL_PACKAGES nicht im Play-Manifest |
| Play In-App Updates | ✓ Flexible, bei Priorität 4–5 Immediate (`app/src/play/.../StoreUpdates.kt`) |
| Kein Nachladen von nativem Code | ✓ seit 2.4.0 gar kein nativer Code mehr (ONNX Runtime entfernt) |
| Signiertes AAB über GitHub-Workflow | ✓ `kurvenflug-v<Version>`, Fingerabdruck-Prüfung im Workflow |
| Store-Eintrag de | ✓ Titel, Kurz-/Langbeschreibung (+ Fassung mit Pro), Icon 512, Feature-Grafik 1024×500, 8 Screenshots (`store/`) |
| Datensicherheit / Familien / Altersfreigabe | ✓ Abschnitt 4 (Zielgruppe 9–17, Familienprogramm, Nova-Begründung) |
| Lizenzprüfung | **aktualisiert** (`store/LIZENZEN.md` war Stand 0.21): Play Billing + mitgebrachte Google Data Transport, Firebase Encoders, javax.inject (Apache 2.0) ergänzt, auch im Lizenzhinweis der App → deshalb 2.4.2 |
| Klick-Liste interner Test → geschlossener Test (12 Tester, 14 Tage) | ✓ Abschnitte 5–6, 6b auf 2.4.2 gesetzt |
| GitHub-APK mit eigenem Updater | ✓ unverändert |

## 2.4.1 live – Play-Fassung bereit
- Release `kurvenflug-v2.4.1` vollständig: APK 2,6 MB, **Play-AAB 6,5 MB** (versionCode 45), Web-ZIP 5,6 MB.
- Klick-Liste `store/PLAY_CONSOLE_ANLEITUNG.md` 6b auf 2.4.1 gesetzt (inkl. KI-Kennzeichnung, Datensicherheit „Nein“ mit
  Prüfhinweis, Datenschutz-Absatz Nova), Versionshinweise `store/de-DE/release-notes.txt` (Nova + Sprechtempo).
- Stichprobe: fester Text „Volltreffer beim ersten Versuch! …“ liegt jetzt als MP3 unter /nova/ (200, audio/mpeg).

## 2.4.1 – Sprechtempo (Ergänzung Marcel) · Web = APK = Play
- Einstellungen → Vorlesen: Regler „Sprechtempo“ mit den Stufen 0,75 · 0,9 · 1,0 (Standard) · 1,15 · 1,3 · 1,5, gemerkt
  (`Nova.SPEEDS`, Test). 1,0 = Server-Tempo.
- Android: MediaPlayer `PlaybackParams(speed, pitch 1.0)`. Web: Wiedergabe jetzt über HTMLAudioElement mit
  `playbackRate` und `preservesPitch = true` (statt Web-Audio-Puffer). Notlösung Gerätestimme: Sprechrate × Tempo.
- 2.4.0 war schon ausgeliefert, daher eigener kleiner Schritt 2.4.1 (versionCode 45).

## 2.4.0 – Nur noch Nova (Vorgabe Marcel, von ihm im Chat bestätigt) · Web = APK = Play
- **Stimmenwahl:** „Nova – KI-generierte Stimme“ (Kennzeichnung in den Einstellungen) oder Vorlesen aus. Kein Tempo-Regler
  (Server: 1,25). Probe-Taste bleibt. Bildschirmfoto `screenshots/99_vorlesen.png`.
- **Abspielweg (gemeinsame Regeln in `core/model/Nova.kt`):** vorlesefertiger Text (`Speech.clean`) → erst
  `/nova/<h[0:2]>/<h>.mp3` (Zwischenspeicher: Android Cache ≤ 40 MB, Web Cache „kf-nova“) → fehlt sie: `POST /jetzt`
  (nur ≤ 300 Zeichen und nur ohne Spielernamen – Namen der Spielerprofile werden geprüft, ganze Wörter) → 200 {"url"}
  laden, nur Adressen unter /nova/ → 204/offline/Fehler: still die Gerätestimme (nur lokal rechnende deutsche Stimmen).
  Nichts an OpenAI, keine Kennungen, keine Konten.
- **Getestet im Browser (http://localhost, CORS ok):** Volts Begrüßung → /nova/ 404 (Werk hatte noch nichts vertont) → /jetzt →
  URL mit identischem Hash → MP3 gespielt (4,8 s) und zwischengespeichert; keine Gerätestimme nötig. /jetzt-Prüfwert:
  „Schon 24 Sterne gesammelt. Weiter so, Commander!“ → acddd8626f25b43663f8efcd3fe60a1f (Test). Android-Weg nur gebaut,
  nicht auf Gerät gehört.
- **Thorsten entfernt:** ONNX Runtime (Android + Web, `ort/`), Lautschrift (`voice/`), Werkzeug `tools/stimme`, Download,
  Tests. Reste werden beim Start gelöscht (Android `files/stimme`, Web Cache „kf-stimme“). Keine ABI-Beschränkung mehr.
- **Datenschutz-Text** in der App wörtlich nach deiner Vorgabe (Einstellungen → Vorlesen); Store-Beschreibungen,
  Datenschutz-Vorschlag und Play-Anleitung angepasst (Datensicherheit: „Nein“, Begründung + Prüfhinweis; KI-Kennzeichnung
  als Schritt). Play-Manifest: INTERNET jetzt für Nova.
- versionCode 44. Keine neue Dateiendung (eine weniger: `.mjs`/`.tsv` entfallen).

## 2.3.1 – Nova Schritt 1: vorlesen/texte.json auf test · Web = Android
- **URL:** https://kurvenflug-test.mathenachhilfe-online.com/vorlesen/texte.json (nach Live auch ohne -test).
  Format genau nach Vorgabe: `{"format": 1, "app": "kurvenflug", "texte": [{"text": "…", "sprache": "de"}, …]}`.
- **Inhalt:** 1 163 Texte, rund 152 000 Zeichen, längster 697 Zeichen (Grenze 1 500 wird beim Bauen geprüft, ebenso NFC).
  Feste Texte: Aufträge und Tipps aller festen Level (Kapitel, Extrem-Prüfungen, Bosse, Andromeda, Rettungen),
  Rechen-Stern (Frage + Antworten bzw. richtige Antwort + Lösungsweg – genau wie die 🔊-Knöpfe), Theorie (je Punkt,
  Merkformel, „alles vorlesen“), Kapitel-Einführungen, Geschichte, Crew-Sprüche, Probe-Satz und Volts feste Sätze aus der
  Oberfläche (`speech = "…"`). **Nicht drin:** Texte mit Zahlen/Namen zur Laufzeit (Tagesmission, Generator, „Schon 24 ⭐“,
  Spielernamen) – dort weicht die App später leise auf Thorsten/Gerätestimme aus.
- **Gesprochene Form:** Formeln stehen so drin, wie sie gesprochen werden („y gleich 2x plus 1“, „3 Meter pro Sekunde“).
- **Beim Bauen erzeugt:** Gradle-Aufgabe `:core:model:vorlesenTexte` (läuft mit `:web:webapp`), gemeinsame Quelle
  `ReadTexts.kt` – App und Liste haben denselben Wortlaut. Nicht im Offline-Paket der WebApp.
- **Dateiname:** `Nova.hash` mit eigenem SHA-256 (Android = Web), Prüfwert „Hallo Welt“ → cb1e6d67882767042ebaa54d163e450c
  im Test bestätigt. Abspielweg baue ich nach deiner Rückmeldung (Schritt 3).
- Keine neue Dateiendung (json).

## Store-Text für Pro in Play vorbereitet (Auftrag Leitstand)
- `store/de-DE/beschreibung-mit-pro.txt` (3 173 Zeichen): ohne „keine Käufe“, neuer Abschnitt „⭐ Kurvenflug Pro (optional)“ –
  Schulstoff bis Klasse 10 frei; Pro (einmaliger In-App-Kauf, kein Abo): Oberstufe (6 Kapitel, 35 Level), Extrem-Prüfungen,
  Asteroidensturm Profi & Extrem, Andromeda ab Planet 7, bis 6 Spieler (frei: 2), Pro-Werkstatt; Bestandsschutz; Kauf nur
  nach Frage für Erwachsene. Die aktuelle `beschreibung.txt` bleibt unverändert.
- **Test `StoreTextTest`** prüft, dass die Liste genau zu `core/model/Pro.kt` passt (jeder Vorteil, Spieler-/Planeten-/Kapitel-/
  Levelzahlen) und „keine Käufe“ nicht vorkommt – ändert sich Pro, fällt der Test.
- `store/PLAY_CONSOLE_ANLEITUNG.md` 6a: neuer Schritt 0 „Vor dem Anlegen des In-App-Produkts: Beschreibung tauschen“,
  Schritt 6 als **Pflicht**: IARC „Digitale Käufe/In-App-Käufe: Ja“ und Zielgruppe-Frage „In-App-Käufe: Ja“.

## 2.3.0 – „Pro aktiv“ sichtbar (Vorgabe Marcel) · Web = Android
- Gab es vorher nur als Text „Freigeschaltet“ auf der Pro-Karte in den Einstellungen – jetzt eingebaut:
  - **Startseite, Kopf unter „KURVENFLUG“:** goldenes Abzeichen „⭐ Pro aktiv“ (sanft pulsierend), immer sichtbar,
    Bildschirmfoto `screenshots/101_pro_aktiv_start.png`.
  - **Einstellungen, Karte „Kurvenflug Pro“ (oben):** dasselbe Abzeichen plus Art und ggf. Ablauf: „Einmalkauf über Google
    Play – unbegrenzt“, „Einmalkauf über die Website (Code) – unbegrenzt“ oder „Schullizenz – gültig bis 31.7.2027“,
    Bildschirmfoto `screenshots/102_pro_aktiv_einstellungen.png`. Die Pro-Seite selbst zeigte Art/Ablauf schon.
- Ohne Pro: unverändert (kein Abzeichen). Gleicher Code für Web, APK und Play. versionCode 42.

## 2.2.0 live – Nachprüfung
- Live geprüft: version.json 2.2.0, stimme/tokens.txt, ort/*.mjs, ort/*.wasm, ort/LIZENZ.txt kommen an (richtige Typen).
- Release `kurvenflug-v2.2.0` vollständig: APK 15,2 MB, Play-AAB 19,2 MB, Web-ZIP 8,5 MB.
- Play-Fassung vorbereitet: Klick-Liste `store/PLAY_CONSOLE_ANLEITUNG.md` Abschnitt 6b, Versionshinweise in
  `store/de-DE/release-notes.txt`. Hochladen macht Marcel.
- Regel übernommen (README): neue Dateiendungen vorher melden. Derzeit genutzt: html, js, mjs, wasm, json, webmanifest,
  txt, ttf, png, svg, tsv.

## 2.2.0 – Thorsten auch in Play (Entscheidung Marcel) · Web = Android
- Play-Fassung hat jetzt die INTERNET-Berechtigung, ausschließlich für den Thorsten-Download auf Wunsch (versionCode 41).
  Damit ist die Play-Ausnahme weg: Vorlesen inkl. Thorsten ist in GitHub-APK, Play und Web gleich.
- Store-Unterlagen angepasst (Beschreibung, Datenschutz-Text, Play-Console-Anleitung). **Offen für Marcel/Leitstand:**
  Datensicherheits-Formular – **geklärt**: Zugriffsprotokoll für /thorsten/ ist aus (Leitstand), keine IP/Kennung gespeichert →
  bleibt „keine Daten erhoben“. Datenschutz-Text (store/PLAY_CONSOLE_ANLEITUNG.md) nennt den Download vom eigenen Server
  (HTTPS, IP nur für die Verbindung, nicht gespeichert).

## 2.1.0 – Thorsten vom gemeinsamen Server · Web = Android
- Quelle jetzt `stimmen.mathenachhilfe-online.com/thorsten/de_DE-thorsten-medium.onnx`, Größe 63 201 294 Bytes und
  SHA-256 7e64762d… werden in App und WebApp geprüft (eine Stelle im Code: `core/model/VoicePack.kt`). Hinweis „ca. 75 MB –
  am besten im WLAN“, Elternhinweis jetzt „eigener Server, keine fremden Anbieter“. Laden nur auf Wunsch, danach offline.
- **Technik geändert:** Die Server-Datei ist das originale Piper-Modell ohne die Zusatzangaben, die sherpa-onnx verlangt
  (sherpa bricht ohne sie ab). Deshalb rechnet Android jetzt mit **ONNX Runtime Android** – derselbe Weg wie die WebApp
  (Laut-Nummern wie Piper, Werte aus der .onnx.json: noise 0,667 / noise_w 0,8, 22 050 Hz). Laut-Liste mit der
  `phoneme_id_map` der Server-.json verglichen: identisch (152 Laute). sherpa-onnx und JitPack entfallen.
- Alte Stimmdatei (von huggingface.co, 0.27.0/2.0.0) wird automatisch entfernt; wer sie hatte, lädt einmal neu.
- Kein espeak-ng (GPL): Lautschrift weiterhin vorab erzeugt und mitgeliefert. versionCode 40.

## 2.0.0 – neues Versionsschema (Vorgabe Marcel) · Web = Android
- versionName **2.0.0** (APK, AAB, WebApp, `version.json`, Anzeige in App/WebApp), versionCode läuft weiter: **39**
  (0.27.0 war 38). Inhalt = 0.27.0 (Vorlesen, siehe unten). Ab jetzt: kleine Updates 2.1.0, 2.1.1 …, großes Update 3.0.0.
  Release-Tag `kurvenflug-v2.0.0`. Die Update-Prüfung vergleicht Zahlen, 2.0.0 > 0.27.0 – bestehende Installationen bekommen das Update.

## 0.27.0 – 🗣️ Vorlesen (Auftrag Marcel 17.11.) · Web = Android

- **Was vorgelesen wird:** Volts Sprechblasen (auf allen Seiten, damit auch Aufgaben/Aufträge, Kapitel-Erklärungen,
  Rückmeldungen), **Rechen-Stern** (🔊: Frage + alle drei Antworten, nach dem Antworten die richtige + Lösungsweg) und die
  **📖 Theorie** (🔊 je Erklärung, Merkformel, „alles vorlesen“). Tipp auf die Sprechblase = nochmal. Neuer Text unterbricht
  den alten; während Volt spricht, wird die Musik leiser.
- **Einstellungen (gemerkt):** an/aus (Standard an; ersetzt dann das Robo-Plappern), **Stimme**: Automatisch / jede deutsche
  Stimme des Geräts bzw. Browsers / **„Thorsten (natürlich)“**, **Tempo** 70–130 %, **▶ Probe hören**.
- **Formeln und Einheiten:** `Speech.clean` aus Raumlicht übernommen (gemeinsam Android + Web, Raumlicht-Tests
  mitgenommen) und für Kurvenflug erweitert: m/s, m/s², km/h, s, m, N, kg, h, „π/2“, „rechts/links“, f'(x), Live-Rechnung
  („Bahn 1 → 5“ = „ergibt“). Test prüft **alle** Spieltexte (Level, Generatoren, Rechen-Stern, Rechenwege, Theorie):
  keine Sonderzeichen übrig, saubere Satzzeichen.
- **Thorsten technisch wie Raumlicht:** dieselbe Stimmdatei (de_DE-thorsten-medium, 63 MB, SHA-256 geprüft), **nur auf
  Wunsch** geladen; Lautschrift vorab erzeugt (`tools/stimme/lautschrift.py`, liegt in `voice/stimme`, 870 feste Sätze +
  2185 Wörter + Zahlen 0–999) → **kein espeak-ng/GPL** in App oder WebApp. `VoiceDataTest`: jedes Wort aller Spieltexte
  hat Lautschrift. Unbekannte Wörter (z. B. Spielernamen) liest die Gerätestimme.
  - Android: sherpa-onnx 2.0.1 (Apache-2.0), nur ARM (wie Raumlicht).
  - **Web: ja, auch Thorsten** – ONNX Runtime Web 1.22 (MIT) rechnet dasselbe Modell mit denselben Lauten (Raumlicht-Web
    hat das nicht). Im Browser-Test: Laden + Prüfsumme 4,8 s, Thorsten spricht die Begrüßung nach dem ersten Tippen, offline
    danach weiter nutzbar (eigener Speicher „kf-stimme“, nicht im normalen Offline-Paket → Start bleibt so schnell wie bisher).
- **Datenschutz:** Gerätestimmen nur, wenn sie ohne Internet sprechen (es geht kein Text nach draußen). Vor dem Laden von
  Thorsten Hinweis für Eltern: Datei von huggingface.co (externer Anbieter, sieht wie bei jedem Download die IP-Adresse),
  keine Texte/keine Kinderdaten; „Stimme entfernen“ gibt den Platz frei. Lizenzen in den Einstellungen ergänzt.
- **Play-Ausnahme:** Die Play-Fassung hat bewusst kein Internet → dort nur Gerätestimmen (Vorlesen selbst gleich).
  Soll Thorsten auch in Play, müsste Marcel Internet + Datensicherheits-Angabe freigeben.
- **APK-Größe:** wächst durch sherpa-onnx (Maschinencode, komprimiert) – Raumlicht hat dasselbe.

## 0.26.0 – Klang-Identität KurvenFlug (Auftrag Marcel 17.11.) · Web = Android

- **Entstehung:** komplett im Code komponiert und erzeugt (`core/model/SpaceMusic.kt` = Noten, `shared/ui/fx/MusicSynth.kt`
  und `Synth.kt` = Klangsynthese). Keine Samples, keine fremden Dateien, keine Lizenzfragen; Android und Web nutzen
  denselben Code → **klingen gleich**. Keine zusätzliche Ladegröße (Musik entsteht erst nach der ersten Berührung,
  im Browser in kleinen Häppchen: längste Blockade 0,15 s statt vorher 2,3 s).
- **Musik:** Synthwave, A-Moll-Welt mit Arpeggios, weichen Flächen, Takt-Echo und Hall-Raum (Kamm- und Allpassfilter).
  Wiederkehrendes **Kurven-Motiv** (Melodie steigt wie eine Flugbahn und fällt) = Erkennungsmelodie und Thema im
  B-Teil. 16 Takte in zwei Teilen (A ohne, B mit Melodie) → keine kurze nervige Schleife.
  **Jedes Kapitel eigene Variation** (Tonart aus 12, Akkordfolge aus 8, Arpeggio-Muster aus 6, Tempo) – im Test
  geprüft: 30 Kapitel = 30 verschiedene Stücke. Menü ruhig (80–86 BPM), Flug 98–106, **Sturm/Boss 118–122 mit Beat,
  Extrem 128 mit Sechzehntel-Bass**.
- **Effekte:** Start/Schub, **Flug-Ton folgt der Kurve** (Tonhöhe = Höhe der Sonde), Treffer, **knapp daneben**
  (≤ 1 Kästchen, sanftes „oh-oh“) vs. daneben, **Sterne** (steigende Glocken je Stern), **Volt-Robo-Silben**
  (Ringmodulation, Tonhöhe folgt den Vokalen), Erkennungsmelodie beim Start.
- **Bedienung/Respekt:** Musik und Geräusche getrennt schaltbar, **je eigene Lautstärke** (Einstellungen);
  **nie vor der ersten Berührung** (jetzt auch Android); folgt Stummschaltung bzw. anderer Musik auf dem Gerät;
  **Klassen-Challenge still**. Kein Inhalt nur über Ton (alles auch sichtbar).

## 0.25.0 – 📖 Theorie-Knopf in jeder Übung (Wunsch Marcel)
- Jede Übung (alle 607 geprüften spielbaren Level inkl. Sturm, Duell, Andromeda, Extrem) hat einen Knopf 📖: zeigt die
  Kapitel-Erklärung Schritt für Schritt plus Merkformel, jederzeit, ohne Sterne-Abzug. Abgesichert im Prüftest
  („jede Übung hat eine Theorie“). Knopfleiste ab normaler Handybreite nur mit Symbolen, damit „Start“ nicht gequetscht wird.

## Korrektheitsprüfung 17.11. (Version 0.24.2, Commit 1fbf80d)

### Was geprüft wurde und wie
| Bereich | Wie |
|---|---|
| Fachlich, alle 30 Kapitel (175 Level) + 30 Extrem-Prüfungen | **5 unabhängige Prüfdurchgänge** (je ein Teil der Inhalte, jede Zahl von Hand bzw. mit Python nachgerechnet: Auftrag, Tipp, Quiz/Rechen-Stern, Rechenweg, Kapitel-Intro, Physik-Formeln/Einheiten, Lexikon-Fakten) **plus neuer automatischer Prüftest** `AuditTest` |
| `AuditTest` (bleibt dauerhaft in der Testkette) | 607 Level (Kapitel, Extrem, Generator aller 5 Stufen, Bosse, Andromeda 1–20, Rettungen, Gastplaneten, 31 Tagesmissionen): **eigene Schulbuch-Formeln** gegen die Flugbahn an 41 Stellen, Stationen auf der Bahn, **Wurfbahn gegen die Wurfparabel**, Lösbarkeit, Rechenweg-Schritte = Lösung, Live-Rechnung/Rechen-Stern ohne NaN und ohne doppelte Antworten, Text-Hygiene (Platzhalter, englische Dezimalpunkte, doppelte Leerzeichen, Klammern, doppelte Stufen-Namen) → **0 Abweichungen** |
| Stufen-Tests | `DifficultyTest`, `ChapterExtremeTest` grün (jede Stufe strikt schwerer) |
| Funktion | alle Unit-/Oberflächentests (inkl. Klicktest durch die App, 100+ Bildschirmfotos), Lint beider Varianten, APK + AAB gebaut; WebApp: **alle 30 Kapitel bei 320 px und am PC ohne Konsolenfehler**, Offline-Start, Auto-Update, **Pro mit echtem Test-Code** (Link + App-Logik), Bestandsschutz |
| Texte nach außen | Store-Texte (Längen: Titel 25/30, Kurz 80/80, Beschreibung < 4000), Datenschutz-Vorschlag, Preis 7,99 € inkl. 19 % MwSt., Web-Manifest |

### Abweichungen und Korrekturen (vorher → nachher)
**Fachlich falsch:**
- 14-4 Tipp: „tan α = 18 : 16 = 1,125 → α ≈ −48°“ → „… → α ≈ 48° – nach unten, also −48°“ (Vorzeichen)
- 9-3 / 14-7 Tipp: „≈ 7,1. Nicht aufrunden“ / „≈ 10,4. Nicht aufrunden“ (widersprüchlich, 7,07 → 7,1 ist aufgerundet) → „Nicht auf 8 aufrunden“ / „Nicht auf 11 aufrunden“
- 29-3 / 29-4: großer und kleiner Maßstab vertauscht → 10 km je Kästchen = „Kleiner Maßstab“, 0,5 km = „Großer Maßstab“
- 30-1 / 30-5 / 30-6 Rechenweg: Antwort auf 2 Stellen gerundet („0,5⁴ = 0,06“) → in Prozent gefragt: „0,5⁴ = 6,25 %“, „0,7⁵ ≈ 16,81 %“, „22 : 2⁶ ≈ 34,38 %“
- Rechen-Stern beim Wurf von der Klippe (4-4, Generator Profi/Extrem): „richtige“ Antwort „Weite = v² · sin(2α) : g“ gilt dort nicht → bei Höhenunterschied neue Frage „vx = v · cos α, vy = v · sin α“
- Rechen-Stern Wachstum mit d ≠ 0: „y wird mal b genommen“ → „(y − d) wird mal b genommen“
- Generator Extrem: Tipps für Sinus mit c und Wachstum mit d passten nicht → eigene Tipps („Bei x = c ist sin = 0 …“, „Bei x = 0 ist y = a + d …“)
- Kapitel 3 Intro „Je größer a, desto schmaler“ (falsch für a < 0) → „Je weiter a von 0 weg ist …“; Kapitel 5 „b staucht die Welle“ (b < 1 streckt) → „staucht (b > 1) oder streckt (b < 1)“; Kapitel 16 „Größer: stumpf“ → „Zwischen 90° und 180°: stumpf“; Kapitel 15 „Kommazahlen sind Zehntel … 0,25 = ¼“ → „0,25 = 25 Hundertstel = ¼“
- 26-3 Quiz „c = Stelle des Nulldurchgangs“ (bei d = 1 falsch) → „wo die Welle aufsteigend die Mittellinie kreuzt“; Kap. 27 „Fallzeit hängt nur von der Höhe ab“ → „von der Höhe (und vom Planeten)“, „g = 1,62“ → „1,62 m/s²“
- 30-3 Tipp: höchste Säule ⇒ μ ≈ 3 präzisiert; 28-3/28-6 „= −1“ → „≈ −1“; 28-5 Quiz „d abziehen“ (kein d im Auftrag) → „die 2 abziehen“
- Lexikon: Olympus Mons „fast dreimal“ → „rund zweieinhalbmal so hoch“; Marstag 24 h 37 min → Sol „etwa 24 h 40 min“; Merkur „kaum größer als der Mond“ → „nur etwas größer“; Saturnringe „280 000 km breit“ → „Ringsystem 280 000 km von Rand zu Rand“; Saturn „über 140 Monde“ → „über 270“; Voyager „über 22 Stunden“ → „fast einen ganzen Tag“
**Schreibweise / Darstellung:**
- Fehlende Klammern bei negativen Zahlen: Vektor-Tipps 22-1 bis 22-5 „(2 − −6)“ → „(2 − (−6))“; Tangente-Live-Rechnung; Parabel-Rechen-Stern „· −2²“ → „· (−2)²“
- Generator Profi/Extrem: „🔴 Profi: 🔴 Profi – getarnt!“ (Stufe doppelt) → einmal; Rätsel „√0 − 8“ ausgeschlossen
- Einheit „8m/s“ → „8 m/s“ (Wurf, alle Stellen); Regler „Treibstoff A“ bei 320 px abgeschnitten → „Fläche A“ + flexible Breite
- 5-7 „Jedes Kästchen halbiert sich der Wert“ → „Mit jedem Kästchen halbiert sich der Wert“; „Eulersche Zahl“ → „eulersche Zahl“; „Hausaufgabe-Code“ → „Hausaufgaben-Code“; „Weiter fliegen“ → „Weiterfliegen“; „Wenige Fehlschüsse“ → „Weniger“; „nochmal“ → „noch mal“; „waagrecht“ → „waagerecht“; „Noch 1 Bauteile“ → „Noch 1 Bauteil“; widersprüchlich „3 Gold-Teile nötig (hast 5)“ → „Erst die Aurora fertig bauen“; „Knapp verloren“ auch bei großem Abstand → nur bei 1 Versuch Unterschied
- Laser-Hinweis „weiter nach links drehen“ (mehrdeutig) → „gegen den / im Uhrzeigersinn“; Kap. 2 „b ist der Startpunkt“ → „wo die Gerade die y-Achse schneidet“; Kap. 8 „der Achse“ → „den Achsen“; Kap. 12 Rampe ohne Reibung und Drehung; Extrem-Prüfung „Ziele halb so groß“ (nicht immer) → „kleinere Ziele“
**Texte nach außen / Oberfläche:**
- „keine Käufe“ in den Eltern-Texten (Web/GitHub verkaufen Pro) → gestrichen; Galaxie „alle offen“ → „Level für Level“; „Kein Kapitel wird gesperrt“ → „Die Klasse sperrt nichts, sie sortiert nur“
- Inhaltsliste in den Einstellungen veraltet → vollständig (inkl. e-Funktion, Binomialverteilung, Maßstab, Zinseszins, beschleunigte Bewegung, waagerechter Wurf)
- Web-Manifest „Klasse 5–10“ → „5–13“; App-Name einheitlich „Kurvenflug – Mathe im All“
- **Play-Beschreibung** versprach Pro, das es in Play noch nicht gibt → „keine Käufe – alles frei spielbar, auch die Oberstufe“ (bei Pro-Start in Play zurückstellen); Datenschutz-Vorschlag „keine In-App-Käufe“ gestrichen
- CHANGELOG 0.23.1 bewarb die Schullizenz → neutral; Eltern-Anrede vereinheitlicht (neutral statt „Sie“/„dein Kind“); Play-Fassung: Pro-Text ohne „Code einlösen“/„ohne Konto“
- **Regelverstoß behoben:** Update-Check der GitHub-APK lief höchstens alle 30 Minuten → wie vorgeschrieben höchstens alle 20 Stunden (Text angepasst); README-Angaben (Klasse 5–13, Oberstufe) aktualisiert

### Bewusst nicht geändert (geprüft, kein Fehler)
- 3-2: zwei richtige Lösungen (d = 3 und d = 7) – beide treffen, Tipp weist jetzt darauf hin
- 7-1/7-3/7-6 gleicher Treffpunkt (2 | 3), 6-3/11-4 „hinter dir“, doppelter Titel „Um die Ecke“ (6-1/17-4) – fachlich richtig
- 49 ältere Level (Winkel, Laser, Pythagoras, Energie, Lichtbrechung, Teile von Wurf/Trigonometrie/Schleuder) treffen auch einen Reglerschritt neben der Lösung – gewollt großzügig, keine Falschaussage; Verschärfung wäre eine Spieländerung

### Was nicht (vollständig) prüfbar war
- **Lücken, keine Fehler:** 58 Level ohne Live-Rechnung und 31 ohne Rechen-Stern (Brüche, Winkel, Laser, Energie, Lichtbrechung, einzelne Boss-/Rettungs-Etappen) – dort wird nichts Falsches angezeigt, es fehlt nur die Zusatzhilfe
- Echte Android-Geräte (keins angeschlossen): geprüft über Robolectric-Bildschirmfotos und Klicktest; Play-Billing-Testkauf erst nach Anlage des Produkts; Android-12-Startbildschirm nur im Code
- Barrierefreiheit Web: Bildschirmleser-Spiegel ist bewusst aus (Compose-Web-Fehler „Node not found“); Android: Standard-Compose-Semantik, nicht mit TalkBack geprüft
- Marcels echter Kauf über die Kasse (Danke-Seite/E-Mail) – Test-Code vom Lizenz-Dienst funktioniert


## 17.11. – End-to-End-Test mit echtem Pro-Code bestanden (Version 0.24.1)

- Test-Code vom Lizenz-Dienst (Produkt 1, App 1, Serie 1001, Tag 17.11.2026; Code selbst nicht im Repo):
  Signatur mit dem öffentlichen Schlüssel gültig (Python/cryptography **und** App-Logik `License.check`).
- Freischalt-Link `https://kurvenflug.mathenachhilfe-online.com/?pro=<Code>` in der WebApp (0.24.1): Code wird
  gespeichert, Adresse aufgeräumt, keine Konsolenfehler. Mit dem Code sind Pro-Level (z. B. Ableitung 19-3) offen.
- Neu in 0.24.1: Lernstand-Themen klappen beim Antippen ihre Level mit Sternen auf (gezielt nochmal spielen).
- Offen: Marcels echter Testkauf über die Kasse (Danke-Seite + E-Mail) – Code bitte wieder schicken, ich prüfe ihn gleich.

## 0.24.0 – Pro über die Website freigeschaltet (Web + GitHub-APK)

- **Version:** 0.24.0 · **Commit:** bfa1fe2
- `pro/pro.properties`: publicKey `979eb515…b812` (vom Lizenz-Dienst), checkoutUrl `…/kasse/?bestellweg=aendern&add-to-cart=4135`
  (KF-PRO 7,99 €). Kasse geprüft (HTTP 200), Schlüssel gültig. **Ab dieser Version gelten in Web und GitHub-APK die
  Pro-Sperren** (Bestandsschutz für Begonnenes). Play-Fassung unverändert frei (Produkt in der Play Console fehlt noch, Marcel macht das später).
- Schullizenz-Hinweis und -Knopf **nicht** angezeigt (wird auf Klassen-Beitritt umgebaut). Schul-Codes, die es schon
  gäbe, würden weiter angenommen – es werden aber keine ausgegeben.
- **Bitte prüfen / End-to-End:** Nach Marcels Testkauf den Code an KurvenFlug schicken – ich prüfe ihn gegen die App
  (Link `https://kurvenflug.mathenachhilfe-online.com/?pro=<Code>` und Einfügen).

## Konzept Lehrerzugang für KurvenFlug (Auftrag Marcel vom 16.11. – nur Konzept, noch nicht gebaut)

**Heute schon da (ohne Konto, ohne Server):** Klassen-Challenge – die Lehrkraft stellt Level zusammen, die App gibt
einen Code; Schüler spielen und schicken einen Ergebnis-Code zurück, die App der Lehrkraft sammelt ihn ein
(Ergebnis-Tafel). Das bleibt als Offline-Weg erhalten, der Lehrerzugang macht es bequem und automatisch.

**1. Was die Lehrkraft sieht und steuert**

| Bereich | Inhalt |
|---|---|
| Auftrag | Kapitel (z. B. „Geraden“) **oder** eigene Level-Auswahl (wie Klassen-Challenge); Stufe Normal/Schwer/Profi/Extrem (Extrem-Prüfung optional); Anzahl Level; Frist; „Rechen-Stern Pflicht“ (Rechenweg-Frage muss stimmen) |
| Ergebnisse je Schüler | erledigte Level des Auftrags, Sterne, Fehlversuche, Rechen-Stern richtig/falsch, Zielhilfe benutzt ja/nein, Zeit pro Level, zuletzt aktiv |
| Ergebnisse je Thema (Klasse) | Anteil gelöst, Ø Sterne, Ø Fehlversuche, häufigste falsche Rechen-Antwort (zeigt typischen Denkfehler, z. B. „25 : 100 statt 1 + 25 : 100“), schwierigstes Level |
| Freischalten | Kapitel bzw. Stufen für die Klasse öffnen/sperren (z. B. Oberstufe nur ab Kl. 11, Extrem erst nach Kapitelende) |
| Prüfungsmodus | für einen Auftrag: keine Zielhilfe, kein Rechenweg, kein Lösungs-Vorführen durch Volt, ein Versuch je Level, Zeitfenster |
| Rangliste | Sterne/Sturm-Rekord der Klasse (Spitznamen), abschaltbar |

**2. Daten an den Klassen-Dienst (so knapp wie möglich)**

| Wann | Felder |
|---|---|
| Beitritt | Klassen-Code, Spitzname, zufällige Geräte-Kennung (UUID, nur für diese Klasse), App-Version |
| Nach jedem Level eines Auftrags (gesammelt, max. 1× pro Minute; offline in Warteschlange) | Auftrag-ID, Level-ID, Sterne 0–3, Fehlversuche, Rechen-Stern ja/nein, gewählte Rechen-Antwort (Index 0–3), Zielhilfe ja/nein, Sekunden, Zeitpunkt |
| Rangliste (wenn an) | Summe Sterne, Sturm-Rekord – höchstens 1× täglich |
| Abruf (beim Start, dann alle 7 Tage bzw. bei „Aktualisieren“) | neue Aufträge, Freischaltungen, Prüfungsmodus, Lizenzstatus der Klasse (aktiv bis …) |

Keine Klarnamen, keine E-Mail, kein Freitext, keine Standortdaten, keine Werbe-Kennung. Spielstand außerhalb von
Aufträgen bleibt auf dem Gerät. Austreten löscht die Klassendaten des Geräts beim Dienst.

**3. Beitritt und was der Schüler sieht**
- Beitritt per **Klassen-Code** (6 Zeichen), **QR-Code** (Kamera der Handy-App bzw. Link) oder **Link**
  `https://kurvenflug.mathenachhilfe-online.com/?klasse=<Code>`. Spitzname aus Vorschlagsliste (Tier + Farbe, z. B. „Blauer Fuchs“) – kein Freitext.
- Neuer Bereich „🏫 Meine Klasse“ (pro Spieler-Profil): Klassenname, offene Aufträge mit Frist und Fortschritt, Rangliste
  (falls an), „Pro durch Schullizenz bis 31.07.“. Andere Schüler sieht er nur in der Rangliste (Spitzname + Sterne).
- Eltern sehen in den Einstellungen: welche Klasse, welche Daten geschickt werden, „Klasse verlassen“.
- Play-Fassung: braucht dann eine Internet-Berechtigung (nur für den Klassen-Dienst) – Formulare (Datensicherheit:
  „App-Aktivitäten“, an Lehrkraft übermittelt) und Datenschutztext anpassen. Ohne Klasse bleibt die App offline.

**4. Frei oder Schullizenz**

| Frei | Schullizenz (Lehrkraft) |
|---|---|
| Klasse anlegen, Beitritt, Rangliste | Aufträge mit Frist und Auswertung je Schüler/Thema |
| Klassen-Challenge per Code (wie heute, offline) | Freischalten/Sperren von Kapiteln und Stufen, Prüfungsmodus |
| | Pro für alle Schüler der Klasse (solange in der Klasse und Lizenz gültig), Export (CSV) |

Vorschlag Ablauf in der App: Lizenzstatus kommt beim Beitritt vom Dienst, gilt offline bis zum Ablaufdatum, Prüfung
alle 7 Tage; wer die Klasse verlässt, behält nur Einzelkauf/eigenen Code.

## 0.23.0 – Kurvenflug Pro gebaut (Freigabe Marcel: „ja, bau es so“; nicht USt.-befreit)

- **Version:** 0.23.0 · **Commit:** eae218c (Zweig claude/gifted-shannon-657gpr)
- **Noch sperrt nichts:** Pro-Sperren greifen erst, wenn ein Kaufweg angeboten wird – Play: Produkt `kurvenflug_pro`
  in der Play Console aktiv; Web/GitHub: `kurvenflug/pro/pro.properties` mit `publicKey` (Lizenz-Dienst) und
  `checkoutUrl` (Woo-Kasse). Bis dahin zeigt die Pro-Seite „Pro kommt bald – bis dahin ist alles frei“.
- **Gebaut:** Pro-Regeln mit Bestandsschutz (Pro.kt), Pro-Seite mit Vorteilen, Preis (Web: „7,99 € einmalig, inkl. 19 % MwSt.“,
  Play: Preis von Google), **Frage für Erwachsene** (Geburtsjahr, ≥ 18) vor jedem Kauf, Code-Eingabe, Freischalt-Link
  `…/?pro=KF…` (Web) bzw. „Teilen → Kurvenflug“ / Einfügen (GitHub-APK), Play Billing 8.3 (Kauf, Bestätigung,
  Wiederherstellen), Sperren an Kapitelkarten, Extrem-Prüfungen, Sturm (ohne Pro bis „Schwer“), Andromeda, Spieler-Profilen,
  Werkstatt; Einstellungen → „⭐ Kurvenflug Pro“; Eltern-Text angepasst.
- **Play-Fassung bleibt ohne Internet-Berechtigung** (die Billing-Bibliothek bringt INTERNET mit, ist entfernt – der Kauf
  läuft über die Play-Store-App). **Pflicht vor Freigabe:** Testkauf im internen Test (Anleitung `store/PLAY_CONSOLE_ANLEITUNG.md` §6a).
- **Für den Lizenz-Dienst (Leitstand):** Code-Format und Prüfung in `core/model/.../License.kt`, Erzeuger
  `kurvenflug/pro/pro_code.py` (`keygen`, `pro <Nr>`, `school <Nr> <bis-Datum>`; braucht `cryptography`). Ed25519, der Code
  enthält App-Nummer (Kurvenflug = 1) – **ein Schlüsselpaar kann für alle Apps dienen**. Geheimer Schlüssel nur in
  Bitwarden/beim Dienst. Codes sind 117 Zeichen lang (Signatur) → in der E-Mail als Link + zum Kopieren, nicht zum Abtippen.
  Freischalt-Link angenommen als `https://kurvenflug.mathenachhilfe-online.com/?pro=<Code>` (Live-Adresse bitte bestätigen).
- **Tests:** Ed25519/SHA-512 gegen die Java-Krypto (Zufallsschlüssel), feste Code-Beispiele auf JVM **und** im Browser (Wasm),
  gefälscht/fremde App/abgelaufene Schullizenz werden abgelehnt, Code mitten in E-Mail/Link wird gefunden; Pro-Regeln
  (ohne Kaufweg alles frei, 24 freie Kapitel, 35 Pro-Level, Bestandsschutz, Sturm-Grenze).
- **Bitte prüfen (kurvenflug-test):** Einstellungen → „⭐ Kurvenflug Pro“ (zeigt „kommt bald“); sonst alles wie bisher frei.

## Konzept Pro für KurvenFlug (Auftrag Marcel vom 16.11. – nur Konzept, noch nicht gebaut)

### 1. Was bleibt frei, was wird Pro?
Leitgedanke: Kinder sollen **den ganzen Schulstoff bis Klasse 10 kostenlos** üben können – sonst verlieren wir die
Empfehlung von Schulen und Eltern. Pro ist für **Oberstufe, Könner und Fans**.

| Frei (wie heute) | Pro |
|---|---|
| Alle Kapitel Klasse 5–10 (24 Kapitel, ca. 140 Level) mit Rechenweg, Rechen-Stern, Live-Rechnung | **Oberstufe**: Ableitung, Extrempunkte, Integral, Vektoren, e-Funktion, Binomialverteilung (6 Kapitel, 35 Level) |
| Tagesmission, Duell, Formel-Detektiv, Lernstand, Wiederholen | **🔥 Extrem-Prüfungen** aller 30 Kapitel |
| Asteroidensturm Leicht bis Schwer (Welle 1–12) | **Sturm Profi und Extrem** (ab Welle 13), eigene Rekordliste |
| Bosse, Aurora-Geschichte, Andromeda Planet 1–6 | **Andromeda ab Planet 7** (Profi/Extrem) und das Schiff Nova |
| Werkstatt: Grund-Aussehen | Werkstatt: zusätzliche Sonden, Spuren, Volt-Kostüme (rein kosmetisch) |
| Bis 2 Spieler-Profile | Bis 6 Profile (Familie, Geschwister) |
| Klassen-Challenge mit freien Leveln – **bleibt für Lehrkräfte kostenlos** (bringt die App in Klassen) | Klassen-Challenge mit Oberstufen-/Extrem-Leveln; Schullizenz (s. u.) |

**Bestandsschutz:** Wer ein Pro-Level schon geschafft hat, behält Sterne und kann es weiter spielen. Kein Fortschritt geht verloren.

**Preis:** **Einmalkauf statt Abo** – für Eltern ehrlich und ohne Abo-Falle, passend zu einer Kinder-App.
- **Kurvenflug Pro: 7,99 € einmalig, brutto inkl. 19 % USt.** (Familie, alle Profile auf dem Gerät/Browser), zum Start 4,99 €.
- **Schullizenz: 29 € brutto pro Lehrkraft, gültig bis 31.07.** (festgelegt von Marcel): Pro für eine Lehrkraft und alle
  Schülerinnen und Schüler ihrer Klassen, im Unterricht und zu Hause, auf beliebig vielen Geräten. Umsetzung: ein Schul-Code
  (Produkt 2, Ablaufdatum 31.07.), den die Lehrkraft an ihre Klassen weitergibt – kein Geräte- oder Schülerlimit in der App.
- Vergleich (Recherche 16.11., Preise schwanken je Land/Aktion):
  - DragonBox Algebra (Kahoot!): Einmalkauf ca. 7–8 €.
  - Photomath Plus: ca. 55–65 € im Jahr bzw. ca. 10 € im Monat.
  - Prodigy Math: 59–119 $ im Jahr.
  - Brilliant Premium: ca. 150 $ im Jahr (teils mehr).

  Mit 7,99 € einmalig liegt Kurvenflug am unteren Ende, nah an DragonBox. Ein Abo wäre bei einem Spiel ohne laufende
  Serverkosten schwer zu begründen.

### 2. Kaufwege
- **Play-Fassung: Google Play Billing (Pflicht)**: ein Einmal-Produkt „pro“ (nicht verbrauchbar), Kauf bestätigen (acknowledge).
  Kauf läuft über die Play-Store-App, die Play-Fassung braucht dafür **keine Internet-Berechtigung**. Google behält
  15 %. Familienrichtlinie: Kauf nur hinter einer **Eltern-Frage** (z. B. „Gib dein Geburtsjahr ein“ / Rechenaufgabe für
  Erwachsene). Keine Lockangebote, keine Kauf-Hinweise mitten im Level, klarer Preis. Family-Link-Freigabe greift automatisch.
  Formulare anpassen: IARC „Digitale Käufe: Ja“, Store-Text „In-App-Käufe“.
- **Web- und GitHub-Fassung: über unsere Website** (wie Mathe-Trainer Pro): Knopf „Pro freischalten“ (hinter
  Eltern-Frage) → öffnet die Woo-Kasse auf mathenachhilfe-online.com → nach Zahlung Code auf der Danke-Seite und per
  E-Mail an Kunde und Marcel, Bestellung bleibt in WooCommerce als Nachweis → Code in der App eingeben (oder Link
  `…/kurvenflug/?pro=CODE` öffnet die App und schaltet frei). **Kein Konto**, Code gilt für Web und GitHub-APK.
  An der Kasse: Häkchen zum sofortigen Beginn der Leistung (Widerrufsrecht bei digitalen Inhalten).
- **Was bei Marcel ankommt (je Kauf, gerundet; Zahlungsgebühren Web geschätzt: Stripe 1,5 % + 0,25 €, PayPal 2,99 % + 0,39 €):**

  | Produkt | Brutto (inkl. 19 % USt.) | Netto | USt. | Web: Stripe | Web: PayPal | Google Play (netto − 15 %) |
  |---|---|---|---|---|---|---|
  | Kurvenflug Pro | 7,99 € | 6,71 € | 1,28 € | ca. 6,34 € | ca. 6,09 € | 5,71 € |
  | Pro Startpreis | 4,99 € | 4,19 € | 0,80 € | ca. 3,87 € | ca. 3,65 € | 3,56 € |
  | Schullizenz (Jahr) | 29,00 € | 24,37 € | 4,63 € | ca. 23,68 € | ca. 23,11 € | 20,71 € (falls über Play) |

  Bei Google Play: Google führt die USt. ab, vom Netto gehen 15 % Gebühr ab (Abo- und Einmalkäufe für kleine
  Entwickler). Web: die eigene Zahlungsgebühr des gewählten Anbieters (Werte oben nur Richtwerte, bitte mit eurem
  Woo-Zahlungsanbieter abgleichen). Anzeige in der App: Web „7,99 € einmalig · inkl. 19 % MwSt.“, Play zeigt Googles Endpreis.
- **Umsatzsteuer (Marcel ist nicht USt.-befreit)**: Preise sind Bruttopreise inkl. 19 % USt.; Woo-Rechnung mit
  ausgewiesener USt., Woo-Steuerklasse Standard 19 %. Digitale Leistungen an Verbraucher in anderen EU-Ländern: Steuersatz des Kundenlandes (OSS) –
  in WooCommerce einstellen bzw. mit dem Steuerberater klären. Bei Google Play führt Google die USt. selbst ab.
- Play-Käufer, die auch im Browser spielen wollen: Code per E-Mail-Anfrage mit Play-Bestellnummer (später automatisch,
  s. Punkt 3).

### 3. Was der gemeinsame Lizenz-Dienst können muss
- **Signierte Codes, offline prüfbar**: Der Dienst signiert `{App, Produkt, Bestell-Kennung (gehasht), Datum}` mit einem
  privaten Ed25519-Schlüssel; die App enthält nur den öffentlichen Schlüssel und prüft ohne Internet. Privater Schlüssel
  nur auf dem Server / in Bitwarden, nie im Repo. Code-Format kurz zum Abtippen, z. B. `KF-PRO-7Q4M-2XH9-…` (Base32 + Prüfsumme).
- **Woo-Anbindung**: Webhook „Bestellung abgeschlossen“ → Code erzeugen → in der Bestellung speichern → E-Mail an
  Kunde + Marcel (Rechnung aus WooCommerce). Ein Produkt je App plus optional ein **Bundle** über alle Apps.
- **Sperrliste** für Erstattungen/Missbrauch: kleine JSON-Datei (`/lizenz/sperrliste.json`), Web und GitHub-APK lesen
  sie gelegentlich; die Play-Fassung braucht sie nicht (Google erstattet selbst).
- **Schullizenz**: ein einziger Code mit Ablaufdatum 31.07. (Produkt 2) für Lehrkraft und alle ihre Klassen, beliebig viele Geräte.
- **Später, optional**: Prüfung von Play-Kaufbelegen über die Google Play Developer API → Web-Code für Play-Käufer
  automatisch.
- Für die Apps: eine gemeinsame kleine Bibliothek „Lizenz“ (Code prüfen, speichern, Play-Billing-Adapter, Eltern-Frage),
  damit alle Apps gleich funktionieren. In Kurvenflug: Schalter `PRO_PLAY` / `PRO_WEB` je Variante wie `SELF_UPDATE`.
- Datenschutz: Die App speichert nur „Pro: ja + Code“. Keine E-Mail, kein Name in der App.

### 4. KI über die Claude-API – sinnvoll?
- **Kosten wären gering**: Claude Haiku 5.5 kostet 0,10 $ je Mio. Eingabe- und 0,50 $ je Mio. Ausgabe-Token. Eine
  Erklärung („Warum hat meine Bahn die Station verfehlt?“, ca. 1.500 Token rein, 300 raus) kostet etwa **0,03 Cent**;
  100 Erklärungen pro Kind und Monat ≈ 3 Cent.
- **Aber jetzt nicht empfohlen**:
  1. Die Play-Fassung hat bewusst kein Internet. Das ist ein Pluspunkt bei Eltern und in den Datenschutz-Formularen.
  2. Kinder-Daten: Wir bräuchten einen eigenen Server als Zwischenstation (kein Schlüssel in der App), Mengenbegrenzung,
     Filter, Datenschutz-Folgenabschätzung und eine Prüfung der Anbieter-Regeln für Minderjährige.
  3. Kurvenflug erklärt schon heute ohne KI: Rechenweg, Rechen-Frage, Live-Rechnung und Volts Tipps sind für jede Aufgabe fest hinterlegt.
- **Wenn später gewünscht**: „Frag Volt“ nur in Web/GitHub-Pro, freiwillig, ohne Freitext-Eingabe (Kind tippt auf
  „Erklär's mir anders“, wir senden nur Level-Daten und die falschen Reglerwerte, nie persönliche Daten), über einen
  Server-Proxy mit Tageslimit.

### Nächste Schritte (nach Freigabe durch den Leitstand/Marcel)
1. Gemeinsamer Lizenz-Dienst + Woo-Produkt (Leitstand). 2. Lizenz-Bibliothek + Eltern-Frage in Kurvenflug,
Pro-Sperren mit Bestandsschutz, Play-Billing in der Play-Variante (parallel zur Play-Store-Arbeit). 3. Datenschutz-Text und
Play-Formulare anpassen.

## 0.22.0 – Schwierigkeit steigt nachweislich bis „Extrem“ (Vorgabe Marcel vom 16.11.)

- **Version:** 0.22.0 · **Commit:** a5fec15 (Zweig claude/gifted-shannon-657gpr)
- **Wo es in Kurvenflug Schwierigkeit gibt:** der Level-Generator (Asteroidensturm, Andromeda, Rettungsmissionen,
  Bosse, Gastplaneten, Tagesmission, Duell) und die 30 Kapitel. Werkstatt (nur Aussehen) und Klassen-Challenge
  (Lehrkraft wählt Level) haben keine eigene Schwierigkeit.
- **Generator: 5 Stufen** 🟢 Leicht · 🔵 Normal · 🟠 Schwer · 🔴 **Profi** (neu) · 🔥 **Extrem** (neu).
  - Profi: Zielzonen 60 %, 3 Stationen, Regler doppelt so fein, 4 Asteroiden dicht an der Bahn, Wurf von einer Klippe
    (Winkel, Tempo, Schwerkraft und Höhe zugleich), getarnte Koordinaten mit 3 Rechenschritten in halben Kästchen.
  - Extrem: Zielzonen 40 %, 4 Stationen, 6 Asteroiden 0,2 Kästchen neben der Bahn, **zusätzlicher Parameter**
    (Sinus mit Verschiebung c → 4 Unbekannte, Wachstum mit Verschiebung d), Wurf mit 2 Ringen unterwegs,
    Koordinaten in Viertel-Kästchen mit Wurzel im Funkspruch, Rendezvous mit Viertel-Steigungen.
  - Einsatz: Asteroidensturm alle 4 Wellen eine Stufe höher (ab Welle 17 Extrem, Stufe steht im Titel),
    Andromeda Planet 1–6 Schwer, 7–12 Profi, ab 13 Extrem; Rettungsmission ab Nr. 8 mit Profi-Schluss-Etappe.
    Tagesmission und Duell bleiben unverändert (Duell-Codes funktionieren weiter).
- **Kapitel:** Die Kapitel steigern sich inhaltlich (neue Rechenarten), messbar war die letzte Aufgabe aber oft nicht
  schwerer als die erste. Deshalb neu: **🔥 Extrem-Prüfung in jedem der 30 Kapitel** (öffnet nach allen Leveln,
  freiwillig, zählt nicht zum Kapitel-Abschluss → niemand verliert Fortschritt; eigene Sterne).
- **Nachweis (automatische Tests, schlagen fehl, wenn eine Stufe nicht strikt schwerer ist):**
  `core/model/src/jvmTest/.../DifficultyTest.kt` (je Bahnart und gesamt: Gesamtwert strikt steigend, Lösungsbereich
  kleiner, Zielzone kleiner, Hindernisse/Rechenschritte/Parameter nicht weniger; Extrem: Zielzone ≤ 0,35,
  ≥ 4,5 Hindernisse, Lösungsbereich ≤ 5 %, jedes Level lösbar und nur auf dem Raster) und `ChapterExtremeTest.kt`
  (Prüfung schwerer als jedes Level ihres Kapitels).
  Kennzahlen: Parameter = Unbekannte; Zielzone = mittlerer Radius; Rechenschritte = Unbekannte + Stationen +
  Rechenzeichen im Funkspruch + Klippe; Lösungsbereich = Anteil der Reglerstellungen, die noch treffen, wenn man
  einen Regler allein bewegt (Löser prüft jede Stellung); Gesamtwert = Parameter + Rechenschritte + ½·Hindernisse +
  1/Zielzone + 20·(1 − Lösungsbereich). Mittel über alle 7 Bahnarten × 12 Startwerte:

| Stufe | Parameter | Zielzone (Radius, Kästchen) | Hindernisse | Rechenschritte | Lösungsbereich | Gesamtwert |
|---|---|---|---|---|---|---|
| 🟢 Leicht | 2,3 | 0,86 | 0,0 | 4,1 | 11,0 % | 25,5 |
| 🔵 Normal | 2,3 | 0,66 | 1,0 | 4,4 | 9,9 % | 26,8 |
| 🟠 Schwer | 2,3 | 0,56 | 2,4 | 4,7 | 9,1 % | 28,3 |
| 🔴 Profi | 2,3 | 0,39 | 3,4 | 5,7 | 4,6 % | 31,5 |
| 🔥 Extrem | 2,6 | 0,26 | 4,6 | 6,7 | 3,8 % | 34,9 |

  Extrem-Prüfung je Kapitel (Gesamtwert gegenüber dem schwersten Level des Kapitels):

| Kapitel | schwerstes Level (Gesamtwert) | Extrem-Prüfung | Zielzone | Hindernisse | Lösungsbereich Vorlage → Extrem |
|---|---|---|---|---|---|
| 📍 Koordinaten | 27,4 | **31,3** | 0,70 → 0,35 | 4 | 5,3 % → 2,7 % |
| 🍕 Brüche & Kommazahlen | 40,6 | **59,3** | 0,06 → 0,03 | 4 | 0,6 % → 0,3 % |
| 📐 Winkel | 26,0 | **31,5** | 0,30 → 0,15 | 4 | 1,7 % → 0,8 % |
| 🗺️ Maßstab & Karten | 25,9 | **30,5** | 0,50 → 0,25 | 4 | 5,3 % → 2,7 % |
| ⚖️ Zuordnungen | 24,8 | **28,6** | 0,70 → 0,35 | 5 | 7,3 % → 3,7 % |
| ⏱️ Weg & Zeit | 26,5 | **30,4** | 0,70 → 0,35 | 4 | 4,6 % → 2,4 % |
| 🔦 Laser-Optik | 25,4 | **28,1** | 0,60 → 0,36 | 5 | 1,1 % → 0,8 % |
| 📈 Geraden | 28,4 | **32,3** | 0,70 → 0,35 | 8 | 5,0 % → 2,6 % |
| 💶 Prozent & Zinseszins | 26,4 | **32,7** | 0,25 → 0,13 | 4 | 2,9 % → 1,4 % |
| 🤝 Rendezvous | 30,3 | **35,4** | 0,35 → 0,18 | 6 | 2,7 % → 1,4 % |
| 📡 Pythagoras | 27,8 | **34,2** | 0,25 → 0,13 | 5 | 3,4 % → 1,7 % |
| 🌙 Parabeln | 29,9 | **33,8** | 0,70 → 0,35 | 5 | 5,0 % → 2,6 % |
| 🎯 Nullstellen | 28,3 | **32,3** | 0,70 → 0,35 | 4 | 5,6 % → 2,9 % |
| 🎢 Energie | 22,0 | **26,1** | 0,80 → 0,40 | 4 | 14,3 % → 7,2 % |
| 💎 Lichtbrechung | 24,7 | **29,3** | 0,50 → 0,25 | 5 | 6,7 % → 1,1 % |
| 🎯 Wurf & Schwerkraft | 25,9 | **29,1** | 0,80 → 0,40 | 5 | 11,8 % → 4,4 % |
| 🚗 Beschleunigte Bewegung | 27,4 | **32,5** | 0,40 → 0,20 | 4 | 5,3 % → 2,7 % |
| 📐 Trigonometrie | 26,8 | **33,2** | 0,25 → 0,13 | 5 | 3,4 % → 1,7 % |
| 🌊 Wellen & Wachstum | 27,4 | **32,6** | 0,70 → 0,35 | 6 | 15,1 % → 6,3 % |
| 📊 Potenzfunktionen | 24,3 | **29,2** | 0,40 → 0,20 | 4 | 7,7 % → 4,0 % |
| 🏃 Waagerechter Wurf | 25,8 | **30,6** | 0,40 → 0,20 | 4 | 3,4 % → 1,8 % |
| 🌊 Verschobene Wellen | 28,3 | **34,1** | 0,30 → 0,15 | 4 | 5,3 % → 2,7 % |
| 🔢 Logarithmus | 26,3 | **30,4** | 0,35 → 0,33 | 4 | 5,4 % → 2,8 % |
| 🪐 Schwerkraft-Schleuder | 27,0 | **29,7** | 0,80 → 0,48 | 6 | 3,9 % → 2,1 % |
| 🏂 Ableitung & Tangente | 24,6 | **28,5** | 0,60 → 0,30 | 5 | 3,0 % → 1,5 % |
| ⛰️ Extrempunkte | 25,3 | **30,4** | 0,35 → 0,18 | 4 | 2,7 % → 1,4 % |
| 🧭 Vektoren | 27,7 | **32,2** | 0,45 → 0,23 | 4 | 2,7 % → 1,4 % |
| ⛽ Integral | 56,2 | **91,6** | 0,03 → 0,02 | 4 | 0,6 % → 0,3 % |
| 📈 e-Funktion | 28,5 | **34,3** | 0,30 → 0,15 | 4 | 4,0 % → 2,0 % |
| 🎲 Binomialverteilung | 32,9 | **45,5** | 0,10 → 0,05 | 4 | 5,3 % → 2,7 % |

- **Geprüft:** alle Unit-/Oberflächentests (Gleichstand JVM = Wasm), Lint beider Varianten, APK + AAB, WebApp bei 320 px
  ohne Konsolenfehler, Bildschirmfotos 91–93 angesehen.
- **Bitte prüfen:** Galaxie → ein fertiges Kapitel → „🔥 Extrem-Prüfung“; Spiele → Asteroidensturm (Stufe im Titel).

## 0.21.1 – Schneller Start, Ladebalken, kein weißer Bildschirm (Auftrag Marcel vom 16.11.)

- **Version:** 0.21.1 · **Commit:** d6d0962 (Zweig claude/gifted-shannon-657gpr)
- **Hauptursache gefunden:** Beim ersten Start lud der Service Worker (Offline-Speicher) **alle Dateien ein zweites Mal**,
  parallel zur App – auf dem Handy also ~9,9 MB statt 5,2 MB über dieselbe Leitung. Jetzt registriert er sich erst,
  wenn die App läuft, und fragt nur „geändert?“ (der Testserver antwortet korrekt mit 304, geprüft).
- **Schneller:** Wasm-Dateien und Mathe-Schrift werden sofort beim Seitenaufruf parallel angefragt (nicht erst nach
  kurvenflug.js) und als Strom an die App gereicht – das Übersetzen des Wasm läuft weiter schon während des Ladens.
- **Echter Ladebalken:** „Lade … 54 %“ nach geladenen Bytes (Gesamtgrößen setzt der Build ein), danach
  „Starte die Triebwerke …“ und sanftes Überblenden in die App. Ladebild erscheint sofort (inline, kein weißer Moment).
- **Android:** Android-12+-Startbildschirm in App-Farbe (`values-v31`, `windowSplashScreenBackground` #0B1026),
  darunter war das Fenster schon dunkel – kein weißes Aufblitzen.
- **Messung** (Chromium, Drosselung **am Server**, damit auch Service-Worker-Downloads zählen; 412 px; gzip wie Testserver):

  | Netz | Start | vorher (0.21.0) bedienbar | nachher (0.21.1) bedienbar | Download vorher → nachher | erster Inhalt |
  |---|---|---|---|---|---|
  | Fast 4G (9 Mbit/s, 60 ms) | 1. Start | 10,5 s | **6,0 s** | 9,9 MB → **5,2 MB** | ~0,1 s (beide) |
  | Fast 4G | 2. Start | 0,85 s | 0,84 s | 0 MB | ~0,06 s |
  | Slow 4G (1,6 Mbit/s, 150 ms) | 1. Start | 52,5 s | **28,2 s** | 9,9 MB → **5,2 MB** | ~0,2 s (beide) |
  | Slow 4G | 2. Start | 0,9 s | 0,84 s | 0 MB | ~0,07 s |

  Größen (gzip): Skiko-Wasm 3,26 MB, App-Wasm 1,55 MB, Emoji-Schrift 0,52 MB, kurvenflug.js 0,11 MB, Rest < 20 kB.
- **Geprüft:** Auto-Update (0.21.1 → Testversion 0.99.0 wird nach wenigen Sekunden übernommen), Offline-Start ohne
  Netz, 320 px ohne Konsolenfehler, alle Unit-/Oberflächentests, Lint beider Varianten, APK + AAB gebaut.
- **Bitte prüfen:** Ladebalken beim ersten Öffnen (am besten im privaten Fenster), zweiter Start < 1 s, Android-Start dunkel.
- **Zu Marcels Frage „bündeln und verkleinern“:** Ist bereits so (webpack-Produktionsbuild + Binaryen-optimiertes Wasm).
  Mehr bringt nur noch **Brotli am Server** (5,2 → ca. 4,0 MB, ~25 % weniger) – das ist Server-Konfiguration,
  die Dateien selbst bleiben gleich. Der große Brocken ist die Grafik-Engine (Skia), die jede Compose-Web-App braucht.

## 0.21.0 – Play Store vorbereitet + Binomialverteilung (Auftrag vom 16.11., vorgezogen)

- **Version:** 0.21.0 · **Commit:** 3d4f3e6 (Zweig claude/gifted-shannon-657gpr)
- **Play Store (alles erledigt, was ohne Marcels Konto geht):**
  - Zwei Varianten aus einem Code: `github` (APK, eigener Updater) und `play` (AAB). Die Play-Variante hat **kein Internet,
    keine Installations-Berechtigung, keinen eigenen Updater**; Updates über die **Play In-App Updates API** (flexibel,
    Installation im Hintergrund; „sofort“ bei Priorität ≥ 4 oder > 30 Tage alt). Kein Nachladen von Code.
  - **Signiertes AAB** baut der Release-Workflow (`Kurvenflug-<V>-play.aab` am GitHub-Release), Signatur wird geprüft.
  - **App-Signaturschlüssel:** neuer Workflow „Kurvenflug: Signaturschlüssel für Google Play“ exportiert den bestehenden
    Schlüssel mit Googles PEPK verschlüsselt – Marcel muss nichts installieren. Damit Wechsel APK ↔ Play ohne Deinstallieren.
  - **Store-Eintrag:** `kurvenflug/store/` – Titel, Kurz-/Langbeschreibung, Release-Text, 8 Screenshots 1080 × 2160,
    Feature-Grafik 1024 × 500, Icon 512.
  - **Formulare:** Datensicherheit (keine Datenerhebung), IARC-Einstufung (alles Nein → USK 0/PEGI 3), Zielgruppe 9–17
    mit Familienprogramm, Werbung Nein, Datenschutz-URL + Textvorschlag – alles in `store/PLAY_CONSOLE_ANLEITUNG.md`.
  - **Lizenzen:** geprüft, alles Apache 2.0 bzw. Play-Core-Bedingungen (nur Play-Build); Hinweis in der App
    (Einstellungen → Open-Source-Lizenzen), Übersicht `store/LIZENZEN.md`.
  - **Klick-Liste für Marcel:** `store/PLAY_CONSOLE_ANLEITUNG.md` (App anlegen → Schlüssel hochladen → Store-Eintrag →
    App-Inhalte → interner Test → geschlossener Test 12 Tester / 14 Tage → Produktion).
- **Neu: Binomialverteilung** (Kapitel 30, Kl. 10–13): Histogramm mit Dichte P(X = k) und kumuliert P(X ≤ k), 6 Level,
  jede mit Rechenweg und Rechen-Frage. Jetzt **175 Level in 30 Kapiteln**.
- **Prüfung:** Gleichstand JVM = Wasm (neuer Fingerabdruck), alle Level lösbar, Nachbarwerte treffen nicht, Regression,
  Lint beider Varianten; Browser bei 320 px und PC ohne Konsolenfehler (siehe unten).
- **Ladezeit (Bitte an den Server):** Die WebApp ist bereits gebündelt und verkleinert (webpack-Produktionsbuild,
  Wasm mit Binaryen optimiert). Den größten Gewinn bringt jetzt **Komprimierung durch den Webserver** für `.wasm`, `.js`,
  `.ttf`, `.json`: gemessen 14,6 MB → **3,8 MB mit Brotli** (gzip: 4,9 MB). Bitte Brotli/gzip für diese Typen aktivieren
  (vorkomprimierte .br/.gz-Dateien sind laut Dateityp-Liste nicht erlaubt). Ab dem zweiten Besuch lädt ohnehin alles aus dem Service Worker.
- **Standard-Branch:** Bitwarden-Hinweis in `.github/workflows/kurvenflug-release.yml` auch auf `ccr-0e3accf3-cdxlc8` (fa5687c).

---

## 0.20.0 – großer Inhaltsausbau (Auftrag vom 16.11.)

- **Version:** 0.20.0 · **Commit:** ae5831d (Zweig claude/gifted-shannon-657gpr)
- **Neu: 38 Level in 7 Kapiteln** (jetzt 169 Level in 29 Kapiteln, Klasse 5–13):
  Maßstab & Karten (Kl. 5–6), Prozent & Zinseszins (Kl. 7–8), Beschleunigte Bewegung (Physik 9–10),
  Potenzfunktionen inkl. Wurzel (Kl. 10), Verschobene Wellen a·sin(b(x−c))+d (Kl. 10–11),
  Waagerechter Wurf auf Erde/Mond/Mars (Physik 10), e-Funktion & ln (Kl. 11–12).
- **Erklärung je Aufgabe:** Tipp, eigener Rechenweg (Schritte zum Antippen), eigene Rechen-Frage mit typischen
  Fehlern als Ablenkern, Live-Rechnung beim Schieben, Lösungsweg nach dem Treffer, Kapitel-Einführung durch Volt.
- **Prüfung (automatisch):**
  - Gleichstand JVM = Wasm: Fingerabdruck aller 169 Level, Bahnen, Tagesmissionen und Codes identisch.
  - Alle Level lösbar, Startwerte lösen nicht, **ein Reglerschritt neben der Lösung trifft nicht** (Raten reicht nicht).
  - Jeder Rechenweg wird gegen die Lösung geprüft; jede Rechen-Frage hat drei verschiedene Antworten.
  - Regression: 213 Tests grün (Logik, Datenbank-Migration, 102 Oberflächen-/Bildschirmfoto-Tests inkl. Klicktest), Lint ohne Fehler.
  - Browser (Chromium) bei **320 × 640** und **1280 × 800**: alle 11 neuen bzw. Oberstufen-Kapitel geöffnet – keine Konsolenfehler.
- **Dabei behoben:** auf sehr schmalen Handys (320 px) wurde der Start-Knopf zerquetscht (jetzt kompakte Knöpfe);
  im Browser fehlte das Zeichen „ˣ“ und Pfeile erschienen als bunte Emoji (Schriften korrigiert).
- **Prüfen auf der Testadresse:** Klasse 8 wählen → Galaxie → Liste → „Prozent & Zinseszins“ → Level 1: Faktor b = 1,25
  → Treffer → Rechen-Frage „1 + 25 : 100“. Physik: „Waagerechter Wurf“ → Mondklippe (v = 3 m/s).

---

## Stand 16.11.2026 (Antwort auf die Anfrage von heute)

**Hinweis:** Diese Datei liegt auf dem Entwicklungszweig `claude/gifted-shannon-657gpr` und auf `kurvenflug-test`
(nicht auf `main`).

**Web = Android: erledigt.** Eine gemeinsame Codebasis (Kotlin Multiplatform + Compose Multiplatform, Kotlin/Wasm).
Jede Version erscheint gleichzeitig als APK (GitHub-Release) und WebApp (`kurvenflug/webapp/` auf `kurvenflug-test`,
plus ZIP am Release) mit derselben Nummer. Ein Gleichstands-Test vergleicht bei jedem Build einen Fingerabdruck aller
Level, Bahnen, Tagesmissionen und Codes auf JVM und Wasm. Aktuell: **0.19.0** (Oberstufe Kl. 11–13).
WebApp: installierbar, offline, aktualisiert sich selbst (ohne Knopf), Impressum/Datenschutz, keine externen Aufrufe.

**Play Store: noch nicht begonnen** – Marcel hat den Start auf **17.11., 15:00** gelegt. Geplant: Play-Variante
(ohne eigenen APK-Updater und ohne Installations-Berechtigung, mit Play In-App Updates), signiertes AAB über den
Workflow, Store-Eintrag (Texte, Screenshots, Feature-Grafik, Icon), Datensicherheit/Altersfreigabe (Familien-Richtlinien),
Lizenzprüfung, Klickliste für die Play Console. Damit Wechsel APK ↔ Play ohne Deinstallieren geht, soll der bestehende
Schlüssel beim Anlegen der App als App-Signaturschlüssel hochgeladen werden – **die App in der Play Console bitte bis
dahin nicht anlegen** (die Wahl des Signaturschlüssels ist danach nicht mehr änderbar).

**Workflow-Kopfzeile:** „Drive, Ordner Schlüssel (geheim)“ durch „Bitwarden“ ersetzt (auch in `kurvenflug/.gitignore`).

---

Neueste Meldung oben. Zweig `kurvenflug-test`, Ordner `kurvenflug/webapp/` (index.html oben). `kurvenflug-live` setzt nur der Leitstand.

---

## 0.19.0 – Oberstufe Klasse 11–13 (Gleichstand mit APK 0.19.0)

- **Version:** 0.19.0 · **Commit:** 021eca8 (Zweig claude/gifted-shannon-657gpr)
- **Neu:** Klassenwahl 5–13; vier neue Kapitel: Ableitung & Tangente, Extrempunkte, Integral (Treibstoff = Fläche),
  Vektoren (X = P + t · v) – je mit Live-Rechnung, Rechen-Stern und Lösungsweg. Aurora-Geschichte (Kl. 5–10) unverändert.
- **Prüfen:** Klasse 12 wählen → „Weiter geht's!“ führt zu „Vektoren · Erster Pfeil“: v = (8 | 6) → Treffer.
  Galaxie → Liste: Kapitel „Integral“ – beim Flug füllt sich die Fläche unter der Kurve.
- Gleichstands-Test (JVM ↔ Wasm) deckt die neuen Kapitel mit ab. Browser: keine Konsolenfehler.

---

## 0.18.0 – Verstehen statt nur Schieben (Gleichstand mit APK 0.18.0)

- **Version:** 0.18.0 · **Commit:** fefa4b4 (Zweig claude/gifted-shannon-657gpr)
- **Neu:** Live-Rechnung unter der Formel, „Rechen-Stern“ (3. Stern nur mit der richtigen Rechnung aus drei), danach
  der Lösungsweg mit den Zahlen des Levels; die Aufgabe bleibt nach einem Fehlversuch sichtbar.
- **Prüfen:** Level „Erster Kontakt“ gewinnen → 2 Sterne + Frage „Wie liest du (4 | 3)?“ → richtige Antwort → 3. Stern,
  „So rechnet man's“. Seite neu laden: 3 Sterne gespeichert.
- **Technik-Hinweis:** Der Bildschirmleser-Spiegel von Compose (unsichtbare Textkopie für Screenreader) ist abgeschaltet –
  er schrieb bei Levelende gelegentlich „IllegalStateException: Node … not found“ in die Konsole (Fehler der Bibliothek,
  auch in 0.17.x). Danach in 10 Durchläufen keine Konsolenfehler.

---

## 0.17.1 – WebApp aktualisiert sich selbst (Gleichstand mit APK 0.17.1)

- **Version:** 0.17.1 (Ladebildschirm, `version.json`, Einstellungen)
- **Commit:** 4e9fdb3 (Zweig claude/gifted-shannon-657gpr)
- **Neu:** kein Update-Knopf, die WebApp aktualisiert sich selbst. Sie sucht beim Öffnen, beim Zurückkehren in den Tab
  und alle 30 Minuten nach einer neuen Version. Ist die Seite gerade erst geöffnet (unter 15 s), wird sofort neu geladen.
  Sonst wird gewechselt, sobald der Tab im Hintergrund war (nie mitten im Level). Der Spielstand bleibt erhalten.
- **Prüfen:** Seite öffnen → „Version 0.17.1“. Wer 0.17.0 schon offen hatte, sieht nach dem nächsten Öffnen 0.17.1, ohne etwas zu tun.
- Selbst geprüft: Wechsel A → B beim Öffnen (sofort) und im laufenden Spiel (erst nach Hintergrund), ohne Konsolenfehler.

---

## 0.17.0 – erste WebApp (Gleichstand mit APK 0.17.0)

- **Version:** 0.17.0 (APK und WebApp gleich; steht im Ladebildschirm, in `version.json` und unter Einstellungen „Version 0.17.0 · WebApp“)
- **Commit:** 3d78727 (Zweig claude/gifted-shannon-657gpr) (Quellcode), WebApp darauf gebaut mit `./gradlew :web:webapp`
- **Größe:** ca. 15 MB, nur erlaubte Dateitypen (html, js, wasm, json, webmanifest, png, svg, ttf, txt), keine Symlinks, kein Server-Code
- **Technik:** eine gemeinsame Codebasis (Kotlin Multiplatform + Compose Multiplatform, Kotlin/Wasm). Ein Gleichstands-Test
  rechnet alle Level, Flugbahnen, Tagesmissionen und Codes auf Android (JVM) und im Browser (Wasm) und vergleicht den Fingerabdruck.
- **Datenschutz:** keine externen Aufrufe (keine CDNs, Schriften liegen bei, kein Tracking). Spielstand nur in `localStorage`
  (Schlüssel `kf.*`), nichts an den Server. Links „Impressum“ und „Datenschutz“ im Ladebildschirm und unter Einstellungen.

### Bitte auf https://kurvenflug-test.mathenachhilfe-online.com prüfen
1. Seite lädt (Ladebildschirm mit Rakete → Startseite mit Volt), unten im Ladebildschirm „Version 0.17.0“.
2. Klasse wählen → Geschichte „Die Reise der Aurora“ → Startseite. Reiter Heute / Galaxie / Spiele / Ich antippen.
3. „Weiter geht's!“ → Level „Erster Kontakt“: x mit + auf 4, y auf 3, „Start“ → 3 Sterne, Sticker.
4. Seite neu laden: Spielstand bleibt (Abzeichen „Abheben“ erscheint).
5. ⚙️ Einstellungen: „Version 0.17.0 · WebApp“, Links Impressum und Datenschutz öffnen die richtigen Seiten.
6. Browser-„Zurück“ (bzw. Esc am PC) führt eine Ebene zurück, auf der Startseite verlässt es die Seite.
7. Installieren: Chrome/Edge „App installieren“ bzw. iPhone „Zum Home-Bildschirm“. Danach Flugmodus → App startet trotzdem (Service Worker).
8. Am PC: App erscheint als Handy-Spalte in der Mitte, Spielfeld und Regler sind ganz sichtbar.

### Hinweise für den Server
- `.wasm` muss als `application/wasm` ausgeliefert werden (sonst lädt die App nicht), `.webmanifest` als `application/manifest+json`.
- `sw.js`, `index.html` und `version.json` am besten ohne langes Caching (`Cache-Control: no-cache`); die `.wasm`-Dateien haben
  Hash-Namen und dürfen lange gecacht werden.
- Läuft in aktuellen Browsern mit WebAssembly-GC: Chrome/Edge ab 119, Firefox ab 120, Safari ab 18.2.

### Selbst geprüft (Chromium, Handy 412×860 und PC 1280×800)
Keine Konsolenfehler. Laden ca. 1 s (lokal). Klassenwahl, Geschichte, alle 4 Reiter, alle 14 Kacheln unter Spiele/Ich
(jeweils hinein und mit Browser-Zurück heraus), Level gewonnen, Neuladen mit erhaltenem Spielstand, offline neu geladen.
