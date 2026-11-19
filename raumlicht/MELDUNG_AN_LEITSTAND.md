# Meldungen der Raumlicht-Sitzung an den Leitstand

Neueste oben.

## 19.11.2026 – 2.18.36 auf test: Familien-Speicher nach FAMILIEN-SPEICHER.md v1.5 (von Marcel hier direkt bestätigt)

- **Einheitlicher Eltern-Hinweis** wörtlich (`Familie.NOTE`), darunter „Datenschutzerklärung ›“ und **„Einverstanden“** – erst danach gibt es „Familie anlegen“ und „Familie beitreten“ (auch beim Öffnen mit `#familie=…`). Das Einverständnis wird nicht gespeichert; es wird bei jedem Anlegen/Beitreten neu gegeben.
- **Protokoll** auf der Familien-Seite (`GET …/protokoll`, Einträge `{art, t}`; „angelegt“, „wiederhergestellt“ lesbar, Unbekanntes wörtlich).
- **Familie löschen:** nur hinter dem Tor, Warnung wörtlich (`Familie.DELETE_WARNING`), Bestätigung durch Eintippen der Familien-Nummer (gelesen wie der Server: ohne Bindestrich, O→0, I/L→1), dann `loeschen()`; lokal wird nur `raumlicht.familie` entfernt. Ein einzelnes Kind entfernen gibt es nicht.
- **Andere Geräte bei 404:** „Diese Familie gibt es nicht mehr. Lernstand bleibt auf diesem Gerät.“ – die Verbindung wird getrennt, nichts wird gelöscht. (Der Baustein meldet 404 für „Familie weg“ und „noch kein Stand“ gleich; die Brücke unterscheidet es über `/liste`.)
- Weiter nur Teststufe (`Familie.ENABLED = false`). Zahlen: siehe Nachtrag.

## 19.11.2026 – 2.18.35 auf test: Familien-Speicher angebunden (nur Teststufe; von Marcel hier direkt bestätigt: „daneben, eigene Version nur auf der Teststufe“)

**Gebaut:**
- `familie.js` @ 1f4b377 unverändert im Web-Paket (`web/src/wasmJsMain/resources/familie.js`, sha256 3016aabd…e640; `node familie/tests/familie.mjs` → 51 Prüfungen). Wird erst geladen, wenn die Familien-Seite etwas tut; steht in der Offline-Liste des Service Workers.
- Brücke `familieRuf` (`web/…/Browser.kt`): neu (anlegen mit REC_PUB + X-Notfall-Pruef, Index mit Kind und App `raumlicht`), kinder, kind, sichern (`standSichern` mit rev/If-Match), laden. Aufrufe ohne Cookies und ohne Referrer.
- Logik `core/model/…/Familie.kt`: Lernstand des Kindes = Sicherungs-Inhalt **ohne** `proCode`/`kiCode`, ohne Geräte-Kennungen, ohne Eltern-Kopplung, ohne den Familien-Link selbst. Zustand je Gerät in `raumlicht.familie` (Familie, kindId, Nummer, rev, Prüfwert, Spitzname) – wandert in keine Sicherung.
- Seite „Familie“ (`shared/…/settings/FamilieScreen.kt`), Eintrag in den Einstellungen; **Weg nur durchs Eltern-Tor** – auch `?ziel=familie` und `#familie=…` zeigen zuerst das Tor. Der Schlüssel verschwindet beim Öffnen sofort aus der Adresszeile.
- Gesichert wird nach dem Lernen von selbst, nur bei Änderung. **Konflikt (409):** nichts wird überschrieben; die Eltern wählen „Stand aus der Familie holen“ oder „Stand dieses Geräts behalten“. Beitritt auf einem Gerät mit eigenem Lernstand fragt ebenso.
- Sichtbar nur auf `lernen-test.` / `raumlicht-test.` (Server `https://lernen-test.mathenachhilfe-online.com/familie/v1/`). Live: `Familie.ENABLED = false` → kein Eintrag, kein Aufruf. Zum Freischalten später: Schalter umlegen, neue Version.
- Eltern-Kopplung (`eltern/v1`) unverändert daneben.

**Noch nicht gebaut (bewusst):** Rechte (`/rechte`, `einloesen`, Sperrliste) – Pro/KI laufen weiter über die Codes auf dem Gerät; Familie auf dem Server löschen (betrifft alle Apps – bitte gemeinsame Regel); Kind umbenennen; Zusammenführen zweier Stände (derzeit Entweder-oder mit Nachfrage).

**Bitte prüfen / entscheiden:** (1) Wortlaut zur Notfall-Kopie – Raumlicht sagt Eltern ausdrücklich, dass der Betreiber den Zugang wiederherstellen kann (Text in `Familie.NOTE`, Datenschutz Punkt 9); bitte für alle Apps gleich. (2) Lässt der Test-Server Aufrufe von `raumlicht-test.` (andere Herkunft) zu? Auf `lernen-test.` ist es dieselbe Herkunft.

**Geprüft:** Logik-Tests, Oberflächen-Tests (neu `familienSpeicher`, `familienLinkFuehrtDurchsElternTor`), Lint, 201 Logik-Tests, 153 Oberflächen-Tests, Lint, 192 Browser-Prüfungen (166 bisherige + 26 neue in `web-familie.mjs`: Tor, anlegen, Server sieht nichts Lesbares, zweites Gerät holt Stand, Konflikt; je Wasm und JS). Bild `screenshots/95_familie.png`.

**Nachtrag – echter Rundlauf auf `lernen-test.mathenachhilfe-online.com/raumlicht/` (2.18.35):** ausgelieferte `familie.js` hat die Prüfsumme 3016aabd…; im Browser auf der Teststufe: anlegen ✓, Index ✓, Stand sichern (rev 1) ✓, mit dem Familien-Link laden ✓, veraltetes rev → `konflikt` mit aktuellem Stand ✓, Prüf-Familie gelöscht ✓ (danach 404). Die Seite selbst (Eltern-Tor, Knöpfe) ist mit dem nachgebauten Server geprüft, nicht von Hand auf der Teststufe und nicht auf einem echten iPhone/iPad. Live zeigt inzwischen 2.18.34; `/familie/v1/` antwortet live mit 404.

## 19.11.2026 – 2.18.34 auf test: Eltern-Tor vor dem Shop, Lux wirbt nicht mehr (Marcel hat hier selbst „ja“ gesagt – zu meinem Vorschlag, nicht zur vollen gemeinsamen Vorgabe)

**Umgesetzt (Entscheidung „wie Mathe-Trainer“):**
- `ElternTor` / `ElternTorDialog`: Malaufgabe 13–19 × 6–9 **in Worten** („siebzehn mal acht“), eigene Ziffern-Tastatur (keine Auswahlknöpfe, kein Textfeld), nach **einem** Fehler sofort eine neue Aufgabe, kein Zeitdruck, „Abbrechen“ jederzeit, Beschreibungen für Bildschirmleser. Nach dem Lösen 10 Minuten offen, nur im Arbeitsspeicher.
- Hinter dem Tor: „Zum Shop: Raumlicht Pro“ (`ProScreen.kt`) und „Zum Shop: KI-Abi-Pass“ (`KiScreen.kt`). Hinweis im Tor und unter den Knöpfen: „Wer unter 18 ist, kauft nicht selbst.“ Preise und sachliche Infos bleiben sichtbar.
- Lux nach 3 KI-Proben: nur „Deine 3 Proben sind aufgebraucht.“ – kein Hinweis auf den Pass, der Knopf „KI dazubuchen“ ist entfernt. Die KI-Seite bleibt über die Einstellungen erreichbar.
- Zählung: neues Ereignis `elternbereich` (ohne Ort/Thema), wenn das Tor gelöst wird. **Bitte in die Positivliste des Zählers aufnehmen.**

**Bewusst NICHT hinter dem Tor – mit Marcel so abgesprochen:**
- Eltern-Kopplung (bei Raumlicht koppelt die Schülerin bzw. der Schüler selbst: „Du entscheidest, ob deine Eltern mitlesen“),
- „← Alle Apps“, die Links „Mehr üben im Mathe-Trainer“, der Probestunden-Hinweis.
- Kein Vorlese-Knopf sichtbar: Das Vorlesen ist in Raumlicht seit 2.18.14 abgeschaltet; der Knopf ist gebaut und erscheint, sobald es wieder eine Stimme gibt.
- Hinweis von mir an Marcel (er kennt ihn): Eine Malaufgabe hält Oberstufenschüler kaum auf; das Tor ist hier eher ein deutlicher Hinweis als eine Hürde.

**Geprüft:** `ElternTorTest` (alle 28 Aufgaben, Worte ohne Ziffern, neue Aufgabe nach Fehler, 10 Minuten), `AppRootTest.elternTorVorDemShop` (falsch → neue Aufgabe und nichts geht auf; richtig → Tor auf, gezählt; 10 Minuten offen; danach wieder zu; Abbrechen), `kiTutor` (kein Pass-Hinweis, kein Kauf-Knopf). Bild: `screenshots/94_eltern_tor.png`. Enthält 2.18.33 unverändert (Umzug).

## 19.11.2026 – 2.18.33: Umzugs-Version für lernen.mathenachhilfe-online.com/raumlicht/ (Marcel hat hier selbst „Ja ja“ gesagt: Umzug heute, Reihenfolge erst 2.18.32 live, dann diese)

**Dieselbe Fassung läuft alt und neu.** Enthält alles aus 2.18.29–2.18.32.

**Pflichtpunkte aus UMZUG-LERNEN.md – erledigt:**
- `sw.js`: beim Aktivieren werden nur Speicher mit Präfix `raumlicht-` gelöscht; gesucht wird nur im eigenen Speicher (`caches.open(CACHE).match`), nur für Anfragen im eigenen Ordner (`registration.scope`).
- `diagnose.html`: „zurücksetzen“ löscht nur `raumlicht-*` und meldet nur die eigene Anmeldung ab (`getRegistration('./')`).
- Kein `clear()`. localStorage-Präfix `raumlicht.`, sessionStorage `rl-…`, Cache-Namen `raumlicht-<Version>`, `raumlicht-nova(-offline)`. Kein IndexedDB, kein Mikrofon.
- Manifest: `id`, `start_url`, `scope` = `./` (ergibt an der neuen Adresse `/raumlicht/`, eindeutig).
- Test/Live: `isTestSite()` erkennt `raumlicht-test.…` und `lernen-test.…` am Hostnamen (KI-, Eltern-Server, „Alle Apps“).

**Umzug des Lernstands (im Startablauf von `index.html`, vor dem Laden der App):**
- ALT (`raumlicht(-test).mathenachhilfe-online.com`): erst prüfen, ob die neue Adresse bereit ist – ein Bild `umzug-bereit.png` von dort laden (die Datei gibt es erst ab 2.18.33; so ist sicher, dass die neue Adresse den Umzug annimmt, und niemand landet vor einer Fehlerseite). Dann alle `raumlicht.*`-Schlüssel außer Browser-Merkern (`mitschrift`, `startfehler`, `modus.`, `sparsam`, `test`) einsammeln, AES-GCM-256 mit 12-Byte-IV, `PUT trainer(-test)…/eltern/v1/s/<id>` mit `X-Schreib`, danach `location.replace` auf `lernen(-test)…/raumlicht/` + Query + `#umzug=<id>.<key>.<schreib>` (vorhandene `#pro=`/`#ki=` mit `&` angehängt). Ohne Lernstand ohne `#umzug`. Bei jedem Fehler keine Weiterleitung; nächster Start, nächster Versuch. Der Stand an der alten Adresse bleibt liegen (Rückweg).
- NEU: `GET`, entschlüsseln, nur einspielen, wenn dort noch kein `raumlicht.*`-Stand liegt, `DELETE` mit `X-Schreib`, `umzug` aus der Adresse nehmen.
- **Abweichung von UMZUG-LERNEN.md – bitte dort korrigieren:** Der PUT-Body ist **`text/plain` mit `<iv>.<ct>`**, nicht JSON `{"d":…}`. Am echten trainer-test geprüft (19.11.): `text/plain` → 204, JSON → **400**. GET liefert `{"d":"<iv>.<ct>","t":…}`.

**Am echten Server geprüft (trainer-test, 19.11.):** PUT s **ohne Kopplung** 204 (Origin raumlicht-test) · GET mit Origin `lernen-test.…` 200, CORS passt · OPTIONS für DELETE von `lernen-test.…` 204 · DELETE 204 → GET 404. Meine Testdaten sind gelöscht.
**Nicht prüfbar:** `https://lernen-test.mathenachhilfe-online.com/raumlicht/` und `https://lernen.…/raumlicht/` antworten mit **503** – den echten Rundlauf alt → neu konnte ich noch nicht fahren. CORS von `lernen(-test).…` für `/zaehler/v1/e` und `/api/raumlicht/ki` habe ich nicht geprüft.

**Browser-Test `scene/test/web-umzug.mjs` (35 Prüfungen, Wasm und JS, drei Server auf dem Rechner = echte getrennte Speicher):** Umzug gelingt mit `?ziel=` und Raute-Teil · nur Verschlüsseltes am Server, Schlüssel nie · Sicherung nach dem Abholen gelöscht · Ziel hat schon Daten → nichts überschrieben · Server-Fehler → keine Weiterleitung, nächster Start klappt · neue Adresse antwortet mit Fehler → nichts abgelegt, keine Weiterleitung · ohne Lernstand direkt hinüber · Umzugs-Link ohne Sicherung → normaler Start · Service Worker und Diagnose-Seite lassen einen fremden Speicher (`andere-app-1`) stehen.

**NACHTRAG 19.11., 9:30 Uhr – echter Rundlauf auf der Test-Stufe bestanden** (nachdem das Zertifikat für lernen-test stand):
- `raumlicht-test.…/?ziel=galton#anker=1` mit Lernstand → `PUT 204` → Weiterleitung → `lernen-test.…/raumlicht/#anker=1`, dort 2.18.33, App gestartet, Ziel „Galton-Brett“ geöffnet, Raute-Teil erhalten, „umzug“ aus der Adresse entfernt.
- Sterne, Monatssummen und Einstellungen sind an der neuen Adresse da; der Browser-Merker (`mitschrift`) kam nicht mit. `GET 200`, `DELETE 204` – die Einmal-Sicherung ist gelöscht.
- Zweiter Besuch der alten Adresse, nachdem am Ziel weitergelernt wurde: Stand am Ziel unverändert, neue Sicherung geholt und gelöscht.
- CORS von `lernen-test.…`: Zählung (`/zaehler/v1/e`) 204 mit passender Freigabe; KI auf trainer-test 204 mit passender Freigabe; KI live für `lernen.…` 204 mit passender Freigabe (nur Vorab-Anfrage geprüft, keine echte KI-Frage gestellt).
- Weiter ungeprüft: iPhone/iPad, installierte WebApp.

**Was du tun musst – in dieser Reihenfolge:**
1. 2.18.32 live nehmen (Marcels Reihenfolge) – oder gleich 2.18.33, sie enthält 2.18.32.
2. **Zuerst die NEUE Adresse** mit 2.18.33 bestücken (`raumlicht/webapp/` nach `lernen-test.…/raumlicht/`, dann `lernen.…/raumlicht/`). Solange dort nichts oder eine ältere Fassung liegt, zieht niemand um – die alte Adresse läuft normal weiter.
3. CORS/Allowlist für `lernen(-test).mathenachhilfe-online.com`: `/eltern/v1/b`, `/eltern/v1/s`, `/zaehler/v1/e`, `/api/raumlicht/ki`.
4. Danach die alte Adresse auf 2.18.33 bringen. Ab dann zieht jeder beim nächsten Öffnen um.
5. Links umstellen (Shop-Mails `#pro=`/`#ki=`, /lern-apps/, Querverweise der anderen Apps); alte Links funktionieren über die Weiterleitung weiter.
- **Rückweg:** alte Adresse zurück auf 2.18.32 – der Lernstand liegt dort unverändert (Stand des Umzugs).
- **Offen:** installierte WebApp (Startbildschirm) muss unter der neuen Adresse neu installiert werden; auf iPhone/iPad nicht getestet. Löschfrist für nicht abgeholte Einmal-Sicherungen (heute 90 Tage wie beim Bericht?) bitte festlegen. Datenschutztext: `store/datenschutz-abschnitt.md` Punkt 8.

## 19.11.2026 – 2.18.32 auf test: Umbau aufs Profil „Geometrie im Raum + Analysis & Zufall erleben“ (von Marcel direkt bestätigt am 18.11.; auf heute vorgezogen laut AppZentrale)

**Herausgenommen (gestrichen, nicht verschoben – der Mathe-Trainer hat laut seinem Abgleich alle Aufgabentypen):**
- 10 Kapitel, 52 Aufgaben: 12 Rotationskörper, 13 Wahrscheinlichkeit, 14 Kurvendiskussion, 15 Funktionen bauen, 16 Bäume/Tafeln/Binomial, 17 Ableiten & Anwenden, 22 Integrale & Flächen, 23 Erwartung/Sigma/Testen, 24 Binomialverteilung, 25 e-Funktion & Logarithmus.
- 3 von 7 Abi-Prüfungsaufgaben (Lichtbogen, Lostrommel, Einlass). Im endlosen Üben und in der Tagesaufgabe die Aufgabenart „Rotationskörper-Volumen“. „Ohne Hilfsmittel“ zieht 6 Aufgaben nur noch aus der Geometrie.
- „Kommt bald“: Unabhängigkeit, Zufallsgrößen, Normalverteilung, Sinus/Kosinus, Funktionenscharen → ersetzt durch Schattenwurf, Körper im Raum, Lage von Gerade und Ebene (LGS bleibt).

**Bleibt / neu:**
- **92 Aufgaben in 16 Kapiteln**, angezeigt lückenlos 1–16. Intern bleiben die Nummern 1–11, 18–21, 26 (Sterne, Links von außen wie `?ziel=level/5-6`, `level/3-1`, `level/5-1` gelten weiter). 4 Abi-Prüfungsaufgaben (Festivalzelt, Laser & Leinwand, Drohnen-Show, Planetarium).
- **Startseite:** statt drei Gebiets-Reitern jetzt „📐 Geometrie im Raum“ und daneben „🎢 Analysis & Zufall erleben ›“.
- **Seite „Analysis & Zufall erleben“** (`?ziel=erleben`): Kurve abfahren, Achterbahn-Baumeister, Geisterfahrt gegen f′, Rotationskörper-Labor, Zufallsmaschine, Galton-Brett, Binomial-Labor – und unten „Mehr üben im Mathe-Trainer“ mit zehn Links (`#/u/regeln`, `kurve`, `extremwert`, `integral`, `a-rotation`, `expln`, `s-baum`, `s-bedingt`, `s-verteilung`, `s-test`).
- **Rotationskörper-Labor** (`?ziel=rotation`, frei, kein Pro): sieben Vorlagen (Zylinder, Kegel, Kegelstumpf, Vase, Trichter, Kugel, Kelch) und eigene Kurve (Gerade, Wurzel, Halbkreis, Parabel) mit eigenen Zahlen und Grenzen; der Körper wächst in 3D. Volumen exakt als Vielfaches von π und gerundet, Rechenweg in sechs Schritten, Probe mit der Schulformel bei Zylinder, Kegel, Kugel; verständliche Rückmeldung bei unpassender Eingabe.

**Geprüft:**
- Jedes Volumen des Labors ist doppelt gerechnet: exakt (Stammfunktion, Brüche) und mit 4000 bzw. 20000 Scheiben – für alle Vorlagen und alle 400+ einstellbaren gültigen Kombinationen (`ErlebenTest`). Bekannte Körper gegen die Schulformeln (Zylinder 16π, Kegel 9π, Kugel 36π, Halbkugel 18π).
- Test, dass nirgends eine gestrichene Aufgabe auftaucht (Tour, Abi-Teil, Pflichtteil, Tagesaufgabe, Üben, Formelsammlung-Sprünge, „Kommt bald“); alte Links auf gestrichene Aufgaben führen zur Startseite.
- 189 Logik-Tests, 150 Oberflächentests, Lint, 131 Browser-Prüfungen (Wasm und JS) – alle bestanden. Bilder: `screenshots/92_analysis_zufall_erleben.png`, `93_rotationskoerper_labor.png`.

**Bitte von dir / offen:**
- **Shop-Texte:** Raumlicht Pro und KI-Abi-Pass sollten nicht mehr „Analysis und Stochastik“ versprechen. In der App nennt Pro jetzt „alle 92 Aufgaben in 16 Kapiteln“, 4 Prüfungsaufgaben; der KI-Tutor erklärt weiter jede Aufgabe. Im Server-Prompt (`raumlicht/server/raumlicht-ki.js`) steht jetzt „Analytische Geometrie und Vektoren im Raum“ – bitte übernehmen.
- **/lern-apps/-Text und Eltern-Seite:** Raumlicht meldet im Eltern-Bericht nur noch ein Gebiet („Vektoren und Geometrie“, 92 Aufgaben).
- **Noch nicht gelöscht:** Die Definitionen der gestrichenen Kapitel liegen ungenutzt im Quelltext (`Levels.removed`). Ich lösche sie nach deiner Live-Freigabe, damit ein Zurück bis dahin leicht bleibt.
- **Android-Widget „Lernstand“** (APK pausiert) zeigt noch drei Balken, zwei davon bleiben leer. Wird angepasst, falls die APK wieder ausgeliefert wird.
- Links zum Mathe-Trainer: Die zehn Kennungen stammen aus seiner Liste vom 18.11.; ich habe sie nicht einzeln im Browser geöffnet.

## 18.11.2026 – 2.18.31 auf test: Knopf „← Alle Apps“ (Marcel hat hier selbst „Ok“ gesagt)

- **Wo:** oben auf der Startseite neben dem Zahnrad; am PC in der Kopfzeile der rechten Spalte. Bilder: `screenshots/91_alle_apps.png` (Handy), `screenshots/90_startseite_pc.png` (PC).
- **Ziel:** `https://www.mathenachhilfe-online.com/lern-apps/?src=raumlicht-kopf`, auf raumlicht-test `https://staging.…/lern-apps/?src=raumlicht-kopf` (beide am 18.11. mit 200 geprüft).
- **Verhalten:** im Browser im selben Tab (`location.assign`); läuft die WebApp installiert (`display-mode: standalone`), öffnet der normale Browser (`window.open`).
- **Tippfläche** mindestens 44 dp hoch (Test), heller Text auf dunklem Grund, kein Preis, kein Kaufhinweis. Auf schmalen Handys (320 px) schrumpft der Titel, der Knopf bleibt ganz sichtbar.
- **Abweichung, mit Marcel so abgesprochen:** Der Knopf steht **nur auf der Startseite**, nicht in den Kopfzeilen der Übersichten („Mehr“, Einstellungen …). Dort sitzt an derselben Stelle der Zurück-Pfeil zur Startseite; zwei Rückwege nebeneinander würden verwirren. In Aufgaben, Spielen und Prüfungen erscheint er nicht.
- **Ohne Logo-Zeichen:** Es liegt mir nicht als Datei vor. Wenn du es als SVG oder PNG ins Repo legst (`raumlicht/shared/…/composeResources` oder nenn mir den Pfad), baue ich es ein.
- Keine Zählung am Knopf (die Herkunft steht in `src=`). Tests: `AppRootTest.alleAppsObenAufDerStartseite`, `web-breit.mjs` (14 Prüfungen, Handy und PC, Wasm und JS).

## 18.11.2026 – Dauerregel übernommen: großes Inhaltsupdate dienstags und freitags (Marcel hat hier selbst „Ja“ gesagt)

- Die geplante Aufgabe läuft jetzt **dienstags und freitags um 8:47 Uhr** (vorher mittwochs), erstmals **Freitag, 20.11.**; Ziel ist raumlicht-test bis 16 Uhr. Die Monatsprüfung am 24. läuft unverändert weiter.
- Pflichtpunkte stehen im Auftragstext und in `PRUEFPLAN.md`: alle Schritte im Lösungsweg, jede Formel mit Beispiel in der Theorie, jedes Ergebnis nachgerechnet. Die Meldung nennt je Update: was neu ist, wie viele Aufgaben, wie geprüft.
- **Umfang (Marcel direkt: „Aber heißt nicht sparsam sein … es soll voran gehen.“):** Richtwert je Update ein ganzes neues Thema als Kapitel mit mindestens 6 bis 8 Aufgaben samt Theorie, dazu nach Möglichkeit ein zweites Thema, weitere Aufgaben, Korrekturen oder eine neue Option. Gründlich prüfen und viel liefern; nur eine einzelne Aufgabe, die sich nicht sauber nachrechnen lässt, bleibt draußen und steht in der Meldung.

## 18.11.2026 – 2.18.30 auf test: Klausur-Hinweis → Probestunde (Marcel hat hier selbst „ja“ gesagt)

- **Umgestellt:** Text „In N Tagen ist deine Klausur. Eine kostenlose Probestunde kann vor der Arbeit helfen.“, Knopf „Kostenlose Probestunde anfragen“. Zählung `probestunde_klick` mit `ort=klassenarbeit`; **`crashkurs_klick` ist aus der App entfernt** (du kannst es aus der Positivliste nehmen). `nachhilfe_hinweis` mit `ort=klassenarbeit` bleibt.
- **Abweichung 1 – Adresse:** `https://www.mathenachhilfe-online.com/probestunde-anfrage/` liefert **404** (18.11., 20:55 Uhr). Der Link zeigt deshalb auf die bestehende `…/probestunde/?src=raumlicht-klassenarbeit&thema=<id>` (wie der Hinweis im Lernstand). Wenn du `/probestunde-anfrage/` einrichtest, melde es – dann stelle ich um.
- **Abweichung 2 – `klasse`:** Raumlicht kennt keine Klassenstufe (Oberstufe), der Wert fehlt im Link. `thema` fehlt wie bisher, wenn der Plan Kapitel aus mehreren Gebieten enthält.
- Enthält die PC-Ansicht aus 2.18.29. Alle Tests bestehen (Browser: 126 Prüfungen).

## 18.11.2026 – 2.18.29 auf test: PC-Ansicht (Marcel direkt, mit Bildschirmfoto von live: „am PC sieht das so nicht toll aus … nicht nur strecken“)

- **Startseite ab 900 dp Breite im Querformat zweigeteilt:** links die Bühne über die ganze Höhe mit Titel, „Tour starten“ und den vier Kacheln; rechts (40 %, 440–640 dp) Gebiete, alle Kapitel umbrechend statt zum Wischen, darunter die Aufgaben als Zeilen mit Namen. Bild: `screenshots/90_startseite_pc.png`.
- **Lese- und Auswahlseiten** (Einstellungen, Pro, KI, Eltern, Mehr, Lernstand, Klausurplaner, Spickzettel, Abzeichen, Abi-Liste, Üben-Liste, Schritte-Liste, Tagebuch, Kalender) stehen ab 860 dp als Spalte in der Mitte. Aufgaben und Spiele nutzen weiter die volle Breite (dort gab es schon eine Queransicht).
- Handy-Ansicht unverändert. Neuer Browser-Test `scene/test/web-breit.mjs` (9 Prüfungen, Wasm und JS); `web-update.mjs` an die neue Lage der Kachel „Mehr“ angepasst. Enthält alles aus 2.18.27 und 2.18.28.
- **Noch nicht angefasst:** das Begrüßungsfenster einer Aufgabe zieht sich am PC noch über die ganze Breite.

## 18.11.2026 – 2.18.28 auf test: Eltern-Koppeln für alle, Update 11 (Marcel hat hier selbst „jo“ gesagt)

- **`Eltern.ENABLED = true`.** Geprüft am 18.11.: `https://www.mathenachhilfe-online.com/eltern-bericht/` und die staging-Adresse liefern 200. Live zeigt der Link auf www und schreibt nach `trainer.…`, auf raumlicht-test auf staging und `trainer-test.…`.
- **Update 11, Punkt 1 – drin:** Die WebApp liest `#pro=CODE` und weiter `?pro=CODE`; ebenso `#ki=CODE` und `?ki=CODE` für den KI-Abi-Pass. Der Wert hinter dem „#“ hat Vorrang. Danach nimmt `history.replaceState` den Code in beiden Formen aus der Adresszeile; anderes hinter dem „#“ bleibt stehen. Browser-Test `web-pro.mjs` (10 Prüfungen): Bei `#pro=` sieht der Server den Code nicht. **Du kannst die Links in Karten und Mails auf `…/#pro=CODE` und `…/#ki=CODE` umstellen, sobald 2.18.28 live ist** (vorher nicht – 2.18.27 und älter lesen nur `?pro=`).
- **Punkt 2 – war schon so:** Code-Eingabe von Hand gibt es für Pro („Raumlicht Pro“ → Feld „Code“) und für den KI-Abi-Pass (KI-Seite → Feld „Code“), auch in der installierten WebApp. Zusätzlich holt die Sicherung beide Codes auf ein neues Gerät.
- **Punkt 3 – gegengeprüft:** kein `sendBeacon` im Code. Die Zählung nutzt `fetch` mit `credentials:'omit'`, `keepalive:true`, `Content-Type: text/plain`, `mode:'no-cors'`.
- Alle Tests wie bei 2.18.27 (Browser: 117 Prüfungen, 0 Fehler).
- **Echt gekoppelt auf raumlicht-test (2.18.28), 18.11.:** Koppeln → Bericht und Sicherung auf trainer-test (GET s 200) → Eltern-Seite auf staging zeigt „Euer Kind · Raumlicht“, Lernzeit, Aufgaben, Fortschritt je Gebiet, **Verlauf** (September bis November 2026) und den Knopf „Neues Gerät des Kindes? Fortschritt übertragen“; keine Fehler der Seite. Danach DELETE b 204, GET s 404 – nichts von mir liegt mehr dort.
- **Kleinigkeit auf deiner Seite:** „Stand 2026-11-18 19:13“ ist die Weltzeit aus `stand` (`…Z`), nicht die deutsche Uhrzeit (20:13). Bitte beim Anzeigen in Ortszeit umrechnen.

## 18.11.2026 – 2.18.27 auf test: verschlüsselte Sicherung, „Fortschritt wiederherstellen“, „verlauf“ (Marcel hat hier selbst „ja“ gesagt)

**Gebaut nach deinem Zusatz-Auftrag, bitte prüfen. Live erst nach deiner Prüfung; `Eltern.ENABLED` bleibt `false`, bis die Eltern-Seite auf www steht (dort am 18.11. noch 404, auf staging inzwischen 200).**
- **Sicherung:** geht zusammen mit dem Bericht raus (gleiche 10-Minuten-Grenze), nur wenn sich der Speicherstand geändert hat. `PUT …/eltern/v1/s/<id>`, `X-Schreib`, `text/plain`, `<iv>.<ciphertext>`, AES-GCM-256, gleicher Schlüssel. Klartext `{format:1, app:"raumlicht", v, stand, schreib, daten:{…}}`.
- **`daten`** = der ganze Browser-Speicher der App als Schlüssel/Wert-Paare (Sterne, Rekorde, Lern-Protokoll, Monatssummen, Einstellungen, Pro-Code, KI-Abi-Pass, Spitzname der Kopplung). **Nicht** drin: `kiDevice` (Geräte-Kennung des KI-Tutors), die Kopplung selbst, Sende-Merker, Diagnose-Merker des Browsers. Eine eingespielte Sicherung kann diese Schlüssel auch nicht setzen.
- **Größe:** höchstens 512 KB nach Verschlüsselung; sonst fallen zuerst Tagebuch und gespeicherte Bahnen weg, passt es dann noch nicht, wird nichts gesendet. Real sind es wenige KB.
- **Wiederherstellen:** „Eltern-Handy koppeln“ → Karte „Neues Gerät? Fortschritt wiederherstellen“ → Feld „Kopplungs-Link einfügen“. `GET …/s/<id>` ohne Schreibrecht, entschlüsseln, `app` prüfen („Dieser Link gehört zu einer anderen App.“), 404 → „Keine Sicherung gefunden.“ Vorhandener Lernstand → erst Nachfrage („Ja, ersetzen“ / „Abbrechen“). Danach sind id, key und schreib gespeichert, das Gerät ist wieder gekoppelt. Kein QR-Scan (die WebApp nutzt keine Kamera).
- **`verlauf`** im Bericht: `[{monat:"2026-11", min, aufgaben, richtigProzent}]`, höchstens 12, älteste zuerst. `richtigProzent` ist in Monaten ohne Aufgaben `0` (Feld immer vorhanden). Die Monatssummen bleiben dauerhaft auf dem Gerät; beim ersten Mal werden sie aus dem 60-Tage-Protokoll übernommen. Leere Liste, wenn noch nichts gelernt wurde.
- **Datenschutz:** dein Satz steht in den Einstellungen („Datenschutz“) und in `store/datenschutz-abschnitt.md` (Punkt 7).

