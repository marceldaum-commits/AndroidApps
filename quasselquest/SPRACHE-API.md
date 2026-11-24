# SPRACHE-API – Aussprache-Auswertung über unseren Server (Weg 2)

Stand 19.11.2026 · Entwurf für den Leitstand (Server) · App-Seite: Quasselquest, Arbeits-Branch, hinter Feature-Flag

## Wann ruft die App den Server auf?
**Nur** wenn die Eltern im Elternbereich (hinter dem Eltern-Tor) bei „Aussprache-Auswertung“ die Möglichkeit
„Über unseren Server in Deutschland“ gewählt haben. Diese Wahl ist **nie** voreingestellt. Bei „Im Gerät“ und „Aus“
gibt es **keinen einzigen** Aufruf (eigener Test in der App). Eine andere Wahl gilt als Widerruf und wirkt sofort.

## Anfrage
`POST https://lernen.mathenachhilfe-online.com/sprache/v1/auswerten`

- `Content-Type: multipart/form-data`
- **Keine** Cookies (`credentials: 'omit'`), **keine** Kennung des Kindes, kein Name, keine Geräte-ID, kein Lernstand.

| Feld | Typ | Pflicht | Beispiel | Bedeutung |
|---|---|---|---|---|
| `sprache` | Text | ja | `en` | `en`, `fr`, `es`, `de` (später `it`, `uk`); **nie** `la` |
| `ziel` | Text | ja | `My name is Ruby.` | der bekannte Zielsatz (öffentlicher App-Inhalt, max. 300 Zeichen) |
| `audio` | Datei | ja | `aufnahme.wav` | WAV mono 16 kHz 16 bit **oder** WebM/Opus (`audio/webm;codecs=opus`) |

Grenzen: Aufnahme **höchstens 15 s** und **höchstens 1 MB**. Die App schneidet vorher ab bzw. schickt nichts Größeres.

## Antwort `200 OK` (`application/json`)
```json
{
  "format": 1,
  "text": "my name is ruby",
  "woerter": [ { "w": "my", "ok": true }, { "w": "name", "ok": true }, { "w": "is", "ok": true }, { "w": "ruby", "ok": true } ],
  "treffer": 1.0,
  "dauer_ms": 820
}
```
- `text`: erkannter Text (klein, ohne Satzzeichen genügt).
- `woerter` (optional): Wörter des **Zielsatzes** mit `ok` = erkannt. Fehlt das Feld, gleicht die App selbst ab.
- `treffer` (optional, 0–1): Übereinstimmung mit dem Zielsatz. Fehlt es, rechnet die App selbst.

Die App wertet in beiden Wegen gleich: ★★★ ab 90 %, ★★ ab 70 %, ★ ab 50 %, darunter „Versuch es noch mal“ mit Hörbeispiel.

## Fehler
| Code | Bedeutung | App-Verhalten |
|---|---|---|
| 400 | Feld fehlt/ungültig (`{"fehler":"feld","feld":"sprache"}`) | keine Sterne, neutraler Hinweis |
| 413 | Aufnahme zu groß/lang | keine Sterne, neutraler Hinweis |
| 415 | Audioformat nicht unterstützt | App versucht beim nächsten Mal WAV |
| 429 / 503 | Warteschlange voll bzw. überlastet (Header `Retry-After`) | keine Sterne, Weg 1 wenn verfügbar, sonst „Aus“ für diesen Satz |
| 5xx / Zeitüberschreitung | Server nicht erreichbar | **Rückfall auf Weg 1** (wenn das Gerät es kann), sonst ohne Sterne |

Zeitgrenze: bis etwa 5 s zeigt die App einen Ladekreis („Ruby hört genau hin …“). Nach **8 s** bricht sie ab (Rückfall auf Weg 1, wenn das Gerät es kann, sonst ohne Sterne).
Modell nach Nachmessung mit echten Aufnahmen (Marcel: „turbo für alles“): Whisper large-v3-turbo (MIT, OpenAI) für EN, FR und ES, ca. 3–4 s pro Satz, mit Warteschlange. **Latein: keine Auswertung** (Server lehnt `la` ab, die App fragt nie an).

## Pflichten des Servers (Recht V12)
- Server in Deutschland; Aufnahme **nur im Arbeitsspeicher**, nach der Auswertung **sofort gelöscht**, nie auf Platte, nie in Logs.
- Logs ohne IP-Adresse (oder gekürzt), ohne Audio, ohne erkannten Text. Erlaubt: Zeit, Sprache, Dauer, Statuscode.
- Keine Stimmprofile, kein Wiedererkennen von Sprechern, kein Training mit Aufnahmen.
- CORS: nur `https://lernen.mathenachhilfe-online.com` und `https://lernen-test.mathenachhilfe-online.com`.
- Erkenner: Whisper (MIT-Lizenz), Sprache fest aus `sprache`, Abgleich gegen den Zielsatz.

## Testen
`tools/sprache-mock.mjs` (Mock-Server): antwortet wie oben, mit Modi 413/415/429/503/500, `langsam` (9,5 s) und `schlecht`.
`node tools/web-auswertung.mjs` prüft im Browser mit Netzwerkmitschnitt die Auflagen L1/L6 (Stand 24.11.2026: alle grün):
ohne Wahl „Server“ (keine Auswahl, „Im Gerät“, „Aus“, Flag aus) 0 Anfragen; bei „Server“ genau 1 POST mit den Feldern `sprache`, `ziel`, `audio`,
ohne Cookie, Autorisierung und Referrer, unter 1 MB; bei Fehlern keine Sterne und ein neutraler Hinweis; de/la werden nie gesendet.

## Stand der App (0.5.1)
Client fertig, hinter dem Feature-Flag `AUSWERTUNG_AKTIV = false` (webapp/version.js). Die Auswahl der Eltern („Im Gerät“ – noch nicht verfügbar, „Über unseren Server“, „Aus“) erscheint erst mit dem Flag.
Texte: die endgültige Fassung von Recht (V12 – Auswahl) liegt mir noch nicht wörtlich vor; bis dahin Platzhalter, die nicht sichtbar sind.
