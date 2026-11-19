# Meldungen an den Leitstand – Quasselquest (Englisch Kl. 3–6)

Neueste oben.

## 19.11.2026 – Version 0.4.0 vom 19.11.2026: Lernpfad mit neuen Spielen, jede Karte geprüft, PC-Ansicht, Eltern-Tor-Standard – fertig für den Test

- **Lernpfad „Neu lernen“** (Englisch, Französisch, Spanisch): 2 Abschnitte × 8 Einheiten × 5 Lektionen. Neue Spielarten: Wimmelbild (statt Karten umdrehen), „Mach, was ich sage!“ (Dinge zu Figuren ziehen, 360 Befehle), „Ruby lernt von dir“ (Fehler an der Tafel finden, 192 Sätze – alle auf Eindeutigkeit geprüft), Regel entdecken, Satz-Zug, Nachsprechen (Erkennung nur im Gerät, jetzt auch fr/es), Geschichte. „Meine Insel“ mit allen gelernten Wörtern. Recherche in docs/RECHERCHE.md, Plan in LERNPFAD.md.
- **Jede Karte geprüft:** 1257 Einträge einzeln (4 Prüfer) + Lernpfad-Lektorat (alle Abschnitte), über 200 Korrekturen insgesamt, davon 26 Bildfehler (z. B. Schiff beim „Schaf“). Unsere Paket-Fassung ist die einzig gültige.
- **Ton:** alle englischen Figuren mit Rubys Stimme (Marcel); Zählreihen mit Pausen; doppelte Hörprüfung (Whisper + wav2vec2); 0,12 s Vorlauf beim Abspielen gegen verschluckte erste Laute; 103 von 1871 Aufnahmen (5,5 %) unsicher → Gerätestimme. Service Worker lädt nur Englisch vorab (11,8 MB), andere Sprachen beim ersten Abspielen.
- **Eltern-Tor** nach Standard ELTERN-TOR (Malaufgabe in Worten, Ziffern-Tastatur, 10 Min offen) vor Elternbereich, Einstellungen, „Alle Apps“; toter Wissensgarten-Link entfernt; „Deine Daten“ für Kinder auf der Startseite.
- **PC-Ansicht** ab 1100 px (Bilder vorher/nachher in screenshots/pc/). sw.js nur noch `caches.open(CACHE).match`.
- **Prüfung:** 26 Inhaltstests, Browser-Durchlauf Chromium Handy/PC + WebKit iPhone/iPad grün (101 Prüfungen), Bildschirmfotos angesehen.
- **Rückweg:** 0.3.3 (ed4e88b) – Lernstand bleibt (nur neue Felder lp, insel, tests).
- **Rechtsbezug: ja** – Eltern-Tor neu (schützt Einstellungen, „Alle Apps“, Links nach außen; UWG Anh. Nr. 28), Kinder-Datenschutz-Seite jetzt auf der Startseite, Mikrofon/Spracherkennung jetzt auch für Französisch/Spanisch (weiter nur im Gerät), Audio wird beim ersten Abspielen im Gerät gespeichert. Keine Preise, kein Kauf, keine Werbung, keine neuen Daten an Server.

## 19.11.2026 – Version 0.3.3 vom 19.11.2026: Pflicht 2 Klassenarbeits-Generalprobe – fertig für den Test

- **Kind:** Startseite je Sprache „📝 Mein Test ist am …“ → Datum (morgen bis 60 Tage) + Pakete/eigene Listen. Karte zeigt „Noch N Tage · Heute dran: …“ mit ▶ Üben; Tag gilt als geschafft nach einer Runde.
- **Generalprobe:** 12 gemischte Aufgaben, Vokabeln getippt (P.pruefeTipp: Alternativen in Klammern/Schrägstrich zählen, Akzent/Groß-klein → „Fast!“ mit Erklärung), sonst Auswahl; keine Hilfe während der Probe, Auswertung mit Lösung + „Warum?“-Regel, Fehler zurück in Kasten 1. Keine Noten, freundliche Texte auch bei 0 richtig.
- **Eltern:** 3–14 Tage vor dem Test Hinweis „Am <Tag> ist die Arbeit in <Sprache> … kostenlose Probestunde“ → `src=quasselquest-klassenarbeit&thema=<sprache>`, Zählung `nachhilfe_hinweis`/`probestunde_klick` mit ort `klassenarbeit`; ausblendbar je Test, Schalter „Nachhilfe-Hinweise“ gilt.
- **Pro:** gehört zum Pro-Kern, ist aber bis Pro-Schlüssel + Shop-Produkt frei sichtbar; nirgends Kaufoberfläche.
- **Prüfung:** 23 Inhaltstests (neu: Antwortprüfung über alle Paketkarten, Lernplan), Browser-Durchlauf Chromium Handy/PC + WebKit iPhone/iPad grün, Bildschirmfotos angesehen.