**Geprüft**
- Echter Rundlauf gegen **trainer-test** (18.11., mit Zufallswerten, danach gelöscht): PUT b 204 · PUT s 204 · GET s 200 `{d,t}`, entschlüsselt = Klartext · PUT s mit fremdem Schreib-Geheimnis **403** · PUT s mit dem aus der Sicherung gelesenen `schreib` 204 · CORS für `raumlicht-test.…` passt (OPTIONS 204, `Content-Type, X-Schreib`) · DELETE b 204 → GET s **404**.
- Browser-Test `scene/test/web-eltern.mjs` (36 Prüfungen, Wasm- und JS-Fassung, nachgebauter Server): Sicherung verschlüsselt hochgeladen, frisches Profil holt sie mit dem Link, **derselbe Lernstand** ist da, das neue Gerät schreibt weiter, fremder Schlüssel 403, ohne Änderung keine zweite Sicherung.
- Logik-Tests (`ElternTest`) und Oberflächentest (`AppRootTest.sicherungUndWiederherstellen`, Bild `screenshots/89_fortschritt_wiederherstellen.png`).

**Bitte von dir**
- Auf deiner Eltern-Seite den Monatsverlauf und den Knopf „Neues Gerät des Kindes? Fortschritt übertragen“ gegen einen Raumlicht-Bericht von der Testseite prüfen.
- Hinweis an Marcel steht im CHANGELOG: Wer den Kopplungs-Link hat, kann den Lernstand samt Freischalt-Codes auf ein anderes Gerät holen.

## 18.11.2026 – OFFENE PUNKTE AN DICH (Marcel: „klär deine offenen Punkte mit dem Leitstand“)

Eine Nachricht an deine Sitzung kann ich von hier nicht zustellen (`elevated_session_untrusted_device`), darum gesammelt hier. Was ich selbst prüfen konnte, habe ich geprüft.

**1. Die Eltern-Seite fehlt – von mir geprüft, bitte bauen und melden.**
- `https://staging.mathenachhilfe-online.com/eltern-bericht/` → **404**; `https://www.mathenachhilfe-online.com/eltern-bericht/` → **404** (18.11.2026, 17 Uhr). Die Server-Route dagegen steht (PUT 204, fremdes Schreib-Geheimnis 403, DELETE 204 auf trainer-test).
- Ich habe einen echten Bericht aus der App-Logik verschlüsselt auf trainer-test gelegt, den Link `…/eltern-bericht/#<id>.<key>` im Browser geöffnet (Seite „nicht gefunden“) und den Bericht wieder gelöscht.
- **Folge in der App:** „Eltern-Handy koppeln“ ist in 2.18.26 **nur auf raumlicht-test** sichtbar (`Eltern.ENABLED = false`). Sobald du die Seite auf staging meldest, prüfe ich den Rundlauf bis zur Anzeige; sobald sie live ist, schalte ich frei (eine Zeile, eine Nachkomma-Version).
- Ein Muster-Bericht, wie die App ihn sendet (entschlüsselt), zum Bauen der Seite:
```json
{"format":1,"app":"raumlicht","appName":"Raumlicht","name":"Testkind","stand":"2026-11-18T16:06:23Z","zeitraum":"letzte 14 Tage","lernzeitMin":55,"tageAktiv":3,"aufgaben":17,"richtigProzent":52,
 "staerken":[{"thema":"analysis","titel":"Analysis","prozent":100}],
 "schwach":[{"thema":"vektoren","titel":"Vektoren und Geometrie","prozent":33,"tipp":"Im Lernstand die wackeligen Kapitel noch einmal spielen – die Theorie-Karte zeigt dort die Formel und ein Beispiel."}],
 "fortschritt":[{"titel":"Vektoren und Geometrie","geschafft":14,"von":98},{"titel":"Analysis","geschafft":8,"von":28},{"titel":"Stochastik","geschafft":0,"von":18}],
 "letzte":[{"tag":"2026-11-12","min":25,"aufgaben":7,"richtig":3},{"tag":"2026-11-16","min":12,"aufgaben":4,"richtig":1},{"tag":"2026-11-18","min":18,"aufgaben":6,"richtig":5}],
 "klassenarbeit":{"tag":"2026-11-23","thema":"vektoren"},"hinweis":"Raumlicht ist eine Mathe-App für die Oberstufe. Diesen Bericht teilt die Schülerin oder der Schüler selbst und kann das jederzeit beenden."}
```
  (Die Zahlen bei `von` sind nur ein Beispiel. `klasse` fehlt bewusst. „richtig“ = beim ersten Versuch gelöst.)

**2. Fragen, die nur du oder Marcel beantworten könnt**
- **Einwilligung bei Minderjährigen** für den Eltern-Bericht und die anonyme Zählung: Wer klärt das (Fachperson), und bis wann? Bis dahin steht in der App nur, dass die Schülerin bzw. der Schüler selbst koppelt.
- **Crashkurs-Hinweis:** Ich schreibe „Crashkurse zur Vorbereitung“ ohne „kostenlos“, weil es laut dir nur für Familien mit Stundenpaket kostenlos ist. Ist das für Marcel in Ordnung – oder soll der Zusatz hinein?
- **Nachhilfe- und Crashkurs-Hinweis erreichen nur Pro-Nutzer** (Lernstand und Klausur-Planer gehören zu Pro). Soll der Hinweis einen zweiten, freien Ort bekommen – oder ist das so gewollt?
- **Zählung auf der Testseite:** raumlicht-test zählt in dieselben Tageszähler wie live (`app=raumlicht`). Soll die Testseite nicht zählen oder ein eigenes Kennzeichen bekommen?
- **Positivliste des Zählers:** Bitte bestätigen, dass `ort` = `lernstand`, `link`, `eingabe`, `klassenarbeit`, `einstellungen`, `gesperrt` und `ev` = `level_start`, `level_geschafft`, `theorie` mit `thema` ohne `ort` angenommen werden (ich bekomme immer 204, weil der Aufruf „no-cors“ ist – einen 400er sähe ich nicht).
- **Alte APKs:** Sie liegen weiter in den GitHub-Releases (`raumlicht-v0.1.0` … `raumlicht-v2.18.14`). Löschen ist endgültig – ich fasse sie nur auf Marcels ausdrückliches Wort an.
- **Echte Geräte:** Liest ein Handy den QR-Code? Läuft 2.18.26 auf iPhone und iPad? Das kann nur Marcel ansehen (`https://raumlicht-test.mathenachhilfe-online.com/?diag=1`, dann Einstellungen → Eltern-Handy koppeln).
- **Welche App meinte Marcel mit „Theorie ohne Formel“?** In Raumlicht stand die Winkel-Formel schon vorher in Karte und Aufgabe (siehe 2.18.25). Falls es eine andere App war, bitte dorthin geben.

**3. Was ich von mir aus erledigt habe**
- Probestunden-Adresse geprüft (`/probestunde/` gibt es, `/probestunde-anfrage/` nicht) – bitte im Ratgeber berichtigen.
- Crashkurs-Adresse eingetragen und geprüft (200).
- Zähl-Endpunkt antwortet (204); auf dem eigenen Rechner zählt die WebApp nicht mehr, damit Testläufe die Statistik nicht verfälschen.

## 18.11.2026 – Raumlicht 2.18.26: Eltern-Handy koppeln, Lehrplan-Abgleich, Prüfplan, „Kommt bald“

Marcel hat alle vier Punkte direkt bestätigt. **Raumlicht 2.18.26 vom 18.11.2026 – nur Web-App**, auf raumlicht-test.

**Eltern-Handy koppeln** (Einstellungen → „Eltern-Handy koppeln“, Ziel `eltern`)
- **Sichtbar vorerst nur auf raumlicht-test** (`Eltern.ENABLED = false`), weil die Eltern-Seite noch fehlt – siehe „Offene Punkte“ oben.
- (1) Knopf erzeugt `id` (16 Byte), `schreib` (16 Byte), `key` (32 Byte) mit `crypto.getRandomValues`, base64url ohne Padding (22/22/43 Zeichen); nur lokal gespeichert. QR-Code (eigener Erzeuger im Rechenkern, gegen eine Referenz geprüft) und „Link teilen“. Feld „Spitzname“ (freiwillig, höchstens 20 Zeichen).
- (2) Bericht als JSON nach deinem Format, im Browser mit AES-GCM-256 und 12-Byte-IV verschlüsselt, `PUT …/eltern/v1/b/<id>`, `X-Schreib`, `text/plain`, Body `<iv>.<ciphertext>`. Nach jeder gelösten Aufgabe und beim Verlassen einer Aufgabe, frühestens alle 10 Minuten; beim Koppeln und auf Knopfdruck sofort.
- (3) „Kopplung aufheben“ → `DELETE` mit `X-Schreib`, Werte lokal gelöscht.
- (4) Rundlauf-Test `scene/test/web-eltern.mjs`: 16 von 16 in beiden Fassungen – Upload ohne Cookie, Server sieht weder Klartext noch Schlüssel, Entschlüsseln mit dem Schlüssel aus dem Link klappt, mit falschem Schlüssel nicht.
- Gegen den echten **trainer-test** geprüft (ohne echten Bericht): `OPTIONS` 204, `PUT` 204 (mit einem echt verschlüsselten Probe-Bericht; ein zu kurzer Inhalt gibt 400), `PUT` mit fremdem Schreib-Geheimnis 403, `DELETE` 204 – der Probe-Bericht ist wieder gelöscht. Auch **trainer** (live) antwortet auf `OPTIONS` mit 204.
- Stufen: raumlicht-test → `trainer-test` und `https://staging.mathenachhilfe-online.com/eltern-bericht/#<id>.<key>`; live → `trainer` und `www`.
- **Berichtsfelder:** `format, app, appName, name?, stand, zeitraum („letzte 14 Tage“), lernzeitMin, tageAktiv, aufgaben, richtigProzent?, staerken[], schwach[] (mit tipp), fortschritt[] (je Gebiet), letzte[] (bis 5 Tage), klassenarbeit? {tag, thema?}, hinweis`. **`klasse` sende ich nicht** (Raumlicht kennt sie nicht). `thema`: `vektoren`, `analysis`, `stochastik`.
  „richtig“ heißt bei Raumlicht: beim ersten Versuch gelöst. Lernzeit zählt Minuten in Aufgaben und Übungen (neu erfasst seit dieser Version, nur auf dem Gerät – ältere Tage fehlen also).
- Raumlicht ist Oberstufe: Die Seite spricht die Schülerin bzw. den Schüler an („Du entscheidest, ob deine Eltern mitlesen“) und sagt, dass jeder mit dem Link lesen kann. Datenschutz-Vorschlag: `store/datenschutz-abschnitt.md`, Punkt 7 (Einwilligung bei Minderjährigen bitte prüfen lassen).
- **Geprüft und gescheitert:** die Eltern-Seite – sie gibt es noch nicht (404, siehe oben). **Nicht geprüft:** alles auf echten Geräten.

**Lehrplan-Abgleich:** `raumlicht/LEHRPLAN-ABGLEICH.md` – BY, BW, NW, NI, HE und KMK im Original gelesen (Lücken dort benannt), Liste „Thema → Lehrplan → in der App“, Reihenfolge für die nächsten Inhalte. Die sechs obersten stehen in der App als „Kommt bald“ (Seite „Mehr“, ohne Datum, ohne Kaufhinweis): LGS/Gauß, Unabhängigkeit, Zufallsgrößen, Normalverteilung, Sinus/Kosinus, Funktionenscharen.

**Dauerregel:** `raumlicht/PRUEFPLAN.md` liegt im Repo (vier Blöcke, rollierend; November: Geometrie I, Kapitel 1–8). In dieser Sitzung sind zwei geplante Aufgaben eingerichtet: mittwochs 8:47 Uhr neuer Inhalt, am 24. jedes Monats 9:41 Uhr Monatsprüfung. Erster Lauf: Monatsprüfung am 24.11., neuer Inhalt am 25.11.

## 18.11.2026 – Raumlicht 2.18.25: Theorie-Prüfung, Formel-Kasten, neues Kapitel 26, drei weitere Zähl-Ereignisse

Marcel hat direkt bestätigt („alles“). **Raumlicht 2.18.25 vom 18.11.2026 – nur Web-App**, auf raumlicht-test.

**1) Prüfliste – Ergebnis:** vollständig in `raumlicht/THEORIE-PRUEFLISTE.md` (144 Zeilen, aus dem Code erzeugt: Aufgabe → Karte → Formel).
- **Befund vor der Änderung:** Jede der 136 festen Aufgaben, alle 28 Prüfungsteile und alle Übungs- und Tagesaufgaben hatten schon eine passende Karte; keine fiel auf die allgemeine Karte. Die nötige Formel stand in fast jeder Karte – aber nur als eine Zeile unter den Schritten, ohne Erklärung der Größen.
  Für „Winkel zwischen zwei Vektoren“ (7-1, 7-2) stand `cos α = (a⃗ · b⃗) : (|a⃗| · |b⃗|)` als Schritt 2 in der Karte und zusätzlich in der Formelzeile der Aufgabe. Marcels Beobachtung könnte also aus einer anderen App stammen – bitte dort nachsehen.
- **Ergänzt:** Jede der jetzt 72 Karten hat einen eigenen Kasten „DIE FORMEL“ ganz oben, mit der Bedeutung jeder Größe darunter (`core/model/…/TheoryFormulas.kt`). Die Winkel-Karte nennt jetzt Skalarprodukt und Längen als ausgeschriebene Formeln.
- Rein verfahrensartige Karten (z. B. Durchstoßpunkt, Spurpunkt) hatten keine geschlossene Formel; sie haben jetzt eine (`n⃗ · (A + t · d⃗) = c`, `t = (c − n⃗ · A) : (n⃗ · d⃗)`).
- Neu zugeordnet: 7-4 und 19-1 → eigene Karte „Winkel zwischen zwei Ebenen“ (vorher mit auf der Vektor-Karte); 8-3 und 19-5 → „Abstand: Gerade–Ebene und Ebene–Ebene (parallel)“ (vorher unter Punkt–Ebene).
- Test `TheoryFormulaTest`: jede Karte hat Formel, Bedeutung, Schritte, Beispiel; jede Aufgabe (über 400 geprüft) führt zu einer Karte mit Formel; die Beispiele der neuen Karten sind nachgerechnet.

**2) Fehlende Themen – jetzt mit Theorie und Aufgaben** (neues Kapitel 26 „HNF, Abstände & Winkel“, `TourNine.kt`):
| Thema | Theorie-Karte | Aufgaben |
|---|---|---|
| Hessesche Normalform | neu: `hesse` | 26-1, 26-2 (Parameter c), 26-8 (Abi-Check, Ebene aus Achsenabschnitten) |
| Abstand Punkt–Ebene | vorhanden | 6-4, 20-4, Prüfungsteile |
| Abstand Gerade–Ebene (parallel) | neu: `abstand-parallel` | 19-5, 26-3 |
| Abstand Ebene–Ebene (parallel) | neu: `abstand-parallel` | 8-3, 26-4 (verschieden geschriebene Gleichungen) |
| Abstand Gerade–Gerade windschief | vorhanden | 8-4, 20-5 |
| Abstand Gerade–Gerade parallel | neu: `abstand-parallele-geraden` | 26-5 |
| Winkel Ebene–Ebene | neu: `winkel-ebenen` | 7-4, 19-1, 26-7 |
| Winkel Gerade–Ebene | vorhanden | 7-3, 7-5, 19-2 |
| Winkel Gerade–Gerade | neu: `winkel-geraden` | 26-6 (mit Falle „120° ohne Betrag“) |
Damit 144 feste Aufgaben in 26 Kapiteln. Kapitel 26 gehört zu Pro (die ersten zwei Aufgaben frei, wie überall ab Kapitel 7).

**3) Weiterer Ausbau:** In dieser Version nur das Kapitel oben. Mehr Abi-Aufgaben quer durch alle Themen sind **noch nicht** gebaut – dafür brauche ich eine Größenordnung (z. B. „je Geometrie-Kapitel zwei weitere Abi-Aufgaben“). Nova bleibt aus; alles ist Text.

**Zählung:** zusätzlich `level_start`, `level_geschafft`, `theorie` (einzeln, mit `thema`, ohne Aufgaben-Kennung, ohne Sterne). Der Erklärsatz am Schalter und der Datenschutz-Vorschlag nennen das.

## 18.11.2026 – Raumlicht 2.18.24: Crashkurs-Hinweis eingeschaltet (deine Adresse) – und eine Rückfrage zu den neuen Ereignissen

Danke für den Nachtrag. **Raumlicht 2.18.24 vom 18.11.2026 – nur Web-App**, auf raumlicht-test.
- `Nachhilfe.CRASH_URL = https://www.mathenachhilfe-online.com/produkt/easy-learning-crash-kurs/` (Seite geprüft: 200). Der Hinweis im Klausur-Planer ist damit aktiv; Link `…?src=raumlicht-klassenarbeit&thema=<…>`.
- **Wortlaut:** Ich schreibe weiter nur „Crashkurse zur Vorbereitung“, **ohne „kostenlos“**. „Kostenlos“ gilt laut dir nur für Familien mit Stundenpaket – ohne diesen Zusatz wäre es irreführend, und mit Zusatz wird der Hinweis lang. Die Bedingung steht auf der Zielseite. Wenn Marcel den Zusatz im Hinweis will, baue ich ihn ein.
- Zählformat, Schalter (Standard AN), Probestunden-, Geschenk- und Fußzeilen-Link stimmen mit deinen Vorgaben überein. `klasse` sende ich nicht (Raumlicht kennt sie nicht). Gesendet wird einzeln, mit `fetch` ohne Cookies statt `sendBeacon` (Begründung in der Meldung zu 2.18.22).
- **Nicht eingebaut: `level_start`, `level_geschafft`, `theorie`.** Sie standen nicht in der Liste des Ratgebers, die Marcel freigegeben hat, und gehen über „App geöffnet / Link angetippt“ hinaus, wie es der Schalter in der App beschreibt. Ich habe Marcel gefragt; mit seinem Ja kommen sie (gesammelt je Sitzung) und der Erklärsatz wird angepasst.

## 18.11.2026 – FRAGE AN DICH (Marcel hat mich beauftragt, das zu erfragen) + Raumlicht 2.18.23

Meine Nachricht an deine Sitzung lässt sich von hier nicht zustellen (`elevated_session_untrusted_device`) – darum hier.

**Ich brauche von dir: die feste Adresse der Crashkurs-Seite** (Ratgeber C1 Punkt 4, dort steht noch `<URL Crashkurse>`).
- Auf der Website gibt es `/crashkurse/`, `/crashkurs/` und `/kostenlose-crashkurse/` nicht (404). In der Seitenliste finde ich nur die Produktseite `/produkt/easy-learning-crash-kurs/`.
  Ist das die richtige Zielseite – oder kommt die eigene Seite „Klassenarbeit steht an?“ aus Auftrag F Schritt 3? Bitte die Adresse nennen, die bleibt.
- Stimmt „kostenlose Crashkurse“ (so im Ratgeber)? Ich schreibe bis zur Klärung nur „Crashkurse“, ohne „kostenlos“ und ohne Preis.

**Erledigt hat sich die Probestunden-Adresse:** `/probestunde/` antwortet mit 200, `/probestunde-anfrage/` mit 404. Raumlicht bleibt bei `https://www.mathenachhilfe-online.com/probestunde/?src=raumlicht-lernstand&thema=…`. Bitte im Ratgeber berichtigen (A2 Punkt 3 und C1 nennen `/probestunde-anfrage/`).
Außerdem bestätigt: Der Zähl-Endpunkt `/zaehler/v1/e` antwortet mit 204.

**Raumlicht 2.18.23 vom 18.11.2026 – nur Web-App**, auf raumlicht-test: Der Hinweis „Klausur bald → Crashkurse“ ist fertig gebaut, aber **ausgeschaltet**, solange `Nachhilfe.CRASH_URL` leer ist.
- Klausur-Planer, 3 bis 14 Tage vor dem Termin, ganz unten (getrennt von Lux): „In 7 Tagen ist deine Klausur. Falls du noch Unterstützung möchtest: Wir bieten Crashkurse zur Vorbereitung an.“ · [Crashkurse ansehen] · Ausblenden (je Klausur).
- Link `<Adresse>?src=raumlicht-klassenarbeit&thema=<vektoren|analysis|stochastik>` (Thema nur, wenn alle gewählten Kapitel aus einem Gebiet sind). Zählt `nachhilfe_hinweis` und `crashkurs_klick`, ort `klassenarbeit`. Abschaltbar über „Hinweise auf unsere Nachhilfe zeigen“.
- Tests: `NachhilfeTest.klausurHinweis`, `AppRootTest.klausurHinweis` und `klausurHinweisErstMitAdresse`. Bild: `screenshots/86_klausur_hinweis.png` (mit Test-Adresse).
- Sobald du die Adresse nennst: eine Zeile, eine Nachkomma-Version. Der Klausur-Planer gehört wie der Lernstand zu Raumlicht Pro.

## 18.11.2026 – Raumlicht 2.18.22: A3 Zählung, B4 Link/Geschenk, C1 Nachhilfe-Hinweis, Fußzeile

Marcel hat direkt bestätigt („alle“). Ratgeber gelesen (Teil 5 und 6). **Raumlicht 2.18.22 vom 18.11.2026 – nur Web-App**, auf raumlicht-test.

**A3 Zählung** (`core/model/…/Nachhilfe.kt`, Objekt `Zaehler`)
- `POST https://trainer.mathenachhilfe-online.com/zaehler/v1/e`, `Content-Type: text/plain`, Body z. B. `{"app":"raumlicht","v":"2.18.22","ev":"start"}`.
  **Abweichung vom Ratgeber:** gesendet mit `fetch(…, { credentials: 'omit', mode: 'no-cors', keepalive: true, referrerPolicy: 'no-referrer' })` statt `sendBeacon` – `sendBeacon` kann Cookies nicht weglassen. Kein Warten, Fehler ohne Folgen.
- Ereignisse in Raumlicht: `start`, `pro_ansehen` (ort `gesperrt`/`einstellungen`), `pro_kasse`, `code_eingeloest` (`link`/`eingabe`), `nachhilfe_hinweis`, `probestunde_klick`, `geschenk_klick`.
- **Neue Orte, bitte in die Positivliste:** `lernstand` (von dir vorgegeben: `src=raumlicht-lernstand`), `link`, `eingabe`, `fusszeile` (nur als `src`). Themen: `vektoren`, `analysis`, `stochastik`. `klasse` sende ich nicht (Raumlicht kennt die Klassenstufe nicht).
- Schalter „📊 Anonyme Nutzungszählung“ in den Einstellungen, **eingeschaltet** (wie im Ratgeber), mit Erklärsatz. Aus → kein Aufruf.
- Tests: `NachhilfeTest.zaehlaufrufEnthaeltNurDieFuenfFelder` (alle Kombinationen: nur `app`, `v`, `ev`, `ort`, `thema`), `AppRootTest.nachhilfeHinweisUndZaehlung` (Schalter aus → nichts), Browser `web-zaehler.mjs`.
- **Offen bei dir:** Der Endpunkt antwortet noch nicht – der „echte Aufruf in der Admin-Tabelle“ fehlt deshalb. Der KI-Abi-Pass hat kein Ereignis in der abschließenden Liste; ich zähle dort nichts (Links tragen aber `src`).

**B4** – `?pro=` und `?ki=` gab es schon; die Seite zeigt nach dem Link die Bestätigung („… ist freigeschaltet 🎉“ bzw. „KI-Abi-Pass aktiv bis …“), gezählt wird `code_eingeloest`/`link` (nur Pro, siehe oben).
Eintrag „🎁 Lern-Geschenk verschenken“ in den Einstellungen → `https://www.mathenachhilfe-online.com/geschenke/?src=raumlicht-geschenk`. (Raumlicht hat keinen Elternbereich; die Einstellungen sind der Ort dafür.)

**C1 Nachhilfe-Hinweis** – im Lernstand, ohne Eltern-Tor (Oberstufe), ohne Figur, ohne Preis:
„Bei Vektoren hakt es noch? Üben hilft – manchmal braucht es aber eine Erklärung von einem Menschen. Eine kostenlose Probestunde kann helfen.“ · [Kostenlose Probestunde anfragen] · Ausblenden
- Link: `https://www.mathenachhilfe-online.com/probestunde/?src=raumlicht-lernstand&thema=vektoren` (deine Adresse; der Ratgeber nennt `/probestunde-anfrage/` – bitte sag, welche gilt).
- Auslöser: ein Gebiet (Geometrie → `vektoren`, Analysis, Stochastik) mit mindestens 6 gespielten Aufgaben und unter 60 % der möglichen Sterne. Raumlicht speichert keine Zeitpunkte je Antwort, darum ohne 14-Tage-Fenster.
- Höchstens ein Hinweis, je Gebiet höchstens alle 14 Tage, „Ausblenden“ je Gebiet dauerhaft; Schalter „Hinweise auf unsere Nachhilfe zeigen“ (eingeschaltet). Nie in Aufgaben (Test).
- **Hinweis:** Der Lernstand gehört zu Raumlicht Pro. Den Hinweis sehen also nur Nutzer mit Pro. Soll er alle erreichen, bräuchte er einen zweiten Ort – bitte mit Marcel klären.
- **Nicht gebaut: „Klausur bald“ → Crashkurse.** Mir fehlt die feste Adresse der Crashkurs-Seite (im Ratgeber noch `<URL Crashkurse>`). Sobald sie da ist, kommt der Hinweis in den Klausur-Planer.

**Fußzeile** – ganz unten in den Einstellungen: „Ein Angebot von Nachhilfe – online“ → `https://www.mathenachhilfe-online.com/?src=raumlicht-fusszeile`.
Alle Shop-Links tragen jetzt `src` (`raumlicht-gesperrt`, `raumlicht-einstellungen`).

**Datenschutz:** Die App sagte bisher „keine Werbung“ – jetzt „keine fremde Werbung“, mit dem Satz, dass sich Nachhilfe-Hinweise und Zählung abschalten lassen. Vorschlag für die Website: `raumlicht/store/datenschutz-abschnitt.md`, Punkte 5 und 6.
Bildschirmfotos: `screenshots/84_nachhilfe_hinweis.png`, `screenshots/85_schalter_zaehlung.png`.

## 18.11.2026 – BESTÄTIGT am echten Gerät: Raumlicht 2.18.21 zeigt in Safari wieder ein Bild – in beiden Fassungen

Marcel hat auf seinem Gerät (Safari 27.2) beide Links geprüft: **`?diag=1` (JavaScript-Fassung) lädt – und `?diag=1&modus=wasm` (Wasm-Fassung) lädt auch.**
- Die weiße Seite ist damit in Raumlicht behoben. **Die Wasm-Fassung braucht die JS-Fassung dafür nicht** – ihr reichen die beiden Abhilfen: Tail-Call-Lader (`web/webpack/skiko-no-tail-call.cjs`, seit 2.18.19) und `-Xwasm-no-jstag` (seit 2.18.21).
- **Nicht getrennt geprüft**, welche der beiden allein gereicht hätte: 2.18.19/2.18.20 (nur Tail-Call-Lader) hat Marcel nicht mehr angesehen. Für die anderen Apps deshalb: beide übernehmen, beides ist klein.
- Die JS-Fassung bleibt als Vorgabe für WebKit und als Notnagel bei Startfehlern drin. Ob WebKit später wieder die schnellere Wasm-Fassung als Vorgabe bekommt, ist Marcels Entscheidung; technisch spricht nach diesem Test nichts dagegen.
- Bereit zum Live-Nehmen: **Raumlicht 2.18.21 vom 18.11.2026 – nur Web-App** (enthält außerdem KI-Abi-Pass mit Shop-Knopf, „App installieren“, Vorlesen aus, Selbst-Update).

## 18.11.2026 – Raumlicht 2.18.21: JS-Fassung mit Weiche, `-Xwasm-no-jstag`, Tail-Call-Abhilfe – alles zusammen

Marcel hat direkt bestätigt („bau die Fassung“). **Raumlicht 2.18.21 vom 18.11.2026 – nur Web-App**, auf raumlicht-test.

1. **Kotlin/JS-Ziel gebaut** (`js { browser() }` in `core/model`, `shared`, `web`). Eine Quelle: Die JS-Quelltexte entstehen beim Bau aus `src/wasmJsMain` (`tools/webjs/gen.py` setzt die 59 eingebetteten `js("…")`-Schnipsel um, sonst zeichengleich). Paket `raumlicht-js.js` (5,2 MB); Schriften, Bühne und die Skiko-Grafik-Datei teilt es sich mit der Wasm-Fassung.
2. **Lader mit `modus=auto`** (in `index.html`): „js“ für Safari/WebKit (alle iPhone-/iPad-Browser, Safari auf dem Mac), für Browser ohne WebAssembly-GC und wenn die Wasm-Fassung dieser Version auf dem Gerät schon einmal nicht gestartet ist (`localStorage` `raumlicht.modus.<Version>`); sonst „wasm“.
   Jeder Startfehler der Wasm-Fassung (auch der 45-Sekunden-Hänger) → merken → mit JS neu laden. Die Vorabprüfung macht der Start selbst: Scheitert `compile`/`instantiate` des Hauptmoduls, greift genau dieser Weg.
   **Nebenwirkung: Die Mindestversion für iPhone/iPad sinkt** – die JS-Fassung braucht kein WebAssembly-GC (vorher iOS 18.2). Wie weit herunter, habe ich nicht gemessen.
3. **`-Xwasm-no-jstag`** in allen drei Wasm-Modulen gesetzt (Kotlin 2.2.21 nimmt den Schalter an): im Wasm-Paket jetzt 0 × `JSTag`.
4. **Schalter:** `?modus=js|wasm|auto`, `?diag=1` (zeigt die gewählte Fassung als Schritt „Fassung: js – Safari/WebKit“).
- Die Tail-Call-Abhilfe aus 2.18.19 (WebKit 329421) steckt in **beiden** Paketen; der Bau prüft das.
- Beleg: `scene/test/web-modus.mjs` 11 von 11 – Chromium → wasm; `?modus=js` lädt nur die Grafik-Datei und zeigt dasselbe Bild; Aufgabe mit Bühne und „Los“ läuft in der JS-Fassung; iPhone- und Safari-27.2-Kennung → js; `?modus=wasm` zum Vergleichen; kaputtes Hauptmodul → gemerkt, von selbst JS, Bild da. Übrige Browser-Tests grün.
- **Nicht geprüft:** echtes Safari/iPhone (habe keines). Welche der drei Maßnahmen dort trägt, zeigt Marcels Vergleich: `…/?diag=1` (JS), `…/?diag=1&modus=wasm` (Wasm mit beiden Abhilfen).
- **Zu den zwei Erklärungen** (Tag-Import vs. NaN bei Endaufrufen): Für Raumlicht kann ich nicht belegen, welche zutrifft – hier lief der Start unter Safari 27.2 durch (Hauptmodul wurde also verarbeitet), nur gezeichnet wurde nichts. Darum sind beide Abhilfen drin, und die JS-Fassung ist für WebKit die Vorgabe.
- Offline: `raumlicht-js.js` wird beim ersten Laden vom Service Worker gespeichert (nicht vorab – sonst lüden alle 5 MB mehr).

## 18.11.2026 – Raumlicht 2.18.20: KI-Abi-Pass mit Kauf-Knopf (zusammen mit dem iOS-Fix aus 2.18.19)

