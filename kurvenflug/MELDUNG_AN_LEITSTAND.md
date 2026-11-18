# Meldungen an den Leitstand – Kurvenflug WebApp

## ✅ 2.5.31 – C1 geändert: vor Klassenarbeiten Probestunde statt Crashkurs (Marcel 18.11.)
- Crashkurs-Knopf und -Link entfernt (auch aus dem Code), `crashkurs_klick` aus der Ereignisliste genommen.
- „Klassenarbeit bald“: Text „… Eine kostenlose Probestunde kann vor der Arbeit helfen – bei Nachhilfe – online, mit einer Lehrkraft, passend zum Thema.“,
  Knopf „Probestunde ansehen“ → `https://www.mathenachhilfe-online.com/probestunde-anfrage/?src=kurvenflug-klassenarbeit&thema=<id>&klasse=<n>`
  (Test in `ParentOffersTest`). „Thema hakt“ bleibt `…/probestunde/?src=kurvenflug-elternbericht&…`.
- Zählung: `probestunde_klick` wie bisher. **ort=klassenarbeit sende ich nicht:** unsere Sammelsendung trägt mehrere Ereignisse, ein einzelnes `ort` je
  Sendung wäre für die anderen falsch. Wenn ihr es braucht: Format für ort je Ereignis nennen, dann baue ich es ein.
- Bildschirmfoto 104 angesehen. versionCode 78 / 2.5.31, Prüfkette grün, keine neuen Dateiendungen.

## ✅ 2.5.30 – Klassen-Dialog am PC als mittige Karte
- „In welcher Klasse bist du?“: Karte höchstens 640 dp breit, mittig (Browser 1440×900 angesehen; Knöpfe ~180 px statt ~450 px). Handy unverändert.
- versionCode 77 / 2.5.30, Prüfkette grün, keine neuen Dateiendungen.

## ✅ 2.5.29 – PC-Ansicht im ganzen Bildschirm (Marcel 18.11.: „am PC auf Fullscreen … und nicht nur strecken“)
- Keine feste Handy-Spalte mehr. Ab 900 dp Breite eigenes Breitbild-Layout:
  - **Startseite:** Seitenleiste (NavigationRail) statt Leiste unten; „Heute“ zweispaltig (links Kopf, Volt, Sterne – rechts Weiter/Tagesmission/
    Lernstand/Aurora) und darunter **„🪐 Deine Klasse“** als Kapitel-Raster (3 Spalten, Fortschrittsring, Klick startet das nächste offene Level);
    „Spiele“ und „Ich“ als Kachel-Raster mit 4 Spalten; Inhalt höchstens 1280 dp, mittig.
  - **Level (alle Spielarten mit dem Level-Bildschirm):** Spielfeld groß links (Höhe des Fensters), Aufgabe, Volt, Formel, Regler und „Start“
    rechts (360–560 dp) – kein Scrollen nötig. Ergebnis-Karte, Theorie, Einführung und Rechenweg auf Lesebreite (640–760 dp).
  - **Lese-Seiten** (Einstellungen, Eltern, Sammlung, Lernstand …): höchstens 820 dp, mittig auf dem vollen Sternenhintergrund.
