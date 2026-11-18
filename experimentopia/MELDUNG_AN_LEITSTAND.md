# Meldungen an den Leitstand – Experimentopia

Neueste oben.

## 18.11.2026 – Version 2.5.2 vom 18.11.2026: iOS-Laufzeit abgesichert – fertig für den Betriebstest

Test-Link: https://experimentopia-test.mathenachhilfe-online.com (2.5.2). Ursache auf Marcels iPhone (iOS ≥ 18.4) mit WebKit nicht nachstellbar; abgesichert wurden alle Kandidaten:
- **(a) Speicher**: Pixeldichte für die Zeichenfläche auf 2 begrenzt (iPhone hat 3) → Zeichenfläche iPhone 15: 786×1704 statt 1179×2556 = **5,1 statt 11,5 MB je Bildpuffer (−56 %)**,
  JS-Heap 13,9 statt 17,6 MB (gemessen in Chromium mit DPR 3). Große Teile (Nova, Stimme) werden ohnehin nicht geladen (Vorlesen aus).
- **(b) WebGL-Kontextverlust**: `webglcontextlost` auf der Compose-Zeichenfläche abgefangen → Fehlerseite mit Version, Grund und „Neu laden“ (Test mit WEBGL_lose_context ✅).
- **(c) Alte Version**: `pageshow` mit `persisted` (Safari holt eingefrorenen Tab/Startbildschirm-App zurück) → sauberes Neuladen; Fehlerseite bietet „Speicher leeren & neu laden“
  (Service Worker + Versions-Speicher weg, Spielstand bleibt). Auto-Update unverändert (7/7).
- **Audio vor dem ersten Tippen**: im Browser schon abgesichert (Ton erst nach `userActivation`).
- **Jeder Startfehler sichtbar** (seit 2.5.1): Fehlerseite statt weißer Seite, Wächter „keine Zeichenfläche nach 4 s“ und „nach 60 s nicht gestartet“.
- Tests: `tools/web-start.mjs` (WebKit) 5/5 ✅ · `tools/web-update.mjs` 7/7 ✅. JS-Ersatz zurückgestellt (WasmGC ist auf iOS ≥ 18.2 da).
Bitte Marcel auf dem iPhone test öffnen lassen: Startet sie, ist es behoben; sonst zeigt die Fehlerseite unten Version, iOS und Grund – Foto davon an mich.

## 18.11.2026 – Version 2.5.1 vom 18.11.2026: iOS „weiße Seite“ – Zwischenstand, fertig für den Betriebstest

- **Nachgestellt mit Playwright-WebKit 26.6 (≈ Safari iOS 26), iPhone-13-Profil**: test startet einwandfrei (Bildschirmfoto ok, keine Fehler). Die weiße Seite war damit
  hier nicht nachzustellen.
- **Mindestversion**: Kotlin/Wasm braucht WasmGC → **Safari/iOS ab 18.2** (exnref/JSTag nutzt die App nur, wenn vorhanden – kein Problem). Ältere iOS-Versionen bekamen schon bisher
  eine Meldung auf dem Ladebild; eine **weiße** Seite entsteht, wenn der Fehler erst nach dem Ausblenden des Ladebilds passiert (oder Safari den Tab wegen Speicher neu lädt).
- **Nie mehr weiße Seite**: eigene Fehlerseite (unabhängig von der App) mit Version, Grund, iOS-Version, WasmGC ja/nein und den Knöpfen „🔄 Neu laden“ und
  „Speicher leeren & neu laden“ (Service Worker + Versions-Speicher weg, Spielstand und Nova-Speicher bleiben). Greift bei: jedem Fehler vor dem Start, Fehlern in den ersten
  Sekunden danach, wenn noch keine Zeichenfläche da ist, Wächter „nach 4 s keine Zeichenfläche“ und „nach 60 s nicht gestartet“. Ohne WasmGC auf iOS < 18.2:
  „Dein iPhone hat iOS x.y. Experimentopia braucht mindestens iOS 18.2 – Einstellungen → Allgemein → Softwareupdate.“
- **Test** `tools/web-start.mjs` (WebKit): normaler Start ohne Meldung ✅ · altes iOS ohne WasmGC → Meldung iOS 18.2 ✅ · fehlende Datei → „Neu laden“ ✅. Auto-Update-Test 7/7 ✅, Chrome ohne Fehlmeldung ✅.
- **Kotlin/JS-Ersatz** (Compose-Kompatibilitätsmodus) noch **nicht** gebaut: großer Umbau (eigene JS-Fassung aller Browser-Brücken), lohnt nur, wenn die Ursache wirklich iOS < 18.2 ist.
  Bitte Marcel: iOS-Version des iPhones (Einstellungen → Allgemein → Info) bzw. ein Foto der neuen Fehlerseite von test – dann entscheide ich. Mindestversion vorher und nachher: iOS 18.2.

## 18.11.2026 – Version 2.5.0 vom 18.11.2026: nur WebApp + „App installieren“ – fertig für den Betriebstest

Auf Marcels Bestätigung („Alles umsetzen“):
1) Android-Code bleibt im Repo (`experimentopia/app`), wird in den Tests weiter mitgebaut, aber **nicht mehr ausgeliefert**: `experimentopia-release.yml` hat keinen Push-Auslöser mehr (nur noch von Hand).
2) **APKs lagen**: als Assets in den GitHub-Releases `experimentopia-v0.1.0 … v2.4.8` (Repo marceldaum-commits/AndroidApps; Drive-Upload war nie eingerichtet, Play nie hochgeladen).
   Entfernt durch den Workflow `experimentopia-apk-entfernen.yml` (löscht alle `.apk`-Assets, prüft danach auf 0; die Releases mit Text bleiben als Verlauf).
   In App und README keine APK-/Play-Hinweise mehr (README: „Nur WebApp“). Der Android-Updater bleibt nur im (nicht ausgelieferten) Android-Code.
3) Auto-Update der WebApp unverändert aktiv (2.4.7, Test `tools/web-update.mjs` 7/7).
4) **„📲 App installieren“** auf Startseite und in den Einstellungen, nur wenn noch nicht installiert (display-mode standalone / navigator.standalone):
   Chrome/Android über `beforeinstallprompt` (früh im index.html abgefangen), iPhone/iPad mit Anleitung „Teilen ⬆️ → Zum Home-Bildschirm → Hinzufügen“.
   Manifest geprüft: `display: standalone`, `start_url`/`scope` `./`, Icons 192/512 `any maskable`, offline startfähig (Service Worker, getestet).
   Browser-Test: Chrome mit Angebot → Knopf da; iPhone 13 → Knopf mit Anleitung; als installierte App → kein Knopf. Bildschirmfoto `screenshots/70_app_installieren.png`.
5) Forscher-Pass-Texte: „gilt in der Web-App“ (Einstellungen, Kaufseite, Eltern-Hinweis, Datenschutz-Satz).
Liegt auf test (nicht live).

## 18.11.2026 – 2.4.8 ist live

- Auf Marcels „jo 2.4.8“ `experimentopia-live` auf den getesteten Stand gesetzt (fast-forward, d5e911d). Live: Vorlesen aus, texte.json leer.

## 18.11.2026 – Version 2.4.8 vom 18.11.2026: Vorlesen aus – fertig für den Betriebstest

- Auf Marcels Bestätigung („Ja, abschalten“): Schalter `Nova.ENABLED = false` (core/model, Code bleibt vollständig). Aus: alle 🔊-Knöpfe und das 🔊 in Blubbs Sprechblasen,
  automatisches Vorlesen, Bereich „Vorlesen“ in den Einstellungen (Stimme, Sprechtempo, Probe hören, Offline laden, Hinweise), Nova-Absatz im Datenschutztext.
  Android legt den Vorleser gar nicht erst an (keine Gerätestimme), die WebApp ebenso.
- `vorlesen/texte.json` = `{"format":1,"app":"experimentopia","texte":[]}` (Test prüft das).
- Browser-Test (Startseite, Rätsel des Tages, Vermuten, angetippte Sprechblasen): **0 Abrufe** an stimmen.mathenachhilfe-online.com, **0 × Gerätestimme**.
- Musik (neu seit 2.4.0) und Effekte bleiben, „Musik“ und „Effekte“ getrennt schaltbar, dazu Vibration. Blubb-Blubbern, Labor-Klänge, Erfolgs-Töne unverändert.
- Alle Tests grün (Logik, 69 Bildschirmfotos, Lint), Web = Android. Enthält Nacht-Auftrag (2.4.2), Logo (2.4.3), eindeutige Aufträge (2.4.5), Auto-Update (2.4.7). Liegt auf test; live ist 2.4.7.

