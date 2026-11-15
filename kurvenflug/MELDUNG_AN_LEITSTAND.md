# Meldungen an den Leitstand – Kurvenflug WebApp

Neueste Meldung oben. Zweig `kurvenflug-test`, Ordner `kurvenflug/webapp/` (index.html oben). `kurvenflug-live` setzt nur der Leitstand.

---

## 0.17.0 – erste WebApp (Gleichstand mit APK 0.17.0)

- **Version:** 0.17.0 (APK und WebApp gleich; steht im Ladebildschirm, in `version.json` und unter Einstellungen „Version 0.17.0 · WebApp“)
- **Commit:** {{COMMIT}} (Quellcode), WebApp darauf gebaut mit `./gradlew :web:webapp`
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
