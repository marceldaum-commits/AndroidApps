# Natürliche Stimme „Thorsten“ für Rechenblitz

Rechenblitz kann Aufgaben, Lösungswege, Theorie und Blitz mit der natürlichen Stimme **„Thorsten“** vorlesen (nur Deutsch).
Gerechnet wird **im Browser** – derselbe Code läuft im Web und in der Android-App (Web = Android).

## Bausteine und Lizenzen

| Baustein | Zweck | Lizenz | Wo |
|---|---|---|---|
| Piper-Stimme `de_DE-thorsten-medium` (`.onnx` + `.onnx.json`) | Stimm-Modell, ca. 63 MB | Aufnahmen CC0 (Thorsten Müller) | gemeinsamer Stimmen-Server `https://stimmen.mathenachhilfe-online.com/thorsten/` |
| onnxruntime-web 1.20.1, nur WebAssembly (`ort.wasm.min.js`, `ort-wasm-simd-threaded.mjs/.wasm`) | rechnet das Modell, ca. 11 MB | MIT (`LICENSE-onnxruntime.txt` daneben) | `kopfrechnen/stimme/` (Web-Seite; **nicht** in der APK) |
| `stimme-de.json` | Lautschrift-Wörterbuch: 3 507 Wörter (alle Sprechtexte, Zahlwörter 0–999, Ordnungszahlen, Buchstaben) | eigene Daten | `kopfrechnen/stimme/` |
| espeak-ng (über piper-tts) | erzeugt das Wörterbuch **auf dem Entwicklungsrechner** | GPL-3 | **nicht** in der App, nicht auf einem Server |

**Kein espeak-ng in der App, auch nicht als WASM.** Die App setzt Text so in Laute um: Wörterbuch nachschlagen, Zahlen aus Bausteinen zusammensetzen
(„dreizehn+tausend+vierhundert“), Uhrzeiten, Geld und Datum umschreiben. Die Laute werden erst in der App mit der `.onnx.json` des Modells in IDs
übersetzt – das Wörterbuch passt so zu älteren (ohne Doppellaute) und neueren Fassungen der Stimme. **Fehlt ein Wort, liest die Gerätestimme diesen Satz.**

## Ablauf in der App

Einstellungen → Stimme → „Thorsten – natürliche Stimme“ (nur bei Deutsch, und nur wenn der Stimmen-Server erreichbar ist) → Hinweis „ca. 75 MB, am besten im WLAN“
→ „⬇ Stimme laden“ (mit Fortschritt und **Abbrechen**). Das Modell wird mit **SHA-256** geprüft (`CONFIG.voice.sha256`) und dann im Gerätespeicher abgelegt;
danach klappt die Stimme auch offline. Beim Start der App wird nichts geladen. In der Android-App lädt das Rechenwerk von der Live-Seite;
der Abruf läuft mit dem normalen Browser-`fetch` (nicht über die Capacitor-Brücke) – der Stimmen-Server muss deshalb CORS auch für `https://localhost` erlauben.

## Wörterbuch neu erzeugen (nach Änderungen an Aufgaben- oder Sprechtexten)

```bash
node stimme/sprechtexte.js        # 1. alle Sprechtexte sammeln (Web-App lokal auf Port 8765)
node stimme/wortliste.js          # 2. Wortliste
python3 -m venv /tmp/piper && /tmp/piper/bin/pip install piper-tts==1.8.0
/tmp/piper/bin/python stimme/lautschrift.py <de_DE-thorsten-medium.onnx.json>   # 3. Lautschrift → build/stimme-de.json
bash stimme/rechenwerk.sh         # 4. Rechenwerk + Wörterbuch nach kopfrechnen/stimme/
```