## 18.11.2026 – 2.4.7 ist live

- Auf Marcels „Ja“ `experimentopia-live` auf den getesteten Stand gesetzt (fast-forward, 3d8dd7f). Ab jetzt aktualisieren sich offene WebApps von selbst.

## 18.11.2026 – Version 2.4.7 vom 18.11.2026: WebApp immer automatisch aktuell – fertig für den Betriebstest

1) Update-Prüfung beim Öffnen, bei Rückkehr (visibilitychange **und** focus) und alle 30 Minuten: `registration.update()` + Abgleich `version.json?nc=…` mit `cache:'no-store'`
   (der Service Worker leitet version.json und `?nc=` immer ans Netz durch, nie aus dem Speicher).
2) skipWaiting + clients.claim; Neuladen nur auf der Startseite oder wenn der Tab im Hintergrund ist – nie mitten in einem Versuch (sonst beim nächsten Wechsel zur Startseite).
   Danach Hinweis „✨ Neue Version x.y.z geladen“.
3) localStorage bleibt unangetastet; alte Versions-Speicher werden gelöscht, `experimentopia-nova` (Offline-Stimme) bleibt.
4) Wer eine alte Version offen hat, bekommt die neue spätestens beim nächsten Öffnen/Zurückkehren.
5) Version sichtbar: Einstellungen („Version x vom …“) und Ladebild.
**Beleg**: Browser-Test `experimentopia/tools/web-update.mjs` (Playwright): A läuft → B erscheint auf dem Server → Tab kehrt zurück: mitten im Versuch kein Neuladen ✅,
auf der Startseite automatisch B ✅, Hinweis ✅, localStorage erhalten ✅, Caches aufgeräumt + Nova bleibt ✅, neuer Tab startet mit B ✅ (7/7 grün).
Außerdem in 2.4.6: Android-Nova robuster (Sprechtempo erst nach dem Start setzen – vermutlich Ursache dafür, dass auf Marcels Handy die Gerätestimme kam) + Diagnose unter „Probe hören“.

## 18.11.2026 – 2.4.5 ist live

- Auf Marcels „Ja“ `experimentopia-live` auf den getesteten Stand von `experimentopia-test` gesetzt (fast-forward, ac22462). Live meldet 2.4.5. Nova: 3 neue Auftrags-Sätze werden beim nächsten Werk-Lauf vertont (vorher 99,9 %).

## 18.11.2026 – Version 2.4.5 vom 18.11.2026: „zwei Wege“ geprüft – fertig für den Betriebstest

Geprüft: alle 200 Versuche – Vermutungs- und Knobelfragen (Auswahl), Erklär-Schritt (Begründungen) und **die Ziel-Kontrolle aller 34 Labor-Geräte** (Code gelesen).
- **Auswahlfragen**: genau eine richtige Antwort (Nacht-Auftrag, 72 Korrekturen). Neuer Test `EindeutigTest`: keine doppelten Antworten, keine gleichwertigen Größen
  (gleiche Zahl + Einheit, Prozent/Bruch für denselben Anteil), keine doppelten Begründungen – grün.
- **Labor-Aufträge akzeptieren alle richtigen Wege** (Kontrolle rechnet mit dem Modell, nicht gegen eine Musterlösung): Wippe (jede ausgeglichene Belegung),
  Flaschenzug (jede Seilzahl unter der Kraftgrenze), Rampe, Mischtemperatur (jedes Verhältnis ±1 °C), Pendel (jede Länge ±1 %), Stromkreis (jeder Aufbau mit der
  Ziel-Stromstärke), Licht-/Farbmischen (jede Mischung, die den Zielton trifft), Titration (jede Zugabe-Folge im pH-Bereich), Ballon (jede Natron-Menge ≥ Ziel),
  Mikroskop (jede Vergrößerung mit „gut“), Genetik (jedes Elternpaar mit dem Ziel-Anteil), Nahrungskette (jede gültige Kette), Reihenfolge der Schritte ist überall frei.
- **Ein Fall eindeutig gemacht (b)**: Reaktions-Baukasten (8 Versuche) – Vielfache sind chemisch auch ausgeglichen, gezählt wird aber nur die kleinste Form
  (die App sagt dann „Stimmt – aber es geht mit kleineren Zahlen!“). Der Auftrag lautet jetzt „Gleiche die Gleichung mit den kleinsten ganzen Zahlen aus …“; Test sichert das ab.
- texte.json: 3 neue Auftrags-Sätze (klein, unter 1000 Zeichen).

## 18.11.2026 – Version 2.4.4 vom 18.11.2026: fertig für den Betriebstest (Nacht-Auftrag + Logo + Nova abgeschlossen)

- **Nova abgeschlossen**: texte.json (live und test) enthält **alle** festen Sätze und alle vorhersehbaren Oberflächen-Sätze (Begrüßung, Insel-/Fach-Einleitungen,
  Forscherbuch-Tipps, Forscher-Pass-Sätze je Versuch/Gerät) als ganze Sätze – **2579 Sätze**, keine echten Laufzeit-Sätze mehr (`/jetzt` wird nicht gebraucht).
  Liste unverändert seit 2.4.2 – keine neue große Liste, keine Kostenmeldung nötig.
- **Nova-Abdeckung: 100 %** (2579/2579, HEAD 200, live und test, 18.11. früh).
- **Klicktest** auf test (320 px, Chromium): Begrüßung, Rätsel des Tages, Rätsel-Karte, Frage, Blubb-Sätze → alle Nova-Abrufe 200, **0 × Gerätestimme**.
  Dazu verhindert ein Test (`NovaCoverageTest`), dass ein Bildschirm eigene, nicht vertonte Vorlesetexte baut.
- **Offline laden**: lädt genau die 2579 Sätze aus derselben Liste; Größenangabe aus `stand-<host>.json` „bytes“ = **72,4 MB**, nachgemessen als Summe aller
  Dateien = 72,4 MB (stimmt). Bestqualität laut „bytes_original“ 228 MB. Fassung 2 des Werks: an der App nichts zu ändern (Hash = Originaltext).
- **Logo**: siehe 2.4.3 (überall das App-Symbol). **Nacht-Auftrag**: siehe 2.4.2 (200 Versuche geprüft, 72 Stellen in 60 Versuchen geändert).
- 2.4.4 korrigiert nur das Versionsdatum (2.4.3 zeigte „vom 17.11.2026“). Liegt auf test; live ist 2.4.2.

## 18.11.2026 – Version 2.4.3 vom 18.11.2026: ein Logo überall – fertig für den Betriebstest

- Referenz: App-Symbol (`icon-512.png` = Android `ic_launcher_foreground`, gleiche Geometrie). Schon gleich waren: Android-Symbol (adaptiv + monochrom), PWA-Symbole 192/512, Favicon/Apple-Touch-Icon.
- **Angeglichen**: Kopf der Startseite (zeigte bisher die Figur Blubb mit Lächeln/Bäckchen) → `AppLogo` zeichnet exakt das App-Symbol; Ladebild der WebApp (eigene Variante) → dieselben Pfade wie das App-Symbol.
- Forscherbuch und Teilen-Bilder: Das Forscherbuch zeigt kein Logo, Teilen-Bilder gibt es in der App nicht – nichts abzugleichen. Blubb bleibt als Figur in den Sprechblasen (kein Logo).
- Enthält den Nacht-Auftrag aus 2.4.2 (200 Versuche geprüft, 72 Stellen geändert, Nova 100 %). Liegt auf test; live ist noch 2.4.2.

## 18.11.2026 – 2.4.2 ist live

- Auf Marcels Anweisung („Stell live“) `experimentopia-live` auf den getesteten Stand von `experimentopia-test` gesetzt (fast-forward, Commit fade530). Live meldet 2.4.2; Nova-Abdeckung live 100 %.

## 17.11.2026 – Version 2.4.2 vom 17.11.2026: fertig für den Betriebstest (Aufgaben geprüft, Nova ganze Sätze)

**1) Aufgaben geprüft: 200 von 200 Versuchen** (je Rätsel, Vermutung mit Auswahl, Laboraufbau, Auftrag, Auflösung, Begründungen, Merksatz, Knobelfrage) –
automatisch ausgelesen, von vier Prüfdurchgängen fachlich und sprachlich gelesen (mit Nachrechnen), dazu die bestehenden Modelltests (Antworten werden aus
Physik-/Chemie-/Bio-Modellen berechnet). Keine Zufallsaufgaben in dieser App. **Geändert: 72 Stellen in 60 Versuchen**:
- Fehlende Angaben ergänzt: k1 (Papa auf Feld 1), k9/k17 (ohne Reibung), k12 (Wippe hat 5 Felder), h4 (Wasser, gesuchte Größe), h9 (Vergleich zu 100 g Eis),
  h12 (Masse/Wassertemperatur), h13 (0 → 100 °C), c26 (100 mL → 2 L), p4 (verglichen mit 10 cm), e8 (37 °C, 20 min), m2 (welche Farben), f16 (eine Schwingung), w9 (im Wasser).
