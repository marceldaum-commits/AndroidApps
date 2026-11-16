# Meldungen an den Leitstand – Wissensgarten

## 16.11.2026 – Server-Einstellung: Komprimierung für die WebApp (nginx)

**Befund (live geprüft):** nginx komprimiert `.wasm` bereits, `wissensgarten.js` (647 KB, gepackt 127 KB) und `.ttf` aber nicht.
Mit der Einstellung unten lädt jedes neue Gerät beim ersten Start ca. 0,6 MB weniger. Die App muss dafür nicht geändert werden.

In den `server { … }`-Block von **wissensgarten.mathenachhilfe-online.com** und **wissensgarten-test.mathenachhilfe-online.com**
(oder einmal in den `http { … }`-Block) eintragen:

```nginx
gzip on;
gzip_comp_level 6;
gzip_min_length 1024;
gzip_vary on;
gzip_types text/javascript application/javascript application/json application/manifest+json application/wasm font/ttf text/css image/svg+xml;
```

`text/html` komprimiert nginx immer, das muss nicht in die Liste. `application/wasm` steht drin, damit die heutige Komprimierung erhalten bleibt.

**Testen** (nach `nginx -t` und `systemctl reload nginx`):
```bash
curl -sI -H "Accept-Encoding: gzip" https://wissensgarten.mathenachhilfe-online.com/wissensgarten.js | grep -i content-encoding
curl -sI -H "Accept-Encoding: gzip" https://wissensgarten.mathenachhilfe-online.com/composeResources/de.wissensgarten.web.res/font/noto_color_emoji.ttf | grep -i content-encoding
```
Beide Befehle sollen `content-encoding: gzip` zeigen.

## 0.22.0 vom 16.11.2026 – Start optimiert (Web + Android)

**1. Kein weißer Bildschirm**
- **Ladebild:** Das Ladebild (Brummi, Name, App-Farben) steht inline in HTML/CSS und ist ab dem ersten Byte ohne JS sichtbar. Erstes Bild nach 0,4 s (Fast 4G) bzw. 1,2 s (Slow 4G).
- **Fortschrittsbalken:** Der Balken ist echt und zeigt Prozent. Ein `fetch`-Wrapper zählt per ReadableStream die Bytes von App-JS, beiden `.wasm` und den Schriften. Die Gesamtgröße schreibt das Build in die Seite.
- **Übergang:** Danach blendet die Seite in 0,5 s in die App über.
- **Ursache des weißen Bildschirms:** Eine zweite CSS-Regel hatte die Ebene des Startbilds wieder heruntergesetzt (z-index 10), daher sah Marcel nur den Ladekreis der App auf Weiß. Beides ist behoben: Das Startbild liegt oben, und auch der Lade-Zwischenschritt der App hat jetzt die Garten-Farben statt Weiß/Lila.

**2. Schneller**
- **Komprimierung geprüft:** Auf dem Live-Server kommen `.wasm` gzip-komprimiert an, `wissensgarten.js` aber **nicht** (647 KB statt 127 KB gzip) und `.ttf` ebenfalls nicht.
  Empfehlung für nginx: `gzip_types text/javascript application/javascript font/ttf application/json;`
- **Emoji-Schrift:** Sie enthält nur die genutzten Emojis, 2,0 statt 6,0 MB (seit 0.21.2). Sie wird per `<link rel="preload">` parallel geladen.
- **Service Worker:** Er speichert beim Installieren alle Dateien (App, wasm, Schriften, 77 Pakete). Angemeldet wird er erst, wenn die App steht, damit er beim ersten Laden nicht um die Leitung konkurriert. Der zweite Start lädt 0 Byte und läuft offline.
- **Code-Splitting:** Kotlin/Wasm (Compose Multiplatform) unterstützt kein Aufteilen des Programmcodes in nachladbare Teile. Die zwei wasm-Dateien (App und Grafik-Engine Skia) werden gebraucht, bevor überhaupt etwas gezeichnet wird. Nachgeladen wird daher nur, was Daten sind (Pakete, Schriften).

**3. Messungen**
- **Aufbau:** Pixel 7 (Playwright). Beide Versionen kommen vom selben lokalen Server mit fester Bandbreite, gzip wie nginx und ETag.
  Grund: Die Drosselung von Chrome erfasste nicht alle Downloads.
