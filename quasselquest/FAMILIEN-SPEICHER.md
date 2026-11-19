# Familien-Speicher – Auszug für Quasselquest (AppZentrale, Bauplan v1.1, 19.11.2026)

Entscheidung Marcel: Alle Lern- und Spielstände aller Apps und Kinder liegen verschlüsselt auf unserem Server und gehören zur
Familie. Nur die Geräte können sie öffnen. Kein Google, kein Konto fürs Kind. Anbindung über `familie.js` (Bibliothek vom
Mathe-Trainer, API-Doku folgt, Bibliothek bis So 22.11.). Reihenfolge: 0.4.0 + PC-Ansicht → vorbereiten → anbinden ab Fr 20.11.

## Begriffe
- famId 16 B base64url (22 Zeichen), famKey 32 B (nur auf Geräten), schreib 16 B (Header `X-Schreib`).
- Familien-Link/QR: `https://lernen.mathenachhilfe-online.com/quasselquest/#familie=<famId>.<famKey>.<schreib>`
- Familien-Nummer = erste 8 Zeichen von famId (nur zum Finden).
- kindId 16 B base64url je Kind, dauerhaft. Spitzname + Klasse nur im verschlüsselten Familien-Index (→ Profilwahl).

## Blob je App und Kind
- JSON `{"app":"quasselquest","v":1,"kind":"<kindId>","stand":"<ISO-Zeit>","daten":{…Format v3…}}` – Pro-Rechte NICHT darin.
- Rahmen vor dem Verschlüsseln: 1. Byte `z` + gzip(JSON) oder `j` + JSON; lesen müssen beide.
- Schlüssel: HKDF-SHA256(famKey, salt 32 Nullbytes, info `lernapps/v1/quasselquest/<kindId>`, 32 B); Index: info `lernapps/v1/index`.
- AES-GCM-256, 12-Byte-IV, Body text/plain `<iv>.<ct>` (base64url), höchstens 2 MB.

## Server `/familie/v1/` (unter lernen(-test).…)
PUT `<famId>` anlegen · GET/PUT `<famId>/index` · GET/PUT `<famId>/s/quasselquest/<kindId>` (PUT mit `If-Match: <rev>`, Antwort `{rev}`,
veraltet → 409 `{rev, d, t}`) · GET `liste` · GET `rechte` · POST `einloesen {code}` · DELETE `<famId>` · GET `protokoll`.

## Konflikte – nie still überschreiben
Bei 409 zusammenführen, wo sicher: Karten (Leitner) → höherer Kasten/spätere Fälligkeit gewinnt; Tagessummen → je Tag Maximum
(nicht doppelt zählen); Lernpfad `lp` → Maximum je Einheit; Episoden-Sterne → Maximum; eigene Listen → Vereinigung nach id;
Insel-Positionen, Einstellungen, Name → fragen (Elternbereich), sonst Gerätestand behalten.

## Ablauf in der App
- Automatisch sichern nach jeder Runde bzw. höchstens alle 2 Minuten und bei `visibilitychange: hidden`. Beim Start `liste` prüfen.
- Elternbereich (hinter dem Eltern-Tor): koppeln (Link/QR), Kind wählen, Status, Notfall-Blatt, Löschknopf, Protokoll, Zustimmung beim ersten Koppeln.
- Profilwahl für mehrere Kinder über den Familien-Index; ohne Kopplung bleibt alles wie heute (ein Profil im Browser).

## Vorbereitung (vor familie.js)
- [ ] Laden/Speichern hinter `Speicher`-Schnittstelle (lokal + später familie), `stand`-Zeit pflegen
- [ ] Zusammenführen-Funktion (rein, mit Unit-Tests)
- [ ] Profil-Platzhalter (kindId lokal erzeugen, sobald gekoppelt)

## familie.js – API (Mathe-Trainer 3b0d53c, gespiegelt in FAMILIE-API.md, 19.11.2026)
- Netz-Funktionen werfen nie: `{ok:true,…}` | `{ok:false, grund}`; grund: netz | keine | konflikt | schreibrecht | zu-gross | zu-oft | schluessel | ungueltig | fehler.
- `neueFamilie()`, `familieLink(fam, app, basis?)`, `familieLesen(textOderHash)`, `familienNummer(famId)`, `notfallBlatt(fam, app, basis?)`, `neueKindId()`, `geraetKindId(id)`.
- Index (= Profilwahl): `neuerIndex()`, `kindHinzu(index, {name, klasse, farbe})`, `appMerken(index, app)`.
- Server: `const fa = familie(fam, {basis, f})` → `anlegen`, `indexLaden/indexSichern(index, rev)`, `standLaden(app, kindId)`,
  `standSichern(app, kindId, daten, rev)` (Konflikt → `{ok:false, grund:'konflikt', daten, stand, rev}`: zusammenführen, erneut sichern),
  `liste`, `rechte`, `einloesen(code)` (nur hinter dem Tor), `protokoll`, `loeschen`, `nummer`, `link(app)`.
