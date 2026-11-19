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
- **Eltern-Login (Google oder Familien-Code):** serverseitig, Leitstand richtet ein; Feinplanung Wochenrunde 23.11. – noch nichts bauen.