- **Profile:** Fast 4G = 9 Mbit/s, 165 ms; Slow 4G = 1,6 Mbit/s, 562 ms.
- **Erster vs. zweiter Start:** Der zweite Start folgt, nachdem der Service Worker fertig gespeichert hat.

| Version | Netz | 1. Start: erstes Bild | 1. Start: bedienbar | 2. Start: erstes Bild | 2. Start: bedienbar |
| --- | --- | --- | --- | --- | --- |
| vorher 0.21.0 | Fast 4G | 0,4 s | 16,9 s | 0,2 s | 4,0 s |
| **nachher 0.22.0** | Fast 4G | 0,4 s | **13,0 s** | 0,2 s | 4,2 s |
| vorher 0.21.0 | Slow 4G | 1,2 s | 67,3 s | 0,6 s | 5,0 s |
| **nachher 0.22.0** | Slow 4G | 1,2 s | **39,8 s** | 0,6 s | 4,4 s |

**Downloads beim 1. Start** (gemessen pro Datei):

| Datei | vorher (live) | nachher |
| --- | --- | --- |
| Emoji-Schrift | 6.055 KB | 2.045 KB |
| wasm (App) | 3.550 KB | 3.187 KB |
| wasm (Skia) | 2.240 KB | 1.968 KB |
| JS | 647 KB (live ohne gzip) | 127 KB (mit gzip) |
| Symbolschrift | 120 KB | 120 KB |
| **Summe** | **ca. 12,3 MB** | **ca. 7,3 MB (−41 %)** |

- **Restzeit:** Etwa 4 s des zweiten Starts sind reine Rechenzeit: Übersetzen des wasm-Codes auf der langsamen Test-Maschine. Auf einem aktuellen Handy ist das weniger.

**4. Android**
- **Startbild:** Android-12-Splash-API (`core-splashscreen`, rückwärtskompatibel), hell in Creme `#FBF6EC` und dunkel in `#15190F`, mit dem Garten-Logo. Danach `postSplashScreenTheme` mit gleichem Fensterhintergrund, also kein weißes Aufblitzen.
- **Nicht auf Gerät geprüft:** Das Startbild konnte ich nicht auf einem echten Gerät ansehen (kein Emulator hier). Kompiliert und Lint sind sauber.

**Tests:** 379 Tests grün, Lint ohne Fehler, Play-Variante kompiliert. Im Browser keine Fehler.
**Bitte prüfen und `wissensgarten-live` auf 0.22.0 schalten.**

## 0.21.2 vom 16.11.2026 – WebApp: erster Start deutlich schneller

Marcel meldete: „Lädt recht lange“. Beim ersten Öffnen wurden ca. 11 MB übertragen. Mehr als die Hälfte davon war die Emoji-Schrift (6 MB),
weil sie alle Flaggen- und Hautfarben-Varianten enthielt.
- Die Schrift enthält jetzt nur die tatsächlich genutzten Emojis (+ Smileys): **2 MB statt 6 MB**. Damit werden beim ersten Start insgesamt ca. 7 statt 11 MB geladen.
  Alle Glyphen aus Paketen und Code sind enthalten; geprüft per Skript, dazu ein Sichttest im Browser.
- Die Schrift wird per `<link rel="preload">` gleich parallel zur App geladen statt erst danach.
- Die 379 Android-Tests sind grün, Lint ohne Fehler.
- Im Browser (Pixel 7) startet die WebApp ohne Fehler, die Emojis sind korrekt dargestellt.

Tipp für den Server (nginx): `gzip_types` um `font/ttf` ergänzen. Das spart bei der Schrift nur ca. 7 %, kostet aber nichts. WASM wird schon gzip-komprimiert.
**Bitte nach Prüfung `wissensgarten-live` auf 0.21.2 schalten.**

## 0.21.1 vom 16.11.2026 – WebApp: Startbild bleibt beim ersten Laden sichtbar

**Problem (von Marcel am Handy gemeldet):** Beim ersten Öffnen über mobiles Netz war nur ein weißer Bildschirm mit lila Ladekreis zu sehen.
Ursache: Die Zeichenfläche der App lag über Brummis Startbild, während einmalig ca. 10 MB geladen werden (App + Emoji-Schrift).
Das ist bei schwachem Netz entsprechend langsam.