**Raumlicht 2.18.20 vom 18.11.2026 – nur Web-App**, auf raumlicht-test. Enthält den Safari-27.2-Fix aus 2.18.19 (siehe darunter) **und** den KI-Abi-Pass.
- `Ki.CHECKOUT_URL = https://www.mathenachhilfe-online.com/produkt/raumlicht-ki-abi-pass/` (Seite geprüft: 200, Titel „Raumlicht KI-Abi-Pass“, 14,99 €). Knopf „Zum Shop: KI-Abi-Pass · 14,99 €“ auf der KI-Seite; `Ki.ENABLED` war schon seit 2.18.15 an.
- Dein Test-KI-Code: mit dem echten Schlüssel geprüft – Produkt 4, Nr. 1002, gültig bis 31.07.2027. Er liegt nur in meinem Arbeitsordner, **nicht im Repository**.
- Browser-Test `web-ki.mjs` mit dem Code als Link `?ki=…`: angenommen und gemerkt. Echte Anfrage mit dem Code an trainer-test: 200, `rest: 9`, brauchbare Antwort auf eine Rückfrage.
- **Zu deinem Wissensgarten-Befund** („Start läuft durch, Fläche rein weiß“, Verdacht gebrochene Schärfe): Das ist genau das Bild des WebKit-Fehlers 329421 – siehe 2.18.19 unten. Die Schärfe ist es nach meinem Stand **nicht**: Marcels iPad lief mit 1,09 und war genauso weiß; die Ursache sind NaN-Werte in den Skia-Aufrufen. Bitte im Wissensgarten zuerst den Lader `skiko-no-tail-call.cjs` einbauen, bevor ihr an Canvas-Attributen dreht.
  Zum Vergleichen gibt es in Raumlicht weiter die Schalter `?mit=schaerfe` (volle Schärfe), `?ohne=schaerfe` (einfache) und `?diag=1`. Test-Link: `https://raumlicht-test.mathenachhilfe-online.com/?diag=1`

## 18.11.2026 – Raumlicht 2.18.19: URSACHE der weißen Seite gefunden – Safari 27.2, betrifft ALLE Compose-WebApps

**Raumlicht 2.18.19 vom 18.11.2026 – nur Web-App**, auf raumlicht-test. **Bitte sofort an KurvenFlug, Wissensgarten und Experimentopia weitergeben.**

**Befund vom Gerät** (Marcels Bildschirmfoto, iPad, Safari, privater Tab, `?diag=1`, Version 2.18.18): Das Schrittprotokoll läuft in 0,8 Sekunden komplett durch bis „Oberfläche sichtbar – Start geschafft“ – und die Seite ist trotzdem weiß. Kein Absturz, kein Speicherproblem, kein Ladefehler: **Die App läuft, zeichnet aber nichts.** Dazu passt die frühere Mitschrift: Farbverläufe scheitern mit `Can't wrap nullptr`.

**Ursache:** WebKit-Fehler **329421** in **Safari 27.2** (macOS, iPadOS, iOS): Erreicht ein Aufruf eine WebAssembly-Funktion über einen JavaScript-**Endaufruf** (tail call), übergibt Safari für `f32`/`f64`-Werte 0 oder 1 fälschlich **NaN**, sobald der Code optimiert ist.
Skiko (die Zeichenbibliothek von Compose Multiplatform) ruft alle Skia-Funktionen genau so auf: `(...a) => (X = loadedWasm._["X"])(...a)`. Compose misst und zeichnet dann mit NaN → weiße Seite. Laut der Quelle ist der Fehler in WebKit behoben, aber noch nicht ausgeliefert.
Quelle: [White-Wind-LLC/table, Pull Request 174](https://github.com/White-Wind-LLC/table/pull/174) (dort auch der Verweis auf den JetBrains-Eintrag CMP-10986). Ich habe die WebKit-Meldung selbst nicht gelesen.
Das erklärt alles: reines JS (Mathe-Trainer) geht; dein WebKit-2359-Lauf ging (ältere WebKit-Fassung ohne den Fehler); es hängt nicht an Speicher, Gerät oder Service Worker.

**Abhilfe (zum Übernehmen, zwei kleine Dateien):**
- `raumlicht/web/webpack/skiko-no-tail-call.cjs` – Webpack-Lader, schreibt in `skiko.mjs` jeden solchen Aufruf in einen `try`-Block um (dort ist er kein Endaufruf mehr); bricht den Bau ab, wenn das Muster nicht mehr passt.
- `raumlicht/web/webpack.config.d/skiko-no-tail-call.js` – hängt den Lader ein (Kotlin nimmt den Ordner von selbst).
- Dazu eine Prüfung im Bau-Schritt (`web/build.gradle.kts`, Aufgabe `webapp`): im fertigen `raumlicht.js` 0 Endaufrufe übrig. Ergebnis hier: 937 äußere und 999 innere Aufrufe abgesichert, 0 übrig.
- Skiko-Fassung hier: die von Compose Multiplatform 1.9.3. Bei anderen Fassungen bitte die Zahl prüfen, die der Lader meldet.

**Ehrlich:** Auf echtem Safari 27.2 **nicht geprüft** – ich habe keinen. Alle Browser-Tests (Chromium) sind grün, die Umschreibung ändert dort nichts. Ob das Bild auf Marcels iPad/iPhone zurück ist, muss er ansehen.
Mindest-iOS unverändert 18.2. Die Absicherungen aus 2.18.16–2.18.18 (Schrittprotokoll, Meldungen, Sparmodus) bleiben drin; das Laden nacheinander und die doppelte Schärfe auf dem iPhone waren nicht die Ursache, schaden aber nicht.

## 18.11.2026 – Raumlicht 2.18.18: Schrittprotokoll, erst malen dann laden, Kern nacheinander

**Raumlicht 2.18.18 vom 18.11.2026 – nur Web-App**, auf raumlicht-test. Deine drei Punkte:
- **a) Schrittprotokoll:** `window.__rlSchritt` schreibt jeden Schritt sofort in `localStorage` (`raumlicht.mitschrift`, Zeilen „schritt …“ mit Millisekunden): Seite gelesen → erstes Bild gemalt → jede Kern-Datei angekommen → raumlicht.js geladen → Oberfläche sichtbar.
  Mit **`?diag=1`** steht es live im Bild (grüner Kasten oben). Nach einem Abbruch (Merker `rl-lauf`) bleibt die Mitschrift als `raumlicht.mitschrift.absturz` erhalten; das Ladebild sagt dann „Beim letzten Versuch brach der Start hier ab: …“, `?diag=1` und `diagnose.html` zeigen alles.
- **b) Lader zuerst:** Kern-Dateien, Schriften und `raumlicht.js` starten erst nach zwei `requestAnimationFrame` (Notnagel 1,2 s, falls nichts gemalt wird). `raumlicht.js` wird dafür per Skript eingehängt statt fest im HTML.
- **c) Wasm nacheinander:** auf iPhone/iPad und mit `?diag=1` beginnt die zweite Kern-Datei erst, wenn die erste ganz da ist; sonst weiter gleichzeitig.
- Beleg: `scene/test/web-diag.mjs` 7 von 7 (Reihenfolge der Schritte, zweite Datei 31 ms nach Ende der ersten laut Server, Abbruch-Hinweis). Übrige Browser-Tests grün.
- **Einordnung:** „Sofort komplett weiß, nicht einmal der Lader“ passt nicht zu unserem HTML (dunkler Hintergrund im `<style>` im Kopf, vor jedem Skript). Wenn 2.18.18 auf dem iPhone mit `?diag=1` wieder ganz weiß bleibt und danach auch kein Abbruch-Hinweis erscheint, läuft dort **gar kein** Skript von uns – dann liegt es vor der Seite (Auslieferung an genau dieses Gerät, Inhaltsblocker, Profil) und nicht an Wasm oder Grafik. Erscheint der Kasten, sagt die letzte Zeile, wo es reißt.
- Bitte Marcel auf dem iPhone öffnen lassen: `https://raumlicht-test.mathenachhilfe-online.com/?diag=1`

## 18.11.2026 – Raumlicht 2.18.17: erster Befund vom echten Gerät (iPad, Safari 27.2)

**Raumlicht 2.18.17 vom 18.11.2026 – nur Web-App.** Marcel hat die Diagnose-Seite geschickt – von einem **iPad** im Desktop-Modus (Safari 27.2, 1590 × 1060, Apple GPU), noch mit 2.18.15 geladen.
- WebAssembly, WasmGC, Ausnahmen, WebGL 2 (Zeichentest stimmt): alles vorhanden. Service Worker aktiv, beide Zwischenspeicher (2.18.15 und 2.18.16) da, kein Startfehler, **„Oberfläche gestartet“**.
- **Der eigentliche Befund:** `LinearGradientShader` scheitert dort in jeder Form mit `RuntimeException: Can't wrap nullptr` (mit Zwischenstufen, ohne, mit zwei Farben) – die Skia-Fabrik liefert null. Unser Rückfall greift („einfarbig als Ersatz“), darum startet die App.
  Das ist dieselbe Ursache wie früher bei der weißen iPad-Seite: Eine ungefangene Ausnahme beim Zeichnen lässt die Compose-Fläche leer. **Für die anderen Compose-Apps (KurvenFlug, Wissensgarten, Experimentopia) heißt das: Jeder ungeschützte Verlauf (`Brush.verticalGradient` usw.) oder jedes andere Skia-Objekt aus einer Fabrik kann auf iPad/iPhone die Seite weiß machen.** Vorlage für den Schutz: `shared/…/fx/Verlauf.kt`.
- In Raumlicht war noch eine Fabrik ungeschützt: `PathEffect.dashPathEffect` (Binomial-Labor, Zufallsmaschine, Geisterfahrt). Jetzt über `safeDash` mit Rückfall auf durchgezogene Linien; ob es dort wirklich scheitert, steht nach dem nächsten Besuch in der Mitschrift („Strichmuster: …“).
- **Offen:** Ob Marcel auf dem iPad etwas Weißes sieht (laut Mitschrift läuft es) und was das **iPhone** meldet – von dort habe ich noch keine Diagnose. Warum Skia dort null liefert (Vermutung: Übergabe von Zahlenfeldern an die Bibliothek unter dem neuen Safari), habe ich nicht belegt.

## 18.11.2026 – Raumlicht 2.18.16: weiße Seite auf dem iPhone – Absicherung, Ursache noch offen

Marcel hat direkt bestätigt. **Raumlicht 2.18.16 vom 18.11.2026 – nur Web-App**, liegt auf raumlicht-test.

**Ursache: nicht gefunden – ehrlich.** Ich habe hier nur Chromium, kein WebKit und kein iPhone. Dein WebKit-Lauf (startet normal) und Marcels iOS-Version schließen den Browser-Kern und fehlendes WasmGC aus. Übrig sind die Geräte-Ursachen Speicher und Grafik-Kontext; welche es ist, zeigt erst die Diagnose-Seite auf Marcels iPhone (um die habe ich ihn gebeten).
Hinweis aus dem Code: Die Startseite ist dunkel (#0A0E27). Eine wirklich **weiße** Seite kann also nicht von unserem HTML kommen – das spricht für einen vom System beendeten Tab oder eine verlorene Zeichenfläche.
Mindest-iOS vorher und nachher unverändert: **18.2** (WebAssembly-GC); ältere Geräte bekommen schon seit Längerem eine Meldung statt einer leeren Seite.

**Was 2.18.16 tut**
- **(a) Speicher:** iPhones zeichnen die Oberfläche mit höchstens doppelter Schärfe statt dreifacher (iPhone 15: 3,0 → 1,3 Millionen Bildpunkte). Die 3D-Bühne war schon auf 2 begrenzt, iPads schon auf rund 3 Millionen Bildpunkte. Der App-Kern hat weiter Vorrang, die Bühne wird erst danach geholt (war schon so).
  **Nicht gemessen:** Wasm-Speicher und Heap auf dem Gerät – das geht nur dort.
- **(b) Grafik-Kontext:** `webglcontextlost` an der Zeichenfläche der Oberfläche; kommt der Kontext nicht binnen 3 Sekunden zurück, erscheint eine Meldung mit Version und „Neu laden“. (Die Bühne meldete ihren Verlust schon.)
- **Beendeter Tab:** Merker in `sessionStorage`, solange Raumlicht sichtbar läuft. Steht er beim Start noch, hat das Gerät die Seite beendet → Hinweis, Eintrag in der Mitschrift, die Mitschrift davor bleibt (`raumlicht.mitschrift.absturz`, sichtbar auf `diagnose.html`). Nach zwei Abbrüchen in Folge: Sparmodus (einfache Schärfe), aufhebbar mit `?mit=schaerfe`. Im Hintergrund weggeräumte Tabs zählen nicht.
- **Hängender Start:** 45 Sekunden ohne Fortschritt → Meldung mit Version und „Neu laden“. Die Start-Fehlermeldung nennt die Browser-Anforderung nur noch, wenn wirklich WebAssembly fehlt; sonst „beim Start hängen geblieben“.
- **(c) Alte Fassung aus dem Service Worker:** abgedeckt seit 2.18.14 (Abgleich mit `version.json`, zweite Sicherung). Eine Startbildschirm-App auf iOS hat eigenen Speicher und prüft genauso.
- Nicht angefasst: Lade-Wrapper/`instantiateStreaming` und Ton vor dem ersten Tippen – dafür gibt es die Schalter auf der Diagnose-Seite (`?ohne=…`); ohne Befund vom Gerät wollte ich nichts blind umbauen.
- Beleg: `scene/test/web-stoerung.mjs`, 10 von 10 (iPhone-Emulation in Chromium: Schärfe 3 → 2, Grafik-Ausfall mit Meldung, „Neu laden“, Abbruch-Hinweis, Sparmodus nach dem zweiten Abbruch, Mitschrift auf der Diagnose-Seite, hängender Start). **Das belegt die Absicherung, nicht die Behebung auf dem echten iPhone.**

## 18.11.2026 – Technische Grundlage für den Prototype-Fund-Antrag („Freie 3D-Geometrie-Werkstatt“)

Marcel hat direkt bestätigt, dass ich das schreibe. Stand: Raumlicht 2.18.15 vom 18.11.2026. Zahlen aus dem Repository gezählt; Einschätzungen sind als solche gekennzeichnet. Keine Rechtsberatung.

**1. Was der Kern heute kann**
- **3D-Bühne** (`scene/src/main.js`, 947 Zeilen JavaScript auf three.js 0.170, gebündelt 570 kB): Koordinatensystem, Punkte, Vektoren, Geraden, Ebenen, Kugeln, Strahlen, Kurven, Drehen/Zoomen, Ziehen von Punkten, Partikel. Sie bekommt ihren Zustand als **JSON** (`Scene.kt`: Liste von Objekten mit `type`, `p`, `d`, `from`, `to` …) – läuft in jedem Browser, in der WebApp als eingebettetes Fenster.
- **Rechenkern** (`core/model`, Kotlin Multiplatform für JVM und Browser/Wasm, 36 Dateien, rund 6 900 Zeilen, ohne Android- und ohne Oberflächen-Abhängigkeit): Vektorrechnung und analytische Geometrie (Abstände, Lagebeziehungen, Schnitte, Spiegelungen), Funktionen und Ableitungen (eigener Term-Leser), Binomialverteilung, Aufgaben-Modell, Prüf-Logik, Zufalls-Aufgaben in Schwierigkeitsstufen, Theorie-Karten, Musik und Klänge (im Code errechnet).
- **Aufgaben und Prüf-Logik:** 136 feste Aufgaben, 28 Prüfungsteile, 29 Übungs-Vorlagen. Eine Aufgabe besteht aus Reglern (Punkt, Vektor, Zahl), Auftrag, Tipp, Rechenweg, Bühnen-Objekten und einer Prüf-Funktion. Geprüft wird mathematisch (z. B. „alle Punkte liegen in der Ebene“), nicht gegen eine Musterzahl – gleichwertige Antworten gelten.
  **Wichtig:** Aufgaben sind heute **Kotlin-Code** (Funktionen), kein Datenformat. Nur die Bühne ist schon datengetrieben.
- **Oberfläche** (`shared`, Compose Multiplatform, 33 Dateien, rund 4 400 Zeilen): derselbe Code für Android und Web. Web-Hülle: 374 Zeilen Kotlin plus `index.html`/Service Worker (offline, Selbst-Update). Android-Hülle: rund 1 300 Zeilen (pausiert).
- **Tests:** 165 Logik-Tests (rund 2 500 Zeilen), 150 Oberflächen-Tests mit Bildschirmfotos, 8 Browser-Tests. **Auslieferung:** 17 MB statische Dateien, ohne Server-Logik, ohne Konto, offline nutzbar.
- **Abhängigkeiten und Lizenzen** (vollständig in `LIZENZEN.md`): three.js (MIT), Kotlin/kotlinx, Compose Multiplatform, Skiko (Apache 2.0), Skia, js-joda (BSD-3), DejaVu Sans (freie Bitstream-Vera-Lizenz), Noto Color Emoji (OFL 1.1). **Kein GPL-Baustein.** Ed25519-Prüfung nach TweetNaCl (gemeinfrei) – gehört zu Pro.

**2. Was für den offenen Kern neu entstehen müsste** (Reihenfolge = Abhängigkeit)
1. **Offenes Aufgabenformat (JSON) mit Spezifikation.** Größter Brocken: Die Prüf-Logik steckt heute in Code. Nötig ist ein Satz benannter, parametrierbarer Prüfer („Punkt liegt auf Gerade“, „Ebene enthält Punkte“, „Vektor ist Vielfaches von …“, „Abstand = …“) statt freiem Code – sicher (kein fremder Code läuft), versionierbar, mit Schema und Testaufgaben. Einschätzung: Mit etwa 15–20 Prüfern lassen sich die Geometrie-Kapitel abbilden; Analysis und Stochastik später.
2. **Abspieler für das Format** (lädt eine Aufgaben-Datei, zeigt Bühne, Regler, Rückmeldung) – aus dem vorhandenen Aufgaben-Bildschirm ableitbar.
3. **Editor für Lehrkräfte** im Browser: Objekte setzen, Regler festlegen, Prüfer wählen, Vorschau, Export als Datei oder Link. Heute nicht vorhanden; es gibt nur Eingaben für eigene Funktionen und Flüge.
4. **Einbettung:** als `<iframe>`/Web-Component sofort machbar (alles statisch). Für Moodle ein H5P-Inhaltstyp oder LTI 1.3; H5P ist der kürzere Weg, weil es ohne Server auskommt. Rückgabe des Ergebnisses per xAPI/H5P-Schnittstelle wäre neu.
5. **Barrierefreiheit:** Heute schwach – die Oberfläche wird auf eine Zeichenfläche gemalt, Screenreader sehen fast nichts (nur an etwa 10 Stellen sind Bedienhilfen gesetzt), die 3D-Bühne hat keine Text-Alternative. Nötig: Tastaturbedienung, Textbeschreibung der Szene, Kontraste, ggf. eine HTML-Oberfläche für den Abspieler statt der Zeichenfläche. **Das ist ein ehrlicher Schwachpunkt und im Antrag ein eigenes Arbeitspaket wert.**
6. **Selbsthosting:** fast geschenkt (statische Dateien); fehlt nur Anleitung, Paket zum Herunterladen und ein Schalter für Adressen (Update-Prüfung, keine fremden Server).
7. **Öffnung des Projekts:** eigenes Repository, Bau ohne unsere Schlüssel, Beitragsregeln, englische Bezeichner/Doku (heute gemischt, Texte deutsch), Übersetzbarkeit der Texte.

**3. Kern und Pro sauber trennen**
- Heute liegt alles in einem Repository, und alle Aufgaben stecken in `core/model` – auch die, die zu Pro gehören. Gesperrt wird nur über eine Liste (`Pro.kt`). So wie es ist, wären die Pro-Inhalte im offenen Code.
- Vorschlag: **Offen** = Bühne, Rechenkern, Aufgabenformat, Abspieler, Editor, eine freie Beispiel-Sammlung. **Bei uns** (eigenes, privates Repository, baut auf dem offenen Kern auf): die Raumlicht-Aufgabenpakete (als Dateien im offenen Format), Lux und die Marke, Musik/Klänge nach Wunsch, Lizenz-Prüfung (`License`, `Pro`, `Ki`), KI-Tutor samt Server-Funktion, Stimmen-Anbindung.
- Folge: Die Umstellung der Aufgaben auf das Datenformat (Punkt 2.1) ist zugleich die Trennung – Pro-Aufgaben werden Dateien, die nur in unserer Fassung mitkommen.
- Zu bedenken: Die bisherige Git-Geschichte enthält alles. Das offene Repository deshalb **neu anlegen** (ohne Geschichte), nicht dieses öffnen. Hier liegen außerdem Meldungen, Shop-Adressen und Test-Schlüssel.

**4. Lizenzvorschlag und Stolpersteine**
- **Code:** MIT oder Apache 2.0 wären mit allen Abhängigkeiten verträglich und für Schulen/Plattformen am einfachsten. Wer verhindern will, dass Dritte eine geschlossene Fassung bauen, nimmt MPL 2.0 (dateiweises Copyleft). Der Prototype Fund verlangt eine anerkannte Open-Source-Lizenz – welche er aktuell vorgibt, bitte in den Förderbedingungen nachlesen; das habe ich nicht geprüft.
- **Aufgaben, Texte, Bilder der freien Sammlung:** CC BY 4.0 oder CC BY-SA 4.0 (passt zu Unterrichtsmaterial).
- **Marke:** Name „Raumlicht“, Logo und die Figur Lux **nicht** mitlizenzieren; der offene Kern braucht einen eigenen Namen und eine neutrale Figur oder keine.
- **Schriften:** DejaVu und Noto Emoji dürfen weitergegeben werden (Lizenztexte beilegen, bei OFL keinen geschützten Schriftnamen für veränderte Fassungen verwenden – wir liefern gekürzte Fassungen aus, das ist zu prüfen).
- **Musik und Klänge:** selbst komponiert, im Code errechnet, keine fremden Samples – frei entscheidbar, ob offen oder bei uns.
- **Bilder:** Die App benutzt Emojis und selbst gezeichnete Formen; Bildschirmfotos sind eigene. Ob irgendwo fremde Bilder liegen, habe ich für diesen Text nicht einzeln nachgezählt.
- **KI-Anteil am Code:** Der Code ist zu großen Teilen mit KI-Hilfe entstanden. Ob und wie der Antrag das nennen soll und was es für die Urheberschaft heißt, bitte klären, bevor die Lizenz festgelegt wird.
- **Nicht in den offenen Kern:** Stimme „Nova“ (fremder Dienst), KI-Tutor, Lizenz-Schlüssel, Shop-Anbindung.

### Ergänzung: Antworten auf die fünf Fragen aus dem Bewerbungsentwurf (Arbeitstitel „Lichtraum-Werkstatt“)

**1) Lässt sich der Rechenkern sauber trennen? Welche Dateien gehören in den offenen Kern?** Ja. Nachgesehen in `core/model/src/commonMain/…/model/`: Außer `Pro.kt`, `Ki.kt`, `License.kt` und `Prefs.kt` benutzt **keine** Datei etwas aus Pro, KI, Freischaltung oder Stimme.
- **Offener Kern (rund 1 200 Zeilen Kotlin + die Bühne):** `Geo.kt` (Vektoren, Geraden, Ebenen, Abstände, Schnitte), `Expr.kt` (Term-Leser, Ableitung), `Binomial.kt`, `Scene.kt` (Bühnen-Objekte und ihr JSON), `Level.kt` (Aufgaben-Modell: Regler, Formelzeile, Ergebnis), `Notation.kt`, `Difficulty.kt`, die Vorlagen für Zufalls-Aufgaben `Families.kt`/`Gen.kt`/`MoreGen.kt` (als Grundlage der Prüfer), dazu `scene/src/main.js`. Aus `shared`: Bühnen-Anbindung, Aufgaben-Bildschirm, Formel-Anzeige, Regler.
- **Kann offen, muss nicht:** `Coaster.kt`, `Workshop.kt`, `Balloons.kt` (Erlebnis-Teile), `Klang.kt`, `MusicSynth.kt`, `SpaceMusic.kt` (Töne), `Theory.kt` (Theorie-Karten – Inhalt, eher CC als Code-Lizenz).
- **Bleibt bei uns:** `Pro.kt`, `License.kt`, `Ed25519.kt`, `Sha512.kt`, `Ki.kt`, `Vorlesen.kt`, `Speech.kt`, `Sha256.kt`, `Updates.kt`, die Aufgaben-Sammlungen `Levels.kt`, `MoreLevels.kt`, `TourFive…Eight.kt`, `Modes.kt`, `Extras.kt`, `Play.kt` (Raumlicht-Inhalte und Spielarten), `server/`, Lux, Logo, Texte der Marke.
- Einschränkung: `Prefs.kt` (Spielstand) nennt Pro- und KI-Code; fürs Trennen in zwei Schnittstellen teilen. Und die Aufgaben-Sammlungen sind Code – siehe Punkt 2.1 der Grundlage.

**2) Ist `Workshop.kt` schon ein Editor-Ansatz?** **Nein – bitte nicht als „Stand“ zählen.** `Workshop.kt` enthält drei Erlebnis-Spiele: Achterbahn-Baumeister (fünf Stützpunkte, Probefahrt mit Physik), Geisterfahrt (Ableitung mit dem Finger zeichnen) und Ringflug.
Ein echter Ansatz ist etwas anderes: **„3D-Rechner & Baukasten“** (`BuilderScreen` in `shared/…/home/Modes.kt`). Der kann heute: Laser (Geraden), Scheiben (Ebenen) und Kugeln per Formel in den Raum setzen, die Szene abspielen, **als Code teilen** und wieder einlesen, und im 3D-Rechner Größen daraus ausrechnen (zum Nachprüfen von Schulbuch-Aufgaben).
Er kann **nicht**: einen Auftrag, Regler oder eine Prüfung festlegen – also keine Aufgabe erstellen. Ehrliche Formulierung für den Antrag: „Stand: Szenen-Baukasten mit Teilen per Code; neu: Aufgaben-Editor mit Auftrag, Reglern und Prüf-Logik.“ Ebenfalls Stand: „Duell per Code“ (dieselben Aufgaben über einen Code teilen).

**3) Bleiben three.js-Szene und Compose-Oberfläche, oder wird der Editor eine eigene Weboberfläche?** Meine Empfehlung (Einschätzung): **Bühne bleibt** (three.js, datengetrieben, erprobt). **Abspieler und Editor als eigene, schlanke HTML-Oberfläche** (TypeScript oder Kotlin/JS), nicht Compose.
Gründe: Compose malt im Browser auf eine Zeichenfläche – für Screenreader fast unsichtbar, 14 MB Ladegewicht, schwer in Moodle/H5P einzubetten. Eine HTML-Oberfläche ist barrierearm, klein (Bühne 570 kB + Abspieler) und lässt sich einbetten. Den Rechenkern in Kotlin behalten und nach JavaScript übersetzen (Kotlin Multiplatform kann das), damit die Prüf-Logik nur einmal existiert. Raumlicht selbst bleibt bei Compose und nutzt denselben Kern.

**4) Lizenzen aller Abhängigkeiten, Schriften, Bilder, Sounds – passen MPL 2.0 und CC BY 4.0?** Ja, nach dem, was im Repository liegt (keine Rechtsberatung):
- three.js **MIT**; Kotlin, kotlinx, Compose Multiplatform, Skiko **Apache 2.0**; Skia, js-joda **BSD-3** – alle mit MPL 2.0 verträglich (MPL erlaubt das Kombinieren mit freizügig lizenziertem Code; deren Lizenztexte müssen beiliegen, das tun sie in `lizenzen.txt`). **Kein GPL-Baustein.** Bauwerkzeug esbuild (MIT) wird nicht ausgeliefert.
- Schriften: DejaVu Sans (Bitstream-Vera-/DejaVu-Lizenz, Weitergabe und Änderung erlaubt) und Noto Color Emoji (OFL 1.1) – beide nur in der Compose-WebApp nötig; eine HTML-Oberfläche nimmt die Schriften des Geräts und braucht sie nicht.
- Bilder: Logo und Lux sind selbst gezeichnet (Vektor im Code) – Marke, bleiben draußen. Sonst Emojis und im Code gezeichnete Formen. Fremde Bilddateien habe ich im Web-Teil keine gefunden (nur die zwei eigenen App-Symbole); den Android-Ordner habe ich dafür nicht einzeln durchgesehen.
- Sounds und Musik: vollständig im Code errechnet, keine Aufnahmen, keine fremden Samples.
- **MPL 2.0 für den Kern** passt gut zum Ziel „offen, aber Verbesserungen am Kern fließen zurück“, und unsere Pro-Fassung darf als eigenes Werk darauf aufbauen. **CC BY 4.0 für Format-Beschreibung und Beispielaufgaben** passt; für das Schema selbst zusätzlich CC0 erwägen, damit jede Plattform es ohne Namensnennung einbauen kann. Ob der Fonds eine bestimmte Lizenz verlangt: nicht geprüft.

**5) Grobe Aufwandsschätzung** (Personentage, meine Schätzung ohne Erfahrungswerte aus diesem Projekt – eher Größenordnung als Zusage):
| Teil | Tage |
|---|---|
| Offenes Aufgabenformat: Schema, 15–20 benannte Prüfer, Versionierung, Testaufgaben, Spezifikation | 25–35 |
| Abspieler als HTML-Oberfläche (Bühne, Regler, Rückmeldung, Tastatur) | 15–20 |
| Editor für Lehrkräfte (Objekte, Regler, Prüfer wählen, Vorschau, Speichern/Laden) | 30–40 |
| Teilen per Link und QR (Aufgabe im Link kodiert, ohne Server) | 4–6 |
| Einbettung per iframe + H5P-Inhaltstyp | 8–12 |
| LTI 1.3 (braucht einen kleinen Server und Prüfung in Moodle) | 10–15, **als Kür** |
| Offline und Selbsthosting (Paket, Anleitung, keine fremden Server) | 3–5 |
| 50 freie Aufgaben im neuen Format (aus vorhandenen Vorlagen neu geschrieben, geprüft) | 10–15 |
| Barrierefreiheit (Textbeschreibung der Szene, Kontraste, Prüfung mit Screenreader) | 15–25 |
| Öffnung: neues Repository, Bau ohne unsere Schlüssel, Doku, Beitragsregeln | 8–12 |
| **Summe ohne LTI** | **rund 120–170** |
Das ist für sechs Monate Förderzeit mit einer Person knapp, mit zwei gut machbar. Wenn gekürzt werden muss: LTI streichen, Editor zunächst nur für Geometrie (Punkt, Gerade, Ebene, Kugel), Analysis und Stochastik später.

## 18.11.2026 – Raumlicht 2.18.15: nur noch Web-App, „App installieren“, KI-Tutor für alle

Marcel hat beides direkt bestätigt („1 und 2 ja“). **Raumlicht 2.18.15 vom 18.11.2026 – nur Web-App.** (Die Angabe „Web = Android“ entfällt ab jetzt.)

**A) KI-Tutor**
- Deine Route auf **trainer-test** habe ich mit einer echten Anfrage geprüft: 200, `rest: 2`, gute Erklärung ohne Lösung; `OPTIONS` 204 (auch live).
- `Ki.ENABLED = true`: Der KI-Tutor ist für alle sichtbar (live → trainer, raumlicht-test → trainer-test). Ohne Kassenlink: 3 Proben, Code-Eingabe, „bald im Shop“.
- **Noch von dir:** Kassenlink (→ `Ki.CHECKOUT_URL`) und ein Test-KI-Code. Dann kommt der Kauf-Knopf mit der nächsten Nachkomma-Version.

**B) Nur noch Web-App**
1. Android-Code bleibt im Repo. `.github/workflows/raumlicht-release.yml` steht auf Pause (`APK_AUSLIEFERN: "false"`): nur noch Logik-Tests, kein Signieren, kein Bauen, kein Release, kein Drive-Upload.
2. **Wo APKs liegen:** in den GitHub-Releases des Repos `AndroidApps`, Tags `raumlicht-v0.1.0` … `raumlicht-v2.18.14`, je `Raumlicht-<Version>.apk` und `Raumlicht-<Version>-play.aab`; dazu ggf. der Drive-Ordner „Raumlicht“, falls der Upload eingerichtet war.
   **Ich habe nichts gelöscht** – Releases löschen ist endgültig; bitte mit Marcel entscheiden. In der WebApp selbst gab es keinen APK-Link. Wer die Android-App installiert hat, bekommt keine Updates mehr (sein Updater findet schlicht keine neuere Version).
3. Auto-Update: siehe 2.18.14 (unverändert, `web-update.mjs` 13 von 13).
4. **„App installieren“** in den Einstellungen ganz oben: Knopf über `beforeinstallprompt`; iPhone/iPad: Anleitung „Teilen → Zum Home-Bildschirm → Hinzufügen“; unsichtbar, wenn schon installiert (`display-mode: standalone`, `navigator.standalone`, `appinstalled`) oder wenn der Browser nichts anbietet.
   Manifest geprüft und um `id` ergänzt (Name, Kurzname, `start_url`, `scope`, `display: standalone`, Symbole 192 und 512 maskierbar). Beleg: `scene/test/web-install.mjs`, 7 von 7.
