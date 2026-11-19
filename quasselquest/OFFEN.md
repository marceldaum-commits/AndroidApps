# Quasselquest – Stand und offene Punkte

**Stand:** Version 0.2.1 vom 19.11.2026 auf `quasselquest-test` (Commit a183ccf). Live-Meldung ist beim Leitstand (über die AppZentrale); live geht es über `lernen.mathenachhilfe-online.com/quasselquest/`.

## Nächste Version (klein, vor Di 24.11.)
- Test-Stufe nur noch über den Hostnamen erkennen („lernen-test.“, „-test.“); die Pfad-Regel `/test/` entfällt, weil der Standard sie nicht vorsieht.
- Elternbereich, Mikrofon (Hinweis „Geld verdienen“): Wenn das Gerät keine Erkennung auf dem Gerät kann (iPad/Safari), ehrlich erklären:
  „Auf diesem Gerät gibt es die Aussprache-Prüfung nur mit der Online-Erkennung von Apple – möchtet ihr sie erlauben?“ Kein Versprechen im Pro-Text, das ohne Zustimmung nicht geht.
- Lernen-test prüfen (`version.json`), sobald die Adresse steht.

## Zeitplan (Vorschlag, Marcel entscheidet am Mo 23.11.)
| Termin | Inhalt |
|---|---|
| Di 24.11. | „Warum?“ (falsche Stelle + Regel), eigene Wortlisten „Mein Schulbuch“ |
| Fr 27.11. | Klassenarbeits-Generalprobe (Pro-Kern), Nachhilfe-Hinweis im Elternbereich |
| Di 1.12. | Wortlisten per QR, Datenschutz in Kindersprache + Datenblatt für Schulen, eigener Grundwortschatz |
| danach | Latein-Welt, Deutsch-Welt, Dolmetsch-Mission, Diktat |

## Bekannt vom Leitstand / anderen Räumen
- Pro-Codes: App-Nummer 7, Präfix „QQ-“, Link `#pro=CODE`. Shop-Schlüssel noch nicht scharf → nichts Kaufbares zeigen.
- Pro je Sprache 7,99 €, alle Sprachen 19,99 €. Kaufschutz: Wissensgarten-Plus-Code (öffentlicher Ed25519-Schlüssel 979eb515…b812, Format in `wissensgarten/core/model/.../License.kt`) schaltet Englisch und Deutsch frei.
- Plus-Pakete (ob-englisch-abi-vokabeln, aw-experte-sprache): privat in `website_backup`, Branch `wg-plus-pakete`, nur über den Plus-Endpunkt – nie öffentlich ablegen.
- Nachhilfe-Hinweis: `https://www.mathenachhilfe-online.com/probestunde/?src=quasselquest-elternbericht&thema=<englisch|deutsch|franzoesisch|latein|spanisch>&klasse=<k>`, vor Klassenarbeiten `src=quasselquest-klassenarbeit`.
- Zählung: `app=quasselquest`, Themen englisch, deutsch, franzoesisch, latein, spanisch sind am Server angelegt.
- Tagesrunde Mo–Fr 9:43 Uhr an die AppZentrale (Erinnerung eingerichtet), Wochenrunde montags.

## Entscheidungen Marcel 19.11. (über AppZentrale)
- **Offline-Audio je Sprache:** Grundausstattung (Englisch-Hafen + aktuelle Sprache) lädt automatisch, weitere Sprachen per Knopf mit Größe („Französisch offline laden (35 MB)“). → Service Worker: nicht mehr alle MP3 beim Installieren; Audio-Index je Sprache; Knopf in Einstellungen/Elternbereich. Kein Server nötig – nach 0.4.0 bauen.
- **Pro:** Gekauftes Pro läuft nie ab; nur Probe-/Geschenk-Codes mit Ablaufdatum (offline geprüft, spätestes gesehenes Datum merken). Quasselquest hat noch keinen Pro-Code (Pro neutral vorbereitet: QQ-, App 7, Sprach-Byte) → bis Mo 23.11. so an AppZentrale melden.
- **Familien-Speicher OHNE Google (ersetzt Eltern-Login-Nachricht):** Lern-/Spielstände, Pro, KI pro Familie verschlüsselt auf unserem Server (eltern/v1), Kopplung per Familien-Code/QR, automatische Sicherung bei Verbindung, Notfall-Code zum Ausdrucken + Generalschlüssel (Marcel). Kein Google, kein Konto fürs Kind. Noch nichts Serverseitiges bauen (Wochenrunde 23.11.).
- **Bis Mo 23.11. melden:** Lernstand = localStorage `quasselquest.v1` (alt `englisch.v1`), JSON mit klasse, name, lang, karten (Leitner je Karte), eigene Listen, tage, hinweis, tests, lp (Lernpfad), insel (Positionen), progress, settings, week – Größe messen; Pro-/Geschenk-/KI-Codes: noch keine.

## Aufträge aus dem USP-Bericht (Geld verdienen, 19.11.) – nach den Pflichtaufgaben, nach Wirkung in Di/Fr-Updates
Lücken: Sprechpraxis mit Rückmeldung in Schulwerkzeugen; spielerisches Latein aus dem Deutschen (ca. 539.000 Lateinschüler); Training im Klassenarbeits-Format mit eigenen Schulbuch-Vokabeln. Duolingo for Schools endet 31.07.2027.
1. Eigene Wortlisten in alle Spielarten (gratis) – **weitgehend erledigt** (Mein Schulbuch: Ballon, Hören, Bauen, Memory, Diktat); prüfen: Hör-Detektiv für eigene Listen.
2. Generalprobe als Pro-Kern je Sprache mit Hören, Lesen, sprachlichen Mitteln, Vokabeln beide Richtungen, ab Kl. 7 Sprachmittlung – **Grundform in 0.3.3**, Ausbau offen.
3. Regel-Rückmeldung für jede Fehlerart + Test für Abdeckung – Grundform („Warum?“) vorhanden, Abdeckungstest fehlt.
4. Leichter Klassenmodus ohne Konten (Klassen-Code) vor Sommer 2027.
Elternbereich: Einmalkauf, Gratis-Sicherung und Kontofreiheit ausdrücklich nennen. Schullizenz einheitlich 29 €/Klasse, 149–249 €/Schule.
- **Schloss-Text bei Pro (AppZentrale 19.11., UWG Anh. Nr. 28):** kein Preis, kein Kaufen-Knopf, keine Aufforderung ans Kind (auch nicht „Frag deine Eltern“). Nur sachlich: „Das gehört zu Quasselquest Pro. Deine Eltern können im Elternbereich nachsehen.“ → Weg nur zum Eltern-Tor.
