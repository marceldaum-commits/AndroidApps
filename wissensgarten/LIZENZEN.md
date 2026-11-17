# Lizenzen fremder Bestandteile für Brummis Vorlese-Stimme

| Bestandteil | Wozu | Lizenz | Wie eingebunden |
|---|---|---|---|
| sherpa-onnx 2.0.1 | Sprachsynthese auf dem Gerät (natürliche Stimme „Thorsten“); **ohne** espeak-ng | Apache 2.0 | Android: Bibliothek über JitPack (nur arm-Bibliotheken im APK) |
| ONNX Runtime | rechnet das Stimm-Modell | MIT | Android: Teil von sherpa-onnx |
| ONNX Runtime Web 1.22.0 (`onnxruntime-web`) | rechnet das Stimm-Modell im Browser | MIT | WebApp: Laufzeit-Dateien per npm geholt und unter `webapp/ort/` selbst ausgeliefert; **nur bei Bedarf** geladen, nicht im Offline-Vorabspeicher |
| Lautschrift-Wörterbuch | Laute für die Wörter der Wissenspakete und Brummis Sätze | eigene Daten (erzeugt mit piper-tts/espeak-ng am Entwicklungsrechner – beides **nicht** in App oder WebApp) | `content/src/main/assets/stimme/woerter.tsv.gz`, `tokens.txt` |
| Piper-Stimme „Thorsten“ (de_DE-thorsten-medium) | natürliche deutsche Stimme | Aufnahmen CC0 (Thorsten Müller) | **nicht** in App oder WebApp enthalten – wird auf Wunsch von unserem Stimm-Server stimmen.mathenachhilfe-online.com geladen (63 MB, Prüfsumme SHA-256 wird geprüft) |

Die Lizenztexte im Wortlaut liegen bei: `content/src/main/assets/lizenzen.txt` (im APK als Asset, in der WebApp als `lizenzen.txt`).

## Kein GPL-Baustein

espeak-ng (GPL-3.0) setzt normalerweise Text in Lautschrift um. Es steckt **nicht** im Wissensgarten: Die Lautschrift wird vorab am
Entwicklungsrechner erzeugt (`tools/stimme/lautschrift.py`, nutzt dort espeak-ng über piper-tts) und als reine Daten mitgeliefert.
Die Ausgabe eines Programms fällt nach verbreiteter Lesart nicht unter dessen Lizenz; das Werkzeug selbst ist nicht Teil der App.
Wörter, die das Wörterbuch nicht kennt, liest die Stimme des Geräts. Das ist keine Rechtsberatung.
