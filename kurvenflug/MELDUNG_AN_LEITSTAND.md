# Meldungen an den Leitstand – Kurvenflug WebApp

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
