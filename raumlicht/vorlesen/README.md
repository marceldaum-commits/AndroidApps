# Tonstudio-Textliste Raumlicht (Format 1, Stand 25.11.2026)

`textliste.json` – alle festen Vorlese-Texte der App, **nur Deutsch, Tempo normal (1,0)**, im selben Format 1 wie `quasselquest/VERTONUNG.md`.
Erzeugt von `TextlisteTest` (`./gradlew :core:model:jvmTest`); nach jeder Inhaltsänderung neu erzeugen und mit einchecken.

- **963 Texte**, zusammen 82 584 Zeichen (Mission, Tipp, Rückmeldungen, Lux-Finale, Einführungen, Prüfungstexte, Theorie-Ideen, Geisterfahrt-Urteile, Ballerbude-Regeln, vorhersehbare Rückmeldungen).
- `sprechtext`: Zahlen, Formeln und Symbole sind ausgeschrieben („x eins“, „minus drei“, „vier Komma sieben“); nichts Ungesprochenes bleibt (vom Test geprüft).
- `id` = erste 20 Hex-Zeichen von SHA-1(`de|sprechtext`); `datei` = `audio/server/de/<id>.mp3` (relativ zum Web-Ordner der App). Stimme: „de-1“ (Wahl durch Marcel/Leitstand), ID3-Kommentar „KI-generiert (synthetische Stimme)“.
- Vorlesen ist in der App **aus** (`Vorlesen.ENABLED = false`) und bleibt es, bis die fertigen MP3 eingebaut, geprüft und Marcel informiert ist.
- Offen (Bestätigung durch das Tonstudio): Dateinamen-Schema `<id>.mp3` und ob die App-Schlüssel (`Vorlesen.key`, SHA-256) auf die Format-1-ID umgestellt werden sollen – das passiert erst beim Einbau.
