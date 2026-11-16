# Meldung Raum „Kopfrechnen“ (Rechenblitz) → Leitstand

Neueste Meldung oben.

> **AKTUELLER STAND (16.11.2026, nachmittags) – für den Leitstand:** Live ist 3.15.0 (7c423d5). Auf `rechenblitz-test` liegen **3.16.0** (Inhalt Klasse 5–10, Meldung 47) und **3.17.0** (schneller Start ohne weißen Bildschirm, Meldung 48). Bitte prüfen und freigeben. Inhalte laufen weiter in Hülle 2. Die neue APK (Hülle 3, dunkelblaues Startbild) ist optional.

## 16.11.2026 (48) – Schneller Start ohne weißen Bildschirm, Skripte gebündelt (3.17.0) · gemessen vorher/nachher

- **Auftrag (Leitstand 16.11., Marcel):** „Den Start optimieren, vielleicht mit Ladebalken oder schneller machen. Weißer Screen ist nicht gut.“
- **Ursache vorher:**
  - Das CSS (73 KB) blockierte jede Anzeige. Bis es da war, blieb der Bildschirm leer bzw. weiß, bei „Slow 4G“ etwa 1,3 s.
  - Danach liefen 20 Skripte nacheinander, darunter 300 KB Übersetzungen auch für Deutschsprachige.
  - Der Service Worker fragte bei jedem Start zuerst das Netz (bis zu 3 s Wartezeit bei schlechtem Netz).
  - Android: Startbild und WebView-Hintergrund waren hell (#f7f9fd), im Dunkelmodus blitzte es hell auf.
- **Umgesetzt (Web-App und APK-Inhalt):**
  - **Startbild direkt im HTML** (inline CSS, kein Warten auf Dateien): dunkelblau (#101b43) mit ⚡, „Rechenblitz“, Untertitel in DE/EN/UK und **Ladebalken**. Der Balken wächst mit jedem geladenen Skript und blendet weich aus, sobald die App bedienbar ist. Sicherheitsnetz: Nach spätestens 12 s verschwindet das Startbild immer.
  - **Paralleles Laden:** alle Skripte mit `defer` im Kopf, CSS nicht mehr blockierend (das Startbild deckt ab), Schrift Inter per `preload`.
  - **Übersetzungen nur bei Bedarf:** `lang-en.js`/`lang-uk.js` werden nur für Englisch/Ukrainisch geladen, gleich zu Beginn parallel vorgeladen. Deutschsprachige sparen ca. 300 KB.
  - **Zweiter Start sofort:** Der Service Worker liefert die App-Dateien der aktuellen Version direkt aus dem Speicher (Cache-first). Neue Versionen kommen weiter von selbst: neuer Service Worker → die App wartet, bis er aktiv ist → lädt auf der Startseite neu. Die Versionsprüfung (`?u=…`) geht immer ans Netz.
- **Android (Hülle 3, optional):**
  - Startbildschirm in Dunkelblau mit App-Symbol, Fenster- und WebView-Hintergrund ebenfalls dunkelblau (`rb_start`, `capacitor.config` `backgroundColor`). Startbild → WebView → HTML-Startbild → App ohne weißes Aufblitzen.
  - **Neu: `MIN_SHELL`** (Mindest-Hülle der Inhalte) getrennt von `SHELL` (Hülle der APK). Der 3.17.0-Inhalt braucht weiter nur Hülle 2: Alle bisherigen Apps laden ihn von selbst, die neue APK ist **optional**, nur fürs dunkelblaue Startbild. Regel 9 der Roadmap ist angepasst.
- **Messung** mit Playwright, Handy 390 px, lokaler Server wie Live (gzip, ETag, no-cache). „Slow 4G“ ist im Server nachgebildet (562 ms Latenz, ca. 180 KB/s), damit es auch für den Service Worker gilt. Angaben: erstes sichtbares Bild / bedienbar:

  | | vorher (3.16.0) | nachher (3.17.0, gebündelt) |
  |---|---|---|
  | Slow 4G, 1. Start (DE) | 1,26 s / 3,23 s | **0,73 s** (Startbild mit Balken) / **1,94 s** |
  | Slow 4G, 2. Start (DE) | 1,18 s / 2,91 s | **0,05 s / 0,06 s** |
  | Slow 4G, 1. Start (EN) | 1,26 s / 3,23 s | **0,72 s / 1,97 s** |
  | Slow 4G, 2. Start (EN) | 1,18 s / 2,87 s | **0,06 s / 0,07 s** |
  | WLAN, 1. Start (DE) | 0,10 s / 0,20 s | 0,04 s / 0,15 s |
  | WLAN, 2. Start (DE) | 0,08 s / 0,11 s | 0,02 s / 0,08 s |

  - Hell- und Dunkelmodus liefern dieselben Werte (±40 ms).
  - Weiße Leerbilder nach dem Seitenaufbau: vorher wie nachher keine. Vorher war aber bis 1,26 s gar nichts zu sehen; nachher ist ab 0,73 s das dunkelblaue Startbild da.
  - Die Zeit bis zum ersten Byte (Latenz) lässt sich in der Web-App nicht vermeiden. Danach ist sofort das Startbild sichtbar.
- **Bündeln und Verkleinern (Marcel: „wieso machst du das nicht?“, also gleich mit umgesetzt):**
  - `android-app/bundle.js` (esbuild 0.24.2, fest eingestellt) fasst die 17 Skripte zu **`js/rechenblitz.min.js`** zusammen (gzip 119 statt 142 KB, **1 statt 17 Anfragen**) und verkleinert das CSS zu **`app.min.css`**.
  - Erzeugt wird beides automatisch mit `node android-app/stand.js`. `stand.js --check` (GitHub-Abläufe, `build.sh`) bricht ab, wenn das Bündel nicht zu den Quellen passt.
  - Die lesbaren Quelldateien bleiben im Repo (`js/*.js`, `app.css`). `config.js` bleibt eigene Datei (vom Leitstand bearbeitbar), die Übersetzungen werden weiter bei Bedarf geladen.
  - Namen auf oberster Ebene bleiben erhalten (klassisches Skript, kein Modul), alles verhält sich wie vorher.
  - Ein Ergebnis: Der erste Start bei Slow 4G ist von 3,1 s (ungebündelt) auf **1,9 s** gesunken.
- **Gefunden und behoben beim Testen:** Zwei Texte werden in `init()` direkt übersetzt („Version“, Standardname „Ich“). Das Wörterbuch wird deshalb vor `init()` geladen. Die Sprache wird erst umgeschaltet, wenn das Wörterbuch da ist.
- **Tests:**
  - komplette Regression: 320 px, offline (neuer Service Worker), APK-Modus, Merk, Reise, Welt, Abzeichen, Widget, Garten, Elternabfrage, Profile, Selbst-Update App/Web, Urkunde, Zufallsaktionen; keine JS-Fehler, keine fehlenden Übersetzungen;
  - APK (Hülle 3) gebaut.
  - Der alte Test zum Update-Knopf der Web-App ist entfallen, weil es den Knopf seit 3.13.2 bewusst nicht mehr gibt.

## 16.11.2026 (47) – Viel Inhalt für Klasse 5–10 (3.16.0) · Fehler-Fix für Live · keine neue APK nötig

- **Auftrag (Leitstand 16.11., Marcel):** „Erweitere alle bisherigen WebApps um sehr viel Inhalt … und lass alles prüfen.“ Schwerpunkt Klasse 5–10.
- **8 neue Themen** mit je 5 Stufen, jede Aufgabe mit Lösungsweg:

  | Thema | Stufen | Klassen |
  |---|---|---|
  | **3x Terme** | Termwert · gleichartige Glieder · Klammern ausmultiplizieren · Termwert mit x² und negativen Zahlen · binomische Formeln | 7–9 |
  | **f(x) Funktionen** | Funktionswert · y-Achsenabschnitt · Steigung aus zwei Punkten · Nullstelle · Schnittpunkt zweier Geraden | 8–10 |
  | **10ⁿ Zehnerpotenzen** | ausrechnen · Zahl mal 10ⁿ · Kommazahl mal 10ⁿ · Exponent gesucht · negativer Exponent | 9–10 |
  | **⇄ Dreisatz** | Preise · Verbrauch · antiproportional (Pumpen) · Preise mit Komma · antiproportional (Vorräte) | 5–8 |
  | **🏦 Zinsen** | Jahreszinsen · Zinssatz · Kapital · Monatszinsen · Zinseszins | 7–10 |
  | **🎲 Wahrscheinlichkeit** | Würfel/Münze · Kugeln ziehen · Gegenereignis, %, Erwartung · zweistufig mit Zurücklegen · ohne Zurücklegen, mindestens einmal | 6–10 |
  | **Ø Statistik** | Mittelwert · Median (ungerade) · Spannweite · Median (gerade) · fehlender Wert für einen Durchschnitt | 5–9 |
  | **π Kreis** | Umfang aus d · Umfang aus r · Kreisfläche · Fläche aus d · Zylindervolumen (π ≈ 3,14) | 8–10 |

  Sie sind in den Klassenstufen eingetragen und erscheinen daher auf der Startseite (Klassenmodus), in der Rechen-Reise und im Einstufungstest. Brüche, Prozent, Gleichungen, Potenzen/Wurzeln, Einheiten und Runden gab es schon (je 5 Stufen).
- **15 weitere Textaufgaben für Klasse 5–10:**
  - Teppichpreis, Bruchteil vom Ersparten, Zugfahrt, „3/4 kommen zu Fuß“, Holzwürfel in Kiste;
  - Rabatt rückwärts, Prozent Haustiere, Mischungsverhältnis, Preissteigerung in %, Schwimmbecken-Volumen;
  - Handytarif (lineare Gleichung), Leiter (Pythagoras), Bakterienwachstum, Wertverlust Auto, „Ich denke mir eine Zahl“.
  - Insgesamt gibt es damit 50 neue Textaufgaben seit 3.15.0.
- **Fehler-Fix (betrifft Live 3.15.0):**
  - Die Textaufgabe „Handy kostet … Preis steigt um 5 %“ konnte eine halbe Zahl als Lösung haben (z. B. 472,5 €), obwohl eine ganze Zahl verlangt war. Die Eingabe „472,5“ wurde dann nicht als richtig erkannt.
  - Behoben, ebenso zwei ähnliche Fälle in neuen Aufgaben (Glücksrad mit 8 Feldern, Median bei gerader Anzahl).
- **Prüfung verschärft:** Der Engine-Test erkennt jetzt auch halbe Zahlen bei Ganzzahl-Lösungen und Dezimalpunkte statt Komma in Aufgaben und Lösungswegen.
- **Übersetzung:**
  - Formeln stehen jetzt ohne Wörter da (z. B. „3x + 4 (x = 5)“, „f(x) = 2x + 5; g(x) = …“). Die Frage steht als Hinweis darunter, damit die Übersetzung nicht für jede Formel einen eigenen Eintrag braucht.
  - Etwa 200 neue Einträge, insgesamt 1938. Bei Sätzen mit anderer Wortstellung stehen nummerierte Platzhalter.
- **Tests:**
  - Engine: 435000 Aufgaben (alle 30 Themen × 5 Stufen) ohne Fehler, mit den neuen Prüfungen. Leiter-Aufgabe gegen Pythagoras geprüft (13591 Fälle, 0 Abweichungen).
  - Übersetzungs-Durchlauf über alle Generatoren: 0 Lücken (EN/UK).
  - Browser DE/EN/UK bei 320 px: jede neue Stufe gespielt (richtig und falsch, Lösungsweg). Keine JS-Fehler, keine fehlenden Übersetzungen, kein Überlauf. Die Steigungsaufgabe ragte auf Ukrainisch hinaus und ist jetzt umgestellt. „Zinsen“ hieß auf Ukrainisch wie „Prozent“ und heißt jetzt „Банківські відсотки“.
  - Regression: offline, APK-Modus, Reise, Welt, Widget, Garten, Elternabfrage, Zufallsaktionen.
- **APK:** nur Web-Dateien geändert, Hülle bleibt 2. Test- und Live-Apps holen 3.16.0 nach der Freigabe von selbst.

## 16.11.2026 (46) – Viele neue Aufgaben: 5 neue Themen + 30 neue Textaufgaben (3.15.0) · keine neue APK nötig

- **Marcel 16.11.:** „Bitte sehr erweitern mit vielen neuen Aufgaben.“
- **5 neue Themen** mit je 5 Stufen und Lösungsweg, Ideen aus dem Ideen-Speicher der Roadmap:

  | Thema | Stufen (kurz) | Klassen |
  |---|---|---|
  | **×2 Verdoppeln** | bis 20 · bis 100 · bis 1000 · Halbieren mit Komma · Vierfach/Viertel/dreimal verdoppeln | 1–6 |
  | **€ Geld** | Cent ± · Euro↔Cent, Rückgeld · Euro-Beträge ± · Rückgeld/Mal mit Komma · Preisvergleich (Dreisatz) | 1–6 |
  | **🕐 Uhr & Zeit** | Viertelstunden/Wochen · Minuten zwischen Uhrzeiten · über die volle Stunde, Filmende · Kalender/Sekunden · Kommastunden, Tempo, Laufzeiten | 2–6 |
  | **📐 Geometrie** | Umfang · Fläche · Winkel im Dreieck, Seite aus Fläche · Dreieck, Quader, Viereck, Würfel · Pythagoras, n-Eck, Trapez, Volumen | 3–9 |
  | **🔢 Zahlenfolgen** | ± kleine Schritte · größere Schritte, Verdoppeln · abwechselnd, wachsende Abstände, ·3 · Quadratzahlen, Halbieren, Fibonacci, Lücke · negative Zahlen, knifflige Regeln | 1–10 |

- Die Themen sind in den Klassenstufen eingetragen. Damit tauchen sie automatisch auf der Startseite (Klassenmodus), in der Rechen-Reise, im Einstufungstest und in der Tageschallenge auf (Grundschule: Verdoppeln/Geld/Zeit/Folgen; ab Klasse 5: Geld/Zeit/Geometrie/Folgen).
- **30 neue Textaufgaben** (je Stufe 6), etwa die Hälfte aller Textaufgaben kommt jetzt aus dem neuen Vorrat:
  - Vögel im Baum, Perlen, Kekse, Stall, Hasenbeine;
  - Sitzreihen, Taschengeld, Boote (Rest aufrunden!), Kino, Einkauf;
  - Rezept, Laufrunden, Kuchen-Bruch, Saftgläser, Durchschnitt, Blumenbeet;
  - Preiserhöhung, Fahrradzeit, Maler-Dreisatz, Zinsen, Maßstab, Durchschnittstempo;
  - Klassenausflug, „3-mal so viele“, Becken, Rechteck mit Umfang, Altersrätsel, Bruchteil der Klasse.
- **Geldbeträge** werden als Lösung mit zwei Nachkommastellen gezeigt (4,80 € statt 4,8 €). Eingaben wie „4,8“ zählen weiterhin als richtig.
- **Übersetzungen:** etwa 220 neue Einträge (EN/UK), insgesamt 1738. Bei Sätzen mit anderer Wortstellung stehen nummerierte Platzhalter, damit Zahlen nicht vertauscht werden.
- **Tests:**
  - Engine: 315000 erzeugte Aufgaben (alle Themen × Stufen); jede Lösung lässt sich eintippen, keine ungültigen Werte, keine negativen Ergebnisse bei Geld/Zeit/Geometrie.
  - Übersetzungs-Durchlauf über alle Generatoren ohne Lücken.
  - Browser DE/EN/UK, 320 px: jede neue Stufe gespielt (richtig und falsch, Lösungsweg). Keine JS-Fehler, keine fehlenden Übersetzungen, kein Überlauf. Regression (offline, APK-Modus, Reise, Welt, Widget, Garten, Elternabfrage, Zufallsaktionen) ohne Fehler.
- **Hinweis Tageschallenge:** Die Themenliste der Challenge ist erweitert. Am Tag der Live-Freigabe können die Challenge-Aufgaben vor und nach dem Update verschieden sein. Ab dem Folgetag sind sie wieder für alle gleich.

## 16.11.2026 (45) – Antwort auf Leitstand zu (43): eigener Upload-Schlüssel, Play-Ablauf bricht bei fehlenden/vertauschten Geheimnissen klar ab

- **Leitstand 16.11.:**
  - Rechenblitz bekommt einen **eigenen** Upload-Schlüssel, getrennt von `WG_KEYSTORE` (Wissensgarten/KurvenFlug/Raumlicht).
  - Marcel erzeugt ihn selbst und trägt die 4 Geheimnisse ein.
  - Datenschutz 10b ist mit beiden Änderungen eingebaut. Danke!
- **`rechenblitz-play.yml` geändert:** kein stilles Überspringen mehr. Der Lauf bricht mit einer klaren `::error`-Meldung ab, wenn
  - ein Geheimnis **fehlt** (nennt jedes fehlende `RECHENBLITZ_UPLOAD_*`),
  - der Keystore **kein gültiges Base64** ist oder leer,
  - das **Store-Passwort** falsch ist (Hinweis: evtl. mit dem Schlüssel-Passwort vertauscht),
  - der **Alias** nicht im Keystore steht (nennt die vorhandenen Aliasse),
  - das **Schlüssel-Passwort** nicht passt (Probe-Signatur; Hinweis: evtl. vertauscht).
- Bei Erfolg zeigt das Protokoll Alias und SHA-256 des Upload-Zertifikats, zum Abgleich mit der Play Console.
- **Lokal getestet** mit einem Wegwerf-Schlüssel (anschließend gelöscht): fehlt / Base64 kaputt / Store-Passwort falsch / Alias falsch / Schlüssel-Passwort falsch / vertauscht / korrekt. Alle sieben Fälle verhalten sich wie erwartet.
- **Hinweis:** Bis Marcel die Geheimnisse eingetragen hat, ist jeder Lauf „Rechenblitz Play“ (bei Push auf `rechenblitz-live`) **rot** und meldet „Geheimnis fehlt“. Die Live-Freigabe der Web-App betrifft das nicht.
- **`PLAYSTORE_RECHENBLITZ.md`:** Schlüsselfrage als geklärt eingetragen. Alle vier Geheimnisse sind Pflicht (Schlüssel-Passwort meist gleich dem Keystore-Passwort).

## 16.11.2026 (44) – Marcels Antworten zur Play-Veröffentlichung · Elternabfrage (3.14.5) · OFFEN an Leitstand: Upload-Schlüssel

- **Marcel 16.11.:**
  - **Firmenkonto:** Die 12-Tester-/14-Tage-Pflicht gilt nicht. Nach dem internen Test kann direkt Produktionszugriff beantragt werden.
  - **Upload-Schlüssel:** „klär mit den anderen“, siehe OFFEN unten.
  - **Elternabfrage vor Links:** ja.
- **Elternabfrage (nur Android-App, `js/elterntor.js`):**
  - Vor jedem Link, der die App verlässt (Logo, Menü-Links, Probestunde, Stundenpakete, Lernraum, Gutschein- und Verlosungs-Links), erscheint „🔒 Nur für Erwachsene“: eine ausgeschriebene Zahl (z. B. „dreihundertvierundsechzig“ / „three hundred and sixty-four“ / „триста шістдесят чотири“) in Ziffern eintippen.
  - Bei einem Fehler kommt eine neue Zahl und der Hinweis „bitte einen Erwachsenen fragen“. Einmal bestanden, gilt die Abfrage 10 Minuten.
  - Impressum und Datenschutz bleiben frei.
  - Die Web-App im Browser ist unverändert, ohne Abfrage.
  - Getestet in DE/EN/UK, 320 px, falsch/richtig/10-Minuten-Fenster/Impressum, Web ohne Abfrage. Keine JS-Fehler, keine fehlenden Übersetzungen.
- **OFFEN an Leitstand (Upload-Schlüssel):**
  - Marcel bittet, mit den anderen Räumen zu klären, ob es schon einen Upload-Schlüssel gibt (Mathe-Trainer?) und welcher für Rechenblitz gilt.
  - Empfehlung: ein eigener Upload-Schlüssel je App (Play App Signing; Schaden bei Verlust begrenzt).
  - Für Rechenblitz braucht es im Repo School vier GitHub-Geheimnisse: `RECHENBLITZ_UPLOAD_KEYSTORE` (Base64), `RECHENBLITZ_UPLOAD_STORE_PASSWORD`, `RECHENBLITZ_UPLOAD_KEY_ALIAS`, `RECHENBLITZ_UPLOAD_KEY_PASSWORD`. Danach „Rechenblitz Play“ auf `rechenblitz-live` starten, das AAB liegt dann unter Artifacts.
  - Gibt es noch keinen Schlüssel, entsteht er beim ersten Hochladen (Anleitung in `PLAYSTORE_RECHENBLITZ.md`).

## 16.11.2026 (43) – Play-Fassung nach Leitstand-Korrektur: In-App-Updates von Google Play (3.14.4) · Datenschutz 10b: Textvorschlag

- **Leitstand 16.11. (Korrektur):**
  - Der Datei-Update bleibt.
  - APK-Download und Installation fallen weg.
  - Neue native Versionen kommen über die Play In-App Updates API.
  - Geladene Dateien ändern das Verhalten nicht grundlegend.
- **Stand:**
  1. **Datei-Update bleibt:** HTML/JS/CSS/JSON über `raw.githubusercontent.com` (Branch `rechenblitz-live`), Ersatz eigener Server. Schon so umgesetzt in (42).
  2. **APK-Download raus:** In der Play-Fassung (`RB_STORE=play`) gibt es keinen Abruf von `api.github.com`, keinen „Herunterladen“-Knopf und kein `REQUEST_INSTALL_PACKAGES` (war nie im Manifest). Geprüft: Im Play-Modus geht kein Abruf an GitHub-Releases.
  3. **Play In-App Updates eingebaut** (`com.google.android.play:app-update:2.1.0`, Plugin-Methoden `playUpdate`/`playComplete`):
     - einmal je Start **flexibel**: lädt im Hintergrund, die App installiert es von selbst, sobald man auf der Startseite ist (nie mitten in einer Runde);
     - **sofort** (Immediate), wenn der neue Inhalt eine neuere Hülle verlangt (`stand.json` → `shell`);
     - Ersatz, falls Play kein Update anbietet (z. B. nicht über Play installiert): Karte „Bitte im Play Store aktualisieren“;
     - in der GitHub-Test-APK wird die Methode nie aufgerufen.
  4. **Geladene Dateien:** dieselbe Web-App wie im Browser (Inhalte, Fehlerkorrekturen). Neue Berechtigungen gehen technisch nur über eine neue Hülle bei Play. In `PLAYSTORE_RECHENBLITZ.md` festgehalten.
- **Tests:**
  - Play-AAB mit In-App-Updates gebaut (2,4 MB);
  - im nachgebildeten Play-Modus: Update-Abfrage beim Start, Einspielen erst auf der Startseite, kein GitHub-Releases-Abruf; GitHub-Modus unverändert;
  - Regression ohne Fehler.
- **Datenschutz 10b – nötige Textänderungen:**
  1. Im Absatz **„Android-App Rechenblitz: automatische Aktualisierung“** den Satz „Nur wenn eine neue App-Version installiert werden muss, ruft sie zusätzlich die Versionsangaben bei api.github.com ab und lädt – erst nach deinem Tipp auf „Herunterladen“ – die App-Datei von github.com bzw. objects.githubusercontent.com.“ ersetzen durch:
     > „Nur in der Test-Fassung, die wir direkt über GitHub verteilen, ruft sie zusätzlich die Versionsangaben bei api.github.com ab, wenn eine neue App-Version installiert werden muss, und lädt – erst nach deinem Tipp auf „Herunterladen“ – die App-Datei von github.com bzw. objects.githubusercontent.com. In der Fassung aus Google Play entfällt das: Neue App-Versionen kommen dort ausschließlich über Google Play, auch wenn die App ein Update selbst anzeigt (Google-Play-Schnittstelle „In-App-Updates“); dabei verarbeitet Google die Daten in eigener Verantwortung.“
  2. Im Absatz **„Android-App.“** ist der Satz „Verbindungen zu unserem Server entstehen nur, wenn du selbst einen Fehler meldest oder den Advents-Gutschein abholst“ veraltet. Ersetzen durch:
     > „Verbindungen zu unserem Server entstehen nur, wenn du selbst einen Fehler meldest, an der Klassen-Challenge teilnimmst, den Advents-Gutschein abholst oder wenn GitHub für die automatische Aktualisierung nicht erreichbar ist; dabei gelten die Angaben oben.“
  - Alles andere in 10b stimmt: kein Konto, keine Cookies, lokaler Lernstand, GitHub-Abruf mit IP und Kennung „Rechenblitz-App“, Fehlermeldung, Klassen-Challenge, Erinnerung lokal in der App.
  - Der Satz „Die Web-App Rechenblitz lädt keine Inhalte von fremden Servern“ gilt weiterhin für die Web-App im Browser.

## 16.11.2026 (42) – Google Play: Rechenblitz ist store-fertig vorbereitet (3.14.3) · Antwort auf Leitstand 16.11.

- **Auftrag (Leitstand 16.11.):** Signiertes AAB, Versionscode, Store-Eintrag DE, Grafiken, Datensicherheit, Altersfreigabe/Zielgruppe, interner Test, Play-Variante ohne Self-Update, Klick-Liste für Marcel.
- **Richtigstellung zum Self-Update:**
  - Die Play-Richtlinie verbietet Selbst-Updates außerhalb von Play und das Nachladen von **ausführbarem Code (dex/JAR/.so)**.
  - **Ausdrücklich erlaubt** ist Code, der in einer WebView läuft (JavaScript). Darauf beruhen auch Capacitor-Live-Updates.
  - Die Play-Fassung lädt deshalb **weiterhin neue Web-Inhalte** selbst, aus `rechenblitz-live`, also nur Freigegebenes. So bleibt Marcels Wunsch „App immer aktuell“ erfüllt.
  - Abgeschaltet ist nur der **APK-Download**. Braucht ein Stand eine neue Hülle, zeigt die Play-Fassung „Bitte im Play Store aktualisieren“ und fragt GitHub-Releases nicht ab.
- **Umgesetzt:**
  - **Play-Variante** über Build-Schalter `RB_STORE=play` + `RB_CHANNEL=live` (BuildConfig, wie der Kanal; keine Gradle-Flavors nötig). Die Test-APK bleibt unverändert (`github`/`test`).
  - **Signieren mit dem Upload-Schlüssel** nur über Umgebungsvariablen bzw. GitHub-Geheimnisse `RECHENBLITZ_UPLOAD_KEYSTORE` (Base64), `…_STORE_PASSWORD`, `…_KEY_ALIAS`, `…_KEY_PASSWORD`. Mit einem Wegwerf-Schlüssel getestet, der anschließend gelöscht wurde. Nichts davon ist im Repo.
  - **GitHub-Ablauf `rechenblitz-play.yml`:**
    - baut bei jedem Push auf `rechenblitz-live` (oder von Hand) das signierte AAB und legt es als Artefakt ab (90 Tage, nicht öffentlich als Release);
    - ohne Geheimnisse: Hinweis statt Fehler.
  - **Versionscode** automatisch aus der Version (3.14.3 → 31403), steigt mit jeder Version.
  - **Store-Eintrag DE:** `android-app/store/eintrag-de.txt`. Name „Rechenblitz: Kopfrechnen üben“ (29 Zeichen), Kurzbeschreibung (79), Langbeschreibung (ca. 2100).
  - **Grafiken in `android-app/store/`:**
    - Symbol 512, Vorschaubild 1024×500;
    - 7 Handy-Bilder 1080×1920;
    - 6 Tablet-Bilder 7″ (1200×1920) und 6 Tablet-Bilder 10″ (2560×1600).
  - **Klick-Liste für Marcel:** in `PLAYSTORE_RECHENBLITZ.md`, Abschnitt „Veröffentlichung bei Google Play“. Sie enthält:
    - Geheimnisse anlegen;
    - App erstellen;
    - Store-Eintrag;
    - App-Inhalte mit fertigen Antworten für **Datensicherheit**, **IARC** (erwartet USK 0/PEGI 3) und **Zielgruppe** (6–15, Familienrichtlinie);
    - interner Test → geschlossener Test → Produktion.
- **12-Tester-Pflicht:**
  - Sie gilt für **private** Entwicklerkonten, die nach dem 13.11.2023 angelegt wurden: geschlossener Test mit mindestens 12 Testern über 14 Tage vor der Produktion.
  - **Organisationskonten** (D-U-N-S) sind ausgenommen. Unser Plan vom 14.11. ging von einem Organisationskonto aus.
  - **OFFEN (Marcel):** Ist das Konto privat oder eine Organisation? Die Play Console zeigt es unter „Produktionszugriff beantragen“.
- **OFFEN:**
  - Soll vor den Links zur Nachhilfe-Website (Buchung möglich) eine Elternabfrage stehen? Das ist für die Familienrichtlinie nicht sicher geklärt und schnell einbaubar.
  - Gibt es den Upload-Schlüssel schon? Wenn nicht, erzeugt Marcel ihn beim ersten Hochladen (Anleitung in der Klick-Liste).
- **Datenschutz:**
  - 10b deckt den GitHub-Abruf ab.
  - Für die Datensicherheit sind deklariert: Spitzname/Spieler-Kennung/Punkte der Klassen-Challenge sowie Name/E-Mail (optional)/Text/Diagnose der Fehlermeldung.
- **Tests:**
  - Play-AAB und signiertes AAB lokal gebaut, BuildConfig `play`/`live` im Bundle geprüft;
  - Play-Karte „Im Play Store aktualisieren“ ohne GitHub-Abruf;
  - Regression ohne Fehler.

## 16.11.2026 (41) – Urkunde: „A4 quer – ganze Seite“ (3.14.2) · keine neue APK nötig

- **Marcel 16.11. (Bild vom Druckdialog):** Die Urkunde füllte nur das obere Drittel des A4-Blatts. Er wünscht die Option „quer, die das ganze A4 ausfüllt“.
- **Ablauf:** „Drucken“ zeigt jetzt zwei Wege:
  - **„📄 A4 quer – ganze Seite“:** Die Urkunde wird um 90° gedreht auf ein Hochformat-Blatt gelegt (`@page A4 portrait`, 6 mm Rand). Sie füllt dann das ganze Blatt im Querformat. Das funktioniert unabhängig von der Ausrichtung im Druckdialog, auch beim Android-Dialog „Als PDF speichern“.
  - **„📃 Halbe Seite“:** wie bisher.
- **Nebenbei behoben:**
  - Auf 320 px ragte im Ukrainischen der Knopf „Drucken“ hinaus.
  - Die Abzeichen-Zeile auf der Urkunde wird jetzt auch übersetzt.
- **Test:** PDF-Ausgabe beider Varianten mit je einer Seite; die ganze Seite ist gefüllt; keine JS-Fehler.

## 16.11.2026 (40) – Profile einzeln löschen (3.14.1) · keine neue APK nötig

- **Marcel 16.11.:** „Man muss Spielnamen / Accounts auch löschen können.“
- **Vorher:** Nur das gerade aktive Profil ließ sich löschen, über einen kleinen Link, und nur wenn es mehrere gab.
- **Jetzt:**
  - Unter „Wer übt?“ steht neben **jedem** Profil ein 🗑️-Knopf.
  - Es gibt eine Sicherheitsabfrage mit dem Namen: „Alle Punkte, Abzeichen, Reise und Garten dieses Profils werden gelöscht.“
  - Wird das aktive Profil gelöscht, wechselt die App zum nächsten. Eine laufende Runde wird beendet.
  - Auch das **letzte** Profil lässt sich löschen. Dann gibt es ein frisches Profil „Ich 🦊“.
  - War das Profil in einer **Klassen-Challenge**, wird sein Ranglisten-Eintrag auf dem Server gleich mit gelöscht (`POST /api/klasse/verlassen`). Ohne Netz bleibt nur der lokale Teil gelöscht.
  - Unverändert gibt es „Klasse verlassen“ in der Klassen-Challenge (löscht den Spitznamen aus der Rangliste) und „Alles zurücksetzen“ in den Einstellungen.
- **Tests:**
  - DE/EN, 320 px ohne Überlauf;
  - Löschen von fremdem, aktivem und letztem Profil, Löschen während einer Runde, Server-Aufruf beim Klassenprofil;
  - keine JS-Fehler, keine fehlenden Übersetzungen.

## 16.11.2026 (39) – Entwicklungstag: Rechengarten 🌻 (3.14.0) · keine neue APK nötig

- **Roadmap Tag 5** („gemütlich statt Druck“), vorgezogen vom 18.11.
- **💧 Wasser:**
  - Jede richtige Antwort ist ein Wassertropfen, egal in welchem Modus.
  - Die Tropfen gehen reihum an alle wachsenden Pflanzen.
  - Wer das Tagesziel schafft, bekommt Regen: einmal am Tag +10 Tropfen.
  - Wächst gerade nichts, sammelt eine Regentonne die Tropfen (max. 60). Sie gießt den nächsten Samen.
- **🌱 Pflanzen:**
  - 7 eigene SVG-Pflanzen, je 5 Stufen (Samen → Keim → Pflanze → Knospe → reif), mit Wiege-Animation. Was gewachsen ist, „wächst“ beim Öffnen sichtbar, mit fallenden Tropfen.
  - Karotte (20 Tropfen), Tulpe (30), Sonnenblume (40), Erdbeere (35, ab 1 Ernte), Tomate (50, ab 3), Kürbis (70, ab 6), Apfelbaum (100, ab 10).
- **Ernten:**
  - Eine reife Pflanze antippen: +15 💫 Funken, Konfetti, Erntekorb, dann Samen wählen.
  - Beete 4–6 werden nach 2, 5 und 9 Ernten frei.
  - Neue Abzeichen: „Erste Ernte“ und „Gärtner-Profi“ (10 Ernten).
- **Garten-Bild:**
  - Tag/Nacht passend zur Uhrzeit, Zaun, Wolken bzw. Mond und Sterne.
  - Blitz sitzt auf dem Zaun, reife Pflanzen funkeln.
- **Gemütlich statt Serie:**
  - In der Statusleiste steht statt 🔥 „Tage in Folge“ jetzt **🌱 Tage diese Woche (x/7)**. Ein Tipp darauf öffnet den Garten. Das Widget zeigt dasselbe.
  - Im Garten gibt es eine Wochenleiste Mo–So und den Satz: „Pausen sind okay – in deinem Garten welkt nichts.“
  - Die Serien-Abzeichen bleiben bestehen.
  - **Marcel 16.11. bestätigt:** „Tage diese Woche“ statt „Tage in Folge“ bleibt so.
- **Startseite:**
  - neue Karte „Rechengarten“ mit Mini-Beeten (Tropfen heute / reif → „Ernten“);
  - Blitz' Begrüßung weist auf reife Pflanzen hin, „Los →“ öffnet dann den Garten;
  - im Ergebnis die Zeile „💧 +N Tropfen für deinen Garten ›“.
- **Daten:** nur auf dem Gerät (`S.garten`). Die Wochenzählung nutzt den vorhandenen Tagesverlauf (`S.hist`).
- **Tests:**
  - Rundenablauf mit Tropfen, Wachsen, Ernte, Samenwahl mit Zurück, gesperrte Beete, Regentonne;
  - DE/EN/UK ohne fehlende Übersetzungen (1500 Einträge), 320 px ohne Überlauf, keine JS-Fehler, keine fremden Abrufe;
  - vollständige Regression (offline, APK-Modus, Merk, Reise, Welt, Widget, Selbst-Update, installierte Ansicht, 400+ Zufallsaktionen).
- **APK:** nur JS/CSS geändert, Hülle bleibt 2. Die App holt 3.14.0 von selbst, **keine neue APK nötig**.

## 15.11.2026 (38) – Knopf „Zur Website“ entfernt (3.13.4, keine neue APK nötig)

- **Marcel 15.11.:** Den Knopf „Zur Website“ oben rechts braucht es nicht. Das Logo oben links führt bereits zur Website.
- **Kopfzeile jetzt:** links „← Lernraum“ (bzw. „Persönliche Online-Nachhilfe 1:1“). Die Website bleibt über Logo, Menü und Fußzeile erreichbar.

## 15.11.2026 (37) – Android-App ebenfalls ohne Update-Knopf (3.13.3, keine neue APK nötig)

- **Marcel 15.11.:** Auch in der App soll es keinen Update-Knopf mehr geben.
- **Entfernt:** „Nach Updates suchen“ in Menü und Einstellungen. In den Einstellungen steht stattdessen eine Info-Zeile: „✨ Aktualisiert sich automatisch · Installiert: Version … · Geladen von: GitHub/Server/App-Paket · Kanal · Hülle“.
- **Ablauf:**
  - Einen neu geladenen Stand spielt die App von selbst ein, sobald man auf der Startseite ist, nie mitten in einer Runde oder bei offenem Menü. Höchstens ein Versuch pro Version und Sitzung.
  - Spätestens beim nächsten Start ist er ohnehin aktiv.
- **Bleibt:** Nur wenn eine neue App-Hülle nötig ist, erscheint die Karte „Neue App-Version nötig · Herunterladen“, weil Android die Installation selbst bestätigen lässt.
- **Am echten Gerät bestätigt (Marcel 15.11.):** 3.13.3 ist auf seinem Handy von selbst angekommen, ohne neue APK. Die Selbst-Aktualisierung über GitHub funktioniert.
- **Test (nachgebildete App-Schnittstelle):**
  - Ein Stand, der auf der Startseite ankommt, wird sofort eingespielt.
  - Kommt er während einer Runde an, wird er dort nicht eingespielt, sondern erst nach der Rückkehr zur Startseite.
  - Die Karte für eine neue Hülle erscheint, und es gibt keinen Knopf mehr. Keine JS-Fehler.

## 15.11.2026 (36) – Web-App aktualisiert sich selbst, kein Update-Knopf mehr (3.13.2)

- **Marcel 15.11.:** „Die WebApp soll automatisch updaten. Dort kein Button zum Aktualisieren.“
- **Web-App (Browser/PWA):**
  - „Nach Updates suchen“ ist aus Menü und Einstellungen entfernt.
  - Beim Start und bei jeder Rückkehr in die App (höchstens alle 10 Minuten) prüft sie still `js/changelog.js` auf dem Server.
  - Ist eine neuere Version da, wird der Service Worker aktualisiert und die Seite neu geladen, sobald man auf der Startseite ist. Nie mitten in einer Runde oder bei offenem Menü.
  - Danach zeigt Blitz „Was ist neu?“. Der Lernstand bleibt.
  - Schutz gegen Neulade-Schleifen: höchstens ein Versuch pro Version und Sitzung.
- **Android-App:** unverändert. Neue Stände kommen von selbst (Meldung 34). Der Knopf in den Einstellungen dient dort nur zur Kontrolle.
- **Test:** Das Update erscheint während einer Runde → kein Neuladen. Nach dem Wechsel auf die Startseite wird genau einmal neu geladen, ohne zweites Neuladen. Knöpfe in der Web-App sind weg. Keine JS-Fehler.

## 15.11.2026 (35) – Erster Selbst-Update-Test: 3.13.1 (keine neue APK nötig)

- Marcel hat 3.13.0 (Hülle 2) installiert. 3.13.1 ist der erste Stand, den die App **von selbst** von `rechenblitz-test` holen soll.
- **Inhalt:** In den Einstellungen steht unter „Nach Update suchen“ jetzt „Geladen von: GitHub/Server/App-Paket · Kanal Test/Live · Hülle 2“. So lässt sich die Selbst-Aktualisierung am Gerät kontrollieren.
- Die Hülle ist unverändert (2), **es braucht keine neue APK**.

## 15.11.2026 (34) – Android-App aktualisiert ihre Inhalte selbst (3.13.0 · Hülle 2) · NEUE APK INSTALLIEREN (einmalig)

- **Auftrag:** Leitstand, von Marcel am 15.11. bestätigt: „der Updateserver soll der Git sein“.
- **So funktioniert es:**
  - Die App bringt einen vollständigen Stand mit und läuft sofort und ohne Internet.
  - Kurz nach jedem Start lädt sie im Hintergrund `stand.json` (Version, Hüllen-Nummer, SHA-256 jeder Datei).
  - Zuerst fragt sie **GitHub** ab, ersatzweise den **eigenen Server**:
    - **Test-Kanal:** Branch `rechenblitz-test` bzw. `rechenblitz-test.mathenachhilfe-online.com`. Ihn hat jede APK aus dem GitHub-Ablauf.
    - **Live-/Store-Kanal:** Branch `rechenblitz-live` bzw. `rechenblitz.mathenachhilfe-online.com`. Er ist für den Store-Build gedacht (`RB_CHANNEL=live`), bekommt also nur, was der Leitstand auf `rechenblitz-live` freigibt.
  - Geänderte Dateien werden geladen und per Prüfsumme kontrolliert, unveränderte vom Gerät kopiert.
  - **Nur ein vollständig geprüfter Stand** wird übernommen. Aktiv wird er beim nächsten Start oder sofort über die Karte „Version X ist geladen · Jetzt neu starten“.
  - Ohne Netz oder bei einem Fehler bleibt der letzte gute Stand. Ältere Stände als der mitgelieferte werden nie geladen.
  - Kein Bestätigen, keine Neuinstallation. Der Lernstand bleibt.
  - „Nach Updates suchen“ prüft sofort.
- **Neue APK** braucht es nur noch, wenn sich die App-Hülle ändert (`android-app/SHELL`, jetzt **2**). Verlangt ein Stand eine neuere Hülle, übernimmt die App nichts. Sie zeigt dann „Neue App-Version nötig“ mit Download-Knopf. In der Meldung steht das künftig jedes Mal deutlich.
- **⚠️ NEUE APK INSTALLIEREN (einmalig):**
  - Marcel muss **Rechenblitz 3.13.0** einmal installieren (Release `rechenblitz-3.13.0` bzw. Drive). Die bisherigen APKs (bis 3.12.1, Hülle 1) zeigen das Update von selbst an.
  - Danach kommen alle Versionen automatisch.
- **Versionsnummer:** Web-App und App sind immer gleich, denn die App zeigt die Version des geladenen Standes. `stand.json` wird mit `node android-app/stand.js` erzeugt. GitHub-Ablauf und `build.sh` brechen ab, wenn sie veraltet ist (neue Regel 9 in der Roadmap).
- **Für die Datenschutzerklärung – diese Adressen ruft die App ab:**
  - `raw.githubusercontent.com` (GitHub; nur `stand.json` und geänderte Dateien der Web-App aus dem öffentlichen Repo `marceldaum-commits/School`);
  - als Ersatz `rechenblitz.mathenachhilfe-online.com` bzw. `rechenblitz-test.mathenachhilfe-online.com`;
  - nur wenn eine neue App-Hülle nötig ist: `api.github.com` (Release-Info), nach Tipp auf „Herunterladen“ `github.com` / `objects.githubusercontent.com` (APK).
  - Es werden **keine Nutzerdaten** gesendet, nur die technisch nötigen Verbindungsdaten (IP-Adresse, Kennung „Rechenblitz-App“).
  - Unverändert bleiben die Server-Funktionen (Fehler melden, Gutschein, Klassen-Challenge) über `rechenblitz.mathenachhilfe-online.com`.
- **Tests:**
  - Die APK kompiliert, und `stand.json` ist in der APK.
  - Die Web-Anzeige ist mit einer nachgebildeten App-Schnittstelle getestet: geladen, Hülle nötig, aktuell; DE/EN; keine JS-Fehler.
  - Der Download-Weg ist nach dem Push gegen GitHub und den Server geprüft: alle Prüfsummen stimmen.
  - Auf einem echten Gerät ist noch nicht getestet, denn hier läuft kein Android-Emulator. **Bitte auf dem Handy prüfen:** 3.13.0 installieren. Bei der nächsten Version muss die Karte „Version … ist geladen“ erscheinen.

## 15.11.2026 (33) – Bedien-Check: Korrekturen nach Marcels Klick-Test (3.12.1)

- **Anlass:** Marcel hat bemerkt, dass manche Stellen nicht wie erwartet reagieren. Ein Beispiel war die Rechen-Reise: Nach Rein und Raus fehlte die Karte. Deshalb gab es zwei Prüfungen:
  - eine automatische Klick-Prüfung: jedes anklickbare Element in 26 Ansichten, ohne echte Funde;
  - einen unabhängigen Bedien-Test wie ein Kind: Abbrechen, Neuladen, Doppeltipp, Zurück.
- **Behoben:**
  - **Rechen-Reise:**
    - Die Startkarte öffnet immer die Karte, dort gibt es „▶ Etappe N spielen“.
    - Abbrechen und „Zurück“ führen zur Karte zurück.
    - „ändern“ zeigt eine echte Klassenauswahl, mit Vorauswahl und „Abbrechen“.
    - Die Funken-Anzeige im Ergebnis enthält jetzt den Etappen-Bonus.
  - **Adventskalender und Merk-Training:** Abbrechen und „Zurück“ führen in den Kalender bzw. die Merk-Übersicht statt zur Startseite.
  - **Zurück-Taste (Android-App und Browser):**
    - schließt Fenster bzw. geht im Menü eine Ebene zurück;
    - beendet laufende Runden;
    - verlässt die App erst auf der Startseite.
    - Dafür gibt es eine Änderung in der APK (`MainActivity`).
  - **Tageschallenge:**
    - Ein abgebrochener Countdown zählt nicht mehr als Versuch.
    - Die gemessene Zeit enthält das 3-2-1 nicht mehr.
    - Im Advent (Karte ausgeblendet) schlägt Blitz die Challenge nicht mehr vor.
  - **Blitz auf der Startseite:** Ein Antippen des Fuchses startet keine Runde mehr (er hüpft nur). Gestartet wird über den Knopf „Los →“ in der Sprechblase.
  - **Rätsel des Tages:** Ein Klassenwechsel löscht kein angefangenes Rätsel mehr. Funken und Statistik gibt es nur einmal pro Tag.
  - **Abzeichen** „Modefuchs“ und „Rätselprofi“ erscheinen sofort.
  - **Garderobe:** „ausziehen“ erscheint nur, wenn im Bereich etwas getragen wird.
  - **Zurücksetzen:**
    - Teil-Zurücksetzen beendet nur die passende Runde.
    - „Alles zurücksetzen“ behält Auftrag der Lehrkraft, Adventskalender und Klasse. Der Text nennt jetzt alles, was gelöscht wird.
  - **Advent-Ergebnis:**
    - keine Urkunde unter 3 richtigen;
    - nur ein Wiederholen-Knopf;
    - bei schlechterer Wiederholung: „Dein bestes Ergebnis bleibt“.
    - Die Klassenwahl im Kalender meldet, dass sie auch für die App gilt.
  - **Merk-Training:** Die Erklärkarte hat ein eigenes ✕ zum Beenden.
  - **Ergebnis-Knöpfe:**
    - „Menü“ heißt jetzt „Startseite“ bzw. „Zurück“.
    - Nach dem Einstufungstest startet „Weiter üben“ direkt ein Training.
    - Nach leerer Fehlerkiste heißt der Knopf „Fertig“.
  - **Startseite:** unten mehr Platz, damit der runde Menü-Knopf keine Karten-Knöpfe verdeckt.
- **Bewusst so gelassen:** Im Merk-Training wächst der Zähler bei Fehlern, z. B. 3/7. Falsche Aufgaben kommen in derselben Runde noch einmal dran, das ist gewollt.
- **Tests:**
  - 41 gezielte Prüfungen (DE und EN);
  - kompletter Regressionstest: Breite 320 px, offline, APK-Modus, Merk, Reise, Welt, Abzeichen, Update-Knopf, Widget, 423 Zufallsaktionen;
  - keine JS-Fehler, keine fehlenden Übersetzungen (1434 Einträge).

## 15.11.2026 (32) – Großes Paket „Blitz-Welt“ (3.12.0)

- **Marcel 15.11.:** Er hat das große Paket („leg alles um“) freigegeben.
- **💫 Funken (Währung):**
  - **Verdienen:** +1 je richtige Antwort, +10 Tagesziel, +10 je erste geschaffte Reise-Etappe (+25 Boss), +15 je Sternbild, +3 wenn eine Merk-Aufgabe fest sitzt, +10 bis +15 für das Rätsel des Tages. Start mit 30 Funken.
  - **Anzeige:** in der Statusleiste und im Ergebnis („+20 💫“).
- **🎩 Blitz-Garderobe:**
  - 16 selbst gezeichnete Teile in 5 Bereichen: Hüte (Basecap, Zylinder, Zauberhut, Astronauten-Helm, Königskrone …), Brillen, Hals (Fliege, Schal), Anzug (Fußball-Trikot) und Umhang. Sie kosten 30–400 Funken.
  - Blitz trägt sein Outfit überall in der App: Startseite, Spiel, Ergebnis, Merk-Karten, Reise-Karte. Das Bild im Android-Widget bleibt der Standard-Blitz.
  - Bereits gekaufte Teile kann man an- und ausziehen.
- **🏠 Blitz' Zimmer:**
  - Fenster mit Tag- und Nachthimmel (Merk-Sterne nachts), Regal mit den Reise-Stickern, Pokale für Abzeichen, Teleskop, sobald Merk-Aufgaben fest sitzen.
  - Kaufbare Deko: Pflanze, Lampe, Teppich, Globus, Aquarium, Gitarre, Rakete, Katze „Pixel“.
- **🧩 Rätsel des Tages („Blitz-Zahl“):**
  - Eine geheime dreistellige Zahl (Klasse 1–2: zweistellig) in 6 Versuchen finden, jeden Tag für alle gleich.
  - Felder zeigen 🟩 richtige Stelle, 🟨 andere Stelle, ⬜ kommt nicht vor. Jeder Versuch schaltet einen Mathe-Hinweis frei: gerade/ungerade, Quersumme, Bereich, Teilbarkeit durch 3, letzte Ziffer.
  - Statistik (gespielt, gelöst, in Folge). Das Ergebnis lässt sich als Kästchen-Muster teilen, mit Link zur App (Werbung).
- **🍂 Jahreszeiten-Events** mit Sonder-Outfits, die nur im Zeitraum kaufbar sind (gekaufte bleiben):
  - Herbstzeit (bis 30.11., Pilzhut – läuft jetzt);
  - Winterzauber (1.12.–6.1., Wintermütze und Ringelschal);
  - Fasching (Partyhut), Frühlingsfest (Blumenkranz), Sommerferien (Sonnenbrille).
- **Startseite:** eine Kachelzeile „🧩 Rätsel · 🎩 Garderobe · 🏠 Zimmer“ (mit Funkenstand und aktuellem Event).
- **Abzeichen:** „Rätselprofi“, „Modefuchs“.
- **Daten:** nur auf dem Gerät (`S.funken`, `S.welt`, `S.raetsel`). Keine neuen Server-Daten.
- **Getestet:**
  - alle Outfits gezeichnet, Kauf, An- und Ausziehen, Zimmer-Deko, Event-Teil, Rätsel gewinnen und verlieren, Teilen, Tastatur;
  - Funken aus Training, Reise, Merk;
  - DE/EN/UK ohne Fehlstellen (1410 Einträge), 320 px;
  - komplette Regression inklusive Aufgaben-Sweep.
- **Neue APK: 3.12.0** – GitHub baut sie selbst.

## 15.11.2026 (31) – Einzelne Bereiche zurücksetzen (3.11.1)

- **Marcel 15.11.:** „Bitte einfügen, dass man es zurücksetzen kann.“
- **Neu in den Einstellungen unter „Zurücksetzen“:** Rechen-Reise neu starten, Merk-Training zurücksetzen, Abzeichen zurücksetzen, Fehlerkiste leeren und, wie bisher, „Alles zurücksetzen“ (jetzt inklusive Merk-Training und Reise). Reise- und Merk-Übersicht haben zusätzlich einen eigenen Link „↺ … neu starten/zurücksetzen“.
- Jedes Zurücksetzen fragt vorher nach und beendet eine laufende Runde. Der übrige Fortschritt bleibt erhalten, und das Android-Widget zeigt sofort den neuen Stand.
- Getestet: alle Wege in DE/EN/UK, 320 px, Regression. **Neue APK: 3.11.1** (GitHub baut sie selbst).

## 15.11.2026 (30) – Android-Widget für den Startbildschirm (3.11.0)

- **Marcel 15.11.:** „Kannst du ein Widget machen auch?“ Das geht nur in der Android-App, denn der Browser kennt keine Startbildschirm-Widgets.
- **Neu – Rechenblitz-Widget (4×1, frei in der Größe änderbar):**
  - Blitz, dazu „🧠 6 Aufgaben warten · ca. 3 Min.“ bzw. „Alles wiederholt ✓ · 48 sitzen fest“.
  - Darunter „🎯 12/50 heute · 🔥 3 Tage · Heute dran: 7 · 8“ und der Knopf „Los“. Ein Tipp öffnet die App.
  - Die Texte kommen fertig übersetzt aus der App (DE/EN/UK, Sprache des Handys).
  - Ist der Stand nicht von heute, zeigt das Widget stattdessen „🧠 Zeit für 3 Minuten Kopfrechnen! – Tippe hier, Blitz wartet schon“.
  - Aktualisierung bei jedem Start der App, nach jeder Runde und beim Verlassen der App.
- **Technik:**
  - **App-Seite** (`android-app/android/…`): `RbWidget.java` (AppWidgetProvider), `RbWidgetPlugin.java` (Capacitor-Brücke `RbWidget.update`, speichert nur auf dem Gerät), Layout und Hintergrund, Blitz-Bild und Vorschaubild, Texte in 3 Sprachen, Eintrag im Manifest.
  - **Web-Seite:** `widgetUpdate()` in `js/native.js`, nur in der APK aktiv. In der Web-App ändert sich nichts.
- **Getestet:**
  - Datenweitergabe in DE/EN/UK, auch nach einer Runde.
  - APK baut. Android erkennt das Widget (`provides-component: app-widget`), alle Klassen bleiben nach dem Verkleinern erhalten.
  - Regression ist grün.
  - **Auf einem echten Handy noch nicht getestet** – bitte Marcel: Startbildschirm lange drücken → Widgets → Rechenblitz.
- **Neue APK: 3.11.0** – die Hülle ändert sich (Widget), GitHub baut sie selbst. Das Update erscheint in der App bzw. über „Nach Updates suchen“.

## 15.11.2026 (29) – Knopf „Nach Updates suchen“ (3.10.1)

- **Marcel 15.11.:** „Bitte einen Button zum Prüfen der Aktualisierung einfügen.“
- **Neu:** Im Menü steht unter „Was ist neu?“ der Punkt **🔄 Nach Updates suchen** mit der installierten Version. Er steht auch in den Einstellungen, für Web und APK.
  - **Web-App:** liest die aktuelle Version frisch vom Server (`js/changelog.js`, ohne Cache). Ist sie neuer, wird der Service Worker aktualisiert und die Seite neu geladen. Sonst erscheint „Du hast die neueste Version ✓“.
  - **Android-App:** fragt die neueste GitHub-Release-Seite ab. Ist sie neuer, erscheint die Karte „⬆️ Neue Version … – Jetzt aktualisieren“.
  - Ohne Netz erscheint ein freundlicher Hinweis, und ein Doppel-Tipp wird abgefangen.
- Getestet (DE/EN/UK, Web und APK-Modus): gleiche Version, neuere Version (Neuladen bzw. Karte), keine Fehler, 320 px, offline, Regression.
- **Neue APK: 3.10.1** (GitHub baut sie selbst).

## 15.11.2026 (28) – Entwicklungstag: Rechen-Reise komplett (Kapitel 4–6), Übersetzungen vervollständigt (3.10.0)

- **Rechen-Reise, Roadmap Tag 4:**
  - **Kapitel 4–6** mit Geschichte, Karte, je 5 Etappen und Boss:
    - Wolkenstadt ☁️ (Ballon, Wolkenmarkt, Regenbogenbrücke);
    - Feuerberg 🌋 (Lava-Brücke, Feuerdrache);
    - Sternenschloss 🏰 (Spiegelsaal, Sternentreppe, Sternenwächter).
  - **Schwierigkeit:** Ab Kapitel 4 kommen Textaufgaben dazu, jedes Kapitel eine Stufe höher (passend zur Klasse). Die Bosse in Kapitel 5 und 6 haben 12 Aufgaben.
  - **Finale nach Kapitel 6:** Blitz leuchtet golden („Rechen-Reise geschafft!“), alle 6 Sticker, große Urkunde („Die ganze Rechen-Reise geschafft – alle 6 Kapitel!“) und viel Konfetti.
  - **Sticker-Album** in der Reise-Übersicht (🦉 🐟 💎 🎈 🐉 👑, noch fehlende als ❔).
  - **Abzeichen:** Wolkenflieger, Drachenbezwinger, Sternenkönig, „Echter Rechenblitz“.
  - **Karte:** Etappen-Sterne werden nicht mehr abgeschnitten. Blitz ragt an der obersten Etappe nicht mehr über den Rand.
- **Übersetzungen vervollständigt:**
  - Ein Durchlauf über 7 500 erzeugte Aufgaben je Sprache (alle Themen und Stufen) fand 15 fehlende Texte, vor allem in „Das üben wir nochmal“: Kürze …, Runde auf Hunderter/Tausender, kg/g, h/min, Quersumme, Teiler, Primfaktor u. a. Alle sind ergänzt, der erneute Durchlauf findet 0.
  - **Advents-Vorschau** (läuft seit heute, 15.11.): „Ab 1. Dezember: jeden Tag ein Türchen mit Rechen-Rätseln“ fehlte auf EN/UK und ist jetzt ergänzt.
- **Getestet:**
  - Kapitel 1–6 durchgespielt, Finale, Urkunde, Album.
  - Komplette Regression: 320 px, offline, APK-Modus, Merk, Blitz, Reise.
  - DE/EN/UK ohne Fehlstellen, Zufalls-Stresstest.
- **Neue APK: 3.10.0** – GitHub baut die Release-Seite `rechenblitz-3.10.0` selbst. Die Android-App bietet das Update an.

## 14.11.2026 (27) – Rechen-Reise (Kapitel 1–3) + großer Fehler-Check (3.9.0)

- **Marcel 14.11.:** „Mach jetzt gern weiter mit dem nächsten Paket … prüfe das Ganze mal auf Bugs und Verbesserungen.“
- **Neu: Rechen-Reise** (`js/reise.js`, Roadmap Tag 3):
  - **Geschichte:** Ein Sturm hat Blitz' Funken verstreut, das Kind sammelt sie wieder ein.
  - **Kapitel 1–3:** Zahlenwald 🌲, Rechenfluss 🌊, Funkenhöhle 🕯️. Jedes Kapitel hat eine gezeichnete Karte mit Weg und 5 Etappen plus Boss-Etappe. Blitz steht an der aktuellen Etappe, jede Etappe beginnt mit einem Story-Satz.
  - **Aufgaben:** Sie passen zur Klassenstufe, die einmal abgefragt wird: Kapitel 1 wärmt auf, Kapitel 3 eine Stufe höher. Etappen haben 8 Aufgaben aus 2 Themen, die Boss-Etappe 10 gemischte.
  - **Bewertung:** Sterne ab 60/80/95 %, unter 60 % „Nochmal versuchen“ mit Blitz-Trost.
  - **Kapitelende:** Sticker 🦉/🐟/💎, Urkunde, Abzeichen „Waldläufer“, „Flussspringer“, „Höhlenforscher“. Das nächste Kapitel wird freigeschaltet.
  - **Einstieg:** Startseiten-Karte „Rechen-Reise“ und Menüpunkt. Kapitel 4–6 (Wolkenstadt, Feuerberg, Sternenschloss) folgen an Tag 4.
- **Fehler-Check:** Ein unabhängiger Prüf-Durchgang über den ganzen Code fand 22 Punkte, davon 12 nachgestellt. Behoben:
  - **Sicherheit (hoch):**
    - Auftrags- und Ergebnis-Links hätten Schadcode einschleusen können, beim Auftrag sogar dauerhaft gespeichert. Kaputte Links (z. B. Stufe 7, fehlende Daten) haben die App blockiert.
    - Jetzt werden alle Link-Daten streng geprüft (bekannte Themen, Zahlenbereiche, Textlängen) und überall escaped. Bereits gespeicherte Altdaten werden beim Laden bereinigt.
    - Die Klassen-Rangliste nimmt nur noch Zahlen an.
  - **Timing:**
    - Alte Timer greifen nicht mehr in die nächste Runde ein. Vorher konnte „Nochmal“ während des Countdowns schon eine Aufgabe zeigen.
    - Kein doppelter Countdown nach Abbruch und Neustart.
    - Enter auf der Ergebnisseite startet erst nach 0,9 s neu.
    - Der Bild-Tipp ist nach dem Antworten gesperrt.
  - **Fortschritt:**
    - Ein Profilwechsel oder Zurücksetzen mitten im Spiel beendet erst die Runde. Vorher bekam das falsche Profil die Punkte.
    - Richtige Antworten zählen sofort für Tagesziel, Tage-Serie und Gesamtzahl, auch bei Abbruch.
    - Die Tageschallenge zählt den ersten *gestarteten* Versuch, damit man sie nicht ausspähen kann.
    - Die Fehlerkiste merkt sich „Als Dezimalzahl“.
  - **Versand:**
    - Der Postausgang für Fehlermeldungen verliert keine Meldungen mehr, wenn gleichzeitig gesendet wird.
    - Klassenpunkte werden nicht mehr doppelt gesendet, und das Profil wird festgehalten.
  - **Offline-Cache:** Der Service Worker speichert nur noch erfolgreiche Antworten, also keine Fehler- oder WLAN-Portalseiten. Bei schlechtem Netz nimmt er nach 3 s die gespeicherte Fassung.
  - **Sonstiges:**
    - Datumsrechnung sommerzeitsicher (Tage-Serie, Merk-Fälligkeiten).
    - Blitzrunde pausiert, wenn die App in den Hintergrund geht.
    - Hinweis, wenn das Speichern scheitert (Gerätespeicher voll).
    - Timer-Anzeige zeichnet nur bei Änderung neu (spart Rechenzeit in EN/UK).
    - Lange Hinweise brechen um, Rückmeldung richtig/falsch für Bildschirmleser (`aria-live`), Fokus in der Merk-Karte.
    - Blitz-Einblendung verschwindet nicht mehr zu früh.
  - **Bewusst gelassen:**
    - Startstufe ohne Klassenangabe = Stufe 1 (gewollt).
    - APK-Update per `location.href`: Capacitor öffnet fremde Adressen von sich aus im System-Browser, der die APK lädt. Auf einem echten Gerät ist das noch zu bestätigen.
- **Getestet:**
  - Die Nachstell-Skripte des Prüf-Durchgangs zeigen die Fehler nicht mehr.
  - Zufalls-Stresstest der Oberfläche (über 800 Aktionen in DE/EN): 0 Fehler.
  - Engine-Fuzz (20 000 Aufgaben je Thema/Stufe): sauber.
  - Komplette Regression: 320 px, offline, APK-Modus, Updater, Merk, Blitz, Reise.
  - DE/EN/UK ohne fehlende Übersetzungen, dazu fehlende Einheiten-Übersetzungen (dm, kg, l …) ergänzt.
- **Neue APK: 3.9.0** – GitHub baut die Release-Seite `rechenblitz-3.9.0` beim Push selbst.
- Bitte prüfen und live freigeben.

## 14.11.2026 (26) – Maskottchen Blitz, Merksätze, Sternbilder, „Was ist neu?“ (3.8.0)

- **Marcel 14.11.:** „Alle 4, leg los.“ Außerdem: „Bei dem Button unten rechts den roten Punkt entfernen – das sieht aus, als wär immer eine Mitteilung da.“
- **Neu, Datei `js/blitz.js` – Maskottchen „Blitz“:**
  - Ein orangefarbener Fuchs mit Blitz-Schwanz und navyblauem Halstuch in den Farben der Website. Eigene SVG-Zeichnung mit 6 Stimmungen: freut sich, staunt, denkt nach, tröstet, winkt, jubelt.
  - Blitz grüßt oben auf der Startseite je nach Tageszeit und Lage („Im Merk-Training warten 6 Aufgaben auf dich“).
  - Er jubelt im Spiel bei Serien (3, 5, 10 …) und nach einem Comeback, tröstet im Lösungsweg, erzählt im Merk-Training die Merksätze und kommentiert das Ergebnis.
  - In den Einstellungen abschaltbar („Maskottchen Blitz“). Bei „Bewegung reduzieren“ ohne Animation.
- **Merksätze** (`merkStory` in `js/merkvis.js`): Für jede der 185 Merk-Aufgaben gibt es eine Geschichte oder einen Trick, zum Beispiel:
  - „5, 6, 7, 8 → 56 = 7 · 8“, Spinnenbeine, Marienkäferpunkte, Wochen, Schachbrett;
  - Neuner-, Fünfer- und Verdoppeln-Trick, Zehnerfreunde;
  - Brüche über Geld („1/4 = 25 Cent“).
  Der 💡 Bild-Tipp gibt nur einen Hinweis, ohne das Ergebnis zu verraten.
- **Sternbilder mit Namen:**
  - 21 Sternbilder, zum Beispiel „Die Siebener-Schlange 🐍“, „Der Achter-Drache 🐉“, „Das Quadrat-Schloss 🏰“, „Die Komma-Pizza 🍕“.
  - Im Merk-Himmel werden die festen Sterne eines Sternbilds verbunden. Fertige Sternbilder leuchten.
  - Wenn ein Sternbild fertig wird: Feier mit Blitz und eine Karte „Neues Sternbild entdeckt!“ im Ergebnis.
  - **Sternbilder-Album** in der Merk-Übersicht (unentdeckte zeigen ❔).
  - Neues Abzeichen „Sternbild-Entdecker“.
- **„Was ist neu?“** erscheint nach einem Update einmal als Karte mit Blitz und den Neuerungen (statt eines kurzen Hinweises). Ganz neue Nutzer begrüßt Blitz mit 3 Schritten zum Start.
- **Roter Punkt am Menü-Knopf unten rechts entfernt**, wie von Marcel gewünscht.
- **Getestet:**
  - alle Merksätze und Tipps, Sternbild-Feier, Album, Begrüßung, Neuigkeiten, Spiel-Auftritte, Blitz aus;
  - DE/EN/UK ohne fehlende Übersetzungen (1145 Einträge), 320 px und PC;
  - offline, APK-Modus, Updater, keine Konsolenfehler, keine fremden Abrufe.
- **Neue APK: 3.8.0.** GitHub baut sie beim Push selbst (Release-Seite `rechenblitz-3.8.0`), die Android-App meldet das Update von sich aus.
- Bitte prüfen und live freigeben.

## 14.11.2026 (25) – FREIGABE-STAND 3.7.2: Startbild Merk-Training ist erkennbar

- **Antwort auf eure Nachricht 15:05:** Das Startbild ist bereits umgesetzt, seit 3.7.1, siehe Meldung (22). Auf der Startkarte „Merk-Training“ steht statt des dunklen Rechtecks ein klares Symbol: Mondsichel und goldenes Sternbild auf Nachtblau, 44 px, abgerundet wie 📆/🏫.
- **Auf rechenblitz-test geprüft (15:07, echtes Handy-Abbild iPhone 13):** `APP_VERSION = '3.7.2'`, Symbol vorhanden (`#merkCard .merk-ico svg`), keine JS-Fehler.
- **Zur Live-Freigabe: 3.7.2.**
  - **Code-Commit** `7e8c7bb` (letzte Änderung in `kopfrechnen/`: `ae68e89`).
  - **Branch-Kopf** `0c9d5e3`; danach kamen nur noch Meldungen und Roadmap hinzu.
  - **Enthält:**
    - 3.7.1: Startkarten-Symbol.
    - 3.7.2: Selbst-Aktualisierung der Android-App über GitHub-Releases. In der Web-App ändert sich dadurch nichts; Versionsnummern werden im Englischen jetzt richtig angezeigt.
- **Datenschutz 10b:** Bitte den Satz zur Update-Abfrage der Android-Testfassung bei GitHub ergänzen, siehe (23). Er betrifft nur die APK, nicht die Web-App.
- **Neue APK:** 3.7.2 ist auf der Release-Seite https://github.com/marceldaum-commits/School/releases/tag/rechenblitz-3.7.2. GitHub hat sie selbst gebaut, sie ist mit dem festen Test-Schlüssel signiert.

## 14.11.2026 (24) – Plan erweitert: Alleinstellungsmerkmale (Marcel) + Anfrage Nachhilfe-Verbindung

- **Marcel 14.11.:** Er möchte Alleinstellungsmerkmale und hat sie in die Roadmap übernommen („step by step“). Neu in `ROADMAP_RECHENBLITZ.md` ab Tag 9:
  - Fehler-Detektiv: Denkfehler erkennen und erklären, dazu ein Lernstand-Brief für Eltern als Brücke zur Probestunde.
  - Sprachbrücke Mathe + Deutsch.
  - Einmaleins-Rhythmus.
  - Geister-Rennen und Herausforderungs-Links.
  - „Marcel erklärt“-Clips.
  - Eltern-Bereich mit **Nachhilfe-Verbindung**.
  - Verschoben in den Ideen-Speicher: Uhr & Zeit, Geometrie.
- **ANFRAGE an den Leitstand (für Tag 17, 30.11.) – Nachhilfe-Verbindung:**
  - **Ablauf:** Das Kind gibt in Rechenblitz einen Code seiner Nachhilfelehrkraft ein.
  - **Kind → Lehrkraft:** Die App sendet freiwillig und regelmäßig einen kleinen Lernstand: Merk-Stufen je Stapel, die 10 schwächsten Aufgaben, erkannte Fehlermuster, Übungszeit je Woche. Es werden keine Namen gesendet, nur der Spitzname aus der App.
  - **Lehrkraft → Kind:** Sie sieht das in der Lehrer-App und kann Merk-Stapel oder Themen als Hausaufgabe schicken.
  - **Vorschlag für Endpunkte** (gleiches Muster wie Klassen-Challenge):
    - `POST /api/nachhilfe/verbinden {code, spitzname}` → `{schueler}`
    - `POST /api/nachhilfe/stand {code, schueler, stand}`
    - `GET /api/nachhilfe/auftrag?schueler=` → `{stapel[], themen[], bis}`
    - `POST /api/nachhilfe/trennen`
  - Bitte prüfen, ob das zur Lehrer-App/zum Kern passt. Den Datenschutz-Text dafür bitte mit Marcel klären.
  - Ich baue den App-Teil hinter einem Schalter in `config.js`, ohne Server bleibt er ausgeblendet.
- **OFFEN Marcel:** Möchte er die kurzen „Marcel erklärt“-Clips (je 30–60 Sek., ca. 15 Stück) aufnehmen? Die App bekommt am 27.11. die Plätze dafür.

## 14.11.2026 (23) – Android-App aktualisiert sich selbst über GitHub-Releases (3.7.2)

- **Marcel 14.11.:** „Kann man in der App einen Auto-Updater machen per Git, damit man es (solange es nicht public ist) auf dem Handy aktuell hat?“
- **Umsetzung, nur in der APK (`js/native.js`, `config.js` → `apkUpdates: { repo: 'marceldaum-commits/School' }`):**
  - Höchstens einmal am Tag fragt die App `https://api.github.com/repos/marceldaum-commits/School/releases/latest` ab.
  - Ist die Release-Seite `rechenblitz-X.Y.Z` neuer, erscheint oben auf der Startseite die Karte „⬆️ Neue Version X.Y.Z ist da – Jetzt aktualisieren“.
  - Der Knopf öffnet den APK-Download. Android fragt dann „Installieren?“. Weil der Test-Schlüssel gleich bleibt, ist das ein Update, und der Lernstand bleibt erhalten.
  - In den Einstellungen gibt es „Nach Update suchen“ mit der installierten Version.
  - Die Web-App fragt GitHub nie ab, das ist geprüft. Für die spätere Play-Store-Fassung wird `apkUpdates: null` gesetzt.
- **Release-Seiten** entstehen automatisch beim Push auf `rechenblitz-test` (`.github/workflows/rechenblitz-release.yml`), sobald Marcel das Repo-Geheimnis `RECHENBLITZ_TEST_KEYSTORE` hinterlegt hat. Bis dahin gibt es keine Release-Seiten, und der Updater zeigt nichts an.
- **Bitte Datenschutz 10b ergänzen:** Die Android-Testfassung außerhalb des Play Stores fragt einmal täglich bei GitHub (Microsoft) nach neuen Versionen. Dabei sieht GitHub die IP-Adresse. Es werden keine weiteren Daten gesendet.
- **Nebenbei behoben:** Versionsnummern wurden im Englischen wie Zahlen umgeformt („3,8,0“, „·“ → „×“). Jetzt bleiben sie unverändert, Rechnungen werden weiter richtig umgeformt.
- Getestet mit nachgebildeter APK und nachgebildeter GitHub-Antwort in DE/EN/UK:
  - neuere Version → Karte und Download;
  - gleiche Version → „Du hast die neueste Version ✓“;
  - zweiter Start am selben Tag → keine neue Anfrage;
  - Web-App → 0 Anfragen.
  Dazu 320 px und die komplette Regression.
- **Neue APK nötig: ja**, einmalig von Hand: 3.7.2 mit Updater liegt in Marcels Drive. Ab dann kommen Updates über die Release-Seiten.
- **Nachtrag 13:50:** Marcel hat das Geheimnis hinterlegt. Die erste Release-Seite `rechenblitz-3.7.2` ist da: https://github.com/marceldaum-commits/School/releases/tag/rechenblitz-3.7.2
  - Der erste Bau war mit einem falschen Schlüssel signiert, weil der Debug-Schlüssel bei GitHub woanders gesucht wird.
  - Behoben: Der Schlüssel wird jetzt ausdrücklich angegeben (`RB_KEYSTORE`). Vor dem Veröffentlichen prüft der Ablauf den Fingerabdruck (`c8e0…ff6d`) und veröffentlicht sonst nichts.
  - Die Release-APK ist jetzt nachweislich richtig signiert und installiert sich als Update über die Drive-Fassung.

## 14.11.2026 (22) – Danke für die Freigabe 3.7.0 · Startkarten-Symbol korrigiert (3.7.1)

- Freigabe 3.7.0 (live = 2819194, 11:31) ist angekommen. Danke!
- **Hinweis Optik umgesetzt:** Auf der Startkarte „Merk-Training“ steht statt des kleinen Himmels jetzt ein klares Symbol: Mondsichel und goldenes Sternbild auf Nachtblau. Es ist 44 px groß mit abgerundeten Ecken wie die Symbole der anderen Karten (📆, 🏫) und in Hell und Dunkel gut erkennbar. Der volle Sternenhimmel bleibt in der Merk-Übersicht und im Ergebnis.
- Getestet: alle Ansichten bei 320 px, Merk-Ablauf DE/EN/UK und PC, keine Konsolenfehler, keine fremden Abrufe.
- **Commit:** siehe Branch `rechenblitz-test` (Nachricht „Rechenblitz 3.7.1“). Bitte prüfen und live freigeben.
- **Neue APK nötig: ja.** 3.7.1 ist gebaut und liegt in Marcels Drive.

## 14.11.2026 (21) – FREIGABE-STAND für den Leitstand: 3.7.0

- **Bereit zum Testen und für die Live-Freigabe:** Rechenblitz **3.7.0 · 14.11.2026**.
- **Commit:** `1fc5c4c` (Branch `rechenblitz-test`, identisch mit dem Arbeits-Branch). Auf rechenblitz-test.mathenachhilfe-online.com um 11:08 Uhr geprüft: `APP_VERSION = '3.7.0'`, `js/merkvis.js` liefert 200.
- **Enthält alles seit dem letzten Live-Stand 3.4.0:**
  - 3.4.2 Advents-Gutschein vom Server;
  - 3.5.0 Version mit Datum und „Was ist neu?“;
  - 3.5.1 Erinnerung aufs Handy;
  - 3.5.2 Gutschein für jedes Stundenpaket und APK-Unterstützung (`native.js`);
  - 3.6.0 Merk-Training;
  - 3.6.1 Klassen-Challenge an (`classApi: '/api'`);
  - 3.7.0 Merk-Training mit Bildern, Animationen und Sternenhimmel.
  Einzelheiten stehen in den Meldungen (14)–(20).
- **Vor der Live-Freigabe nötig:**
  - Datenschutz-Abschnitt 10b (Erinnerung, Klassen-Challenge, Android-App) muss live sein, wie mit euch abgesprochen.
  - `/api/push`, `/api/gutschein`, `/api/klasse` und `/api/meldung` müssen auch auf rechenblitz… (live) laufen.
- **Testvorschlag:**
  - Startseite → Merk-Training „Los“ → neue Aufgabe zeigt „✨ Neu – merk dir das!“ mit Bild → einmal falsch antworten → Karte mit Bild und Lösungsweg → Ergebnis mit Sternenhimmel.
  - Einstellungen → Erinnerung an.
  - Menü → Klassen-Challenge.
  - Menü → „Was ist neu?“.
- **Neue APK nötig: ja** – 3.7.0 ist gebaut (gleicher Test-Schlüssel) und liegt in Marcels Drive „AndroidApps/Rechenblitz“. Web und APK haben denselben Stand.

## 14.11.2026 (20) – Merk-Training zum Erleben: Bilder, Sternenhimmel, Bewegung (3.7.0)

- **Marcel 14.11.:** „Irgendwie catcht das nicht … statisch mit den Lernkarten. Man muss Kinder fesseln. Interaktiver, erlebbar, damit man sich erinnert – mit einem Bild, einer Animation.“
- **Neu, Datei `kopfrechnen/js/merkvis.js`:** Jede Merk-Aufgabe hat ein eigenes, bewegtes Bild aus reinem SVG, offline und selbst gezeichnet:
  - Einmaleins und Geteilt als **Punktefeld**, das sich Reihe für Reihe aufbaut, mit laufender Reihensumme.
  - Plus bis 20 im **Zwanzigerfeld** („erst bis 10 auffüllen“).
  - Quadratzahlen als **Quadrat in vier Teilen** (100 + 10·k + 10·k + k²).
  - Brüche als **Pizza + Hunderterfeld**.
  - Das Bild folgt immer der angezeigten Reihenfolge (8 · 7 = 8 Reihen zu 7).
- **Ablauf:**
  - Neue Aufgaben werden zuerst als große Karte gezeigt („✨ Neu – merk dir das!“, Rechnung mit Ergebnis und Bild), danach gefragt.
  - Bei wackligen Aufgaben gibt es einen **💡 Bild-Tipp** (Bild ohne Lösung). Er zählt nicht als Aufstieg.
  - Nach einem Fehler erklärt die große Karte die Aufgabe mit Bild und Lösungsweg.
- **Merk-Himmel:**
  - Jede Aufgabe ist ein Stern. Er leuchtet umso heller, je besser die Aufgabe sitzt. Ab „sitzt fest“ ist er golden und funkelt.
  - Benachbarte feste Aufgaben einer Reihe werden mit Linien verbunden, ganze Reihen also zu Sternbildern.
  - Der Himmel erscheint klein auf der Startseite, groß in der Merk-Übersicht und im Ergebnis. Dort pulsieren die Sterne, die in der Runde heller geworden sind („✨ 3 Sterne heller“).
- **Bewegung:** Die Aufgabenkarte dreht sich herein, bei „richtig“ fliegt ein Stern davon. Sitzt eine Aufgabe neu fest, gibt es eine kleine Feier („⭐ 7 · 8 = 56 sitzt jetzt fest!“). Bei „Bewegung reduzieren“ im System sind alle Animationen aus.
- Getestet:
  - alle 185 Karten in allen drei Arten (Merk-Moment, Bild-Tipp, Fehler) in DE/EN/UK ohne fehlende Übersetzungen;
  - große Karte bei 320 px ohne Überlauf;
  - komplette Regression: Breite, offline, APK-Modus, Erinnerung, EN/UK komplett, Klassen-Challenge;
  - keine Konsolenfehler, keine fremden Abrufe.
- **Neue APK: 3.7.0 gebaut**, gleicher Test-Schlüssel, liegt in Marcels Drive.

## 14.11.2026 (19) – Antwort Leitstand: Klassen-Challenge eingeschaltet (3.6.1)

- **Danke, der Server läuft.** Gegen `rechenblitz-test` geprüft:
  - Anlegen → `{code}`.
  - Beitreten → `{spieler, name}`.
  - Punkte mit UUID-Runden-ID → ok. Dieselbe ID noch einmal zählt nur einmal.
  - 900 Punkte → 400.
  - Rangliste → `{name, bis, eintraege}`.
  - Verlassen → ok.
  - Falscher Code → 404.
  Die Pfade und Felder passen 1:1 zum App-Teil. Die Testklasse ist wieder verlassen.
- `config.js`: **`classApi: '/api'`**. Damit erscheinen auf der Startseite die Karte „Klassen-Challenge“, der Menüpunkt und die Punkte nach jeder Runde. In der APK gehen die Aufrufe an `https://rechenblitz.mathenachhilfe-online.com/api/…` (live).
- **Angepasst:**
  - Punkte je Runde sind auf höchstens 500 begrenzt (Server-Grenze). Eine sehr lange „Ruhig üben“-Runde wird sonst abgelehnt.
  - Runden-IDs sind UUIDs (8–64 Zeichen passt).
- **Gutschein für jedes Stundenpaket inkl. Gruppennachhilfe:** Die App sagt „10 % auf jedes Stundenpaket“. Das passt.
- Getestet: Startseite, Klassen-Panel und „Was ist neu?“ in DE/EN/UK ohne fehlende Übersetzungen, alle Ansichten bei 320 px, APK-Modus ohne fremde Abrufe.
- **Neue APK: 3.6.1 gebaut** (versionCode 30601, gleicher Test-Schlüssel). Sie liegt in Marcels Drive.
- Bereit zur Live-Freigabe nach der Datenschutz-Freigabe (10b).

## 14.11.2026 (18) – Entwicklungstag 1: Merk-Training (3.6.0)

- **Neu: Merk-Training 🧠** (verteilte Wiederholung, Roadmap Tag 1). Neue Datei `kopfrechnen/js/memory.js`.
  - **Stapel nach Klassenstufe:** Plus bis 20 (Kl. 1–2), Einmaleins (2–5), Geteilt (3–6), Quadratzahlen 11–20 (6–10), Brüche als Dezimalzahl (6–10), Brüche als Prozent (7–10). Zusammen 185 Kernaufgaben. Die Stapel sind im Menü umschaltbar.
  - **Merkstärke 0–6:** Die Aufgabe kommt nach 0/1/2/4/8/16/32 Tagen wieder. Richtig und flott: eine Stufe höher. Richtig, aber langsam: bleibt. Falsch: zurück auf heute, und die Aufgabe kommt in derselben Runde noch einmal. Ab Stufe 4 „sitzt fest“.
  - **„Heute wiederholen“:** eine Runde mit höchstens 20 Aufgaben, die nach etwa 3 Minuten endet. Fällige und schwächste Aufgaben kommen zuerst, dazu höchstens 6 neue pro Tag, gemischt aus den Stapeln.
  - **Fehler aus normalen Runden** (Einmaleins und Geteilt) setzen die Aufgabe im Merk-Training wieder auf „heute“.
  - **Oberfläche:** Startseite mit Karte „Merk-Training · X Aufgaben · ca. 3 Minuten · Y sitzen fest“. Im Menü eine Übersicht mit fest/heute/morgen und einem Balken je Stapel. Im Spiel stehen oben „Neu ✨“ bzw. „Merkstärke x/6“, im Ergebnis „X Aufgaben sitzen fest (+n)“. Neues Abzeichen „Gedächtnis-Profi“ (25 fest).
  - Wer bei „Als Dezimalzahl“ einen Bruch eingibt, bekommt einen Hinweis. Das wird nicht als falsch gezählt.
  - **Daten:** nur auf dem Gerät, `localStorage['rechenblitz']` → Profil → `merk` (Stufe und Fälligkeit je Aufgabe). Keine neuen Server-Daten.
- **Nebenbei:**
  - Abzeichen-Hinweise sind jetzt in EN/UK für alle Abzeichen übersetzt (vorher fehlten einige).
  - Lange Fenstertitel enden auf schmalen Handys mit „…“.
  - Ein fehlender Rundungs-Text in EN/UK ist ergänzt.
- **Getestet:**
  - Alle 185 Karten: Lösung, Eingabe-Prüfung, Lösungsweg.
  - Abstände und Rundenaufbau.
  - Durchlauf auf Handy (320 px) und PC in DE/EN/UK ohne fehlende Übersetzungen, ohne Konsolenfehler, ohne fremde Abrufe.
  - Regression: alle Ansichten 320 px, offline, APK-Modus, Erinnerung.
- **Neue APK: 3.6.0 gebaut** (versionCode 30600, 2,4 MB, gleicher Test-Schlüssel wie 3.5.2 → installiert sich als Update). Sie liegt in Marcels Drive. Die GitHub-Release-Seite folgt, sobald das Geheimnis `RECHENBLITZ_TEST_KEYSTORE` hinterlegt ist. Bis dahin überspringt der Ablauf das Veröffentlichen nur mit einem Hinweis, ohne Fehler-Mail.
- Bitte 3.6.0 prüfen und live freigeben (Datenschutz-Ergänzung zur Erinnerung aus (16)/(17) vorher).

## 14.11.2026 (17) – Marcel: Gutschein für jedes Stundenpaket, APK offline, Web + APK parallel (3.5.2)

- **Gutschein (Marcel 13.11.: „Jedes“):** 10 % gelten für **jedes Stundenpaket**, nicht nur für Einzelnachhilfe. App-Texte DE/EN/UK angepasst („10 % auf jedes Stundenpaket“), `config.js` → `text: 'auf jedes Stundenpaket'`. **Bitte Leitstand:** In `ops/trainer-api` beim Gutschein `gilt: 'jedes Stundenpaket'` statt `'ein Einzelnachhilfe-Paket'` setzen und im Shop die Gutschein-Codes für alle Pakete freigeben. Die App zeigt `gilt` derzeit nicht an, also ist das nur für Shop und Mail wichtig.
- **Datenschutz (Marcel: „Ok“):** Bitte vor der Live-Freigabe den Abschnitt Rechenblitz ergänzen:
  - Web-Erinnerung: Push-Adresse + Uhrzeit auf dem Server, freiwillig, löschbar durch Ausschalten.
  - APK-Erinnerung: nur lokal auf dem Gerät.
  - Abschnitt zur Android-App.
- **APK offline – ersetzt den TWA-Weg aus (16)/(38)/(39):** Marcel: „Die App muss, wenn APK, auch offline spielbar sein. Wer erwirbt eine Rechenapp mit Internetzwang … daher muss zukünftig die WebApp und die APK parallel gepflegt werden.“
  - Die APK liefert jetzt **alle Dateien im Paket mit** (Capacitor 7, Ordner `android-app/` im Repo-Stamm) und läuft ab dem ersten Start ohne Internet. Eine reine TWA würde beim ersten Start das Netz brauchen.
  - Eine Quelle (`kopfrechnen/`). `android-app/build.sh` baut daraus die APK, mit Version = Web-Version.
  - Unterschiede nur in der neuen Datei `kopfrechnen/js/native.js`, die nur in der APK greift:
    - Server-Aufrufe absolut an `https://rechenblitz.mathenachhilfe-online.com/api/…` über natives HTTP, also ohne CORS. Kommen die Aufrufe dort mit `X-NHO-App: rechenblitz` an? Das setzt nginx nach Host und sollte passen.
    - Die Erinnerung ist eine **lokale Mitteilung** ohne Server.
    - Kein Service Worker, keine Test-Vorschau.
  - **`assetlinks.json` wird nicht mehr gebraucht.**
  - Paketname-Vorschlag `com.mathenachhilfeonline.rechenblitz` bleibt **OFFEN** zur Bestätigung.
  - Anleitung und Store-Punkte stehen in `PLAYSTORE_RECHENBLITZ.md`.
- **Test-APK 3.5.2** (versionCode 30502, 2,4 MB, debug-signiert, mit R8 verkleinert; Capacitor-Brücke und Mitteilungs-Plugin bleiben laut Prüfung erhalten) ist gebaut und liegt nicht im Repo. Sie liegt mit der Web-App als ZIP in Marcels Google Drive unter „AndroidApps/Rechenblitz“. Der Web-Modus ist unverändert. Getestet:
  - Web-App komplett offline nach dem ersten Besuch: 4 Themen gespielt, Panels geöffnet, Meldung landet im Postausgang.
  - APK-Modus im Browser mit nachgebildetem Capacitor ohne Netz: keine einzige Netzanfrage, Erinnerung planen, Uhrzeit ändern und ausschalten in DE/EN/UK, keine Fehler.
  - Seitenbreite 320 px in allen Ansichten.
  - EN/UK ohne fehlende Übersetzungen.
  - **Auf echtem Android-Gerät noch nicht getestet** (hier kein Emulator): bitte Marcel.
- Service Worker: `icons/icon-maskable-512.png` und `js/native.js` werden jetzt auch offline vorgehalten.
- **Neue APK nötig: ja.** Das ist die erste APK, und ab jetzt gibt es zu jeder Web-Version eine APK mit derselben Nummer.
- Bitte 3.5.2 prüfen und live freigeben, mit der Datenschutz-Ergänzung vorher.

## 13.11.2026 (16) – Antwort auf Übergabe (38): Erinnerung aufs Handy, „Zur Website“, Play Store (3.5.1)

- **(1) Erinnerung aufs Handy – fertig (3.5.1):** Einstellungen → „🔔 Erinnerung aufs Handy“, Bedienung wie beim Trainer: echter Schalter (`role=switch`) An/Aus, Uhrzeitfeld (frei, 5-Minuten-Schritte, Vorschlag 17:00) nur bei „An“. Zustand beim Öffnen aus `Notification.permission` + `pushManager.getSubscription()` (nicht aus dem Speicher). Einschalten = Erlaubnis + `GET /api/push/schluessel` + `subscribe` + `POST /api/push {abo, zeit}`; Uhrzeit ändern = erneut `POST /api/push`; Ausschalten = `POST /api/push/aus {endpoint}` + `unsubscribe`. Bei **gesperrt**: Schalter aus + Anleitung (Android-Browser / installierte App / iPhone). Nicht installiert: Hinweis „Am besten als App installieren …“ mit Installieren-Knopf. iPhone im Browser: Hinweis „nur als Home-Bildschirm-App“. Einmal am Tag meldet die App sich still erneut an (hält das Abo frisch, max. 1 Anfrage/Tag).
  - **Service Worker:** `push` zeigt den Text, den die App in der jeweiligen Sprache im Cache `rechenblitz-push` ablegt (DE/EN/UK; Rückfall Deutsch), `tag: rechenblitz-erinnerung`; `notificationclick` öffnet/fokussiert die App; `pushsubscriptionchange` meldet die neue Adresse mit gleicher Uhrzeit an und die alte ab.
  - `config.js`: `push: { endpoint: '/api/push', time: '17:00' }` (leer = Bereich ausgeblendet).
  - **Datenschutz – bitte vor Live-Freigabe:** Abschnitt Rechenblitz (10b) um die Erinnerung ergänzen (freiwillig; gespeichert nur Push-Adresse + Uhrzeit; Löschen durch Ausschalten bzw. nach 6 Monaten ohne Zustellung). In der App steht ein kurzer Satz dazu mit Link.
  - Getestet: alle Zustände (aus, an, Uhrzeit ändern, gesperrt, Server-Fehler, ohne Push) in DE/EN/UK, iPhone- und Android-Ansicht, 320 px; Push-Ereignis im echten Service Worker (Text kommt an, Cache bleibt bei Updates). Eine echte Zustellung über FCM bitte auf der Testseite mit einem Handy prüfen.
- **(2) „Zur Website“:** vorhanden – Knopf „Zur Website ↗“ oben rechts, Logo, Fußzeile, Menüpunkt „Zu Nachhilfe – online“, Werbekarte. Alle führen zu `https://www.mathenachhilfe-online.com/`.
- **(3) Play Store:** vorbereitet in `PLAYSTORE_RECHENBLITZ.md` (TWA mit Benachrichtigungs-Delegation, Paketname-Vorschlag `com.mathenachhilfeonline.rechenblitz`, `assetlinks.json`-Inhalt für den Leitstand, Store-Texte/Bilder am 03.12.). Manifest um `id` und `categories` ergänzt. Veröffentlichung laut Marcel frühestens 2027. **OFFEN Leitstand:** Paketname-Schema wie beim Trainer bestätigen; später `/.well-known/assetlinks.json` auf rechenblitz.….
- **Nebenbei behoben:** Auf sehr schmalen Handys (320–360 px, z. B. iPhone SE) war die Seite 354 px breit und wurde verkleinert dargestellt (Kopfzeile, Themenraster, Profil-, Abzeichen-, Arbeitsblatt-, Flyer-Ansicht). Jetzt passt alles ohne Verkleinern. Fehlende Übersetzungen beim Gutschein (EN/UK) ergänzt.
- **Zu 3.4.2:** Auf `rechenblitz-test` liegt bereits **3.5.0** (enthält 3.4.2), ab jetzt **3.5.1**. Bitte 3.5.1 prüfen und live freigeben (vor dem 15.11. wegen Advents-Hinweis) – Datenschutz-Ergänzung zur Erinnerung vorher.
- **Nachtrag zu Übergabe (39):** Verstanden – Web-App und APK (TWA um dieselbe Web-App) haben immer denselben Stand, jede Freigabe gilt für beide; Prüfpunkt „Zur Website“ entfällt. 3.5.1 zusätzlich in der **installierten Ansicht** getestet (Android/iPhone, DE/EN/UK): kein Installier-Hinweis, bei Sperre die Anleitung für App-Einstellungen, kein Überlauf, keine Fehler. **Neue APK nötig: nein** (es gibt noch keine; die Hülle ändert sich nicht – nur Manifest `id`/`categories`, die eine künftige TWA beim Bau übernimmt). Ab jetzt steht in jeder Meldung „Neue APK nötig: ja/nein“.
- Lern-Apps-Tabelle (LERN-APPS-PARALLEL.md): Rechenblitz → Erinnerung ✅ 3.5.1 (test), Zur Website ✅, Als-App-Hinweis ✅, Play Store vorbereitet, Was-ist-neu ✅ 3.5.0, mehrsprachig ✅.

## 13.11.2026 (15) – Version mit Datum, „Was ist neu?“, 3-Wochen-Plan (3.5.0)

- **Marcel 13.11.:** „Für die nächsten 3 Wochen täglich ordentlich neue Dinge entwickeln. Bei der Version das Datum dazuschreiben.“
- **3.5.0:** Version jetzt mit Datum („Version 3.5.0 · 13.11.2026“, Fußzeile + Menü), neue Datei `kopfrechnen/js/changelog.js` (`APP_VERSION`, `APP_DATE`, `CHANGELOG`), Menüpunkt **„Was ist neu?“**, einmaliger Hinweis nach einem Update. Meldungen tragen zusätzlich `app.versionDate`. Neuer Speicherwert `localStorage['rechenblitz'].seenVersion` (zuletzt gesehene Version).
- **Plan:** `ROADMAP_RECHENBLITZ.md` im Repo-Stamm – ab 14.11. täglich ein neues Paket (u. a. Thema Uhr & Zeit, Trick des Tages, Zahlenmauern, Sterne-Shop, Lernpfad, Barrierefreiheit, Geometrie, Eltern-Bereich, Klassenzimmer-Quiz, Winterdesign, Statistik), jeweils getestet, übersetzt (EN/UK) und auf **`rechenblitz-test`** – Freigabe live wie bisher durch den Leitstand. Jede Lieferung bekommt eine eigene Meldung hier.

## 13.11.2026 (14) – Gutschein: persönlicher Code vom Server statt festem Code (3.4.2)

- **Korrektur Leitstand umgesetzt:** `ADVENT2026` ist wieder entfernt (stand nur auf `rechenblitz-test`, nie live; steht aber in der öffentlichen Git-Historie – bitte im Shop deaktivieren, falls angelegt).
- `config.js`: `adventVoucher = { endpoint: '/api/gutschein', percent: 10, text: 'auf ein Einzelnachhilfe-Paket', until: '2027-01-15' }`.
- **Ablauf:** Nach dem 24. geschafften Türchen (je ≥ 3/5) holt die App **einmal** `POST /api/gutschein` mit `{ klasse: "1"…"10" | "OS", tueren: 24 }` ab und speichert `{ code, prozent, bis, gilt }` im Gerät (`localStorage['rechenblitz']` → Profil → `advent.voucher`). Danach keine weitere Anfrage. Ohne Verbindung/429/400/5xx: `advent.voucherPending = true`, erneuter Versuch beim nächsten Start, beim Öffnen des Kalenders und bei „online“. **410** → „Die Adventsaktion ist beendet.“ (`advent.voucherEnded`). Anzeige nutzt `prozent`/`bis` aus der Antwort.
- **Text:** „Gilt für Klasse X: 10 % auf ein Einzelnachhilfe-Paket, einmal je Familie, nur bei zutreffender Klassenstufe (Prüfung bei der Buchung). Nicht mit anderen Gutscheinen kombinierbar.“ + „Dieser Code gilt nur einmal.“ (DE/EN/UK).
- **Werbung** für den Gutschein (Karte, Banner, Werbekarte, Flyer) erscheint, sobald `endpoint` gesetzt ist – also jetzt auf der Testseite. Falls der Endpunkt bis zur Live-Freigabe nicht läuft, bitte `endpoint: ''` setzen (dann keine Gutschein-Werbung).
- Getestet gegen nachgebauten Endpunkt (offline → später, 200 → Code gespeichert und nur einmal abgeholt, 410 → beendet). Auf `rechenblitz-test` gesetzt.
- **Nachtrag 16:38:** Echter Endpunkt auf rechenblitz-test geprüft (`{klasse:"7",tueren:24}` → 200, `ADVT-…`, Format passt zur App). Kein weiterer Umbau nötig – 3.4.2 (Commit 6d586d1) ist bereits auf `rechenblitz-test`; die Testseite zeigte um 16:38 noch 3.4.1 (Abholdienst). Bitte nach dem Abholen prüfen und live freigeben.

## 13.11.2026 (13) – Antwort an Leitstand: Gutscheincode eingetragen (3.4.1)

- `kopfrechnen/config.js`: `adventVoucher = { code: 'ADVENT2026', percent: 10, text: 'auf ein Einzelnachhilfe-Paket', until: '2027-01-15' }`.
- App-Texte angepasst: Werbekarte „10 % auf ein Einzelnachhilfe-Paket“, Gutschein/Banner entsprechend; Bedingungszeile jetzt „… Einmal je Familie einlösbar, nicht mit anderen Gutscheinen kombinierbar.“ (EN/UK übersetzt). Gutschein erscheint weiterhin erst nach 24 geschafften Türchen (je ≥ 3/5), Werbung dafür ab 15.11.
- **Nebenbei behoben (Englisch):** Der Trennpunkt „ · “ in Texten wurde bisher zu „×“ (z. B. „Level 3 × Auto“). Jetzt nur noch in echten Rechnungen.
- Auf `rechenblitz-test` gesetzt – bitte prüfen und `rechenblitz-live` freigeben.

## 13.11.2026 (12) – Vereinfachte Startseite (3.4.0)

- **Marcel 13.11.:** Startseite zu komplex. **Neu:** schmale Fortschrittszeile (Stufe, Tage-Serie, Tagesziel; antippen → Abzeichen), eine „Heute“-Karte (Adventskalender im Dezember, sonst Tageschallenge; Auftrag nur wenn vorhanden), „Was willst du üben?“ mit 9 Hauptthemen + „Mehr“, Schwierigkeit standardmäßig **Automatisch** (aufklappbar), ein großer Knopf **„Los geht’s“** (Training), weitere Modi als kleine Knöpfe (Blitzrunde, Ruhig üben, Duell, Fehlerkiste, Einstufungstest), eine Werbekarte. Abzeichen, 1×1-Übersicht, Wochenbericht, Einladen weiterhin im Menü (schwebender Button). Keine Funktion entfernt, keine neuen Daten.

## 13.11.2026 (11) – Sprache folgt dem Gerät (Deutsch, Englisch, Ukrainisch), Logo führt zur Website (3.3.0)

- **Marcel 13.11.:** App soll sich der Handy-Sprache anpassen – **Englisch und Ukrainisch** zusätzlich zu Deutsch; außerdem soll das **Logo oben links** zur Website führen (Knopf „Zur Website“ bleibt).
- **Umsetzung:** Sprache aus `navigator.languages` (de/en/uk, sonst Deutsch), in den Einstellungen fest wählbar (gespeichert in `localStorage['rechenblitz'].lang`). Neue Dateien `kopfrechnen/js/i18n.js`, `js/lang-en.js`, `js/lang-uk.js` (erzeugt aus `i18n/uebersetzungen.tsv` im Repo-Stamm mit `node i18n/build.js`). Alle sichtbaren Texte werden beim Anzeigen übersetzt (Menüs, Aufgaben, Lösungswege, Textaufgaben, Abzeichen, Adventskalender, Urkunde, Teilen-Texte, Vorlesen in der jeweiligen Sprache). Englisch mit `2.5`, `×`, `÷`; Datumsformat je Sprache. Eigene Eingaben (Profil-, Klassen-, Spitz-, Lehrkraft-Namen) werden nie übersetzt.
- **Geprüft:** komplette App automatisch auf Englisch und Ukrainisch durchgespielt – keine fehlenden Übersetzungen außer Eigennamen (Rechenblitz, Nachhilfe – online, Webadressen); Layout aller Themen passt in allen drei Sprachen auch auf 320 px; keine fremden Abrufe.
- **Hinweis Datenschutz/Prüfung:** keine neuen Daten, nur die gewählte Sprache im bestehenden Schlüssel. `<html lang>` und Seitentitel folgen der Sprache.

## 13.11.2026 (10) – Gutschein: einmal je Familie ist Regel

- **Marcel 13.11.:** „Einmal je Familie“ ist **verbindliche Regel** (nicht nur Empfehlung). In der App steht jetzt auf Gutschein, Banner und Klassenabfrage: „… einmal je Familie einlösbar“. **Bitte im Shop technisch so einstellen** (Nutzung je Kunde = 1) und in den Gutschein-Bedingungen nennen.

## 13.11.2026 (9) – Adventskalender nach Klassenstufe, Gutschein an die Klassenstufe gebunden (3.2.0)

- **Marcel 13.11.:** Der Kalender muss zur Klassenstufe passen („Klasse 5 keine Oberstufe, Oberstufe keine Klasse 5“), damit nicht z. B. Abiturienten Stufe 1 rechnen und reihenweise Gutscheine bekommen. **Die Korrektheit der Klassenangabe ist Bedingung für den Gutschein.**
- **Umgesetzt:**
  1. Beim ersten Öffnen fragt der Kalender die **Klassenstufe** ab (1–10 oder Oberstufe 11–13). Sie gilt für den ganzen Kalender; Ändern setzt den Fortschritt auf 0.
  2. Aufgaben je Türchen aus den Themen dieser Klasse (doppelt gewichtet) plus Wiederholung aus dem Vorjahr; ab Türchen 9 teils, ab 17 ganz eine Stufe schwerer. Oberstufe: nur Stufe 4–5 (Gleichungen, Potenzen, Prozent, Brüche, Dezimal, Negativ, Terme, Teiler, Textaufgaben …). Geprüft: Klasse 1 nur Plus/Minus/Lücke Stufe 1–2; Klasse 5 ohne Prozent/Gleichungen/Potenzen; Oberstufe ohne Grundschulaufgaben.
  3. Ein Türchen zählt erst ab **3 von 5 richtig**, sonst „Fast! … nochmal versuchen“.
  4. Gutschein-Anzeige trägt die Bedingung: **„Gilt für Klasse X – nur, wenn die Klassenstufe stimmt. Sie wird bei der Buchung geprüft.“** Hinweis auch bei der Klassenabfrage und im Banner; Eltern sollen den Code zusammen mit der Klassenstufe angeben. Die Advents-Urkunde nennt die Klassenstufe.
- **OFFEN (Leitstand/Marcel):** Gutschein-Bedingungen im Shop/auf der Paketseite festhalten (10 % auf jedes Stundenpaket, gültig bis 15.01.2027, nur bei zutreffender Klassenstufe des Kindes, Prüfung bei Buchung, **einmal je Familie – Regel laut Marcel 13.11.**, bitte im Shop so einstellen). Die App kann die Klassenstufe nicht selbst prüfen.

## 13.11.2026 (8) – Gutschein wird in der App beworben (3.1.2)

- **Marcel 13.11.:** Gutschein auch in der App bewerben. Gültigkeit **15.01.2027** (bereits in (7)/`config.js`).
- **Umgesetzt** – erscheint nur, wenn `adventVoucher.code` gesetzt ist (kein Versprechen ohne Code):
  - Startseite: Adventskalender-Karte mit Hinweis „🎁 10 % Gutschein bei 24 Türchen“.
  - Große Werbekarte: vom 15.11. bis 24.12. „Adventsaktion – 10 % auf jedes Stundenpaket“ statt der wechselnden Website-Texte, Knopf zum Kalender.
  - Im Kalender oben ein Banner „Schaff alle 24 Türchen! … Noch X Türchen.“
  - Flyer/Poster im Advent mit Zeile „Im Advent: Rechen-Adventskalender mit 10-%-Gutschein“.
- **Bitte an den Leitstand:** Code möglichst **vor dem 15.11.** anlegen und eintragen – ab dann zeigt die App die Kalender-Karte (Vorankündigung). Gern auch auf der Website (Startseite/Lernraum) auf die Adventsaktion hinweisen; Direktlink in den Kalender: `https://rechenblitz.mathenachhilfe-online.com/?advent`.

## 13.11.2026 (7) – 10-%-Gutschein für alle 24 Türchen (3.1.1)

- **Marcel 13.11. (Auswahlfenster):** statt Verlosung ein **10-%-Gutschein**, den bekommt, wer **alle 24 Türchen** schafft; gilt für **jedes Stundenpaket**.
- **App:** Nach dem 24. geschafften Türchen erscheint der Gutschein (Code, „Kopieren“, Hinweis „Zeig den Code deinen Eltern“, Knopf „Zu den Stundenpaketen“ → `https://www.mathenachhilfe-online.com/stundenpakete-preise/`) im Ergebnis und dauerhaft im Adventskalender. Vorher steht im Kalender nur der Hinweis auf den Gutschein. **Ohne Code in `config.js` erscheint nichts** (aktueller Stand).
- **OFFEN (Leitstand, bis spätestens 24.12.):**
  1. Gutscheincode im Shop anlegen: 10 % auf jedes Stundenpaket, gültig bis **15.01.2027** (Marcel 13.11.; in `config.js` als `until` hinterlegt), ggf. Nutzung je Kunde begrenzen.
  2. Code in `kopfrechnen/config.js` → `adventVoucher.code` eintragen (oder mir nennen) und auf `rechenblitz-test`/`-live` bringen.
  3. **Hinweis:** Der Code steht im öffentlichen Repo und im Quelltext der App und ist damit für Findige lesbar. Ein „echter“ Nachweis von 24 Türchen ginge nur mit Server; für eine Werbeaktion halte ich den offenen Code für vertretbar (ggf. Nutzung je Kunde begrenzen).
  4. Falls nötig: kurzer Hinweis zu den Gutschein-Bedingungen auf der Paketseite oder in den AGB.

## 13.11.2026 (6) – Entscheidung Marcel: keine Verlosung zum Adventskalender

- Marcel 13.11.: **keine Verlosung.** Der OFFEN-Punkt aus (5) Nr. 1 entfällt. `adventRaffleUrl` bleibt leer, in der App erscheint kein Verlosungs-Knopf; der Adventskalender läuft ohne Gewinnspiel (Türchen, Rechen-Tricks, Advents-Urkunde).
- Weiter offen beim Leitstand: Server Klassen-Challenge (`KLASSEN-API.md`), Themenseiten (`THEMENSEITEN.md`), Prüfung Testseite, Datenschutz `#rechenblitz`, `rechenblitz-live`, Umleitung `/lernraum/kopfrechnen/`.

## 13.11.2026 (5) – Rechenblitz 3.1.0: Reichweite & Werbung (Marcel: „Setze Punkt 1–7 um“)

**Neu in der App** (alles ohne fremde Abrufe, getestet im Browser; QR-Codes mit einem Scanner-Programm nachgelesen):
1. **Rechen-Adventskalender:** Karte ab 15.11., Türchen 1.–24.12., nachholbar bis 6.1. Jedes Türchen: 5 Aufgaben (für alle gleich, Grundschule/ab Klasse 5) + Rechen-Trick. Ab 20 Türchen Advents-Urkunde. Auf Testseiten (`adventPreview: 'auto'`, Hostname mit „test“) sind zum Prüfen alle Türchen offen. **Verlosung:** In der App nur ein Knopf zu einer Website-Seite (`adventRaffleUrl` in `config.js`, ab `adventRaffleMin` = 12 Türchen); derzeit leer = keine Verlosung sichtbar. **OFFEN (Marcel/Leitstand):** Soll es eine Verlosung geben (Teilnahmebedingungen, Datenschutz, Preis z. B. Probestunde/Gutschein)? Dann bitte Seite auf der Website anlegen und Adresse nennen – die App sammelt selbst keine Kontaktdaten.
2. **Urkunde:** Bild (PNG, 1600×1131) mit Spitzname (freiwillig), Leistung, Datum, Logo und QR-Code zur App; Teilen (Web Share), Speichern, Drucken. Angeboten nach guten Runden, neuen Abzeichen, Einstufungstest, Adventskalender und für jedes Abzeichen in der Vitrine.
3. **Flyer & Poster** (Menü → Für Lehrkräfte): A4-Poster oder 4 Handzettel A6, mit QR-Code und optionalem eigenem Hinweis. QR-Bibliothek lokal: `kopfrechnen/js/vendor/qrcode.js` (MIT, `kopfrechnen/LICENSE-qrcode-generator.txt`).
4. **Klassen-Challenge:** App-Seite fertig (anlegen, Code/QR/Link `#klasse=CODE`, Beitritt mit Spitzname, Wochen-Rangliste, Klasse verlassen, Punkte nachreichen). **Unsichtbar, solange `classApi` leer ist.** Schnittstelle: `KLASSEN-API.md` im Repo-Stamm (an die Trainer-Rangliste aus Übergabe 25 angelehnt). **OFFEN (Leitstand):** Server bereitstellen und `classApi` freigeben; Datenschutz `#rechenblitz` ergänzen.
5. **Freunde einladen:** QR-Code + „Link teilen“; für Eltern Hinweis auf „Freunde werben“ (`referralUrl`).
6. **Probestunden-Hinweis passend zum Lernstand:** auf dem Ergebnis-Bildschirm höchstens **einmal am Tag**; wenn ein Thema in den letzten 14 Tagen < 70 % richtig war (mind. 10 Aufgaben), wird es genannt („Brüche klappt noch nicht ganz? …“).
7. **Themenseiten:** Direktlinks `?thema=…&stufe=…&modus=…` und `?advent`; Textvorschläge für 6 Seiten in `THEMENSEITEN.md` (Repo-Stamm). **OFFEN (Website-Raum/Leitstand):** Seiten anlegen.

**Weitere Änderungen:** `publicUrl` in `config.js` = `https://rechenblitz.mathenachhilfe-online.com/` (Ziel aller QR-Codes, Flyer, Urkunden, Einladungen – auch auf der Testseite); Service Worker lässt `/api/…` grundsätzlich durch (kein Cache); 3 neue Abzeichen (Adventsrechner, Weihnachtsprofi, Teamplayer).

**Neue Speicher-Einträge** (zu (1) „Gespeicherte Daten“): in `localStorage['rechenblitz']` je Profil `advent` (geöffnete Türchen, richtige Aufgaben, Datum), `klasse` (Code, Klassenname, Spieler-Kennung, Spitzname), `klassePending` (noch nicht übertragene Punkte), `promoDay` (Datum des letzten Probestunden-Hinweises). Neuer Schlüssel `localStorage['rb-classes']` nur auf Geräten, auf denen eine Lehrkraft eine Klasse angelegt hat (Code + Klassenname). Urkunden und Flyer werden nur im Gerät erzeugt, nichts wird übertragen.

## 13.11.2026 (4) – Entscheidungen Marcel zu den offenen Punkten aus (1) (3.0.3)

- **utm-Parameter:** Marcel nutzt derzeit keine Besucherstatistik → Website-Links **ohne** `utm_…` (Stand 3.0.3). Umschaltbar über `utm: true` in `kopfrechnen/config.js`, falls später eine Statistik kommt. Der Hinweis in (1) zu den utm-Parametern ist damit überholt.
- **Lehrkraft-Ergebnisse fälschungssicher (Server):** Marcel: nicht relevant → bleibt bei Links ohne Server. Kein OFFEN-Punkt mehr.
- Damit sind aus diesem Raum keine Punkte offen; Rechenblitz wartet auf Prüfung der Testseite, Datenschutz `#rechenblitz` und `rechenblitz-live` durch den Leitstand.

## 13.11.2026 (3) – Rück-Link „← Lernraum“ eingebaut (3.0.2)

- Marcel 13.11.: „ja“ zum Rück-Link. Ziel `https://www.mathenachhilfe-online.com/lernraum/` (Seite „Lernraum | Mathe mit Marcel“), eingetragen in `kopfrechnen/config.js` als `lernraumUrl` – leer setzen blendet den Link aus, andere Adresse ohne Codeänderung möglich.
- Anzeige: Knopf „← Lernraum“ oben links (neben „Zur Website“) und Eintrag „Zurück zum Lernraum“ im Menü. Öffnet im selben Fenster, mit `utm_source=rechenblitz&utm_campaign=lernraum`.
- Auf `rechenblitz-test` gesetzt (Commit siehe Branch).

## 13.11.2026 (2) – Entscheidung Marcel: `/lernraum/kopfrechnen/` durch Rechenblitz ersetzen

- **Marcel 13.11. (Auswahlfenster in diesem Raum):** Die bestehende Seite `https://www.mathenachhilfe-online.com/lernraum/kopfrechnen/` („Kopfrechen-Trainer“, 10 Levels, Zeitmodus, Challenge-Links, nutzt `../mm-core.js`) soll **durch Rechenblitz ersetzt** werden. Grund: Marcel findet die Seite am PC nicht ansprechend; Rechenblitz hat das Website-Design, Hell/Dunkel, Ton, eigenes PC-Layout und mehr Themen. Die Seite stammt nicht aus diesem Raum und liegt nicht in `School` – Umsetzung daher beim Leitstand.
- **Vorschlag zur Umsetzung:**
  1. Bis `rechenblitz-live` steht: Seite unverändert lassen (oder für Marcel zum Vergleich auf die Testseite verlinken).
  2. Mit `rechenblitz-live`: `/lernraum/kopfrechnen/` per **301** auf `https://rechenblitz.mathenachhilfe-online.com/` umleiten (auch mit angehängter Abfrage/Hash, damit alte Lesezeichen und Challenge-Links nicht ins Leere laufen – sie landen dann auf der Rechenblitz-Startseite; die alten Challenge-Codes sind mit Rechenblitz nicht kompatibel). Menüpunkt „Kopfrechnen“ im Lernraum auf dieselbe Adresse setzen.
  3. **Kein iframe-Einbetten** empfohlen: Installieren/Offline (Service Worker) und `localStorage` funktionieren in einem iframe einer anderen Adresse nur eingeschränkt (Speicher-Partitionierung).
  4. Falls `../mm-core.js` nur von dieser Seite genutzt wird, kann es danach entfallen – bitte im Lernraum prüfen.
- Wenn Rechenblitz einen Rück-Link „← Lernraum“ bekommen soll, bitte die Zieladresse nennen; ich baue ihn dann ein (über `config.js`, ohne Codeänderung umschaltbar).

## 13.11.2026 (1) – Antwort auf Übergabe (26) und (27): Stand 3.0.1 auf `rechenblitz-test`

**Erledigt nach (27):**
1. `kopfrechnen/config.js`: `reportEndpoint: '/api/meldung'` (relativ, kein CORS).
2. `MELDUNGEN-API.md` aus `kopfrechnen/` in den Repo-Stamm verschoben. In `kopfrechnen/` liegen jetzt nur noch: html, css, js, webmanifest, svg, png, woff2, txt (`LICENSE-fonts-OFL.txt`). Keine Symlinks, Größe ca. 750 KB, kein PHP.
3. Branch **`rechenblitz-test`** gesetzt: App-Stand **c2825c9** (3.0.1); dieser Nachtrag ändert nur `MELDUNG_RECHENBLITZ.md`, nichts in `kopfrechnen/`.
4. Meldeformular: Hinweis „Name und E-Mail sind freiwillig und werden nur für Rückfragen zu deiner Meldung genutzt. Ein echter Name ist nicht nötig – ein Spitzname reicht.“; Felder heißen „Name oder Spitzname (freiwillig)“ und „E-Mail für Rückfragen (freiwillig)“.
5. Server-Antworten werden ausgewertet: `200` → „Danke“; `429` → Meldung bleibt im Postausgang, später erneut; `400` → wird verworfen und nicht wiederholt (Hinweis an die Person); Netzfehler/5xx → Postausgang. Meldungen werden vor dem Senden auf < 15 KB begrenzt (zur Not ohne Rohdaten der Aufgabe). `id` = `crypto.randomUUID()` (Fallback: Ziffern/Buchstaben, ≥ 8 Zeichen).

**Erledigt nach (26):**
- Keine fremden Abrufe: Schriften lokal als woff2 (`kopfrechnen/fonts/`: Inter, Source Serif 4 kursiv, Caveat; **nicht** Fredoka/Nunito), Lizenz `kopfrechnen/LICENSE-fonts-OFL.txt` (SIL OFL 1.1). Keine CDNs, kein Tracking, keine Cookies. Browsertest: beim Laden und Melden nur Anfragen an die eigene Adresse.
- Logo: `kopfrechnen/icons/logo.svg` = Logo-Symbol der Website (`logo-symbol-20261111.svg`); App-Icons daraus erzeugt.
- Fußzeile: Impressum `…/impressum/`, Datenschutz `…/datenschutz/#rechenblitz`, Probestunde `…/probestunde/`.
- Links zur Website tragen `?utm_source=rechenblitz&utm_medium=app&utm_campaign=<Stelle>` (Button „Zur Website“, Werbekarte, Ergebnis-Hinweis, Menü, Wochenbericht). Bitte sagen, falls das nicht gewünscht ist – dann nehme ich die Parameter heraus.

**Gespeicherte Daten (nur im Gerät, nie automatisch übertragen außer Meldungen):**
- `localStorage['rechenblitz']` (JSON, Version 3): je Profil (Name/Spitzname + Emoji, frei wählbar) Lernstand: XP, gelöste Aufgaben, Rekorde, Fehlerkiste (Aufgaben), Abzeichen, Einmaleins-Statistik je Aufgabe, Übungsverlauf je Tag (max. 70 Tage: Anzahl, richtig, Übungszeit, je Thema), Tageschallenge-Ergebnis, Einstellungen (Ton, Vorlesen, Design, Klassenstufe), Auftrag einer Lehrkraft (falls per Link geöffnet) mit eigenen Runden-Ergebnissen. Außerdem geräteweit: **Postausgang** (`outbox`, s. u.), zuletzt eingegebener Name/E-Mail des Meldeformulars (`reporter`, damit man sie nicht neu tippen muss), Zeitpunkt der letzten Meldung (`lastReportAt`).
- `localStorage['rb-teacher']`: nur auf Geräten, auf denen jemand einen Lehrkraft-Auftrag erstellt hat – der angezeigte Name der Lehrkraft.
- Cache Storage `rechenblitz-<Version>` (Service Worker): nur die App-Dateien für Offline/Installieren. Keine Cookies, kein sessionStorage, keine IndexedDB.
- **Postausgang:** vollständige Meldungen (Format `MELDUNGEN-API.md`: id, Zeit, Art, Nachricht, freiwillig Name/E-Mail, ggf. Aufgabe mit Antwort und Lösung, App-Version, Browserkennung, Bildschirmgröße, Einstellungen), die noch nicht übertragen werden konnten (offline, 429, Serverfehler). Werden beim nächsten Start bzw. wenn das Gerät wieder online ist gesendet und danach gelöscht; 400-Antworten werden verworfen.
- **Links ohne Server:** Lehrkraft-Aufträge (`#auftrag=…`) und Ergebnisse (`#ergebnis=…`) stecken base64-kodiert in der Adresse; sie enthalten Titel, Name der Lehrkraft, Thema/Stufe und beim Ergebnis den Profilnamen des Kindes und dessen Runden (richtig/falsch, Zeiten, bis 8 Fehler je Runde). Diese Links werden nur geteilt, wenn jemand selbst auf „Teilen/Senden“ tippt. Für den Datenschutztext relevant.

**Was gebaut ist (3.0.1):** 17 Themen × 5 Stufen, alle Aufgaben zufällig erzeugt, mit Lösungsweg bei Fehlern; Stufe „Auto“; Einstufungstest; Klassenmodus; Training, Blitzrunde, Ruhig üben, Duell (zwei Personen, ein Gerät), Fehlerkiste, Tageschallenge (für alle gleich, Ergebnis teilbar); Einmaleins-Übersicht; 29 Abzeichen; Profile; Wochenbericht (teilen/drucken); Lehrkraft-Aufträge und Arbeitsblätter (Druck); schwebender Menü-Button mit „Fehler melden“; Werbung für die Website; PWA (offline, installierbar); Hell/Dunkel, Ton (Web Audio), Vorlesen (Sprachausgabe des Browsers); eigenes PC-Layout. Details: `README.md`.

**Hinweise für die Prüfung:**
- „Vorlesen“ nutzt `speechSynthesis` des Browsers (je nach System können Stimmen online sein – nicht von der App gesteuert). Standard: aus.
- Der Service Worker cached nur GET-Anfragen der eigenen Adresse; `/api/meldung` (POST) wird nie gecacht. Seite, JS und CSS werden „erst Netz, dann Cache“ geladen.
- Teilen nutzt `navigator.share` bzw. die Zwischenablage, sonst ein Textfeld zum Kopieren.

**OFFEN:**
- Lehrkraft-Ergebnisse sind nicht fälschungssicher (werden im Gerät des Kindes erzeugt). Wenn gewünscht, könnte ein Server-Endpunkt Ergebnisse annehmen – nur nach Marcels Entscheidung.
- Bitte Rückmeldung, ob die `utm`-Parameter an Website-Links bleiben sollen.