- Unklare Bezüge/Fragen: h10, e7, n7 (3×), f6, f9, u2, u5, c8, c25, c19 (Volumen), k4, k13 („statt …“), f4 (praktisch gleichzeitig), t6, s18, h13.
- Geschichte passend zum Labor: w3 (Kartoffel ist kein Obst), m4, l9 (pusten), o2 (Kartoffel statt Gurke), s17 (Teilspannung nicht messbar), l19 (Werte einstellbar),
  p2, u5, h14 (−30 °C statt Tiefkühltruhe), n1, w8 (Reihenfolge).
- Halb richtige Ablenker ersetzt: h5, j2, s13, k19; h7 neu „Was schmilzt als Letztes?“ (Butter/Schoko überlappen), h7-Knobel-Antwort präzisiert.
- Fachlich präzisiert: h3 (Verdampfen ≠ schneller), h12 (pro Gramm), w16 (Gewichtskraft), t1 (Gelöstes läuft durch), s20 („meist“), s10, s2 (2×), s5, s13, w4,
  w12/w14 (Einheit g/cm³), l9/l16 (Begründung), c18 (pH 5 genannt), k11.
- Rechenfehler: l17 (4 · 659 = 2636), l18 (262 · 1,5 ≈ 393), k18 (490 J).

**2) Nova – ganze Sätze, nie ungewollt die Gerätestimme**
- Jeder Text wird an Satzgrenzen geteilt, jedes Stück ist ein ganzer Satz (≤ 300 Zeichen, geprüft: kein Satz zerstückelt). Nichts wird aus Schnipseln zusammengesetzt.
- **Alle** Oberflächen-Sätze (Begrüßung, 8 Insel-, 3 Fach-Einleitungen, Erklär-Reaktionen, Forscherbuch-Tipps, Forscher-Pass-Sätze inkl. jedes Versuchs-/Gerätenamens,
  Theorie-Einleitungen) stehen jetzt in texte.json → **2579 Sätze** (vorher 2301), 213 119 Zeichen. Ein Test verhindert, dass ein Bildschirm eigene Vorlesetexte baut.
- Ursachen für die Gerätestimme behoben: (a) Web prüfte vor dem Abspielen den AudioContext – der war kurz nach dem Antippen oft noch nicht „running“ → Browserstimme;
  jetzt nur noch „hat der Nutzer schon getippt?“. (b) Oberflächen-Sätze lagen nicht vorab vor → /jetzt; jetzt alle vorab. (c) Ein einzelner Netz-Aussetzer führte sofort zur
  Gerätestimme; jetzt ein zweiter Versuch nach 0,8 s. Gerätestimme nur, wenn Nova auch dann nicht kommt – mit einmaligem Hinweis „Nova gerade nicht erreichbar“.
  (Marcels Handy lief außerdem noch auf einer alten Version vor Nova.)
- **Nova-Abdeckung: 100 %** – alle 2579 Sätze aus texte.json (test) haben ihre Nova-Datei (HEAD 200, geprüft nach dem Lauf des Stimmen-Werks).

## 17.11.2026 – Version 2.4.1 vom 17.11.2026: fertig für den Betriebstest (Musik + nur Nova)

1) **Musik** (seit 2.4.0): Android (native App, kein WebView) startet die Musik sofort beim Öffnen – folgt nur der Medien-Lautstärke, nicht mehr dem
   Klingelmodus/Vibration. Web: startet automatisch beim allerersten Tippen/Klick irgendwo. Kein Musik-Knopf; „Musik aus“ wird respektiert.
   Neue Musik mit komponierten Melodien (Marcel fand die alte „grausam/hypnotisch“).
2) **Nur Nova**: Codesuche nach „thorsten“ und „sherpa“ (Quellcode, Ressourcen, gebaute WebApp) = **0 Treffer**; nur CHANGELOG und dieses Meldungs-Protokoll
   (Versionsgeschichte) nennen Thorsten noch. Keine Thorsten-/Sherpa-Dateien, -URLs oder -Auswahl. Fallback: Gerätestimme. Alte Stimmdatei wird auf Android
   gelöscht, alter Browser-Speicher in der WebApp ebenfalls.
Geprüft: Tests grün (Logik, Bildschirmfotos, Audio), Lint ohne Fehler, beide APKs gebaut. Liegt auf test.

## 17.11.2026 – Version 2.3.3 vom 17.11.2026: Gratis-Regel ergänzt (Marcel)

- Auf Marcels Hinweis (Bio unter „Lernen nach Fach“ komplett gesperrt) und seine Wahl: zusätzlich zu Wasserinsel + erste 5 je Insel sind **in jedem Thema die ersten 2** frei.
  Frei jetzt **74 von 200** (vorher 52). Bitte Texte auf der Website/Produktseite anpassen, falls dort „52“ oder nur „erste 5 je Insel“ steht.
- Einstellungen: alle Kästen volle Breite. Test: jedes Thema hat seine ersten beiden Versuche frei (CatalogTest).

## 17.11.2026 – Version 2.3.2 vom 17.11.2026: fertig für den Betriebstest

**Fertiggestellt**: Nova (nur Nova, /jetzt mit Origin, Sprechtempo, Offline laden Standard/Bestqualität), Forscher-Pass-Abzeichen und Kasse, Datenschutz-Text (wörtlich), Web = Android.

**Geprüft**
- Alle Tests grün: Logik (core:model, inkl. Nova-Hash-Prüfwert, Typo, alte Einstellungen aus 2.1), Datenbank, 69 Bildschirmfotos, 79 Schmal-Bilder (320 dp), Lint Github + Play ohne Fehler, beide APKs gebaut.
- WebApp mit Playwright bei 320 px und 1280 px: Start, Rätsel des Tages komplett (Rätsel → Vermuten → Labor → Auflösung → Erklären → Merksatz), Einstellungen; Konsole ohne Fehler (nur WebGL-Hinweise des Test-Browsers), kein waagerechtes Scrollen.
- Offline/Flugmodus: Service Worker aktiv, nach Neuladen ohne Netz startet die App und Versuche öffnen sich.
- Merken: Musik aus und Sprechtempo 1,3 bleiben nach Neuladen erhalten (localStorage „experimentopia.klang“).
- Freischalten: ?pro=-Link mit Test-Schlüsselpaar (eigener Testbau, nicht ausgeliefert) → „Forscher-Pass aktiv“, Code bleibt nach Neuladen, Link wird aus der Adresszeile entfernt.
- Update von der Vorversion: Datenbank seit 1.0 unverändert (Version 1), alte Klang-Einstellungen (Thorsten, Tempo 0,8) werden gelesen, Tempo rastet auf 0,75 ein; Android löscht die alte Thorsten-Stimmdatei (ca. 63 MB).
- texte.json unverändert (2301 Sätze) – nichts muss neu vertont werden.

**Behoben in 2.3.2**: Wortumbrüche bei 320 px (Startseiten-Knöpfe, „5 %“, Theorie-Knopf, „ziemlich sicher“), 🔊 überdeckte Text in Sprechblasen, „+0 %“ → „±0 %“, Thorsten-Reste auf Android.

**Bekannte Restpunkte**
- Nova auf einem echten Android-Gerät (Probe hören, Offline laden) nur von Marcel testbar.
- Lokal (localhost) blockt der Stimmen-Server den Zählstand per CORS – auf test/live erlaubt, kein Fehler im Betrieb.
- Der Thorsten-Speicher im Browser wird seit 2.2.0 gelöscht; Android erst ab 2.3.2.

## 17.11.2026 – Version 2.3.1 vom 17.11.2026: Bestqualität offline

- Zwei Knöpfe „Offline laden – Standard (ca. X MB)“ (/nova/…, "bytes") und „Offline laden – Bestqualität (ca. Z MB)“ (/nova/original/…, "bytes_original"; 404 → normale Datei).
  Wahl gemerkt (AudioPrefs „best“), beide Fassungen liegen unter demselben Satz-Schlüssel, „Offline-Stimme löschen“ löscht alles. Normales Vorlesen online bleibt Standard.
  Bildschirmfoto: screenshots/65_musik_und_vorlesen.png.

