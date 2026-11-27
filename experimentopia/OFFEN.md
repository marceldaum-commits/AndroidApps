# Experimentopia – Offene Punkte und Plan (Stand 25.11.2026)

**Version:** 2.18.0 vom 27.11.2026 (nur Test, `lernen-test.mathenachhilfe-online.com/experimentopia/`). **Live:** 2.13.2 (`6c69e270`). Rückweg: 2.17.0 (`432af4d2`) / 2.16.2 (`2c38962e`) / live-Stand 2.13.2.
Arbeitsregel (Marcel, 25.11.): Liste führen, selbständig der Reihe nach abarbeiten, Erledigtes mit Datum nach „Erledigt“ (nie löschen). Wartet etwas auf andere: einmal über die AppZentrale mit Frist anfragen, dann weiter; nach Fristablauf selbst nachhaken. Fragen an Marcel nur über die AppZentrale, als Auswahl mit Empfehlung.
Alter Stand (bis 24.11.) vollständig in `docs/OFFEN-ARCHIV-bis-24-11.md`.

## Offen (der Reihe nach)

| # | Was | Ziel / Frist | Nächster Schritt | Hängt ab von | Status |
|---|---|---|---|---|---|
| 2 | Weitere Lehrplan-Lücken: Atombau/PSE, Radioaktivität, Bio Kl. 5/6, Wärme wandert | je Update Di/Fr | Reihenfolge laut `docs/LEHRPLAN-ABGLEICH.md` | – | offen |
| 3 | Wochenrunde Fr 27.11. 17–19 Uhr (Abstimmung), Beitrag vorbereiten | Fr 27.11. | `WOCHENRUNDE.md` ergänzen | AppZentrale | offen |
| 4 | Satz „Gekaufte Codes werden wieder frei“ (`LOESCHEN_WARNUNG`) gegen V38 (ein Gerät pro Code) prüfen; nie „Ende-zu-Ende“/„können nicht lesen“ | vor Live-Gang | Wortlaut mit V38 abgleichen, bei Abweichung Text über AppZentrale/Recht klären | Recht (V38-Text) | wartet; Anfrage 25.11., Nachhaken Fr 27.11. |
| 5 | Live-Gang 2.16.x | durch Leitstand | Bedingungen unten abhaken | Leitstand, Recht, Marcel (Geräte-Test) | wartet |
| 6 | Zähl-Ereignis „umzug“ am Server freischalten | vor Live-Gang | Leitstand schaltet frei | Leitstand | wartet; gemeldet 24.11., Nachhaken Fr 27.11. |
| 7 | Tonstudio: vertonte Dateien einbinden, `ENABLED=true` | nach Lieferung | Liste liegt in `vertonung/` (25.11.); Einbau nur nach Dateien + Freigabe V36 (Kennzeichnung „KI-generierte Stimme“, Frist 02.12.) | Tonstudio/Leitstand, Recht | wartet; Format von Tonstudio prüfen lassen |
| 8 | Eltern-Tor: Vorlese-Sätze sobald Tonstudio-Stimme läuft | nach #7 | Sätze in Liste aufnehmen | #7 | wartet |
| 9 | Familien-Speicher einschalten (aus bis Freigabe) | nach Leitstand | Test mit zweiter echter App, Geräte-Test iPhone/iPad | andere App, Marcel, Leitstand | wartet |
| 10 | Geräte-Test iPhone/iPad (Start ohne nova, Startbild, Umzug, Tor) | vor Live-Gang | Test-Adresse durch Marcel öffnen lassen | Marcel | wartet |
| 11 | Monatsprüfung Dezember (Block A: Wasser + Kraft) | Dezember | Prüfplan `PRUEFPLAN.md` | – | offen |
| 12 | Tägliche Meldung Mi–Sa 9:43 an AppZentrale; Wochenrhythmus (Arbeit Di 15:00 bis So, Pause Mo/Di bis 15:00) | laufend | Routine | – | läuft |
| 13 | 301-Umleitung alte Adresse: nicht vor 1.3.2027 und nur wenn „umzug“-Zählung fast alle zeigt | ab 1.3.2027 | Zählung auswerten | Leitstand | später |

**Live-Bedingungen (alle nötig):** Bestätigung V45c ✔ (24.11.); nova-Dateien auf dem Server weg bis 01.12. (Leitstand sperrt erst nach Live-Update); Test Familien-Speicher mit zweiter App (nur nötig, wenn er an ist; er bleibt zunächst aus); Geräte-Test; Freigabe Leitstand.

## Erledigt (mit Datum)
- 27.11.: 2.18.0 Druck & Luftdruck (8 Versuche, Druck-Werkstatt, Theorie, Tests, Bildschirmfotos 140–143).
- 26.11.: 2.17.0 Energie-Achterbahn (8 Versuche, Achterbahn-Werkstatt, Theorie, Tests, Bildschirmfotos 130–135 angesehen).
- 24.11.: 2.14.0 Recht-Pflichtpunkte (Eltern-Tor, Schloss-Text, Probestunde V15, nova/OpenAI entfernt, Startbild), 2.15.0 Familien-Speicher angebunden (aus), 2.16.0 Motor/Dynamo/Trafo (13 Versuche), 2.16.1 Dienst-Speicher-Fehler `/familie/` behoben, PC-Ansicht geprüft.
- 24.11.: 2.16.2 Startbild überspringbar, Manifest-`id`, `docs/TEXTE-ABGLEICH.md`; Recht V45d freigegeben mit Auflagen (KI-Status a: Simulation).
- 24.11.: 2.16.3/2.16.4 anonymes Ereignis „umzug“ (ohne eigenen Merker), Recht V45c erledigt.
- 24.11.: Antwort nova-Auftrag (Experimentopia lädt nova nicht mehr); 25.11.: Profil an Leitstand gemeldet.
- 25.11.: Textliste Tonstudio (`vertonung/`, 2 998 Stücke / 559 langsam), Commit `76b108c8`.
