# Dateien für www.mathenachhilfe-online.com

Diese zwei Dateien machen Duell-Links anklickbar (`https://www.mathenachhilfe-online.com/duell/#WG1-…`).

| Datei | Hochladen nach | Zweck |
| --- | --- | --- |
| `.well-known/assetlinks.json` | `https://www.mathenachhilfe-online.com/.well-known/assetlinks.json` | Android prüft damit, dass die Domain der App vertraut („App Links“). Ist die App installiert, öffnet der Link direkt das Duell. |
| `duell/index.html` | `https://www.mathenachhilfe-online.com/duell/` | Ausweich-Seite für alle ohne App: zeigt Thema und Punktestand, Code kopieren, Anleitung. |

**Hochladen:** per SFTP/FTP ins Hauptverzeichnis der WordPress-Installation (dort, wo `wp-config.php` liegt) –
die Ordner `.well-known` und `duell` einfach daneben anlegen. WordPress wird dadurch nicht verändert.

**Wichtig**
- `assetlinks.json` muss **ohne Weiterleitung** und mit Status 200 erreichbar sein (Content-Type `application/json`).
- Der Fingerabdruck in `assetlinks.json` gehört zum aktuellen Signatur-Schlüssel. Kommt ein neuer Schlüssel dazu
  (eigener Upload-Schlüssel, Google Play App Signing), muss sein SHA-256-Fingerabdruck **zusätzlich** in die Liste.
- Die Paket-Titel in `duell/index.html` werden aus `content/src/main/assets/packs` erzeugt – bei neuen Paketen neu erzeugen.
- Datenschutz: Der Code steht hinter `#` und wird nie an den Server gesendet.

**Prüfen nach dem Hochladen**
```
https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://www.mathenachhilfe-online.com&relation=delegate_permission/common.handle_all_urls
```
Auf dem Handy (Android 12+): App-Info → „Standardmäßig öffnen“ → `www.mathenachhilfe-online.com` sollte als bestätigt erscheinen.
Falls nicht: App einmal neu installieren (Android prüft beim Installieren).