- **Handy unverändert:** alle Bildschirmfotos neu aufgenommen – keines hat sich verändert. Browser 1600×900 angesehen (Start, Level, Galaxie, Ich, Spiele).
- versionCode 76 / 2.5.29, Prüfkette grün. Enthält auch 2.5.28 (#pro=, Pro-Code im Elternbereich, Zählung per fetch).

## ✅ 2.5.28 – Ratgeber-Update 11 eingebaut (noch NICHT auf test – Sperre bis 2.5.27 live)
1. **Freischalt-Link:** liest `#pro=CODE` (neu) **und** `?pro=CODE` (alt); danach sofort `history.replaceState` auf den reinen Pfad
   (Browser-Test: beide Formen erkannt und gespeichert, Adresse danach `…/` ohne Code; Bestätigung bzw. Hinweis erscheint).
   **→ Ihr könnt Shop-Mails und Geschenkkarten für KurvenFlug auf `#pro=` umstellen, sobald 2.5.28 live ist.**
2. **iPhone/iPad Home-Bildschirm:** Code-Eingabe von Hand geprüft – Pro-Seite (Einstellungen → Kurvenflug Pro) hat das Feld „Code aus der
   E-Mail“ (prüft lokal mit dem öffentlichen Schlüssel, kein Server nötig). Neu: Eintrag „⭐ Pro-Code einlösen“ im Elternbereich (führt dorthin)
   und Hinweis im Feld, dass die Home-Bildschirm-App einen eigenen Speicher hat.
3. **Zählung:** kein `navigator.sendBeacon` mehr, sondern `fetch(url, {method:'POST', credentials:'omit', keepalive:true,
   headers:{'Content-Type':'text/plain'}, body})` (Browser-Test: POST, text/plain, kein Cookie; aus → nichts).
- versionCode 75 / 2.5.28, Prüfkette grün. **Liegt nur auf dem Entwicklungs-Branch**, nicht auf kurvenflug-test, bis ihr 2.5.27 live gegeben habt –
  sagt mir dann Bescheid, dann lege ich 2.5.28 auf test.

## ✅ 2.5.27 – Verschlüsselte Sicherung über die Kopplung + Monatsverlauf
1. **Sicherung:** Zusammen mit dem Bericht (beim Verstecken/Verlassen, höchstens alle 10 min) `PUT …/eltern/v1/s/<id>`, X-Schreib, text/plain
   `<iv>.<ct>` (AES-GCM-256 mit key, neuer 12-Byte-IV je Sendung), **nur bei Änderung**, ≤ 512 KB. Klartext
   `{"format":1,"app":"kurvenflug","v":"2.5.27","stand":"…+01:00","schreib":"…","daten":{…}}`; daten = alle `kf.*`-Einträge des Browsers
   (Fortschritt, Durchgänge der letzten 60 Tage, Monatssummen, Einstellungen, Pro-Code) **ohne** technische Merker und ohne `kf.eltern.*`.
   Wäre es zu groß: ohne Einzeldurchgänge (Monatssummen bleiben).
2. **Wiederherstellen** (Elternbereich hinter dem Eltern-Tor): „♻️ Fortschritt wiederherstellen“ → „Kopplungs-Link einfügen“ → GET `…/s/<id>` →
   `d` entschlüsseln → app ≠ kurvenflug: „Dieser Link gehört zu einer anderen App“; 404: „Keine Sicherung gefunden“; vorhandener Lernstand:
   **Rückfrage „Lernstand ersetzen?“**; danach id/key/schreib gespeichert → wieder gekoppelt, App startet neu.
3. **Monatssummen** dauerhaft (`kf.monate` je Spieler; je Monat Maximum aus Gespeichertem und Neuem, damit sie beim 60-Tage-Aufräumen nicht
   schrumpfen) und im Bericht als `"verlauf":[{"monat":"2026-11","min":7,"aufgaben":3,"richtigProzent":67}]`, höchstens 12, älteste zuerst.
   Tests in `ParentReportTest`.
4. **Rundlauf gegen trainer-test (zwei getrennte Browser-Profile, echte Antworten):** Gerät A (Lernstand vorhanden) koppelt → PUT s/ 204, PUT b/ 204;
   Sicherung entschlüsselt: app, v, stand, **schreib passt**, 10 Einträge, keine kf.eltern-Schlüssel; Bericht enthält verlauf 2026-11. Gerät B (leer):
   Wiederherstellen → **ok**, **Fortschritt und Durchgänge identisch**, gleiche id/schreib → danach **PUT b/ 204, PUT s/ 204** (neues Gerät schreibt);
   erneutes Wiederherstellen → „vorhanden“ (Rückfrage); fremdes X-Schreib → **403**; unbekannter Link → „keine“; Entkoppeln (DELETE b/ 204) →
   Bericht **404** und Sicherung **404**.
5. **Datenschutz-Satz** in der App (Eltern-Karte und Einstellungen/Für Eltern im Browser): „Bei gekoppeltem Eltern-Handy wird zusätzlich eine
   Ende-zu-Ende-verschlüsselte Sicherung des Lernstands gespeichert (siehe Datenschutzerklärung 10i).“
- Bildschirmfoto `105_eltern_handy` angesehen. versionCode 74 / 2.5.27, Prüfkette grün, keine neuen Dateiendungen.

## ✅ 2.5.26 – hinweis „Klassenarbeit bald“ + Rundlauf gegen trainer-test
- **hinweis:** Ist „Klassenarbeit bald“ eingestellt, enthält der Bericht `"hinweis":"Bald steht eine Klassenarbeit an: <Thema>"` (Thema = schwächstes
  Thema, sonst zuletzt gespieltes Kapitel; ohne Spieldaten „Bald steht eine Klassenarbeit an.“). Test in `ParentReportTest`.
- **Rundlauf gegen https://trainer-test.mathenachhilfe-online.com (18.11., echte Antworten):** App koppelt → **PUT 204** (Inhalt/Kopfzeilen der App) →
  **GET 200** `{"d":"<iv>.<ct>","t":…}` → mit dem key aus dem Link **entschlüsselt** = gültiges JSON (format 1, name, klasse 8, stand, hinweis …) →
  PUT mit fremdem X-Schreib **403** → App entkoppelt → **DELETE 204** → **GET 404** `{"fehler":"kein_bericht"}`.
  (Im Test lief die Seite als localhost; die App-Anfragen gingen deshalb über Playwright an den echten Server.)
- **CORS geprüft:** Preflight von `https://kurvenflug-test.…` und `https://kurvenflug.…` → 204, allow-origin passend, `Content-Type, X-Schreib`, `GET, PUT, DELETE, OPTIONS`.
- Hinweis für die Eltern-Seite: GET liefert JSON `{"d","t"}` – zum Entschlüsseln `d` nehmen.
- versionCode 73 / 2.5.26, Prüfkette grün, keine neuen Dateiendungen.

## ✅ 2.5.25 – Eltern-Handy koppeln + Prüfplan (Dauerregel)
**Eltern-Handy koppeln** (nur WebApp, nur im Elternbereich hinter dem Eltern-Tor):
1. Karte „📱 Eltern-Handy koppeln“ mit freiwilligem Feld „Spitzname für den Bericht“. Koppeln erzeugt mit `crypto.getRandomValues`:
   id 16 Bytes (22 Zeichen), schreib 16 Bytes (22), key 32 Bytes (43), base64url ohne Padding – nur in localStorage (`kf.eltern.*`).
   Danach QR-Code (eigener Erzeuger, Fehlerkorrektur M, Modul für Modul gegen die Python-Bibliothek „qrcode“ geprüft; Test
   `QrCodeTest`) und „Link teilen“. Link: Test-Stufe (`*-test.*`/localhost) `https://staging.mathenachhilfe-online.com/eltern-bericht/#<id>.<key>`,
   sonst `https://www.…/eltern-bericht/#<id>.<key>`.
2. Bericht (Format 1 genau nach eurer Spezifikation, `core/model/ParentReport.kt`, Test `ParentReportTest`): format, app, appName, name
   (nur wenn eingegeben), klasse, stand (ISO mit Zeitzone), zeitraum „letzte 14 Tage“, lernzeitMin, tageAktiv, aufgaben (= Durchgänge),
   richtigProzent (= geschaffte Durchgänge), staerken (≥ 80 %, ab 3 Durchgängen, max. 3), schwach (≤ 60 %, mit tipp = Merkformel des Kapitels),
   fortschritt (zuletzt gespielte Kapitel: geschafft/von), letzte (bis 7 Tage). thema aus der gemeinsamen Liste. **klassenarbeit** sende ich
   nicht: KurvenFlug kennt nur „bald“ (Schalter), kein Datum – sagt, falls ihr dafür ein Datum-Feld wollt. Keine E-Mail, Gerätedaten, Standort.
   Die App hält den Bericht aktuell, verschlüsselt ihn sofort (AES-GCM-256, WebCrypto, 12-Byte-IV) und sendet beim Verstecken/Verlassen
   **höchstens alle 10 Minuten** `PUT https://trainer(-test).mathenachhilfe-online.com/eltern/v1/b/<id>`, `X-Schreib`, Body text/plain
   `<iv>.<ciphertext>` (≤ 24 KB); gleich nach dem Koppeln einmal sofort (legt das Schreibrecht fest).
3. „Kopplung aufheben“ (mit Rückfrage) → `DELETE …/<id>` mit X-Schreib, alle `kf.eltern.*` lokal gelöscht.
4. **Rundlauf-Test (Chromium, Server abgefangen):** Link-Format ok; PUT mit 22-Zeichen-X-Schreib, text/plain, iv 16 Zeichen; Schlüssel nicht im
   Körper; mit dem key aus dem Link entschlüsselt → gültiges JSON (format 1, name „Lena“, klasse 7 …); zweites Verstecken < 10 min → kein
   zweiter PUT; DELETE mit demselben X-Schreib, lokal leer. Kinderbereich zeigt nichts (Karte nur in der Elternseite). Bildschirmfoto `105_eltern_handy`.
**Prüfplan (Dauerregel):** `kurvenflug/PRUEFPLAN.md` – 4 rollierende Blöcke (A Grundlagen 5–8, B Funktionen 7–10, C Physik, D Oberstufe +
Spielmodi), je Monat ein Block gründlich + Neu-Content + Rundum-Prüfung; erster Prüfmonat November (23.–30.11.: Block A). Wöchentlicher
Neu-Content mittwochs; diese Woche: Theorie-Merkformeln + Beispiele (2.5.23), „Kommt bald“ (2.5.24), Eltern-Handy (2.5.25).
- versionCode 72 / 2.5.25, Prüfkette grün, keine neuen Dateiendungen.

## ✅ 2.5.24 – Lehrplan-Abgleich (Marcel 18.11.) + „Kommt bald“ in der Galaxie
**Grundlage und Ehrlichkeit:** Abgleich gegen die KMK-Bildungsstandards (Primar/Sek I: Leitideen Zahl, Messen, Raum & Form,
Funktionaler Zusammenhang, Daten & Zufall; Sek II/Abitur: Analysis, Analytische Geometrie, Stochastik; Physik: Mechanik, Elektrizität,
Wärme, Optik, Atom-/Kernphysik) und die Kernlehrpläne Gymnasium/Realschule in BY, BW, NRW, NI, HE **aus meinem Wissen** – ich habe die
Lehrplan-Dokumente in dieser Sitzung nicht einzeln aufgerufen. Klassenangaben sind typische Spannen über die Länder (in einzelnen
Ländern ±1 Jahr; Realschule ohne Oberstufe).

**Vorhanden (30 Kapitel):** Koordinaten (Kl. 5–6) · Brüche & Kommazahlen (5–6) · Winkel (5–6) · Maßstab & Karten (5–6) · Zuordnungen (6–7) ·
Prozent & Zinseszins (7–8) · Geraden/lineare Funktionen (7–8) · Gleichungssysteme/Rendezvous (8) · Pythagoras (8–9) · Parabeln (9) ·
Nullstellen/quadratische Gleichungen (9) · Trigonometrie (9–10) · Logarithmus (10) · Sinus & Exponentialwachstum (10) · Potenzfunktionen (10) ·
Verschobene Wellen (10–11) · Ableitung & Tangente (11) · Extrempunkte (11) · e-Funktion (11–12) · Integral (12) · Vektoren 2D (11–13) ·
Binomialverteilung (10–13) · Physik: Weg & Zeit (7) · Laser-Optik/Reflexion (7) · Energie (8–9) · Lichtbrechung (9) · Beschleunigte Bewegung (9–10) ·
Wurf & Schwerkraft (7–10) · Waagerechter Wurf (10) · Gravitation/Schleuder (10).

**Fehlt → „Kommt bald“ (in der Galaxie sichtbar, ohne Datum, ohne Kaufhinweis):**
| Thema | Lehrplan (typisch) | vorhanden | geplant |
|---|---|---|---|
| Negative Zahlen | alle Länder Kl. 6–7 (Gym/RS) | nein | kommt bald |
| Terme & Gleichungen | Kl. 7–8 | nein (nur Gleichsetzen im Rendezvous) | kommt bald – Vorschlag als erstes neues Kapitel |
| Kreis & Flächen (π) | Kl. 5–9 | nein | kommt bald |
| Körper & Volumen | Kl. 5–10 | nein | kommt bald |
| Symmetrie & Abbildungen | Kl. 5–7 | nein | kommt bald |
| Dreiecke konstruieren (Kongruenz) | Kl. 7 | nein | kommt bald |
| Ähnlichkeit & Strahlensätze | Kl. 9 | nein | kommt bald |
| Daten & Diagramme (Mittelwert, Median, Boxplot) | Kl. 5–8 | nein | kommt bald |
| Wahrscheinlichkeit (Laplace, Baumdiagramm) | Kl. 6–9 | nein (nur Binomial) | kommt bald |
| Potenzen & Wurzeln | Kl. 9–10 | teilweise (Potenzfunktionen) | kommt bald |
| Kurvendiskussion (Wendepunkte, Scharen) | Kl. 11–12 Gym | teilweise (Extrempunkte) | kommt bald |
| Geraden & Ebenen im Raum, Skalarprodukt | Kl. 12–13 Gym | nein (Vektoren nur 2D) | kommt bald |
| Normalverteilung & Hypothesentest | Kl. 12–13 Gym | nein | kommt bald |
| Kräfte & Hebel | Physik Kl. 7–8 | nein | kommt bald |
| Druck & Auftrieb | Physik Kl. 8–9 | nein | kommt bald |
| Strom & Ohmsches Gesetz | Physik Kl. 7–9 | nein | kommt bald |
| Magnetismus & Induktion | Physik Kl. 8–10 | nein | kommt bald |
| Wärme | Physik Kl. 7–9 | nein | kommt bald |
| Kreisbahnen & Satelliten | Physik Kl. 10–11 | teilweise (Gravitation) | kommt bald |
| Impuls & Stöße | Physik Kl. 10–11 | nein | kommt bald |
| Schwingungen (Pendel, Feder) | Physik Kl. 10–11 | teilweise (Sinus) | kommt bald |
| Radioaktivität (Halbwertszeit) | Physik Kl. 9–10 | teilweise (Zerfall in e-Funktion) | kommt bald |
**Gebaut in diesem Schritt:** noch kein neues Kapitel – jedes braucht eine eigene Spielmechanik, Level mit Lösbarkeits-Prüfung,
Theorie + Beispiel und Tests. Vorschlag Reihenfolge (passt zur Flugbahn-Idee, großer Lehrplan-Anteil): 1) Terme & Gleichungen,
2) Kreis & Flächen, 3) Wahrscheinlichkeit, 4) Radioaktivität, 5) Strom & Ohm. Bitte Marcel kurz bestätigen oder umsortieren –
dann je ein Kapitel pro Nachkomma-Version.
**Umsetzung:** `core/model/Curriculum.kt` (Liste), Test `CurriculumTest` (kein Doppel zu bestehenden Kapiteln, keine Preise/Daten);
in der Galaxie (Karte und Liste) unten „🔭 Kommt bald“, nach Fach gefiltert, Themen der gewählten Klasse zuerst und hervorgehoben.
Im Browser angesehen. versionCode 71 / 2.5.24, Prüfkette grün, keine neuen Dateiendungen.

