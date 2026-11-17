# Lizenzen fremder Bestandteile in Raumlicht

| Bestandteil | Wozu | Lizenz | Wie eingebunden |
|---|---|---|---|
| three.js 0.170 | 3D-Bühne | MIT | gebündelt in `app/src/main/assets/scene/scene.js` |
| sherpa-onnx 2.0.1 | Sprachsynthese auf dem Gerät (natürliche Stimme); ohne espeak-ng | Apache 2.0 | Bibliothek über JitPack |
| ONNX Runtime | rechnet das Stimm-Modell | MIT | Android: Teil von sherpa-onnx · Web: onnxruntime-web 1.22.0, drei Dateien unter `webapp/ort/`, erst bei Bedarf geladen |
| Lautschrift-Daten | Laute für Lux’ Texte, vorab erzeugt | eigene Daten (erzeugt mit espeak-ng, das **nicht** in der App steckt) | `app/src/main/assets/stimme/*.tsv` |
| AndroidX Core SplashScreen | Startbild der Android-App | Apache 2.0 | Bibliothek |
| Play In-App Updates | Updates über den Store (nur Play-Fassung) | Play Core Software Development Kit Terms of Service (Google) | Bibliothek |
| DejaVu Sans (gekürzt auf die benutzten Zeichen) | Schrift der WebApp (kann Mathe-Zeichen wie x₃, ✓, √) | frei (Bitstream Vera / DejaVu-Lizenz) | `web/src/wasmJsMain/resources/fonts` |
| Noto Color Emoji (gekürzt auf die benutzten Zeichen) | Emojis in der WebApp | SIL Open Font License 1.1 (Google) | `web/src/wasmJsMain/resources/fonts` |
| Compose Multiplatform, Skiko | Oberfläche der WebApp | Apache 2.0 | Bibliotheken |
| Skia (in Skiko) | Zeichenbibliothek der WebApp | BSD (3 Klauseln) | Teil von Skiko |
| js-joda | Datumsrechnung, kommt mit kotlinx-datetime in die WebApp | BSD (3 Klauseln) | gebündelt in `webapp/raumlicht.js` |
| Kotlin, kotlinx (Coroutines, Serialization, Datetime) | Sprache und Grundbibliotheken | Apache 2.0 | Bibliotheken |
| Jetpack / AndroidX (Compose, Room, Lifecycle, Core u. a.) | Oberfläche und Speicher der Android-App | Apache 2.0 | Bibliotheken |
| Ed25519-Prüfung nach TweetNaCl | prüft Freischalt-Codes für den Abi-Pass | gemeinfrei (Public Domain) | eigener Kotlin-Code in `core/model` |
| Piper-Stimme „Thorsten“ (de_DE-thorsten-medium) | natürliche deutsche Stimme | Aufnahmen CC0 (Thorsten Müller) | **nicht** in der App enthalten – wird auf Wunsch vom eigenen Server geladen (stimmen.mathenachhilfe-online.com) |

**Musik und Klänge** sind keine fremden Bestandteile: Sie sind für Raumlicht komponiert und werden vollständig im Programmcode errechnet
(`core/model/…/SpaceMusic.kt`, `MusicSynth.kt`, `Klang.kt`) – ohne Aufnahmen, ohne fremde Samples, ohne Klang-Bibliotheken.

Die Lizenztexte im Wortlaut liegen der App bei: `app/src/main/assets/lizenzen.txt` (Android: Einstellungen → „Fremde Bausteine & Lizenzen“;
WebApp: derselbe Eintrag öffnet `lizenzen.txt`). Bis 2.12.0 fehlten diese Texte in der ausgelieferten App und WebApp.

## Kein GPL-Baustein mehr (seit 2.6.0)

Von 2.4.0 bis 2.5.1 enthielt die Android-App mit sherpa-onnx 1.13.8 auch **espeak-ng (GPL-3.0)**, das Text in Lautschrift umsetzt.
Seit **2.6.0** ist das nicht mehr so: sherpa-onnx 2.0.1 enthält kein espeak-ng, und die Hilfsdaten (`espeak-ng-data`) sind aus der App entfernt.
Die Lautschrift für Lux’ Texte wird stattdessen vorab am Entwicklungsrechner erzeugt (`tools/stimme/lautschrift.py`, nutzt dort espeak-ng über piper-tts)
und als reine Daten mitgeliefert (`app/src/main/assets/stimme/saetze.tsv`, `woerter.tsv`). Die Ausgabe eines Programms fällt nach verbreiteter Lesart
nicht unter dessen Lizenz; das Werkzeug selbst ist nicht Teil der App. Wörter, die das Wörterbuch nicht kennt, liest die Stimme des Geräts.
Der GitHub-Lauf prüft, dass das Store-Paket keine espeak-ng-Daten enthält. Das ist keine Rechtsberatung.
