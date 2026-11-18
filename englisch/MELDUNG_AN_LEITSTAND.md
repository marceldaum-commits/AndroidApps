# Meldungen an den Leitstand – Englisch-App (Arbeitsname, Ordner `englisch/`)

Neueste oben.

## 18.11.2026 – Konzept zur Entscheidung (Frist Mo 23.11., 11 Uhr – früher fertig)

- **Konzept:** `englisch/KONZEPT.md` (Namen, Zielgruppe, Welten, Spielarten, Stimmen/Mikrofon, Gratis/Pro, Abgrenzung Wissensgarten, Plan 4 Wochen).
- **Namen (Marcel entscheidet):** ⭐ **Quasselquest** · Wortwelle · Plauderhafen. Websuche ohne Konflikt; DPMA/EUIPO-Prüfung bitte vor der Wahl.
- **Kurz:** Kinder Kl. 3–6 reisen mit Ruby 🦜 (Papagei) durch eine englische Hafenstadt und nach London. Jede Episode (5–8 Min.): Szene hören → 3 Spiele → Sprech-Mission mit einer Figur → Stempel.
  10 Spielarten (Hör-Detektiv, Papagei-Echo, Dialog-Bühne, Satz-Zug, Ruby says, Hör-Malbild, Chant-Bühne, Wort-Fangen, Klang-Paare, Mini-Geschichte). Grammatik nur als Sprechmuster.
- **Gratis:** Welt 1 + Welt 7 komplett, erste Episode jeder Welt, alle Spielarten. **Pro:** übrige Welten, Geschichten-Reihe, Klassenarbeit-Hörtraining 5/6. Kinderbereich ohne Preise/Kaufaufforderung.
- **Technik:** schlichtes JavaScript (kein Wasm → keine weißen Seiten auf iOS), PWA offline + Auto-Update, alle Plattform-Bausteine (Alle Apps, Eltern-Kopplung + Sicherung, Zählung, `#pro=`).
- **Bitte vom Leitstand:**
  1. Entscheidung Name/Konzept (Marcel).
  2. Subdomains `<name>` und `<name>-test` (Server holt `<name>/webapp/` aus `<name>-test`/`<name>-live`).
  3. CORS auf trainer/trainer-test, Zähl-Kennung `app=<name>`, Themen `englisch-*`.
  4. ⚠️ **Stimmen:** Der OpenAI-Zugang meldet „kein Guthaben“ (18.11.). Für die Vertonung (britisches Englisch, mehrere Figuren) Guthaben oder Stimm-Server-Zugang. Bis dahin Prototyp mit Gerätestimme.
  5. Pro: Shop-Produkt und Signatur-Schlüssel. 6. Teilnehmerliste Montagsrunde.
- **Abstimmung Wissensgarten:** Vorschlag – WG behält Vokabel-/Verb-/Tenses-Pakete als Merkwissen, wir bauen keine Karteikarten; gegenseitige Links (`#paket=` ↔ `#welt=`/`#episode=`). Nachricht an den WG-Raum ist raus; Antwort folgt hier.
- **Nächster Schritt:** Ich beginne schon jetzt mit dem Prototyp (Welt 1, 3 Episoden, 5 Spielarten) im Arbeitsordner, damit am Fr 27.11. etwas Spielbares auf `<name>-test` liegt. Umbenennen nach der Namenswahl.