## ✅ 2.5.23 – Theorie geprüft: Merkformel + Beispiel für jedes Level (Marcel 18.11.)
**Prüfung:** alle 30 Kapitel (175 Level + Extrem-Stufen), dazu Bosse, Andromeda, Rettungen, Jahreszeiten-, Tages- und Zufallsmissionen
(nutzen die Theorie des Kapitels mit derselben Bahnart). Je Level: Steht die nötige Formel mit allen einstellbaren Größen da?
Gibt es ein durchgerechnetes Beispiel? Passt der Inhalt zur Überschrift? Ergebnis (Level je Kapitel in Klammern):
| Kap. | Thema (Level) | Formel in Theorie vorher | jetzt (Merkformel + Beispiel) |
|---|---|---|---|
| 1 | Koordinaten (7) | ja (Regel), kein Beispiel | (x \| y) + Beispiel (3 \| −2) |
| 2 | Geraden (8) | y = mx + b ja, **m = Δy:Δx fehlte** | + Steigung aus zwei Punkten + Beispiel |
| 3 | Parabeln (6) | ja | + Beispiel a aus Scheitel und Ziel |
| 4 | Wurf & Schwerkraft (6) | ja | + Beispiel w = v²·sin2α:g |
| 5 | Wellen & Wachstum (7) | ja (Sinus, a·bˣ) | je Bahnart eigene Merkformel + Beispiel |
| 6 | Schwerkraft-Schleuder (5) | nur „nah vorbei biegt stark“ | F = G·m·M:r² + Beispiel |
| 7 | Rendezvous (7) | „gleichsetzen“ | Verfahren + durchgerechnetes Gleichungssystem |
| 8 | Zuordnungen (6) | ja | y = k·x / y = k:x je Bahnart + Beispiel |
| 9 | Pythagoras (5) | ja | r = √(Δx²+Δy²) + Beispiel |
| 10 | Logarithmus (5) | ja, d nicht in Merkformel | y = log_b(x) + d + Beispiel |
| 11 | Laser-Optik (5) | ja | + Beispiel |
| 12 | Energie (5) | Merkformel ohne Weiten-Formel | + s = 2·√((h−h₀)·h₀) + Beispiel |
| 13 | Weg & Zeit (4) | ja | + v = Δs:Δt + Beispiel |
| 14 | Trigonometrie (7) | **nein – Spiegelgesetz/Pythagoras** | tan (Laser) bzw. sin/cos (Funk) je Bahnart + Beispiele |
| 15 | Brüche & Kommazahlen (8) | **nein – Koordinaten-Regel** | Bruch = Zähler:Nenner + Beispiel |
| 16 | Winkel (6) | **nein – Spiegelgesetz** | Winkelsumme 180°, Nebenwinkel + Beispiel |
| 17 | Lichtbrechung (5) | **nein – Spiegelgesetz** | sin α₁ = n·sin α₂ + Beispiel |
| 18 | Nullstellen (6) | allgemeine Parabel-Regel | pq-Formel, d = Mitte der Nullstellen + Beispiel |
| 19 | Ableitung & Tangente (6) | ja | + Ableitungsregel + Beispiel |
| 20 | Extrempunkte (6) | ja | + f''-Test + Beispiel |
| 21 | Integral (6) | ja | + Stammfunktions-Regel + Beispiel |
| 22 | Vektoren (5) | ja | + Beispiel |
| 23 | Prozent & Zinseszins (6) | ja (allg. Exponentiell) | b = 1 ± p:100 + Zinseszins-Beispiel |
| 24 | Beschleunigte Bewegung (6) | ja | + Beispiel |
| 25 | Potenzfunktionen (5) | **d fehlte in Merkformel** | y = a·xⁿ + d + Beispiel |
| 26 | Verschobene Wellen (5) | **c fehlte in Merkformel** | y = a·sin(b·(x−c)) + d + Beispiel |
| 27 | Waagerechter Wurf (5) | **nein – Schrägwurf-Formel** | t = √(2h:g), v = x:t + Beispiel |
| 28 | e-Funktion (6) | ja (exponentiell, e^(k·x)) | + k = ln(q):x + Beispiel |
| 29 | Maßstab & Karten (5) | **nein – Koordinaten-Regel** | Kästchen = Länge:Maßstab + Beispiel |
| 30 | Binomialverteilung (6) | ja | + kumuliert + Beispiel |

**Überschriften geprüft:** e-Funktion/Exponentiell sind wirklich exponentiell (a·bˣ bzw. a·e^(k·x)); Potenzfunktionen ganzrational y = a·xⁿ + d;
nichts vertauscht. Die Kapitel-Erklärungen (1–4 Punkte) bleiben, darunter jetzt **💡 Merkformel** und **✏️ Beispiel**.
**Absicherung (neuer Test `TheoryFormulaTest`):** jedes Kapitel-Level und jede Mission hat Theorie + Merkformel + Beispiel; jeder Regler
(m, b, a, d, e, c, k, n, p, r, h, v, α, s₀, v₀ …) kommt in der Merkformel vor; Kap. 14/16/17 nie das Spiegelgesetz, Kap. 27 nie die
Schrägwurf-Formel, Kap. 28 immer e^(k·x), jede Exponential-Aufgabe a·bˣ.
- Bildschirmfoto 98_theorie angesehen. versionCode 70 / 2.5.23, Prüfkette grün, keine neuen Dateiendungen.

## ✅ 2.5.22 – Vorgaben aus dem Ratgeber übernommen (eure Nachricht 18.11. 14:37)
1. **Zählung:** Sendung jetzt `{"app":"kurvenflug","v":"2.5.22","e":{…}}` (ohne `f`), je Ereignis höchstens 50. Gezählt werden:
   `start`, `level_start`, `level_geschafft`, `theorie`, `elternbereich` (Eltern-Tor bestanden), `pro_ansehen` (Pro-Seite geöffnet),
   `pro_kasse` (Kauf nach Eltern-Tor), `code_eingeloest` (Code-Feld oder `?pro=`-Link gültig), `probestunde_klick`, `crashkurs_klick`,
   `nachhilfe_hinweis` (Hinweis gezeigt), `geschenk_klick`. `neukunde_klick` steht in der erlaubten Liste, KurvenFlug hat dafür keinen Knopf.
   Kein `ort`/`thema` (optional). Unbekannte Ereignisse kann die App nicht senden (feste Liste, Test). Browser: an → `{"start":1}` gesendet, aus → nichts.
2. **Schalter** „📊 Anonyme Nutzungszählung (hilft uns, die App zu verbessern)“, **Standard AN**, Satz: gezählt wird nur, wie oft etwas
   passiert – ohne Kennung, ohne IP, ohne Namen/Spielstand; auf dem Gerät nur die Schalterstellung (Zähler nur im Speicher).
3. **Themen-Zuordnung** (Kapitel → Kennung): 1 Koordinaten → geometrie · 2 Geraden → lineare-funktionen · 3 Parabeln → quadratische-funktionen ·
   4 Wurf & Schwerkraft → physik-mechanik · 5 Wellen & Wachstum → trigonometrie · 6 Schwerkraft-Schleuder → physik-mechanik ·
   7 Rendezvous → gleichungssysteme · 8 Zuordnungen → zuordnungen · 9 Pythagoras → pythagoras · 10 Logarithmus → exponential-log ·
   11 Laser-Optik → physik-optik · 12 Energie → physik-energie · 13 Weg & Zeit → physik-mechanik · 14 Trigonometrie → trigonometrie ·
   15 Brüche & Kommazahlen → brueche · 16 Winkel → geometrie · 17 Lichtbrechung → physik-optik · 18 Nullstellen → quadratische-funktionen ·
   19 Ableitung & Tangente → analysis · 20 Extrempunkte → analysis · 21 Integral → analysis · 22 Vektoren → vektoren ·
   23 Prozent & Zinseszins → prozent · 24 Beschleunigte Bewegung → physik-mechanik · 25 Potenzfunktionen → potenzen-wurzeln ·
   26 Verschobene Wellen → trigonometrie · 27 Waagerechter Wurf → physik-mechanik · 28 e-Funktion → exponential-log ·
   29 Maßstab & Karten → zuordnungen · 30 Binomialverteilung → stochastik. (Test: jedes Kapitel hat eine Kennung aus der Liste.)
4. **Probestunde:** `…/probestunde/?src=kurvenflug-elternbericht&thema=<id>&klasse=<n>` (Thema hakt) bzw. `src=kurvenflug-klassenarbeit`.
5. **Crashkurs** (nur bei „Klassenarbeit bald“): Knopf „Crashkurs ansehen“ → `…/produkt/easy-learning-crash-kurs/?src=kurvenflug-klassenarbeit`,
   Wortlaut „kostenlos für Familien mit Stundenpaket“.