## 17.11.2026 – Version 2.3.0 vom 17.11.2026: Alle Nova-Texte offline laden

- Einstellungen → Vorlesen → „📦 Ohne Internet vorlesen“: Knopf „Alle Nova-Texte offline laden (ca. X MB)“, X = live "bytes" aus stand-<host>.json (Android: experimentopia.mathenachhilfe-online.com,
  Web: eigener Host) minus schon Gespeichertes; Hinweis „am besten im WLAN“; Fortschritt „x von n“, Abbrechen, Weitermachen beim nächsten Tippen; 4 parallel; 404 still übersprungen;
  „Offline-Stimme löschen (Y MB)“ sobald etwas gespeichert ist. Liste = dieselben Sätze wie /vorlesen/texte.json (aus Nova.catalog()), /jetzt-Texte nicht dabei.
  Speicher: Android files/nova/, Web Cache Storage „experimentopia-nova“. Bildschirmfoto: screenshots/65_musik_und_vorlesen.png.

## 17.11.2026 – Version 2.2.1 vom 17.11.2026: Sprechtempo

- Regler „Sprechtempo“ mit den Stufen 0,75 · 0,9 · 1,0 (Standard) · 1,15 · 1,3 · 1,5, gemerkt. Web: <audio> mit playbackRate + preservesPitch (nova.js spielt jetzt über HTMLAudioElement),
  Android: MediaPlayer PlaybackParams(speed, pitch 1). Gerätestimme als Notlösung: Sprechrate × gleicher Faktor.

## 17.11.2026 – Version 2.2.0 vom 17.11.2026: nur noch Nova

- **texte.json** liegt unter `/vorlesen/texte.json` (live und test), bei jedem Bau aus den Inhalten erzeugt: 2301 Sätze, 192 346 Zeichen, je Satz ≤ 300 Zeichen
  (Texte werden an Satzgrenzen geteilt, damit jedes Stück auch über /jetzt gehen kann), NFC geprüft, Formeln/Einheiten ausgeschrieben, ohne Emojis.
  Hash wie vorgegeben, Prüfwert „Hallo Welt“ → cb1e6d67… im Test.
- Abspielweg Web = Android: Gerätespeicher → /nova/… → POST /jetzt (nur Text ≤ 300) → still Geräte-/Browserstimme. Android schickt bei /jetzt den Header
  `Origin: https://experimentopia.mathenachhilfe-online.com` (ohne Origin antwortet der Server 403 „Herkunft“) – bitte so zulassen.
- Thorsten entfernt (sherpa-onnx, onnxruntime-web, Lautschrift, Stimm-Download); der alte Browser-Speicher „experimentopia-stimme“ wird gelöscht. Android-APK ca. 1,7 MB.
- Datenschutz-Text wörtlich übernommen (App und WebApp). Stimmenwahl zeigt nur „Nova – KI-generierte Stimme“, Vorlesen bleibt abschaltbar. Kein Offline-Komplettdownload (optional, vorerst nicht).

## 17.11.2026 – Version 2.1.1 vom 17.11.2026: „Forscher-Pass aktiv“ sichtbar, Rätsel nachlesbar, Update-Prüfung bei jedem Start

- Rätsel-Karte „📜 Rätsel nochmal lesen“ in allen Schritten; Android prüft Updates bei jedem Öffnen (mind. 5 Min. Abstand) und zeigt ein gefundenes Update sofort als Banner auf dem Startbildschirm (bleibt gespeichert bis installiert). Web aktualisiert sich weiter selbst über den Service Worker.
- **Web = Android** (2.1.1). Nach dem Freischalten: Abzeichen „💛 Forscher-Pass aktiv – alle Inseln und Geräte sind offen!“ auf dem Startbildschirm (unter dem Kopf)
  und im Eltern-Bereich der Einstellungen („Forscher-Pass aktiv – auf diesem Gerät freigeschaltet“). Erscheint sofort nach dem Einlösen.
- Kasse (Produkt 4143) seit 2.1.0 drin, Android-Stimme seit 2.0.2 auf `/thorsten/sherpa/`. Liegt auf `experimentopia-test`.
  **Bitte den Gratis-Testcode schicken** – damit prüfe ich das Freischalten Ende-zu-Ende.

## 17.11.2026 – Version 2.1.0 vom 17.11.2026: Forscher-Pass mit Kasse (bereit für den Live-Gang)

- **Web = Android** (2.1.0). Kasse eingetragen: `checkoutUrl=https://www.mathenachhilfe-online.com/kasse/?bestellweg=aendern&add-to-cart=4143` (Produkt 4143, 7,99 €).
  Ab jetzt in WebApp und GitHub-APK: frei 52 von 200 Versuchen (ganze Wasserinsel + je Insel die ersten 5), Rätsel des Tages, Lernen nach Fach, Theorie, Themensuche,
  Forscherbuch, Musik, Vorlesen, 6 Geräte im Freien Labor; Bestandsschutz für schon gemachte Versuche. Play-Fassung bleibt ganz frei (noch kein Google-Kauf).
- Kauf nur nach Elternfrage (Geburtsjahr, ab 18); Freischalten per Link `https://experimentopia.mathenachhilfe-online.com/?pro=<Code>` oder Code-Eingabe (EX-…).
- Android-Stimme ist seit 2.0.2 auf `/thorsten/sherpa/` umgestellt; liegt auf `experimentopia-test`. **Gern einen Gratis-Testcode schicken** – ich prüfe damit das Freischalten.
  Aus meiner Sicht kann 2.1.0 live gehen, sobald euer Test grün ist.

## 17.11.2026 – Version 2.0.2 vom 17.11.2026: Android-Stimme vom Stimmen-Server, Testseite repariert

- **Web = Android** (2.0.2). **Android lädt Thorsten jetzt von** `https://stimmen.mathenachhilfe-online.com/thorsten/sherpa/de_DE-thorsten-medium.onnx`
  (63 201 421 Bytes, sha256 1c305abe…65a6, geprüft; tokens.txt identisch mit eurer, sha256 2619c1a9…ec36). **Hugging Face kommt in App, WebApp und Datenschutz-Text nicht mehr vor.**
- **Warum die Testseite auf 0.6.0 stand:** Unser Workflow „WebApp → Test“ (spult `experimentopia-test` bei jedem Push nach) ist seit 0.7.0 an eurer Dateityp-Prüfung gescheitert:
  Die Lautschrift lag als `.tsv` vor. Jetzt `.txt` – die Prüfung läuft wieder, `experimentopia-test` wird ab 2.0.2 wieder automatisch nachgezogen (direkt pushen darf diese Sitzung nicht, das erledigt der Workflow).
  Nebenbei: Ohne die Umbenennung hätte Thorsten in der WebApp auf eurem Server nicht starten können (`.tsv` wird nicht ausgeliefert). Danke fürs manuelle Nachziehen!
- Offen: Kassen-Link für den Forscher-Pass – sobald er da ist, baue ich die Version mit Kasse.

## 17.11.2026 – Version 2.0.1 vom 17.11.2026: Thorsten vom gemeinsamen Stimmen-Server

- **Web = Android** (2.0.1). **WebApp** lädt Thorsten jetzt von `https://stimmen.mathenachhilfe-online.com/thorsten/de_DE-thorsten-medium.onnx`, Größe 63 201 294 Bytes und
  sha256 7e64762d…7819 werden geprüft; die Laut-Nummern eurer `.onnx.json` stimmen mit unserer Zeichenliste überein (152/152, geprüft). Danach offline (Browser-Speicher).
  Wer die Stimme schon von Hugging Face geladen hatte, behält sie. Hinweis in der App: „ca. 75 MB – am besten im WLAN“. Datenschutz-Text der WebApp angepasst.
- **Android lädt vorerst weiter von Hugging Face:** sherpa-onnx braucht die Modell-Fassung mit Zusatzangaben (Metadaten wie sample_rate, model_type, comment=piper).
  Eure Datei hat keine Metadaten (geprüft) – damit würde Thorsten auf Android nicht starten. **Bitte zusätzlich die sherpa-Fassung auf den Server legen:**
  `de_DE-thorsten-medium.onnx` aus `csukuangfj/vits-piper-de_DE-thorsten-medium`, 63 201 421 Bytes, sha256 1c305abe13dda2f50a9783da3e7645396833cdf2d09f24fa9a823c91293d65a6
  (z. B. unter `…/thorsten/sherpa/`). Dann stelle ich Android ebenfalls um. Kein espeak-ng in App oder WebApp (Lautschrift vorab erzeugt).