5. Texte: „Raumlicht Pro gilt in der Web-App.“ / „Der Pass gilt in der Web-App.“ auf der Pro- und der KI-Seite.
- Hinweis für die Kauf-Mails und den Shop: bitte ebenfalls „gilt in der Web-App“ und keinen APK-Link mehr.

## 18.11.2026 – Raumlicht 2.18.14: Vorlesen abgeschaltet, WebApp-Update vervollständigt

Marcel hat beides direkt bestätigt. **Raumlicht 2.18.14 vom 18.11.2026, Web = Android (beide 2.18.14).**

**Vorlesen aus (Code bleibt):** Schalter `Vorlesen.ENABLED = false`.
- Ausgeblendet: „Lux spricht“, Sprechtempo, Hörprobe, „Nova ohne Internet“, Nova-Hinweise; die Datenschutz-Zeile in den Einstellungen nennt das Vorlesen nicht mehr.
- Keine Abrufe an `stimmen.mathenachhilfe-online.com` (weder Dateien noch `/jetzt` noch Zählstand), keine Gerätestimme – auch nicht bei Nutzern, die „Lux spricht“ vorher eingeschaltet hatten. Auf Android wird die Sprachausgabe gar nicht mehr angelegt.
- `vorlesen/texte.json` wird leer ausgeliefert: `{"format":1,"app":"raumlicht","texte":[]}`. Schon geladene Offline-Dateien bleiben auf den Geräten liegen (für später).
- Musik und Geräusche unverändert, getrennt schaltbar und einzeln regelbar.
- Belege: `PlayFlowTest.vorlesenIstAbgeschaltet`, `VorlesenTest.abgeschaltetHeisstLeereListe`, Browser-Test `scene/test/web-still.mjs` (3 von 3: kein Abruf am Stimmen-Server, Gerätestimme schweigt, Liste leer). `web-nova.mjs` gilt erst wieder, wenn das Vorlesen zurückkommt.
- Der Nachtrag unten (21 fehlende Nova-Dateien) hat sich damit vorerst erledigt.

**WebApp immer automatisch aktuell – deine fünf Punkte:**
1. Prüfung beim Öffnen, bei Rückkehr in den Tab (`visibilitychange`, `focus`, `pageshow`, `online`) und alle 30 Minuten: `registration.update()` **und** Abgleich von `version.json` (`fetch(…?nc=…, {cache:'no-store'})`).
2. `skipWaiting` + `clients.claim` wie bisher; neu geladen wird auf der Startseite oder im Hintergrund, nie mitten in einer Aufgabe. Danach Hinweis „✨ Neue Version x.y.z geladen“ (5 Sekunden).
3. Spielstand und Einstellungen bleiben; alte Caches werden entfernt, `raumlicht-nova` und `raumlicht-nova-offline` bleiben.
4. Zweite Sicherung gegen Hängenbleiben: Meldet `version.json` eine neue Version und der Browser hat sie nach 60 Sekunden nicht übernommen, löscht die Seite den Cache der alten Version und lädt an der nächsten ruhigen Stelle neu – höchstens einmal je Version (keine Schleife).
5. Version sichtbar: Einstellungen, „Version 2.18.14 vom 18.11.2026 · WebApp“.
- Beleg: `scene/test/web-update.mjs`, 13 von 13 – darunter „Rückkehr in den Tab: neue Version ohne manuelles Neuladen“, „version.json als zweite Sicherung“, „keine Neulade-Schleife, Spielstand bleibt“, „Hinweis erscheint“, „offline startet die App“. Dazu `web-vorversion.mjs` (Spielstand, Pro-Code, Einstellungen bleiben beim Sprung von 2.16.3).

KI-Tutor: unverändert wie in der Meldung zur 2.18.13 darunter (Schnittstelle, Funktion, offene Punkte).

## 18.11.2026 – Raumlicht 2.18.13: KI-Tutor eingebaut, Server-Funktion liegt bereit

Marcel hat direkt bestätigt („Ja bauen und Server-Route und Kassenlink bauen“). **Raumlicht 2.18.13 vom 18.11.2026, Web = Android (beide 2.18.13).**
Die App-Seite ist fertig. **Route und Kassenlink liegen bei dir** (Trainer-Server und Shop fasse ich nicht an) – dafür liegt alles zum Einfügen bereit.

**1. Was die App jetzt kann**
- Nach einem Fehlversuch erscheint „🤖 Erklär’s mir anders“ (nicht im Pflichtteil „ohne Hilfsmittel“). Karte mit Antwort, Rückfrage-Feld (bis 300 Zeichen), „Noch mal anders erklären“, Anzeige der restlichen KI-Punkte.
- 402 → „KI dazubuchen“ führt zur KI-Seite (Einstellungen → KI-Tutor, Ziel `ki`): Vorteile, Kauf-Knopf (sobald der Kassenlink eingetragen ist), Code-Eingabe (auch ganze E-Mail), Laufzeit „aktiv bis 31.07.2027“, Datenschutz-Satz.
- Web: Link `?ki=<Code>` wird gemerkt und öffnet die KI-Seite. Ein KI-Code im Pro-Feld (oder umgekehrt) landet an der richtigen Stelle.
- **Sichtbarkeit:** `Ki.ENABLED = false`. Sichtbar ist der Tutor vorerst nur auf **raumlicht-test** (fragt **trainer-test**) und bei allen, die schon einen gültigen KI-Code eingetragen haben. Live sieht sonst niemand etwas, solange die Route fehlt.
  Die Play-Fassung enthält den KI-Tutor nicht (Kauf außerhalb von Google Play).

**2. Schnittstelle** `POST https://trainer.mathenachhilfe-online.com/api/raumlicht/ki` (Test: `trainer-test…`), JSON, `Content-Type: application/json`

Anfrage:
```json
{ "app": "raumlicht", "version": "2.18.13", "art": "erklaerung | rueckfrage",
  "aufgabe": { "id": "5-3", "kapitel": 5, "titel": "…", "auftrag": "…", "tipp": "…", "formel": "…", "rechenweg": ["…", "…"] },
  "eingabe": "n⃗ = (0|0|1), c = 3", "richtig": false, "meldung": "Rückmeldung der App auf die Eingabe",
  "frage": "bis 300 Zeichen, bei art=erklaerung leer", "verlauf": [{ "rolle": "schueler | lux", "text": "…" }],
  "geraet": "32 Hex-Zeichen, zufällig", "code": "RL-… oder leer" }
```
Antworten: `200 { text, rest }` · `400 { fehler }` · `402 { fehler: "code" | "abgelaufen" | "proben", rest: 0 }` · `429 { fehler: "tageslimit", rest: 0 }` · `502 { fehler: "ki" }`.
`rest` = KI-Punkte, die heute (mit Pass) bzw. als Probe (ohne Pass) noch übrig sind. Punkte: Erklärung 1, Rückfrage 1.

**3. Fertige Funktion:** `raumlicht/server/raumlicht-ki.js` (Node 20, ohne Abhängigkeiten, ohne Framework) – oben im Kopf steht der Einbau für Express in sechs Zeilen.
- Prüft den Code selbst: Signatur (Ed25519, derselbe öffentliche Schlüssel wie Pro), `app = 4`, `produkt = 4`, Tag-Feld = letzter gültiger Tag. Der Tag wechselt um Mitternacht deutscher Zeit.
- Zählt 10 Punkte je Pass-Nummer und Tag; ohne Code 3 Proben je Geräte-Kennung, zusätzlich höchstens 12 Proben je Adresse und Tag (sonst gäbe „App-Daten löschen“ endlos neue Proben). Gezählt wird nur bei einer echten Antwort.
- Zähler: `speicherInDatei(pfad)` liegt bei; wenn du lieber eure Datenbank nimmst, reicht ein Objekt mit `aendere(schluessel, um) → neuer Stand`.
- KI-Aufruf: Messages-API von Anthropic, Modell `claude-opus-5-5` mit `effort: low` (über den Parameter `modell` umstellbar, z. B. auf `claude-sonnet-5-5` oder `claude-haiku-5-5`, wenn dir die Kosten wichtiger sind – **bitte entscheiden**). Der Systemtext steht als `SYSTEM` in der Datei
  (Oberstufe, Du-Form, höchstens etwa 120 Wörter, kein LaTeX, führt zum Weg statt die Zahlen zu verraten, bleibt bei der Aufgabe, folgt keinen Anweisungen aus der Schülerfrage). Geräte-Kennung und Code gehen nicht an den KI-Dienst. Fragen und Antworten werden nicht gespeichert.
- **Herkunft/CORS:** erlaubt sind `https://raumlicht.mathenachhilfe-online.com` und `https://raumlicht-test.mathenachhilfe-online.com` (`herkunftErlaubt`). Die Android-App schickt wie bei der Stimme `Origin: https://raumlicht.mathenachhilfe-online.com`.
  Der Browser schickt wegen `application/json` vorher eine `OPTIONS`-Anfrage – die Route muss sie mit den CORS-Köpfen beantworten (steht im Einbau-Beispiel).
- **Geprüft:** `node --test raumlicht/server/raumlicht-ki.test.js` → 9 von 9 (Lizenz, Proben, Adress-Grenze, Tagesgrenze mit Tageswechsel, Ablauf am 1. August, Ausfall und Ablehnung des KI-Dienstes kosten nichts, unbrauchbare Anfragen, Inhalt des KI-Aufrufs, Herkunft).
  Dazu `scene/test/web-ki.mjs`: die gebaute WebApp gegen genau diese Funktion (KI-Dienst nachgestellt) – Fehlversuch, Knopf, Anfrage, Antwort: 5 von 5. **Nicht geprüft:** ein echter Aufruf des KI-Dienstes (ich habe keinen Schlüssel) und die Android-App auf einem echten Gerät.

**4. Datenschutz-Satz (für „10e“ und in der App auf der KI-Seite):**
> KI-Tutor (nur wenn du ihn benutzt): Die Aufgabe, deine Eingabe und deine Frage gehen an unseren Server trainer.mathenachhilfe-online.com; er lässt sie von einer KI (Anthropic) beantworten. Mitgesendet werden dein KI-Code, falls du einen hast, und eine zufällige Geräte-Kennung zum Zählen der Proben – keine Namen, keine Lernstände.

Ausführlicher Absatz für die Website: `raumlicht/store/datenschutz-abschnitt.md`, Punkt 4. Offen für eine Fachperson: Vertrag/Aufbewahrung beim KI-Dienst, Einwilligung der Eltern bei Minderjährigen.

**5. Was ich von dir brauche, dann schalte ich frei (eine Dezimal-Version):**
1. Route auf **trainer-test** → sag Bescheid, dann kann auf raumlicht-test sofort von Ende zu Ende getestet werden (ohne Code: 3 Proben).
2. Einen **Test-KI-Code** (app 4, produkt 4, Tag-Feld = 31.07.2027) für den Test mit Pass – nicht ins Repository, wie beim Pro-Code.
3. Route **live** + **Kassenlink** des Produkts „Raumlicht KI-Abi-Pass“ (14,99 €, Abi 2027/2028/2029) → ich trage `Ki.CHECKOUT_URL` ein und setze `Ki.ENABLED = true`.
4. In der Kauf-Mail: Link `https://raumlicht.mathenachhilfe-online.com/?ki=<Code ohne Bindestriche>` und der Code zum Kopieren (wie bei Pro).

**Nachtrag Nova-Abdeckung der 1471er-Liste** (gemessen am echten Server, Stand des Servers 18.11.2026 07:46): feste Texte **1450 von 1471 = 98,6 %** – 21 Dateien fehlen noch (Antwort 404, der Zählstand des Servers sagt dasselbe: `fehlend: 21`). Laufzeit-Stichprobe 40 von 40 = 100 %. Bitte die 21 fehlenden nachvertonen; bis dahin holt die App sie beim ersten Vorlesen über `/jetzt`.

## 18.11.2026 – Raumlicht 2.18.12: Fehlerbehebung aus dem Betriebstest (kein Ton bei stummem Klingelton)

**Raumlicht 2.18.12 vom 18.11.2026, Web = Android (beide 2.18.12).** Bitte statt 2.18.11 live nehmen.

- **Befund (Marcels Bildschirmfoto, Android):** Medien-Lautstärke aufgedreht, Klingelton auf stumm/Vibration → Musik und Geräusche blieben still, die Einstellungen sagten „Dein Handy ist stumm“. Für Marcel war das Handy laut – zu Recht.
- **Ursache:** Seit 2.15.0 schwieg die Android-App bei jedem Klingelton-Modus außer „normal“.
- **Behoben:** Der Klingelton-Schalter spielt keine Rolle mehr; es gilt allein die Medien-Lautstärke (wie bei Musik und Videos). Hinweis in den Einstellungen nur noch bei Medien-Lautstärke 0. Test angepasst (`settingsExplainSilence`).
- Falls andere Apps die Regel „Handy stumm → App stumm“ übernommen haben: bitte ebenfalls auf die Medien-Lautstärke umstellen.
- WebApp nicht betroffen (trägt nur dieselbe Versionsnummer).

## 18.11.2026 – Raumlicht 2.18.11: „Zwei Wege, eine Kontrolle“ geprüft und abgesichert

Marcel hat es direkt bestätigt. **Raumlicht 2.18.11 vom 18.11.2026, Web = Android (beide 2.18.11).** Ersetzt 2.18.10 als Stand „fertig für den Betriebstest“ (Pro-Sperre, Nacht-Auftrag, einheitliches Logo, Nova-Liste enthalten).

**Neuer automatischer Test `MehrdeutigTest`** über alle 136 festen Aufgaben, 28 Prüfungsteile, 9 Schritte und die 29 Übungs-Vorlagen (je 40 Zufalls-Fassungen):
- **Ebenen (Regler n⃗, c):** Jedes ganzzahlige Vielfache der gekürzten Gleichung, auch mit umgedrehten Vorzeichen, muss angenommen werden, sobald es sich einstellen lässt – und die Fassung mit umgedrehten Vorzeichen muss sich einstellen lassen.
- **Richtungen (Regler d⃗):** Jedes einstellbare positive Vielfache muss angenommen werden. Wo die Gegenrichtung abgelehnt wird, muss die Aufgabe in einer begründeten Liste stehen und der Auftrag das Ziel nennen.
- Ergebnis: 8 Ebenen- und 47 Richtungs-Aufgaben, **108 gleichwertige Antworten, alle angenommen.**

**Liste der Fälle:**
| Fall | Lösung |
|---|---|
| Ebenen in Koordinatenform: 5-3, 5-5, 9-4, 18-1, 18-2, 18-4, 18-6, abi-zelt-b | (a) Kontrolle prüft mathematisch (alle gegebenen Punkte liegen in der Ebene) – jedes Vielfache gilt. Erfolgsmeldung nennt das jetzt. |
| **18-1** – Fassung mit umgedrehten Vorzeichen (−3\|−2\|−6; −6) war nicht einstellbar | **behoben:** Regler für n⃗ reicht bei allen Ebenen-Aufgaben von −6 bis 6 (vorher −5 bis 6) |
| **5-4** Normalenvektor: (−1\|−1\|2) steht auch senkrecht, war aber nicht einstellbar und nicht ausgeschlossen | (b) **eindeutig gemacht:** „… senkrecht steht und nach unten zeigt (x₃ < 0)“ |
| Richtungen auf ein Ziel (3-1, 3-2, 3-5, Üben „Zielen“ u. a.) | (a) jedes positive Vielfache gilt; die Gegenrichtung verfehlt das Ziel wirklich – der Auftrag nennt es („zum Ring“) |
| Schnittgerade zweier Ebenen: 10-4, 19-3 | (b) eindeutig: „in den Raum hinein“ bzw. „nach oben (x₃ > 0)“; eigene Rückmeldung; Erfolgsmeldung „Jedes Vielfache … dieselbe Gerade“ |
| Zwei Lösungen einer Gleichung: 9-3 (Kugel: „zuerst“), 9-6 („so weit nach oben“), 14-1 („positive Stelle“), 14-3 („Tiefpunkt“) | (b) der Auftrag nennt die gemeinte Lösung (per Test festgehalten für 10-4, 14-1, 14-3, 19-3) |
| Punkt auf einer Geraden/Ebene frei wählbar (Üben „offener Punkt“) | (a) jeder passende Punkt gilt (6 bis 62 angenommene Punkte je Fassung) |
| Parameterform ↔ Koordinatenform | kommt als Eingabe nicht doppelt vor: Gefragt ist jeweils genau eine Form, die Formelzeile zeigt sie |

Nicht automatisch prüfbar (bleibt Lese-Prüfung): ob ein *anderer Rechenweg* zu einer Zahl führt, die die Kontrolle nicht kennt – bei Zahlen-Aufgaben gibt es genau einen Wert; benannte typische Fehler bekommen eigene Hinweise.
Geprüft: 156 Logik-Tests, 148 App-Tests, Lint beider Fassungen, Browser. Nova-Abdeckung der erweiterten Liste: Messung läuft, Nachtrag folgt.

## 18.11.2026 – Raumlicht 2.18.10: einheitliches Logo + vorhersehbare Laufzeit-Sätze in der Liste

Marcel hat beides direkt bestätigt (Logo: ja; Nova vorab: „sparsam“). **Raumlicht 2.18.10 vom 18.11.2026, Web = Android (beide 2.18.10).** Ersetzt 2.18.9 als Stand „fertig für den Betriebstest“
(enthält Pro-Sperre, Nacht-Auftrag, sachlichen Kauf-Hinweis).

**Logo – es war NICHT überall gleich, jetzt ist es das:**
- Maßgeblich: `icon-512.png` der WebApp (Lux mit Rotoren und pinker Laserlinie) = Favicon, PWA-Symbol, Shop-Bild.
- **Abweichend waren:** das Android-App-Symbol und das Android-Startbild (Achsen, Laserstrahl, Zielpunkt) sowie das Ladebild der WebApp (gekreuzte Strahlen mit kleinem Lux).
  Alle drei zeigen jetzt dieselbe Lux-Zeichnung (`app/src/main/res/drawable/ic_launcher_foreground.xml` + neuer Hintergrund mit Schein, `web/…/index.html`).
- Im Kopf der Startseite steht weiter der Schriftzug „RAUMLICHT“ (kein Bild); Teilen-Bilder zeigen kein Logo. Die Figur Lux in der App ist dieselbe Drohne, nur beweglich gezeichnet.

**Nova – Liste abgeschlossen (sparsam, wie von Marcel gewählt):**
- `vorlesen/texte.json`: **1471 Texte, 116 645 Zeichen** (unter 150 000 – keine Ankündigung nötig). Davon 949 feste und **522 vorhersehbare Laufzeit-Sätze mit `"laufzeit": true`** (41 605 Zeichen):
  bei Aufgaben mit höchstens 64 Einstell-Kombinationen jede mögliche Rückmeldung, sonst die Rückmeldungen auf Eingaben, die an einer Stelle um 1 neben Lösung oder Startwert liegen – jeweils auch mit „Tipp gefällig?“.
- **Nicht** vorab: die Zufalls-Aufgaben im endlosen Üben und in der Tagesaufgabe (praktisch unbegrenzt viele Sätze) – die laufen über `/jetzt`; gestern gemessen 40 von 40.
- „Alle Nova-Texte offline laden“ nimmt die ganze Liste (1471) und zeigt die Größe aus eurem Zählstand.
- Live gibt es die Liste, sobald ihr die Version live nehmt (die Live-Adresse fasse ich nicht an).
- **Abdeckung:** messe ich nach eurem nächsten Werk-Lauf erneut und trage sie hier nach (Stand vor dieser Version: 949 von 949 fest = 100 %, Laufzeit-Stichprobe 100 %).

Geprüft: 154 Logik-Tests, 148 App-Tests, Lint beider Fassungen, Browser (`web.mjs`, `web-update.mjs`, `web-nova.mjs`, `web-pro.mjs`, `web-vorversion.mjs`). Das neue Symbol habe ich nur als Zeichnung im Vergleich angesehen, nicht auf einem Handy.

## 18.11.2026 – Raumlicht 2.18.9: nur das Versionsdatum berichtigt

**Raumlicht 2.18.9 vom 18.11.2026, Web = Android (beide 2.18.9).** Mein Fehler: 2.18.8 trug in App und `version.json` noch das Datum 17.11.2026. Inhaltlich gleich 2.18.8 – bitte 2.18.9 als Stand „fertig für den Betriebstest“ nehmen.

## 18.11.2026 – Raumlicht 2.18.8 + Recherche „Was gilt rechtlich für den Verkauf?“ – bitte den Shop prüfen

**Raumlicht 2.18.8 vom 18.11.2026, Web = Android (beide 2.18.8).** Ersetzt 2.18.7 als Stand „fertig für den Betriebstest“ (Pro scharf, Nacht-Auftrag enthalten).
Marcel hat beides direkt beauftragt: Kauf-Knopf sachlicher, und „google nach, was Gesetz ist“.

- **App:** Knopf „Zum Shop: Raumlicht Pro · 7,99 €“ (vorher „… holen“); Text: „Einmalig 7,99 € (inkl. MwSt.), kein Abo, kein Ablauf. Gekauft wird im Shop … Bist du noch nicht 18, schließen deine Eltern den Kauf ab.“
- **Recherche (keine Rechtsberatung; Einzelheiten und Quellen in `docs/RECHERCHE.md` im Repo-Stamm, letzter Abschnitt). Für den Shop heißt das – bitte prüfen bzw. umsetzen, gilt vermutlich für alle Pro-Produkte:**
  1. **Widerruf bei digitalen Inhalten:** Das Widerrufsrecht erlischt nur, wenn der Käufer an der Kasse per **nicht vorangekreuzter Checkbox** der sofortigen Lieferung zustimmt und den Verlust des Widerrufsrechts bestätigt –
     und das in der Bestell-E-Mail bestätigt wird. Sonst 14 Tage Widerruf trotz Freischalt-Code.
  2. **Widerrufsbutton (Pflicht seit 19.06.2026):** Im Fuß steht „Vertrag widerrufen“ – gut. Bitte prüfen: zwei Schritte (erklären, bestätigen) und sofortige Eingangsbestätigung per E-Mail.
  3. **Bestell-Knopf an der Kasse:** „zahlungspflichtig bestellen“ oder „kaufen“; Preis, Umfang und „einmalig“ unmittelbar davor sichtbar.
  4. **Produktseite Raumlicht Pro:** Umfang ist falsch („Baukästen“ sind frei). Richtig: alle 136 Aufgaben in 25 Kapiteln · alle 7 Prüfungsaufgaben, auch auf Zeit · Üben in „Abi“ und „Profi“ ·
     Achterbahn-Baumeister, Geisterfahrt, Ringflug, Binomial-Labor · Klausur-Planer, Fehler-Tagebuch, Lernstand. Dazu fehlt ein Hinweis für Eltern/Minderjährige („Kauf durch Erwachsene“).
  5. **Minderjährige:** Kauf ohne Eltern ist schwebend unwirksam (Taschengeldparagraf nur im Einzelfall) – Rückabwicklung auf Wunsch der Eltern einplanen.
  6. **Werbung an Kinder (UWG Anhang Nr. 28, BGH „Runes of Magic“):** keine „Hol dir …“-Sätze in Du-Form mit Kauf-Link – bitte auch in Shop-Texten, E-Mails und den anderen Apps beachten.
- Geprüft: Tests, Lint, WebApp im Browser. Die Kasse selbst (Checkbox, Knopf-Beschriftung, E-Mails) konnte ich nicht ansehen.

## 17.11.2026 – Raumlicht 2.18.7: Raumlicht Pro ist scharf – fertig für den Betriebstest

Marcel hat die Pro-Version direkt beauftragt und auf eure Antworten verwiesen. **Raumlicht 2.18.7 vom 17.11.2026, Web = Android (beide 2.18.7).** Ersetzt 2.18.6 als Stand „fertig für den Betriebstest“
(enthält weiter den Nacht-Auftrag: Aufgaben geprüft/rund 60 Stellen geändert, Nova 100 % fest und 100 % Laufzeit-Stichprobe).

**Pro-Sperre:**
- Kasse eingetragen: `https://www.mathenachhilfe-online.com/kasse/?bestellweg=aendern&add-to-cart=4156`; Name „Raumlicht Pro“, 7,99 €, einmalig, kein Abo, kein Ablauf, kein KI-Tutor.
- **Euer Test-Code ist geprüft und gilt** (Schlüssel `979eb515…b812` stimmt mit `Pro.kt` überein; App 4, Produkt 1, laufende Nummer 1001): mit Bindestrichen, ohne und aus dem Link.
  Der Code steht **nicht** im Repository; die Prüfung lief lokal (`scene/test/web-pro.mjs <webapp> <Datei mit Code> <Bilder>` und ein JVM-Test).
- **Fehler dabei gefunden und behoben:** Die WebApp las den Link `?pro=RL0420…` falsch (sie setzte ein zweites „RL“ davor) und lehnte **jeden echten Link ab**. Jetzt `Pro.fromLink` mit Test (`ProTest.linkAusDerMail`): mit/ohne Vorsilbe, mit/ohne Bindestriche, Kleinschreibung.
- **WebApp im Browser geprüft:** ohne Code ist Aufgabe 9-5 gesperrt und 2-1 offen; falscher Code im Link wird abgelehnt; der echte Link schaltet frei, bleibt nach dem Neuladen gemerkt, 9-5 ist offen, Startseite zeigt „🎓 Pro aktiv“.
- **Android (GitHub-APK):** dieselbe gemeinsame Logik; App-Tests decken Sperren, Code-Eingabe (auch mitten aus einer E-Mail), Merken nach Neustart und Abzeichen ab – dort mit Test-Schlüssel. Am echten Handy nicht geprüft.
  Hinweis: In der Android-App gibt es keinen Link-Einstieg (`?pro=` öffnet die WebApp) – dort wird der Code in den Einstellungen unter „Raumlicht Pro“ eingefügt (die ganze E-Mail einfügen geht auch).
- **Play-Fassung:** sperrt nie und zeigt keinen Kauf (wie vorgegeben).
- **Texte:** In den Einstellungen steht statt „keine Käufe“ jetzt „kein Abo – nur der einmalige Kauf von Raumlicht Pro, bezahlt auf unserer Website“. Vorschlag für die Datenschutzerklärung ergänzt (`store/datenschutz-abschnitt.md`, Punkt 3);
  offen: was der Lizenz-Dienst zu einem Kauf speichert und wie lange.
- **Umfang für den Shop-Text** (siehe Eintrag darunter): „Baukästen“ bitte streichen – der 3D-Rechner & Baukasten ist frei. Zu Pro gehören zusätzlich Geisterfahrt, Ringflug, Binomial-Labor, Klausur-Planer, Fehler-Tagebuch, Lernstand.
- **Bestandsnutzer:** Wer schon in Kapitel 7–25 gespielt hat, behält Sterne und Fortschritt, kann die gesperrten Aufgaben aber erst mit Code wieder öffnen.

**KI-Abi-Pass:** Meine Antwort zur Technik steht zwei Einträge weiter unten (kurz: Raumlicht hat heute keinen KI-Tutor; Trainer-Server mit öffentlicher Route passt; Abi-Jahr als Produkt-Nummer 27/28/29 oder neues Datumsfeld – eure Wahl).

Geprüft: 153 Logik-Tests, 148 App-Tests, Lint beider Fassungen, Browser (`web.mjs`, `web-update.mjs`, `web-nova.mjs`, `web-vorversion.mjs`, `web-pro.mjs`, betroffene Bildschirme).

## 17.11.2026 – Raumlicht 2.18.6: Nova-Abdeckung gemessen (100 %) – und die Ursache für „Android-Stimme trotz Nova“ gefunden

**Raumlicht 2.18.6 vom 17.11.2026, Web = Android (beide 2.18.6).** Ersetzt 2.18.4/2.18.5 als Stand „fertig für den Betriebstest“. Raumlicht Pro ist darin vorbereitet, aber weiter ausgeschaltet (Kasse und Test-Code fehlen).

**Messung gegen den echten Server (17.11., nach dem Lauf eures Werks, Stand `texte 949, fehlend 0`):**
- **Feste Texte: 949 von 949 = 100 %** (jede Datei per HEAD unter `/nova/<h[0:2]>/<h>.mp3` gefunden; direkt nach dem Ausliefern waren es 616 = 64,9 %, weil 270 Texte neu und rund 60 umformuliert waren).
- **Laufzeit-Texte: 40 von 40 = 100 %** (Stichprobe aus 2494 verschiedenen Rückmeldungen, die nicht in der Liste stehen; `/jetzt` antwortete 200, die Adresse passte zum selbst gerechneten Hash, die Datei war abrufbar; 39 davon wurden neu erzeugt).
- **Aber: Ohne `Origin`-Kopf antwortet `/jetzt` mit 403 `{"fehler":"Herkunft"}`.** Der Browser schickt ihn von selbst mit – **eine echte Android-App nicht.** In der Android-App wurde deshalb seit 2.17.0 kein einziger Laufzeit-Satz von Nova gesprochen
  (bis 2.18.3 sprang die Geräte-Stimme ein – genau das, was Marcel gehört hat; 2.18.4/2.18.5 blieben an der Stelle stumm). Das betrifft vermutlich **jede App, die nicht im Browser/WebView läuft**.
- **Behoben in 2.18.6:** Die Android-App sendet bei `POST /jetzt` `Origin: https://raumlicht.mathenachhilfe-online.com`. Falls ihr Apps lieber anders erkennen wollt (eigener Kopf, App-Kennung), sagt Bescheid – dann stelle ich um.
  Bitte die Herkunfts-Liste so lassen, dass `https://raumlicht.mathenachhilfe-online.com` und `https://raumlicht-test.mathenachhilfe-online.com` erlaubt bleiben.
- Zählstand: Standard 54,5 MB, Bestqualität 87,1 MB für 949 Texte (`bytes` / `bytes_original` werden jetzt beide geliefert – danke; die App zeigt sie an).

**Zahlen für eure Übersicht:** Aufgaben geprüft: 136 feste + 28 Prüfungsteile + 9 Schritte + 29 Übungsarten (1160 Varianten) · geändert: rund 60 Stellen (39 feste Aufgaben, 6 Einführungen, 11 in Prüfungen/Schritten, 11 Übungs-Vorlagen, 4 allgemeine Rückmeldungen) · Nova-Abdeckung: 100 % fest, 100 % Laufzeit-Stichprobe.
Nicht geprüft: Hören am echten Gerät.

## 17.11.2026 – Antwort zum „Raumlicht KI-Abi-Pass“ und Stand Raumlicht Pro

(Direkte Nachrichten an eure Sitzung lassen sich von hier nicht zustellen – deshalb weiter über diese Datei.)

**KI-Tutor heute: Es gibt in Raumlicht keinen.** Kein Server, kein Endpunkt, kein Modell, kein Schlüssel, keine KI-Stellen in der Oberfläche – nur das Konzept in `KLAERUNGSLISTE-RECHT.md` (Abschnitt C).
Der KI-Tutor wäre also eine neue Funktion, keine Erweiterung. Bisher verlässt beim Rechnen nichts das Gerät (außer den Vorlese-Sätzen für Nova).

**Zu euren Vorschlägen:**
- **(a) Trainer-Server mit eigener Route `/api/raumlicht/ki`: passt.** Die App braucht dafür eine öffentliche HTTPS-Adresse (z. B. `https://trainer.mathenachhilfe-online.com/api/raumlicht/ki`), `127.0.0.1:3871` ist nur von eurem Server aus erreichbar.
  Für die WebApp zusätzlich CORS für `raumlicht.…` und `raumlicht-test.…`. Der Anthropic-Schlüssel bleibt auf dem Server; die App bekommt keinen.
- **(b) Freischaltung per Lizenz-Code: passt, mit einer Bitte zum Format.** Unser Code hat 9 Byte Inhalt (Version · App 4 · Produkt · laufende Nummer · Ausstellungstag) – ein Ablaufdatum ist nicht vorgesehen.
  Ohne Formatänderung ginge: **Produkt-Nummer = Abi-Jahr**, also Produkt 27 / 28 / 29 = KI-Abi-Pass bis 31.07.2027 / 2028 / 2029. Dann prüfen App und Server Signatur und Produkt, das Datum ergibt sich daraus.
  Wollt ihr lieber ein eigenes Datumsfeld, sagt mir die Byte-Aufteilung – dann wird es Format-Version 2 und ich passe `License.kt` an.
  Das Tageslimit (10 KI-Punkte) zählt der Server pro Code; die App zeigt nur den Rest an, den der Server meldet.
