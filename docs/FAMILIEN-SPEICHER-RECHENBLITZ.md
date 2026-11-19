# Familien-Speicher – Anbindung Rechenblitz (Auszug Bauplan v1.1, AppZentrale 19.11.2026, 9edd3a9)

Stand: Anbindung geplant nach dem Fr-Update 20.11., sobald `familie.js` (Mathe-Trainer) und der Test-Server stehen.

## Begriffe
- `famId` 16 B base64url, `famKey` 32 B (nur auf Geräten), `schreib` 16 B (Header `X-Schreib`).
- Familien-Link: `https://lernen.mathenachhilfe-online.com/rechenblitz/#familie=<famId>.<famKey>.<schreib>`
- `kindId` 16 B base64url, dauerhaft im Profil (`ROOT.profiles[x].kindId`); Gerätedaten (`rb-classes`, `rb-teacher`) als `geraet-<Geräte-Id>`.

## Blob je Kind
`{"app":"rechenblitz","v":1,"kind":"<kindId>","stand":"<ISO>","daten":{"profil":{name,emoji,data},"lang"}}`.
- Pro-/Rechte-Codes NICHT im Kind-Blob (gehören zur Familie).
- Rahmen vor dem Verschlüsseln: `z` + gzip(JSON) oder `j` + JSON; beide lesen.
- Schlüssel: HKDF-SHA256(famKey, salt = 32 × 0, info = `lernapps/v1/rechenblitz/<kindId>`, 32 B); Index: `lernapps/v1/index`.
- AES-GCM-256, 12-B-IV, Body `<iv>.<ct>` (base64url); ≤ 2 MB je Blob, ≤ 30 MB je Familie.

## Server `/familie/v1/`
- `PUT /<famId>` (X-Schreib) anlegen · `GET/PUT /<famId>/index` · `GET/PUT /<famId>/s/rechenblitz/<kindId>` (If-Match: rev; 409 → {rev,d,t})
- `GET /<famId>/liste` · `GET /<famId>/rechte` · `POST /<famId>/einloesen {code}` · `DELETE /<famId>` · `GET /<famId>/protokoll`

## Konflikt
Server-`rev` entscheidet, nie Zeitstempel allein. Bei 409 sicher zusammenführen (beste Werte, Summen), sonst im Elternbereich beide Stände zeigen. Nie still überschreiben.

## In der App
- Automatisch sichern nach jeder Runde (höchstens alle 2 Min.) und beim Verstecken; beim Start `liste` prüfen, Neueres laden.
- Elternbereich (hinter dem Eltern-Tor): koppeln, Kind zuordnen, Status, Notfall-Blatt, Löschen, Protokoll, Zustimmung beim ersten Koppeln.
- Bestehende eltern/v1-Kopplung: beim Familien-Link Stand übernehmen; Bericht (`b/`) bleibt.