## 17.11.2026 – Version 2.0.0 vom 17.11.2026 (neues Versionsschema, Forscher-Pass vorbereitet)

- **Versionsnummer: 2.0.0** (versionCode 9). Ab jetzt 2.x.y für kleine Updates, 3.0.0 erst beim großen Update. **Web = Android** (gleiche Nummer, gleiche Inhalte).
  Enthält alles aus 0.7.0 (Musik, Vorlesen mit Thorsten) – liegt auf `experimentopia-test` zum Prüfen.
- **Preis von Marcel freigegeben:** Forscher-Pass **7,99 € einmalig** (6,71 € netto + 1,28 € USt.), kein Abo.
- **Pro technisch fertig nach euren Vorgaben** (Vorbild `kurvenflug/pro/`): Code-Version 1, App-Byte 6, Präfix „EX-“, Produkt 1 = Pro (Produkt 2 = Schullizenz wird wie bei Kurvenflug ebenfalls angenommen),
  Ed25519 mit eurem öffentlichen Schlüssel, Base32-Alphabet wie vorgegeben, Freischalt-Link `https://experimentopia.mathenachhilfe-online.com/?pro=<Code>`,
  Elternfrage (Geburtsjahr, mindestens 18) vor dem Kauf, Bestandsschutz. Werkzeug: `experimentopia/pro/pro_code.py`.
- **Bitte eintragen:** die Woo-Kasse für das Produkt „Experimentopia Forscher-Pass“ in `experimentopia/pro/pro.properties` (`checkoutUrl=`).
  Solange sie leer ist, bleibt alles frei – nichts wird gesperrt. Mit Eintrag wird beim nächsten Build gesperrt (frei bleiben 52 von 200 Versuchen plus alles Übrige oben).

## 17.11.2026 – Version 0.7.0 vom 17.11.2026: eigene Musik und Vorlesen (Aufträge von Marcel)

**Web = Android:** gleicher Code (Compose Multiplatform), gleiche Version 0.7.0, gleiche Texte, gleiche Musik, gleiche Stimmen-Auswahl inklusive „Thorsten“.

**Musik – wie sie entsteht:** komplett im Code komponiert und erzeugt (`shared/.../fx/Music.kt`), keine Samples, keine fremden Dateien.
Klang-Identität „verspieltes Forscher-Labor“: Pizzicato, Klarinette, Fagott, Glas, Marimba, Blubbern. Vier Welten mit eigener Tonart, eigenem Tempo und eigenen Instrumenten:
Labor C-Dur, Physik G-Dur (Uhrwerk), Chemie D-dorisch, Biologie F-lydisch. Jede Phrase (8 Takte, etwa 12 Sekunden) wird nach Regeln neu erfunden
(Akkordfolge der Welt, Rhythmus-Vorrat, Akkordtöne auf betonten Zählzeiten, kleine Schritte, gelegentliche Vorschläge), danach 4–9 Sekunden Stille – keine Schleife.
Dazu eine Erkennungsmelodie (Klarinette steigt auf, Pizzicato antwortet, Glas funkelt, Blubb). Im Versuch selbst ist es still.
**Effekte:** Reagenzglas, Blubbern, Zischen, Funken, Messgerät, Heureka (Entdeckung), richtig, falsch (sanftes Fagott). Alles neu im Code erzeugt.
**Bedienung:** Musik und Effekte getrennt schaltbar mit eigener Lautstärke; Start erst nach der ersten Berührung; folgt der Stummschaltung; leiser beim Vorlesen; kein Inhalt nur über Ton.
**Schnell:** Musik wird erst nach dem ersten Antippen komponiert, kein Einfluss auf Start und Ladegröße.

**Vorlesen:** Anleitungen, Beobachtungsfragen, Erklärungen, Merksätze, Theorie, Sicherheitshinweise – per 🔊-Knopf oder automatisch (abschaltbar).
Stimmen des Geräts/Browsers oder „Thorsten“ (natürlich), Tempo, Probe-Taste; die Wahl wird gemerkt. Formeln, Einheiten und chemische Formeln werden richtig gesprochen (`Speech.kt`, Test).
**Thorsten technisch:** Android wie Raumlicht (sherpa-onnx 2.0.1, ohne espeak-ng, Lautschrift vorab erzeugt). Web mit onnxruntime-web 1.20.1 (MIT, selbst gehostet in `webapp/ort/`,
11 MB, nur bei Bedarf geladen). Stimmdatei (63 MB, CC0/MIT) nur auf Wunsch von Hugging Face, mit Prüfsumme, im Web im Browser-Speicher. Datenschutz-Text angepasst.
Hinweis: Die Android-APK wird durch die Sprach-Bibliothek größer: 1,7 MB → 20,7 MB (Maschinencode nur für echte Handys, komprimiert eingepackt; wie Raumlicht).
Die WebApp lädt beim Start nichts davon – Rechenbaustein (11 MB) und Stimmdatei kommen erst, wenn jemand Thorsten wählt.
Geprüft im Browser: Thorsten lädt, prüft die Datei und spricht; Musik startet erst nach dem ersten Antippen; keine Konsolenfehler.

**WebApp füllt die Seite:** geprüft bei 360×740, 740×360, 820×1180, 1366×768 und 1920×1080 sowie nach Größenänderung – die Fläche füllt das Fenster exakt,
keine Ränder, keine Scrollleisten. Auf breiten Bildschirmen steht der Inhalt jetzt in einer lesbaren Spalte (höchstens 720 dp) in der Mitte, der Hintergrund füllt den Rest.

## 17.11.2026 – Version 0.6.0 vom 17.11.2026: Theorie-Knopf zu jedem Versuch und Lernen nach Fach (Wünsche von Marcel)

- **Lernen nach Fach:** Physik, Chemie, Biologie auf dem Startbildschirm. Fachbereiche in Schulstoff-Reihenfolge (nach Einstiegs-Klassenstufe), gefiltert nach Klasse,
  je Bereich Lernziel, Fortschritt, „Weiter“ zum nächsten passenden Versuch, Theorie und Versuchsliste.

- Jeder Versuch hat „📖 Theorie“ (oben in der Leiste, bei der Vermutung, im Labor; dazu in Themensuche und Forscherbuch).
- 19 Theorie-Karten (eine je Fachbereich): Grundidee, Punkte mit Bildern, Alltagsbeispiel, Fachwörter, aufklappbarer Teil „Für Profis (ab Klasse 7)“ mit Formeln.
  Kein Verraten der Versuchslösung; Alltagsbeispiele bewusst anders als in den Versuchen. Inhalte fachlich wie in der Korrektheitsprüfung abgeglichen.

## Korrektheitsprüfung 17.11.

Version **0.5.1 vom 17.11.2026** (Android-App und WebApp, gleiche Version). Geprüft wurde der gesamte Stand: 200 Versuche auf 8 Inseln und 34 Geräte.

### Was geprüft wurde und wie
1. **Fachlich (unabhängige Zweitprüfung):** Vier getrennte Prüfer haben ohne meine Vorarbeit jede Zeile gelesen. Aufgeteilt war nach (a) Wasser, Mechanik, Strom und Magnet, (b) Chemie und Stofftrennung, (c) Wärme, Licht, Schall, Fall und Malkasten, (d) Biologie.
   Geprüft gegen verlässliche Quellen (Lehrerfortbildung BW, NABU, LfU Bayern, NASA Fact Sheets, Sicherheitsdatenblätter u. a.). Alle berechneten Antworten haben sie nachgerechnet, Fallzeiten und Räuber-Beute-Kurven in einem eigenen Nachbau.
   Zusätzlich gibt es automatische Tests: Jede richtige Antwort wird aus den Modellen berechnet und in `CatalogTest`, `PhysicsTest`, `BiologyTest` und `Biology2Test` geprüft (jetzt auch Sicherheitshinweise, Trägheit und Mikroskop-Sehfeld).
2. **Sprache:** Rechtschreibung, Grammatik, Du-Anrede, Fachbegriffe und Platzhalter, bei allen Texten durch die vier Prüfer. Einheitlich ist jetzt „violett“ statt „lila“ beim Rotkohl, Einheiten und Bindestriche sind korrigiert.
3. **Funktion:**
   - Android: 59 Bildschirmfotos (Robolectric) angesehen.
   - Neu: Prüfung bei 320 dp für Start, alle 8 Inseln und alle 34 Geräte, jeweils im Versuch und im Freien Labor (77 Bilder, `NarrowTest`).
   - Web: Chromium bei 320 und 400 px – kein weißer Bildschirm (Hintergrund ab dem ersten Bild dunkel), keine waagerechte Scrollleiste, keine Konsolenfehler.
   - Lint ohne Fehler, beide APKs gebaut.
