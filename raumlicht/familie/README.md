# Familien-Speicher – Baustein für Raumlicht (noch AUS)

`familie.js` @ 1f4b377 (Quelle: mathe-trainer, `app/js/familie.js`), **unverändert übernommen**.
sha256 der Datei: `3016aabd994d9f00ddead3c2246b05e5dd3ec9106669638396cc42400283e640` – prüfen mit `sha256sum familie/familie.js`.
Der Herkunftsvermerk steht hier und nicht in der Datei, damit die Prüfsumme stimmt. Nicht von Hand ändern; neue Fassungen kommen vom Mathe-Trainer.

- Test: `node familie/tests/familie.mjs` (Node 20+) → „familie: 51 Prüfungen bestanden“. Im Test ist nur Zeile 5 (Pfad) angepasst.
- Server Test: `https://lernen-test.mathenachhilfe-online.com/familie/v1/` – Live antwortet bis zu Marcels Freigabe mit 404 `noch_nicht_live`.
- Verbindliches Format: `ops/familie/SCHNITTSTELLE.md` (Repo website_backup, Branch ops-status).
- Raumlicht: `core/model/…/Familie.kt` (`ENABLED = false`) trennt Lernstand des Kindes und Rechte. Die Datei liegt bewusst **nicht** im Web-Paket; sie wird erst mit der Anbindung ausgeliefert.