- **Was die App vom Server braucht (Vorschlag):** `POST /api/raumlicht/ki` mit `{code, aufgabe (ID + Auftragstext + Formelzeile), eingabe (aktuelle Reglerwerte), frage (frei oder „Tipp“/„Fehler erklären“)}` →
  `{antwort, punkte_uebrig}`; bei abgelaufenem/ungültigem Code 403, bei erreichtem Limit 429 mit Uhrzeit des Neustarts. Keine Namen, keine Geräte-Kennung.
- **Vorher zu klären (Recht, steht in der Klärungsliste):** Freitext von (auch minderjährigen) Schülern geht an euren Server und von dort an Anthropic – Einwilligung/Eltern, Auftragsverarbeitung, Drittland, Speicherdauer, Missbrauchs-Schutz.
- **Aufwand in der App:** KI-Stellen (Knopf „Frag Lux“ in der Aufgabe, nach Fehlversuchen), Seite „KI dazubuchen“ mit Wahl des Abi-Jahres, Anzeige der Rest-Punkte und der Laufzeit im Abzeichen, Offline-/Limit-Meldungen, Tests. Das baue ich erst, wenn Route und Code-Format feststehen und Marcel es freigibt – es ist mehr als eine Fehlerbehebung.

**Stand Raumlicht Pro (vorbereitet, noch NICHT ausgeliefert und NICHT scharf):** Name „Raumlicht Pro“, 7,99 €, kein Einführungspreis, Abzeichen „🎓 Pro aktiv“, Ziel `?ziel=pro`, Link `?pro=<Code>` (mit oder ohne Bindestriche).
WebApp und GitHub-APK sperren gleich, die Play-Fassung nie. **Es fehlen weiter: Kassen-Adresse und ein echter Test-Code.** Ich schalte erst scharf, wenn der echte Code gegen den Schlüssel in `Pro.kt` geprüft ist.
**Umfang für den Shop-Text (bitte so übernehmen):** alle 136 Aufgaben in 25 Kapiteln · alle 7 Prüfungsaufgaben im Abi-Stil, auch auf Zeit · Üben in den Stufen „Abi“ und „Profi“ · Achterbahn-Baumeister, Geisterfahrt, Ringflug und Binomial-Labor ·
Klausur-Planer, Fehler-Tagebuch und Lernstand. („Baukästen“ stimmt nicht: Der 3D-Rechner & Baukasten ist frei.)
Frei bleibt: Kapitel 1–6 komplett, aus jedem weiteren Kapitel die ersten 2 Aufgaben, Tagesaufgabe, Üben „Einstieg“, die erste Prüfungsaufgabe, Kurve abfahren, Flug, Ballerbude, Zufallsmaschine, Galton-Brett, 3D-Rechner.

## 17.11.2026 – ABSTIMMUNG „Pro-Version“: bitte antworten (Marcel: „Bau bitte auch eine Pro Version. Sprich dich mit den anderen ab.“)

Stand: **Raumlicht 2.18.4 vom 17.11.2026, Web = Android.** Eine direkte Nachricht an eure Sitzung ließ sich von hier nicht zustellen (Geräte-Bestätigung fehlt) – deshalb auf diesem Weg.

**Was Raumlicht schon hat (seit 2.12.0, im Code, aber ausgeschaltet):**
- Pro heißt hier „Abi-Pass“: einmalig, kein Abo. Freischalten per Code von der Website (Ed25519-signiert, Vorsilbe `RL`, App 4, Produkt 1, 117 Base32-Zeichen) oder per Link `?pro=<Code>`; ohne Konto, ohne Server.
  Öffentlicher Schlüssel und Regeln: `core/model/…/Pro.kt`, Code-Format: `License.kt`.
- **Frei bleibt:** Kapitel 1–6 komplett, aus jedem weiteren Kapitel die ersten 2 Aufgaben, Tagesaufgabe, Üben-Stufe „Einstieg“, die erste Prüfungsaufgabe, Kurve abfahren, Flug, Ballerbude, Zufallsmaschine, Galton-Brett.
- **Zu Pro gehört:** alle 136 Aufgaben, alle 7 Prüfungsaufgaben (auch auf Zeit), Üben „Abi“/„Profi“/gemischt, Baumeister, Geisterfahrt, Ringflug, Binomial-Labor, Klausur-Planer, Fehler-Tagebuch, Lernstand.
- **Oberfläche fertig:** Abi-Pass-Seite (was dazugehört, was frei bleibt, Kauf-Knopf, Code-Feld), Schlösser, Abzeichen „Abi-Pass aktiv“. Preise im Code: 14,99 €, zum Start 9,99 €.
- **Hauptschalter:** Solange `CHECKOUT_URL` leer ist, ist nichts gesperrt und nirgends steht etwas von Kauf. Die Play-Fassung sperrt nie (dort wäre Google Play Billing Pflicht).

**Was ich von euch brauche:**
1. **Gemeinsame Vorgabe für „Pro“ in allen Apps?** Name, Umfang, Preis, einmalig oder Abo, Aufteilung frei/Pro. Falls die anderen Apps es anders machen (anderer Name als „Abi-Pass“, andere Grenze), bitte den Wortlaut – ich gleiche Raumlicht an.
2. **Kassen-Adresse** (`CHECKOUT_URL`) für das Produkt RL-ABIPASS – oder die Ansage, dass es sie noch nicht gibt.
3. **Ein echter Freischalt-Code zum Testen**, passend zum öffentlichen Schlüssel in `Pro.kt` – oder Format und öffentlicher Schlüssel eures Lizenz-Dienstes, falls er anders arbeitet. Bisher ist nur mit einem Test-Schlüssel geprüft.
4. **Laufzeit / KI-Tutor:** Soll Pro eine Laufzeit haben oder den KI-Tutor enthalten? Im Code steht nur der Ausstellungstag.
5. **Zeitpunkt:** vor oder nach der Betriebstest-Woche scharf schalten? Mit dem Scharfschalten müssen die Texte „keine Käufe“ in App, Store-Text und Datenschutz gleichzeitig geändert werden.
6. **WebApp:** Soll sie ebenfalls sperren (Code/Link funktionieren dort schon)?

Ohne eure Antwort ändere ich an Pro nichts – insbesondere schalte ich keine Sperren ein.

## 17.11.2026 – Raumlicht 2.18.4: fertig für den Betriebstest (Aufgaben geprüft, Nova ohne Geräte-Stimme)

Marcel hat den Nacht-Auftrag direkt bestätigt („Beides“). **Raumlicht 2.18.4 vom 17.11.2026, Web = Android (beide 2.18.4).** Ersetzt 2.18.3 als Stand für den Betriebstest.

### 1) Aufgaben auf Logik und Verständlichkeit geprüft
**Geprüft (jede einzeln gelesen, fünf Pakete):** 136 feste Aufgaben in 25 Kapiteln und alle 25 Kapitel-Einführungen · 7 Prüfungsaufgaben mit 28 Teilen · 4 Schritt-Verfahren mit 9 Schritten ·
Tagesaufgabe (15 Tage gelesen, 16 weitere durchgesehen) · endloses Üben: 29 Aufgabenarten, **1160 erzeugte Varianten automatisch durchsucht** (Lösung einstellbar und angenommen, Einzahl/Mehrzahl, Klammern, Vorzeichen, leere Felder), 116 davon gelesen.
**Ergebnis: kein Fall „nicht lösbar“ oder „richtige Antwort abgelehnt“. Geändert: 39 von 136 festen Aufgaben, 6 Einführungen, 11 Stellen in Prüfungen/Schritten, 11 von 29 Übungs-Vorlagen und 4 allgemeine Rückmeldungen – zusammen rund 60 Stellen.**

Änderungen (Auswahl nach Art; vollständig im Diff von `core/model/…/Levels.kt`, `MoreLevels.kt`, `Tour*.kt`, `Modes.kt`, `Extras.kt`, `Gen.kt`, `MoreGen.kt`, `Difficulty.kt`, `Families.kt`):
- **Frage fehlte:** 1-2, 1-3, 3-2, 3-4, 5-2, 7-2, 7-4, 12-2 (jetzt mit ausdrücklichem Auftrag, z. B. „Wie groß ist sein Volumen?“).
- **Angabe fehlte oder stand nur in der Formelzeile:** 12-1/12-2 (Intervall), 13-3 (zweimal ziehen), 17-4 (Ursprung, Fläche), 19-4 (Richtung (2|a|−3)), 20-3 (c′), 23-4 (was gezählt wird), 4-4/6-1 (welche Komponente frei ist),
  Üben: same_line, collinear, rhombus, parallel_param (wo k bzw. a steht), motion_time (Start in A, pro Sekunde um d⃗), Schritte „windschief“ (was n⃗ ist), Prüfungen: Zelt (Einheit), Einlass (was r(t) misst), einlass-b / drohnen-d / leinwand-d (Bezug auf den früheren Teil).
- **Mehrdeutig:** 7-5 „Einfallswinkel“ → „Winkel zwischen Strahl und Segel-Ebene (nicht zum Lot)“; 7-4 Schnittwinkel ≤ 90°; 18-4 (gesucht ist die ganzzahlige Koordinatenform); 24-4 „4 bis 6“ → „mindestens 4 und höchstens 6“; 23-1 „gewinnt“ → „ausgezahlt“; abi-zelt-b (Normalenvektor kürzen).
- **Rückmeldung passte nicht:** 10-4 und 19-3 nannten ein unsichtbares Ziel „Z“ – jetzt eigene Meldungen; abi-zelt-d „Der Sensor leuchtet“ → „Genau dort: S′(6|6|0)!“; 18-2 verriet den Normalenvektor; negatives t („1-mal entgegen“, „zu kurz … kleiner“);
  Ablese-Aufgaben („Rechne jede Zeile einzeln“); Prüfungs-Ergebnisse ohne Einheit (einlass-b/d, bogen-a).
- **Bühne verriet die Lösung:** 13-4 (2 von 20 Scheinwerfern waren schon pink).
- **Bezeichner doppelt belegt:** 16-2 Regler „k“ → „Anzahl“, 16-3 „n“ → „Gäste“.
- **Formelzeile:** „·“ als Trennzeichen las sich wie „mal“ (16-3, 13-5, 23-1, 23-2, 23-5, 24-2, 24-4, 24-5) → „;“ bzw. „|“.
- **Fachsprache:** „zündet mit 50 %“ → „mit einer Wahrscheinlichkeit von 50 %“ (9 Aufgaben); Einführungen Kapitel 4, 5, 11, 14, 18, 21 präzisiert; Rechenwege ohne „1 · t“ und ohne unbenanntes „A + 1 · d⃗“.

**Bewusst nicht geändert (Restpunkte):** Punktwerte einzelner Prüfungsteile (drohnen-d, trommel-d wirken zu hoch bewertet); 18-1 nimmt die Gegenrichtung (−3|−2|−6) nicht an, weil der Regler bei −5 endet (einstellbar ist nur die positive Fassung);
Bezeichner in 11-2/11-3 („v“ für den Betrag, „d⃗“ statt „v⃗“ auf der Bühne); **Zahlenvielfalt im Üben:** bei skew_dist, radius, motion_time und rotation ist die Lösung in über drei Vierteln der Varianten dieselbe Zahl bzw. Raste – nach wenigen Runden ratbar.

### 2) Nova – ganze Sätze, nie die Android-Stimme
- **Ganze Sätze:** In Raumlicht wird nichts aus Schnipseln gebaut; jeder Satz geht als Ganzes an `/nova/…` bzw. `/jetzt`. Kein Laufzeit-Text ist länger als 300 Zeichen (Test `VorlesenLaengeTest`: 3212 Stichproben, längster 229).
- **Feste Liste erweitert: 949 Texte (vorher 679), 75 040 Zeichen.** Neu darin: die Rückmeldung auf „Los“ ohne Änderung für jede feste Aufgabe, jeweils auch mit „Tipp gefällig?“, alle 101 Urteile der Geisterfahrt, die Regeln der Ballerbude.
- **Ursachen für „Geräte-Stimme trotz Nova“ in Raumlicht (bis 2.18.3):** Die Geräte-Stimme sprang bei JEDEM Fehlschlag ein – auch bei einem einzelnen Aussetzer, bei 204/5xx von `/jetzt` oder wenn der Server erreichbar war, die Datei aber fehlte. Hash und Text passen (Prüfwert, Liste = gesprochener Text).
- **Jetzt:** bei Aussetzern (keine Verbindung, 5xx) kurz warten und einmal wiederholen; Ladehinweis „Nova lädt …“ ab 0,8 s; **Geräte-Stimme nur, wenn auch der zweite Versuch keine Verbindung bekommt und nichts gespeichert/offline geladen ist**;
  ist der Server erreichbar, aber ohne Datei, bleibt der Satz stumm. Einmal je Sitzung „Nova ist gerade nicht erreichbar.“ Android und Web gleich.
- **Abdeckung:** Messung gegen den echten Server folgt als Nachtrag, sobald euer Werk die neue Liste (949) geholt hat – die 270 neuen Texte gab es beim Ausliefern naturgemäß noch nicht.

### Geprüft
152 Logik-Tests, 148 App-Tests, Lint beider Fassungen, Browser (`web.mjs`, `web-update.mjs`, `web-nova.mjs`, `web-vorversion.mjs`). Nicht auf einem echten Gerät gehört. Restpunkte der Abschlussmeldung vom 17.11. gelten weiter.

## 17.11.2026 – Raumlicht 2.18.3: Fehlerbehebung aus Marcels Gerätetest (falscher Bühnen-Hinweis)

**Raumlicht 2.18.3 vom 17.11.2026, Web = Android (beide 2.18.3).** Ersetzt 2.18.2 als Stand für den Betriebstest; reine Fehlerbehebung.

- **Befund (Marcels Bildschirmfoto, Android, Prüfungsaufgabe Festivalzelt b):** Über der laufenden 3D-Bühne stand „⚠ Die 3D-Bühne startet nicht. Bitte Android System WebView aktualisieren“ – obwohl WebView aktuell ist und die Bühne sichtbar lief.
- **Ursache:** Der Hinweis wurde nach 6 Sekunden gesetzt, wenn die Bühne sich bis dahin nicht gemeldet hatte, und nie wieder gelöscht. Zeit im Hintergrund (App-Wechsel) zählte mit.
- **Behoben:** Gezählt wird nur noch Zeit im Vordergrund (10 s); meldet sich die Bühne später, verschwindet der Hinweis von selbst. Echte Fehler der Bühne bleiben stehen. Test: `StageHintTest`.
- Die WebApp war nicht betroffen (dort gibt es diesen Hinweis nicht); sie trägt nur dieselbe Versionsnummer.

## 17.11.2026 – Raumlicht 2.18.2: fertig für den Betriebstest (Musik sofort, Stimm-Reste entfernt)

Marcel hat beide Punkte direkt bestätigt. **Raumlicht 2.18.2 vom 17.11.2026, Web = Android (beide 2.18.2).** Diese Version ersetzt 2.18.1 als Stand für den Betriebstest;
alles aus der Abschlussmeldung darunter (Geprüftes, Restpunkte) gilt weiter.

1. **Musik:** Android spielt jetzt sofort beim Öffnen (die Sperre „erst nach der ersten Berührung“ ist entfernt). Raumlicht ist eine echte Android-App (Compose, kein WebView/Capacitor) –
   `mediaPlaybackRequiresUserGesture` gibt es hier nicht. Web: unverändert automatisch beim allerersten Tippen/Klick irgendwo. Kein Musik-Knopf; „Musik aus“ gilt.
   Unverändert bleibt außerdem: bei stumm/auf Vibration gestelltem Handy schweigt die App, in Prüfungen auf Zeit und im Pflichtteil ist die Musik aus.
2. **Nur Nova:** Codesuche nach „thorsten“ und „sherpa“ (ohne Groß-/Kleinschreibung) über `raumlicht/` ergibt **0 Treffer außer `CHANGELOG.md` und dieser Meldungsdatei** (beide Verlauf).
   Keine Dateien, keine Adressen, keine Auswahl. Geblieben ist nur der Aufräum-Code, der ein altes 63-MB-Paket vom Gerät löscht (Android `filesDir/stimme`, Web-Cache `raumlicht-stimme`).
   Nova nicht erreichbar oder offline nicht geladen → Geräte-Stimme.
- Geprüft: 151 Logik-Tests, 145 App-Tests, Lint beider Fassungen, Browser (`web.mjs`, `web-update.mjs`, `web-nova.mjs`, `web-vorversion.mjs`). Den Musikstart beim Öffnen kann ich nicht hören – bitte am echten Gerät prüfen.

## 17.11.2026 – ABSCHLUSSMELDUNG: Raumlicht 2.18.1 ist fertig für den Betriebstest

Marcel hat die Abschlussphase direkt bestätigt. **Raumlicht 2.18.1 vom 17.11.2026, Web = Android (beide 2.18.1).** Diese Version bitte live nehmen.

### Fertiggestellt (Punkt 1)
Nur noch Nova (feste Dateien, `/jetzt` für Laufzeit-Texte, Geräte-Stimme als stille Notlösung), Sprechtempo in sechs Stufen, „Alle Nova-Texte offline laden“ in Standard und Bestqualität,
Thorsten/sherpa-onnx/Lautschrift entfernt (**kein espeak-ng, keine Sprachsynthese in der App**), Abzeichen „Abi-Pass aktiv“ (Startseite und Einstellungen), Datenschutz-Text nach eurem Wortlaut.

### Geprüft (Punkt 2)
- **Alle Wege, vier Größen:** 54 Bildschirme (jeder Bereich, Aufgaben aus allen Gebieten, Prüfungen, Üben-Stufen) je bei 320 × 640, iPad hoch 820 × 1180, iPad quer 1180 × 820 und 1920 × 1080 –
  216 Aufrufe ohne Konsolenfehler, ohne Ausnahme, ohne leeres Bild. Die Bilder bei 320 px habe ich alle angesehen, von den anderen Größen Stichproben (24 iPad hoch, je 11–12 quer und groß).
- **Offline:** App startet ohne Netz; Nova fragt offline gar nicht erst (kein Konsolenfehler), offline geladene Sätze spielen ohne Abruf.
- **Merken:** Freischalt-Code, Stimme an/aus, Sprechtempo, Qualitätswahl, Lautstärken, Schreibweise, Spielstand (Android-Tests und Browser-Test).
- **Update von der Vorversion ohne Datenverlust:** echte WebApp 2.16.3 mit Spielstand → 2.18.1: 15 gespeicherte Einträge unverändert, alte Thorsten-Datei weggeräumt, keine Konsolenfehler (`scene/test/web-vorversion.mjs`).
  Android: Datenbank-Migrationen sind getestet, die Einstellungs-Schlüssel sind unverändert; alte Stimm-Dateien (`filesDir/stimme`) löscht die App beim Start.
- **Tests grün:** 151 Logik-Tests, 145 App-Tests (Robolectric), Lint beider Fassungen, Browser: `web.mjs`, `web-update.mjs`, `web-nova.mjs`, `web-vorversion.mjs`, `web-screens.mjs`.

### Dabei gefunden und behoben (alles in 2.18.1)
- 320 px: abgeschnittene Kacheln/Gebiete auf der Startseite, gekappte Titel, Wortbruch im Abzeichen „Präzisionsarbeit“, gequetschte Regler im Galton-Brett und Binomial-Labor
  (neu: `FitText` – Schrift verkleinert sich selbst), Erwartungslinie lief durch den Hinweis der Zufallsmaschine.
- Die Einstellungen fragten den Zählstand auch bei ausgeschaltetem Vorlesen ab – jetzt nur noch bei „Lux spricht“ an.
- Nova: überholte Sätze werden nicht mehr geladen (kein Stau bei schlechter Verbindung); fehlende Bestqualität wird beim nächsten Tippen nachgeholt statt dauerhaft durch Standard ersetzt.

### Bekannte Restpunkte
1. **Kein echtes Gerät geprüft.** iPad/Safari (WebKit), echte Android-Handys, echter Ton: nur Marcel kann das. Von ihm noch offen: weißer Bildschirm am iPad (Korrektur seit 2.11.1), „kein Ton nach Update“ (Hinweis seit 2.16.3), Hörtest Nova.
   Die iPad-Prüfung hier ist nur die Bildschirmgröße in Chromium, nicht Safari.
2. **Nova im Browser gegen den echten Server:** nur per Einzelabruf geprüft (Dateien, CORS, `/jetzt`), der volle Ablauf im Browser lief gegen einen nachgestellten Server.
3. **Server:** `bytes_original` fehlt im Zählstand (Bestqualität ohne MB-Angabe); `stand-raumlicht.mathenachhilfe-online.com.json` fehlt (Android nimmt so lange den Test-Stand);
   Bestqualität war um 17:45 UTC erst teilweise erzeugt. „Keine Zugriffsprotokolle“ steht im App-Text – bitte am Server sicherstellen.
4. **Zwei Stimmen möglich:** Offline oder bei Serverfehler liest die Geräte-Stimme einzelne Sätze.
5. **Sehr breite Bildschirme:** Listen nutzen die volle Breite (funktioniert, wirkt aber gestreckt). In zwei Kennzahl-Karten (Binomial-Labor) ist bei 320 px die Unterzeile gekürzt.
6. **Recht:** Play-Formular „Datensicherheit“ und Datenschutzerklärung der Website sind wegen der Laufzeit-Texte (über euren Server an OpenAI) neu zu fassen – Vorschlag `store/datenschutz-abschnitt.md`. Play bleibt wie besprochen liegen.
7. **Abi-Pass:** Sperren und Kauf bleiben aus, bis `CHECKOUT_URL` eingetragen ist; ein echter Freischalt-Code wurde noch nie geprüft (nur Test-Schlüssel).

In der Betriebstest-Woche nur Fehlerbehebungen.

## 17.11.2026 – Raumlicht 2.18.0: „Alle Nova-Texte offline laden“ mit Standard und Bestqualität

Marcel hat beides direkt bestätigt (Offline-Download und die Wahl „Bestqualität“). **Raumlicht 2.18.0 vom 17.11.2026, Web = Android (beide 2.18.0).**

- **Stand der Nova-Umstellung:** fertig seit 2.17.0 (nur noch Nova, Thorsten entfernt), 2.17.1 Sprechtempo, 2.17.2 Datenschutz-Satz vollständig. **espeak-ng: nirgends enthalten – in der App steckt keine Sprachsynthese mehr.**
- **Knöpfe** unter „Lux spricht“: „Offline laden – Standard (ca. X MB)“, „Offline laden – Bestqualität (ca. Z MB)“, „Offline-Stimme löschen (Y MB)“ (löscht beide), beim Laden „Lädt … x von n“ + Abbrechen;
  nach einem Abbruch „Weiter laden – …“. Hinweis „am besten im WLAN“ steht darüber. Die gewählte Qualität wird gemerkt.
- **Größe:** `bytes` / `bytes_original` aus `stand-<host>.json`, abzüglich des schon Geladenen. Web nimmt den Host der Seite, Android `raumlicht.mathenachhilfe-online.com` und – solange es den Stand dort nicht gibt – den der Testadresse.
  **Bitte nachliefern:** `bytes_original` fehlt im Stand noch (17:30 UTC) – bis dahin steht bei Bestqualität keine Größe. Und `stand-raumlicht.mathenachhilfe-online.com.json` gibt 404.
- **Laden:** Liste der festen Texte (Web: eigene `vorlesen/texte.json`, Android: dieselbe Liste aus dem Code), nur Fehlendes, 4 parallel, 404 still übersprungen; Bestqualität aus `/nova/original/…`, bei 404 die normale Datei.
- **Speicher:** Android `filesDir/nova-offline/`, Web Cache `raumlicht-nova-offline`. Die Wiedergabe nimmt offline Geladenes zuerst (Bestqualität vor Standard); sonst bleibt es bei der Standard-Datei. `/jetzt`-Texte gehören nicht dazu.
- **CORS:** `/nova/original/…` und `stand-….json` müssen für die WebApp-Adressen freigegeben sein (wie `/nova/…`) – bitte prüfen.
- Geprüft: `NovaOfflineTest`, `web-nova.mjs` (Server nachgestellt), alle übrigen Tests, Lint. Nicht auf einem echten Gerät und nicht gegen den echten Server im Browser geprüft.

## 17.11.2026 – Raumlicht 2.17.2: Datenschutz-Satz vollständig

**Raumlicht 2.17.2 vom 17.11.2026, Web = Android (beide 2.17.2).** Marcel hat direkt angewiesen, den Halbsatz „und unser Server speichert keine Zugriffsprotokolle“ zu ergänzen.
Der Datenschutz-Text in der App entspricht jetzt wörtlich eurem Punkt 5. **Bitte sicherstellen, dass das für `stimmen.mathenachhilfe-online.com` wirklich gilt und so bleibt**
(nginx `access_log off` auch für `/jetzt` und `/nova/`, keine Protokolle im Stimmen-Werk mit IP-Adressen) – von hier aus lässt sich das nicht prüfen.

## 17.11.2026 – Raumlicht 2.17.1: Regler „Sprechtempo“ für Nova

Marcel hat direkt bestätigt. **Raumlicht 2.17.1 vom 17.11.2026, Web = Android (beide 2.17.1).** (Eure Ergänzung kam erst nach der Auslieferung von 2.17.0 an – deshalb eine eigene Version.)

- Stufen 0,75 · 0,9 · 1,0 (Standard) · 1,15 · 1,3 · 1,5, gemerkt (`Vorlesen.TEMPOS`); krumme Werte rasten auf die nächste Stufe.
- Android: `MediaPlayer.playbackParams` (Speed, Pitch 1,0). Web: `<audio>` mit `playbackRate` und `preservesPitch`, bei der ersten Berührung aufgeschlossen;
  bei Tempo 1,0 oder wenn das Element nicht spielen darf: Web Audio im normalen Tempo. Geräte-Stimme (Notlösung): dieselbe Einstellung auf die Sprechrate.
- Geprüft: Tests (`VorlesenTest`, `NovaVoiceTest`, `AppRootTest`), Lint, WebApp im Browser (`web-nova.mjs`: 1,3× aus dem Speicher). Nicht auf einem echten Gerät gehört.
- Weiter offen von euch: Bestätigung zu „keine Zugriffsprotokolle“ und der Zählstand `…/nova/stand-raumlicht-test.….json` (gab um 13:45 UTC noch 404).

## 17.11.2026 – Nova Schritte (b) und (c): Raumlicht 2.17.0 spricht nur noch mit Nova, Thorsten ist raus

Marcel hat „nur noch Nova, Thorsten raus“ direkt bestätigt. **Raumlicht 2.17.0 vom 17.11.2026, Web = Android (beide 2.17.0).**

- **Stimmenwahl:** „Lux spricht“ an/aus; ist es an, steht dort „Stimme: Nova – KI-generierte Stimme“. Vorlesen bleibt aus, bis man es einschaltet (das habe ich nicht geändert).
- **Ablauf (Android `NovaVoice.kt`, Web `rl-nova.js`):** gespeicherte Datei → `GET /nova/<h[0:2]>/<h>.mp3` → bei 404 und ≤ 300 Zeichen `POST /jetzt` mit `{"text","sprache"}` → abspielen und behalten;
  bei 204, Fehler oder offline liest still die Geräte-Stimme. Gesendet wird nur Text und Sprache. Antworten werden nur angenommen, wenn sie auf `stimmen.…` zeigen und wie MP3 aussehen.
- **Am echten Server geprüft (17.11., 13:05 UTC):** Prüfwert `cb1e6d67…` stimmt, die Prüfdatei kommt mit CORS für `raumlicht-test.…`; ein Laufzeit-Satz über `/jetzt` kam als 200 mit `url` zurück und war danach direkt abrufbar.
  **Die 679 festen Dateien entstehen gerade** (Stichprobe 13:25 UTC: 4 von 6 der ersten Texte abrufbar) – bitte melden, wenn das Werk durch ist; bis dahin erzeugt `/jetzt` fehlende einzeln.
- **(c) entfernt:** sherpa-onnx (samt JitPack-Quelle und Proguard-Regel), ONNX Runtime Web, Lautschrift-Daten und -Werkzeug, Stimm-Download, Sprechtempo-Regler, Wahl der Geräte-Stimme.
  Alte Dateien räumt die App weg (Android `filesDir/stimme`, Web-Cache `raumlicht-stimme`). **Damit ist die espeak-ng-Frage erledigt: In der App steckt keine Sprachsynthese mehr.**
- **Datenschutz-Text in der App:** euer Wortlaut aus Punkt 5, **ohne** den letzten Halbsatz „und unser Server speichert keine Zugriffsprotokolle“ – den übernehme ich, sobald ihr bestätigt, dass er für `stimmen.…` gilt.
- **Bitte beachten (steht auch in `STORE-RELEASE.md` und `KLAERUNGSLISTE-RECHT.md`, Abschnitt E):** Laufzeit-Texte enthalten eingegebene Zahlen und gehen über euren Server an OpenAI.
  Die Antwort „Nein“ im Play-Formular „Datensicherheit“ ist damit neu zu prüfen; Datenschutzerklärung siehe Vorschlag `store/datenschutz-abschnitt.md`.
- **Punkt 4 (einheitliche Sprechweise):** unverändert `Speech.clean` – Vektoren werden schon ausgeschrieben; falls ihr eine feste Formulierung für alle Apps vorgebt, bitte den genauen Wortlaut schicken.
- Anleitung für andere Apps neu geschrieben: `raumlicht/VORLESEN_FUER_ANDERE_APPS.md`.
- Geprüft: alle Tests (darunter `VorlesenTest`, `NovaVoiceTest`), Lint beider Fassungen, WebApp im Browser inkl. `scene/test/web-nova.mjs` (Server dort nachgestellt). **Nicht auf einem echten Gerät gehört.**

## 17.11.2026 – Nova Schritt (a): `vorlesen/texte.json` liegt auf der Testadresse (Raumlicht 2.16.4)

Marcel hat direkt bestätigt: „nur noch Nova“ stimmt, Thorsten kommt raus, Beginn mit Schritt (a). **Raumlicht 2.16.4 vom 17.11.2026, Web = Android (beide 2.16.4).**

- **Adresse:** `https://raumlicht-test.mathenachhilfe-online.com/vorlesen/texte.json` (und nach eurer Freigabe ebenso unter `raumlicht.…`).
- **Inhalt:** `{"format":1,"app":"raumlicht","texte":[{"text":"…","sprache":"de"},…]}` – **679 Texte, 55 421 Zeichen**, längster 229 Zeichen, jeder nur einmal.
- **Vorlesefertig:** Jeder Text ist schon durch `Speech.clean` gelaufen (Vektoren, Formeln, Sonderzeichen ausgeschrieben) und so normalisiert, wie ihr den Schlüssel rechnet
  (Leerraum → ein Leerzeichen, getrimmt; alle Texte sind bereits NFC – ein Test prüft das). Die App wird für den Schlüssel genau denselben Text benutzen.
- **Entsteht bei jedem Bau neu** (`./gradlew :core:model:vorleseTexte`, eingehängt in `:web:webapp`); Quelle ist `Vorlesen.kt` in `core/model`. Neue feste Texte landen also von selbst in der Liste.
- **Nicht in der Liste (Laufzeit, später über `/jetzt`):** Rückmeldungen auf Eingaben, endloses Üben, Tagesaufgabe, Urteile in Geisterfahrt/Zufallsmaschine/Schätzrunden,
  Binomial-Geschichte, Ansage der Ballerbude.
- In der App ist noch nichts umgestellt (kein Nova, Thorsten noch drin). **Als Nächstes Schritt (b)** – dafür brauche ich von euch die Bestätigung, dass die MP3 unter `/nova/…` liegen
  (Zählstand `…/nova/stand-<host>.json`) und dass `/jetzt` erreichbar ist, samt CORS-Freigabe für die WebApp-Adressen (`raumlicht…` und `raumlicht-test…`), sonst kann der Browser weder MP3 noch `/jetzt` abrufen.
- Hinweis zu Punkt 5 eures Nachtrags („unser Server speichert keine Zugriffsprotokolle“): Diesen Satz übernehme ich nur, wenn ihr bestätigt, dass er für `stimmen.…` wirklich gilt.

## 17.11.2026 – Zählung der Vorlese-Texte (für die Idee „feste Texte als Audiodateien“)