- Sperrliste: `sperrlisteLaden({basis, f, speicher, schluessel:'quasselquest.sperrliste'})`, `codeGesperrt(code, …)` vor jeder Offline-Pro-Prüfung.
- Datei-Weg folgt vom Mathe-Trainer; Server vom Leitstand steht noch aus.
- Öffentlicher Wiederherstellungs-Schlüssel (privater Teil nur bei Marcel): `REC_PUB = vMrORj9yObM3Ijd9sGStjUB9I3f6ot9E8M-70yGeQ0M` – fest in familie.js, gilt auch auf lernen-test.
- **familie.js @ ffcfe39** (sha256 `a67bc3a8939e5e72800e7b28a53ddecfc9bf71d98869f59baffe040ef6750b6f`) kam als Text vom Mathe-Trainer (19.11., 14:23 UTC).
  Einbinden: unverändert als `webapp/js/familie.js`, oben Kommentar „familie.js @ ffcfe39 (Quelle mathe-trainer)“, nichts nachladen.
  Tests: `tests/familie.mjs` (Pfad in Zeile 5 anpassen) → „familie: 41 Prüfungen bestanden“. Prüfsumme vor dem Kommentar vergleichen.
- **NEU: familie.js @ 1f4b377** (sha256 `3016aabd994d9f00ddead3c2246b05e5dd3ec9106669638396cc42400283e640`) ersetzt ffcfe39 vollständig (19.11., 14:43 UTC).
  `anlegen()` ohne Parameter nutzt REC_PUB und schickt X-Notfall-Pruef; Sperrliste `{sperrliste:[SHA-256(codeNr)]}`, `codeNummer(code)` → `L7.<produkt>.<serie>` für QQ-Codes, `codeGesperrt(codeOderNr)`.
  Neue grund-Werte: gibt-es-schon, code-unbekannt, code-gesperrt, code-vergeben, noch-nicht-live. `einloesen` → `{rechte, recht}` mit `recht.codeNr`.
  Tests → „familie: 51 Prüfungen bestanden“. Test-Server: https://lernen-test.mathenachhilfe-online.com/familie/v1/ (live: 404 noch_nicht_live).

## Pflichttexte (Bauplan v1.5, AppZentrale 19.11.)
**Hinweis beim ersten Koppeln und im Bereich „Familie“** (darunter Link zur Datenschutzerklärung und „Einverstanden“):
„Eure Lernstände werden verschlüsselt auf unserem Server in Deutschland gespeichert. Der Schlüssel liegt auf euren Geräten. Damit wir euch helfen können, falls ihr den Familien-Zugang verliert, hinterlegen wir zusätzlich eine verschlüsselte Notfall-Kopie. Sie lässt sich nur mit einem getrennt und offline aufbewahrten Schlüssel von Mathenachhilfe Online öffnen. Technisch könnten wir damit eure Lernstände lesen; wir tun das nicht und nutzen ihn nur, wenn ihr uns um Wiederherstellung bittet. Jede Wiederherstellung seht ihr hier im Protokoll.“

**Löschen** (nur hinter dem Tor): Warnung „Das löscht die Familie in ALLEN Lern-Apps: alle Lernstände aller Kinder auf unserem Server, die Notfall-Kopie und das Protokoll. Gekaufte Codes werden wieder frei und lassen sich in einer neuen Familie erneut einlösen. Die Lernstände auf diesem Gerät bleiben erhalten.“ → Familien-Nummer eintippen → `fa.loeschen()` → lokal nur die Familien-Schlüssel entfernen.
Bei 404 auf anderen Geräten: „Diese Familie gibt es nicht mehr. Lernstand bleibt auf diesem Gerät.“
- **familie.js @ e9993ec** (sha256 ohne Kopfkommentar `3f7ec567e4849c3870be8398aa72894475a55df672f79f22e536fdb8f209f5f9`) = 1f4b377 + Patch: FEHLER-Tabelle um `leer: 'keine', unbekannt: 'familie-weg'` ergänzt
  (+ Kommentar bei standLaden). Bei grund `familie-weg`: „Diese Familie gibt es nicht mehr. Lernstand bleibt auf diesem Gerät.“
  Tests: nachgebauter Server mit `{fehler:'unbekannt'}` bzw. `{fehler:'leer'}`; „Nach dem Löschen“-Prüfung auf 'familie-weg'. → 51 Prüfungen.
- **Recht: freigegeben mit Auflagen (A6, v1.6):** nur EIN Einwilligungstext (der Pflichttext), darunter Link „Mehr dazu in der Datenschutzerklärung (Abschnitt 10m)“ und Knopf „Einverstanden – Familie anlegen“, nichts vorausgewählt. Anlegen/Beitreten nur hinter dem Eltern-Tor. Roadmap (noch nicht bauen): ein Kind einzeln löschen.
- **Recht V8 (v1.7):** Beitreten per Link/QR hinter dem Eltern-Tor mit demselben Pflichttext, Knopf „Einverstanden – Familie beitreten“ (nicht vorausgewählt), Abbrechen = nichts hochladen.
  Sicherheitshinweis NICHT im Pflichttext, sondern dauerhaft auf der Familien-Seite und bei „Link teilen“, QR-Code und Notfall-Blatt:
  „Wer den Familien-Link oder den QR-Code hat, kann die Lernstände eurer Familie öffnen, ändern und löschen. Teilt ihn nur mit Personen, denen ihr vertraut, und bewahrt das Notfall-Blatt sicher auf.“
  Meldung mit „Rechtsbezug: ja (V8)“.
