# Überarbeitungsplan Quasselquest (19.11.2026)

Anlass: Marcel, 19.11.2026: „Die ganze App muss massiv überarbeitet werden.“ Maßstab ist eine gute Sprach-Lern-App für
Schüler: abwechslungsreich, aufbauend und mit viel Material je Klassenstufe. Hier steht ehrlich, was heute schwach ist und wann was kommt.

## Was heute schwach ist (ehrlich)

| Bereich | Schwäche | Folge für Kinder |
|---|---|---|
| **Abwechslung** | Hör-Detektiv schloss nur das letzte Ziel aus, „Ruby says“ gar keines. Papagei-Echo und Satz-Zug hatten 3–4 feste Sätze, immer gleich (**in 0.5.0 behoben**). | „Immer dieselben 2 Aufgaben“ |
| **Sprechen** | Papagei-Echo hat nie erkannt, sondern nur aufgenommen. Sterne und „klang super“ kamen aus der Lautstärke (**in 0.5.0 behoben**: ohne Erkennung keine Sterne). Eine echte Auswertung fehlt bisher. | Lob ohne Grund, Erkennung „null geklappt“ |
| **Menge** | Englisch: 3 spielbare Welt-Episoden, 2 Lernpfad-Abschnitte. FR/ES/LA hatten nur je 1–2 Pakete und Latein keinen Lernpfad (**0.5.0: +8 FR/ES-Pakete, FR/ES Abschnitt 3, Latein Abschnitt 1 und 4 Pakete**). Für Kl. 5–13 reicht das noch lange nicht. | Nach wenigen Tagen ist alles gespielt |
| **Ablauf** | Auf der Startseite stehen Lernpfad, Welten, Pakete, Generalprobe und „Mein Schulbuch“ gleichrangig nebeneinander. Was heute dran ist, sagt die App nicht klar. | Kinder irren herum, Eltern verstehen den Plan nicht |
| **Aufgabentypen** | In den Schul-Paketen dominieren Auswahl (Ballon), Hören und Buchstaben-Bau. Lückensätze sind fast nur Auswahl, es gibt kaum freies Schreiben, keine längeren Hör-/Lesetexte und kein Rollenspiel mit Antworten. | Gut fürs Wiedererkennen, schwach fürs freie Anwenden (Klassenarbeit) |
| **Wiederholung** | Die Leitner-Kästen laufen je Paket. Eine tägliche, gemischte Wiederholung über alle Pakete und den Lernpfad fehlt. | Gelerntes aus anderen Paketen geht verloren |
| **Latein** | Abschnitt 1 ist jetzt da. Es fehlen noch Übersetzungs-Trainer mit Satzanalyse und der Lernwortschatz nach Häufigkeit. | Kl. 9–13 kaum versorgt |
| **Welten** | Die Geschichten-Welten (Hafen) gibt es nur auf Englisch. | FR/ES fühlen sich „nackter“ an |

## Heute (19.11., Version 0.5.0)
- **Wiederholungsfix:** Ziehen ohne Zurücklegen (`PAKETE.ziehen`), Echo und Satz-Zug mit je 6 Sätzen, bei jedem Spielen 5 verschiedene in neuer Reihenfolge.
  Ein automatischer Test prüft alle Quests, alle Lernpfad-Lektionen (5 Zufallsläufe je Lektion) und die Größe der Pools.
- **Ehrlichkeit:** keine Sterne und kein „klang super“ ohne echte Erkennung (Echo, Sprech-Mission, Lernpfad-Sprechen), mit Test.
- **Material** (selbst geschrieben, lektoriert):
  - Französisch: 4 Pakete und Lernpfad Abschnitt 3
  - Spanisch: 4 Pakete und Lernpfad Abschnitt 3
  - Latein: Lernpfad Abschnitt 1 (Lesen statt Sprechen) und 4 Pakete
- Danach auf dem Arbeits-Branch: SPRACHE-API.md, Eltern-Auswahl „Aussprache-Auswertung“ (Im Gerät / Server / Aus), Client für Weg 2 hinter dem Feature-Flag mit Mock-Server, Grundgerüst für Weg 1.

