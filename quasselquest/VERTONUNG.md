# Vertonung auf dem Server (Kokoro-82M) – Textliste für den Leitstand

Stand 19.11.2026. Marcel: Englisch, Französisch und Spanisch (später Italienisch) werden mit **Kokoro-82M** neu vertont
(festgelegt: EN `af_heart`, FR `ff_siwis`, ES `ef_dora`, IT `if_sara`; Tempo normal **1,0**, langsam **0,8**).
Deutsch steht schon in derselben Liste (`stimme: null`) und wird vertont, sobald der Leitstand die Stimme gebaut hat (Chatterbox/Zonos).
Kokoro läuft **nur auf unserem Server** (Leitstand, Nachtlauf). In die App kommen nur die fertigen MP3. Kokoro, misaki und espeak-ng
werden **nicht** mit der App ausgeliefert. Deutsch bleibt bis dahin ohne Ton (nur Text).

## Datei
`quasselquest/webapp/vorlesen/textliste.json`, erzeugt mit `python3 tools/textliste.py`. Das Skript wird nach jeder Inhaltsänderung neu ausgeführt.

```json
{
  "format": 1, "app": "quasselquest", "modell": "Kokoro-82M (hexgrad, Apache-2.0)",
  "stimmen": { "en": { "stimme": "af_heart", "kokoro_lang": "en-us" }, "fr": { "stimme": "ff_siwis", "kokoro_lang": "fr-fr" },
               "es": { "stimme": "ef_dora", "kokoro_lang": "es" }, "it": { "stimme": "if_sara", "kokoro_lang": "it" },
               "de": { "stimme": null, "hinweis": "Stimme folgt (Chatterbox/Zonos, Leitstand)" } },
  "mp3": { "kanaele": 1, "abtastrate": 24000, "bitrate": "48k", "id3_comment": "KI-generiert (synthetische Stimme, <Modell>)",
           "stille_rand_kuerzen": true, "lautheit_lufs": -18, "vorlauf_s": 0.12 },
  "eintraege": [
    { "id": "3f2a…(20 Zeichen)", "sprache": "en", "text": "Give Mia three fish.", "sprechtext": "Give Mia three fish.",
      "rollen": ["ruby"],
      "fassungen": [ { "art": "normal",  "speed": 1.0, "datei": "audio/server/en/3f2a….mp3" },
                     { "art": "langsam", "speed": 0.8, "datei": "audio/server/en/3f2a…-langsam.mp3" } ] }
  ]
}
```

| Feld | Bedeutung |
|---|---|
| `id` | fester Schlüssel: die ersten 20 Hex-Zeichen von SHA-1(`sprache|sprechtext`, normalisiert). Gleicher Text ergibt dieselbe Datei. |
| `text` | Text, wie er in der App steht (nur zur Kontrolle) |
| `sprechtext` | **dieser** Text wird vertont: Ziffern als Wörter, Pausen in Zahlenreihen (gleiche Aufbereitung wie bisher in tools/vertonen.py) |
| `rollen` | Figuren, die den Satz sprechen. Zurzeit spricht in jeder Sprache **eine** Stimme alle Figuren (Marcel: „nicht mit den Stimmen switchen“). |
| `fassungen[].speed` | Kokoro `speed`. Normal 1,0 (Marcel), Einzelwörter etwas langsamer (1,0 ÷ 1,15 = 0,87), langsam (🐢) 0,8. |
| `fassungen[].datei` | Pfad relativ zu `quasselquest/webapp/`, an dem die MP3 abgelegt wird |

## Rückgabe an Quasselquest
- Alle MP3 unter `audio/server/<sprache>/` mit den Dateinamen aus der Liste, dazu ID3-Kommentar „KI-generiert (synthetische Stimme, Kokoro-82M)“ (Deutsch später mit dem dann verwendeten Modell).
- Optional eine Datei `audio/server/pruefung.json` mit `{ "<id>": { "gehoert": "…", "treffer": 0.97 } }`, falls der Server selbst nachhört.
  Sonst übernehme ich die Hörprüfung (Whisper, wie bisher).
- Ich baue die Dateien in `audio/index.json` ein, prüfe sie (Hörprüfung, Browser-Test) und melde die Version (geplant 0.5.1).

## Umfang (Stand 0.5.0)
Englisch 721, Französisch 838, Spanisch 801, Deutsch 185 Texte, jeweils normal und langsam, zusammen 2545 Texte und 5090 MP3-Dateien.