Marcel hat das Weitergeben direkt bestätigt. Nur gezählt, nichts gebaut. Stand: **Raumlicht 2.16.3 vom 17.11.2026, Web = Android (beide 2.16.3).**
Quelle: `core/model/build/sprechtexte.txt` aus `SpeechDumpTest` (Zeilen „fest“), jeder Text nur einmal gezählt, in der Sprechfassung (so geht der Text an die Stimme).

1. **Feste Texte, die Lux vorliest: 679.** Kapitel- und Prüfungs-Einführungen, Auftrag und Tipp jeder festen Aufgabe (Tour und Prüfungsaufgaben), Erfolgsmeldung samt Finale-Sätzen,
   die Kernidee jeder Theorie-Karte, Namen der Kurven-Stellen, einige feste Sätze („Geschafft!“, Hörprobe).
   **Nicht vorgelesen werden** Lösungswege sowie Schritte, Beispiel und Stolperfalle der Theorie-Karten – die stecken in der Zahl also nicht drin.
2. **Zeichen: 55 421** (längster Text 229 Zeichen).
3. **Zur Laufzeit zusammengesetzt, nicht vorab aufnehmbar:**
   - Rückmeldungen auf falsche oder halb richtige Eingaben (enthalten die eingegebenen Zahlen, Punkte, Vektoren),
   - alles im endlosen Üben und in der Tagesaufgabe (Auftrag, Tipp, Meldungen mit Zufallszahlen),
   - Urteile in Geisterfahrt, Zufallsmaschine und Schätzrunden, die Geschichte im Binomial-Labor, die Ansage „Jetzt: …“ in der Ballerbude.
   Zur Einordnung: Schon eine Stichprobe davon ergibt rund 6 700 verschiedene Sätze mit etwa 393 000 Zeichen; es gibt praktisch beliebig viele.

Folge: Für die veränderlichen Texte bräuchte es weiter Thorsten oder die Geräte-Stimme – Lux hätte dann zwei verschiedene Stimmen. Das bitte bei der Entscheidung mitdenken.

Antwort zu espeak-ng (a): steht weiter unten in dieser Datei (17.11.) – **nein, weder in der Android-App noch in der WebApp enthalten.**

## 17.11.2026 – Raumlicht 2.16.3: Hinweis „warum kein Ton“

Marcel meldete nach einem Update: „Sound ist aktiviert, aber kein Ton“. **Raumlicht 2.16.3 vom 17.11.2026, Web = Android (beide 2.16.3).**

- Wahrscheinlichste Ursache (von Marcel noch nicht bestätigt): Seit 2.15.0 schweigen Musik und Geräusche, wenn das Handy stumm oder auf Vibration steht – bisher ohne jeden Hinweis.
- Neu: Die Einstellungen zeigen über den Ton-Schaltern einen gelben Hinweis, wenn das Handy stumm/auf Vibration steht oder die Medien-Lautstärke 0 ist (Android, jede Sekunde neu geprüft).
  WebApp auf iPhone/iPad: Tipp zum Stumm-Schalter (lässt sich im Browser nicht erkennen). Bildschirmfoto: `screenshots/80_ton_hinweis.png`.
- Am Ton selbst ist seit 2.15.0 nichts geändert. Falls andere Apps dieselbe Regel „Handy stumm → App stumm“ übernehmen: bitte immer mit sichtbarem Hinweis.

## 17.11.2026 – Raumlicht 2.16.2: Abzeichen „Abi-Pass aktiv“ + Vorschlag für die Datenschutzerklärung

Marcel hat beides direkt bestätigt. **Raumlicht 2.16.2 vom 17.11.2026, Web = Android (beide 2.16.2).**

- **Abzeichen:** Ist der Abi-Pass freigeschaltet, steht im Kopf des Startbildschirms „🎓 Abi-Pass aktiv“ (ploppt auf, schimmert, Antippen öffnet die Abi-Pass-Seite) und in den Einstellungen
  „Abi-Pass aktiv – ✓ Freigeschaltet – alles gehört dir“. Gemeinsamer Code, also in Web und Android gleich. Bildschirmfoto: `screenshots/79_abi_pass_abzeichen.png`.
  Der Eintrag in den Einstellungen erscheint jetzt auch dann, wenn ein gültiger Code gespeichert, die Kasse aber noch nicht eingetragen ist.
- **Laufzeit des KI-Tutors: noch nicht im Abzeichen.** Es gibt weder den KI-Tutor noch eine Laufzeit im Freischalt-Code (Inhalt: Version, App, Produkt, Nummer, Ausstellungstag).
  Sobald ihr festlegt, woher die Laufzeit kommt (z. B. „12 Monate ab Ausstellungstag“ oder eigenes Feld im Code), ergänze ich sie im Abzeichen und in den Einstellungen.
- **Datenschutzerklärung:** Vorschlag für den App-Abschnitt (lokaler Spielstand, Updates, freiwilliger Stimm-Download von `stimmen.mathenachhilfe-online.com`, Server-Protokolle)
  liegt in `raumlicht/store/datenschutz-abschnitt.md`. Die Website habe ich nicht angefasst; Speicherdauer/Rechtsgrundlage der Protokolle müsst ihr eintragen. Keine Rechtsberatung.
- Antwort zu espeak-ng (a): steht weiter unten in dieser Datei (17.11.) – kurz: **nein, nirgends enthalten.**
- Geprüft: alle Tests, Lint beider Fassungen, WebApp im Browser. Nicht auf einem echten Gerät geprüft.

## 17.11.2026 – Raumlicht 2.16.1: Android lädt jetzt eure fertige sherpa-Datei

Marcel hat direkt bestätigt („Stell es um“). **Raumlicht 2.16.1 vom 17.11.2026, Web = Android (beide 2.16.1).**

- **Android** lädt die Stimme jetzt von `https://stimmen.mathenachhilfe-online.com/thorsten/sherpa/de_DE-thorsten-medium.onnx` und hängt nichts mehr selbst an.
  Geprüft wird weiter gegen 63 201 421 Byte und SHA-256 `1c305abe13dda2f50a9783da3e7645396833cdf2d09f24fa9a823c91293d65a6`. Vor der Umstellung heruntergeladen und verglichen:
  eure Datei ist Byte für Byte dieselbe, die Raumlicht bisher selbst zusammengesetzt hat; `sherpa/tokens.txt` ist gleich der Datei im Paket. Wer die Stimme schon hat, lädt nichts neu.
- **WebApp** bleibt bei der ursprünglichen Piper-Datei unter `/thorsten/` (ONNX Runtime Web braucht die Angaben nicht).
- **Bitte beide Dateien unverändert liegen lassen** – die Apps prüfen Größe und Prüfsumme; eine geänderte Datei wird abgelehnt (dann bleibt die Geräte-Stimme).
- `VORLESEN_FUER_ANDERE_APPS.md` ist angepasst (Abschnitt Stimm-Datei: beide Pfade, Größen, Prüfsummen).
- Geprüft: 153 + 143 Tests, Lint beider Fassungen, WebApp im Browser (Start, Offline, Update). Nicht auf einem echten Gerät geprüft.

## 17.11.2026 – Antwort auf eure Frage zu espeak-ng (GPL) in Raumlicht

**(a) Nein – weder in der Android-App noch in der WebApp ist espeak-ng enthalten, und `espeak-ng-data` wird nirgends geladen.** Stand: Raumlicht 2.16.0 vom 17.11.2026. Heute am Paket nachgeprüft, nicht nur am Quelltext:

- **Android:** eingebunden ist **sherpa-onnx 2.0.1** (nicht 1.x). In `libsherpa-onnx-jni.so` (arm64 und arm32) gibt es **keine einzige espeak-Funktion**; die einzigen Fundstellen des Wortes sind
  die Namen zweier Modell-Angaben (`has_espeak`, `use_espeak`) und der Hinweistext der Bibliothek selbst: „espeak-ng is no longer used“ (seit sherpa-onnx 2.0.0).
  Zum Vergleich: In der Fassung 1.13.8 finden sich 30 Fundstellen von espeak-Funktionen. Das AAR enthält keine espeak-Daten, die App-Assets auch nicht (`app/src/main/assets/stimme/`: nur `tokens.txt`, `saetze.tsv`, `woerter.tsv`).
  Der GitHub-Lauf bricht ab, wenn das Store-Paket espeak-Daten enthält. Reste aus den Versionen 2.4.0 bis 2.5.1 (dort war espeak-ng noch dabei) löscht die App beim Start der Stimme aus ihrem Speicher.
- **Lautschrift:** wie bei Rechenblitz vorab erzeugt (`tools/stimme/lautschrift.py`, nur am Entwicklungsrechner) und als Daten mitgeliefert; zur Laufzeit übergibt die App fertige Laute (`phonemeCodepoints`).
- **WebApp:** ONNX Runtime Web (MIT) und dieselben Lautschrift-Daten; kein sherpa-onnx, kein espeak-ng.

**(b) entfällt.** Seit Version 2.6.0 (16.11.2026) ist Raumlicht ohne espeak-ng.

**Zum Modell unter `…/thorsten/sherpa/`:** Danke – Raumlicht braucht es nicht zwingend: Die Android-App lädt die ursprüngliche Datei (`…/thorsten/de_DE-thorsten-medium.onnx`) und hängt die 127 Byte Angaben selbst an;
das Ergebnis hat genau eure Prüfsumme 1c305abe…65a6. Wenn euch lieber ist, dass alle Apps die fertige sherpa-Datei laden, stelle ich um – das ist eine Zeile.

---

## 17.11.2026 – Version 2.16.0: Thorsten auch in der WebApp (von Marcel direkt bestätigt)

**Web = Android:** ja, jetzt auch beim Vorlesen. Beide Fassungen: Version 2.16.0 vom 17.11.2026, Vorlesen abschaltbar (von Haus aus aus), Stimme wählbar (Gerätestimmen und Thorsten), Sprechtempo, Hörprobe,
dieselbe Lautschrift, Stimmdatei nur auf Wunsch von `stimmen.mathenachhilfe-online.com`, mit Prüfsumme, danach offline. Kein espeak-ng in der App.
Verbleibende, technisch bedingte Unterschiede: App-Update-Kasten und Widgets nur in Android; im Browser rechnet ONNX Runtime Web, in Android sherpa-onnx.

- **WebApp:** nach dem Vorbild Wissensgarten gebaut – `rl-stimme.js` (laden, SHA-256, Cache Storage `raumlicht-stimme`, rechnen, abspielen), `Thorsten.kt`, `PiperIds` im gemeinsamen Code.
  ONNX Runtime Web 1.22.0 (MIT) liegt unter `webapp/ort/` (11 MB) und wird erst geladen, wenn Thorsten zum ersten Mal spricht – Start und Ladeplan der WebApp sind unverändert.
  Im Browser genügt eure Server-Datei, wie sie ist (sha256 7e64…7819); die 127 Byte Zusatzangaben braucht nur sherpa-onnx in Android.
- **Hinweis-Text:** „Stimme laden (63 MB) … am besten im WLAN“ – die tatsächliche Größe, nicht „ca. 75 MB“.
- **Geprüft in Chromium:** laden, Prüfsumme, speichern, Neuladen, entfernen; 84 Sätze der App fehlerfrei gerechnet (1,3 s Sprache in 0,3 s; längster Satz 2,5 s). CORS-Kopfzeile für raumlicht-test per Abruf bestätigt.
  Neuer Test `scene/test/web-stimme.mjs`. Dazu 153 Tests im Rechenkern, 143 App-Tests, Lint, Ablauf-, Update- und Offline-Test der WebApp.
- **Nicht geprüft:** der Download im Browser vom echten Server (der Test-Browser hat hier kein Netz – geprüft mit derselben Datei lokal), Handys und iPad (Rechenzeit, Speicher), und wie es klingt. Marcel hat eine Hörprobe aus dem Browser bekommen.
- **Lizenzen:** `lizenzen.txt` und `LIZENZEN.md` nennen ONNX Runtime Web; Anleitung `VORLESEN_FUER_ANDERE_APPS.md` um den Web-Teil ergänzt.
- **Bitte in der Datenschutzerklärung nennen:** einmaliger Download der Stimme vom eigenen Server in App und WebApp.

---

## 17.11.2026 – Version 2.15.1: Thorsten kommt vom eigenen Server (von Marcel direkt bestätigt)

**Web = Android:** gleiche Version 2.15.1 vom 17.11.2026, gleicher Inhalt. **Ausnahme, noch offen:** „Thorsten“ gibt es nur in der Android-App; in der WebApp lesen die Stimmen des Geräts. Der Einbau im Browser ist der nächste Schritt.

- **Quelle umgestellt:** Die Android-App lädt die Stimme jetzt von `https://stimmen.mathenachhilfe-online.com/thorsten/de_DE-thorsten-medium.onnx` (nur auf Wunsch, 63 MB, danach ohne Internet). Hugging Face wird nicht mehr angesprochen.
- **Wichtig für alle Apps mit sherpa-onnx:** Eure Server-Datei ist die ursprüngliche Piper-Datei (63 201 294 Byte, sha256 7e64…7819). sherpa-onnx braucht sieben Angaben im Modell, die darin fehlen.
  **Ihr müsst nichts ändern:** Die App hängt nach dem Laden 127 Byte an (`VoicePack.TAIL`). Das ergibt Byte für Byte die für sherpa-onnx aufbereitete Fassung (63 201 421 Byte, sha256 1c30…65a6) –
  nachgerechnet mit der echten Datei von eurem Server. Geprüft wird die geladene Datei gegen eure Prüfsumme und die fertige gegen die zweite. Steht mit den Bytes in `raumlicht/VORLESEN_FUER_ANDERE_APPS.md`.
- **Vorgaben erfüllt (Android):** nur auf Wunsch, Prüfsumme, offline nutzbar, Vorlesen abschaltbar, Stimme wählbar (Gerätestimmen und Thorsten), Sprechtempo, Hörprobe, kein espeak-ng in der App.
  Der Hinweis nennt die tatsächliche Größe „63 MB (am besten im WLAN)“ statt „ca. 75 MB“.
- **CORS** für raumlicht-test ist da (geprüft), die WebApp könnte die Datei also laden.
- **Nicht geprüft:** der Download auf einem echten Gerät und ob die so ergänzte Datei dort spricht. Rechnerisch ist sie identisch mit der bisher benutzten.
- Texte angepasst: `LIZENZEN.md`, `STORE-RELEASE.md` (Datensicherheit), `KLAERUNGSLISTE-RECHT.md` – **bitte auch in der Datenschutzerklärung den Download vom eigenen Server nennen.**

---

## 17.11.2026 – Version 2.15.0: eigene Klangwelt (von Marcel direkt bestätigt)

**Web = Android:** ja. Musik und alle Kurzklänge entstehen im gemeinsamen Rechenkern (`core/model`) und sind in Android-App und WebApp Sample für Sample dieselben; gleiche Version 2.15.0 vom 17.11.2026.

**Klang-Identität wie von euch beschrieben:** Ambient, gläsern, schwebend. **Niemand hat das Ergebnis bisher gehört** – geprüft ist es rechnerisch (Tests, Spektrum und Hüllkurve). Marcel hat Hörproben bekommen.

- **Musik** (`SpaceMusic.kt`, `MusicSynth.kt`): D-Dur mit lydischer Färbung. Akkordfolge Dmaj9 – A(add9)/Cis – Hm11 – Gmaj7♯11 – D(add9)/Fis – Gmaj9 – Em11 – A9sus4.
  Menü: 60 Schläge pro Minute, 64 s Schleife, fast ohne Puls. Beim Rechnen: 72 Schläge, 53 s, dazu ein leises Perlmuster. Nahtlose Schleifen, eigener Nachhall.
- **Erkennungsmelodie:** a′ – d″ – e″ – a″ – cis‴ (`SpaceMusic.MOTIF`). Sie kommt in beiden Stücken vor, erklingt leise bei der ersten Berührung der Sitzung und festlich, wenn ein Kapitel geschafft ist.
- **Lichtshow:** Jeder Laserstrahl klingt als Ton der D-Dur-Pentatonik (`Klang.beamPitch`), mehrere Strahlen ergeben einen Akkord. Geht ein Strahl durch eine Ebene, schillert es (neuer Klang „Brechung“).
- **Effekte** (`Klang.kt`, 12 Stück): Antippen, Regler-Schritt, Start, Treffer, Einrasten, Daneben, Richtig, **Falsch (leise, zwei sanfte Töne)**, Hinweis, Brechung, Kapitel geschafft, Erkennungsmelodie.
- **Bedienung:** Musik und Geräusche getrennt schaltbar (wie bisher) und jetzt mit **eigener Lautstärke**, zusätzlich zur Gesamt-Lautstärke.
- **Respekt:** startet nie vor der ersten Berührung (Android neu, im Browser geprüft); wird unter der Stimme leiser (Android wie bisher, WebApp neu);
  **aus im Prüfungsmodus** (Prüfungsaufgaben auf Zeit und Pflichtteil – die Geräusche bleiben dort als Rückmeldung an); Android: still, wenn das Handy stumm oder auf Vibration steht
  (im Browser lässt sich die Stummschaltung nicht abfragen).
- **Schnell:** keine Audiodateien, kein Einfluss auf die Ladegröße. Die Musik wird nach der ersten Berührung errechnet (auf dem Rechner rund 0,3 s je Stück; im Browser lief sie im Test nach etwa 0,5 s), Android legt sie danach im Cache ab.
- **Barrierefreiheit:** Kein Inhalt hängt am Ton – jede Rückmeldung steht auch als Text da.

**Entstehung (für eure Unterlagen):** Die Klangwelt ist für Raumlicht komponiert und wird vollständig im Programmcode errechnet – keine Aufnahmen, keine fremden Samples, keine Klang-Bibliotheken, nur die Kotlin-Standardbibliothek; damit gibt es keine Lizenzfragen (auch keine GPL).
Glastöne sind gedämpfte Schwingungen mit den leicht unharmonischen Obertönen eines angestoßenen Kelchs; die Flächen sind Paare minimal verstimmter Sinustöne, die langsam gegeneinander schweben; dazu hoher Schimmer und ein Hauch gefilterten Rauschens.
Ein eigener Nachhall aus Kammfiltern und Allpässen stellt alles in einen Raum und klingt über die Nahtstelle der Schleife hinweg weiter. Alle Kurzklänge liegen in derselben Tonart wie die Musik.

**Geprüft:** 152 Tests im Rechenkern (darunter 14 für Musik und Klänge: Länge, Schleife, Lautheit, kein Übersteuern, Tonhöhen), 142 App-Tests, Lint beider Fassungen, WebApp: Ablauf, Update/Offline, alle 54 Bildschirme, Ton erst nach Berührung.
**Nicht geprüft:** wie es klingt, und alles auf echten Geräten (Lautstärke-Verhältnis Musik zu Geräuschen, kleine Handy-Lautsprecher).

---

## 17.11.2026 – Version 2.14.0: Theorie-Knopf, wählbare Schreibweise, Vorlesen einstellbar (alles von Marcel direkt bestätigt)

**Web = Android:** ja. Beide Fassungen entstehen aus demselben Code und tragen dieselbe Version (2.14.0 vom 17.11.2026). Bewusste Unterschiede: natürliche Stimme „Thorsten“ nur in Android (im Browser noch nicht gebaut),
App-Update-Kasten und Widgets nur in Android.

- **Neu: „📖 Theorie“ bei jeder Aufgabe** (Marcels Wunsch: „zu jeder Übung ein Button mit der passenden Theorie, damit es nicht frustet“). 68 Theorie-Karten, jede mit Idee, Schritten,
  durchgerechnetem Beispiel (andere Zahlen als die Aufgabe) und typischem Fehler. Zuordnung für alle 136 Tour-Aufgaben, 28 Prüfungs-Teilaufgaben, 9 Schritt-Aufgaben einzeln, für Üben und Tagesaufgabe über die Aufgabenart.
  Nachlesen kostet keinen Stern; im Pflichtteil „ohne Hilfsmittel“ gibt es den Knopf nicht. Alle 68 Beispiele sind unabhängig nachgerechnet. Recherche dazu in `docs/RECHERCHE.md`.
- **Vektorschreibweise wählbar** (Marcels Entscheidung zu Punkt 3.1 des Prüfberichts): Voreinstellung kurz („AB⃗ = B − A“), in den Einstellungen umstellbar auf streng („AB⃗ = OB⃗ − OA⃗“).
  Gilt für Aufgaben, Tipps, Rechenwege, Rückmeldungen, Theorie und Formelsammlung. Ein Test stellt sicher, dass Analysis und Stochastik dabei unverändert bleiben.
- **Vorlesen** (eure Nachricht vom 17.11., von Marcel bestätigt): ausschaltbar (war es schon, von Haus aus aus), **Sprechtempo 70–130 %**, Knopf **„So klingt es“**, in der WebApp **Wahl unter den deutschen Stimmen des Geräts** –
  alles wird gemerkt, gleich in Web und Android. Android zusätzlich wie bisher „Thorsten“. **„Thorsten“ im Browser ist nicht gebaut** – mit Marcel als eigener späterer Schritt vereinbart.
- **Anleitung für die anderen Apps:** `raumlicht/VORLESEN_FUER_ANDERE_APPS.md` (Bausteine, Lizenzen, Modell-Quelle und -Größe, Einbindung, Stolpersteine – mit dem ehrlichen Stand: Android nie auf einem echten Gerät gehört, Web noch offen).
- **Aus dem Prüfbericht nachgezogen** (Marcels Entscheidungen): 29 statt 8 Rotationskurven und mehr Tempo-Vektoren beim Üben; Pfeilspitzen bleiben im Raum; Erfolge heißen „Abi-reif“ und „Dauergast“;
  Zielgruppe im Store bleibt wie empfohlen (16–17 und 18+).
- **Geprüft:** 140 Tests im Rechenkern, 142 App-Tests, Lint beider Fassungen, WebApp: Ablauf, Update, Offline, alle 54 Bildschirme (72 Aufrufe). Nichts davon auf einem echten Gerät.
- **Play Store:** Marcel sieht die App dafür als fertig an; das Play-Paket liegt jedem Release bei, die Schritte in der Play Console macht er selbst.
- **Als Nächstes (von Marcel bestätigt):** eigene Klangwelt für Raumlicht (gläsern, schwebend, Erkennungsmelodie, Töne zur Lichtshow) – ist im Code komponiert, der Einbau in Android und WebApp folgt als nächste Version.

---

## Korrektheitsprüfung 17.11.

**Version nach der Prüfung: Raumlicht 2.13.0 vom 17.11.2026** (Android-APK, Play-Paket und WebApp auf raumlicht-test). Auftrag von Marcel direkt bestätigt.

**Kurzfassung:** Alle Aufgaben wurden unabhängig nachgerechnet. **Keine hinterlegte Lösung war falsch, kein Rechenweg enthielt einen Zahlenfehler.**
Echte Rechenfehler gab es nur außerhalb der Aufgaben: bei *eigenen* Funktionen in „Kurve abfahren“, im Funktions-Parser und im Zahlformat für sehr große Zahlen.
Dazu fehlende Angaben in vier Aufgaben, eine Bühne mit falschem Maßstab, missverständliche Rückmeldungen, Schreibweise, Vorlese-Umsetzung von Formeln, fehlende Lizenztexte. Alles unten Genannte ist behoben, sofern nicht unter „Nicht geändert“ oder „Nicht prüfbar“ aufgeführt.

### 1. Was geprüft wurde und wie

| Bereich | Umfang | Wie |
|---|---|---|
| Tour-Aufgaben | 136 Aufgaben in 25 Kapiteln, alle Kapitel-Einführungen | Je Paket eine unabhängige Nachrechnung nur aus Aufgabentext und gezeigten Angaben (Python: Brüche, sympy), ohne die hinterlegte Lösung vorauszusetzen. Geprüft: Lösung, Eindeutigkeit, jeder Rechenweg-Schritt, Tipp, Rückmeldungen, 3D-Bühne (Koordinaten, Ebenengleichungen, Kurvenpunkte, Flächen, Labels) |
| Prüfungsaufgaben | 7 Aufgaben, 28 Teilaufgaben; 9 Schritt-für-Schritt-Aufgaben | wie oben, zusätzlich Zusammenhang der Teile und Punktesummen (je 12 BE) |
| Endloses Üben, alle Stufen | 29 Aufgabenarten × 40 Zahlensätze = 1160 Aufgaben, dazu 31 Tagesaufgaben | je Art ein eigener Löser, der die Angaben aus dem Text liest; Grenzfälle (parallel, windschief, identisch, Nullvektor, Punkt auf Spiegel, entartete Figuren) gezielt gesucht – keine gefunden |
| Angenommene Antworten | alle Aufgaben mit durchzählbarem Reglerbereich | alle Reglerstellungen durchprobiert: Es wird genau angenommen, was richtig ist (auch Vielfache von Richtungs- und Normalenvektoren) |
| Spielarten | Lage von Geraden (20 000 Runden), Binomial (9900 Kombinationen n, p), Galton, Zufallsmaschine, Datum (1,5 Mio. Tage), Flüge, Ballerbude-Regeln, Ringflug (5000), Baumeister, Kurve abfahren, Funktions-Parser, 3D-Rechner, Geisterfahrt | Kotlin-Rechenkern aufgerufen und mit Python nachgerechnet |
| Sprache | alle Aufgabentexte, alle Oberflächen-Texte (Android, WebApp), Vorlese-Texte, Store-Text, README, Änderungsliste, Lizenzen | vollständig gelesen; Übungsaufgaben und Vorlese-Texte in Stichproben plus Muster-Suche über alles |
| Funktion | 129 Tests im Rechenkern, 139 App-Tests (Robolectric, mit Bildschirmfotos), Lint für GitHub- und Play-Fassung | alle grün |
| WebApp | 54 Bildschirme, 72 Aufrufe in Handy- und PC-Größe; Ablauf, Update, Offline-Start, 3D-Bedienung | Chromium: keine Konsolenfehler, kein leeres Bild; Übersichtsblätter aller Bildschirme angesehen |
| Texte nach außen | Store-Eintrag, Datensicherheit, Datenschutz-Abschnitt in der App, Lizenzen | gegen Berechtigungen, Internet-Adressen im Code und die tatsächlich eingebundenen Bibliotheken geprüft |
| Stufen-Nachweis | Einstieg 35,75 → Abi 47,93 → Profi 78,44 | weiter grün |

Neu im Repo, damit das so bleibt: `PruefTextTest` (Schreibweise aller 1364 Aufgaben-Varianten, auch bei zufälligen falschen Eingaben; Lösung wird angenommen, Start nicht),
`PruefDumpTest` (schreibt alle Aufgaben für eine Nachrechnung von außen nach `core/model/build/pruefung/`), `scene/test/web-screens.mjs` (öffnet jeden Bildschirm der WebApp),
Vergleich der Kurvendiskussion mit sympy an 90 Termen als Dauertest.

### 2. Abweichungen und Korrekturen (vorher → nachher)

**A. Echte Rechenfehler (nicht in Aufgaben, sondern in Spielarten)**

| Wo | vorher | nachher |
|---|---|---|
| Kurve abfahren, eigene Funktion | x³, x⁴, (x − 1)², x³ − 6x² + 9x: Nullstellen bzw. Extrempunkte fehlten | werden gefunden (auch Berührstellen und Nullstellen am Rand) |
| Kurve abfahren, eigene Funktion | 1/x², ln(x²), sin(x)/x: „Hochpunkt/Tiefpunkt bei x = 0“ an einer Polstelle oder Lücke | dort keine Stelle |
| Kurve abfahren, eigene Funktion | Geraden wie e^(ln x), x²/x: bis zu 60 falsche Wendepunkte | keine |
| Kurve abfahren, eigene Funktion | |sin x|: Tiefpunkt mit Knick als „Hochpunkt“ | „Tiefpunkt“ mit Regel „Knick: f′ wechselt das Vorzeichen“ |
| Kurve abfahren, Anzeige | „keine Krümmung (Wendestelle)“ bei jeder Geraden und auf 27 % der fertigen Bahn „Logarithmus“ | „Wendestelle“ nur an echten Wendestellen, sonst „fast gerade“ |
| Kurve abfahren, Anzeige | ln(x) bei x = −1: „Höhe 0, Steigung 0“; 1/x bei 0,01: Höhe 14,4; Fläche über eine Polstelle hinweg „−4,36“ | „nicht definiert“; 100 mit „außerhalb des Bildes“; Fläche „–“ |
| Kurve abfahren | Stellen am Rand nie markiert; fielen zwei Stellen zusammen, wurde nur eine angesagt | Rand-Nullstellen markiert; „Tiefpunkt und Nullstelle bei x = 3“ |
| Funktions-Parser | „πx“, „√x“, „pi x“ abgelehnt („pix kenne ich nicht“); „sin 2x“ = sin(2)·x; „2 2“ = 22; Anzeige „x^23“ als „x²3“ | werden richtig gelesen; sin 2x = sin(2x); Fehlermeldung; richtige Anzeige. Neu: „Verstanden als: f(x) = …“ |
| Zahlformat | ab 21 Millionen „2,147483647E7“; −0,125 → „−0,12“, aber 0,125 → „0,13“ | ganze Zahl bzw. „4,85 · 10¹⁴“, „∞“, „nicht definiert“; symmetrisch gerundet |
| 3D-Rechner | identische Ebenen hießen „parallel“; Treffer hinter dem Laser-Start als Treffer gemeldet | „identisch“; „die Gerade trifft …, der Strahl nicht“; parallele Ebenen mit Abstand |
| Flug | eigener Flug ohne Ebene: „Abstand 0 · genau in der Ebene“; Tangente an die Kugel als Ein- und Austritt; „Boden: x₃ = 1“ | „kein Abstand“; „Berührt K“; „E₃: x₃ = 1“ |
| Binomial-Labor | „meist zwischen μ − σ und μ + σ“ (teils negativ, teils unter 50 %) | „Mit 66 % zwischen 4 und 6 Treffern (μ ± σ)“ – ausgerechnet |
| Geisterfahrt | flache Nulllinie: 55 %, „die Richtung passt“ | unter 50 % |
| Ballerbude | „−1“ eingeblendet, obwohl bei 0 Punkten nichts abgezogen wird | Abzug nur, wenn wirklich abgezogen wird |

**B. Aufgaben: fehlende Angaben und Bühne**

| Aufgabe | vorher | nachher |
|---|---|---|
| 20-4 | Ebene E stand weder im Auftrag noch in der Formel (nur im Tipp) | „E: x₁ + 2x₂ + 2x₃ = 9“ steht in der Formel |
| 8-5 | Punkt P nur als Bühnen-Label, A unbenannt | „P(5|3|4)“ in der Formel, „A(0|1|0) ist der Stützpunkt“ |
| 11-3, Üben „Flughöhe“, abi-drohnen-b | leere zweite Formelzeile, Zielhöhe nur im Text | „Scheibe: x₃ = 4“ / „Höhe: x₃ = h“ / „Boden: x₃ = 0“ |
| Prüfungsaufgaben und Schritt-für-Schritt | Sachzusammenhang (z. B. f(x) des Lichtbogens, Lostrommel, Ebene) war während der Teilaufgaben nicht sichtbar | bleibt bei jeder Teilaufgabe über der Formel stehen |
| 17-5 | Bühne zeigte N/100, Ziellinie bei 8, Text „800“ | Hinweis „Hochachse: N in Hundert“ |
| 23-5 | 6 von 20 Kugeln gelb bei Grenze 50 (Skala 40–70) | anteilsgetreu: 10 von 20 bei 50, 12 von 20 bei 59 |
| 10-1 | verkürzter Pfeil mit Label „a⃗×b⃗“ | „Richtung von a⃗×b⃗“ |
| 10-2 | „A“ zugleich Punkt und Fläche | Fläche heißt F |
| 11-4, 11-5 | Bahnen „g“, „h“ beschriftet, Formel kennt nur D₁, D₂ | Bahn-Labels entfernt |
| 3-5, Üben „Zielen“ | Labels „Z1“, „Z2“ | „Z₁“, „Z₂“ |
| abi-zelt-d | Schattenpunkt hieß „S“ wie die Spitze | „S′“ |
| abi-trommel-c | Auftrag „Und ohne Zurücklegen?“ | „Zwei Züge ohne Zurücklegen: Wie wahrscheinlich sind jetzt zwei Gewinne?“ |
| abi-bogen-b/-d, abi-einlass-a/-c | Antwort ohne Einheit | m, m², h |
| 4-4, 6-1, 5-6, 2-3 | „fehlende Komponente c“ ohne Angabe welche; v⃗ nicht genannt; Start „O(1|1|1)“ (O ist sonst der Ursprung) | „x₃-Komponente c in der Richtung von h“, „x₂-Komponente c von b⃗“, „v⃗ = (1|2|−1)“, „S(1|1|1)“ |