## Di 24.11.
1. **Weg 1 (Whisper im Gerät):** Modell (tiny/base, quantisiert) auf lernen.…, laden erst beim ersten Nachsprechen, echte Sterne (★★★ ab 90 %, ★★ ab 70 %, ★ ab 50 %), nicht erkannte Wörter markiert. **Messtabelle** EN/FR/ES/LA × Android-Mittelklasse/iPhone/PC, Ziel: Trefferquote ≥ 90 % bei korrekt gesprochenen Sätzen.
2. **„Heute dran“:** ein großer Knopf auf der Startseite. Er stellt eine Runde von etwa 5 Minuten zusammen: fällige Wiederholungen aus allen Paketen und dem Lernpfad, dann der nächste Lernpfad-Schritt. Alles andere wandert unter „Mehr üben“.
3. Familien-Speicher (familie.js @ e9993ec).
4. Material: Latein Abschnitt 2, FR/ES je 2 weitere Pakete (Lernjahr 2–3), Englisch Lernpfad Abschnitt 3.

## Mi 25.11. – Ukrainisch (Marcel 19.11.2026: „Bau auch ukrainisch ein ab mittwoch“ → beides)
1. **Deutsch für ukrainische Kinder (DaZ):** Ruby erklärt auf Ukrainisch, Übersetzungen Ukrainisch ↔ Deutsch, Lernpfad
   „Deutsch von Anfang an“ (Ankommen, Schule, Familie, Einkaufen …) mit denselben Spielarten. Umschalter „Meine Sprache: Українська“.
2. **Ukrainisch als Lernsprache:** eigene Einheit für das kyrillische Alphabet (Buchstaben-Bau, Wimmelbild), dann Lernpfad Abschnitt 1 wie FR/ES.
3. **Stimme:** Piper `uk_UA-ukrainian_tts-medium`, Sprecherin 2 „tetiana“ (Recht freigegeben, Marcel gewählt). Nennung: „Stimmen lada/mykyta/tetiana: Datensatz ukrainian-tts-datasets (Yehor Smoliakov), Apache License 2.0; Modell Piper (uk_UA-ukrainian_tts-medium)“ + Apache-Lizenztext; KI-Kennzeichnung wie bei den anderen Stimmen. Vertonung im Tonstudio (Leitstand).
4. Alles selbst geschrieben und von einem unabhängigen Lektorat auf Muttersprachler-Niveau geprüft, Recherche in docs/RECHERCHE.md.

## Bis Ende Dezember
- **Neue Spielarten**, damit es nie eintönig wird:
  - **Rollenspiel-Dialog:** Das Kind wählt oder sagt die Antwort im Gespräch (Café, Weg fragen), mit Verzweigungen.
  - **Hör-Geschichte mit Fragen:** 1–2 Minuten pro Folge, Fortsetzungsgeschichte je Sprache.
  - **Lückentext ziehen:** Wörter in einen kurzen Text ziehen, für Grammatik im Zusammenhang.
  - **Schreib-Werkstatt:** kurze freie Sätze tippen, mit „Fast!“-Rückmeldung zu Akzent und Endung.
  - **Latein-Detektiv:** Satzglieder farbig markieren (Subjekt, Prädikat, Objekt), dann übersetzen.
  - **Tägliche gemischte Wiederholung** über alle Inhalte (Interleaving).
- **Menge nach INHALTSPLAN.md:**
  - FR/ES: Lernpfad bis Abschnitt 6 (B1), Pakete Lernjahr 1–5 und Oberstufe, je 800–1000 Karten.
  - Latein: 3 Abschnitte, Lernwortschatz von etwa 1000 Wörtern, Übersetzungs-Trainer Kl. 10–13 mit gemeinfreien Originalsätzen.
- **Welten** für FR und ES (Geschichten-Episoden wie der Hafen), Klassenarbeits-Generalprobe je Lernjahr.
- **Weg 2** (Server-Auswertung) live, sobald der Server des Leitstands steht und Recht ihn freigegeben hat. Danach entfällt die Browser-Online-Erkennung.
- **Qualität:** jede Spielart einmal komplett „wie ein Kind“ durchgespielt (automatisch und von Hand), mit Bildschirmfotos. Dazu automatische Abwechslungs-Tests für jede neue Spielart.
