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

**Hinweis zum Ablageort der Schlüssel:** erledigt am 16.11. (von Marcel bestätigt) – in `raumlicht-release.yml` und `raumlicht/.gitignore` steht jetzt „Bitwarden“ statt Drive.

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


---

## 16.11.2026 – WebApp 2.5.0: jetzt die ganze App (Auftrag von Marcel: „Bau die webapp“)

**Stand:** Die WebApp zeigt jetzt dieselben Bildschirme wie die Android-App – aus demselben Code (Modul `shared`). Drin sind: Startseite mit Lux und Bühne,
alle Kapitel mit Einführung, Rechenweg und Formelanzeige, Tagesaufgabe, Üben, Abi, „Mehr“ mit allen Extras (Kurve abfahren, Flug, Ballerbude, Baumeister,
Geisterfahrt, Ringflug, Zufallsmaschine, Galton-Brett …), Planer, Einstellungen (mit Links zu Impressum und Datenschutz, „Fortschritt zurücksetzen“).
Emojis und Mathe-Zeichen kommen aus mitgelieferten Schriften (DejaVu Sans, Noto Color Emoji – beide frei, siehe `LIZENZEN.md`).

**Technik, die ihr wissen solltet:** `webapp/` ist jetzt rund 27 MB (vorher 15 MB; die Emoji-Schrift hat 10,8 MB, wird wie alles andere vom Service Worker
einmal gespeichert). Dateitypen unverändert (`.wasm`, `.ttf`, `.js`, `.html`, `.json`, `.webmanifest`, `.png`, `.map`). Gespeichert wird nur im Browser
(localStorage, Schlüssel `raumlicht.*`); keine Anfragen an fremde Server, keine Cookies. Sprachausgabe: `speechSynthesis` des Browsers –
je nach Browser/System rechnet die auf dem Gerät oder beim Browser-Hersteller; das steht so auch in den Einstellungen der WebApp. Bitte für die Datenschutzseite bedenken.

**Noch nicht gleich mit Android (ehrlich):** Hintergrundmusik, natürliche Stimme „Thorsten“, Widgets, Erfolgskarte als Bild teilen (im Web wird nur Text geteilt).
Der In-App-Updater entfällt im Web (aktualisiert sich selbst).

**Von hier geprüft (Chromium ohne echte Grafikkarte, Handy- und PC-Größe, lokal ausgeliefert):** `scene/test/web.mjs` – Start, Aufgabe 1-1 mit Einführung öffnen,
Bühne drehen und zoomen, Aufgabe lösen, Sterne gespeichert, Browser-Zurück, „Mehr“, Neuladen; keine Konsolenfehler. `scene/test/web-update.mjs` – automatisches Update und Offline-Start.
**Nicht geprüft:** Safari und Firefox, echtes Handy (Fingerbedienung der Bühne, Zwei-Finger-Zoom), Ton und Stimme (im Test nicht hörbar),
die Extras einzeln im Browser (sie sind derselbe Code wie unter Android und dort getestet, aber im Browser nicht einzeln durchgeklickt).

**Bitte auf der Testadresse prüfen:** 1. „Version 2.5.0 vom 16.11.2026“ in den Einstellungen (Startseite → Mehr → Einstellungen). 2. Aufgabe 1-1 lösen, neu laden, Sterne noch da.
3. Auf einem echten Handy: Bühne mit einem Finger drehen, mit zwei zoomen; Punkt in einer Zieh-Aufgabe ziehen. 4. Safari (iPhone) und Firefox starten überhaupt.
5. Je ein Extra aus „Mehr“ öffnen (Kurve abfahren, Ballerbude, Galton-Brett). 6. Ton/Stimme nach dem ersten Antippen hörbar.

**Freigabe:** `raumlicht-live` beantrage ich noch **nicht** – erst wenn Punkte 3 und 4 auf echten Geräten bestätigt sind. `raumlicht-live` fasse ich nicht an.

---

## 16.11.2026 – WebApp 2.5.1 (Wunsch von Marcel)

Hintergrundmusik läuft jetzt auch in der WebApp (im Browser erzeugt, keine Audiodateien, startet nach der ersten Berührung, abschaltbar).
Das Vorlesen („Lux spricht“) ist in Android und Web ab jetzt **aus**, bis man es in den Einstellungen einschaltet – die Sprachausgabe des Browsers
wird also ohne Zutun gar nicht mehr benutzt. Noch nur Android: natürliche Stimme, Widgets, Erfolgskarte als Bild. Im Test nicht hörbar geprüft (kein Lautsprecher) –
bitte auf der Testadresse einmal anhören. `raumlicht-live` weiterhin nicht beantragt.

---

## 16.11.2026 – Antwort zu Datenschutz-Abschnitt 10e (WebApp 2.5.1)

Der Abschnitt stimmt im Kern. Drei Genauigkeiten, bitte so übernehmen:

1. **Lokaler Speicher:** richtig. Gespeichert wird nur im Browser des Geräts: Spielstand und Einstellungen (localStorage, Schlüssel `raumlicht.*`)
   sowie die App-Dateien selbst für den Offline-Betrieb (Cache des Service Workers `raumlicht-<Version>`). Keine Cookies, kein Konto, keine Analyse.
   „Fortschritt zurücksetzen“ in den Einstellungen löscht den Spielstand; Browserdaten löschen entfernt alles.
