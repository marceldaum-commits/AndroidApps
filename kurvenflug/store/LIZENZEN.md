# Lizenzprüfung Kurvenflug (Stand 0.21.0)

Ermittelt aus `./gradlew :app:dependencies --configuration playReleaseRuntimeClasspath` (51 Gruppen) und dem Web-Build.
**Ergebnis: alle Lizenzen erlauben die Veröffentlichung im Play Store und im Web. Kein Copyleft (GPL/LGPL) dabei.**

| Komponente | Lizenz | Pflicht | erfüllt |
|---|---|---|---|
| AndroidX (activity, annotation, arch.core, autofill, collection, compose.*, concurrent, core, customview, datastore, emoji2, fragment, glance, graphics, interpolator, lifecycle, loader, navigation, profileinstaller, room, savedstate, sqlite, startup, tracing, versionedparcelable, viewpager, work) | Apache 2.0 | Lizenzhinweis | Einstellungen → Open-Source-Lizenzen |
| Kotlin, kotlinx.coroutines, kotlinx.serialization, JetBrains Annotations | Apache 2.0 | Lizenzhinweis | ✓ |
| Compose Multiplatform (org.jetbrains.compose.*, org.jetbrains.androidx.*) | Apache 2.0 | Lizenzhinweis | ✓ |
| Guava (com.google.guava, über Room/WorkManager) | Apache 2.0 | Lizenzhinweis | ✓ |
| JSpecify | Apache 2.0 | Lizenzhinweis | ✓ |
| Play In-App Updates (com.google.android.play:app-update, core-common) – nur Play-Variante | Play Core SDK Terms of Service | Nutzung nur mit Google Play | ✓ (nur Play-Build) |
| Google Play services Tasks/Basement (com.google.android.gms, über Play Core) – nur Play-Variante | Android SDK License / Google APIs Terms | – | ✓ |
| **Web:** Skia/Skiko (in der .wasm) | BSD-3-Clause | Hinweis | `kurvenflug.js.LICENSE.txt`, Einstellungen |
| **Web:** Noto Color Emoji (zugeschnitten) | SIL OFL 1.1 | Lizenztext beilegen | `fonts-lizenzen.txt` |
| **Web:** DejaVu Sans (zugeschnitten) | Bitstream Vera / DejaVu (frei) | Lizenztext beilegen | `fonts-lizenzen.txt` |

Eigene Inhalte (Level, Texte, Grafiken, im Code erzeugte Töne und Musik): eigene Werke, keine fremden Assets.
