# Prüfplan Raumlicht

Angelegt am 18.11.2026 (Auftrag: „jede Woche neuer Inhalt, einmal im Monat gegen Ende alles prüfen und fixen – mit Plan, damit nicht jedes Mal alles neu geprüft werden muss“).

## Rhythmus

- **Jeden Dienstag und Freitag (seit 18.11.2026, vorher mittwochs):** ein großes Inhaltsupdate bis 16 Uhr auf die Testadresse (Richtwert: ein ganzes neues Thema als Kapitel mit mindestens 6 bis 8 Aufgaben, dazu mehr, wenn Luft ist – nicht sparsam sein) – neue Themen aus `LEHRPLAN-ABGLEICH.md`, mehr Aufgaben, Korrekturen oder neue Optionen. Pflicht: jeder Lösungsweg mit allen Schritten, jede Formel mit Beispiel in der Theorie, jedes Ergebnis nachgerechnet. Meldung in `MELDUNG_AN_LEITSTAND.md` (was neu, wie viele Aufgaben, wie geprüft). Live nimmt der Leitstand.
- **Jeden Monat (ab dem 24.):** ein Block gründlich, dazu der neue Inhalt des Monats und die kurze Rundum-Prüfung.
- Beides läuft als geplante Aufgabe in dieser Sitzung (dienstags und freitags 8:47 Uhr und am 24. um 9:41 Uhr).

**Seit 2.18.32 (19.11.2026):** Raumlicht hat nur noch Aufgaben zur Geometrie im Raum (seit 2.18.43: 18 Kapitel, 107 Aufgaben; intern Kapitel 1–11, 18–21, 26, 27, 28). Die Rechenaufgaben zu Analysis und Stochastik sind herausgenommen; Block 3 und 4 prüfen stattdessen die Erlebnis-Bereiche.

## Die vier Blöcke (rollierend)

| Block | Inhalt | Monate |
|---|---|---|
| 1 | Geometrie I: Kapitel 1–8 (Punkte, Vektoren, Geraden, Lage, Ebenen, Skalarprodukt/Spiegelung, Winkel, Abstände) | November, März, Juli |
| 2 | Geometrie II: Kapitel 9–11, 18–21, 26 (Kugeln, Kreuzprodukt, Bewegung, Ebenen-Formen, Schnitte, Spiegeln & Lote, Kugel & Ebene, HNF) | Dezember, April, August |
| 3 | Analysis & Zufall erleben: Kurve abfahren (auch eigene Funktionen), Achterbahn-Baumeister, Geisterfahrt, Rotationskörper-Labor (alle Formen und Bereiche, Rechenweg, Rückmeldungen) | Januar, Mai, September |
| 4 | Zufallsmaschine, Galton-Brett, Binomial-Labor – dazu Prüfungsaufgaben, „Rechenweg in Schritten“, Übungs- und Tagesaufgaben (alle Vorlagen), Verweise zu den anderen Apps | Februar, Juni, Oktober |

So ist jeder Teil dreimal im Jahr gründlich dran.

## Was „gründlich“ heißt (je Aufgabe des Blocks)

1. **Richtig:** Lösung von Hand nachrechnen; Rechenweg und Tipp stimmen mit der Lösung überein.
2. **Eindeutig:** Auftrag lässt nur eine Deutung zu; wo mehrere Antworten gleichwertig sind, werden alle angenommen (siehe `MehrdeutigTest`).
3. **Erreichbar:** Die Lösung lässt sich mit den Reglern einstellen; Bereiche sind groß genug.
4. **Theorie:** Die Karte passt zur Aufgabe und zeigt die nötige Formel mit Bedeutung der Größen (siehe `THEORIE-PRUEFLISTE.md`, `TheoryFormulaTest`).
5. **Verständlich:** Texte kurz, in der Sprache der Oberstufe, ohne Fachwort ohne Erklärung; Fehler-Rückmeldungen helfen weiter.
6. **Bild:** Die 3D-Bühne zeigt, was der Text sagt (Stichprobe je Kapitel im Browser).

## Kurze Rundum-Prüfung (jeden Monat)