6. Rest unverändert. versionCode 69 / 2.5.22, Prüfkette grün, keine neuen Dateiendungen.

## ✅ 2.5.21 – A3 Zählung, B4 Einlöse-Link + Lern-Geschenk, C1 Nachhilfe-Hinweis, Fußzeile
**Hinweis:** Den Ratgeber (Website_Backup, geld-verdienen/RATGEBER.md) konnte ich **nicht lesen** – das Hinzufügen des Repos wurde
in meiner Sitzung abgelehnt. Umgesetzt nach eurer Nachricht; wo der Ratgeber Genaueres vorgibt (Format, Crashkurs-Links,
„Fertig, wenn“), bitte melden – ich passe an.
- **Eltern-Tor:** „Ich“ → „👪 Für Eltern“ öffnet jetzt erst nach der Frage für Erwachsene (Geburtsjahr, ≥ 18). Alle Angebote liegen
  nur dort; Volt/Spiel zeigen nichts (UWG Anh. Nr. 28). Keine Preise. Anrede du.
- **A3 Zählung:** Schalter „📊 Anonyme Nutzungszählung“ im Elternbereich, **Standard AUS** (datensparsam, Kinder-App – bitte sagen,
  falls der Ratgeber „an“ vorsieht). Ereignisse nur: `start`, `level_start`, `level_geschafft`, `theorie` (feste Liste,
  `UsageCount.EVENTS`). Beim Verstecken/Verlassen der Seite (`visibilitychange`/`pagehide`) **ein** `navigator.sendBeacon`
  POST an `https://trainer.mathenachhilfe-online.com/zaehler/v1/e`, `text/plain`, ohne Cookies/Kennung, Inhalt z. B.
  `{"app":"kurvenflug","v":"2.5.21","f":"wasm","e":{"level_geschafft":3,"start":1}}`. Im Browser geprüft: an → 1 Sendung, aus → keine.
- **B4:** `?pro=<CODE>` wird wie bisher eingelöst; jetzt mit Bestätigung („🎉 Kurvenflug Pro ist freigeschaltet“ bzw. Hinweis, den Code
  in den Einstellungen einzugeben). Im Elternbereich „🎁 Lern-Geschenk verschenken“ → `…/geschenke/?src=kurvenflug-geschenk`.
- **C1 Nachhilfe-Hinweis** (Elternbereich): Auslöser „Thema hakt“ (Wochenübersicht „Hier täte Hilfe gut“) oder „Klassenarbeit bald“
  (Schalter, gilt 21 Tage). Häufigkeitsgrenze: an einem Tag sichtbar, dann 6 Tage Pause; „Nicht mehr zeigen“ blendet ihn dauerhaft aus.
  Link: `…/probestunde/?src=kurvenflug-eltern&thema=kf-<Kapitel>&klasse=<n>` (Themen-ID = `kf-` + Kapitelnummer – bitte sagen, falls ihr
  andere IDs nutzt). **Crashkurs-Links fehlen noch** (stehen im Ratgeber, den ich nicht lesen konnte).
- **Fußzeile:** „Ein Angebot von Nachhilfe – online“ → `https://www.mathenachhilfe-online.com/?src=kurvenflug-fusszeile`, ohne Preis:
  unten in den Einstellungen, im Elternbereich und im Ladebild (neben Impressum/Datenschutz).
- Tests: `ParentOffersTest` (Häufigkeitsgrenze, Links, Zählformat), `ParentGateTest` (Tor: 2015 abgelehnt, 1980 durch), Bildschirmfoto `104_eltern_angebote`; Eltern-Tor im Browser durchgespielt. versionCode 68 / 2.5.21,
  Prüfkette grün, keine neuen Dateiendungen.

## ✅ 2.5.20 – Wasm für Safari/WebKit wieder offen (Marcel 18.11.: „Geht beides“ – ?modus=wasm und JS auf dem iPhone)
- Die Regel „WebKit → direkt JS“ ist entfernt: iPhone/iPad/Safari bekommen wieder Kotlin/Wasm (mit `-Xwasm-no-jstag` und der
  Skiko-Umgehung für WebKit 329421). Rückfall bleibt: Jeder Startfehler/Absturz → automatisch JS (gemerkt je Version),
  `?modus=js` zum Erzwingen, `?modus=auto` zurück. Hinweis: Wer zuvor `?modus=wasm` aufgerufen hat, behält den Merker bis `?modus=auto`.
- Chromium-Tests: Chrome → wasm, iPhone-/Safari-Kennung → wasm, Startfehler → js. versionCode 67 / 2.5.20, Prüfkette grün.

## ✅ 2.5.19 – Safari-27.2-Umgehung (WebKit 329421, Endaufrufe) von Raumlicht übernommen
- `web/webpack/skiko-no-tail-call.cjs` + `web/webpack.config.d/skiko-no-tail-call.js` aus Raumlicht 92608706 übernommen: schreibt in
  `skiko.mjs` jeden Skia-Aufruf aus der Endaufruf-Stellung (try-Block). Gilt für **beide** Fassungen (Wasm und JS nutzen dasselbe
  skiko.mjs). Bau-Prüfung in `:web:webapp`: kurvenflug.js **937 abgesichert, 0 Endaufrufe übrig**; kurvenflug-compat.js ebenso.
  `-Xwasm-no-jstag` bleibt an.
- **Wasm für WebKit noch NICHT wieder offen:** Bliebe die Seite trotz Umgehung weiß, gäbe es keinen Fehler, den der Auto-Wechsel
  erkennen könnte (die App „läuft“, malt aber nichts). Darum bitte zuerst Marcel: **https://kurvenflug-test.mathenachhilfe-online.com/?modus=wasm**
  (danach `?modus=auto`). Zeigt das iPhone/iPad damit das Spiel, öffne ich Wasm für Safari im nächsten Nachkomma-Schritt (mit
  Rückfall auf JS bei jedem Startfehler).
- Chromium-Tests beider Fassungen grün. versionCode 66 / 2.5.19, keine neuen Dateiendungen.

## ✅ 2.5.18 – iOS/Safari: JS-Fassung automatisch, Startfehler wechseln ohne Zutun, Kotlin-Flags geprüft
**Ursache genauer (aus Marcels iPad-Befund + Analyse der Module):**
- Das App-Wasm importiert genau **einen Nicht-Funktions-Import: `intrinsics.tag` (kind „tag“)**. Bisher lieferte die Kotlin-Brücke
  dafür **`WebAssembly.JSTag`** (wenn vorhanden, sonst eigenes `new WebAssembly.Tag({parameters:['externref']})`).
  Safari 26/27 kennt JSTag, verarbeitet aber den Tag-Import offenbar fehlerhaft: `WebAssembly.Module.imports` scheitert
  („unable to produce import descriptors“), die App startet und bricht dann mit **„Can't wrap nullptr“** ab (Skiko-Objekt null –
  passt zu falsch weitergereichten/gefangenen JS-Ausnahmen). Das Skiko-Wasm (Emscripten, nur Funktions-Importe) ist unauffällig.
- **Kotlin 2.2.21 – Flags geprüft:** vorhanden u. a. `-Xwasm-no-jstag`, `-Xwasm-use-new-exception-proposal`,
  `-Xwasm-use-traps-instead-of-exceptions` (bricht try/catch – unbrauchbar). **`-Xwasm-no-jstag` ist jetzt an** (Web, shared,
  core/model): die Brücke nutzt nie mehr `WebAssembly.JSTag`, sondern immer ein eigenes Tag (im Bündel geprüft: 0 × JSTag).
  `-Xwasm-use-js-string-builtins` gibt es in 2.2.21 nicht als Schalter (Importe sind reine Funktionen aus `js_code`/`./skiko.mjs`).
  Ob Safari mit no-jstag die schnelle Fassung startet, kann nur Marcel prüfen: **`…/?modus=wasm`** (danach `?modus=auto`).
**Umgesetzt (Vorlage für die anderen Räume):**
1. **WebKit → direkt JS:** iPhone/iPad (alle Browser) und Safari am Mac (AppleWebKit ohne Chrome/Edge/Firefox/Android) bekommen
   sofort `kurvenflug-compat.js` – nichts wird doppelt geladen. Ausnahme zum Testen: `?modus=wasm`.
2. **Auto-Wechsel ohne Zutun:** Jeder Startfehler der Wasm-Fassung (error/unhandledrejection inkl. „Can't wrap nullptr“, auch in
   den ersten 6 s nach dem Start, Zeitüberschreitung, Tab-Absturz nach dem Laden) → Merker `kf.wasmGescheitert = <Version>` und
   **sofort neu laden mit JS**; danach kurzer Hinweis „Schnelle Fassung ging nicht (…) – einfache Fassung geladen“.
   Der Merker gilt **nur für diese Version** – eine neue Version versucht Wasm wieder (außer bei WebKit, siehe 1).
3. diag.html: Schritt 4 wertet „Importe nicht lesbar“ jetzt als Warnung (Übersetzen ok) statt als Fehler.
**Tests (Chromium):** Chrome → wasm; iPhone-Kennung → js; Mac-Safari-Kennung → js; iPhone + `?modus=wasm` → wasm; simulierter
„Can't wrap nullptr“ beim Start → automatisch js, Merker 2.5.18. Test-Link: https://kurvenflug-test.mathenachhilfe-online.com/
- versionCode 65 / 2.5.18, Prüfkette grün, keine neuen Dateiendungen.

