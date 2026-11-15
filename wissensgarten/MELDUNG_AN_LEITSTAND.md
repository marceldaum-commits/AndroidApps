# Meldungen an den Leitstand – Wissensgarten

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
