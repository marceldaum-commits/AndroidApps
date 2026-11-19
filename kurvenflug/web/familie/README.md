# Familien-Speicher (gemeinsame Bibliothek)

`familie.js` ist eine **unveränderte Kopie** der gemeinsamen Client-Bibliothek aller Lern-Apps (Quelle: Mathe-Trainer,
Stand `ffcfe39`, sha256 der Originaldatei `a67bc3a8…`). Neue Versionen kommen über die AppZentrale; dann Datei ersetzen,
erste Zeile (Versionskommentar) anpassen und die Tests laufen lassen:

```
node web/familie/tests/familie.mjs      # erwartet: „familie: 41 Prüfungen bestanden“
```

Die Tests enthalten einen nachgebauten Server (nur zum Testen; den echten `/familie/v1/` betreibt der Leitstand).
Noch nicht in der WebApp eingebunden – die Anbindung (Spieler 0–5 → Kinder, automatisches Sichern, Elternbereich) folgt.
Zusammenführen bei Konflikten: `core/model/FamilyMerge.kt`.
