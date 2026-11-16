# Lizenzen fremder Bestandteile in Raumlicht

| Bestandteil | Wozu | Lizenz | Wie eingebunden |
|---|---|---|---|
| three.js 0.170 | 3D-Bühne | MIT | gebündelt in `app/src/main/assets/scene/scene.js` |
| sherpa-onnx 1.13.8 | Sprachsynthese auf dem Gerät (natürliche Stimme) | Apache 2.0 | Bibliothek über JitPack |
| ONNX Runtime | rechnet das Stimm-Modell | MIT | Teil von sherpa-onnx |
| **espeak-ng** | zerlegt Text in Laute | **GPL-3.0** | Teil von sherpa-onnx; Hilfsdaten für Deutsch in `app/src/main/assets/stimme/espeak-ng-data` |
| DejaVu Sans | Schrift der WebApp (kann Mathe-Zeichen wie x₃, ✓, √) | frei (Bitstream Vera / DejaVu-Lizenz) | `web/src/wasmJsMain/resources/fonts` |
| Noto Color Emoji | Emojis in der WebApp | SIL Open Font License 1.1 (Google) | `web/src/wasmJsMain/resources/fonts` |
| Compose Multiplatform, Skiko | Oberfläche der WebApp | Apache 2.0 | Bibliotheken |
| Piper-Stimme „Thorsten“ (de_DE-thorsten-medium) | natürliche deutsche Stimme | Aufnahmen CC0 (Thorsten Müller) | **nicht** in der App enthalten – wird auf Wunsch von Hugging Face geladen |

## Wichtig: GPL-3 durch espeak-ng

Seit Version 2.4.0 enthält die App mit sherpa-onnx auch espeak-ng, das unter der GPL-3.0 steht. Wer die App weitergibt, muss nach verbreiteter Lesart
den Quelltext der ganzen App unter GPL-kompatiblen Bedingungen anbieten. Solange die App privat genutzt oder ihr Quelltext offen ist, ist das unkritisch.
**Vor einer Veröffentlichung im Play Store oder einem Bezahl-Angebot muss das geklärt werden** – entweder Quelltext offenlegen oder die natürliche Stimme
durch eine Lösung ohne espeak-ng ersetzen (neuere sherpa-onnx-Versionen kommen ohne espeak-ng, brauchen dann aber eine andere Lautschrift-Quelle).
Das ist keine Rechtsberatung.
