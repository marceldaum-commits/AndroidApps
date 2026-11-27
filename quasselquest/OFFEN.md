# Quasselquest – OFFEN (Arbeitsliste, Stand 25.11.2026, Version 0.5.5)

Regel (Marcel/Leitstand 25.11.): Liste selbständig der Reihe nach abarbeiten, Erledigtes mit Datum nach „Erledigt“, nie löschen. Wartet ein Punkt auf andere: einmal über die AppZentrale mit Frist fragen, dann weitermachen, nach Fristablauf nachhaken. Live: 0.4.6 · lernen-test: 0.5.5 (nach Deploy).

## Offen (Reihenfolge = Arbeitsreihenfolge)
| # | Was | Frist | Nächster Schritt | Abhängigkeit | Status |
|---|---|---|---|---|---|
| 2 | Ukrainisch: Deutsch für ukrainische Kinder (DaZ): Ruby erklärt auf Ukrainisch, Lernpfad „Deutsch von Anfang an“, Umschalter „Meine Sprache: Українська“ | Do/Fr 26./27.11. | Konzept steht in `DAZ.md` (26.11.); als Nächstes `lernpfad/de-uk.json` Abschnitt 1 (4 Einheiten), dann „Meine Sprache“ + `i18n-uk.js` | Lektorat Muttersprache (Marcel entscheidet) | Lernpfad `de-uk.json` (4 Einheiten) geschrieben 27.11., Lektorat läuft; danach „Meine Sprache“ + `i18n-uk.js` |
| 3 | Ukrainisch: Textliste „uk“ für das Tonstudio (Piper tetiana, 1.0/0.8, ID3, Lizenztext, Wasserzeichen-Pflicht klären) | Fr 27.11. | `tools/textliste.py` um „uk“ erweitern, Commit melden | Tonstudio (über AppZentrale) | offen |
| 4 | Kokoro-Dateien mit Perth-Wasserzeichen einbauen (en → fr → es) | Einbau je Sprache ≤ 3 h nach Meldung, Release Mo 30.11., hart 01.12. | `tools/server-audio-einbauen.py`, Hörprüfung, ID3-/Perth-Check, Browser-Test, DSE-Zusatz „unhörbares digitales Wasserzeichen“ | Tonstudio (Übergabe Sa/So 28./29.11.) | wartet |
| 5 | Notbremse 0.4.7 „Ton aus“ (fertig als Patch `hotfix/`) | Entscheidung Mo 30.11., ausliefern bis 01.12. 12:00 | nur bei Verzug von 4 anwenden lassen | Leitstand/Marcel (Termin) | bereit |
| 6 | Pro-Umstellung 1.5.0 (6 Monate ab Einlösen, ein Gerät pro Code, Kauftext/AGB) | Stichtag offen | neue Texte hinter Flag, Server-Weg (signierte Einlösung) | Leitstand/Marcel (Stichtag), Server | wartet |
| 7 | Aussprache-Auswertung live (Weg 2) | offen | Flag `AUSWERTUNG_AKTIV` erst mit DSE 10g/10n/10k + Live-Server | Leitstand (Server), Recht | wartet |
| 8 | Weg 1 (Whisper im Gerät), „Im Gerät“-Option mit Modellgröße X | Dezember | Modell wählen, messen, Messtabelle | – | offen |
| 9 | Familien-Speicher live (`FAMILIE_AKTIV`) | offen | Tor-Hinweis, DSE 10m, Live-Server | Leitstand, Recht | wartet |
| 10 | familie.js `codeNummer()` Sprach-Byte 5 und 9 | offen | Bitte an Mathe-Trainer-Raum, dann familie.js neu übernehmen | Mathe-Trainer (über AppZentrale) | angefragt 24.11. |
| 11 | DSE-Anker `10n`/`aussprache` | offen | setzen, sobald Website-Raum Nummer+Name bestätigt | Website-Raum (über Marcel) | wartet |
| 12 | Probestunden-Weg V15 (Kinderhilfe-Satz, Eltern-Hinweis, Kl. 9–10-Tipp) | Mi/Do | nach `PROBESTUNDE.md` umsetzen, melden mit „Rechtsbezug: ja (V15)“ | – | offen |
| 13 | Tonstudio-Tor-Sätze (Eltern-Tor spricht wieder) | offen | Sätze vom Tonstudio/Rechenblitz einbauen | Tonstudio | wartet |
| 14 | „Heute dran“-Knopf, tägliche gemischte Wiederholung, Lernpfad EN Abschnitt 3, Latein Abschnitt 2, FR/ES je 2 weitere Pakete | Dezember | nach UEBERARBEITUNG.md/INHALTSPLAN.md | – | offen |
| 15 | Browser-Test 4 Geräte für 0.5.3–0.5.5 neu laufen lassen | Do 26.11. | `node tools/web-start.mjs` | – | offen |
| 16 | Ukrainisch im Browser bei `UK_AKTIV=true` prüfen (Sprachwahl, Lernpfad, Bildschirmfotos) | Do 26.11. | web-start mit Flag, Fotos ansehen | – | offen |

