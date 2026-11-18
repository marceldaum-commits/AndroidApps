# Prüfplan Raumlicht

Angelegt am 18.11.2026 (Auftrag: „jede Woche neuer Inhalt, einmal im Monat gegen Ende alles prüfen und fixen – mit Plan, damit nicht jedes Mal alles neu geprüft werden muss“).

## Rhythmus

- **Jede Woche (mittwochs):** mindestens eine neue Inhaltseinheit aus `LEHRPLAN-ABGLEICH.md`, auf die Testadresse, kurze Zeile in `MELDUNG_AN_LEITSTAND.md`. Live nimmt der Leitstand.
- **Jeden Monat (ab dem 24.):** ein Block gründlich, dazu der neue Inhalt des Monats und die kurze Rundum-Prüfung.
- Beides läuft als geplante Aufgabe in dieser Sitzung (mittwochs 8:47 Uhr und am 24. um 9:41 Uhr).

## Die vier Blöcke (rollierend)

| Block | Inhalt | Monate |
|---|---|---|
| 1 | Geometrie I: Kapitel 1–8 (Punkte, Vektoren, Geraden, Lage, Ebenen, Skalarprodukt/Spiegelung, Winkel, Abstände) | November, März, Juli |
| 2 | Geometrie II: Kapitel 9–11, 18–21, 26 (Kugeln, Kreuzprodukt, Bewegung, Ebenen-Formen, Schnitte, Spiegeln & Lote, Kugel & Ebene, HNF) | Dezember, April, August |
| 3 | Analysis: Kapitel 12, 14, 15, 17, 22, 25 – und die Erlebnis-Teile (Kurve abfahren, Baumeister, Geisterfahrt, eigene Funktionen) | Januar, Mai, September |
| 4 | Stochastik: Kapitel 13, 16, 23, 24 – dazu Prüfungsaufgaben, „Rechenweg in Schritten“, Übungs- und Tagesaufgaben (alle Vorlagen), Zufallsmaschine, Galton-Brett, Binomial-Labor | Februar, Juni, Oktober |

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
- Alle Links nach außen antworten (Shop Pro, Shop KI, Probestunde, Crashkurse, Geschenke, Startseite, Impressum, Datenschutz)
- Logik- und App-Tests, Lint
- Auf dem echten iPhone/iPad kann nur Marcel prüfen: einmal im Monat `…/?diag=1` öffnen.

## Neu in diesem Monat (wird in der Monatsprüfung mitgeprüft)

| Monat | Neuer Inhalt | Version |
|---|---|---|
| November 2026 | Eltern-Handy koppeln (Bericht, QR-Code), „Kommt bald“-Kacheln, Lern-Protokoll | 2.18.26 |
| November 2026 | Kapitel 26 „HNF, Abstände & Winkel“ (8 Aufgaben), 5 neue Theorie-Karten, Formel-Kasten in allen Karten | 2.18.25 |

## Protokoll

| Datum | Block | Befunde | Behoben in |
|---|---|---|---|
| 17.–18.11.2026 (vor dem Plan) | alle Aufgaben: Klarheit und gleichwertige Antworten | rund 60 Textklärungen; 18-1 und 5-4 korrigiert; Dauertest `MehrdeutigTest` | 2.18.4, 2.18.11 |
| 18.11.2026 (vor dem Plan) | alle Karten: Formel in der Theorie | Formel stand meist nur als Schritt; jetzt eigener Kasten; zwei Zuordnungen verbessert | 2.18.25 |
