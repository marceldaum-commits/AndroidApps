# Meldungen an den Leitstand – Wissensgarten

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
