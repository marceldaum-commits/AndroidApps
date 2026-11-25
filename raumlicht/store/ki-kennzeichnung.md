# KI-Kennzeichnung im Raumlicht-KI-Tutor (KI-Verordnung Art. 50) – Stand 2.18.41 (24.11.2026)

Gilt für die einzige KI-Funktion der App: „Erklär’s mir anders“ (KI-Tutor, Lux erklärt nach einem Fehlversuch).

**Sichtbar für Menschen:** Unter jeder KI-Antwort steht „Antwort einer KI – sie kann Fehler enthalten.“ (gleicher Wortlaut in allen Lern-Apps, `Ki.LABEL`).

**Maschinenlesbar (K4, seit 2.18.41):** Die WebApp zeichnet auf einer Fläche; Text steht nicht im Seitenmodell. Darum gibt es zusätzlich ein verstecktes Element
`<div id="ki-erzeugt"><div role="note" data-ki-erzeugt="true" data-ki-anbieter="Anthropic" data-ki-hinweis="KI-erzeugt">KI-erzeugt: …Antwort…</div></div>`,
je KI-Antwort ein eigenes Element (Rolle „note“, aria-label „KI-erzeugte Antwort“, aria-description für Screenreader) in einem versteckten Container `#ki-erzeugt` (die letzten 20 bleiben) (`kiKennzeichnen` in `Browser.kt`, aufgerufen über `AppRoot(kiMark = …)`). Prüfung: `scene/test/web-ki.mjs`, `AppRootTest.kiTutor`.

**Kopieren/Export:** Es gibt keine Kopier-, Teilen- oder Export-Funktion für KI-Antworten (Antworten sind nicht markier- oder kopierbar, die Karte hat nur „Schließen“). Ein Vermerk „KI-erzeugt“ beim Export entfällt daher. Kommt je eine Export-Funktion, muss sie den Vermerk mitliefern.

**Kein Text-Wasserzeichen – Begründung:** Die Antworten sind kurz (höchstens etwa 120 Wörter) und enthalten Formeln; ein statistisches oder unsichtbares Wasserzeichen im Text wäre bei dieser Länge unzuverlässig, unsichtbare Zeichen stören Screenreader und Vorlesefunktionen und lassen sich leicht entfernen. Belastbar sind die sichtbare Kennzeichnung unter jeder Antwort und das maschinenlesbare Element.

**Was gespeichert wird:** App: nichts (der Wortwechsel liegt nur im Arbeitsspeicher, weg beim Verlassen der Aufgabe). Server (`raumlicht-ki.js`): nur Zähler (Pass-Nummer/Tag, Geräte-Kennung für die Proben, Adresse/Tag gegen Missbrauch); Fragen und Antworten werden nicht gespeichert und nicht protokolliert. Der Tutor vergibt weder Punkte noch Noten.
