# Notbremse 0.4.7 „Ton aus“ (nur falls 0.5.x mit Kokoro/Perth am 01.12.2026 nicht live ist)

Marcel (24.11.2026): Fällt 0.5.x bis 01.12. aus, werden die Piper-Audios in der Live-Version 0.4.6 abgeschaltet.
0.4.6 hat keinen Schalter, deshalb liegt hier ein fertiger Patch auf Basis ae7a4273 (0.4.6):

- `0.4.7-ton-aus.patch`: `audio.js` liefert keine vorab vertonten Dateien mehr, `sw.js` lädt und merkt keine `audio/piper` mehr
  (neue Cache-Version `quasselquest-0.4.7` löscht beim Aktivieren die alten Dateien aus dem Gerätespeicher), Version 0.4.7 vom 01.12.2026.
- Die Gerätestimme (Browser, keine KI) bleibt. Soll auch sie aus, `A.canSpeak` auf false setzen (zusätzliche Änderung).
- Anwenden (Leitstand, Abend 01.12.): auf Stand ae7a4273 `git apply hotfix/0.4.7-ton-aus.patch`, deployen. Danach auf dem Server `webapp/audio/piper/` löschen.
- Wirkt erst, wenn ein Gerät die neue Version lädt (Prüfung bei jedem Öffnen und alle 30 min); offline-Geräte behalten bis dahin ihren Speicher.
- Recht V47c (24.11.2026): freigegeben nur als Notbremse, Vorrang hat 0.5.x live. Entscheidung spätestens Mo 30.11., Auslieferung bis 01.12. 12:00, Piper-Dateien unter Server-Adresse am selben Tag sperren.
- Der Patch ersetzt den Stimmen-Absatz in `datenschutz.html` durch den Recht-Wortlaut („Zum Vorlesen nutzt die App die Vorlesestimme deines Geräts bzw. Browsers. Dabei schickt die App keine Daten an uns. Ob dein Gerät oder Browser dafür einen Dienst seines Herstellers nutzt, hängt von dessen Einstellungen ab. Es werden keine Stimmen von uns oder von Dritten abgerufen.“), entfernt Piper/Nova aus den Einstellungen und der Fußzeile, zwingt die Gerätestimme und zeigt „Die Aussprache-Dateien werden gerade erneuert. Bis dahin liest die Stimme deines Geräts vor.“
