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