**Lösung:**
- Das Startbild liegt jetzt sicher oben und zeigt einen Ladebalken.
- Nach 6 s erscheint ein Hinweis, dass der erste Start einmalig ca. 10 MB lädt, nach 30 s „Netz langsam“, nach 90 s ein Knopf „Nochmal versuchen“.
- Ab dem zweiten Start kommt alles aus dem Zwischenspeicher.

**Getestet:** Mit gedrosseltem 4G-Profil (Pixel 7): Startbild und Hinweis sind sichtbar, danach erscheint die App, ohne Fehler.
Nur die WebApp hat sich geändert; die APK hat aus Gründen des Gleichstands die gleiche Versionsnummer.
**Bitte `wissensgarten-live` auf 0.21.1 schalten.**

## 0.21.0 vom 16.11.2026 – Inhalte stark erweitert (30 neue Pakete)

**Umfang:** 30 neue Wissenspakete mit 1.046 neuen Karten. Jetzt sind es **77 Pakete mit 2.653 Karten** (vorher 47 Pakete).
Jedes neue Paket hat 4–5 Erklär-Szenen (Brummi), zu jeder Karte 3 Ablenker, eine Erklärung und ein Emoji-Merkbild.
- **Grundschule (10):**
  - Mathe/Deutsch: Rechnen bis 100, Zahlen bis 1000, Schriftlich rechnen, Formen und Körper, Satzzeichen und Sätze
  - Englisch und Sachkunde: Farben/Zahlen/Tiere, Familie/Körper/Kleidung, Pflanzen und Wald, Gesund und fit, Ritter und Burgen
- **Klasse 5–10 (19):**
  - Mathe: Kopfrechnen-Tricks, Negative Zahlen, Gleichungen, Zufall und Statistik, Pythagoras und Trigonometrie
  - Sprachen: English Tenses, Englisch im Alltag, Komma & Co., Fremdwörter
  - Naturwissenschaften: Pflanzen, Wirbeltiere, Organe, Strom und Magnetismus, Säuren/Basen/Salze
  - Gesellschaft: Steinzeit und Ägypten, DDR und Deutsche Einheit, Klima und Naturgefahren, Flüsse/Berge/Meere, Weltreligionen (erstes Ethik-Paket)
- **Für alle (1):** Redewendungen.

**Fachliche Prüfung:** Jedes Paket hat ein Autor geschrieben und ein zweiter, unabhängiger Prüfer Karte für Karte gegengelesen.
Der Prüfer hat Rechnungen nachgerechnet und geprüft, dass kein Ablenker ebenfalls richtig ist. Dabei gab es 16 Korrekturen, zum Beispiel:
- vertretbare Ablenker ersetzt (DDR war doch UN-Mitglied; „We visit …“ im amerikanischen Englisch)
- umstrittene Aussagen entschärft (Bergfried als „letzte Zuflucht“)
- eine ungenaue Regel („vor dass immer Komma“)

Zusätzlich habe ich 20 Karten stichprobenartig selbst geprüft: ohne Fehler.
Umstrittene Rekorde formulieren die Karten vorsichtig, etwa Nil/Amazonas als „streiten um den Titel“.

**Tests:**
- **Pakete:** Neues Prüfskript `content/tools/check_pack.py` (Format, ids, Versionen, Emoji nur bis Version 11, max. 1 MB) – 77/77 in Ordnung.
  `PackValidationTest` prüft jetzt auch die 1-MB-Grenze und läuft ab jetzt im Release-Workflow mit; vorher lief er dort nicht und war rot (Intro mit 6 Szenen, 🧃 in gs-geld → behoben, gs-geld v2).
- **Android:** 379 Unit-/Robolectric-Tests grün, Lint ohne Fehler, Bildschirmfotos angesehen (Samenhandlung: „77 Wissenspakete mit 2653 Karten“).
- **Gleichstand Web = Android:** `diff` zwischen App-Paketen und `webapp/packs` ohne Unterschied; `versions.json`/`index.json` mit 77 Einträgen.
- **Browser:** Handy (Pixel 7) und PC (1280×800): Start, alle 77 Paketdateien abrufbar, keine Konsolenfehler, Onboarding klickbar.
  Die Emoji-Schrift der WebApp ist neu erzeugt; keine fehlende Glyphe in den Paketen.
- **Regression:** Bestehende Pakete unverändert (außer gs-geld v2: Merkbild 🧃 → 🥤); der Lernfortschritt bleibt erhalten.

