# Meldungen an den Leitstand – Quasselquest (Englisch Kl. 3–6)

Neueste oben.

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