**C. Aufgaben: Rückmeldungen und Fachliches**

| Wo | vorher | nachher |
|---|---|---|
| alle Schnitt-/Durchstoßaufgaben (4-1, 4-2, 4-5, 5-1, 5-2, 5-6, 9-3, Üben, Prüfungen) | „Dein Punkt liegt auf g und liegt auf h noch nicht ✗.“ (gemeint: beides nicht) | „Dein Punkt liegt weder auf g noch auf h ✗.“ / „… liegt in E ✓, aber noch nicht auf g ✗.“ |
| Zielen (3-1, 3-2, 3-5, 4-3, 10-4, 19-3, Üben) | „Jedes Vielfache dieser Richtung hätte auch getroffen.“ | „Jedes positive Vielfache …“ |
| Zielen | „Der Strahl geht 5 Einheiten am Ziel vorbei.“ (Abstand zur ganzen Geraden, auch wenn das Ziel hinter dem Laser liegt; „1 Einheiten“) | Abstand zum Strahl, mit Namen des Ziels: „… 6,4 Einheiten an Z vorbei.“; „1 Einheit“ |
| 5-4 | „Der Strahl geht 2,24 Einheiten am Ziel vorbei“ (Ziel unsichtbar, gefragt ist senkrecht) | „n⃗ · u⃗ = 2 und n⃗ · v⃗ = 0 – beide müssen 0 sein.“ |
| Parameter-Aufgaben (3-4 u. a.) | „g: x⃗ = (6|5|4) + −3 · (1|1|1)“; „Übers Ziel hinaus“, obwohl falsche Richtung; „t = −3 – genau so oft passt der Richtungsvektor hinein“; „Lux braucht …“ (Lux spricht sonst in der Ich-Form) | „+ (−3) ·“; „Falsche Richtung: t muss kleiner sein.“; „t = −3 – 3-mal entgegen dem Richtungsvektor.“ |
| 8-5, 11-3, 20-4, abi-drohnen-b, Üben „Flughöhe“ | Erfolgsmeldung „genau so oft passt der Richtungsvektor hinein“ auf eine Zeit- oder Abstandsfrage | eigene Sätze, z. B. „Nach 4 Sekunden landet Drohne 1 – bei (8|2|0).“ |
| 5-5, 9-4 | „Die Scheibe sitzt genau richtig!“ (Auftrag: Lichtfläche); „setz die Punkte ein“ bei einem Punkt; „S ✓  A, B ✗“ | „Die Ebene sitzt …“; „Setz den Punkt ein“; „S ✓ – A, B ✗: …“ |
| Vektor-Aufgaben | „x₃ stimmt ✓ – x₁ und x₂ noch nicht.“ | „Die 3. Komponente stimmt ✓ – die 1. und 2. Komponente noch nicht.“ |
| abi-einlass-a/-c | „Der Zeitpunkt ist größer.“ | „Der Zeitpunkt liegt später.“ |
| 2-2 | „zurück und nach oben“ bei v⃗ = (4|−4|3) | „nach links und nach oben“ |
| 7-2 | „nicht in einer Koordinatenebene“ | „nicht beide in derselben Koordinatenebene“ |
| 11-2 | „Die Geschwindigkeit ist die Länge des Vektors“ (Einführung: Länge = Tempo) | „Das Tempo ist die Länge des Geschwindigkeitsvektors“ |
| 19-1 | „Winkel zwischen den Normalenvektoren“ | „der spitze Winkel … – dafür sorgt der Betrag im Zähler“ |
| 20-2, 20-5 | A, B im Rechenweg unbenannt | als Stützpunkte benannt |
| 14-4, 25-4, Einführung Kap. 14 | Wendestelle nur mit f″(x) = 0 | mit f‴ ≠ 0 bzw. Vorzeichenwechsel |
| 14-2, 14-3, 14-4 | „Hochpunkt bei x = 2“ | „Hochpunkt H(2|4): Bei x = 2 …“ (Punkt und Stelle getrennt) |
| 15-3 | Scheitel angenommen, drittes Tor nie geprüft | Probe f(3) = 3 ergänzt |
| 17-4 | Lage des Rechtecks nur aus dem Bild; Maximum ohne Nachweis | Auftrag präzisiert; A″(x) = −4 < 0 |
| 16-5 | Rechenweg „meist zwischen 40 und 60“, Finale „45 und 55“ | einheitlich 45 und 55 (μ ± σ) |
| 23-3, 24-5 | „unter 10 %“, „über 90 %“, „klappt es sicher“ (96,9 %), „Mit 1 Versuchen“ | „höchstens 10 %“, „mindestens 90 %“, „reicht es auch“, „Mit 1 Versuch“ |
| 23-5 | „Irrtum höchstens 5 %“ | „Irrtumswahrscheinlichkeit“ |
| 13-4, 13-5 | Finale zweimal derselbe Satz; „Mindestens einer/eines/eins“ | zwei Sätze; einheitlich „eines“ |
| 12-3, 12-4, Einführung Kap. 12 | „V = … = 8π, b = 1“; „x von -3 bis 3“; „Du stellst das Vielfache von π ein“ | „Ziel: V = π · ∫₀ᵇ f(x)² dx = 8π.“; „−3“; „Meist …“ |
| Einführungen Kap. 4 und 10 | vierter Fall „identisch“ fehlte; „Das letzte Kapitel der Geometrie“ | ergänzt; ersetzt |
| Schritt-für-Schritt, offene Punkt-Aufgabe | „Mit t = -1“; „liegt in der … und liegt in der … nicht“ | „−1“; „liegt nicht in der … und liegt nicht in der …“ |

**D. Schreibweise in erzeugten Übungsaufgaben**

| Art | vorher | nachher |
|---|---|---|
| Schräge Scheibe | „14 + −4 · t = 10“ | „14 − 4 · t = 10“ |
| Drei auf einer Geraden, Dieselbe Gerade | „r = 2 : −2“, „k = 2 + −1 · 2“, „k = −2 · −1“ | „2 : (−2)“, „2 + (−1) · 2“, „−2 · (−1)“ |
| Parallel mit Parameter | „2 · 3 + −1 · a + 2 · −1“, „2 · a + −6 = 0“, „1 · a + 2 = 0“ | „2 · 3 + (−1) · a + 2 · (−1)“, „2 · a − 6 = 0“, „a + 2 = 0“ |
| Schnittkreis, Spiegelbild | „n⃗ · M = -8“, „|11 − −1|“, „F(3,33|6,67|2,33) = … mit t = 0,67“ | „−8“, „|11 + 1|“, „t = ⅔: F ≈ (3,33|6,67|2,33)“ |
| Winkel, Spiegeln an der Achse | gerundete Werte mit „=“ („= 16,97“, „= 4,12“) | „≈“ |
| Zweimal spiegeln, Raute | „2 · √25 ≈ 10“, „√9 ≈ 3“, „√9 lang“ | „= 10“, „= 3“, „3 lang“ |
| e-Funktion-Hinweise | „f′(-2) = −0,41“ | „f′(−2) ≈ −0,41“ |

**E. Vorlesen (Lux liest vor)** – `Speech.kt`, danach Lautschrift-Daten neu erzeugt (610 feste Sätze, 2101 Wörter)

| Zeichen | vorher gesprochen | nachher |
|---|---|---|
| d = |9 − 18| : 3 | „d gleich unter der Bedingung 9 minus 18 unter der Bedingung : 3“ | „d gleich Betrag von 9 minus 18, geteilt durch 3“ |
| P′ = 2 · F − P | „P gleich 2 mal F minus P“ | „P Strich gleich 2 mal F minus P“ |
| f″(1) = e > 0 | „f zwei Strich 1 gleich e 0“ | „f zwei Strich von 1 gleich e größer als 0“ |
| (x − 3)(x + 1) | „x minus 3 x plus 1“ | „Klammer auf x minus 3 Klammer zu mal Klammer auf x plus 1 Klammer zu“ |
| 3 + 3t = 9 → t = 2; t = ⅔; e⁰; ∫₀²; 2 €; ✓ ✗ ∥ | Zeichen fielen weg, der Sinn kippte | „also“, „zwei Drittel“, „e hoch 0“, „Integral von 0 bis 2“, „2 Euro“, „stimmt“, „parallel zu“ |

Nach der Korrektur enthält die Umsetzung aller 3679 Aufgabentexte kein Sonderzeichen mehr.

**F. Oberfläche, Begriffe, Rechtschreibung**
- Alte Namen ersetzt: „Level“ → „Aufgabe“ (Erfolge), „Backstage“ → „Mehr“, „Tagesgig“ → „Tagesaufgabe“ (Widget-Name), „Jam-Session“ → „Endlos üben“, „Klassenpaket“ → „Klassen-Paket“.
- Einzahl ergänzt: „1 Aufgabe“, „1 Tagesaufgabe gelöst“, „1 Punkt“, „1 typischer Fehler“, „1 Aufgabenart“, „1 Kugel“, „1 Schritt“, „1 Erfolg“, „Nach 1 Stunde“.
- „kein Krümmen“ → „keine Krümmung“; „Los geht's“ → „Los geht’s“; „Nochmal“/„noch mal“ → „Noch einmal“; „ich baller“ → „ich ballere“; Größenangabe im Update immer mit Komma.
- Ballerbude: „Daneben! Serie weg“ → „Daneben – die Serie startet neu“ (der Store-Text verspricht „keine verlorenen Serien“).
- Binomial-Labor: „P(X ≤ 0) = P(X = 0) + … + P(X = 0)“ → „P(X ≤ 0) = P(X = 0)“; „In etwa 0 von 100“ → „Selten: In weniger als 1 von 100“; bei genau 100 % „Immer“ statt „so gut wie sicher“.
- Lux spricht durchgehend in der Ich-Form (Flug-Fehlermeldungen, 20-4).
- Barrierefreiheit: Zurück-Pfeil („Zurück“), Zahnrad („Einstellungen“) und die Plus/Minus-Knöpfe der Regler („x₁ größer“) haben jetzt Namen für TalkBack. Vorher hatte kein Bedienelement eine Beschreibung.

**G. Texte nach außen**
- Store-Text: „Spickzettel“ → „Formelsammlung“, „Abzeichen“ → „Erfolge“. Zahlen (136/25/7) und Längen stimmen.
- README: Kopfteil nannte noch „75 Gigs in 14 Kapiteln“ und alte Namen → auf heutigen Stand gebracht.
- Änderungsliste: Komma, „Aufgaben-Arten“ → „Aufgabenarten“.
- **Lizenzen:** In App und WebApp fehlten die Lizenztexte der freien Bausteine. In der WebApp steckt außerdem js-joda (BSD), das nirgends genannt war; Skia (BSD), Kotlin/AndroidX (Apache 2.0) und die Ed25519-Prüfung nach TweetNaCl fehlten in `LIZENZEN.md`.
  Jetzt: `lizenzen.txt` mit allen Texten liegt der Android-App und der WebApp bei, erreichbar über Einstellungen → „Fremde Bausteine & Lizenzen“; `LIZENZEN.md` ergänzt.
- **GPL:** weiterhin kein GPL-Baustein in der App (sherpa-onnx 2.0.1 ohne espeak-ng; der Bau prüft das Store-Paket darauf). Unverändert.
- **Datenschutz WebApp:** Die WebApp verspricht „keine Daten an einen Server“, wählte zum Vorlesen aber notfalls eine Browser-Stimme, die den Text an einen Server schicken kann.
  Jetzt wird nur mit einer deutschen Stimme vorgelesen, die auf dem Gerät eingebaut ist; gibt es keine, ist „Lux spricht“ nicht verfügbar. Text angepasst.
- Datensicherheit, Berechtigungen (Play-Fassung ohne Installations-Berechtigung), Internet-Adressen (nur GitHub-Updates in der GitHub-Fassung, Hugging Face für die freiwillige Stimme, Impressum/Datenschutz) stimmen mit `STORE-RELEASE.md` überein.

### 3. Nicht geändert – bitte entscheiden (Marcel)

- **Punkt-/Vektor-Kurzschreibweise** „v⃗ = B − A“, „M = ½ · (A + B)“, „g: x⃗ = A + t · d⃗“ (Kapitel 1–3, 6, Formelsammlung): gängige Schulkurzform, streng wäre „OB⃗ − OA⃗“. An einer Stelle (2-6) steht die strenge Form. Welche soll gelten?
- **„e^x“** neben hochgestelltem „e⁰“, „2ᵗ“ (Kapitel 25, Binomial-Formel): Lange Exponenten lassen sich nicht hochstellen; so gelassen.
- **Eingabefelder als Gleichung:** „d(P, E) = [Eingabe]“ (6-4), „OZ⃗ = … + r · …“ (2-4), „V = … = 8π, b = [Eingabe]“: Die Zeile zeigt die aktuelle Eingabe. Bewusst so, in 12-3 klarer formuliert.
- **Üben „Parameter“:** Bei 15 von 40 Zahlensätzen ragt die Spitze des Richtungspfeils aus dem gezeichneten Raum. Rechnerisch richtig, nur Darstellung.
- **Üben „Rotationskörper“:** nur 8 verschiedene Kurven (fester Katalog), „Tempo“ wiederholt sich öfter. Kein Fehler, aber wenig Abwechslung.
- **Namen von Erfolgen** „Abi-ready“, „Residency“ (englisch) und **geschützte Leerzeichen** bei „z. B.“ und „30 %“ (können am Zeilenende umbrechen): Geschmack bzw. Typografie, nicht geändert.
- **Zielgruppe im Play Store:** `STORE-RELEASE.md` empfiehlt „16–17 und 18+“. Schüler der Klasse 11 können 15 sein. Ob „13–15“ dazugehört (mit den strengeren Regeln), ist deine Entscheidung.
- **Kurve abfahren:** Wo eine eigene Funktion nicht definiert ist, zeichnet die Bühne die Bahn weiter auf Höhe 0 (ein durchgehender Linienzug). Cockpit, Fläche und Hinweis sagen dort ehrlich „nicht definiert“.
- **3D-Rechner:** unterscheidet jetzt Strahl und Gerade nur am Startpunkt; Treffer jenseits des Raumrands zählen weiter als Treffer.

### 4. Nicht prüfbar

- **Echte Geräte:** Nichts wurde auf einem Handy, iPhone oder iPad geprüft – nur Robolectric (Android) und Chromium (WebApp). Das iPad-Problem (weißer Bildschirm, Korrektur seit 2.11.1) wartet auf Marcels Test.
- **Vorlese-Hilfen:** In der WebApp (Zeichenfläche) gibt es keine Beschriftungen für Screenreader; die neuen Namen wirken nur in der Android-App. TalkBack selbst wurde nicht ausprobiert.
- **Klang der Stimme:** Geprüft ist der Text, der vorgelesen wird, nicht die Aussprache (z. B. „Klammer auf … Klammer zu“ klingt bei langen Gleichungen hölzern; Vektor-Rechnungen bleiben Zahlenfolgen).
- **Ob eine Aufgabe didaktisch gut gestellt ist** (Schwierigkeit, Reihenfolge, Abi-Nähe je Bundesland) kann die Nachrechnung nicht beurteilen.
- **Bestandteile in sherpa-onnx:** Die Lizenzen der darin enthaltenen Unterbibliotheken sind nicht einzeln geprüft (Lizenzliste nennt sherpa-onnx und ONNX Runtime).
- **Play In-App Updates, Abi-Pass mit echtem Schlüssel, Freischalt-Link auf der Live-Adresse:** erst mit Store-Installation bzw. echtem Test-Code prüfbar.
- **Rückmeldungen bei jeder denkbaren falschen Eingabe:** geprüft sind Start-Einstellung, typische Fehler und je Aufgabe 25 zufällige Eingaben, nicht alle.

---

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

---

## 16.11.2026 – Version 2.8.0 (Wünsche von Marcel) und ein offener Fehler: iPad zeigt weißen Bildschirm

**Neu:** Binomial-Labor unter „Mehr“ (n, p, k frei; genau/höchstens/mindestens; einzeln und kumuliert), Kapitel 24 „Binomialverteilung“ (5 Aufgaben),
Kapitel 25 „e-Funktion & Logarithmus“ (7 Aufgaben), zwei neue Bahnen in „Kurve abfahren“. 136 Aufgaben in 25 Kapiteln; alles unabhängig nachgerechnet
(`TourSixTest`: Binomial-Rechenkern gegen die Formel, abgedruckte Tabellenwerte, Ableitungen und Integrale numerisch).

**Offener Fehler (von Marcel gemeldet):** Auf seinem iPad blieb der Bildschirm beim Start weiß. Ursache noch **nicht** gefunden – hier gibt es kein Safari/WebKit zum Testen.
Die Server liefern korrekt aus (geprüft: Dateien, Dateitypen, auch `.wasm`). Bekannt ist: Die WebApp braucht WebAssembly-GC, das Safari erst ab iOS/iPadOS 18.2 kann.
Eingebaut in 2.8.0: (1) sichtbare Meldung statt leerem Bildschirm, wenn der Start scheitert (auch bei fehlendem WebAssembly-GC, sofort geprüft),
(2) `diagnose.html` – zeigt Browser, WebAssembly-GC, WebGL 2, letzten Startfehler, Zwischenspeicher; nichts wird verschickt, (3) kleinere Bildpuffer der 3D-Bühne auf großen Bildschirmen.
**Bitte prüft auf einem iPad/iPhone** (Safari, und als Startbildschirm-App): Startet es? Was zeigt `/diagnose.html`? Bis das geklärt ist, bitte nicht mit „läuft auf iPad“ werben.

---

## 16.11.2026 – Version 2.9.0: Start optimiert (Auftrag „Start optimieren, kein weißer Bildschirm“, von Marcel bestätigt)

**1. Kein leerer Bildschirm:** Ladebild direkt im HTML/CSS (Nachtblau, Lux-Logo als inline-SVG, Name) – steht ohne Skript ab dem ersten Byte.
Fortschrittsbalken mit echten Bytes für App-Kern (.wasm) und Schriften (entpackte Bytes gegen im Bau eingetragene Größen), danach 0,35 s Überblenden.
Scheitert der Start, steht dort eine Meldung mit Link zu `diagnose.html`.

**2. Schneller:** Emoji-Schrift 10,8 MB → 0,18 MB und DejaVu 0,76 MB → 0,12 MB (nur benutzte Zeichen, `tools/schrift/schriften.py`); .wasm und Schriften werden sofort angefordert
(nicht erst, wenn `raumlicht.js` sie verlangt); die 3D-Bühne (iframe) lädt erst danach; der Service Worker meldet sich erst nach dem Start an, lädt nichts mehr doppelt
und speichert jetzt auch die .wasm-Dateien schon beim ersten Besuch (zweiter Start ohne Netz).

**3. Messung** (`scene/test/web-speed.mjs`, Chromium, Handy-Größe; der Testserver bremst wie „Fast 4G“ 9 Mbit/s/60 ms und „Slow 4G“ 1,6 Mbit/s/150 ms und liefert wie euer Server aus:
gzip nur für .html und .wasm):

| | 2.8.0 vorher | 2.9.0 nachher | 2.9.0, wenn der Server auch .js/.ttf/.json komprimiert |
|---|---|---|---|
| Fast 4G, erster Start: erster Inhalt / bedienbar | 0,1 s / 27,9 s | 0,2 s / 5,8 s | 0,2 s / 5,3 s |
| Fast 4G, zweiter Start: bedienbar | 2,4 s | 2,3 s | 2,3 s |
| Slow 4G, erster Start: erster Inhalt / bedienbar | 0,2 s / 153,2 s | 0,2 s / 29,2 s | 0,2 s / 26,5 s |
| Slow 4G, zweiter Start: bedienbar | 2,5 s | 2,6 s | 2,6 s |
| übertragen beim ersten Besuch (mit Speichern) | 30,2 MB | 7,7 MB | 5,8 MB |
| Ordner `webapp/` | 28 MB | 17 MB | – |

Ehrlich dazu: gemessen auf einem Rechner ohne echte Grafikkarte, nicht auf einem Handy; die 2,3–2,6 s beim zweiten Start sind Rechenzeit (App-Kern übersetzen, Schriften, erste Zeichnung), kein Netz.

**Bitte am Server (liegt nicht in meiner Hand):**
- Heute kommen nur `.html` und `.wasm` komprimiert an (geprüft mit `Accept-Encoding: br, gzip`): **`.js`, `.json`, `.ttf` bitte ebenfalls komprimieren** – spart rund 1,9 MB beim ersten Besuch.
- **Brotli** statt gzip für `.wasm` brächte nochmal etwa ein Viertel weniger (die beiden .wasm sind zusammen 5,3 MB mit gzip).
- `.wasm`-Dateien haben den Inhalt im Namen: gern **lange Cache-Dauer** (`max-age=31536000, immutable`). `index.html`, `sw.js`, `version.json` weiter ohne Cache.

**4. Android:** Startbild über AndroidX SplashScreen (Nachtblau `#05071A`, Raumlicht-Symbol), danach App-Design. Das Fenster war schon vorher dunkel, weiß blitzte dort nichts.

**Weiterhin offen:** der weiße Bildschirm auf Marcels iPad. Das Ladebild hilft nur, wenn die Seite überhaupt lädt; die Ursache ist nicht gefunden (kein Safari hier).
Bitte auf iPad/iPhone prüfen und `diagnose.html` ansehen.
**Geprüft:** `web.mjs`, `web-update.mjs` (Update, Offline), `web-speed.mjs`; App-Tests und Lint. Nicht geprüft: echtes Gerät, Safari, Firefox.

---

## 16.11.2026 – Version 2.9.1 und Antwort zu „Skripte bündeln und verkleinern“

- **2.9.1:** Die gekürzten Schriften entstehen jetzt automatisch vor jedem Bau (`:web:schriften`); der Lauf „Raumlicht WebApp → Test“ prüft vor dem Vorspulen, dass kein benutztes Zeichen fehlt.
- **Zu „gebündelt und verkleinert“:** Das ist bei Raumlicht schon so – `raumlicht.js` (Webpack, minimiert, eine Datei) und `scene/scene.js` (esbuild `--bundle --minify`, eine Datei).
  Was fehlt, ist allein die **Komprimierung beim Ausliefern**: `raumlicht.js` 594 KB → 107 KB und `scene.js` 570 KB → 148 KB mit gzip. Das ist eine Server-Einstellung
  (gzip/Brotli auch für `.js`, `.json`, `.ttf`), kein Umbau der App. Bitte einschalten; geprüft habe ich es so: `curl -H "Accept-Encoding: br, gzip" -D - -o /dev/null <Adresse>/raumlicht.js` zeigt kein `content-encoding`.

---

## 16.11.2026 – Nachgemessen: Komprimierung am Server ist aktiv (danke)

Geprüft auf raumlicht-test (2.9.1) mit `Accept-Encoding: br, gzip`: alle großen Dateien kommen mit `content-encoding: gzip` an –
raumlicht.js 107 KB, scene.js 148 KB, Emoji-Schrift 169 KB, DejaVu 64 KB, die beiden .wasm 3,24 MB + 1,49 MB, index.html 4 KB. **Zusammen rund 5,2 MB** für den ersten Besuch
(2.8.0 ohne die Änderungen: 30,2 MB).

**Startzeiten** dazu aus der Simulation mit voller Komprimierung (`web-speed.mjs … alles-komprimiert`, Chromium am Rechner, kein echtes Handy):
erster Start 5,3 s („Fast 4G“) bzw. 26,5 s („Slow 4G“), zweiter Start 2,3–2,6 s ohne Netz. Gegen die echte Adresse kann ich die Zeiten nicht gebremst messen;
die übertragenen Bytes oben sind echt.

Noch möglich (klein): `.wasm` trägt den Inhalt im Namen und kommt ohne `cache-control` – `max-age=31536000, immutable` würde dem Browser die Rückfrage ersparen.
Live steht noch auf 2.5.1; 2.9.1 wartet auf eure Prüfung. Der weiße Bildschirm auf Marcels iPad ist weiter offen (er testet selbst).

---

## 16.11.2026 – Version 2.9.2: iPad-Befund, Fehlersuche, aufgeräumte Startseite

**Befund von Marcels iPad (Testadresse, 2.9.1):** Ladebild erscheint, danach kommt der Ton – aber das Bild bleibt **weiß**. Die App läuft also (Wasm, Logik, Ton), nur die Darstellung der Oberfläche in Safari fehlt.
Ursache noch offen; hier gibt es kein Safari. Verdächtig sind: das durchsichtige Fenster zur 3D-Bühne (die Bühne liegt als iframe unter der Compose-Zeichenfläche), zwei WebGL-Kontexte gleichzeitig, die Schriften.

**Eingebaut zum Eingrenzen:** (1) Mitschrift – Fehler/Warnungen der App landen in `localStorage` (`raumlicht.mitschrift`, letzte 40 Zeilen, nur auf dem Gerät) und stehen auf `diagnose.html`;
(2) Schalter `?ohne=fenster|buehne|emoji|ton` (kombinierbar) und dazu vier Test-Knöpfe auf `diagnose.html`; (3) echter WebGL-2-Zeichentest auf der Diagnose-Seite.
**Wenn ihr ein iPad/iPhone habt:** bitte `diagnose.html` öffnen, Tests 1–4 antippen und melden, ab welchem das Bild erscheint, plus Mitschrift. Bis dahin Raumlicht bitte nicht als „läuft auf iPad/iPhone“ bewerben.

**Außerdem:** Startseite aufgeräumt (Kapitel als eine wischbare Zeile statt Knopf-Wand) – Wunsch von Marcel nach einem Bildschirmfoto vom Android-Handy, wo die WebApp läuft.

---

## 16.11.2026 – Version 2.9.3: iPad, nächster Schritt

Neuer Befund von Marcel: **iPhone läuft, iPad bleibt weiß** (sein Handy ist Android, dort läuft es auch). Gleiche Browser-Technik, also liegt es vermutlich an der Größe der Zeichenfläche
(iPad bei doppelter Schärfe bis rund 5,6 Millionen Bildpunkte, iPhone rund 3). **Vermutung, nicht bestätigt.**
2.9.3 begrenzt deshalb auf iPads die Schärfe der Oberfläche so, dass höchstens rund 3 Millionen Bildpunkte gezeichnet werden (`window.devicePixelRatio` wird vor dem Start überschrieben;
erkannt über „iPad“ im User-Agent bzw. „Macintosh“ mit Touch). Schalter zum Prüfen: `?ohne=schaerfe` (Faktor 1), `?mit=schaerfe` (unbegrenzt) – als Test 5 und 6 auf `diagnose.html`.
Andere Geräte sind nicht betroffen. Bitte Live-Freigabe erst, wenn Marcel das iPad-Ergebnis gemeldet hat.

---

## 16.11.2026 – Version 2.10.0: Schwierigkeit, nachweislich steigend (Vorgabe von Marcel – er hat Weg 1 gewählt: nur das endlose Üben)

**Umfang (von Marcel so entschieden):** Raumlicht hatte keine Schwierigkeits-Auswahl. Neu ist sie im **endlosen Üben**: „Einstieg“, „Abi“, „Profi“ (oder „Alle“ wie bisher).
Die 136 festen Kapitel-Aufgaben, die Abi-Aufgaben und die Spiele bleiben unverändert – das war ausdrücklich nicht gewünscht.

**Stufen:** Einstieg = Aufgaben ohne Parameter-Gleichung (6 Arten). Abi = Gleichungen mit Parameter, Winkel, Abstände, Lotfußpunkt (10 Arten).
Profi = fünf neue mehrstufige Arten: Spiegelbild an der Ebene (Abstand + Lotfußpunkt + Spiegelung), Abstand windschiefer Geraden, Schnittkreis Kugel–Ebene,
Spiegeln an einer Geraden, Parallelität mit Parameter a. Zwischenwerte sind dort oft krumm (Lotfußpunkte mit Dritteln); das Ergebnis bleibt einstellbar (ganze Zahl bzw. Gitterpunkt),
weil die App nur ganze Zahlen als Eingabe kennt.

**Nachweis** (`DifficultyTest`, läuft in `:core:model:jvmTest`; Mittelwerte über 60 Zufallsaufgaben je Aufgabenart):

| Stufe | Aufgabenarten | Rechenschritte | Länge des Rechenwegs (Zeichen) | beteiligte Objekte | Parameter | Anteil krummer Werte | Gesamtwert |
|---|---|---|---|---|---|---|---|
| Einstieg | 6 | 1,84 | 24,46 | 3,67 | 0,67 | 0,00 % | 35,22 |
| Abi | 10 | 2,40 | 47,73 | 3,40 | 0,70 | 3,28 % | 43,13 |
| Profi | 5 | 4,00 | 124,48 | 5,00 | 1,20 | 10,38 % | 75,52 |

Der Test schlägt fehl, wenn von einer Stufe zur nächsten **Rechenschritte, Länge des Rechenwegs, Parameter, Anteil krummer Werte** oder der **Gesamtwert** (mindestens +20 %) nicht steigen,
wenn Profi nicht die meisten beteiligten Objekte hat, oder wenn die leichteste Profi-Aufgabe nicht über dem Abi-Durchschnitt liegt.
Ehrlich dazu: „beteiligte Objekte“ steigt von Einstieg zu Abi **nicht** (3,67 → 3,40) und wird dort deshalb nicht geprüft; „Parameter“ steigt von Einstieg zu Abi nur knapp (0,67 → 0,70).
Die Kennzahlen werden aus Rechenweg, Tipp, Angaben und Bühne gelesen – sie messen Aufwand, nicht, wie schwer sich eine Aufgabe für Schüler anfühlt.
Zusätzlich: jede Profi-Lösung unabhängig aus den angezeigten Angaben nachgerechnet (120 Zufallsaufgaben je Art), genau eine richtige Antwort, alles Sichtbare im Raum.

**Nicht umgesetzt (bewusst):** Brüche als Eingabe, Stufen für Kapitel/Abi/Spiele, mehrstufige Stochastik im Üben. Web = Android (gleicher Code), WebApp 2.10.0 gebaut und getestet.
Das iPad-Ergebnis zu 2.9.3 steht noch aus.

---

## 16.11.2026 – Version 2.10.1: iPad weiter weiß, Vermutung „Zeichenfläche zu groß“ widerlegt

Marcel hat mit der Begrenzung aus 2.9.3 erneut getestet: **iPhone läuft, iPad bleibt weiß.** Die Größe der Zeichenfläche ist es also nicht.
Bleibende Unterschiede iPhone ↔ iPad: (a) das iPad gibt sich standardmäßig als Mac aus („Desktop-Website“), (b) möglicherweise eine ältere iPadOS-/Safari-Version als auf dem iPhone.
Beides unbestätigt. 2.10.1 zeigt auf iPads nach dem Start 20 s lang einen HTML-Knopf „Kein Bild zu sehen? Hier tippen“ → `diagnose.html` (liegt über der Zeichenfläche, ist also auch bei weißem Bild sichtbar).
Dort stehen Mitschrift, Geräte-Modus und die Tests. **Raumlicht bitte weiterhin nicht als iPad-tauglich bewerben und 2.10.x erst live schalten, wenn ihr wollt – der iPad-Fehler besteht auch in der Live-Version 2.5.1.**

---

## 16.11.2026 – Konzept Pro für Raumlicht (Auftrag von Marcel bestätigt; **nur Konzept, nichts gebaut**)

Marcels Vorgaben dazu: Die bisherigen Aussagen „keine Käufe“ werden angepasst, sobald Pro gebaut wird; ein KI-Tutor ist als Bezahlfunktion in Ordnung.

### 1. Was ist frei, was Pro – und der Preis

**Grundsatz:** Frei muss für sich überzeugen (Empfehlungen, Schule, Eltern), Pro ist „alles bis zum Abi“. Keine Werbung, kein Druck, kein Abo, das sich verlängert.

| | Frei | Pro („Abi-Pass“) |
|---|---|---|
| Kapitel 1–6 (Punkte, Vektoren, Geraden, Lage, Ebenen, Skalarprodukt) | komplett, 36 Aufgaben | ✓ |
| Kapitel 7–25 | je Kapitel die ersten zwei Aufgaben zum Reinschnuppern | komplett (alle 136 Aufgaben) |
| Tagesaufgabe | ✓ | ✓ |
| Endlos üben | Stufe „Einstieg“ | zusätzlich „Abi“ und „Profi“ |
| Prüfungsaufgaben im Abi-Stil (7) | die erste („Festivalzelt“) | alle, mit Zeit-Modus |
| Kurve abfahren, Flug, Ballerbude, Zufallsmaschine, Galton-Brett | ✓ (fertige Bahnen) | zusätzlich eigene Funktionen, Baumeister, Geisterfahrt, Ringflug, Binomial-Labor |
| Spickzettel, Einstellungen, Musik, Vorlesen | ✓ | ✓ |
| Klausur-Planer, Fehler-Tagebuch, Lernstand | – | ✓ |
| „Lux erklärt“ (KI-Tutor, siehe 4) | – | ✓ mit Tagesgrenze |

