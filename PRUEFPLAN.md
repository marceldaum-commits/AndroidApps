# Rechenblitz – Prüfplan (Dauerregel Marcel, 18.11.2026)

> „Jede App soll jede Woche neuen Content einbauen und einmal im Monat gegen Ende alles prüfen und fixen – mit einem Plan, wann was geprüft wird, damit nicht jedes Mal alles neu geprüft werden muss.“

## 1. Wöchentlich: neuer Inhalt (mittwochs auf `rechenblitz-test`)

- Mindestens **eine neue Inhaltseinheit** pro Woche, zum Beispiel:
  - eine neue Aufgabenart,
  - eine neue Stufe,
  - neue Merk-Karten oder ein Merk-Deck,
  - neue Textaufgaben-Vorlagen,
  - ein Rätsel- oder Reise-Kapitel.
- **Mittwoch** auf `rechenblitz-test`, als Nachkomma-Version mit Datum. Kurz in der `MELDUNG_RECHENBLITZ.md` nennen; der Leitstand gibt frei.
- Jede neue Einheit läuft durch die **automatischen Prüfungen**, die bei jeder Version laufen (Abschnitt 3). Sie wird außerdem im nächsten Monats-Check als „Neu-Content“ von Hand geprüft.
- Die Liste steht unten in Abschnitt 5 („Neu-Content-Protokoll“). So weiß der Monats-Check genau, was neu dazugekommen ist.

## 2. Monatlich in der letzten Woche: rollierender Prüfplan

Der Inhalt ist in **4 Blöcke** geteilt. Jeden Monat wird **ein Block gründlich** geprüft. Dazu kommen jeden Monat **der Neu-Content des Monats** und die **Rundum-Prüfung**. So ist nach 4 Monaten alles einmal gründlich geprüft, ohne dass jeden Monat alles neu geprüft werden muss.

| Block | Inhalt | gründlich heißt |
|---|---|---|
| **A – Grundrechnen & Merken** | plus, minus, mal, geteilt, Lücke, Verdoppeln/Halbieren, Zehnerpotenzen, Merk-Training (alle Decks, Merksätze, Sternbilder), Üben nach Wunsch | jede Stufe von Hand spielen; Merksätze lesen; Stufen-Beschreibungen gegen echte Aufgaben; Eingabe/Tastatur |
| **B – Zahlen & Terme** | Brüche, Dezimalzahlen, Prozent, negative Zahlen, Potenzen, Punkt vor Strich, Runden/Überschlag, Einheiten, Teilbarkeit, Terme, Gleichungen, lineare Funktionen | Lösungswege lesen (Verständlichkeit); Hinweise/Antwortformat; gleichwertige Antworten |
| **C – Anwenden (Sachaufgaben)** | Textaufgaben, Geld, Zeit, Geometrie, Kreis, Dreisatz, Zinsen, Wahrscheinlichkeit, Statistik, Folgen | **alle Vorlagen lesen** (Eindeutigkeit, Einheit, Rundung, „Startwert klar?“), EN/UK-Übersetzung der Vorlagen gegenlesen |
| **D – Spiele & Extras** | Blitzrunde, Zahlen-Dungeon (Karten, Bosse), Rechen-Reise, Rechengarten, Rätsel des Tages, Blitz-Welt (Shop, Zimmer), Adventskalender, Theorie-Seiten, Pro, Elternbereich/Berichte, Lehrkräfte-Aufträge | jedes Spiel einmal ganz durchspielen (Sieg/Niederlage/Abbruch); Theorie-Texte lesen; Pro-Sperren und Code einlösen |

### Reihenfolge

| Prüfwoche | Block (gründlich) | dazu immer |
|---|---|---|
| **23.–30.11.2026** (erster Prüfmonat) | **C – Anwenden** (daher kamen zuletzt die „ungenauen“ Aufgaben) | Neu-Content Nov., Rundum-Prüfung |
| 21.–31.12.2026 | A – Grundrechnen & Merken | Neu-Content Dez. (+ Adventskalender-Abschluss), Rundum |
| 25.–31.01.2027 | B – Zahlen & Terme | Neu-Content Jan., Rundum |
| 22.–28.02.2027 | D – Spiele & Extras | Neu-Content Feb., Rundum |
| ab März 2027 | wieder C → A → B → D | … |

### Rundum-Prüfung (jeden Monat, ca. 1 Stunde)

- **Start:**
  - Web frisch (ohne Cache), installierte PWA, offline starten;
  - Auto-Update von der Vormonatsversion (`tests/web-update.mjs`).
- **iPhone/WebKit:**
  - Safari bzw. WebKit-Test: Start, Eingabe, Musik nach dem ersten Tippen;
  - „App installieren“-Anleitung, Teilen.
- **Klassen-Challenge:** beitreten, Punkte senden, Rangliste (Testklasse), Fehlermeldung ohne Netz.
- **Links:**
  - alle Links nach außen: Website, Probestunde, Lernraum, Lern-Geschenk, Shop/Pro-Kasse, Datenschutz, Impressum;
  - QR-Codes und Flyer.
- **Zählung:** kommt `start` beim Zähler an, und ist sie ausgeschaltet still?
- **Sprachen:** DE/EN/UK, 320 px und PC, keine fehlenden Übersetzungen.

## 3. Automatisch bei jeder Version (muss nicht im Monats-Check wiederholt werden)

| Prüfung | Werkzeug |
|---|---|
| alle Aufgaben-Lösungen und Lösungswege nachrechnen (261 000 Aufgaben, unabhängiger Prüfer) | `tests/pruefer/` (export.js → answers.py, steps.py, lint.py) |
| gleichwertige Antworten werden angenommen (≈ 811 000 Eingaben) | `tests/pruefer/equiv.js` |
| jede Stufe strikt schwerer als die vorige (174 Stufen) | `tests/schwierigkeit.js` |
| keine fehlenden Übersetzungen (Aufgaben, Lösungswege, Oberfläche) | `node i18n/build.js` + Fehlend-Prüfung |
| Pro-Codes (echt/falsch/fremd signiert) | `tests/pro-code.js` |
| Auto-Update ohne Datenverlust | `tests/web-update.mjs` |
| „App installieren“ (Chrome/iPhone/installiert) | `tests/app-installieren.js` |
| Vorlesen aus: keine Abrufe, keine Gerätestimme | `tests/vorlesen-aus.js` |
| Barrierefreiheit (axe), Musik/Autostart | Browser-Tests |

## 4. Ablauf im Prüfmonat

1. Montag der Prüfwoche: automatische Prüfungen (Abschnitt 3) laufen lassen.
2. Danach den Block des Monats gründlich prüfen.
3. Anschließend den Neu-Content des Monats (Abschnitt 5) und die Rundum-Prüfung.
4. Gefundene Fehler sofort beheben, als Nachkomma-Version auf test.
5. Meldung „Monats-Check <Monat>: Block X, n Funde, behoben“ in `MELDUNG_RECHENBLITZ.md`. Danach in Abschnitt 6 abhaken.

## 5. Neu-Content-Protokoll

| Woche (Mittwoch) | Version | Neue Inhaltseinheit | im Monats-Check geprüft |
|---|---|---|---|
| 18.11.2026 | – | Betriebstest-Woche: nur Fehlerbehebungen, kein neuer Inhalt | – |
| 25.11.2026 | (geplant) | Zahlen-Dungeon 3: Bosse & Freischaltungen (Roadmap Tag 8) | Nov. |

## 6. Prüf-Protokoll

| Monat | Block | Ergebnis |
|---|---|---|
| Nov. 2026 | C – Anwenden | offen (23.–30.11.) |
