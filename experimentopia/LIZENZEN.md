# Lizenzen – Experimentopia

| Baustein | wofür | Lizenz | wie eingebunden |
|---|---|---|---|
| Kotlin, Compose Multiplatform, AndroidX, kotlinx | App und WebApp | Apache 2.0 | Bibliotheken |
| Room | Spielstand (Android) | Apache 2.0 | Bibliothek |
| sherpa-onnx 2.0.1 | natürliche Stimme auf Android, ohne espeak-ng | Apache 2.0 | Bibliothek über JitPack |
| ONNX Runtime | rechnet das Stimm-Modell (Android, in sherpa-onnx) | MIT | Teil von sherpa-onnx |
| onnxruntime-web 1.20.1 | rechnet das Stimm-Modell im Browser | MIT | Dateien in `web/.../resources/ort/` (mit Lizenztext), erst bei Bedarf geladen |
| Piper-Stimme „Thorsten“ (de_DE-thorsten-medium) | natürliche deutsche Stimme | Aufnahmen CC0 (Thorsten Müller), Modell MIT (Piper) | **nicht** in der App – wird auf Wunsch geladen (Web: Stimmen-Server stimmen.mathenachhilfe-online.com, Android: Hugging Face) |
| Noto Color Emoji, DejaVu Sans | Schriften der WebApp (Teilmengen) | OFL bzw. frei | Dateien in `fonts/` |

Selbst erstellt (keine fremden Inhalte): alle Versuche, Texte, Grafiken, **Musik und Klänge** (im Code komponiert und erzeugt) und die Lautschrift-Daten
(einmalig mit dem Entwicklungswerkzeug `tools/stimme/lautschrift.py` erzeugt; espeak-ng läuft dabei nur auf dem Entwicklungsrechner und ist nicht Teil der App).
Keine GPL-Bausteine in App oder WebApp.
