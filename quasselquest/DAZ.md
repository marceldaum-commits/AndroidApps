# Deutsch für ukrainische Kinder (DaZ) – Konzept (26.11.2026, Entwurf)

Auftrag Marcel (19.11.2026): Ukrainisch „beides“ – Ukrainisch als Lernsprache (liegt als `lernpfad/uk.json`, hinter `UK_AKTIV`) **und** Deutsch für ukrainische Kinder.

## Ziel
Ein ukrainisches Kind (Klasse 1–9), das neu in Deutschland ist, übt Deutsch in kurzen Runden. Ruby erklärt auf Ukrainisch, die Übungen sind die gleichen Spielarten wie sonst (Wimmelbild, Hören, Satz-Zug, Aktionen, Fehler-Suche).

## Umsetzung in Stufen
1. **„Meine Sprache“ (Eltern-Tor oder Startseite, abschaltbar):** Deutsch (Standard) oder Українська. Gespeichert wie die anderen Einstellungen auf dem Gerät. Nur sichtbar bei `UK_AKTIV`.
2. **Lernpfad `lernpfad/de-uk.json`** (Sprache `de`, Bedeutungen/Regeln/Tipps auf Ukrainisch): Abschnitt 1 „Ankommen“ (Hallo, ich heiße …, Schule, Klasse, Familie, Essen, Zahlen, Wochentage). Gleiches Schema wie die anderen Lernpfade, damit alle Spielarten und Tests sofort greifen.
3. **Bedienung auf Ukrainisch:** Tabelle der wichtigsten Oberflächentexte (Start, Weiter, Richtig, Noch mal, Meine Insel, Einstellungen …) in `i18n-uk.js`; fehlende Schlüssel fallen auf Deutsch zurück.
4. **Ruby-Sprüche auf Ukrainisch** (Lob, Trost, Tipp) – kurze, einfache Sätze.
5. **Stimme:** Deutsch hat zurzeit keine Stimme (Chatterbox „de-1“ vom Leitstand kommt später). Bis dahin liest das Kind Text mit Bild; Hören erst, wenn die deutschen Dateien mit Wasserzeichen und KI-Kennzeichnung eingebaut sind.

## Qualität und Recht
- Alles selbst geschrieben; unabhängiges Lektorat wie bei `uk.json`, danach Prüfung durch Muttersprachler (Entscheidung Marcel) vor `UK_AKTIV=true`.
- Keine neuen Daten: „Meine Sprache“ bleibt auf dem Gerät, keine Übermittlung (Kindertext Punkt 1 gilt unverändert).
- Bei Freigabe: Datenschutz-Seite und Kindertexte auf Ukrainisch erst nach Recht-Prüfung (Wortlaut von Recht).

## Reihenfolge
Do 26.11.: Stufe 2 (Abschnitt 1, 4 Einheiten) + Lektorat · Fr 27.11.: Stufen 1 und 3 · danach Stufe 4 und Test mit Bildschirmfotos.
