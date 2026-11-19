# Schwierigkeit im Wissensgarten – Nachweis

Automatisch erzeugt von `content/src/test/java/de/wissensgarten/content/DifficultyTest.kt` aus allen 85 Paketen (2924 Karten, davon die Plus-Pakete aus der privaten Ablage).
Der Test schlägt fehl, sobald eine Stufe nicht strikt schwerer ist als die vorige.

## 1. Beim Spielen: Je größer die Pflanze, desto schwerer die Frage

| Stufe | Pflanze | freie Eingabe | Ø Antwortmöglichkeiten | Ablenker-Ähnlichkeit | Rate-Chance | Wiederholungsabstand |
|---|---|---|---|---|---|---|
| Entdecken | Samen | 0 % | 3,0 | 0,36 | 100 % | neu |
| Leicht | Keimling | 0 % | 3,0 | 0,36 | 39 % | ≈ 1 Tag |
| Mittel | Spross | 20 % | 4,0 | 0,53 | 19 % | ≈ 4 Tage |
| Knifflig | Busch | 29 % | 4,0 | 0,55 | 17 % | ≈ 14 Tage |
| Schwer | Blüte | 40 % | 4,0 | 0,57 | 14 % | ≈ 40 Tage |
| Profi | Baum | 100 % | keine Auswahl | – | 0 % | ≈ 90 Tage |

Freie Eingabe = Antwort eintippen (ab Klasse 5) bzw. ohne Auswahl erinnern. Rate-Chance = Wahrscheinlichkeit, zufällig richtig zu liegen (Samen: Antwort wird gezeigt).
Ablenker-Ähnlichkeit = wie ähnlich der ähnlichste falsche Vorschlag der richtigen Antwort ist (0 = ganz anders, 1 = gleich).

## 2. Inhalte: von der Grundschule bis zum Experten

| Stufe | Pakete | Karten | Ø Klasse | freie Eingabe | Ablenker-Ähnlichkeit | Ø Antwortlänge |
|---|---|---|---|---|---|---|
| Grundschule | 24 | 786 | 2,9 | 0 % | 0,46 | 8,6 Zeichen |
| Klasse 5–7 | 38 | 1343 | 7,3 | 11 % | 0,54 | 15,9 Zeichen |
| Klasse 8–10 | 13 | 452 | 10,3 | 24 % | 0,58 | 16,5 Zeichen |
| Oberstufe/Abi | 6 | 207 | 12,0 | 49 % | 0,72 | 19,5 Zeichen |
| Experte | 4 | 136 | Erw. | 76 % | 0,90 | 21,5 Zeichen |