## ✅ 2.5.17 – Diagnose-Leiste erweitert (zu Marcels Ergebnis: alle Quadrate farbig, App mit gl=beides weiß)
**Bei uns geprüft (Chromium, iPhone-Maße):** Über der Zeichenfläche liegt nichts – Aufbau: `<body>` (dunkelblau 11,16,38) → Shadow-DOM
→ `DIV` 393×856 (transparent) → `CANVAS` 393×852 + ein leeres `DIV` 0×0 (Texteingabe). Lader nach dem Start entfernt.
`elementFromPoint(Mitte)` = BODY, im Shadow = CANVAS. Canvas-Layout 393×852 @0,0 (kein 0-Größe, kein vh-Fehler).
**Neue Leiste (`?diag=1`)** – am `<html>` mit z-index 2147483647, schwarz mit pinker Linie, sofort beim Laden sichtbar:
Fenster/visualViewport/body-Größe · `elementFromPoint` Mitte (Dokument und Shadow-DOM) · Lader da/weg · Canvas Pixel- und
Layout-Größe, Position, display/opacity/transform, im Dokument? · **WebGL-Renderer** (Software? z. B. „SwiftShader“, auf dem
iPhone sollte „Apple GPU“ stehen) · Kontext verlangt/bekommen · **Pixelprobe oben links + Mitte** – mit `gl=beides` echte Farben:
oben links muss ≈ 4,6,13 bzw. 11,16,38 (dunkelblau) sein. Steht dort **255,255,255** → Skia malt selbst Weiß; dunkle Werte bei
weißem Bild → Safari zeigt die fertige Fläche nicht an. Die Zeichenfläche wird alle 5 s für 1,5 s **rot umrandet** (sieht man den
Rahmen, liegt sie richtig). Diagnose-Seite: fünftes Quadrat **violett = genau die Kontext-Einstellungen der App** (depth/stencil).
**Bitte Marcel:** https://kurvenflug-test.mathenachhilfe-online.com/?diag=1&gl=beides → Foto mit der Leiste (Zeile „Pixel …“,
„Renderer“, „Mitte“) und ob der rote Rahmen kommt. Danach `…/?diag=0&gl=standard`.
- versionCode 64 / 2.5.17, Prüfkette grün, keine neuen Dateiendungen.

## ✅ 2.5.16 – WebGL-Anzeige auf iOS untersuchen: Bildprobe, Diagnose-Leiste, Versuchsschalter
**Diagnose-Link:** https://kurvenflug-test.mathenachhilfe-online.com/diag.html – neu als Schritt 6 die **Bildprobe**: vier
Quadrate – türkis (WebGL 2 Standard), grün (preserveDrawingBuffer), pink (alpha:false), orange (normale 2D-Fläche); dazu
readPixels-Werte und die tatsächlichen Kontext-Einstellungen. **Marcel: Welche Quadrate sind farbig, welche weiß?**
Darunter Links für Versuche mit der App (mit Diagnose-Leiste) und „alles zurücksetzen“.
**Geprüft bei uns:**
- Zeichenfläche ganzzahlig × DPR: CSS 393×852, Canvas 786×1704 (DPR echt 3, genutzt 2 – ganzzahlig, keine gebrochene Skalierung;
  mit `?schaerfe=voll` 1179×2556). Kein CSS-transform/will-change auf dem Canvas.
- Kontext-Einstellungen, die Skiko/Compose verlangt (aufgezeichnet): `alpha:1, premultipliedAlpha:1, preserveDrawingBuffer:0,
  antialias:0, depth:1, stencil:8, majorVersion:2` – Compose-Standard, nichts Eigenes von uns.
- Pixelprobe nach dem ersten Bild: ohne preserveDrawingBuffer liefert readPixels nach dem Zusammensetzen 0,0,0,0 (normal);
  mit `?gl=beides` echte Farbwerte (232,236,255,255).
**Versuchsschalter (werden gemerkt, `…=standard` setzt zurück):**
`?diag=1` Diagnose-Leiste oben (Fassung, DPR echt/genutzt, Canvas-Größe + CSS-Größe + transform, Kontext verlangt/bekommen,
Pixelprobe, Kontextverlust) · `?gl=preserve|opak|beides` (preserveDrawingBuffer:true und/oder alpha:false) ·
`?schaerfe=voll|2|1` · `?modus=js|wasm|auto`. Beispiel für Marcel: `…/?diag=1&gl=beides`, dann `…/?diag=1&schaerfe=1`.
- Keine neuen Dateiendungen. versionCode 63 / 2.5.16, Prüfkette grün.

## ✅ 2.5.15 – Diagnose-Seite für iOS + Lader zuerst, Wasm nacheinander
**Diagnose-Link für Marcel:** https://kurvenflug-test.mathenachhilfe-online.com/diag.html
Reines HTML+JS, lädt die App NICHT. Jeder Schritt wird groß angezeigt und **vor** der Ausführung in localStorage (`kf.diag`)
notiert – stürzt der Tab ab, steht beim nächsten Öffnen oben rot „Beim letzten Mal ist der Tab abgestürzt bei Schritt …“:
1) HTML/JS ok (+ Gerät: User-Agent, Bildschirm, DPR) · 2) WebAssembly GC + try/catch und **exnref (try_table)** per `validate`
3) WebGL 2 + `MAX_TEXTURE_SIZE`/`MAX_RENDERBUFFER_SIZE` und nötige Zeichenflächen-Größe · 4) **kleines Wasm** (App, 5,8 MB)
einzeln laden + `WebAssembly.compile` · 5) **großes Wasm** (Skiko, 8,4 MB) einzeln laden + übersetzen (instantiate ginge nur mit
den Emscripten-Importen, darum compile) · 6) Service Worker/Offline-Speicher (aktiv? welche Caches?) · 7) letzter abgebrochener
App-Start (Fassung + Stufe aus dem Start-Protokoll). Knöpfe: „Nochmal prüfen“, **„Echte App starten“** (mit Start-Protokoll),
**„Offline-Speicher leeren“** (SW abmelden, Kurvenflug-Caches löschen – Spielstand bleibt). diag.html liegt nicht im
Offline-Speicher, kommt also immer frisch vom Server. Bitte Marcel: Seite öffnen, Bildschirmfoto (ganze Seite) schicken; bei Absturz
noch einmal öffnen und die rote Zeile fotografieren.
**index.html/kf.js:** Lader wird zuerst gemalt (zwei Animation-Frames, dann erst Programm-Skript und Downloads); die Wasm-Dateien
werden **nacheinander** geholt (nächste erst, wenn die vorige ganz da ist), weiter als Strom für instantiateStreaming.
Tests (Chromium, DPR 3 → 2): Wasm-Start, JS-Fassung, ohne GC, Datei fehlt, kein WebGL 2 – alle wie erwartet; Diagnose-Seite
läuft durch, Absturz-Anzeige getestet. Keine neuen Dateiendungen. versionCode 62 / 2.5.15.

## ✅ 2.5.14 – iOS-Laufzeitursachen abgesichert (zu euren Nachträgen 09:17/09:23)
Eure Nachträge (iOS 18.4+/26, WebKit-Emulation startet normal) kamen erst nach 2.5.13 bei mir an. Die JS-Fassung bleibt
trotzdem drin – sie springt nur noch bei einem echten Abbruch ein (deckt den Speicher-Fall ab), sonst läuft wie bisher Wasm.
**Gemessen (Chromium, iPhone-Größe 393×852, DPR 3):** JS-Heap 11–20 MB, Wasm-Speicher 17→29 MB, Audio 2–6 MB, keine Spitzen –
aber die **Zeichenfläche war 1179×2556 px** (≈ 12 MB je Puffer, mehrere Puffer im Grafikspeicher).
1. **Pixeldichte auf 2 begrenzt** (`devicePixelRatio` vor dem Start überschrieben) → Zeichenfläche **786×1704** (−56 % Pixel).
2. **Kein AudioContext vor dem ersten Tippen** (Musik-Daten werden bis dahin aufgehoben).
3. **`webglcontextlost`** an jeder Zeichenfläche (auch im Shadow-DOM) → Meldung „Die Spielgrafik wurde vom Gerät zurückgesetzt …“
   mit „Neu laden“, Version, Fassung, DPR. Getestet mit `WEBGL_lose_context`.
4. **Start-Protokoll für Marcels Foto:** Stufen startet → geladen → läuft → bereit. Wird iOS den Tab beenden und Safari lädt
   neu, steht im Ladebild und danach als Hinweis: „Letzter Start brach ab (wasm – nach dem Laden / beim Aufbau der Oberfläche).
   Jetzt: Fassung js.“ – und die JS-Fassung wird genommen. Jede Fehlermeldung nennt Version, Fassung, Fehlertext, DPR.
5. **fetch-/ReadableStream-Weg abschaltbar:** `?direkt=1` lädt ohne unseren Vorlade-Strom (Test: startet normal).
6. **Service Worker:** lädt nach einem SW-Wechsel nur neu, wenn `version.json` wirklich eine andere Version meldet (vorher konnte
   ein Wechsel der Fassung einen unnötigen Neustart auslösen); version.json kommt am SW vorbei immer frisch.