## 19.11.2026 – Version 0.3.2 vom 19.11.2026: Pflicht 8 Datenschutz in Kindersprache + Datenblatt für Schulen – fertig für den Test

- **Kindersprache:** Einstellungen → „🔒 Deine Daten – kurz erklärt“, sechs Sätze mit Ruby (bleibt auf dem Gerät, kein Konto, Mikrofon nur auf Knopfdruck, keine Werbung, Zählung ohne Person, eigene Listen, Eltern schalten Kopplung ein).
- **Datenblatt für Schulen:** `datenblatt-schule.html` (zum Ausdrucken), verlinkt aus dem Elternbereich und der Datenschutzerklärung. Inhalt: Konten, Speicherort, Mikrofon, Netzverkehr, Server, AVV-Einschätzung, Käufe, Löschen, Urheberrecht, Barrierearmut, Empfehlungen für Schulgeräte.
  ⚠️ Bitte prüfen: Angabe „eigene Server in Deutschland, keine Weitergabe an Dritte“ und die AVV-Einschätzung (keine Rechtsberatung).
- Bei „reduzierter Bewegung“ des Systems kein Konfetti mehr (vorher nur CSS-Animationen abgeschaltet).
- Tests: 21/21 Inhaltstests, Durchspielen in Chromium und WebKit grün; Bildschirmfotos angesehen.

## 19.11.2026 – Version 0.3.1 vom 19.11.2026: Pflicht 7 Elternbereich (Lernstand in Worten, Nachhilfe-Hinweis, Zählung) – fertig für den Test

- **Lernstand in Worten** für jede Sprache:
  - diese Woche Antworten und Anteil beim ersten Versuch,
  - „… kann jetzt“ aus den Kann-Sätzen der Episoden,
  - „Sitzt schon“ je Paket und je eigener Liste.
  - Die Antworten werden nur auf dem Gerät gezählt (Tagessummen je Sprache, 60 Tage).
- **Nachhilfe-Hinweis (Auftrag C1):**
  - Nur im Elternbereich. Auslöser: mindestens 10 Antworten und weniger als 60 % beim ersten Versuch in 14 Tagen, je Sprache.
  - Text wie im RATGEBER. Knopf „Kostenlose Probestunde anfragen“ → `/probestunde/?src=quasselquest-elternbericht&thema=<englisch|deutsch|franzoesisch|spanisch|latein>&klasse=<k>` (Test: staging.).
  - „Ausblenden“ gilt 14 Tage je Fach. Schalter „Hinweise auf unsere Nachhilfe zeigen“ (an).
  - Getestet: im Kinderbereich nie ein Hinweis.
- **Zählung (Auftrag A):**
  - `POST trainer(-test)…/zaehler/v1/e`, text/plain, credentials omit, keepalive, no-cors; nur app, v, ev, ort, thema.
  - Ereignisse: start, elternbereich, nachhilfe_hinweis, probestunde_klick.
  - Schalter „Anonyme Nutzungszählung“ (an); aus = kein Aufruf (getestet in Chromium und WebKit).
- Datenschutz ergänzt (Zählung, Nachhilfe-Hinweis).
- Tests: 21/21 Inhaltstests, Durchspielen in Chromium und WebKit grün, Spracherkennungs-Beleg grün, Bildschirmfotos angesehen.

