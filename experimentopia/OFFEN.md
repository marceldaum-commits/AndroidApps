# Experimentopia – Stand und offene Punkte (18.11.2026)

**Stand:** Version 2.8.0 vom 18.11.2026 (Eltern-Handy koppeln; 2.7.0 Optik + „Kommt bald“; davor 2.6.2 Theorie mit Formeln; iPad/Safari 27.2 läuft – bestätigt) liegt auf `experimentopia-test`; live ist 2.4.8, Freigabe durch den Leitstand.

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
5. **Nur WebApp** (seit 2.5.0, für 1–2 Monate): keine APK, kein Play. Android-Code bleibt im Repo; Release-Workflow nur von Hand. Live gibt der Leitstand frei.
6. Betriebstest (eine Woche): nur Fehlerbehebungen, Rückmeldungen aus dem Betrieb abarbeiten.
7. **Lehrplan-Lücken** (`docs/LEHRPLAN-ABGLEICH.md`): Marcel hat am 18.11. Bauen + „Kommt bald“ freigegeben. Erledigt: Optik (2.7.0). Als Nächstes: Motor/Dynamo/Trafo, Energie, Bio Kl. 5/6, Atombau/PSE – jeweils Kachel aus `Coming` in `Upcoming.kt` entfernen, wenn gebaut.
8. **Eltern-Koppeln** (2.8.0): echter Handytest auf der Test-Seite + Eltern-Seite (Leitstand). Vor Live: CORS des Live-Servers für experimentopia.mathenachhilfe-online.com.
9. **Prüfplan** (`PRUEFPLAN.md`): wöchentlich neuer Inhalt (mittwochs), Monatsprüfung Dezember = Block A (Wasser + Kraft).
