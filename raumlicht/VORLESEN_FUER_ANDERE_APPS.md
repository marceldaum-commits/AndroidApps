# Vorlesen mit der Stimme „Thorsten“ – Anleitung für andere Apps

Stand: Raumlicht 2.15.1 vom 17.11.2026. Geschrieben auf Wunsch des Leitstands, von Marcel bestätigt.

**Was es ist:** Die App liest Texte mit der natürlichen deutschen Stimme „Thorsten“ vor – auf dem Gerät, ohne Internet, **ohne GPL-Baustein**.
Der Trick: Die Lautschrift (aus Text werden Laute) entsteht vorab am Entwicklungsrechner und wird als reine Daten mitgeliefert.
In der App rechnet nur noch das Stimm-Modell.

**Ehrlicher Stand:**
- **Android:** gebaut und in den automatischen Tests grün. **Auf einem echten Gerät hat es noch niemand gehört.** Bitte vor dem Übernehmen einmal auf einem Handy prüfen.
- **WebApp:** „Thorsten“ gibt es dort **noch nicht**. Die WebApp liest mit der Geräte-Stimme des Browsers (nur Stimmen, die auf dem Gerät eingebaut sind). Abschnitt 6 beschreibt, was zu bauen wäre.

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

- **Quelle (seit Raumlicht 2.15.1): der eigene Server**, gemeinsam für alle Apps: `https://stimmen.mathenachhilfe-online.com/thorsten/de_DE-thorsten-medium.onnx`
  – 63 201 294 Byte, SHA-256 `7e64762d8e5118bb578f2eea6207e1a35a8e0c30595010b666f983fc87bb7819`. Das ist die ursprüngliche Piper-Datei.
- **Achtung, wichtig für sherpa-onnx:** Der Baustein liest sieben Angaben aus dem Modell (`model_type=vits`, `comment=piper`, `language`, `voice`, `has_espeak`, `n_speakers`, `sample_rate`).
  Die ursprüngliche Piper-Datei enthält sie nicht. Lösung ohne zweite Datei auf dem Server: **Die App hängt nach dem Laden 127 Byte an** (`VoicePack.TAIL` in `NaturalVoice.kt` –
  das sind genau diese Angaben als ONNX-Metadaten). Ergebnis ist Byte für Byte die für sherpa-onnx aufbereitete Fassung:
  63 201 421 Byte, SHA-256 `1c305abe13dda2f50a9783da3e7645396833cdf2d09f24fa9a823c91293d65a6` (dieselbe Datei wie bei Hugging Face unter `csukuangfj/vits-piper-de_DE-thorsten-medium`). Am 17.11.2026 nachgerechnet.
  Geprüft wird zweimal: die geladene Datei gegen die erste Prüfsumme, die fertige gegen die zweite.
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

## 6. WebApp – was zu bauen wäre (noch nicht gebaut)

- sherpa-onnx gibt es als WebAssembly-Fassung; das Modell (63 MB) müsste der Browser laden und im Cache halten. Die Lautschrift-Daten und `Lautschrift.kt` sind gemeinsamer Code und sofort nutzbar.
- Offene Fragen: Ladezeit und Speicher auf Handys, Rechenzeit im Browser (ohne eigenen Thread ruckelt die Oberfläche), Ausliefern von huggingface.co im Browser (CORS) oder vom eigenen Server.
- Bis dahin in der WebApp: `speechSynthesis` des Browsers, **nur Stimmen mit `localService`** (sonst kann der Text an den Browser-Hersteller gehen – und „keine Daten an einen Server“ stimmt nicht mehr).

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
