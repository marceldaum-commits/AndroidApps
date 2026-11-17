# Stimme „Thorsten“ – Lautschrift-Wörterbuch erzeugen

Brummis natürliche Stimme „Thorsten“ (Piper-Modell `de_DE-thorsten-medium`) braucht Lautschrift statt Buchstaben.
App und WebApp enthalten dafür **keinen** Sprach-Baustein (espeak-ng ist GPL). Stattdessen liegt ein kleines Wörterbuch bei:

| Datei | Inhalt |
|---|---|
| `content/src/main/assets/stimme/woerter.tsv.gz` | `wort<Tab>Laute`, gzip (ca. 90 KB, ca. 10 600 Wörter + Zahlen 0–999) |
| `content/src/main/assets/stimme/tokens.txt` | Zeichenliste der Stimme (Laut → Nummer im Modell) |

Die App bereitet einen Text auf (`SprechText.clean`), setzt ihn Wort für Wort aus dem Wörterbuch zusammen (`Lautschrift`)
und rechnet daraus mit dem Modell die Sprache. Fehlt **ein** Wort, liest die Stimme des Geräts diesen einen Text.

## Neu erzeugen (nach neuen oder geänderten Paketen bzw. Brummi-Sätzen)

```bash
cd wissensgarten
pip install piper-tts                      # bringt espeak-ng mit – nur am Entwicklungsrechner
python3 tools/stimme/lautschrift.py --plus /pfad/zu/wissensgarten-plus
./gradlew :content:testDebugUnitTest       # StimmeAbdeckungTest: meldet, wenn viele Wörter fehlen
```

Wörter-Quellen: alle freien Pakete (`content/src/main/assets/packs/*.json`: Titel, Beschreibung, Einführung, Frage, Antwort, info),
die Plus-Pakete (nur die Wörter – die Pakete selbst kommen nicht ins Repo) und alle Zeichenketten im Quellcode von
`shared/` und `core/model/` (Brummis Sätze, Oberflächen-Texte). Fremdsprachige Antworten (Englisch, Französisch, Spanisch)
werden ausgelassen – die liest immer eine Gerätestimme der passenden Sprache.

Abkürzungen in Großbuchstaben (ATP, EU) werden buchstabiert, längere mit Selbstlaut (DSGVO, NATO) liest espeak, wie es sie kennt;
Jahreszahlen 1100–1999 liest die App als „neunzehnhundertvierzehn“.

## WebApp prüfen (Headless-Chromium)

```bash
./gradlew :webApp:exportWebApp :content:testDebugUnitTest     # schreibt webapp/ und content/build/hoerprobe.txt
python3 -m http.server 18931 --directory webapp &
node tools/stimme/web-test.js http://127.0.0.1:18931 /pfad/de_DE-thorsten-medium.onnx content/build/hoerprobe.txt /tmp/hoerprobe
```
Der Test bedient den Hugging-Face-Download mit der lokalen Modelldatei, rechnet die Hörproben mit ONNX Runtime Web
(schreibt WAV-Dateien) und prüft, dass `ort/` erst bei Bedarf geladen wird.

## Modell

Das Modell (63 MB) wird nicht mitgeliefert, sondern auf Wunsch geladen:
`https://huggingface.co/csukuangfj/vits-piper-de_DE-thorsten-medium/resolve/main/de_DE-thorsten-medium.onnx`,
SHA-256 `1c305abe13dda2f50a9783da3e7645396833cdf2d09f24fa9a823c91293d65a6` (wie Raumlicht).
Eingaben: `input` (Laut-Nummern: `^`, dann nach jedem Laut `_`, am Ende `$`), `input_lengths`, `scales` = [0,667; 1/Tempo; 0,8].
