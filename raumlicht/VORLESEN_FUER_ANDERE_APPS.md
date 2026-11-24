# Vorlesen – Stand bei Raumlicht

Stand: Raumlicht 2.18.39 vom 24.11.2026.. **Vorlesen ist in Raumlicht abgeschaltet (Schalter `Vorlesen.ENABLED = false`).** Die frühere Anleitung (fertige Dateien über einen fremden Sprachdienst) ist entfallen: Marcel hat am 20.11.2026 entschieden, dass es nur noch eigene Stimmen aus dem Tonstudio auf unserem Server gibt.

## Was Raumlicht dafür bereithält
- **Textliste:** `core/model/…/Vorlesen.kt` sammelt alle festen Texte (Kapitel-Einleitungen, Aufträge, Tipps, Rückmeldungen, Theorie-Ideen), macht sie vorlesefertig (Vektoren, Formeln und Zahlen in Worten, siehe `Speech.kt`) und führt jeden nur einmal. Die Kennung eines Textes ist `key(text)` = die ersten 32 Hex-Zeichen von SHA-256(„de-1|de|“ + Text).
- **Auslieferung:** `./gradlew :web:webapp` schreibt `webapp/vorlesen/texte.json` (solange abgeschaltet: leer). Für das Tonstudio wird die Liste im Format 1 (wie `quasselquest/VERTONUNG.md`) erzeugt: nur Deutsch, Tempo normal.
- **Anzeige:** „KI-generierte Stimme“ (`Vorlesen.VOICE_LABEL`), Sprechtempo-Regler mit sechs Stufen (`Vorlesen.TEMPOS`).
- **Zum Einschalten** braucht es: die fertigen Dateien vom Tonstudio, den Sprecher in der Web- und Android-Fassung (`Speaker`), Lizenzliste und Datenschutz-Wortlaut nach der Vorgabe von „Recht“ (V13) und eine neue Version.
