# Wissenspakete – Format

Jedes Paket ist eine JSON-Datei in `content/src/main/assets/packs/<id>.json`.
Pakete sind reine Daten: Neue Pakete oder Updates brauchen keinen Code.

```json
{
  "id": "gs-einmaleins",
  "version": 1,
  "title": "Das kleine Einmaleins",
  "subject": "mathe",
  "grades": [2, 3],
  "emoji": "✖️",
  "tier": "free",
  "description": "Alle Malaufgaben bis 10 × 10.",
  "cards": [
    { "id": "001", "q": "3 × 4", "a": "12", "choices": ["7", "14", "16"], "info": "3 × 4 = 4 + 4 + 4" }
  ]
}
```

## Felder

| Feld | Pflicht | Regeln |
| --- | --- | --- |
| `id` | ja | eindeutig, kebab-case, **nie ändern**. Präfix nach Stufe: `gs-` (Klasse 1–4), `s1-` (5–10), `ob-` (11–13), `aw-` (Allgemeinwissen/Erwachsene) |
| `version` | ja | Ganzzahl, bei jeder inhaltlichen Änderung +1 |
| `title` | ja | kurz, max. ~30 Zeichen |
| `subject` | ja | eine der Fach-IDs unten |
| `grades` | ja | Klassenstufen 1–13, `14` = Erwachsene/Studium/Beruf. Mehrere möglich |
| `emoji` | ja | ein **klassisches** Emoji (Unicode ≤ 11, also bis 2018), keine Flaggen |
| `tier` | ja | `free` oder `plus` (später kostenpflichtig) |
| `description` | ja | ein Satz für Eltern/Schüler |
| `cards[].id` | ja | eindeutig im Paket, **nie ändern oder wiederverwenden** (Lernfortschritt hängt daran) |
| `cards[].q` | ja | Frage/Vorderseite, kurz (max. ~90 Zeichen) |
| `cards[].a` | ja | Antwort, kurz (max. ~60 Zeichen), eindeutig |
| `cards[].choices` | nein | **nur falsche** Antworten (2–3 Stück), plausibel, gleiche Form wie `a`. Mit `choices` wird die Karte als Auswahl-Aufgabe gespielt und automatisch bewertet. Für Klasse 1–4 Pflicht |
| `cards[].info` | nein | kurze Erklärung/Merkhilfe (max. ~140 Zeichen), wird nach der Antwort gezeigt |

## Fach-IDs

`deutsch`, `mathe`, `sachunterricht`, `englisch`, `franzoesisch`, `latein`, `spanisch`,
`biologie`, `chemie`, `physik`, `informatik`, `geschichte`, `erdkunde`, `politik`,
`wirtschaft`, `ethik`, `musik`, `kunst`, `allgemeinwissen`

## Qualität

- Fachlich **korrekt** und eindeutig – im Zweifel Karte weglassen.
- Altersgerecht formuliert, neue deutsche Rechtschreibung.
- Keine Texte aus Schulbüchern oder Prüfungsfragen abschreiben (Urheberrecht) – eigene Formulierungen.
- 20–40 Karten pro Paket.
