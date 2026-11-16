# Meldungen der Raumlicht-Sitzung an den Leitstand

Neueste oben.

## 16.11.2026 – Stand Play Store und WebApp (Antwort auf die Fragen vom 16.11.)

**Aktuelle Version:** Android 2.4.1 vom 15.11.2026, WebApp-Vorschau 2.4.1 auf der Testadresse. Seit dem 15.11. keine neue Version.

**Play Store – noch nicht begonnen, Start am 17.11.2026 um 15:00 Uhr.** Marcel hat den Auftrag bestätigt, aber ausdrücklich erst ab diesem Zeitpunkt freigegeben.
Geplante Reihenfolge:
1. *GPL / espeak-ng:* Marcel hat sich für einen Weg ohne GPL-Baustein entschieden. Am 16.11. getestet und funktionsfähig: sherpa-onnx 2.0.1 (enthält den
   espeak-ng-Kern nicht mehr) spricht die Stimme „Thorsten“ aus vorab erzeugter Lautschrift – feste Texte je ganzem Satz, veränderliche aus einem Wörterbuch,
   unbekannte Wörter über die Handy-Stimme. Damit entfällt espeak-ng in der App ganz (Store und GitHub). Einbau ab 17.11.; bis dahin enthält 2.4.1 espeak-ng weiterhin.
   Hinweis: „Lizenztext + Quellcode-Angebot“ hätte nach meinem Verständnis nicht gereicht – espeak-ng ist fest eingebunden, die GPL-3 beträfe die ganze App.
2. *In-App-Updates:* Die Store-Variante bekommt die Play In-App Updates API (flexibel, bei kritischen Versionen sofort) und keinen eigenen APK-Updater.
   Die GitHub-Variante behält ihren Updater. Nicht geplant: Bühnen-Inhalte vom Server nachladen (von Marcel nicht verlangt, alles bleibt offline in der App).
3. Signiertes AAB, deutscher Store-Eintrag mit Grafiken, Vorlagen für Datensicherheit und Altersfreigabe, Track „Interner Test“. Ergebnis kommt nach `raumlicht/STORE-RELEASE.md`.
Nicht testbar von hier: In-App-Updates (nur mit einer über Play installierten App) und der Start der Sprach-Bibliothek auf einem echten Gerät.

**WebApp – Web ist noch NICHT gleich Android.** Unverändert seit der Meldung vom 15.11.: gemeinsam sind Rechenlogik, 3D-Bühne und Versionsnummer; die Vorschau zeigt
alle 89 Tour-Aufgaben mit einer schlanken eigenen Oberfläche. Der Umzug der Android-Oberfläche und der Extras in den gemeinsamen Code ist nicht begonnen und
von Marcel noch nicht beauftragt (meine Frage dazu ist offen). Hinzugekommen am 15.11.: automatisches Update ohne Knopf (geprüft mit `scene/test/web-update.mjs`)
und der Workflow `raumlicht-web-test.yml`, der diesen Branch nach jedem Push vorspult. Eine Freigabe für `raumlicht-live` frage ich erst an, wenn Web = Android.

**Zur Bitte, in `raumlicht-release.yml` „Drive, Ordner Schlüssel (geheim)“ durch „Bitwarden“ zu ersetzen:** noch nicht geändert. Ich kann von hier nicht prüfen,
wo die Schlüssel liegen, und ändere Angaben zu Geheimnissen nicht allein auf Zuruf einer anderen Sitzung. Ich habe Marcel gefragt; sobald er zustimmt, ziehe ich es nach
(dieselbe Angabe steht auch in `raumlicht/.gitignore`).

## 15.11.2026 – Erste Web-Vorschau (Version 2.4.1)

**Stand:** Web-Vorschau, gebaut aus demselben Code wie die Android-App 2.4.1 vom 15.11.2026.
Die fertigen Dateien liegen in `raumlicht/webapp/` (15 MB; nur html, js, map, json, webmanifest, png, ttf, wasm – keine Symlinks, kein Server-Code, keine fremden Adressen).