- Start in beiden Fassungen (`web-modus.mjs`), Schrittprotokoll (`web-diag.mjs`), Störungsfälle (`web-stoerung.mjs`)
- Selbst-Update und Sprung von einer alten Version (`web-update.mjs`, `web-vorversion.mjs`)
- Zählung (`web-zaehler.mjs`), „nichts spricht“ (`web-still.mjs`), KI-Tutor (`web-ki.mjs`), „App installieren“ (`web-install.mjs`)
- Alle Links nach außen antworten (Shop Pro, Shop KI, Probestunde, Geschenke, Startseite, Impressum, Datenschutz)
- Logik- und App-Tests, Lint
- Auf dem echten iPhone/iPad kann nur Marcel prüfen: einmal im Monat `…/?diag=1` öffnen.

## Neu in diesem Monat (wird in der Monatsprüfung mitgeprüft)

| Monat | Neuer Inhalt | Version |
|---|---|---|
| November 2026 | Eltern-Handy koppeln (Bericht, QR-Code), „Kommt bald“-Kacheln, Lern-Protokoll | 2.18.26 |
| November 2026 | Umbau aufs Profil „Geometrie im Raum“: 52 Rechenaufgaben zu Analysis und Stochastik heraus, Bereich „Analysis & Zufall erleben“, Rotationskörper-Labor (neu), Kapitel lückenlos 1–16 | 2.18.32 |
| November 2026 | Kapitel 18 (intern 28) „Lage von Gerade und Ebene“ (8 Aufgaben, Theorie-Karte) und 3 neue Abi-Prüfungen (Glaspyramide, Spiegelsaal, Zwei Schiffe; zusammen 11) | 2.18.43 |
| November 2026 | Kapitel 17 (intern 27) „Gleichungssysteme & Gauß“ (7 Aufgaben, drei Ebenen im Raum, Theorie-Karte) und 4 neue Abi-Prüfungen (Dachfläche, Kran, Heißluftballon, Wasserrutsche; zusammen 8) | 2.18.40 |
| November 2026 | Kapitel 26 „HNF, Abstände & Winkel“ (8 Aufgaben), 5 neue Theorie-Karten, Formel-Kasten in allen Karten | 2.18.25 |

## Protokoll

| Datum | Block | Befunde | Behoben in |
|---|---|---|---|
| 24.11.2026 | Block 1 (Kapitel 1–8, 44 Aufgaben) + Kapitel 17 (intern 27) und die 4 neuen Prüfungen + Rundum | Alle Lösungen werden angenommen, der Startwert nie; alle Lösungswerte liegen im Bereich der Regler; jeder Rechenweg hat mindestens 2 Schritte, jeder Tipp ist da; mehrfach angenommene Antworten (3-1, 3-5, 4-3, 5-3, 5-4, 5-5, abi-zelt-b, abi-dach-a) sind alle mathematisch gleichwertig (Vielfache); Bühne zeigt in der Stichprobe (2-3, 4-3, 6-2, 8-4, 17-1/3/6, Dach, Ballon), was der Text sagt; alle Außen-Links antworten (200); Vorversion→neu, Start beide Fassungen, Diagnose, Störung, Update, Installation, Zählung, still, KI-Tutor grün. **Offen (kein Fehler der App):** Der Datenschutz-Anker `#familien-speicher` gibt es auf der Live-Datenschutzseite noch nicht (der Abschnitt 9 liegt als Entwurf in `store/datenschutz-abschnitt.md`) – Voraussetzung für einen Live-Gang des Familien-Speichers; bis dahin ist der Link nur auf der Teststufe sichtbar. | – (keine Änderung nötig); 2.18.40/2.18.41 enthalten den neuen Inhalt |
| 17.–18.11.2026 (vor dem Plan) | alle Aufgaben: Klarheit und gleichwertige Antworten | rund 60 Textklärungen; 18-1 und 5-4 korrigiert; Dauertest `MehrdeutigTest` | 2.18.4, 2.18.11 |
| 18.11.2026 (vor dem Plan) | alle Karten: Formel in der Theorie | Formel stand meist nur als Schritt; jetzt eigener Kasten; zwei Zuordnungen verbessert | 2.18.25 |