4. **Barrierefreiheit:**
   - Kontraste aller Textfarben auf den Hintergründen nachgemessen: mindestens 4,5 : 1 (WCAG AA), Fließtext 12–16 : 1.
   - Tippflächen mindestens 32 dp (WCAG 2.2 verlangt 24 px).
   - Alles lässt sich auch ohne Wischen bedienen (Knöpfe statt Gesten).
   - Ton und Vibration sind abschaltbar.
5. **Standards:**
   - Web = Android: gleicher Code, gleiche Version 0.5.1.
   - Ladebild ohne weißen Bildschirm.
   - Stufen-Nachweis: Jeder Versuch hat eine Klassenstufe, ein Test prüft die Angabe jeder Insel.
   - Datenschutz: keine Daten nach außen, nur lokale Speicherung.
   - Keine GPL, keine kopierten Schulbuchtexte.
6. **Test-Adresse:** Der Branch `experimentopia-test` enthält `experimentopia/webapp/` und wird bei jedem Push automatisch nachgezogen. Vor dieser Prüfung lag dort Version 0.5.0, nach dem Push ist es 0.5.1.

### Abweichungen und Korrekturen (vorher → nachher)

**Echte Fehler**
| Versuch | vorher | nachher |
|---|---|---|
| Mikroskop (u1–u5) | Sehfeld = 22 mm ÷ Gesamtvergrößerung (10-mal zu klein); u3: „etwa 30 Bakterien passen nebeneinander“ | 22 mm ÷ Objektiv-Vergrößerung (bei 400-fach 0,55 mm); u3: „etwa 300“ |
| n9 Trägheit | Text „kein Motor“, im Labor zog aber ein Motor den Wagen | eigener Laborteil: einmal schubsen, dann rollen, spiegelglatt gegen Teppich |
| l9 „Alle meine Entchen“ | zweites „A A A A G“ fehlte | Melodie vollständig |
| j4 Nahrungsnetz | Waldkauz frisst nur Mäuse (falsch, er ist ein Allesfresser) | Schleiereule (fast nur Mäuse) |
| w8 Dichteturm | „Egal in welcher Reihenfolge …“ (Spiritus mischt sich mit Wasser) | „Vorsichtig eingegossen gilt …“, Merksatz angepasst |
| w13 | falsche Begründung „54 g ist mehr als 20“ war in Wahrheit richtig (Archimedes) | „Glänzende Würfel sinken immer.“ |
| c16, c3 | „schwache“ / „starke“ Säure bzw. Lauge für die Konzentration | „verdünnte“ / „konzentriertere“ |
| c22 Thermit | „ein Funke, und …“ (ein Funke zündet Thermit nicht) | „mit einem brennenden Magnesiumband gezündet“ |
| h6 | „Glühendes Eisen … Zisch!“ bei 100 °C | „Heißes Eisen … Plumps!“ |
| h14 | „kälter geht immer“ | „… bis zum absoluten Nullpunkt (−273 °C)“ |
| e7 Merksatz | „Eiweiße nehmen ab 42 °C Schaden“ (widersprach dem Versuch, Amylase arbeitet bei 50 °C) | „Hitze zerstört Enzyme dauerhaft (Denaturierung)“ |
| o1 | Wasser wandert „durch die Zellwand“ | „durch die Zellhaut (Zellmembran)“ |

**Ungenau (präzisiert)**
- **c2:** pH „0–6 sauer, 8–14 basisch“ → „unter 7 / genau 7 / über 7“.
- **c1:** basisch „grün/gelb“ → „blau, grün oder gelb“.
- **c18:** „jede Verzehnfachung +1“ → „anfangs +1, nahe 7 immer weniger“.
- **c19:** Elektrolyse jetzt mit Natron und Batterie statt „Strom“.
- **c4:** „von 2“ → „von gut 2“.
- **c9:** „Verbrennen …“ → „mit genug Sauerstoff“.
- **c22:** „unedleres Metall nimmt einem edleren …“.
- **k18:** „ohne Reibung“ ergänzt; 245 N · 2 m ≈ 491 J (ungerundet 490,5 J).
- **w7:** Knobelfrage als Gedankenversuch („Angenommen, gleiche Dichte …“).
- **Spiritus:** Dichte 0,79 → 0,81 g/cm³ (Brennspiritus).
- **Magnete:** „Eisen und die meisten Stahlsorten (manches Edelstahl kaum)“.
- **s20:** „Messingschlüssel“; **s4:** „UND“ → „egal ob … oder …“.
- **k10:** Merksatz erklärt einseitigen Hebel.
- **k11:** „Nuss 6 kg“ → „Knackkraft wie 6 kg“.
- **k13:** Mobile im Museum statt über dem Babybett.
- **n7:** Impuls „gleich groß, entgegengesetzt“.
- **s12:** „etwas dunkler“ → „deutlich dunkler“.
- **Recyclinghof** → Sortier- bzw. Recyclinganlage.
- **Balsa:** „eines der leichtesten Hölzer“.
- **l18:** Grundton 262 Hz genannt (sonst war die Frage nicht lösbar).
- **l3:** „gelbes Licht wirkt im Auge wie Rot + Grün“; **l2:** „fast jede Lichtfarbe“.
- **h10, h12, h6:** „spezifische Wärmekapazität“.
- **h12:** falsche Begründung „Eisen leitet schlechter“ (die stimmt) ersetzt.
- **h13:** „kostet Kraft“ → „kostet Energie“.
- **f6:** √(9,81 ÷ 1,62) ≈ 2,46.
- **f10:** Hinweis, dass ein Ballon im Vakuum platzt.
- **f15:** „Dauer fast gleich“ statt „immer gleich“.
- **m2:** „fast Schwarz“ → „ziemlich dunkel“ (passt zur gezeigten Farbe).
- **m4:** Ziel heißt einheitlich „Rosa“.
- **p5/p6:** Hase-Luchs-Zyklus jetzt in Kanada. Knobelfrage über Räuber in Deutschland auf Rehe umgestellt (der Luchs frisst hier vor allem Rehe).
- **j2:** Ringelnatter frisst „vor allem Frösche, manchmal Mäuse“.
- **o1:** Merksatz Osmose eindeutig formuliert.
- **e7:** „kocht auf (80 °C)“ → „erhitzt auf 80 °C“.
- **p3:** Fotosynthese-Gleichung mit Licht als Energie; Antwort „Gegen Algen“ (teilweise richtig) ersetzt.
- **u4:** Antwort „Algen, die es gefressen hat“ (teilweise richtig) ersetzt.
- **Apfel:** „reife Äpfel“ (unreife enthalten Stärke).
- **q3:** abgekochtes Wasser, „keimen nicht – ihnen fehlt Luft“.
- **Kresse:** keimt ab 5 °C statt 2 °C.
- **u2:** „ein kleines Stück vom Floh, z. B. das Auge“.
- **u5:** Größenvergleich Amöbe und Pantoffeltierchen ergänzt.
- **g1:** „vor über 160 Jahren“.
- **t6:** Filterpapier nur mit dem Rand ins Wasser.
- **t7:** Antwort „Kühlschrank“ (manche haben Filter) → „Toaster“.
- **c14:** Hinweis, dass man die Farbe beim Kaffee schlecht sieht.

**Sprache:** „einen Viertelmillimeter“, „100 000-mal“, „Wievielmal so lange“, „Wie lange braucht“, „näher an der Temperatur der größeren Menge“, „Tausendstelsekunden“, „ein Sechstel so groß“,
„Mia wiegt nur ein Drittel so viel“, „eine 40-kg-Kiste“, „Feld 4“ statt „ganz außen“, Kraftmesser-Satz, „macht Lebensmittel haltbar“, Schwimmweste-Frage, „Blubb lässt … fallen“.

**Sicherheit (neu):**
- Bisher gab es keine Sicherheitshinweise. Jetzt zeigen **72 Versuche** einen gut sichtbaren Hinweis „⚠️ In echt: Sicherheit zuerst“, im Versuch und im Forscherbuch (`Safety.kt`).
- Inhalte:
  - Strom nur mit Batterie, nie Steckdose; Kurzschluss heiß.
  - Säure/Lauge nur mit Schutzbrille im Chemieraum; erst das Wasser, dann die Säure.
  - Knallgas, Thermit und Magnesium nur Lehrkraft/Fachleute.
  - Elektrolyse ohne Kochsalz (Chlor); Wasserstoffperoxid; Spiritusflamme fast unsichtbar.
  - Herd, Karamell, 60-°C-Wasser, heißes Metall.
  - Prisma: nie in die Sonne schauen; nie etwas von Türmen werfen.
  - Iod und Biuret; Speichel hygienisch.
  - Teichwasser nur mit Erwachsenen; Puls am Hals sanft.
  - Quecksilber giftig.

