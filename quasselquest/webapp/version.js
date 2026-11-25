// Eine Quelle für Version und Datum (auch version.json und sw.js müssen passen – prüft tools/test-inhalt.mjs).
window.APP_VERSION = '0.5.4';
window.APP_VERSION_DATE = '24.11.2026';
// Kennung für Links und Zählung (Name entschieden von Marcel am 19.11.2026).
window.APP_ID = 'quasselquest';
window.APP_NAME = 'Quasselquest';
// Aussprache-Auswertung über unseren Server (Weg 2): vorbereitet, AUS. Erst mit DSE 10n/10k, den Elterntexten von Recht und der Live-Freigabe auf true setzen.
window.AUSWERTUNG_AKTIV = false;
// Familien-Speicher (Lernstand verschlüsselt auf unserem Server, nur hinter dem Eltern-Tor): fertig und AUS, bis Recht den neuen Kinder-Datentext freigegeben hat
// und der Server des Leitstands live ist (FAMILIEN-SPEICHER.md). Ohne true: keine Auswahl, kein Aufruf.
window.FAMILIE_AKTIV = false;
// Quasselquest Pro (Version 1.5.0, PRO.md): Code-Prüfung und Sperre je Sprache sind fertig und AUS. Erst mit Test-Codes vom Leitstand, Recht-Freigabe
// und Shop auf true setzen (dann gilt: kostenlos bleibt Grundwortschatz, erste Episode, Lernpfad Abschnitt 1 + je erste Einheit ab Abschnitt 2).
window.PRO_AKTIV = false;
