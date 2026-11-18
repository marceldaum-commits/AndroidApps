# Prüf- und Inhaltsplan Wissensgarten

Dauerregel von Marcel (18.11.2026, über den Leitstand): **Jede Woche neuer Inhalt**, **einmal im Monat gegen Ende prüfen und beheben**. Damit nicht jedes Mal alles geprüft werden muss, ist der Inhalt in vier Blöcke geteilt. Jeden Monat wird genau ein Block gründlich geprüft.

Stand: 18.11.2026 · 72 freie Wissenspakete mit 2472 Karten, dazu die Plus-Pakete. Die Plus-Pakete liegen privat im Repository Website_Backup im Ordner `wissensgarten-plus/` und werden im Block ihres Fachs mitgeprüft. Ihr Inhalt steht nie in öffentlichen Dateien, Korrekturen dazu nur mit Karten-ID.

## 1. Dienstag und Freitag: großes Inhaltsupdate

Dauerregel von Marcel (18.11.2026, ersetzt „wöchentlich mittwochs“): **Jeden Dienstag und Freitag bis 16 Uhr ein großes Inhaltsupdate auf test**, dazu eine Meldung in `MELDUNG_AN_LEITSTAND.md`. Der Leitstand prüft am selben Tag und gibt frei.

- **Was drin ist, entscheiden wir nach Wirkung:**
  - neue Themen und Karten,
  - bestehende Themen ausweiten,
  - bestehende Karten korrigieren und vervollständigen,
  - neue Optionen in der App.
- **Pflicht (Marcel wörtlich): „mathematisch korrekt, vollständig, nachvollziehbar. Keine Abkürzungen, weil die Schüler damit lernen.“** Das gilt für alle Fächer:
  - fachlich korrekt,
  - vollständige Erklärungen (Theorie-Knopf bzw. `info`),
  - Rechenwege mit allen Schritten,
  - jedes Ergebnis nachgeprüft (bei Mathe nachgerechnet, sonst mit Quelle).
- **Jede neue Karte:** Merkbild (`img`), eindeutige Antwort, passende Klassenstufe. Dazu `VorlesenListeTest` neu laufen lassen (Nova-Liste) und `AntwortKontrolleTest` grün.
- **Meldung:** was neu ist, wie viele Karten bzw. Aufgaben, wie geprüft.

| Termin | Vorschlag |
|---|---|
| Fr 20.11.2026 | **Aufräum-Tag** (Vorschlag Marcel): „Gleichungen lösen“ und „Kopfrechen-Tricks“ zu Merkwissen umbauen, Deeplinks `#paket=<ID>`, abgestimmte Querverweise zu Mathe-Trainer, Rechenblitz, KurvenFlug, Experimentopia und Raumlicht; danach, wenn Zeit bleibt, Rechengeschichten Klasse 1–4 |
| Di 24.11.2026 | Englisch Klasse 3–5: „School & Classroom“ (Vorrang Deutsch/Englisch laut Entscheidung vom 18.11.) + vollständige Rechenwege in Negative Zahlen, Brüche & Prozent, Algebra |
| Fr 27.11.2026 | Deutsch Klasse 3–5: Rechtschreibstrategien (ie/ei, ss/ß, Dehnungs-h, das/dass) mit Beispielen |
| Di 01.12.2026 | Englisch Klasse 3–6: Food & Drinks, Clothes & Weather (abgestimmt mit der Sprachlern-App) |
| Fr 04.12.2026 | Deutsch Grundschule: Wortfamilien und Wortfelder |
| Di 08.12.2026 | Englisch Klasse 4–6: Hobbys & Free Time, Time & Dates; Sachunterricht „Winter, Wetter, Wasser“ |

Die Tabelle wird laufend fortgeschrieben; Wünsche von Marcel oder dem Leitstand gehen vor.

**Abgleich mit den anderen Lern-Apps (Dauerregel, 18.11.):** Vor jedem Update wird in der Wochenrunde (`WOCHENRUNDE.md`) abgestimmt, wer was herausnimmt, abgibt oder behält, damit nichts doppelt entsteht.

## 2. Jeden Monat in der letzten Woche: Prüfen und Beheben