**Android-Apps:** Bekommen die Pakete mit 0.21.0 und ohne App-Update über das Nachladen, sobald `wissensgarten-live` 0.21.0 trägt.
**Bitte prüfen und `wissensgarten-live` schalten.**

## 16.11.2026 – Schlüssel-Ablage: Bitwarden statt Drive

Im Wissensgarten stehen jetzt nur noch Bitwarden und die Repo-Secrets als Ablage. Geändert sind die Kopfzeile von `wissensgarten-release.yml`,
`wissensgarten/README.md` (Abschnitt „Signieren“) und der Kommentar in `wissensgarten/.gitignore`.
`kurvenflug-release.yml` und `raumlicht-release.yml` haben wir **nicht** geändert: Laut unserer Vorgabe dürfen wir KurvenFlug und
Raumlicht nicht anfassen. Dort steht in Zeile 7 noch „Drive, Ordner „Schlüssel (geheim)““ – bitte an die zuständigen Sitzungen geben.

## 0.20.0 vom 16.11.2026 – Updates ohne Store-Besuch + Wissenspakete nachladen

1. **Play In-App Updates:** Standard ist „Flexible“. Die neue Version lädt im Hintergrund und wird eingespielt, sobald die App
   in den Hintergrund geht oder beim nächsten Öffnen. Eine Lernrunde wird also nie unterbrochen.
   „Immediate“ gilt nur für kritische Versionen: In der Play Console beim Release die **In-App-Update-Priorität 4 oder 5** setzen
   (über die Play Developer API, Feld `inAppUpdatePriority`). Ein abgebrochenes Sofort-Update wird beim nächsten Start fortgesetzt.
2. **Inhalte ohne Store-Update:** Die App lädt höchstens alle 20 h neue oder verbesserte Wissenspakete. Das sind **nur JSON-Daten**
   (Fragen, Texte, Emoji-Merkbilder) – kein dex/so/jar und kein Code.
   - **Quelle:** die freigegebene WebApp-Fassung auf GitHub, Branch `wissensgarten-live`, Pfad `wissensgarten/webapp/packs/`
     (`versions.json` + Paketdateien), abgerufen über `raw.githubusercontent.com`. Was der Leitstand live schaltet, bekommen also auch die Apps.
     Solange `wissensgarten-live` nicht existiert, wird nichts nachgeladen.
   - **Prüfung:** Dateiname, Paket-id und Version müssen passen, Größe höchstens 1 MB. Geladen wird nur, was neuer ist; eine
     ältere Fassung verdrängt nie ein mitgeliefertes Paket. Der Fortschritt in den Beeten bleibt erhalten.
   - **Abschaltbar:** Einstellungen (hinter dem Eltern-Tor) → „Neue Wissenspakete“.
3. **Datenschutz, Abschnitt „Updates“ – bitte in 10c übernehmen:**

   > App-Updates: In der Play-Store-Version kommen neue Versionen über Google Play – die App lädt sie im Hintergrund und spielt sie ein,
   > ohne dass der Store geöffnet werden muss. In der GitHub-Version fragt die App bei GitHub nach, ob es eine neue Version gibt.
   > Neue Wissenspakete: Höchstens einmal am Tag schaut die App bei GitHub (raw.githubusercontent.com, betrieben von GitHub Inc.) nach neuen oder
   > verbesserten Wissenspaketen und lädt sie herunter. Das sind nur Inhalte (Fragen, Texte, Merkbilder) – kein Programmcode.
   > Dabei wird nichts über euch oder euer Kind gesendet; GitHub sieht nur, wie bei jedem Webseitenaufruf, die IP-Adresse und den Zeitpunkt.
   > Das Nachladen der Pakete und (in der GitHub-Version) die Update-Suche lassen sich in den Einstellungen abschalten.

   In App und WebApp ist der Text schon so drin, ebenso in der Vorlage `wissensgarten/web/datenschutz/index.html`.
   **Data Safety (Play):** keine Änderung nötig. Es werden keine Nutzerdaten erhoben; die IP-Adresse beim Abruf ist ein normaler Netzwerkzugriff.
4. **Qualität:** 379 Tests grün (davon 4 neue zum Nachladen), Lint ohne Fehler. Die WebApp 0.20.0 ist exportiert und lädt fehlerfrei.
   Die WebApp selbst lädt nichts nach, sie bringt ihre Pakete mit.

## 16.11.2026 – Play Store: Wissensgarten ist store-fertig (Version 0.19.0)

