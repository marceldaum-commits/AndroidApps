# Lizenzen fremder Bestandteile in Raumlicht

| Bestandteil | Wozu | Lizenz | Wie eingebunden |
|---|---|---|---|
| three.js 0.170 | 3D-Bühne | MIT | gebündelt in `app/src/main/assets/scene/scene.js` |
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

**Musik und Klänge** sind keine fremden Bestandteile: Sie sind für Raumlicht komponiert und werden vollständig im Programmcode errechnet
(`core/model/…/SpaceMusic.kt`, `MusicSynth.kt`, `Klang.kt`) – ohne Aufnahmen, ohne fremde Samples, ohne Klang-Bibliotheken.

Die Lizenztexte im Wortlaut liegen der App bei: `app/src/main/assets/lizenzen.txt` (Android: Einstellungen → „Fremde Bausteine & Lizenzen“;
WebApp: derselbe Eintrag öffnet `lizenzen.txt`). Bis 2.12.0 fehlten diese Texte in der ausgelieferten App und WebApp.

## Kein GPL-Baustein (Stand 2.17.0)

Von 2.4.0 bis 2.5.1 enthielt die Android-App über eine Sprach-Bibliothek auch **espeak-ng (GPL-3.0)**. Von 2.6.0 bis 2.16.4 war espeak-ng nicht mehr enthalten
(neuere Fassung der Bibliothek, Lautschrift als mitgelieferte Daten). Seit **2.17.0** gibt es in der App gar keine Sprachsynthese mehr: Die Sprach-Bibliothek, ONNX Runtime,
die Lautschrift-Daten und die frühere ladbare Stimme sind entfernt. Das Vorlesen ist abgeschaltet; wenn es mit fertigen Sprachdateien aus unserem Tonstudio zurückkommt (KI-generierte Stimme, eigener Server), steht hier der Eintrag zur Stimme samt Lizenzangaben in dem Wortlaut, den der Raum „Recht“ vorgibt. Der GitHub-Lauf prüft weiter, dass das Store-Paket keine espeak-ng-Daten enthält. Das ist keine Rechtsberatung.