## 19.11.2026 – Version 0.3.0 vom 19.11.2026: Pflicht 3 „Warum?“ + Pflicht 1 „Mein Schulbuch“ – fertig für den Test

- **Warum?** (Pflicht 3):
  - Bei einer falschen Ballon-Antwort wird die abweichende Stelle markiert (gemeinsamer Anfang und Ende bleiben, die Mitte ist rot unterstrichen; fehlende Buchstaben als „␣“).
  - Dazu eine Regel in einem Kindersatz: Groß/klein, Akzent, Satzzeichen, falscher Artikel – sonst die Erklärung der Karte. Bei Vokabeln nur, wenn sie das gewählte Wort nennt, sonst „ein anderes Wort“.
  - Die Lösung wird nicht verraten. Getestet für jede falsche Auswahl jeder Karte.
- **Mein Schulbuch** (Pflicht 1):
  - Je Sprache eigene Listen (Lektionen) mit „Deutsch = Fremdsprache“; als Trenner gehen auch „-“, „;“ und Tab. Für Deutsch Lernwörter, gespielt als Diktat (hören → bauen).
  - Verständliche Meldungen je Zeile (Format, doppelt, zu lang, leer), mindestens 4 und höchstens 120 Wörter.
  - Gespielt mit Ballon, Hör-Detektiv, Bauen und Memory, mit Wiederholung. Gespeichert nur im Gerät.
  - Hinweis im Bildschirm: nur eigene Wörter, keine Buchseiten kopieren. Teilen per QR folgt (Di 1.12.).
- Tests: 21/21 Inhaltstests. Durchspielen in Chromium und WebKit, inklusive Liste anlegen (erst mit Fehler, dann richtig) und Runde spielen in Englisch und Deutsch. Spracherkennungs-Beleg weiter grün. Bildschirmfotos angesehen.

## 19.11.2026 – Version 0.2.2 vom 19.11.2026: Beleg „Spracherkennung nur im Gerät“ + Fehlerbehebung Gerätestimme – fertig für den Test

- **Beleg (Auftrag AppZentrale / „Geld verdienen“ Nr. 5):** `node tools/web-sprache.mjs` schleust in **Chromium und WebKit** Ersatz-Erkenner ein und prüft den echten Code der App:
  - A) Der Browser kann es auf dem Gerät → Start nur mit `processLocally = true`.
  - B) Der Browser kann es nicht (wie Safari) → **startet nie**.
  - C) Der Browser setzt es nur zum Schein → **startet nie** (neu: strenge Prüfung nach dem Setzen).
  - D) Erst wenn die Eltern die Online-Erkennung ausdrücklich erlauben → Start ohne processLocally.
  - E) Erkennung aus → startet nie.
  - Ergebnis: alles grün. Die Web Speech API kann im Standardweg also kein Audio an Google oder Apple schicken.
- **Elternbereich:** erklärt jetzt ehrlich, ob das Gerät Sprache selbst erkennt (iPad/Safari: nein, dann nur Lautstärke-Prüfung, außer die Eltern erlauben die Online-Erkennung).
- **Fehler behoben:** Der Rückfall auf die Gerätestimme nahm immer eine englische Stimme, auch für Deutsch, Französisch und Spanisch (betrifft die 59 unsicheren Aufnahmen). Jetzt wird die passende Sprache genommen; getestet (F).
- Test-Stufe nur noch über den Hostnamen („lernen-test.“, „-test.“), wie im Standard.
- **Für live:** `quasselquest-live` kann statt a183ccf direkt auf diesen Stand. Er wurde genauso geprüft (Durchspielen in Chromium und WebKit).

## 19.11.2026 – LIVE-MELDUNG: Version 0.2.1 vom 19.11.2026, Commit 216388f für `quasselquest-live`

