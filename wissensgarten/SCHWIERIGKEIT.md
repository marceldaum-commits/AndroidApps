# Schwierigkeit im Wissensgarten – Nachweis

Automatisch erzeugt von `content/src/test/java/de/wissensgarten/content/DifficultyTest.kt` aus allen 85 Paketen (2923 Karten).
Der Test schlägt fehl, sobald eine Stufe nicht strikt schwerer ist als die vorige.

## 1. Beim Spielen: Je größer die Pflanze, desto schwerer die Frage

| Stufe | Pflanze | freie Eingabe | Ø Antwortmöglichkeiten | Ablenker-Ähnlichkeit | Rate-Chance | Wiederholungsabstand |
|---|---|---|---|---|---|---|
| Entdecken | Samen | 0 % | 3,0 | 0,34 | 100 % | neu |
| Leicht | Keimling | 0 % | 3,0 | 0,34 | 39 % | ≈ 1 Tag |
| Mittel | Spross | 27 % | 4,0 | 0,53 | 17 % | ≈ 4 Tage |
| Knifflig | Busch | 43 % | 4,0 | 0,55 | 13 % | ≈ 14 Tage |
| Schwer | Blüte | 63 % | 4,0 | 0,58 | 9 % | ≈ 40 Tage |
| Profi | Baum | 100 % | keine Auswahl | – | 0 % | ≈ 90 Tage |

Freie Eingabe = Antwort eintippen (ab Klasse 5) bzw. ohne Auswahl erinnern. Rate-Chance = Wahrscheinlichkeit, zufällig richtig zu liegen (Samen: Antwort wird gezeigt).
Ablenker-Ähnlichkeit = wie ähnlich der ähnlichste falsche Vorschlag der richtigen Antwort ist (0 = ganz anders, 1 = gleich).

## 2. Inhalte: von der Grundschule bis zum Experten

| Stufe | Pakete | Karten | Ø Klasse | freie Eingabe | Ablenker-Ähnlichkeit | Ø Antwortlänge |
|---|---|---|---|---|---|---|
| Grundschule | 24 | 786 | 2,9 | 1 % | 0,46 | 8,1 Zeichen |
| Klasse 5–7 | 38 | 1342 | 7,3 | 16 % | 0,54 | 14,5 Zeichen |
| Klasse 8–10 | 13 | 452 | 10,3 | 34 % | 0,57 | 16,0 Zeichen |
| Oberstufe/Abi | 6 | 207 | 12,0 | 54 % | 0,75 | 19,4 Zeichen |
| Experte | 4 | 136 | Erw. | 76 % | 0,90 | 21,5 Zeichen |