- Test-Link: https://kurvenflug-test.mathenachhilfe-online.com/ (Varianten: `?modus=js`, `?modus=wasm`, `?direkt=1`, zurück `?modus=auto`).
- Keine neuen Dateiendungen. versionCode 61 / 2.5.14, Prüfkette grün.

## ✅ 2.5.13 – DRINGEND iOS: weiße Seite behoben (Kompatibilitäts-Fassung + Startfehler sichtbar)
**Ursache (Code-Analyse; kein iPhone/WebKit in meiner Umgebung, darum nicht am Gerät bestätigt):**
- Die App läuft als Kotlin/Wasm und braucht **WebAssembly-GC + Ausnahmen** → Safari erst **ab iOS 18.2**. Ältere iPhones/iPads
  können sie gar nicht starten. Auf neueren kann iOS den Tab beim Übersetzen des großen Wasm-Moduls (5,8 MB + 8,4 MB Grafik)
  abschießen – Safari zeigt dann eine leere Seite.
- **Warum weiß statt Meldung:** Die Compose-Oberfläche hängt sich beim Start als Shadow-DOM an `<body>` – ab da ist alles, was im
  `<body>` steht (Ladebild, „Dauert etwas …“-Hinweis), unsichtbar. Jeder spätere Fehler endete deshalb leer. (Dasselbe Muster
  betrifft vermutlich alle vier Compose-Apps.)
- Geprüft und unauffällig: viewport/`user-scalable` (unkritisch), unser fetch-/ReadableStream-Wrapper (Safari kann
  instantiateStreaming mit Response-Strom), Service Worker (liefert jetzt version.json immer frisch).

**Behoben:**
1. **Nie mehr weiße Seite:** Ladebild und Fehlermeldung hängen jetzt am `<html>`-Element. Jeder Startfehler (`error`,
   `unhandledrejection`, fehlende Datei, fehlendes WebGL 2, keine Rückmeldung nach 90 s) zeigt „Kurvenflug konnte nicht starten“
   mit Grund, **Version + Fassung + Fehlertext** und den Knöpfen **„Neu laden“** und **„Einfache Fassung laden“**
   (bzw. „Schnelle Fassung laden“). Auf iPhone/iPad zusätzlich der Tipp, iOS zu aktualisieren.
2. **Kompatibilitäts-Fassung (Kotlin/JS)**, gleiche Oberfläche, gleicher Spielstand: `kurvenflug-compat.js` (5,3 MB) + dieselbe
   Skiko-Grafikdatei (Emscripten-Wasm **ohne** GC). `index.html` wählt:
   - Wasm, wenn `WebAssembly.validate` ein Mini-Modul mit GC-Struct + try/catch_all akzeptiert,
   - sonst JS; **und JS, wenn der letzte Wasm-Start nach vollständigem Laden nie „bereit“ meldete** (= Absturz) – automatisch.
   - Erzwingen zum Testen: `?modus=js`, `?modus=wasm`; zurück: `?modus=auto` (wird gemerkt).
   Service Worker speichert nur die Dateien der genutzten Fassung offline (`sw.js?fassung=wasm|js`), die andere bei Bedarf.
3. **Mindest-iOS:** vorher **iOS 18.2** (sonst weiß) → jetzt **iOS 15** (WebGL 2), ab 18.2 die schnelle Fassung.
4. **Tests (Chromium):** Wasm-Start ok; `?modus=js`: **alle 30 Kapitel, 0 Seitenfehler**; „Browser ohne Wasm-GC“ simuliert → JS
   startet; simulierter Absturz nach dem Laden → nächster Start JS; JS **offline** startbar; fehlende Datei / kein WebGL 2 →
   sichtbare Meldung. Spiellogik-Tests laufen jetzt zusätzlich unter Kotlin/JS (`jsNodeTest`). Zwei Regex-Stellen mit
   Java-Schreibweise (`(?i)`, Lookbehind) für JS umgeschrieben. Pro-Code-Prüfung unter JS ≈ 0,4 s (unter Wasm Millisekunden).
- **Test-Link:** https://kurvenflug-test.mathenachhilfe-online.com/ – zum gezielten Prüfen der einfachen Fassung
  https://kurvenflug-test.mathenachhilfe-online.com/?modus=js (danach `?modus=auto`). Bitte Marcels iOS-Version notieren
  (Einstellungen → Allgemein → Info); falls doch eine Meldung kommt, steht der Fehlertext darin – Bildschirmfoto reicht.
- Keine neuen Dateiendungen (nur `.js`). versionCode 60 / 2.5.13, Prüfkette grün.

## ✅ 2.5.12 – nur noch WebApp + Knopf „App installieren“ (Marcel 18.11.)
1. **Android-Code bleibt, wird nicht mehr gebaut:** Workflow `.github/workflows/kurvenflug-release.yml` läuft nicht mehr bei
   Push (nur noch von Hand, der alte Auslöser steht als Kommentar drin) – also keine neuen APK/AAB, kein Drive-Upload mehr.
   Die Android-Tests laufen in meiner Prüfkette trotzdem weiter mit (kostet nichts, hält den Code gesund).
2. **Wo bei KurvenFlug APKs liegen:**
   - **GitHub-Releases** im Repo AndroidApps: alle Tags `kurvenflug-v*` (über 50, von v0.2.1 bis v2.5.11), je mit
     `Kurvenflug-<version>.apk`, ab 0.2x auch `…-play.aab` und WebApp-zip. **Noch nicht gelöscht** – Löschen ist endgültig,
     ich habe Marcel gefragt.
   - **Google Drive:** Der Workflow lud die APK in den Drive-Ordner `KURVENFLUG_DRIVE_FOLDER`, sofern `GDRIVE_SA_JSON` gesetzt war –
     bitte dort nachsehen (darauf habe ich keinen Zugriff).
   - **In der WebApp:** keine Download-Links, keine Hinweise auf APK/Play (der Update-Knopf mit GitHub-APK gibt es nur in der
     Android-Fassung). README und Store-Texte: oben als „nur WebApp / geparkt“ gekennzeichnet, keine Download-Links.
3. **Auto-Update** wie in 2.5.10 (gilt für alle; Neuladen nur an sicheren Stellen).
4. **„App installieren“** – Karte mit Knopf oben auf der Startseite (mit „Später“ für diese Sitzung) und ganz oben in den
   Einstellungen. Sie erscheint nur, wenn die App nicht installiert ist (`display-mode: standalone`/`navigator.standalone`):
   Android/Chrome/Edge über `beforeinstallprompt` (wird in index.html gleich beim Laden abgefangen) → Knopf öffnet den
   Installations-Dialog; iPhone/iPad: Anleitung „Teilen → Zum Home-Bildschirm → Hinzufügen“; sonst (z. B. Firefox am PC) kein Knopf.
   **Browser-Test:** Android-Fall → Dialog geöffnet, Karte danach weg; iPhone-Fall (iPhone 13) → Karte mit Anleitung; als
   installierte App → keine Karte; **Offline-Start** nach einmaligem Laden ok. Manifest: `id`, `start_url ./`, `scope ./`,
   `display standalone`, Icons 192/512 + **maskable 512** + SVG, apple-touch-icon.
5. **Pro:** In der WebApp „Der Kauf gilt in der Web-App (in diesem Browser)“, „Freigeschaltet mit Code – gilt in der Web-App“.
   (Die Texte im Shop/in der Code-Mail liegen bei euch – bitte dort ebenfalls „gilt in der Web-App“.)
- Keine neuen Dateiendungen. versionCode 59 / Version 2.5.12, Prüfkette grün.

## ✅ 2.5.11 – Vorlesen abgeschaltet, Musik und Klänge bleiben (Marcel 18.11.) · Web = APK = Play
1. **Ein Schalter, Code bleibt:** `Nova.ENABLED = false` (`core/model/Nova.kt`). Dann gibt es keinen Reader (Android und Web):
   keine 🔊-Knöpfe (Volt-Blasen, Theorie, Aufgaben), keine Karte „Vorlesen“ (Sprechtempo, Probe, „Alle Nova-Texte offline
   laden“), kein Hinweis „Nova nicht erreichbar“. Eltern-Text, Lizenzen und Store-Texte erwähnen das Vorlesen nicht mehr.
   **Browser-Test:** Start und mehrere Klicks: **0 Anfragen an stimmen.mathenachhilfe-online.com bzw. /jetzt, 0 × Gerätestimme.**
   Zum Wiedereinschalten genügt `true` (Tests und Bildschirmfotos für das Vorlesen laufen weiter mit eingeschaltetem Reader).
2. **vorlesen/texte.json** = `{"format":1,"app":"kurvenflug","texte":[]}` (Web unter vorlesen/texte.json, APK als Asset).
3. **Musik und Klänge:** Musik startet auf Android sofort; im Browser bei der ersten Berührung (der Browser erlaubt Ton erst
   danach). Klänge bei Start, Treffer, Fehlschuss, Stern, Level geschafft, dazu die Erkennungsmelodie. In den Einstellungen sind
   „🎵 Musik“ und „🔊 Geräusche“ getrennt (je mit Lautstärke); Volts Roboter-Plappern („🤖 Volt-Stimme“) ist wieder da und abschaltbar.
4. versionCode 58, Prüfkette grün (Unit-Tests, Lint, APK, AAB, WebApp).