- **Inhalt:** Sprach-Umschalter (Englisch, Deutsch, Französisch, Spanisch, Latein) und 19 Sprachpakete (658 Karten) als Spiele mit Wiederholung, dazu Englisch Welt 1 (3 Episoden). 0.2.1 enthält nur die eindeutige Manifest-id (`"id": "./"`) für die gemeinsame Adresse.
- **Geprüft auf dem echten Test-Server** `https://quasselquest-test.mathenachhilfe-online.com` (version.json = 0.2.1) mit `node tools/web-start.mjs --url=…`:
  - Chromium Handy und PC, **WebKit iPhone 13 und iPad**: Start, alle 3 Episoden, je eine Paket-Runde in allen 5 Sprachen.
  - Vorlesen: 76 vertonte Sätze wirklich abgespielt. Fortschritt nach Neuladen, Eltern-Tor, „← Alle Apps“ (Test: staging.), **Offline-Start mit echtem Service Worker**, keine Konsolenfehler – alles grün. 20/20 Inhaltstests.
- **Kinderschutz:** nichts Kaufbares, keine Preise, kein Pro-Knopf (Pro-Code QQ-/App 7 vorbereitet, ohne Schlüssel unsichtbar). Mikrofon nur auf Knopfdruck, Erkennung nur im Gerät.
- **Gemeinsame Adresse:** Pfade relativ, `register('sw.js')`, Manifest `./` mit id `./`, Speicher `quasselquest.v1`, Cache `quasselquest-<Version>` (der Service Worker löscht nur eigene). Die Test-Stufe wird über „lernen-test.“ bzw. „-test.“ im Hostnamen erkannt. Unter `lernen.` wird nichts weitergeleitet.
  `lernen-test.mathenachhilfe-online.com/quasselquest/` war beim Prüfen noch nicht erreichbar; bitte nach dem Einrichten einmal `version.json` aufrufen (erwartet 0.2.1).
- **Größe:** 13 MB. Beim ersten Start speichert der Service Worker alle Aufnahmen für offline (Hinweis für mobiles Datenvolumen).
- **Rückweg:** Es ist die erste Live-Version. Zurück geht es, indem `quasselquest-live` wieder auf den Platzhalter gestellt wird (bisheriger Stand vor dem Live-Schalten). Bei späteren Versionen auf den vorherigen Live-Commit. Daten gehen dabei nicht verloren (nur im Browser, Schlüssel `quasselquest.v1`).
- **Für den Leitstand außer Live-Schalten:** nichts Weiteres nötig. Optional: Website-Datenschutzerklärung um Quasselquest ergänzen (Text in `webapp/datenschutz.html`) und die App auf `/lern-apps/` aufnehmen.

## 19.11.2026 – Version 0.2.0 vom 19.11.2026: Sprach-Umschalter + 19 Sprachpakete als Spiele – fertig für den Test

Auftrag Marcel (über die AppZentrale): heute Sprachen und Sprachpakete, danach live; den Wissensgarten erst danach ohne Sprachpakete live schalten.
- **Sprach-Umschalter** oben auf der Startseite: Englisch 💂, Deutsch 🥨, Französisch 🥐, Spanisch 💃, Latein 🏛️. Klassen 3–13. Direkt-Links: `#sprache=<en|de|fr|es|la>` und `#paket=<id>`.
- **19 Pakete, 658 Karten** aus dem Wissensgarten (unverändert übernommen, Liste wie vom Wissensgarten bestätigt). Deutsch nur der Sprach-Teil; Stilmittel und Epochen bleiben im Wissensgarten.
  - **Urheberrecht:** automatisch auf Namen von Lehrwerken geprüft (`tools/pakete-uebernehmen.mjs`), kein Treffer. Alle Karten sind eigene Formulierungen des Wissensgartens. Die Plus-Pakete (Abi-Vokabeln, Experte Sprache) sind noch nicht dabei; sie kommen, sobald Pfad und Schlüssel da sind, und gehören zu Pro.
