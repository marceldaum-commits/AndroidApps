# Familien-Speicher – Baustein für Raumlicht (Teststufe; live noch AUS)

`familie.js` @ e9993ec (Quelle: mathe-trainer, `app/js/familie.js`), **unverändert übernommen** – liegt in `web/src/wasmJsMain/resources/familie.js` und wird mit der WebApp ausgeliefert.
sha256 der Datei: `3f7ec567e4849c3870be8398aa72894475a55df672f79f22e536fdb8f209f5f9` – prüfen mit `sha256sum web/src/wasmJsMain/resources/familie.js`.
Der Herkunftsvermerk steht hier und nicht in der Datei, damit die Prüfsumme stimmt. Nicht von Hand ändern; neue Fassungen kommen vom Mathe-Trainer.

- Test: `node familie/tests/familie.mjs` (Node 20+) → „familie: 51 Prüfungen bestanden“. Im Test ist nur Zeile 5 (Pfad) angepasst.
- Server Test: `https://lernen-test.mathenachhilfe-online.com/familie/v1/` – Live antwortet bis zu Marcels Freigabe mit 404 `noch_nicht_live`.
- Verbindliches Format: `ops/familie/SCHNITTSTELLE.md` (Repo website_backup, Branch ops-status).
- Raumlicht: `core/model/…/Familie.kt` (Logik, `ENABLED = false` = live unsichtbar), Brücke `familieRuf` in `web/…/Browser.kt` (lädt den Baustein erst bei Bedarf), Seite `shared/…/settings/FamilieScreen.kt` hinter dem Eltern-Tor. Browser-Prüfung: `node scene/test/web-familie.mjs <webapp> <out>`.

**Neue Fassungen (Marcel, 19.11.2026, direkt bestätigt):** Baustein-Fassungen vom Mathe-Trainer dürfen ohne Rückfrage übernommen werden, wenn (1) der Patch vorher gelesen wurde, (2) die Prüfsumme stimmt, (3) der Test 51 Prüfungen besteht und (4) es bei der Teststufe bleibt. Alles andere, was Raumlicht ändert, braucht weiter Marcels direktes Ja.
Verlauf: 1f4b377 (2.18.35) → e9993ec (2.18.37: grund `familie-weg` für gelöschte Familien; der Umweg über `/liste` in der Brücke ist entfallen).