## ✅ 2.5.10 – fertig für den Betriebstest: WebApp aktualisiert sich selbst + Theorie im Rechen-Stern · Web = APK = Play
**WebApp immer auf dem neuesten Stand (Leitstand 18.11.):**
- Prüft auf neue Versionen beim Öffnen, bei Rückkehr in den Tab (visibilitychange/focus/pageshow) und alle 30 min:
  `registration.update()` + `version.json?nc=…` mit `cache: 'no-store'`. sw.js: `skipWaiting` + `clients.claim`.
- Neu geladen wird **nur an sicheren Stellen**: in den ersten 15 s nach dem Start, wenn der Tab im Hintergrund ist oder die
  App auf dem Startbildschirm steht (die App meldet das über `window.kfAtHome`) – **nie mitten in einer Aufgabe**; steht das
  Kind in einem Level, wird beim nächsten Wechsel zur Startseite bzw. in den Hintergrund geladen.
- Danach kurz „✨ Neue Version 2.5.10 geladen“; in den Einstellungen steht „Kurvenflug Version … · aktualisiert sich von selbst“.
- **Browser-Test (Playwright):** alte Version offen → neue Version auf den Server → Tab weg und zurück → neue Version aktiv
  ohne Neuladen von Hand, Hinweis sichtbar; **Spielstand (localStorage) und Nova-Offline-Speicher (Cache `kf-nova`)
  unverändert**, alter App-Cache entfernt.
- **Nebenbei gefunden und behoben:** Kleine Hinweise im Browser („Foto gespeichert 📸“, „In die Zwischenablage kopiert“)
  waren unsichtbar (Compose hängt sein Shadow-DOM an `<body>`) – jetzt am `<html>`-Element, sichtbar.
- Für den Server: `index.html`, `sw.js` und `version.json` bitte **ohne langes Caching** ausliefern (`Cache-Control: no-cache`);
  die wasm-Dateien haben Inhalts-Namen und dürfen lange gecacht werden. Keine neuen Dateiendungen.

**Theorie im Rechen-Stern (Marcel 18.11.):** In der Rechen-Stern-Karte gibt es jetzt den Knopf „📖 Theorie nachlesen“
(nach der Antwort „📖 Theorie dazu“): öffnet die Theorie des Kapitels (Formel, Erklärung, Beispiel) darüber, danach geht es
an derselben Stelle weiter. Gleich auf Android und im Web.
- Keine neuen festen Vorlese-Texte (nur das 60-Tage-Fenster der Tagesmissionen rückt wie bei jedem Bau weiter). versionCode 57, Prüfkette grün.

