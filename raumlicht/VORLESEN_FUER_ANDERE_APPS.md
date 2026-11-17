# Vorlesen mit der Stimme „Thorsten“ – Anleitung für andere Apps

Stand: Raumlicht 2.16.1 vom 17.11.2026. Geschrieben auf Wunsch des Leitstands, von Marcel bestätigt.

**Was es ist:** Die App liest Texte mit der natürlichen deutschen Stimme „Thorsten“ vor – auf dem Gerät, ohne Internet, **ohne GPL-Baustein**.
Der Trick: Die Lautschrift (aus Text werden Laute) entsteht vorab am Entwicklungsrechner und wird als reine Daten mitgeliefert.
In der App rechnet nur noch das Stimm-Modell.

**Ehrlicher Stand:**
- **Android:** gebaut und in den automatischen Tests grün. **Auf einem echten Gerät hat es noch niemand gehört.** Bitte vor dem Übernehmen einmal auf einem Handy prüfen.
- **WebApp:** seit 2.16.0 gebaut und in Chromium geprüft (Abschnitt 6). **Auf einem echten Handy oder iPad hat es noch niemand gehört.**

## 1. Bausteine und Lizenzen

| Baustein | Wozu | Lizenz | Wo |
|---|---|---|---|
| sherpa-onnx **2.0.1** | rechnet das Stimm-Modell (VITS/Piper) auf dem Gerät | Apache 2.0 | `com.github.k2-fsa.sherpa-onnx:sherpa-onnx:v2.0.1@aar` über JitPack |
| ONNX Runtime | steckt in sherpa-onnx | MIT | – |
| Stimm-Modell „Thorsten“ (`de_DE-thorsten-medium.onnx`) | die Stimme | Aufnahmen CC0 (Thorsten Müller) | **nicht in der App** – wird auf Wunsch geladen |
| `tokens.txt` | Zeichenliste des Modells | gehört zum Modell | `app/src/main/assets/stimme/` |
| Lautschrift-Daten `saetze.tsv`, `woerter.tsv` | Laute für die Texte der App | eigene Daten | `app/src/main/assets/stimme/` |
| espeak-ng (**GPL-3**) | erzeugt die Lautschrift | GPL-3 | **nur am Entwicklungsrechner** (über `pip install piper-tts`), **nie in der App** |

**Wichtig zur Version:** sherpa-onnx **1.x enthält espeak-ng** fest eingebaut (GPL-3 – beträfe die ganze App). Erst **2.0.1** kommt ohne aus und nimmt fertige Laute entgegen.
Der GitHub-Lauf von Raumlicht prüft bei jedem Bau, dass das Store-Paket keine espeak-ng-Daten enthält (`.github/workflows/raumlicht-release.yml`, Suche nach „espeak“).
Die Ausgabe eines Programms fällt nach verbreiteter Lesart nicht unter dessen Lizenz – das ist keine Rechtsberatung (siehe `KLAERUNGSLISTE-RECHT.md`).

## 2. Modell: Quelle und Größe

- **Quelle: der eigene Server**, gemeinsam für alle Apps (`https://stimmen.mathenachhilfe-online.com/thorsten/`):
  - **für sherpa-onnx (Android):** `sherpa/de_DE-thorsten-medium.onnx` – 63 201 421 Byte, SHA-256 `1c305abe13dda2f50a9783da3e7645396833cdf2d09f24fa9a823c91293d65a6`, dazu `sherpa/tokens.txt`. Raumlicht lädt diese Datei seit 2.16.1.
  - **ursprüngliche Piper-Datei (Browser mit ONNX Runtime Web):** `de_DE-thorsten-medium.onnx` – 63 201 294 Byte, SHA-256 `7e64762d8e5118bb578f2eea6207e1a35a8e0c30595010b666f983fc87bb7819`.
- **Der Unterschied:** sherpa-onnx liest sieben Angaben aus dem Modell (`model_type=vits`, `comment=piper`, `language`, `voice`, `has_espeak`, `n_speakers`, `sample_rate`). Die sherpa-Datei ist die Piper-Datei
  plus genau 127 Byte mit diesen Angaben am Ende (am 17.11.2026 Byte für Byte verglichen). Mit der ursprünglichen Datei startet sherpa-onnx nicht; ONNX Runtime Web braucht die Angaben nicht.
- Die App lädt die Datei **nur auf Knopfdruck** (Einstellungen → „Stimme laden (63 MB)“), prüft Größe und Prüfsumme und legt sie in den App-Speicher (`filesDir/stimme/`).
  Man kann sie wieder entfernen. Ohne Modell liest die Geräte-Stimme.
- Für Store und Datenschutz: einmaliger Download vom eigenen Server, es wird nichts gesendet; der Server sieht die IP-Adresse. In der Datenschutzerklärung nennen.