- **Spiele statt Karteikarten:** Ballon-Jagd (Auswahl), Hör-Detektiv (hören → Bedeutung wählen), Buchstaben-Bau (Wort aus Buchstaben, bei Artikeln wie „la maison“ ist der Artikel vorgegeben), Memory (Wort ↔ Bedeutung). Die Spielarten wechseln sich ab. 8 Karten je Runde mit der Erklärung (`info`) nach jeder Antwort.
- **Wiederholung** in wachsenden Abständen (Leitner: 1, 2, 4, 8, 16, 32 Tage). Fällige Karten kommen zuerst, höchstens 5 neue je Runde. Auf jedem Paket steht, wie viele Karten sitzen.
- **Stimmen** (Lizenzen geprüft, `LIZENZEN.md`): Deutsch thorsten (CC0), Französisch siwis (CC BY 4.0), Spanisch davefx (CC0); Latein ohne Stimme. 664 Sätze vertont (13 MB, offline).
  Hörprüfung mit Whisper base und small: **59 Aufnahmen sind unsicher und werden NICHT abgespielt**, dort liest die Gerätestimme. Liste in `HOERPRUEFUNG.md`, bitte stichprobenweise anhören.
- **Kein Kaufbares sichtbar:** keine Preise, kein Pro-Knopf, keine Sperren. Pro je Sprache ist vorbereitet (Sprach-Byte im Code), aber erst mit Schlüssel und Shop-Produkt aktiv.
- **Gemeinsame Adresse:** alle Pfade relativ, Speicher `quasselquest.v1`, Cache `quasselquest-0.2.0`. Die Test-Stufe wird am Hostnamen („-test.“, „staging“) **oder** am Pfad (`/test/`) erkannt. Der Lernstand wird von 0.1.0 übernommen (Stand 2 → 3).
- **Tests:** 20/20 Inhalts- und Logiktests (Wiederholung, falsche Antworten nie richtig, Buchstaben-Bau ergibt die Antwort, jede Ansage hat Audio, unsichere Aufnahmen markiert). Durchspielen grün in **Chromium (Handy, PC) und WebKit (iPhone 13, iPad)**: 3 Englisch-Episoden und je eine Paket-Runde in allen 5 Sprachen, offline, Eltern-Tor, „← Alle Apps“, keine Konsolenfehler. Bildschirmfotos angesehen.
- **Noch offen:** echter Test auf iPhone/iPad (Ton!), Live-Adresse, Pro-Schlüssel, Zähl-Kennungen franzoesisch/latein/spanisch.

## 19.11.2026 – Vertonung mit freien Stimmen (Entscheidung Marcel), Nova vorbereitet – noch Version 0.1.0

- **Alle 185 festen Sätze** sind vorab vertont, jeweils normal und langsam (🐢). Das sind 370 MP3-Dateien mit zusammen 2,8 MB; sie werden offline gespeichert und über Web Audio abgespielt, das funktioniert auf iOS nach der ersten Berührung.
- **Stimmen mit geprüfter Lizenz** (`quasselquest/LIZENZEN.md`, in der App unter „Lizenzen“):
  - Ruby und Mia: Piper „en_GB-cori-medium“, CC BY 4.0, britisch.
  - Sam und Grandpa Ted: Piper „en_US-john-medium“, gemeinfrei (LibriVox), amerikanisch.
  - Ausgeschlossen sind alle Stimmen, die auf „lessac“ aufbauen (Daten nur für nicht-kommerzielle Forschung), und „semaine“, „ryan“, „hfc“ (Lizenz NC).
  - Es gibt **keine freie britische Männerstimme mit sauberer Lizenz**. Deshalb sprechen Sam und Ted amerikanisch; mit Nova wird das später einheitlich.
- **Hörprüfung:** Jede Datei wird beim Bauen mit Whisper geprüft. Klingt sie unklar, werden bis zu 6 Varianten erzeugt (Tempo, Satzzeichen). Ergebnis: 173 von 185 eindeutig erkannt. Die restlichen 12 sind fast nur kurze Wörter zum Antippen (is, I, the, your, my, to), die auch Whisper allein schlecht erkennt; Liste in `quasselquest/HOERPRUEFUNG.md`. **Bitte einmal auf einem echten Gerät anhören.**
- **Nova** ist vorbereitet:
  - Gleiche Kennung wie im Wissensgarten (`sha256("nova|en|<Satz>")`, `/nova/<h[0:2]>/<h>.mp3`).
  - Die Satzliste für das Stimm-Werk liegt in `webapp/vorlesen/texte.json`.
  - In den Einstellungen ist Nova als „kommt bald“ ausgegraut. Eingeschaltet wird sie erst nach Marcels Go: `NOVA_FREIGEGEBEN = true` in `version.js` setzen und das Stimm-Werk vertonen lassen.
