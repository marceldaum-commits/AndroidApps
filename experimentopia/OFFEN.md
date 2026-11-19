# Experimentopia – Stand und offene Punkte (18.11.2026)

**Stand:** Version 2.12.0 vom 19.11.2026 (Direkt-Links + Querverweise; 2.11.2 Probestunde nur Physik; 2.11.1 „Alle Apps“ live; 2.11.0 Elternbereich + Zählung; 2.10.0 PC-Ansicht live; 2.9.1 #pro=-Link; 2.9.0 Sicherung + Wiederherstellen; 2.8.0 Eltern-Handy koppeln, live; 2.7.0 Optik + „Kommt bald“; davor 2.6.2 Theorie mit Formeln; iPad/Safari 27.2 läuft – bestätigt) liegt auf `experimentopia-test`; live ist 2.8.0 (Leitstand, 18.11.), Freigabe durch den Leitstand.

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
9. **Rhythmus** (`PRUEFPLAN.md`): großes Inhaltsupdate Di + Fr bis 16 Uhr auf test (erstes: Fr 20.11.), Monatsprüfung Dezember = Block A (Wasser + Kraft).
10. **Sicherung** (2.9.0): Zwei-Browser-Test auf der Test-Seite (koppeln → privates Fenster → Link einfügen → gleicher Stand). Leitstand gibt live frei; Marcel möchte Bescheid, wenn alles live ist.
11. ~~Probestunden-Adresse~~ – erledigt: `/probestunde/` ist laut Leitstand richtig (18.11.).

- **Vorlesen (Nova, derzeit aus)**: Beim Wiedereinschalten den Zählstand-Pfad für die gemeinsame Adresse klären (`Audio.kt` nimmt den Seiten-Host, an der neuen Adresse wäre das `stand-lernen.….json`). Der Stimmen-Server erlaubt lernen(-test).… schon (CORS geprüft am 19.11.).

- **Fr 20.11. mit dem großen Update (Pflicht, Marcel auf Empfehlung von Recht, 19.11.)**: Schloss-Text in der Kinderansicht NUR „„<Versuch>“ gehört zum Forscher-Pass.“ – Zusatz „Deine Eltern können im Elternbereich nachsehen.“ entfällt (auch in `Say.PRO_ALL`). „🔐 Für Eltern“ zum Tor bleibt. `KinderTextTest` um „Deine Eltern können“ ergänzen, `proKind` anpassen. Live-Meldung mit „Rechtsbezug: ja (Kindertext am Schloss)“.

- **Fr 20.11. Pflicht-Korrektur Recht V10 (Marcel: „Erst Tor, dann Übersicht“)**:
  (a) „← Alle Apps“ (…/lern-apps/ auf der Website, dort Preise/Shop) HINTER das Eltern-Tor, Grund im Tor: „Hier geht es zur Website.“ Ab Di 24.11. neue Übersicht ohne Preise auf lernen.… (Adresse kommt von der AppZentrale) → dann wieder ohne Tor.
  (b) Querverweise: ohne Tor ok, wenn die Ziel-App dem Tor-Standard folgt. Mathe-Trainer (und Raumlicht) zeigen in der Oberstufe Preise ohne Tor → unsere 3 Mathe-Trainer-Ziele (k9-aehnlich, k7-zuordnung, k8-zufall) prüfen: zeigt die Zielseite Preise? Wenn ja oder unklar: über das Eltern-Tor führen (bzw. nur ab Kl. 9 direkt, wenn die Zielseite keine Preise zeigt). Meldung: „Rechtsbezug: ja (V10)“.
  Ergänzung AppZentrale 19.11. abends: Mathe-Trainer-Links ab Fr OHNE Tor ok (Mathe-Trainer bekommt selbst ein Tor vor Preisen) → bei uns nichts zu ändern. Raumlicht für Kinder unter Kl. 9 nur preisfreie Einstiege (galton, zufall, rotation, raumflug, level/3-1, level/5-1, level/5-6), NICHT „erleben“, Startseite oder Pro-Seiten → geplanter Energie-Verweis (bisher ?ziel=erleben) muss auf einen dieser Einstiege oder entfällt.
  **ÄNDERUNG 19.11. abends (Marcel: „Ohne Tor, neutral“)**: V10 (a) ENTFÄLLT – „← Alle Apps“ bleibt OHNE Eltern-Tor (neutral: keine Kaufsprache, keine Preise/Countdowns in der Kinderansicht, kein Link direkt in Warenkorb/Kasse). Kauf-/Shop-Knöpfe, Elternbereich, Einstellungen bleiben hinter dem Tor. ELTERN-TOR.md (appzentrale, 0d1c53b).

- **Ab Di 24.11. (Recht V15, Marcel „Ja, alles so umsetzen“): Weg zur Probestunde** – Wortlaut verbindlich in docs/plattform/appzentrale/PROBESTUNDE.md (website_backup, appzentrale, d672374; ggf. über die AppZentrale anfordern).
  (1) Elternbereich/Elternbericht hinter dem Tor: Hinweis bei objektiven Schwierigkeiten, Berechnung nur auf dem Gerät, höchstens 1× in 4 Wochen je Thema, wegklickbar, Schalter „Hinweise auf Nachhilfe ausblenden“. Bei uns: nur Physik-Themen (Nachhilfe nur Physik).
  (2) Kinderansicht nur: „Du kommst hier gerade nicht weiter? Das ist okay. Probier eine leichtere Stufe oder schau dir die Erklärung noch einmal an. Und du kannst jederzeit deine Eltern oder deine Lehrkraft fragen.“ Knöpfe [Leichter üben] [Erklärung ansehen], kein Außenlink.
  (3) Kl. 9–10 (wenn Klasse bekannt), ohne Tor: „Tipp: Wenn ein Thema länger hakt, kann eine persönliche Erklärung helfen. Mit deinen Eltern könnt ihr bei uns eine kostenlose, unverbindliche Probestunde anfragen. [Mehr erfahren]“ höchstens 1×/Woche, wegklickbar. Kl. 5–8 nur hinter dem Tor. Zählung nur anonym. Meldung „Rechtsbezug: ja (V15)“.