## Erledigt
- 24.11.2026: 0.5.2 (Familien-Speicher, Pro-Codes, familie.js af01c5c, schnellere Installation, Zoom), 0.5.3 (Recht-Wortlaute V39/V40/V42/V47), 0.5.4 (App-DSE Stimmen-Absatz, kein /nova/), Notbremse 0.4.7 mit Recht V47c, Format der Prüfliste ans Tonstudio.
- 25.11.2026: Ukrainisch-Lektorat (unabhängiger Durchlauf, 15 Funde) umgesetzt; Muttersprachler-Prüfung vor Freigabe bleibt offen (Punkt 2).
- 25.11.2026: 0.5.5 Ukrainisch-Inhalt (4 Einheiten) hinter Flag, Recherche in docs/RECHERCHE.md, Tagesmeldung gesendet.

---

# Archiv: Stand 19.11.2026 (ursprüngliche OFFEN.md, unverändert)

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
- **Schloss-Text bei Pro (AppZentrale 19.11., UWG Anh. Nr. 28):** kein Preis, kein Kaufen-Knopf, keine Aufforderung ans Kind (auch nicht „Frag deine Eltern“). Nur: „Das gehört zu Quasselquest Pro.“ (Marcel/Recht 19.11.), Weg ins Tor über neutralen Knopf „Für Eltern“ → Weg nur zum Eltern-Tor.

## Fr 20.11. – nur Korrekturen (Marcel: „Ja, nur Korrekturen“)
Kein Inhaltsupdate, keine neuen Funktionen (auch NICHT Familien-Speicher / Pro – kommt Di 24.11.). Stattdessen Selbstprüfung:
Start iPhone/WebKit + PC · alle Links + Eltern-Tor (kein Kinderpfad ohne Tor nach außen/zur Kasse) · Schloss-Texte · Inhalte fachlich korrekt ·
Zählung, Datenschutz-, Lizenz-Hinweise, Fehlerkonsole leer. Gefundenes beheben. Bis Fr 16 Uhr an AppZentrale: geprüft / gefunden+behoben / Commit / Rückweg / „Rechtsbezug: ja/nein“.
- **Recht V10 (Fr-Pflicht):** „← Alle Apps“ hinter dem Eltern-Tor – bei uns schon seit 0.4.0 so. Ergänzen: Grund im Tor „Hier geht es zur Website.“ (Tor bekommt optionalen Grund-Text). Links von App zu App: haben wir keine mehr (Wissensgarten-Link in 0.4.0 entfernt). Ab Di 24.11. zeigt „Alle Apps“ auf die neue preisfreie Übersicht (Adresse kommt) und darf wieder ohne Tor sein. Meldung mit „Rechtsbezug: ja (V10)“.
