# Meldungen an den Leitstand – Englisch-App (Arbeitsname, Ordner `englisch/`)

Neueste oben.

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