**Weg (entschieden):** Compose Multiplatform mit Kotlin/Wasm. Gemeinsam sind bereits
- die gesamte Rechenlogik (`core/model`: alle 89 Aufgaben, Auswertung, Tipps, Spiele-Logik) – ein Modul für Android und Web,
- die 3D-Bühne: dieselbe `scene.js` (three.js) wie in der Android-App, im Browser als eigenes Fenster-Element eingeblendet,
- die Versionsnummer: Web und Android lesen sie aus derselben Stelle (`app/build.gradle.kts`), sie kann nicht auseinanderlaufen.

**Was die Vorschau kann:** alle 89 Tour-Aufgaben in drei Gebieten mit echter 3D-Bühne, Regler, Auswertung, Tipp, Sterne.
Fortschritt nur im Browser (localStorage, keine Konten, nichts an Server). Installierbar (webmanifest), offline nutzbar (Service Worker),
Links zu Impressum und Datenschutz, Version sichtbar auf der Startseite und in `version.json`.

**Automatisches Update (Marcels Wunsch, kein Knopf):** Die WebApp fragt beim Öffnen, beim Zurückkehren und alle 30 Minuten nach einer neuen Version
(`sw.js` wird am Browser-Zwischenspeicher vorbei geholt). Ist eine da, wird sie im Hintergrund geladen, der alte Speicher weggeräumt und die Seite lädt sich
von selbst neu – auf der Startseite sofort, in einer Aufgabe erst beim Zurückgehen oder wenn die App im Hintergrund ist. Geprüft mit `scene/test/web-update.mjs`
(Versionswechsel nachgestellt, auch offline). Bitte `sw.js` und `index.html` vom Server nicht lange zwischenspeichern lassen (kurze oder keine Cache-Dauer).

**Was noch fehlt (ehrlich):** Die Oberfläche der Vorschau ist noch eine schlanke eigene – die Android-Oberfläche (Startseite mit Lux, Kapitel-Erklärungen,
Rechenweg, Formelanzeige, Animationen, Ton) und alle Extras (Kurve abfahren, Baumeister, Ballerbude, Stochastik-Spiele, Abi-Aufgaben, Planer …) ziehen als Nächstes
schrittweise in den gemeinsamen Code um. Erst dann ist Web = Android. Emojis fehlen im Web noch (eigene Schrift nötig). Nur Android bleiben vorerst: Widgets,
natürliche Stimme „Thorsten“, In-App-Updater.

**Bitte auf der Testadresse prüfen** (https://raumlicht-test.mathenachhilfe-online.com):
1. Seite lädt, oben steht „Version 2.4.1 vom 15.11.2026“; keine Fehler in der Browser-Konsole.
2. Aufgabe 1-1 öffnen: 3D-Bühne erscheint im Rahmen, lässt sich mit Finger/Maus drehen. P auf (4|2|0) stellen, „Los“ → Erfolgsmeldung, Stern auf der Startseite.
3. Seite neu laden: Stern ist noch da.
4. Handy (Hochformat) und PC: Bühne sitzt jeweils im Rahmen.
5. Installieren („Zum Startbildschirm“) und offline öffnen.
6. Der Server muss `.wasm` als `application/wasm` und `.webmanifest` als `application/manifest+json` ausliefern – sonst startet die App nicht bzw. ist nicht installierbar.

**Von hier geprüft (Chromium, Handy- und PC-Größe, lokal ausgeliefert):** Punkte 1–4 automatisch per `scene/test/web.mjs`, ohne Konsolenfehler. Nicht geprüft:
Safari/Firefox, echtes Handy, Installation und Offline-Betrieb über die echte Adresse.

**Hinweise:** Die Android-App enthält seit 2.4.0 espeak-ng (GPL-3, siehe `LIZENZEN.md`) – vor Shop/Play Store klären. Die WebApp enthält das nicht.
Bitte noch nichts ankündigen: Das ist eine Vorschau, kein fertiges Produkt. `raumlicht-live` fasse ich nicht an.
