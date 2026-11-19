# Familien-Speicher (gemeinsame Bibliothek)

`familie.js` ist eine **unveränderte Kopie** der gemeinsamen Client-Bibliothek aller Lern-Apps (Quelle: Mathe-Trainer,
Stand `1f4b377`, sha256 der Originaldatei `3016aabd…`). Neue Versionen kommen über die AppZentrale; dann Datei ersetzen,
erste Zeile (Versionskommentar) anpassen und die Tests laufen lassen:

```
node web/familie/tests/familie.mjs      # erwartet: „familie: 51 Prüfungen bestanden“
```

Die Tests enthalten einen nachgebauten Server (nur zum Testen; den echten `/familie/v1/` betreibt der Leitstand).
Noch nicht in der WebApp eingebunden – die Anbindung (Spieler 0–5 → Kinder, automatisches Sichern, Elternbereich) folgt.
Zusammenführen bei Konflikten: `core/model/FamilyMerge.kt`.

Öffentlicher Wiederherstellungs-Schlüssel (X25519, Marcel 19.11., privater Teil nur in Bitwarden): `REC_PUB = vMrORj9yObM3Ijd9sGStjUB9I3f6ot9E8M-70yGeQ0M` – gilt auch für lernen-test. Seit 1f4b377 fest in familie.js (REC_PUB).

## Pflichttexte für die Familien-Oberfläche (FAMILIEN-SPEICHER.md v1.5, AppZentrale 19.11.)
**Hinweis** (beim ersten Koppeln und im Bereich „Familie“, darunter Link Datenschutzerklärung + „Einverstanden“):
„Eure Lernstände werden verschlüsselt auf unserem Server in Deutschland gespeichert. Der Schlüssel liegt auf euren Geräten. Damit wir euch helfen können, falls ihr den Familien-Zugang verliert, hinterlegen wir zusätzlich eine verschlüsselte Notfall-Kopie. Sie lässt sich nur mit einem getrennt und offline aufbewahrten Schlüssel von Mathenachhilfe Online öffnen. Technisch könnten wir damit eure Lernstände lesen; wir tun das nicht und nutzen ihn nur, wenn ihr uns um Wiederherstellung bittet. Jede Wiederherstellung seht ihr hier im Protokoll.“

**Löschen** (nur hinter dem Tor): Warnung „Das löscht die Familie in ALLEN Lern-Apps: alle Lernstände aller Kinder auf unserem Server, die Notfall-Kopie und das Protokoll. Gekaufte Codes werden wieder frei und lassen sich in einer neuen Familie erneut einlösen. Die Lernstände auf diesem Gerät bleiben erhalten.“ → Familien-Nummer eintippen → `fa.loeschen()` → lokal nur die Familien-Schlüssel entfernen.
Bei 404 auf anderen Geräten: „Diese Familie gibt es nicht mehr. Lernstand bleibt auf diesem Gerät.“ Einzelnes Kind entfernen: noch nicht anbieten.
