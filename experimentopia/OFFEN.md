# Experimentopia – Stand und offene Punkte (18.11.2026)

**Stand:** Version 2.4.7 vom 18.11.2026 (Logo, Nova, eindeutige Aufträge, Android-Nova-Fix, WebApp immer aktuell) liegt auf `experimentopia-test` und als APK im GitHub-Release. Live ist seit 18.11.2026 **2.4.8** (Vorlesen aus) (`experimentopia.mathenachhilfe-online.com`, Branch `experimentopia-live` = Stand der Testseite, auf Marcels Anweisung gesetzt).

## Nova
- **Seit 2.4.8 ist das Vorlesen ausgeschaltet** (Schalter `Nova.ENABLED` in `core/model/.../Nova.kt`). Zum Wiedereinschalten: auf `true` setzen, bauen – dann gilt alles Folgende wieder.
- Experimentopia nutzt **nur bereits eingesprochene Nova-Dateien**: alle 2579 Sätze aus `/vorlesen/texte.json` sind vertont (Abdeckung 100 %, geprüft 17.11. abends).
- Die App braucht `/jetzt` derzeit **nicht** (keine Sätze, die erst zur Laufzeit entstehen). Die Sperre von `/jetzt` (Kontingent, seit ca. 23:45) betrifft Experimentopia also nicht.
- Erst wenn sich Texte wieder ändern, müssen neue Sätze vertont werden. Bis dahin gilt: neue Texte nur ändern, wenn das Stimmen-Werk wieder vertont – sonst liest dort die Gerätestimme.

## Noch nicht erledigt (wird fortgesetzt)
1. **Handytest durch Marcel**: Musik startet sofort beim Öffnen. (Nova-Test entfällt, solange Vorlesen aus ist.)
2. **Neue Musik** (seit 2.4.0) ist noch nicht von Marcel angehört – Rückmeldung abwarten (Tempo, Stil).
3. ~~Live schalten~~ – erledigt am 18.11.2026 (2.4.2). Der Leitstand sollte das wissen (diese Sitzung kann ihm nicht direkt schreiben).
4. **Website/Produktseite**: Gratis-Umfang ist seit 2.3.3 **74 von 200** Versuchen (Wasserinsel + erste 5 je Insel + erste 2 je Thema) – Texte dort ggf. anpassen.
5. **Play Store**: bleibt bis nach dem Betriebstest liegen.
6. Betriebstest (eine Woche): nur Fehlerbehebungen, Rückmeldungen aus dem Betrieb abarbeiten.