2. **Vorlesen nur auf Wunsch:** richtig – seit 2.5.1 ist es aus, bis man „Lux spricht“ einschaltet. **Aber bitte nicht „rein auf dem Gerät“ versprechen:**
   Die WebApp nutzt die Sprachausgabe des Browsers und bevorzugt eine Stimme, die auf dem Gerät rechnet. Hat der Browser nur Online-Stimmen
   (bei Chrome am PC möglich), schickt **der Browser** den vorzulesenden Text (Aufgaben- und Hinweistexte der App, keine Eingaben mit Personenbezug)
   an seinen Hersteller. Vorschlag für den Text: „… über die Sprachausgabe Ihres Browsers bzw. Geräts; je nach Browser kann dieser den vorzulesenden Text
   zur Sprachsynthese an seinen Anbieter übertragen.“
3. **Keine fremden Server:** für die WebApp richtig – sie lädt ausschließlich von der eigenen Adresse (App-Dateien, Schriften, `version.json`, Update-Prüfung über `sw.js`).
   Für die **Android-App** gilt das nicht wörtlich: Sie fragt GitHub nach Updates (abschaltbar) und lädt die natürliche Stimme auf Wunsch einmalig von Hugging Face.
   Falls 10e nur die WebApp meint, bitte das dazuschreiben.

Hinweis: Musik und Klänge entstehen im Browser (keine Audiodateien, kein Abruf). Am echten Gerät habe ich nichts gehört oder angefasst – eure Prüfung am Handy ist die einzige.

---

## 16.11.2026 – Version 2.6.0: bereit für den Play Store (von Marcel auf heute vorgezogen)

1. **espeak-ng ist raus:** sherpa-onnx 2.0.1 (ohne espeak-ng), Hilfsdaten entfernt; die Lautschrift liefert die App als vorab erzeugte Daten mit
   (`tools/stimme/lautschrift.py`, `LIZENZEN.md`). Test deckt alle Sprechtexte ab; unbekannte Wörter liest die Handy-Stimme.
2. **Play-Fassung:** Fassung `play` ohne eigenen Updater und ohne Berechtigung „Apps installieren“, mit Play In-App Updates
   (im Hintergrund, eingespielt beim nächsten Start; dringend ab Priorität 4). Die GitHub-APK behält ihren Updater.
3. **Signiertes AAB** baut der GitHub-Lauf und hängt es an den Release (`Raumlicht-2.6.0-play.aab`); geprüft werden Signatur, fehlende Installations-Berechtigung, keine espeak-Daten.
4. **Store-Eintrag, Datensicherheit, Altersfreigabe, Zielgruppe, Klick-Liste:** `raumlicht/STORE-RELEASE.md` und `raumlicht/store/` (Texte, Icon 512, Grafik 1024×500, acht Bildschirmfotos).
   Wichtig darin: die Schlüssel-Entscheidung bei Play App Signing (sonst verlieren bisherige APK-Nutzer den Spielstand).

Veröffentlicht ist nichts; hochladen muss Marcel selbst. WebApp 2.6.0 ist inhaltlich gleich 2.5.1 (nur neue Versionsnummer).
Nicht geprüft: echtes Gerät, Play In-App Updates (geht erst mit einer über den Store installierten Version). Der Inhaltsausbau folgt als nächste Version.

---

## 16.11.2026 – Version 2.7.0: Inhaltsausbau (Auftrag „sehr viel Inhalt, alles prüfen“, von Marcel bestätigt)

**Neu:** 35 Aufgaben in sechs Kapiteln (18 Ebenen in allen Formen, 19 Schnitte & Schnittwinkel, 20 Spiegeln & Lote, 21 Kugel & Ebene, 22 Integrale & Flächen,
23 Erwartung/Sigma/Testen) und zwei Prüfungsaufgaben im Abi-Stil („Planetarium“, „Einlass“). Gesamt: 124 Aufgaben in 23 Kapiteln, 7 Prüfungsaufgaben.
Jede Aufgabe hat Tipp, Rechenweg, gezielte Hinweise bei typischen Fehlern und eine 3D-Bühne (Analysis im 2D-Koordinatensystem, Stochastik ohne Achsen).

**Prüfung (automatisch, `:core:model:jvmTest`, 84 Tests):** Lösung wird anerkannt, Startwerte lösen nicht, Werte im erlaubten Bereich, Bühne in beiden Phasen gültig,
alles Sichtbare im Raum; zusätzlich `TourSixTest`: jede Lösung unabhängig nachgerechnet (Kreuzprodukte, Abstände, Spiegelpunkte, numerische Integrale,
Binomialverteilung exakt – Hypothesentest P(X ≥ 59) ≈ 4,4 %), jede Zahlen-Aufgabe hat genau eine richtige Antwort. App-Tests (132) grün.
**Web = Android:** derselbe Code; WebApp 2.7.0 gebaut, `scene/test/web.mjs` (Handy- und PC-Größe) und `web-update.mjs` ohne Fehler.
**Angesehen:** alle 35 neuen Bühnen gerendert (vorher/gelöst), drei Darstellungen nachgebessert.
**Nicht geprüft:** echtes Gerät; die neuen Aufgaben im Browser einzeln durchgespielt (sie nutzen dieselben Bausteine wie die alten); fachliche Gegenprüfung durch einen Menschen.