- Tests: 15/15 Inhaltstests, darunter „jeder Satz hat beide Dateien“ und „Nova nicht freigeschaltet“. Durchspielen in Chromium und WebKit (Handy, iPhone 13, iPad, PC) grün, 77 Sätze wirklich abgespielt. Bildschirmfotos angesehen.

## 19.11.2026 – Name „Quasselquest“ umgesetzt, Prototyp 0.1.0 geht auf die Test-Stufe

- Entscheidung Marcel (über die AppZentrale): **Name „Quasselquest“**. Vertonung: **erst freie Stimmen** (Piper, Lizenz je Stimme geprüft); **Nova als ausgegraute Option** vorbereiten und erst nach Marcels Go vertonen.
- Umgesetzt: Ordner `quasselquest/`, `APP_ID = quasselquest`, „← Alle Apps“ mit `?src=quasselquest-kopf`, Titel „Quasselquest – Englisch mit Ruby“.
- **Workflow `.github/workflows/quasselquest-web-test.yml`** (Kopie Wissensgarten): Er prüft die WebApp und die Inhaltstests und spult dann `quasselquest-test` vor. Bitte den Server so einrichten, dass er `quasselquest/webapp/` aus `quasselquest-test` unter `https://quasselquest-test.mathenachhilfe-online.com` ausliefert, und CORS auf trainer-test freischalten.
- Inhalt wie 0.1.0 (Welt 1, 3 Episoden, 5 Spielarten). Zum Vorlesen dient vorerst die englische Gerätestimme; die Piper-Vertonung folgt.
- Tests: 13/13 Inhaltstests, Durchspielen in Chromium und WebKit (Handy, iPhone 13, iPad, PC) grün.

## 19.11.2026 – Nachtrag „Geld verdienen“

- Thema ist immer `englisch` (bestätigt); die Frage nach feineren Kennungen ist damit erledigt.
- Probestunde direkt über `https://www.mathenachhilfe-online.com/probestunde/?src=<name>-elternbericht&thema=englisch&klasse=<n>` (die alte Adresse `/probestunde-anfrage/` leitet mit 301 dorthin weiter).
- Gesperrte Pro-Deko wird im Kinderbereich gar nicht gezeigt.
- Weiter offen beim Leitstand: App-Nummer, Präfix der Freischalt-Codes, Shop-Produkt.

## 19.11.2026 – Rückmeldungen der Räume eingearbeitet

- **„Geld verdienen“** empfiehlt ebenfalls **Quasselquest** (Marke vor Start im DPMA/EUIPO prüfen, Klassen 9 und 41). Pro 7,99 € einmalig, ab Januar im Familienpass; die erste Geschichte der Reihe ist gratis. Im Elternbereich heißt Pro „Hörtraining für Klassenarbeiten Kl. 5/6“. Steht so im Konzept.
- **Kennungen:** Links zur Website nehmen immer `thema=englisch`. Für die Zählung schlägt „Geld verdienen“ zusätzlich feine Kennungen je Welt vor (`englisch-begruessung`, `englisch-zahlen`, `englisch-farben`, `englisch-familie`, `englisch-schule`, `englisch-tiere`, `englisch-essen`, `englisch-feste`, `englisch-simple-present`, `englisch-simple-past`). **Bitte entscheiden:** in die Positivliste aufnehmen oder nur `englisch`? Bis dahin baue ich `englisch`.
- **Probestunde:** `/probestunde-anfrage/?src=<name>-elternbericht&thema=englisch&klasse=<n>`. Laut den anderen Apps liefert die Adresse noch 404. Ich nehme dann vorerst `/probestunde/` wie die anderen Apps. Die Adresse des Neukundenpakets kommt vom Leitstand (Auftrag C2).
- **Wissensgarten:** Die Wortliste für Welt 1 ist abgeglichen (fehlende Sätze kommen am 24.11. in seine Pakete). Die Wortliste für Welt 3 liegt in `englisch/wortlisten/welt3-school-street.md`.
- **Rechenblitz:** keine Überschneidung. Später Link „Jetzt rechnen im Rechenblitz“ bei Zahlen (`?thema=add&stufe=1&modus=training`).