## 3. So entsteht die Lautschrift (am Entwicklungsrechner)

1. **Texte sammeln.** Ein Test schreibt alle Texte, die vorgelesen werden können, in eine Datei: `core/model/src/jvmTest/.../SpeechDumpTest.kt` → `core/model/build/sprechtexte.txt`.
   Je Zeile `fest⇥Text` (steht wörtlich fest) oder `wort⇥Text` (Beispiel für veränderliche Texte, liefert nur Wörter).
   Vorher macht `Speech.clean()` aus dem angezeigten Text einen sprechbaren Satz (Formelzeichen → Wörter). Das braucht jede App für ihre eigenen Zeichen.
2. **Laute erzeugen.** `tools/stimme/lautschrift.py` (Python, braucht `pip install piper-tts`) schreibt
   - `saetze.tsv`: feste Texte als ganze Sätze umgesetzt (beste Betonung) – `Text⇥Laute Satz 1|Laute Satz 2`
   - `woerter.tsv`: Wörterbuch für veränderliche Texte – `wort⇥Laute`, dazu die Zahlen 0 bis 999
   Dauer: wenige Sekunden. Bei Raumlicht: 610 Sätze, 2101 Wörter, zusammen rund 220 kB.
3. **Absichern.** `LautschriftTest` schlägt an, wenn ein fester Text nicht in `saetze.tsv` steht oder ein Wort fehlt. Dann Schritt 1 und 2 neu laufen lassen.
   **Nach jeder Textänderung nötig** – sonst liest für diese Texte die Geräte-Stimme.

## 4. Einbindung Android

Vorbild: `app/src/main/java/de/raumlicht/app/ui/fx/NaturalVoice.kt` und `core/model/.../Lautschrift.kt`.

- **Gradle** (`app/build.gradle.kts`, `settings.gradle.kts`):
  ```kotlin
  maven("https://jitpack.io") { content { includeGroup("com.github.k2-fsa.sherpa-onnx") } }
  implementation("com.github.k2-fsa.sherpa-onnx:sherpa-onnx:v2.0.1@aar")
  ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
  android.packaging.jniLibs.excludes += listOf("**/libsherpa-onnx-c-api.so", "**/libsherpa-onnx-cxx-api.so")
  android.packaging.jniLibs.useLegacyPackaging = true   // Download weniger als halb so groß
  ```
- **Laden** (`VoicePackLoader`): Download in eine Temp-Datei, Fortschritt in Prozent, abbrechbar, Größe + SHA-256 prüfen, erst dann umbenennen.
- **Starten** (`NaturalVoice.load()`, im Hintergrund-Thread): Hilfsdaten aus den Assets ins Stimm-Verzeichnis kopieren, `Lautschrift` einlesen,
  `OfflineTts(null, OfflineTtsConfig(model = OfflineTtsModelConfig(vits = OfflineTtsVitsModelConfig(model = …, tokens = …), numThreads = 2, provider = "cpu")))`.
- **Sprechen** (`say()`): `Speech.clean(text)` → `Lautschrift.of(clean)` liefert die Laute je Satz, oder `null`, wenn ein Wort fehlt.
  Dann `generateWithConfigAndCallback(clean, GenerationConfig(speed = Tempo, phonemeCodepoints = …))` und die Tonstücke in einen `AudioTrack` (Float, Mono) schreiben.
  Ein neuer Satz unterbricht den alten (Zähler `turn`).
- **Rückfall:** Fehlt ein Wort, liest diesen einen Text die Geräte-Stimme (`TextToSpeech`). Lässt sich das Modell nicht starten, ebenso – mit Hinweis in den Einstellungen.
- **Einstellungen:** Vorlesen ein/aus (von Haus aus **aus**), Geräte-Stimme oder „Thorsten“, Sprechtempo 70–130 %, Knopf „So klingt es“, Lautstärke; die Wahl wird gemerkt.
  Musik wird unter der Stimme leiser (`onSpeaking` → `music.duck`).

## 5. Größen

- App-Paket: Die Raumlicht-APK hat insgesamt 21 MB – mit sherpa-onnx für zwei Prozessor-Arten, komprimiert eingepackt (die beiden Zeilen oben).
- Lautschrift-Daten: rund 220 kB. Modell: 63 MB, nur auf Wunsch.

## 6. Einbindung WebApp (seit Raumlicht 2.16.0, Vorbild: Wissensgarten)

Im Browser rechnet **ONNX Runtime Web** (MIT) das Modell direkt – ohne sherpa-onnx. Deshalb genügt dort die ursprüngliche Piper-Datei vom Server, ohne die 127 angehängten Byte.

- **Bausteine:** `onnxruntime-web` 1.22.0 als npm-Abhängigkeit, aber **nicht eingebunden**: Der Bau kopiert nur drei Dateien nach `webapp/ort/`
  (`ort.wasm.min.js`, `ort-wasm-simd-threaded.mjs`, `ort-wasm-simd-threaded.wasm`, zusammen 11 MB). Sie werden erst geladen, wenn Thorsten zum ersten Mal spricht – der Start der WebApp wird nicht langsamer.