**Oberfläche und Technik:**
- Inselpfade bei 320 dp: Wörter brachen mitten im Wort um („Wasse-r“) → Pfad schlängelt sich nur so weit, dass 160 dp für den Titel bleiben.
- Lange Titel wurden abgeschnitten → dürfen zweizeilig werden.
- Blubb sagte „sieben Inseln“ → zählt jetzt selbst („acht“).
- Mikroskop: Lampen-Schalter direkt unter dem Bild; Augentierchen schwimmen sichtbar zum Licht. Gezeichnet wird jetzt genau, was man erkennt:
  Mit bloßem Auge erschien der Wasserfloh vorher groß, obwohl daneben „nur ein winziger Punkt“ stand.
- Magnet-Labor bei 320 dp: Gegenstände und Namen überlappten sich, die vierte Reihe ragte aus der Fläche → 3 je Reihe, lange Namen zweizeilig, Fläche wächst mit.
- Eingabefelder ohne Einheit zeigten leere Klammern („Hasen ()“) → nur noch der Name.
- Rotkohl-Labor: „lila“ → „violett“ (wie in den Antworten).
- Atom-Baukasten: Die Reaktionsgleichung ist bei 320 dp breiter als der Bildschirm; man kann sie seitlich verschieben (war schon so, geprüft).
- Web: Ton-Warnung in der Konsole vor dem ersten Antippen beseitigt.
- Play-Fassung: Berechtigung „Apps installieren“ entfernt (nur noch GitHub-Fassung mit Selbst-Update).

### Was nicht (vollständig) prüfbar war
- Echte Geräte: kein iPhone/iPad, kein echtes Android-Handy, nur Robolectric und Chromium.
- Bildschirmleser im Web: Compose Multiplatform zeichnet in ein Canvas, die Unterstützung für Bildschirmleser ist dort noch eingeschränkt. Auf Android tragen die Knöpfe Text.
- Reale Messwerte, die von Hersteller und Sorte abhängen: Rf-Werte echter Filzstifte, Farbskalen von Glukose-Teststreifen, Rotkohl-Farbgrenzen (in der Literatur uneinheitlich).
  Die App nutzt übliche Schulwerte.
- Vereinfachte Modelle (bewusst, für Klasse 3–10): Räuber-Beute (Lotka-Volterra), Enzymkurve, Erholungspuls (eher optimistisch), Keimdauer.

### Vorstellung (aktueller Stand)
- **Was:** Experimentopia – ein virtuelles Versuchslabor. Kinder vermuten, probieren aus und erklären dem Laborkolben Blubb, warum etwas passiert (Predict–Observe–Explain).
  200 Versuche auf 8 Inseln: Wasser, Kraft, Strom, Chemie, Wärme, Licht & Klang, Fallturm, Bio-Dschungel. Dazu 34 Geräte im Freien Labor mit eigenen Werten, Themensuche (19 Fachbereiche), Forscherbuch und Abzeichen.
- **Für wen:** Klasse 3–10, gestaffelt; Eltern und Lehrkräfte.
- **Technik:** Kotlin, Compose Multiplatform (Android + WebApp aus einem Code), Room, Service Worker mit Auto-Update, GitHub-Releases mit Updater. Keine Werbung, kein Tracking.
- **Zeitplan:** 0.1–0.5 stehen. Als Nächstes Feinschliff nach euren Rückmeldungen. Pro erst nach Freigabe des Preises.
- **Pro-Konzept (unverändert, wartet auf Freigabe):**
  - Frei bleiben die ganze Wasserinsel und je Insel die ersten 5 Versuche (52 von 200), dazu Rätsel des Tages, Forscherbuch und Themensuche.
  - Forscher-Pass 7,99 € einmalig (6,71 € netto + 1,28 € USt), Einführung 4,99 €.
  - Freischalt-Code nach euren Vorgaben, Eltern-Abfrage.

### App-Karte
- **Name:** Experimentopia
- **Emoji:** 🧪
- **Zielgruppe:** Klasse 3–10 · Physik, Chemie & Biologie
- **Ein Satz:** Das Versuchslabor für die Hosentasche: 200 Versuche vermuten, ausprobieren und dem Laborkolben Blubb erklären.


## 17.11.2026 – Version 0.5.0 vom 17.11.2026: Mikroskop, Keimung, Nahrungsketten, Herz & Puls

- 4 neue Geräte im Bio-Dschungel, **200 Versuche**: Mikroskop (Sehfeld 22 mm ÷ Vergrößerung, Erkennbarkeit ab 5 mm scheinbarer Größe), Kresse-Keimung
  (Wasser, Wärme, Luft; Licht nicht nötig, Vergeilen), Nahrungsketten (Nahrungsnetz einer Wiese, 10-%-Regel, Anreicherung), Herz & Puls (Ruhepuls nach Alter, ca. 5 l/min, Erholungspuls).
- Puls-Messen per Antippen läuft komplett auf dem Gerät, es wird nichts gespeichert oder gesendet.

## 17.11.2026 – Version 0.4.0 vom 17.11.2026: Bio-Dschungel, Stoffe trennen, Malkasten

- **Neue Insel Bio-Dschungel** (Kl. 3–10, neues Fach Biologie): Vererbung nach Mendel mit Kreuzungsquadrat und Aussaat, Blutgruppen, Osmose/Plasmolyse,
  Nährstoffnachweise, Enzym Amylase, Fotosynthese (Wasserpest), Räuber-Beute. Dazu **Stoffe trennen + Chromatografie** (Chemie) und **Malkasten** (Licht & Klang).
- **180 Versuche** (vorher 142), 18 Fachbereiche, 30 Geräte im Freien Labor. Alle Antworten aus Modellen berechnet und getestet (neu: `BiologyTest`).
- Grundlage: Lehrpläne/Beispielcurricula (BW Genetik 9/10, Nährstoffnachweise 5/6, Stofftrennung 5/6) – Quellen in `docs/RECHERCHE.md`. Eigene Texte, nichts kopiert.
- **App-Karte bitte anpassen:** „Klasse 3–10 · Physik, Chemie & Biologie“.

## 16.11.2026 – Version 0.3.0 vom 16.11.2026: doppelt so viele Versuche, Themensuche

- **142 Versuche** (vorher 71) in 13 Fachbereichen, neu: Newtonsche Gesetze (Wagen mit Motor, Skateboards). 7 neue Reaktionsgleichungen, neue Stoffe.
- **Themensuche** (Fach, Fachbereich, Stichwort); Inseln nach Thema gegliedert. Forscherbuch mit ausführlicher Erklärung je Versuch.
- Bedienung nach einem Durchgang „als Kind“ vereinfacht (Antippen schaltet um, Knöpfe statt nur Wischen, Frage + Vermutung im Versuch sichtbar).
- Fachlich korrigiert: Luftdichte von Mars und Jupiter (vorher fälschlich die der Erde).
- **Pro-Konzept angepasst:** frei bleiben jetzt die ganze Wasserinsel und von jeder anderen Insel die ersten 5 Versuche (zusammen etwa ein Drittel),
  dazu Themensuche, Rätsel des Tages, Forscherbuch. Preisvorschlag unverändert (7,99 € einmalig). Elterntext jetzt: „Zurzeit ist alles kostenlos.“
- Für die App-Karte: Zielgruppe weiter „Klasse 3–10 · Physik & Chemie“, Beschreibung gern ergänzen: „142 Versuche, von Schwimmen und Sinken bis zu Newton und Reaktionsgleichungen.“

## 16.11.2026 – Vorstellung und erster Web-Stand: Version 0.2.0 vom 16.11.2026

**Was liegt wo**
- Ordner `experimentopia/` in marceldaum-commits/AndroidApps. Web-Root: **`experimentopia/webapp/`** (index.html im Wurzelverzeichnis, nur statische Dateien, 15 MB, alle Dateiarten erlaubt).
- Entwicklungs-Branch `ccr-2ccbaa44-nkkmyu`; der Workflow `experimentopia-web-test.yml` prüft die WebApp und spult **`experimentopia-test`** vor. `experimentopia-live` fasse ich nicht an.
- Version 0.2.0 vom 16.11.2026: Android und Web gleich (eine Quelle: `app/build.gradle.kts`, der Workflow bricht ab, wenn `webapp/version.json` abweicht).

