# Natürliche Stimme „Thorsten“ für Rechenblitz

Rechenblitz kann Aufgaben, Lösungswege, Theorie und Blitz mit der natürlichen Stimme **„Thorsten“** vorlesen (nur Deutsch).
Gerechnet wird **im Browser** – derselbe Code läuft im Web und in der Android-App (Web = Android).

| Baustein | Zweck | Lizenz | Wo |
|---|---|---|---|
| Piper-Stimme `de_DE-thorsten-medium` | Stimm-Modell (63 MB) | Aufnahmen CC0 (Thorsten Müller) | Server, `stimme/` |
| onnxruntime-web 1.20.1 (nur WebAssembly) | rechnet das Modell | MIT (`LICENSE-onnxruntime.txt` liegt daneben) | Server, `stimme/` |
| `stimme-de.json` | Lautschrift-Wörterbuch (3 500 Wörter, Zahlwörter 0–999, Ordnungszahlen, Buchstaben) | eigene Daten | Repo `stimme/build/`, Server `stimme/` |
| espeak-ng (über piper-tts) | erzeugt das Wörterbuch **auf dem Entwicklungsrechner** | GPL-3 | **nicht** in der App, nicht auf dem Server |

**Kein Laden beim Start:** Erst wenn jemand in den Einstellungen „Thorsten – natürliche Stimme“ wählt und „⬇ Stimme laden“ tippt,
holt die App einmalig ca. 75 MB von unserem Server (`CONFIG.voiceBase`, Standard `/stimme/`; in der Android-App die Live-Adresse)
und speichert sie auf dem Gerät (Cache-Speicher). Danach klappt es auch offline. Fehlt der Ordner auf dem Server, bietet die App Thorsten gar nicht an.
Kommt in einem Satz ein Wort vor, das nicht im Wörterbuch steht (z. B. ein selbst eingegebener Name), liest diesen Satz die Gerätestimme.

## Server-Ordner bauen und hochladen

```bash
node stimme/sprechtexte.js        # 1. alle Sprechtexte sammeln (Web-App lokal auf Port 8765)
node stimme/wortliste.js          # 2. Wortliste
python3 -m venv /tmp/piper && /tmp/piper/bin/pip install piper-tts==1.8.0
/tmp/piper/bin/python stimme/lautschrift.py <de_DE-thorsten-medium.onnx.json>   # 3. Lautschrift → build/stimme-de.json
bash stimme/paket.sh              # 4. Ordner build/paket/ (Modell, Rechenwerk, Wörterbuch, stimme.json)
```

Den **Inhalt** von `stimme/build/paket/` nach `https://rechenblitz.mathenachhilfe-online.com/stimme/` hochladen (und für die Testseite nach `…-test…/stimme/`).
Der Server muss für `/stimme/` den Kopf `Access-Control-Allow-Origin: *` senden – die Android-App lädt von einer anderen Herkunft.
Nach jeder Änderung an Aufgaben- oder Sprechtexten Schritte 1–3 wiederholen und `stimme-de.json` neu hochladen.