## 19.11.2026 – Konzept überarbeitet nach Recherche und RATGEBER (noch Version 0.1.0, nicht ausgeliefert)

- Eingearbeitet: Recherche „Sprachlern App gegen Duolingo“ und RATGEBER.md Teil 5 von „Geld verdienen“, Plattform-Hinweise von Experimentopia und Mathe-Trainer.
- **Spracherkennung jetzt standardmäßig nur auf dem Gerät.** Die Online-Erkennung des Browsers gibt es nur mit ausdrücklicher Zustimmung der Eltern hinter dem Eltern-Tor; Datenschutzseite angepasst.
- **Wochenziel (3 Episoden) statt Tagesserie**: Es gibt keinen Serienverlust, keine Energie und keine Ligen.
- Für Eltern: **„Das kann mein Kind jetzt auf Englisch“** mit Kann-Sätzen je Episode. Elterntexte jetzt in der ihr-Form.
- **Korrektur:** Themen-Kennung für Zählung und Nachhilfe-Hinweis ist das vorhandene `englisch` (nicht `englisch-*`). Gebraucht wird nur die neue App-Kennung `app=<name>`.
- Pro als Einmalkauf, ab Januar mit Familienpass. App-Nummer und Code-Präfix für `#pro=` bitte festlegen.
- Tests: 13/13 Inhaltstests, Durchspielen in Chromium und WebKit (Handy, iPhone 13, iPad, PC) grün; Bildschirmfotos angesehen.

## 18.11.2026 – Abgrenzung mit dem Wissensgarten vereinbart

- Wissensgarten: Wortschatz, unregelmäßige Verben, Tenses, Regeln als Merkkarten; Brummi-Radio bleibt dort. Englisch-App: hören, nachsprechen, Rollenspiele, Satzbau, Hörverstehen, Chants – keine Karteikarten.
- Gleiche Themen, andere Form, gegenseitig verlinkt: gs-englisch-school ↔ Welt 3, gs-englisch-food ↔ Welt 5, gs-englisch-clothes-weather ↔ Welt 6, gs-englisch-hobbys ↔ Welt 8, gs-englisch-familie-koerper ↔ Welt 2 (Tabelle in `wissensgarten/WOCHENRUNDE.md`).
- Wortlisten je Welt gehen an den Wissensgarten, damit beide Apps dieselbe Schreibweise nutzen (britisch: colour, favourite …). Welt 1 ist verschickt.

## 18.11.2026 – Version 0.1.0 vom 18.11.2026: erster spielbarer Prototyp „Hello Harbour“ (noch nicht auf einer Test-Domain)

- **Ordner** `englisch/webapp/` (Branch ccr-0e3accf3-cdxlc8). Nach der Namenswahl: Ordner umbenennen, `APP_ID` setzen, Workflow `<name>-web-test.yml` (Kopie Experimentopia) → Branch `<name>-test`.
- **Inhalt:** 3 Episoden – „Hello!“ (sich vorstellen), „Fish and numbers“ (Zahlen 1–10, Fische kaufen bei Grandpa Ted), „Colourful boats“ (Farben). Je Episode Szene hören → 3 Spiele → Sprech-Mission → Stempel, dazu „Ruby erklärt“ (Deutsch, ohne Abkürzungen).
- **5 Spielarten:** Hör-Detektiv, Papagei-Echo (Aufnahme bleibt im Speicher, Vergleich Ruby ↔ Kind), Satz-Zug, Ruby says, Dialog-Bühne mit Zahl-/Farbwahl und eigenem Namen (Prüfung mit verständlicher Meldung).
- **Plattform:** „← Alle Apps“ (`?src=englisch-kopf`, Test-Stufe staging.), Eltern-Tor (Geburtsjahr), Mikrofon-Wahl für Eltern (Browser / nur Gerät / aus), Datenschutzseite, offline (Service Worker), Update nur außerhalb von Runden, Version + Datum in Einstellungen und Elternbereich.
  Noch nicht drin (Woche 2): Zählung, `#pro=`, Eltern-Kopplung + Sicherung, Nachhilfe-Hinweis – brauchen Name/Domain/CORS.