## ✅ 2.5.9 – Mehrere Lösungen (Marcel 18.11.) · Web = APK = Play
**Grundsatz in KurvenFlug:** Kontrolliert wird der Flug – **jede Einstellung, die trifft, zählt als richtig** (nie nur die
hinterlegte). Der Formel-Detektiv vergleicht die Kurve, nicht die Schreibweise (2(x+1) = 2x+2 wird akzeptiert).
Feste Einzelwerte gibt es nur an zwei Stellen: im **Rechenweg** (Zahl antippen) und in der **Lösungsanzeige nach dem
Rechen-Stern** („So rechnet man's“).

**Suche:** Skript probiert für alle 346 festen Level + 61 Tagesmissionen **jede Reglerstellung** durch und fasst Treffer zu
getrennten Lösungen zusammen (`MultiSolutionScan`). Ergebnis: **73 Level mit mehr als einer Lösung** (4 Profi-Sinus-Level mit 4 Reglern waren zu groß zum Durchprobieren – Flug akzeptiert dort ebenso jeden Treffer):
| Art | Fälle | Entscheidung |
|---|---|---|
| echte Doppellösung der Mathematik: **3-2** (d = 3 oder 7), **4-3** (Wurf 28° oder 63°) | 2 | (a) beide zählen; **Aufgabe nennt jetzt beide Wege** („Zwei Werte für d passen – einer reicht!“, „flach oder steil, beide zählen“) |
| Wurf mit Winkel **und** Tempo frei (4-6, Boss-/Andromeda-/Rettungs-/Tageswürfe) | 13 | (a) „Winkel und Tempo – du entscheidest!“ – alle zählen |
| Gravitation/Schleuder (6-1…6-5, Boss 6: 9) und Laser mit Glas (17-1…17-5 u. Wiederholungen: 8) | 17 | (a) Tüftel-Level, jede treffende Bahn zählt |
| Sinus periodisch / zweiter Wellenberg (5-2, 26-2, 26-4, 5-4 u. a.) | 15 | (b) eindeutig durch „der **erste** Wellenberg“ bzw. „aufsteigend“; andere Treffer zählen trotzdem |
| Parabel „Talstation/Gipfelstation + zweite Station“ (Boss 3, Tagesmissionen, Rettungen), 3-6 – und Gerade 2-8 | 26 | (b) eindeutig durch „Scheitel bei …“; Zweittreffer sind Toleranz-Treffer und zählen trotzdem |
- **Neu (a) für alle Fälle:** Hat das Kind mit anderen Werten getroffen als hinterlegt, zeigt die Lösungsanzeige
  „✅ Deine Einstellung (α = 63°) ist auch richtig – diese Aufgabe hat mehrere Lösungen. Der Lösungsweg zeigt die Lösung α = 27°.“
- **Neuer Test `MultiSolutionTest`:** Kein Rechenweg-Schritt bietet eine Zahl als „falsch“ an, die zu einer anderen treffenden
  Lösung gehört (berücksichtigt die schon festgelegten Werte) – **0 Verstöße**; dazu Test, dass die andere Lösung genannt wird.
- **Marcels Fall:** Der Flug hat nie eine richtige Lösung abgelehnt; sichtbar wurde „nur einer“ in der Lösungsanzeige nach dem
  Rechen-Stern (z. B. Wurf 4-3 mit 63° getroffen, angezeigt wurde nur 27°) – genau das ist jetzt behoben. Falls Marcel ein
  anderes Level meinte: bitte die Level-Nummer schicken.
- versionCode 56, Prüfkette grün.

## ✅ 2.5.8 – fertig für den Betriebstest: Nova abgeschlossen + einheitliches Logo · Web = APK = Play
- **vorlesen/texte.json** (test und nach Freigabe live): **1 162 feste Texte** (153 197 Zeichen, unverändert vertont) +
  **1 797 vorhersehbare Laufzeit-Sätze mit `"laufzeit": true`** (**94 998 Zeichen neu** – unter der 150 000er-Grenze, daher
  ohne Vorankündigung): Volts Begrüßung mit Sternzahl (725 Sätze, 32 517 Z.), Tagesmissionen der nächsten 60 Tage mit
  Tipp und Rechen-Stern (≈ 825 Sätze, 45 057 Z.; jeder Bau schiebt das Fenster weiter), Rechenweg-Abschluss je Level
  (128), Geschichte/Gold-Teile (38), Andromeda (19), Sticker und Gast-Planeten (32), Auffrischen je Kapitel (30).
  Alle als ganze Sätze. Die Satzvorlagen liegen jetzt an einer Stelle (`core/model/VoltLines.kt`), App und Liste haben
  denselben Wortlaut. Nicht vorhersehbar (bleiben bei /jetzt): Rückmeldungen nach Fehlversuchen mit den eingestellten Werten.
- **Klicktest:** alle 30 Kapitel im Browser gegen den echten Server – **30 / 30 Ansagen von Nova = 100 %, 0 × Gerätestimme**.
- **Offline laden:** liest dieselbe texte.json (Android als Asset, Web von der Seite) – nimmt die Laufzeit-Sätze mit;
  Größe kommt live aus `stand-<host>.json` („bytes“/„bytes_original“), stimmt also, sobald das Werk sie vertont hat.
- **Logo** einheitlich (siehe 2.5.7). versionCode 55, Prüfkette grün.

## ✅ 2.5.7 – Logo überall gleich (Marcel 18.11.) · Web = APK = Play
- **Geprüft, war schon gleich** (gleiche Geometrie und Farben, Bilder nebeneinander verglichen): icons/icon.svg,
  icon-512/-192, maskable-512 (PWA), apple-touch-icon, favicon-32, Store-Symbol icon_512, Android-Symbol
  (adaptive icon `ic_launcher_foreground`) und Android-Splash (Android 12+, gleiches Symbol). Web-Ladebildschirm nutzt icon-192.
- **Abweichend – angeglichen:** dort stand nur der Schriftzug „KURVENFLUG“ (bzw. 🤖 im Widget), jetzt mit demselben Logo:
  Kopf der Startseite (neue gemeinsame Komponente `KurvenflugLogo`, Web = Android), Startbildschirm-Widget (groß + klein,
  nutzt direkt die Android-Symboldatei), Teilen-Bilder der Flugbahn (Android `Photo.kt` und Web `kf.js`, gleich gezeichnet).
- Bildschirmfotos 01_start, 47/48_widget angesehen. versionCode 54, Prüfkette grün. Keine neue Dateiendung.

## ✅ 2.5.6 – fertig für den Betriebstest (Nachtauftrag 17.11.) · Web = APK = Play

### 1) Aufgaben geprüft
- **Geprüft:** alle **346 festen Aufgaben** (Kapitel, Extrem-Prüfungen, Bosse, Andromeda, Rettungen) einzeln gelesen +
  Zufallsgeneratoren mit vielen Seeds (Generator alle Stufen × 60 Seeds, 401 Tagesmissionen, Jahreszeiten = **75 Vorlagen**
  aus rund 3 600 erzeugten Aufgaben). Richtigkeit der Lösungen prüft weiterhin `AuditTest` unabhängig nach (eigene Formeln);
  gleichwertige Antworten (z. B. zwei Winkel beim Wurf, zwei d bei Parabeln) werden über den Treffer akzeptiert.
- **Neuer Prüftest `TaskClarityTest`:** jede Aufgabe enthält eine Frage/Aufforderung; keine Formfehler („0x“, „+ 0“, „Wellen-“).
- **Geändert: 41 feste Aufgaben + 2 Generator-Vorlagen** (betrifft alle Zufallsaufgaben dieser Art):
  - Frage ergänzt, wo unklar war, was einzustellen ist: Koordinaten 3, Winkel 4, Zuordnungen 2/5/6 („Wie groß ist k?“),
    Weg-Zeit 1/2/4, Spiegel 2/5, Pythagoras 2/4/5 („Welcher Radius?“), Parabel 3-6, Scheitelform 18-6, Rampe 12-1…12-5
    („Welche Starthöhe h?“), Lichtbrechung 17-1/2/4/5, Wurf 4-2/4-4/4-5/4-6, waagerechter Wurf 27-2…27-5, Trigonometrie
    14-2/14-3, Sinus 5-2/26-3, Logarithmus 10-1/2/4, Zinsen 23-5 (fragte zwei Dinge, nur eins einstellbar).
  - Binomial 30-4: „10 Münzwürfe … p = ?“ verriet p = 0,5 → „10 Versuche … Wie groß ist die Trefferchance p?“.
  - Generator Rendezvous: „y = 0x − 6“ → „y = −6“, „+ 0“ entfällt (auch in der Formelzeile); Generator Sinus:
    „den ersten Wellen-tal“ → „das erste Wellental“ / „den ersten Wellenberg“.
  - (aus 2.5.5) Winkel 5/6, Komma-Funkspruch 15-6, Rendezvous 7-2…7-7.
- Keine falsche Lösung gefunden.

### 2) Nova – ganze Sätze, Gerätestimme nur im Notfall
- **Ursache gefunden:** Laufzeit-Texte über 300 Zeichen (z. B. Rechen-Stern mit Lösungsweg bei Zufallsaufgaben) durften
  nicht an /jetzt und gingen sofort an die Gerätestimme. **Jetzt:** erst der ganze Text (vorab vertont?), sonst in **ganzen
  Sätzen** (≤ 300 Zeichen, nie Wort-Schnipsel) über /jetzt, nacheinander abgespielt. Gerätestimme nur noch, wenn Nova
  wirklich nichts liefert (kein Netz, Server sagt 204); dann **einmal je Sitzung** dezent „🔇 Nova gerade nicht erreichbar“.
  Zeitlimits: Verbindung 5 s, Antwort 20 s (Android), Browser ohne künstliches Limit.
- Spielernamen gehen weiterhin nie an den Server (solche Texte: Gerätestimme).
- **Nova-Abdeckung: 100 %** – Browser-Durchlauf durch alle 30 Kapitel (Level öffnen, Volt liest die Aufgabe) gegen den echten
  Server: 30 von 30 Ansagen von Nova (12 neue Texte frisch über /jetzt, Rest fertige Dateien), **0 × Gerätestimme**.
  Test `NovaTest`: alle vorlesbaren Spieltexte (inkl. Zufallsaufgaben) lassen sich in Sätze ≤ 300 Zeichen teilen.
  Die geänderten Texte stehen in vorlesen/texte.json und werden vom Werk vorab vertont.
- versionCode 53, Prüfkette grün. Android-Wiedergabe weiterhin nur gebaut, nicht auf echtem Gerät gehört.

## 2.5.5 – Fehlerbehebung aus dem Test (Marcel: „unklare Frage“ bei Winkel · Level 6) · Web = APK = Play
- Alle 66 getarnten/Rendezvous-Aufgaben durchgesehen, unklare neu formuliert (auch in Extrem-Prüfung und Rettungsmission,
  die dieselben Texte nutzen):
  - Winkel 6: „Ein Dreieck hat die Winkel 60° und 70°. Wie groß ist der dritte Winkel? Stell genau diesen Winkel ein – dort
    liegt die Station.“ (vorher „Feuere unter dem dritten.“); Tipp nennt jetzt die 180°-Regel.
  - Winkel 5: „Die Station liegt beim Nebenwinkel von 50°. Wie groß ist er? Stell genau diesen Winkel ein.“
  - Brüche 6: „… Rechne aus und flieg genau dorthin.“
  - Rendezvous 2–7: „Wo treffen sie sich?“ ergänzt.
- Nova: die neuen Texte stehen in vorlesen/texte.json; bis das Werk sie vertont hat, gehen sie über /jetzt.
- versionCode 52, Web-/Android-Fingerabdruck neu gesetzt (beide gleich).

## ✅ 2.5.4 – fertig für den Betriebstest (versionCode 51) · Web = APK = Play
- **Musik:** Android startet die Hintergrundmusik jetzt **sofort beim Öffnen** (natives Android, kein WebView – die Sperre
  bis zur ersten Berührung ist entfernt). Web: startet automatisch beim allerersten Tippen/Klick irgendwo (Browser-Regel).
  Kein Musik-Knopf; „Musik aus“ bleibt respektiert; im Hintergrund weiterhin still.
- **Stimme:** nur Nova, Notlösung Gerätestimme – war seit 2.4.0 so. Codesuche „thorsten“/„sherpa“: **0 Treffer** in Code,
  Tests, README und gebauter WebApp (drei Kommentare/Testwerte neutral umformuliert). Treffer nur noch in CHANGELOG und in
  der Historie dieser Meldungsdatei (Protokoll früherer Versionen).
- Prüfkette grün (Tests, Lint, APK, AAB, WebApp). Restpunkte unverändert wie unter 2.5.3.

## ✅ 2.5.3 – fertig für den Betriebstest (versionCode 50) · Web = APK = Play
Begonnenes ist fertig (Nova, Sprechtempo, Offline Standard/Bestqualität, Pro-Abzeichen), keine neuen Funktionen.

**Geprüft**
| Bereich | Wie | Ergebnis |
|---|---|---|
| Alle 30 Kapitel öffnen | Browser, **320 × 640** und **1440 × 900** (je 30 Bildschirmfotos angesehen) | läuft; **Fehler gefunden und behoben:** bei 320 × 640 war „Start“ nur halb sichtbar (Spielfeld zu groß) → Spielfeld wird auf niedrigen Bildschirmen (< 720 dp) kleiner |
| Konsole | beide Größen, alle Kapitel | **keine Fehler** (einzige Meldungen: Stimmen-Server lässt meine Test-Adresse localhost:8765 nicht zu – auf der echten Seite erlaubt, dort getestet) |
| Offline / Flugmodus | WebApp laden, offline neu laden | startet offline ohne Fehler |
| Pro-Code | Freischalt-Link `?pro=…` → gespeichert, nach Neuladen „⭐ Pro aktiv“ sichtbar | ok |
| Stimme / Musik / Einstellungen merken | neuer Test `SettingsPersistenceTest` (Pro-Code, Vorlesen an/aus, Tempo, Bestqualität, Musik, Lautstärken, Ton, Vibration, Updates, Volt-Stimme) + Browser (Tempo 1,3 nach Neuladen aktiv) | ok |
| Update von der Vorversion | Room-Migrationstests (Spielstand), Test mit Einstellungen aus 2.0–2.3 (alte Thorsten-Schlüssel stören nicht, Pro-Code bleibt), Web-Schlüssel unverändert; Thorsten-Reste werden beim Start aufgeräumt | kein Datenverlust |
| Nova | Browser gegen den Server: feste Ansage, /jetzt, Tempo, Offline laden/abbrechen/fortsetzen/löschen, Bestqualität mit Rückfall | ok |
| Gesamt | 275 Tests, Lint (GitHub + Play), APK, AAB, WebApp | grün |

**Bekannte Restpunkte**
- **Android nicht auf einem echten Gerät gehört:** Nova-Wiedergabe, Sprechtempo und Offline-Laden sind gebaut und laufen in Tests/Browser gleich, aber nicht auf einem Handy angehört – bitte im Betriebstest zuerst prüfen.
- 320 × 640: bei sehr langen Volt-Texten (z. B. Binomialverteilung) ist „Start“ noch knapp am Rand – der Bereich lässt sich scrollen.
- Inhaltlich unverändert offen (aus der Korrektheitsprüfung): 58 Level ohne Live-Rechnung, 31 ohne Rechen-Stern – kein Fehler, nur weniger Hilfe.
- Bestqualität: der Server füllt `original/` noch; bis dahin speichert die App die normale Fassung als Standard.
- Play-Store bleibt bis nach dem Betriebstest liegen (Material vorhanden).
- Diese Sitzung kann dir nicht direkt schreiben (vertrauenswürdiges Gerät nötig) – Meldungen stehen hier.

## 2.5.2 – Bestqualität: Rückfall sauberer · Web = APK = Play
- Liefert `original/` noch 404, wird die normale Datei jetzt **als Standard** gespeichert (nicht mehr als Bestqualität).
  Folge: Sie spielt sofort offline, zählt aber nicht als Bestqualität – ein späteres Tippen auf „Bestqualität“ lädt genau
  die nachgeholten Originale. (Vorher hätte ein früher Ladevorgang die Standard-Datei dauerhaft als Bestqualität behalten.)
- versionCode 49.

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