### a) Gründliche Prüfung: ein Block pro Monat (rollierend)

Für jede Karte im Block wird geprüft:
- **Fakten richtig**, auch Zahlen, Jahreszahlen, Rechtschreibung und Grammatik.
- **Eindeutig:** nur eine richtige Antwort, keine falschen Antwortmöglichkeiten, die auch stimmen könnten.
- **Verständlich** für die angegebene Klassenstufe.
- **Merkbild passt.**
- **Vorlesen klingt richtig** (Stichprobe mit Nova).

Korrekturen kommen in eine Datei `KORREKTUREN_<THEMA>_<Datum>.md` (bei Plus-Paketen nur Karten-IDs). Die Paketversion wird erhöht, damit Geräte das Update laden.

| Block | Inhalt | Karten (ca.) | Prüfmonate |
|---|---|---|---|
| **A** | Mathe (17 Pakete, Grundschule bis Klasse 10) und Plus-Mathe | 570 | **Nov 2026** (23.–30.11.), Mär 2027, Jul 2027, Nov 2027 |
| **B** | Deutsch, Englisch, Französisch, Latein, Spanisch und Plus-Sprachen | 690 | Dez 2026, Apr 2027, Aug 2027, … |
| **C** | Sachunterricht, Biologie, Chemie, Physik, Informatik und Plus-Naturwissenschaften | 580 | Jan 2027, Mai 2027, Sep 2027, … |
| **D** | Geschichte, Erdkunde, Allgemeinwissen, Politik, Wirtschaft, Ethik, Musik und Plus-Gesellschaft | 630 | Feb 2027, Jun 2027, Okt 2027, … |

Jede Karte wird damit alle vier Monate einmal gründlich angesehen. Was im Monat neu dazukam, wird **zusätzlich** gründlich geprüft, egal zu welchem Block es gehört.

### b) Kurze Rundum-Prüfung (jeden Monat, ca. 1 Stunde)

| Bereich | Wie | Beleg |
|---|---|---|
| Start und Laden | Startbild sofort sichtbar, Garten steht | Browser-Test, `?diag=1` |
| iPhone/iPad (WebKit) | Wasm-Fassung mit Safari-Abhilfe (0 Endaufrufe), JS-Fassung startet, automatische Umschaltung | Bau-Ausgabe „Safari-27.2-Abhilfe“, `webApp/tools`, WebKit-Lauf durch den Leitstand |
| Update | alte Version → neue Version, Lernstand und Nova-Speicher bleiben | `webApp/tools/web-update.cjs` |
| Installieren | Knopf und Anleitung | `webApp/tools/web-install.cjs` (21 Prüfungen) |
| Zählung | ein `start`, keine Kennung, Schalter aus = nichts | `webApp/tools/web-zaehlung.cjs` |
| Links | Probestunde, Neukundenpaket, Crashkurs, Geschenke, Fußzeile, Impressum, Datenschutz liefern 200 und tragen `src` | `curl` je Link, `AngeboteTest` |
| Vorlesen (Nova) | Abdeckung aus der Stand-Datei des Servers, Stichprobe Wiedergabe | Abdeckung in % in der MELDUNG |
| Plus | Code-Link `?pro=`, Plus-Pakete laden, Abzeichen | Probecode (nie committen) |
| Tests | Logik, Inhalte, Bildschirmfotos | `core:model:jvmTest`, `content:testDebugUnitTest`, Roborazzi; Bildschirmfotos ansehen |

### c) Abschluss

- Gefundene Fehler beheben, neue Version auf test, kurzer Bericht in `MELDUNG_AN_LEITSTAND.md` (Block, Anzahl geprüfter Karten, Korrekturen, Ergebnis der Rundum-Prüfung).
- Diesen Plan fortschreiben: nächster Block, Wochen-Tabelle, neue Prüfpunkte (z. B. wenn eine neue Funktion dazukommt).

## Nächste Termine

- **Fr 20.11.2026:** erstes großes Inhaltsupdate auf test (danach jeden Dienstag und Freitag).
- **23.–30.11.2026:** erste Monatsprüfung, Block A (Mathe) und Rundum-Prüfung.