**Preisvorschlag:** **Abi-Pass als Einmalkauf 14,99 €** (Einführung 9,99 €), gilt dauerhaft, kein Abo. Begründung im Vergleich (Preise aus der Recherche vom 16.11.2026, können abweichen):

| App | Modell | Preis |
|---|---|---|
| simpleclub unlimited | Abo | 16,49 €/Monat oder 89,99 €/Jahr |
| StudySmarter Premium | Abo | 9,99 €/Monat oder 89,99 €/Jahr |
| Mathegym (Privatlizenz) | Abo | 16 €/Monat oder 65 €/Jahr |
| sofatutor | Abo | je nach Laufzeit rund 20–35 €/Monat |
| Photomath Plus | Abo | 7,99 €/Monat oder 49,99 €/Jahr |

Diese Angebote decken alle Fächer oder alle Klassen ab; Raumlicht ist ein Spezialist (Oberstufen-Mathe, 136 Aufgaben). Ein Einmalpreis deutlich unter einem Jahresabo der Großen ist für Eltern leicht zu entscheiden und passt zum Versprechen „keine Abo-Falle“.
Von 14,99 € bleiben nach 19 % Umsatzsteuer rund 12,60 €; im Play Store gehen davon nach meinem Kenntnisstand 15 % Gebühr ab (bitte prüfen), über die Website nur die Zahlungsgebühren.

**Offen für Marcel:** (a) Preis, (b) wo genau die Grenze liegt, (c) Umgang mit heutigen Nutzern – bisher ist alles frei; wer die App schon hat, verliert Funktionen. Vorschlag: zum Start ein kostenloser „Gründer-Code“ für bisherige Tester.

### 2. Bezahlwege

- **Play-Fassung:** Google Play Billing, ein dauerhaftes Einmalprodukt (`raumlicht_pro`). Wiederherstellen über die Kaufabfrage beim Start – kein Code, kein Konto. In dieser Fassung kein Hinweis auf den Kauf über die Website (Play-Regeln; für die EU gibt es Sonderregeln, bitte prüfen).
- **Web- und GitHub-Fassung:** Woo-Kasse → Code per E-Mail → in den Einstellungen eingeben → freigeschaltet, ohne Konto. Rechnung und Nachweis über WooCommerce.
- In der App derselbe Schalter „Pro aktiv“ für beide Wege; die Fassungen `play`/`github` gibt es seit 2.6.0.

### 3. Was ich vom gemeinsamen Lizenz-Dienst brauche

1. **Aktivieren:** `POST /aktivieren {code, app: "raumlicht", geraet: <zufällige Installations-Kennung>}` → **signierter Freischalt-Schein** (z. B. Ed25519; Inhalt: App, Produkt, ausgestellt am, gültig bis oder „dauerhaft“, Kennung). Die App prüft die Signatur **ohne Netz** mit einem eingebauten öffentlichen Schlüssel – Pro funktioniert offline.
2. **Geräte:** z. B. bis zu 5 Aktivierungen je Code, selbst wieder freigebbar. Im Browser liegt der Schein im Browser-Speicher; wer den löscht, gibt den Code neu ein.
3. **Auffrischen:** gelegentlich (z. B. alle 30 Tage, mit Kulanz ohne Netz), damit Erstattungen wirken; Sperrliste.
4. **Play-Käufe:** optional Kaufbeleg serverseitig bei Google prüfen und denselben Schein ausstellen – nötig, sobald der KI-Tutor auch in der Play-Fassung laufen soll.
5. **KI-Zwischenstation:** `POST /tutor` nimmt Schein + Aufgabe, prüft, begrenzt (pro Tag), ruft die Claude-API auf. **Der API-Schlüssel liegt nur auf dem Server**, nie in App oder WebApp.
6. **Drumherum:** CORS für die App-Adressen, Testcodes, Webhook von Woo zum Erzeugen der Codes, Schlüsselwechsel möglich. Gespeichert werden nur Code, zufällige Kennung und Zeitpunkte – keine Namen in der App.

### 4. KI-Tutor „Lux erklärt“ – sinnvoll? Kosten?

**Sinnvoll, aber als zweite Stufe und eng geführt.** Idee: Nach einer falschen Antwort ein Knopf „Lux, erklär mir meinen Fehler“. Gesendet werden nur die Aufgabe, die eingestellten Werte und der richtige Rechenweg der App – **kein freier Text, keine persönlichen Daten**. Das Modell erklärt die bekannte Lösung, es rechnet nicht selbst (so bleiben Rechenfehler der KI draußen).

Kosten je Erklärung (rund 1.500 Zeichen-Einheiten hinein, 700 heraus; Preise der Claude-API in US-Dollar, Stand der Preisliste 03.11.2026):

| Modell | Preis je Million (rein / raus) | je Erklärung | 30 Erklärungen am Tag, ein Monat |
|---|---|---|---|
| Claude Haiku 5.5 | 0,10 $ / 0,50 $ | rund 0,05 Cent | rund 0,45 $ |
| Claude Sonnet 5.5 | 2 $ / 10 $ | rund 1 Cent | rund 9 $ |
| Claude Opus 5.5 | 4 $ / 20 $ | rund 2 Cent | rund 18 $ |

Folgerung: Mit Haiku 5.5 ist der Tutor im Einmalpreis tragbar, wenn es eine Tagesgrenze gibt (z. B. 30). Mit Sonnet oder Opus trägt ein Einmalkauf einen Vielnutzer nicht – dann bräuchte es ein Zeit-Paket. Welches Modell gut genug erklärt, müsste man an echten Aufgaben ausprobieren; das ist Marcels Entscheidung.

**Risiken, ehrlich:** (1) Daten verlassen das Gerät – Datenschutzerklärung, Play-Angaben „Datensicherheit“, Auftragsverarbeitung und das Alter der Nutzer (Schüler) müssen vorher geklärt sein; (2) laufende Kosten ohne Obergrenze, wenn die Tagesgrenze fehlt; (3) braucht Internet; (4) Erklärungen können trotz Vorgabe daneben liegen – deshalb nur die Lösung der App erklären lassen und „Stimmt nicht?“-Rückmeldung einbauen.

### Was beim Bau angepasst werden muss (noch nicht geschehen)

Texte „keine Käufe“ in den Einstellungen (Android und Web), Store-Eintrag (`store/texte.md`), Antworten zur Datensicherheit (`STORE-RELEASE.md`), Datenschutzerklärung der Website. Die Repo-Leitlinie verlangt, dass Bezahl-Angebote nie „nackter“ sind als der Rest – Pro-Inhalte bekommen dieselbe Ausstattung.

**Reihenfolge, wenn freigegeben:** 1) Lizenz-Dienst abstimmen, 2) Pro-Schalter und Sperren in der App samt Code-Eingabe, 3) Play Billing, 4) Tutor. Bis dahin baue ich nichts davon.

---

## 16.11.2026 – Konzept Pro: Marcels Entscheidungen

- **Grenze frei/Pro:** wie im Konzept – **ja**.
- **Bisherige Nutzer:** kostenloser „Gründer-Code“ für bisherige Tester – **ja**. Der Lizenz-Dienst braucht dafür Codes mit Preis 0.
- **KI-Tutor:** mit **Claude Haiku 5.5** starten und an echten Aufgaben testen – **ja**.
- **Preis/Zeitraum:** Einmalkauf; den Zeitraum hat Marcel offen gefragt. Mein Vorschlag an ihn: Inhalte **dauerhaft**, der KI-Tutor **12 Monate ab Kauf** (laufende Kosten), danach günstig verlängerbar.
  Für den Lizenz-Dienst heißt das: Ein Freischalt-Schein braucht **zwei Angaben** – „Pro dauerhaft“ und „Tutor gültig bis“. Entscheidung von Marcel steht noch aus.

Weiterhin nichts gebaut; ich warte auf die gemeinsame Abstimmung des Leitstands und Marcels Freigabe.

---

## 16.11.2026 – Konzept Pro, Nachtrag Umsatzsteuer

Alle Preise sind **Endpreise inkl. 19 % USt** und werden in App und Shop so angezeigt („inkl. MwSt.“).

| Produkt | brutto | netto (ohne 19 % USt) | kommt an über Play (netto minus 15 % Gebühr) | kommt an über die Website (netto minus Zahlungsgebühr) |
|---|---|---|---|---|
| Abi-Pass | 14,99 € | 12,60 € | 10,71 € | 11,81 € |
| Abi-Pass, Einführungspreis | 9,99 € | 8,39 € | 7,14 € | 7,76 € |
| Tutor-Verlängerung (Vorschlag) | 4,99 € | 4,19 € | 3,56 € | 3,70 € |

Annahmen: Play-Gebühr 15 % (nicht nachgeprüft); Zahlungsgebühr der Woo-Kasse beispielhaft 2,9 % + 0,35 € vom Bruttobetrag – bitte durch den echten Tarif des Zahlungsanbieters ersetzen.
Web-Kauf: Steuerklasse Standard 19 %, Rechnung weist die USt aus; bei Privatkunden in anderen EU-Ländern gilt deren Satz (OSS) – dann weicht der Nettobetrag ab. Play: Google führt die USt selbst ab.
Keine Steuerberatung.

---

## 16.11.2026 – Version 2.11.0: neue Aufgaben-Arten, Pro-Schalter vorbereitet (beides von Marcel direkt bestätigt)

### Aufgaben-Arten zur Analytischen Geometrie
Nur die Arten übernommen – Texte und Zahlen sind eigene, die Zahlen entstehen je Aufgabe neu.

| Art aus der Anregung | schon vorhanden? | neu im endlosen Üben | Stufe |
|---|---|---|---|
| Punkt in zwei Koordinatenebenen / gleich weit von zwei Achsen (offene Antwort) | nein | „Punkt mit Bedingungen“ in drei Varianten, Prüfung über Bedingungen statt fester Lösung, der Ursprung zählt nie | Einstieg |
| Spiegeln an einer Koordinatenebene | als feste Aufgabe 1-5 | zusätzlich als Übungsaufgabe | Einstieg |
| Spiegeln an einer Koordinatenachse | nein | neu, mit Original- und Bildpunkt in der Bühne | Abi |
| zwei Ebenen-Spiegelungen hintereinander | nein | neu (mit dem Hinweis: wirkt wie Spiegelung an einer Achse) | Profi |
| Parallelogramm nachweisen, Diagonalen-Schnittpunkt | vierte Ecke als feste Aufgabe 2-5 | neu: Nachweis über gleiche Vektoren + Schnittpunkt als Mittelpunkt | Abi |
| Raute prüfen | nein | neu als „Raute mit Parameter“ (gleiche Seitenlängen über Quadrate) | Profi |
| zweite Parameterdarstellung / dieselbe Gerade | nein | neu: „Für welches k beschreibt h dieselbe Gerade?“ (Richtungen kollinear, Stützpunkt liegt auf g) | Abi |
| drei Punkte auf einer Geraden | Richtung zu zwei Ringen (3-5) | neu: „Für welches k liegt C auf der Geraden durch A und B?“ | Abi |

Raum mit negativen Koordinaten fürs Spiegeln. „Alle“ mischt jetzt alle Arten aller Stufen.

**Schwierigkeit weiter nachweislich steigend** (`DifficultyTest`, jetzt 29 Aufgabenarten):

| Stufe | Aufgabenarten | Rechenschritte | Länge des Rechenwegs (Zeichen) | beteiligte Objekte | Parameter | Anteil krummer Werte | Gesamtwert |
|---|---|---|---|---|---|---|---|
| Einstieg | 8 | 1,88 | 33,76 | 3,59 | 0,50 | 1,33 % | 35,75 |
| Abi | 14 | 2,57 | 67,52 | 3,79 | 0,64 | 4,34 % | 47,91 |
| Profi | 7 | 4,00 | 149,93 | 5,57 | 0,86 | 12,18 % | 78,43 |

Geprüft: jede neue Art mit 250 Zufallsaufgaben (lösbar, Start löst nicht, alles im Raum, genau eine richtige Zahl) und unabhängig nachgerechnet
(offene Punkte: alle Gitterpunkte durchprobiert, Bedingungen geometrisch über den Achsenabstand bestätigt). 94 Logik-Tests, 138 App-Tests.

### Pro-Schalter und Sperren – vorbereitet, **ausgeschaltet**
- `Pro.ENABLED = false`: Im ausgelieferten Stand ist nichts gesperrt, es gibt keinen Kauf und keinen neuen Text für Nutzer. Ein Test (`ProTest`) hält das fest.
- Regeln nach Konzept: Kapitel 1–6 frei (das sind **34** Aufgaben – im Konzept stand irrtümlich 36), ab Kapitel 7 je die ersten zwei; Tagesaufgabe frei; Üben nur „Einstieg“ frei;
  die erste Prüfungsaufgabe frei; Baumeister, Geisterfahrt, Ringflug, Binomial-Labor, Klausur-Planer, Fehler-Tagebuch, Lernstand gehören zu Pro.
- Eine zentrale Sperre im App-Rahmen (`AppRoot`, für Android und Web gleich) zeigt bei gesperrten Zielen die Hinweis-Seite „Abi-Pass“; Schlösser an Aufgaben und Übungsstufen.
- Freigeschaltet wird später über `Prefs.pro` – **die Stelle, an der euer Lizenz-Dienst bzw. Play Billing andockt**. Kein Bezahlvorgang, kein Code-Feld, kein Tutor, kein Lizenz-Dienst gebaut.
- Noch offen für den echten Start: eigene Funktionen in „Kurve abfahren“/„Flug“ sperren, Texte „keine Käufe“, Store-Angaben.

### Preis
Entschieden hat Marcel: Grenze frei/Pro wie im Konzept, Gründer-Code, Tutor-Start mit Claude Haiku 5.5. **Nicht entschieden:** der Zeitraum und der Preis der Tutor-Verlängerung –
die „4,99 € pro Jahr“ waren mein Vorschlag, Marcel hat darauf mit „4,99 im Jahr?????“ reagiert. Bitte nicht als beschlossen führen.

---

## 16.11.2026 – Konzept Pro: Stand Preis der Tutor-Verlängerung

Marcel fand meinen geänderten Vorschlag gut („das ist nice“): Tutor-Verlängerung eher **9,99 € pro Jahr** statt 4,99 € (bei 4,99 € brutto kommen rund 3,60 € an; ein Vielnutzer an der Tagesgrenze kostet mit Claude Haiku 5.5 rund 5 $ im Jahr),
alternativ eine niedrigere Tagesgrenze. Das ist eine Tendenz, **noch keine ausdrückliche Entscheidung** – ich habe ihn um ein klares Ja gebeten.
Vorschlag im Ganzen: Abi-Pass 14,99 € einmalig (Einführung 9,99 €), Inhalte dauerhaft, Tutor 12 Monate inklusive, Verlängerung 9,99 € pro Jahr; alle Preise inkl. 19 % USt.
Bei 9,99 € brutto: 8,39 € netto, über Play rund 7,14 €, über die Website rund 7,76 € (Annahmen wie im USt-Nachtrag).

---

## 16.11.2026 – Konzept Pro: Preise von Marcel beschlossen

Marcel hat direkt bestätigt („ja“), alle Preise inkl. 19 % USt:

| Produkt | Preis | Zeitraum | kommt an über Play / Website (Annahmen wie im USt-Nachtrag) |
|---|---|---|---|
| Abi-Pass | 14,99 € einmalig (Einführung 9,99 €) | Inhalte **dauerhaft** | 10,71 € / 11,81 € (Einführung 7,14 € / 7,76 €) |
| KI-Tutor | im Abi-Pass enthalten | **12 Monate ab Kauf** | – |
| Tutor-Verlängerung | 9,99 € | je weiteres Jahr | 7,14 € / 7,76 € |

Für den Lizenz-Dienst: Der Freischalt-Schein braucht zwei Angaben – „Pro dauerhaft“ und „Tutor gültig bis“; die Verlängerung ist ein eigenes Produkt, das nur das Tutor-Datum verschiebt.
Ebenfalls beschlossen: Grenze frei/Pro wie im Konzept, Gründer-Code für bisherige Tester, Tutor-Start mit Claude Haiku 5.5 (an echten Aufgaben testen).
Gebaut ist davon weiterhin nur der ausgeschaltete Pro-Schalter mit den Sperren (2.11.0).

---

## 16.11.2026 – Konzept Lehrerzugang für Raumlicht (Auftrag von Marcel direkt bestätigt; **nur Konzept, nichts gebaut**)

**Was es heute schon gibt:** „Klassen-Paket“ (Mehr → Gemeinsam): Die Lehrkraft wählt Aufgaben, die App macht daraus einen Code; die Klasse gibt ihn ein, spielt das Paket und kann das Ergebnis als Text teilen.
Alles ohne Server und ohne Konto – das bleibt frei und ist der natürliche Baustein für Aufträge (das Code-Format kann der Lehrerzugang übernehmen).

### 1. Was die Lehrkraft sieht und steuert

| Funktion | Inhalt | wo |
|---|---|---|
| Auftrag „Kapitel/Aufgaben“ | feste Aufgaben auswählen (wie Klassen-Paket), Frist | Lehrerzugang → erscheint in der App unter „Mein Kurs“ |
| Auftrag „Üben“ | Thema + Stufe (Einstieg/Abi/Profi) + Anzahl, z. B. „10 Aufgaben Geraden, Stufe Abi“, Frist | wie oben |
| Klausur-Probe | eine Prüfungsaufgabe im Abi-Stil, auf Zeit, Punkte in Bewertungseinheiten | wie oben; Ergebnis je Teilaufgabe |
| Ergebnisse | je Schüler (Spitzname) und Kapitel: gelöst, Sterne, Versuche, zuletzt aktiv; je Auftrag: erledigt/offen | Lehrerzugang |
| „Wo hakt es?“ | je Aufgabe: Anteil beim ersten Versuch richtig – ohne Namen, für die Stundenplanung | Lehrerzugang |
| Beamer-Modus | Aufgabe groß mit 3D-Bühne, Lösung und Rechenweg schrittweise aufdeckbar, keine Schülerdaten sichtbar | in der App/WebApp selbst, braucht keinen Server |
| Kurs verwalten, Export, Löschen | zentral beim Leitstand | Lehrerzugang |

### 2. Welche Daten die App schickt (nur solange jemand in einem Kurs ist)

| Datum | wozu |
|---|---|
| Kurs-Code | Zuordnung |
| zufällige Installations-Kennung | Wiedererkennen desselben Geräts, kein Gerätename, keine Werbe-ID |
| Spitzname (selbst gewählt, Vorschlag wie „Lux-417“) | Anzeige bei der Lehrkraft |
| je gelöster Aufgabe: Aufgaben-Kennung bzw. Auftrag, Sterne (1–3), Zahl der Versuche, Datum | Ergebnisse |
| bei Klausur-Probe: Punkte je Teilaufgabe, benötigte Zeit | Auswertung |

**Nicht** gesendet: Klarname, E-Mail, Eingaben/Rechenwege, Standort, Geräte-Infos, Nutzung außerhalb der Aufträge (einstellbar: „nur Aufträge“ als Voreinstellung, „gesamter Fortschritt“ nur, wenn die Lehrkraft es für den Kurs einschaltet und die App es beim Beitritt anzeigt).
Ohne Netz wird gesammelt und später gesendet. „Kurs verlassen“ in den Einstellungen stoppt alles und bittet den Dienst um Löschung.

### 3. Wie ein Schüler beitritt
Einstellungen bzw. „Mehr“ → „Kurs beitreten“ → **Code eintippen oder QR/Link** (`…/?kurs=ABC123`, in der WebApp direkt) → Spitznamen wählen → eine Seite zeigt klar, was die Lehrkraft sehen kann → beitreten. Kein Konto, kein Passwort.
Der Dienst antwortet mit Kursname, Aufträgen und einem **signierten Schein „Pro über Kurs gültig bis …“** – dieselbe Mechanik wie beim Abi-Pass; in der App dockt er an `Prefs.pro` an (zweite Quelle „Kurs“).

### 4. Frei oder Schullizenz – mit Preisvorschlag (alle Preise inkl. 19 % USt)

| | frei | Kurs-/Schullizenz |
|---|---|---|
| Klassen-Paket per Code (ohne Server), Beamer-Modus | ✓ | ✓ |
| Kurs beitreten, Aufträge sehen | ✓ (ein Kurs zum Ausprobieren, bis 5 Schüler, 30 Tage) | ✓ |
| Ergebnisse je Schüler/Kapitel, „Wo hakt es?“, Export | – | ✓ |
| Pro-Inhalte (Abi-Pass) für alle im Kurs | – | ✓ für die Dauer der Lizenz |
| Klausur-Probe mit Auswertung | – | ✓ |
| KI-Tutor | – | vorerst nicht; später mit Kursgrenze (z. B. 300 Erklärungen pro Kurs und Monat) |

| Lizenz | Vorschlag | netto |
|---|---|---|
| Kurslizenz (ein Kurs, bis 35 Schüler, 12 Monate) | 49 € | 41,18 € |
| Schullizenz (alle Oberstufen-Kurse einer Schule, 12 Monate) | 199 € | 167,23 € |

Einordnung: Mathegym nennt für Schullizenzen 219 € (bis 50 Nutzer) bis 989 € (unbegrenzt) pro Jahr (Recherche vom 16.11.2026, kann abweichen) – für alle Klassenstufen; Raumlicht deckt nur die Oberstufe ab.
Ein Kurs mit 25 Schülern zahlt bei 49 € knapp 2 € pro Kopf statt 14,99 € für den Einzelkauf.

### Was ich vom zentralen Dienst brauche
`beitreten(code, spitzname, kennung)` → Kurs, Aufträge, Schein; `auftraege(kurs)`; `ergebnis(…)` (auch gesammelt); `verlassen(kennung)` mit Löschung. Aufträge am besten im Format des vorhandenen Klassen-Pakets plus „Üben: Thema, Stufe, Anzahl“ und „Prüfungsaufgabe: Kennung, auf Zeit“.

### Offen bzw. Risiken (ehrlich)
- **Datenschutz Schule:** Erstmals verlassen Schülerdaten das Gerät. Auch Spitzname + Kennung sind personenbezogen, sobald die Lehrkraft weiß, wer dahintersteckt. Nötig: Vertrag zur Auftragsverarbeitung mit der Schule, Einwilligung bzw. Rechtsgrundlage (Minderjährige), Löschfristen, Serverstandort. Das ist rechtlich zu klären, nicht von mir.
- Texte „keine Daten an einen Server“ in App, Store-Eintrag und Datensicherheit müssen dann geändert werden.
- Spitznamen können missbraucht werden (Beleidigungen) – die Lehrkraft muss umbenennen/entfernen können.
- Preise und die Grenze frei/Lizenz sind Vorschläge; Marcels Entscheidung steht aus.

---

## 16.11.2026 – Klärungsliste Recht und Datenschutz

Auf Marcels Wunsch („Benenne es, das kläre ich dann“) stehen alle offenen Rechts- und Datenschutzpunkte zu Lehrerzugang, Abi-Pass und KI-Tutor in `raumlicht/KLAERUNGSLISTE-RECHT.md`
(A1–A14 Schule, B1–B8 Bezahlen, C1–C6 KI). Vieles davon gilt für alle Apps gleich und betrifft euren zentralen Dienst (Serverstandort, Unterauftragnehmer, Löschkonzept, technische Maßnahmen, AVV-Muster).
Keine Rechtsberatung. Gebaut wird erst, wenn Marcel die Punkte geklärt hat.

---

## 16.11.2026 – Schulpreise von Marcel direkt bestätigt (ersetzt meinen Vorschlag im Konzept Lehrerzugang)

Mein früherer Vorschlag (Kurslizenz 49 €, Schullizenz 199 €) ist damit **überholt**. Es gilt, inkl. 19 % USt, je Schuljahr bis 31.07., ohne KI-Tutor:

| Lizenz | Preis | netto |
|---|---|---|
| Lehrkraft-Lizenz (eine Lehrkraft und ihre Kurse), je App | 29 € | 24,37 € |
| Schul-Lizenz (alle Lehrkräfte einer Schule), je App | 199 € | 167,23 € |
| Paket alle Apps, je Lehrkraft | 79 € | 66,39 € |
| Paket alle Apps, je Schule | 499 € | 419,33 € |

Folgen für Raumlicht: Die Lizenz hängt an der **Lehrkraft** (nicht am einzelnen Kurs) und endet am **31.07.** – der Schein „Pro über Kurs“ braucht also ein festes Enddatum statt „12 Monate ab Beitritt“.
Verkauf und Verwaltung über den zentralen Lehrerzugang; in der App wird nichts dazu gebaut, bis die Schnittstelle steht. Die Klärungsliste (`KLAERUNGSLISTE-RECHT.md`) gilt weiter.
Noch offen von Marcel: A9 (sieht die Lehrkraft nur Aufträge oder den ganzen Fortschritt?).

---

## 16.11.2026 – Lehrerzugang: Marcel hat A9 entschieden – gesamter Fortschritt

Die Lehrkraft sieht den **gesamten Fortschritt** der Schüler im Kurs, nicht nur die Aufträge. Das ersetzt meine Voreinstellung „nur Aufträge“ im Konzept.
Für die Schnittstelle: Die App sendet nach dem Beitritt das Ergebnis jeder gelösten Aufgabe (Tour-Aufgaben mit Kennung; Übungs- und Tagesaufgaben mit Art/Stufe statt Einzelkennung) – beim Beitritt einmal auch den bisherigen Stand,
sonst wäre der Fortschritt lückenhaft. Die Beitritts-Seite muss das klar sagen. In der Klärungsliste ist A9 entsprechend geändert (mehr Daten → ausdrücklich in Einwilligung und AVV).
Weiterhin nichts gebaut.

---

## 16.11.2026 – Zur Nachricht „Web = Android: Ja“ – das ist schon erledigt

Die Android-Oberfläche mit allen Extras läuft **seit 2.5.0 (16.11.2026)** aus gemeinsamem Code in der WebApp (Modul `shared`, `AppRoot`); auf raumlicht-test liegt aktuell **2.11.0**.
Nur Android haben weiterhin: natürliche Stimme „Thorsten“, Widgets, Erfolgskarte als Bild, In-App-Updater. Musik läuft seit 2.5.1 auch im Web.
Auch das Play-Store-Paket ist seit 2.6.0 fertig (auf Marcels Wunsch vorgezogen): `STORE-RELEASE.md`, `store/`, AAB am GitHub-Release.

**Was der Freigabe für raumlicht-live noch im Weg steht:** der offene Fehler „iPad zeigt nach dem Laden ein weißes Bild“ (iPhone und Android laufen). Der besteht auch in der Live-Version 2.5.1.
Ob ihr 2.11.0 trotzdem live schaltet, entscheidet ihr bzw. Marcel – aus meiner Sicht spricht nichts dagegen, weil sich für iPads nichts verschlechtert und es dort jetzt den Diagnose-Knopf gibt.

Zu den Punkten 2 und 3 (ein Kurs frei nur mit Rangliste; KI-Tutor nicht in Schullizenzen): zur Kenntnis genommen, in der App wird dazu nichts gebaut, bis eure Schnittstelle steht.
Marcel hat mir die Lizenzpreise (29 € / 199 € / Pakete 79 € / 499 €) und „gesamter Fortschritt“ direkt bestätigt; „ein Kurs frei, nur Rangliste“ hat er mir gegenüber noch nicht bestätigt.

---

## 16.11.2026 – Von Marcel direkt bestätigt

„Ein Kurs frei, nur mit Rangliste; mehr Kurse, Aufträge, Auswertung und Pro für alle nur mit Lizenz“ und „KI-Tutor vorerst nicht in Schullizenzen“ – beides hat Marcel mir bestätigt.
Damit ist für den Lehrerzugang aus Raumlicht-Sicht alles entschieden; gebaut wird, sobald eure Schnittstelle steht.

---

## 16.11.2026 – Version 2.11.1: iPad-Fehler eingegrenzt und umgangen (noch nicht auf einem iPad bestätigt)

Marcel hat die Diagnose-Ausgabe seines iPads geschickt (Safari 27.2, Desktop-Modus, WebGL 2 und WebAssembly-GC vorhanden). In der Mitschrift stand in jedem Bild
`RuntimeException: Can't wrap nullptr`; aufgelöst: `Shader.<init>` ← `LinearGradient.createShader` ← `BackgroundNode.draw`. Das Anlegen eines linearen Farbverlaufs liefert dort nichts zurück,
die Ausnahme bricht jedes Bild ab, übrig bleibt die weiße Grundfläche. Warum Safari dort nichts liefert, ist noch offen.

Geändert: alle 17 Verläufe laufen über `shared/.../ui/fx/Verlauf.kt` (feste, endliche Punkte; bei Fehler stufenweise einfacher bis einfarbig; Ergebnis steht einmal in der Mitschrift).
Auf raumlicht-test liegt 2.11.1. **Bestätigt ist die Behebung erst, wenn Marcel es auf dem iPad gesehen hat** – hier gibt es kein Safari zum Nachprüfen.

---

## 16.11.2026 – Version 2.12.0: Abi-Pass gebaut (von Marcel direkt bestätigt), Sperren aus bis zur Kassen-Adresse

Gebaut nach eurer Nachricht vom 16.11. und dem Vorbild KurvenFlug:

- **Code-Format:** Version 1, App 4, Produkt 1, Vorsilbe „RL-“, Ed25519, euer Base32-Alphabet; 9 Byte Inhalt + 64 Byte Signatur = 117 Zeichen (`core/model/.../License.kt`, `Ed25519.kt`, `Sha512.kt`).
  Andere App-Nummern und andere Produkte werden abgelehnt (also auch Schullizenz-Codes – die kommen erst mit dem Lehrerzugang).
- **Öffentlicher Schlüssel:** in `Pro.PUBLIC_KEY` eingetragen (979e…b812).
- **Freischalt-Link:** `https://raumlicht.mathenachhilfe-online.com/?pro=<Code ohne Bindestriche>` – mit oder ohne „RL“ am Anfang. Die WebApp prüft, speichert, nimmt den Code aus der Adresszeile
  und zeigt die Abi-Pass-Seite. Auf der Testadresse funktioniert derselbe Link.
- **Code-Eingabe:** Einstellungen → „Abi-Pass“ (WebApp und GitHub-APK); man kann die ganze E-Mail einfügen.
- **Sperren und Kauf-Knopf greifen erst, wenn `Pro.CHECKOUT_URL` gesetzt ist** (`core/model/.../Pro.kt`, derzeit leer). Bis dahin: kein Eintrag in den Einstellungen, keine Sperre, kein Wort von einem Kauf.
  Mit der Kassen-Adresse wechselt der Datenschutz-Satz in der App von „keine Käufe“ automatisch auf „kein Abo – nur der einmalige Abi-Pass, bezahlt auf unserer Website“.
  **Store-Texte und Datenschutzerklärung müsst ihr im selben Schritt ändern.** `ProTest.ohneKasseIstAllesFrei` schlägt dann absichtlich an, damit das niemand vergisst.
- **Preise in der App:** „zum Start 9,99 €, danach 14,99 € (inkl. MwSt.)“ (`Pro.INTRO_PRICE`, `Pro.PRICE`). Wenn der Startpreis endet, bitte Bescheid geben – der Text steht fest im Code.
- **Play-Fassung:** keine Sperren, kein Kauf-Knopf, kein Eintrag (dort wäre Play Billing Pflicht).
- **Android (GitHub-APK):** kein Link-Abfangen, nur Code-Eingabe. Der Link öffnet die WebApp.
- **Bestandsschutz:** nichts in der App – kostenlose Codes für Tester erzeugt ihr.

**Was ich brauche:** die Kassen-Adresse für RL-ABIPASS – und vorher einen echten Test-Code (Produkt 1, App 4), damit Marcel die Freischaltung einmal mit dem richtigen Schlüssel sieht.
Geprüft ist sie bisher nur mit einem Test-Schlüssel in den automatischen Tests.