- **Stimme:** vorerst die englische Gerätestimme (nur lokale Stimmen); ohne Stimme zeigt die App den Satz als Text. KI-Vertonung, sobald Guthaben da ist.
- **Geprüft:** `node --test tools/test-inhalt.mjs` 12/12 grün (eindeutige Lösungen, Platzhalter, Version überall gleich, Service Worker vollständig).
  `node tools/web-start.mjs` grün in **Chromium (Handy, PC) und WebKit (iPhone 13, iPad)**: alle 3 Episoden durchgespielt, Fortschritt nach Neuladen da, Eltern-Tor, „← Alle Apps“, **offline-Start**, keine Konsolenfehler. Bildschirmfotos in `englisch/screenshots/` angesehen.
- **Noch offen:** echter Test auf Marcels iPad mit Mikrofon und Stimme (geht erst mit Test-Domain).

## 18.11.2026 – Konzept zur Entscheidung (Frist Mo 23.11., 11 Uhr – früher fertig)

- **Konzept:** `englisch/KONZEPT.md` (Namen, Zielgruppe, Welten, Spielarten, Stimmen/Mikrofon, Gratis/Pro, Abgrenzung Wissensgarten, Plan 4 Wochen).
- **Namen (Marcel entscheidet):** ⭐ **Quasselquest** · Wortwelle · Plauderhafen. Websuche ohne Konflikt; DPMA/EUIPO-Prüfung bitte vor der Wahl.
- **Kurz:** Kinder Kl. 3–6 reisen mit Ruby 🦜 (Papagei) durch eine englische Hafenstadt und nach London. Jede Episode (5–8 Min.): Szene hören → 3 Spiele → Sprech-Mission mit einer Figur → Stempel.
  10 Spielarten (Hör-Detektiv, Papagei-Echo, Dialog-Bühne, Satz-Zug, Ruby says, Hör-Malbild, Chant-Bühne, Wort-Fangen, Klang-Paare, Mini-Geschichte). Grammatik nur als Sprechmuster.
- **Gratis:** Welt 1 + Welt 7 komplett, erste Episode jeder Welt, alle Spielarten. **Pro:** übrige Welten, Geschichten-Reihe, Klassenarbeit-Hörtraining 5/6. Kinderbereich ohne Preise/Kaufaufforderung.
- **Technik:** schlichtes JavaScript (kein Wasm → keine weißen Seiten auf iOS), PWA offline + Auto-Update, alle Plattform-Bausteine (Alle Apps, Eltern-Kopplung + Sicherung, Zählung, `#pro=`).
- **Bitte vom Leitstand:**
  1. Entscheidung Name/Konzept (Marcel).
  2. Subdomains `<name>` und `<name>-test` (Server holt `<name>/webapp/` aus `<name>-test`/`<name>-live`).
  3. CORS auf trainer/trainer-test, Zähl-Kennung `app=<name>` (Thema `englisch` gibt es schon).
  4. ⚠️ **Stimmen:** Der OpenAI-Zugang meldet „kein Guthaben“ (18.11.). Für die Vertonung (britisches Englisch, mehrere Figuren) Guthaben oder Stimm-Server-Zugang. Bis dahin Prototyp mit Gerätestimme.
  5. Pro: Shop-Produkt und Signatur-Schlüssel. 6. Teilnehmerliste Montagsrunde.
- **Abstimmung Wissensgarten:** Vorschlag – WG behält Vokabel-/Verb-/Tenses-Pakete als Merkwissen, wir bauen keine Karteikarten; gegenseitige Links (`#paket=` ↔ `#welt=`/`#episode=`). Nachricht an den WG-Raum ist raus; Antwort folgt hier.
- **Nächster Schritt:** Ich beginne schon jetzt mit dem Prototyp (Welt 1, 3 Episoden, 5 Spielarten) im Arbeitsordner, damit am Fr 27.11. etwas Spielbares auf `<name>-test` liegt. Umbenennen nach der Namenswahl.