### 1. Was ist Experimentopia?
Ein virtuelles Versuchslabor für **Klasse 3 bis 10**, Fächer **Physik und Chemie** (Sachunterricht in Klasse 3/4).
Versuche, die zu Hause nicht gehen (Säure und Lauge, Knallgas, Fallen auf dem Mond, Flaschenzug mit Klavier), werden in der App ausprobiert.
7 Inseln, 67 Versuche (Stand 0.2.0), 20 Geräte im „Freien Labor“ mit eigenen Werten. Maskottchen: Blubb, ein Laborkolben.
Jeder Versuch folgt dem Forscher-Kreislauf: Vermuten (mit Selbsteinschätzung) → Ausprobieren → Auflösung → Blubb erklären (Lernen durch Lehren) → Merksatz → Knobelfrage.
Falsche Vermutungen kosten nichts, sie werden als „Aha-Moment“ gefeiert. Forscherbuch mit Auswertung der Selbsteinschätzung.

### 2. Technik und Zeitplan
- Kotlin, Compose Multiplatform: gemeinsame Oberfläche (`shared/`) für Android und Web (Kotlin/Wasm, `web/`), Rechenlogik in `core/model` (wie Raumlicht).
- **Android:** GitHub-APK mit eigenem Updater (beim Start höchstens alle 20 h, abschaltbar); Play-Fassung (`play`-Flavor) mit Play In-App Updates API, ohne Selbst-Installieren.
- **Web:** Ladebild inline in HTML/CSS ab dem ersten Byte, echter Ladebalken, Service Worker (offline, Update automatisch an ruhiger Stelle = Startseite oder Hintergrund).
  Getestet in Chromium: Start lokal nach 1,3 s, 320 px Breite ohne waagrechtes Scrollen, keine Konsolenfehler.
- Ton im Code erzeugt (Android: SoundPool/AudioTrack, Web: WebAudio mit denselben Klängen), keine Audiodateien.
- Zeitplan: 0.2.0 jetzt zum Test. Danach nach eurer Prüfung Feinschliff; Pro erst nach Freigabe (siehe 4).

### 3. Standards
1. Web = Android: erfüllt (eine Codebasis, gleiche Version).
2. Start ohne weißen Bildschirm: erfüllt (Ladebild, Ladebalken; Schriften auf benutzte Zeichen gekürzt: 116 KB + 359 KB).
3. Schwierigkeit: Es gibt **keine Schwierigkeitsstufen**, nur einen Klassenfilter (3–10), der Versuche hervorhebt. Ein Stufen-Test entfällt darum.
4. Fachliche Prüfung: Alle richtigen Antworten werden aus Physik- und Chemie-Modellen **berechnet** und in Tests gegen Lehrbuchwerte geprüft
   (z. B. pH-Sprung 11,68 bei 11 mL, Prisma-Mindestablenkung 38,6°, Pendel mit elliptischem Integral, Atombilanz aller 12 Reaktionen, Richmann, Fallzeiten mit und ohne Luft).
   Bitte trotzdem stichprobenartig gegenprüfen lassen: `core/model/src/jvmTest/.../CatalogTest.kt` listet jede erwartete Antwort.
5. Datenschutz: kein Konto, keine Werbung, keine Analyse, Daten nur im Browser bzw. auf dem Gerät. Text unten.
6. Barrierefreiheit: 320 px geprüft; Kontraste hell auf dunkel. Offener Punkt: Bedienung per Tastatur in den Laboren (Zeichenflächen) noch nicht vollständig.
7. Fremde Inhalte: alle Texte, Versuche und Zahlen selbst geschrieben. Bibliotheken: Kotlin, Compose Multiplatform, AndroidX, kotlinx (Apache 2.0); Schriften Noto Color Emoji und DejaVu Sans (OFL bzw. frei). Keine GPL.

### 4. Pro-Konzept (bitte Marcels Freigabe einholen – gebaut ist nur der Schalter, ausgeschaltet)
**Kostenlos bleibt der Kern:** die ganze Wasserinsel, von jeder anderen Insel die ersten 3 Versuche (27 von 67), das Rätsel des Tages (jeden Tag ein anderer der 67 Versuche),
Forscherbuch, Abzeichen und 6 Geräte im Freien Labor (Wasserbecken, Wippe, Stromkreis, Rotkohl, Licht, Fallturm).

**Pro („Forscher-Pass“):** alle 67 Versuche (die anspruchsvolleren für Klasse 7–10: Ohmsches Gesetz, pH-Sprung, Reaktionsgleichungen, Pendel, Mond), alle 20 Geräte mit eigenen Werten,
Atom-Baukasten mit allen 12 Reaktionen. Einmalkauf, kein Abo, nur hinter Eltern-Abfrage.

**Preisvorschlag:** **7,99 € einmalig** (Einführung 4,99 €).
| | brutto | netto | USt 19 % |
|---|---|---|---|
| Forscher-Pass | 7,99 € | 6,71 € | 1,28 € |
| Einführungspreis | 4,99 € | 4,19 € | 0,80 € |

Vergleich (Drittquellen, Stand und Länder unterschiedlich, bitte vor Veröffentlichung prüfen): Toca Lab: Elements 4,49 € einmalig (ein Thema, ohne Lernkontrolle);
ANTON-Plus 9,99 €/Jahr (alle Fächer, Abo); sofatutor rund 32 €/Monat (Abo). Experimentopia liegt als Spezialist (Physik/Chemie, Klasse 3–10) unter einem ANTON-Jahr, aber dauerhaft.

**Technik nach Freigabe:** Freischalt-Code wie vorgegeben (Version 1, App-Byte 6, Präfix „EX-“, Produkt 1 = Pro, Ed25519, euer Base32-Alphabet), Prüfung offline mit dem öffentlichen Schlüssel,
Freischalt-Link `?pro=<Code>`, Vorbild `kurvenflug/pro/`. Sperren erst, wenn ihr eine Kassen-URL eintragt. Play-Fassung vorerst ohne Kauf-Knopf. Kein KI-Tutor geplant.

### 5. Konzept Lehrerzugang (nicht gebaut)
- **Lehrkraft sieht:** je Schüler (Spitzname) erledigte Versuche, Sterne, Aha-Momente, ob die Erklärung beim ersten Mal stimmte, und die Selbsteinschätzung (wie oft „ganz sicher“ richtig war).
  Je Versuch: wie viele Kinder richtig vermutet haben – gut für den Einstieg in die nächste Stunde („Die Hälfte dachte, das Ei sinkt …“).
- **Lehrkraft steuert:** Aufträge = Versuch(e) bis Datum, optional nur bestimmte Inseln oder Klassenstufe; Rätsel der Woche für die Klasse.
- **Daten der App (so knapp wie möglich):** Klassen-Code, Spitzname, Versuch-ID, Sterne (0–3), Vermutung richtig ja/nein, Sicherheit (0–2), Erklärung beim ersten Mal ja/nein, Datum. Keine Texte, keine Geräte-Kennung, kein Name.
- **Beitritt:** In den Einstellungen „Klasse beitreten“ → Code eintippen oder QR scannen (Web: nur eintippen) → Spitznamen wählen. Austreten jederzeit, dann löscht der Server die Daten.

### 6. Für die App-Karte auf der Website
- **Name:** Experimentopia
- **Symbol:** 🧪
- **Zielgruppe:** Klasse 3–10 · Physik & Chemie
- **Beschreibung:** Das Versuchslabor für die Hosentasche: vermuten, ausprobieren und dem Laborkolben Blubb erklären, warum das Ei im Salzwasser schwimmt.

### 7. Abschnitt für die Datenschutzerklärung
> **Experimentopia (WebApp und Android-App):** Experimentopia speichert den Spielstand, das Forscherbuch und die Einstellungen ausschließlich lokal im Browser (localStorage) bzw. auf dem Gerät.
> Es gibt keine Anmeldung, keine Werbung und keine Analyse- oder Tracking-Dienste; es werden keine personenbezogenen Daten an uns oder Dritte übertragen.
> Die WebApp nutzt einen Service Worker, um die App für die Offline-Nutzung zwischenzuspeichern und automatisch zu aktualisieren. Die Android-App (Download-Fassung) fragt
> höchstens alle 20 Stunden bei GitHub nach einer neuen Version (abschaltbar); dabei werden außer der normalen technischen Anfrage keine Daten gesendet.
> Töne werden im Gerät erzeugt; Kamera und Mikrofon werden nicht verwendet.

**Bitte prüfen:** Start auf iPhone/iPad (Safari ab 18.2), Ladezeit über den Server mit gzip, Offline nach dem ersten Besuch, automatisches Update beim nächsten Push.