- **`web/src/wasmJsMain/resources/rl-stimme.js`:** laden (Fortschritt, abbrechbar), SHA-256 prüfen (`crypto.subtle`), in einem eigenen Browser-Speicher ablegen (Cache Storage `raumlicht-stimme`),
  Modell starten, Laut-Nummern rechnen, per Web Audio abspielen (Satz für Satz, ein neuer Satz unterbricht den alten). Der Service Worker lässt diesen Speicher bei Updates stehen.
- **Kotlin-Seite** (`web/.../Thorsten.kt`): `Speech.clean` → `Lautschrift.of` (dieselben `saetze.tsv`/`woerter.tsv` wie Android, als `.txt` nach `webapp/stimme/` kopiert – der Abgleich zur Testadresse lässt `.tsv` nicht durch) → `PiperIds.encode`
  (Anfang „^“, zwischen allen Lauten „_“, Ende „$“, Nummern aus `tokens.txt`) → `rl-stimme.js`. Eingaben des Modells: `input` (int64), `input_lengths`, `scales = [0,667; Tempo; 0,8]`.
- **Rückfall:** unbekanntes Wort oder Modell startet nicht → die Stimme des Geräts (`speechSynthesis`, **nur Stimmen mit `localService`** – sonst kann der Text an den Browser-Hersteller gehen).
- **Server:** CORS muss die Web-Adresse erlauben (für `*.mathenachhilfe-online.com` und `localhost` eingerichtet).
- **Gemessen in Chromium am Rechner:** 1,3 s Sprache in 0,3 s gerechnet; erster Satz mit Start des Bausteins knapp 2 s; 84 Sätze der App ohne Fehler, der längste (525 Laut-Nummern) in 2,5 s.
  **Nicht gemessen:** Handys und iPad (Rechenzeit, Speicher). Ohne `crossOriginIsolated` rechnet der Baustein mit einem Thread.
- **Test:** `scene/test/web-stimme.mjs` (laden, Prüfsumme, Satz rechnen, Neuladen, Entfernen).

## 7. Stolpersteine

- **Nicht sherpa-onnx 1.x nehmen** (GPL über espeak-ng, siehe oben). Nach einem Update alte `espeak-ng-data` aus dem App-Speicher löschen.
- **`Speech.clean()` ist die halbe Arbeit.** Formelzeichen müssen zu Wörtern werden, bevor Laute entstehen. Raumlicht hat dabei am 17.11. viele Fehler gefunden
  (Betragsstriche, „′“, „>“, Brüche fielen weg). Jede App braucht dafür eigene Tests mit echten Texten.
- **Zahlen:** Das Wörterbuch kennt 0 bis 999 und setzt größere Zahlen und Kommazahlen daraus zusammen („zwölf Komma fünf“).
- **Feste Sätze klingen besser** als Wort für Wort gesetzte. So viele Texte wie möglich als „fest“ sammeln.
- **Test-Abhängigkeit in Gradle:** Der Test, der die `.tsv`-Dateien prüft, muss sie als Eingabe kennen (`inputs.dir(...)`), sonst gilt er nach einer Änderung als „schon gelaufen“.
- **16-KB-Speicherseiten** (Play-Store-Vorgabe): Die Bibliotheken von sherpa-onnx 2.0.1 sind dafür ausgerichtet; der Raumlicht-Bau prüft das Store-Paket.
- **Kinder-Apps:** Vorlesen von Haus aus aus, klar beschrifteter Download, keine Online-Stimmen.

## 8. Dateien zum Abschauen

| Datei | Inhalt |
|---|---|
| `tools/stimme/lautschrift.py` | erzeugt `saetze.tsv` und `woerter.tsv` |
| `core/model/src/commonMain/kotlin/de/raumlicht/core/model/Speech.kt` | Text → sprechbarer Satz |
| `core/model/src/commonMain/kotlin/de/raumlicht/core/model/Lautschrift.kt` | Satz → Laute (gemeinsamer Code, auch für Web) |
| `core/model/src/jvmTest/kotlin/de/raumlicht/core/model/SpeechDumpTest.kt`, `LautschriftTest.kt`, `SpeechTest.kt` | Texte sammeln und absichern |
| `app/src/main/java/de/raumlicht/app/ui/fx/NaturalVoice.kt` | Laden, Starten, Sprechen, Rückfall |
| `app/src/main/java/de/raumlicht/app/ui/fx/Voice.kt` | Geräte-Stimme (nur lokale, beste Qualität zuerst) |
| `LIZENZEN.md`, `app/src/main/assets/lizenzen.txt` | Lizenzliste und -texte |