1. **Signiertes AAB:** `Wissensgarten-0.19.0-PlayStore.aab` hängt am GitHub-Release 0.19.0, signiert mit dem Upload-Schlüssel
   (GitHub-Secrets; Original nur in Bitwarden). Play-Variante: kein eigener Updater, Updates über Play; Kamera-Safari aus (Familien-Richtlinie).
2. **Store-Eintrag (Deutsch):** `wissensgarten/store/` – Titel, Kurz- und Langbeschreibung (`texte/`), 8 Handy- und 8 Tablet-Screenshots,
   Titelbild 1024×500, Icon 512.
3. **Datensicherheit und Altersfreigabe:** fertige Antworten in `wissensgarten/store/PLAY-STORE.md` (Abschnitte 4 und 5) –
   keine Werbung, kein Tracking, Zielgruppe Kinder + Eltern (Familienprogramm), Eltern-Tor für Einstellungen/Teilen/Sync, erwartete Einstufung USK 0.
4. **Datenschutz-URL:** `https://www.mathenachhilfe-online.com/datenschutz/` (so schon in App und WebApp verlinkt). Vorschlag: keine
   eigene Seite, sondern ein Abschnitt „Wissensgarten (App und WebApp)“ in der Haupt-Datenschutzerklärung; Vorlage: `wissensgarten/web/datenschutz/index.html`.
5. **Ablauf:** zuerst Track „Interner Test“. Marcel hat ein Organisationskonto beantragt – dann gilt die 12-Tester-/14-Tage-Regel nicht;
   falls die Play Console sie doch verlangt, steht der geschlossene Test als Alternative in der Klick-Liste.
   **Klick-Liste für Marcel:** `wissensgarten/store/PLAY-STORE.md`, Abschnitt „Schritt für Schritt“.

## 0.19.0 vom 16.11.2026 – erste WebApp-Version (Test)

**Stand:** Android-App und WebApp entstehen jetzt aus derselben Codebasis (Compose Multiplatform, Web: Kotlin/Wasm) und tragen
dieselbe Versionsnummer. Die fertige WebApp liegt in `wissensgarten/webapp/` (index.html oben), Größe ca. 23 MB, nur erlaubte
Dateitypen (html, js, json, webmanifest, png, ttf, wasm). Der Branch `wissensgarten-test` wird vom Workflow
„Wissensgarten WebApp → Test“ automatisch vorgespult, nachdem Vollständigkeit, Dateitypen, Größe und Gleichstand
(Version + Wissenspakete identisch zur Android-App) geprüft sind. `wissensgarten-live` fasse ich nicht an.

**Bitte auf https://wissensgarten-test.mathenachhilfe-online.com prüfen:**
1. Start: Brummi-Ladebild, dann „Wissensgarten – Los geht's“ (Handy und PC). Version 0.19.0 steht im Ladebild und unter Mehr → Über.
2. Einrichtung: Name eingeben, Klasse wählen, Beete anlegen (47 Pakete), Anleitung überspringen → Garten.
3. Samen säen / Gießen: Lernrunde mit Spielen, Emojis/Merkbilder werden angezeigt, Ton nach dem ersten Antippen.
4. Seite neu laden: Garten und Tautropfen sind noch da (Speicher nur im Browser, kein Server, kein Konto).
5. Installieren („Zum Startbildschirm hinzufügen“) und offline öffnen.
6. Links „Impressum“ und „Datenschutz“ (Startseite der Einrichtung und Mehr → Über) führen auf mathenachhilfe-online.com.
7. Updates kommen automatisch ohne Knopf: beim Öffnen sofort, sonst beim nächsten Verlassen der Seite.

**Bewusst nur in der Android-App:** Widget, tägliche Erinnerung, Geräte-Sync über Google Drive, Wissens-Safari (Kamera),
QR-Scanner (Klassen-Code kann im Web eingefügt werden), Postkarte und Urkunde. Bastelbogen öffnet im Web eine Druckseite.

**Für die Website (Leitstand):** Datenschutz-Seite um die WebApp ergänzen: Lernstand liegt im localStorage des Browsers,
keine Cookies, keine Analyse, keine Server-Übertragung; Vorlesen/Spracherkennung nutzen die Funktionen des Browsers
(bei Chrome kann die Spracherkennung über Google-Server laufen – nur wenn das Kind das Mikrofon im Radio/Sprech-Spiel nutzt).
