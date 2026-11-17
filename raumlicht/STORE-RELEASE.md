# Raumlicht in den Play Store bringen

Stand: Version 2.17.0 vom 17.11.2026. Vorbereitet ist alles, **veröffentlicht ist nichts**. Die Schritte in der Play Console macht Marcel selbst.
Hinweis: Die Beschriftungen in der Play Console ändern sich gelegentlich – die Namen unten können leicht abweichen.

## Was fertig ist

| Was | Wo |
|---|---|
| Store-Paket (AAB), signiert mit dem Upload-Schlüssel | GitHub → Releases → „Raumlicht 2.17.0 (17.11.2026)“ → `Raumlicht-2.17.0-play.aab` |
| Play-Fassung der App | Fassung `play`: kein eigener Updater, keine Berechtigung „Apps installieren“, Updates über den Store (Play In-App Updates) |
| Kein GPL-Baustein | espeak-ng ist seit 2.6.0 nicht mehr enthalten; seit 2.17.0 steckt gar keine Sprachsynthese mehr in der App (siehe `LIZENZEN.md`) |
| Store-Texte (Deutsch) | `store/texte.md` |
| Grafiken und Bildschirmfotos | `store/grafik/`, `store/bildschirmfotos/` |
| Antworten für „Datensicherheit“ und „Altersfreigabe“ | unten in dieser Datei |

Der GitHub-Lauf prüft bei jedem Bau: AAB mit dem Upload-Schlüssel signiert, keine Installations-Berechtigung, keine espeak-ng-Daten.
Technik: Zielversion Android 16 (API 36), ab Android 8 (API 26), Maschinencode für 16-KB-Speicherseiten ausgerichtet.

## Wichtig vorab: der Schlüssel

Der Play Store signiert Apps selbst („Play App Signing“). Dafür gibt es zwei Wege – **die Entscheidung fällt beim ersten Hochladen und lässt sich nicht zurücknehmen**:

1. **Google erzeugt einen neuen Schlüssel (Voreinstellung).** Einfach. Aber: Wer Raumlicht schon als APK von GitHub installiert hat, kann die Store-Version
   nicht darüber installieren (andere Signatur). Die APK müsste erst gelöscht werden – **dabei geht der Spielstand verloren**.
2. **Den vorhandenen Schlüssel aus Bitwarden als App-Signaturschlüssel verwenden** (in der Play Console beim App-Signatur-Schritt:
   „Anderen Schlüssel verwenden“ → „Schlüssel aus Java-Keystore exportieren und hochladen“; Google beschreibt dort das Vorgehen mit dem Werkzeug `pepk`).
   Dann passen Store-Version und GitHub-APK zusammen, und der Spielstand bleibt.

Empfehlung: Weg 2, wenn schon jemand (du, Testkinder) die GitHub-APK mit Spielstand nutzt. Sonst Weg 1.
Der Schlüssel bleibt dabei in Bitwarden – nicht ins Repo, nicht in einen Chat.

## Klick-Liste

1. **Play Console öffnen** (play.google.com/console) → „App erstellen“. Name: `Raumlicht – Mathe-Abi in 3D`, Sprache Deutsch, App (nicht Spiel), kostenlos.
2. **Store-Eintrag** → Texte aus `store/texte.md` einfügen, Icon, Vorstellungsgrafik und die acht Bildschirmfotos hochladen. Kategorie „Lernen“.
3. **App-Inhalte** (linke Leiste, „Richtlinien“):
   - Datenschutzerklärung: `https://www.mathenachhilfe-online.com/datenschutz/`
   - Werbung: **Nein**
   - App-Zugriff: „Alle Funktionen ohne besondere Zugangsdaten verfügbar“
   - Altersfreigabe: Fragebogen wie unten
   - Zielgruppe: wie unten
   - Datensicherheit: wie unten
   - Regierungs-, Finanz-, Gesundheits-Apps: jeweils **Nein**
4. **Test → Interner Test** → „Neuen Release erstellen“ → beim ersten Mal die Schlüssel-Frage (siehe oben) → `Raumlicht-2.17.0-play.aab` hochladen →
   Release-Name `2.17.0 (17.11.2026)`, „Was ist neu“ aus `store/texte.md` → speichern → prüfen → „Einführung starten“.
5. **Tester eintragen** (E-Mail-Liste) und den Einladungs-Link auf dem eigenen Handy öffnen. Prüfen: App startet, Aufgabe lösen, Musik, Einstellungen (dort darf **kein** „App-Update“-Kasten stehen).
6. Erst danach: geschlossener Test bzw. Produktion. **Neue private Entwicklerkonten** müssen laut Google vor der Produktion einen geschlossenen Test
   mit einer Mindestzahl an Testern über mehrere Tage nachweisen – die aktuelle Zahl zeigt die Play Console an.

Spätere Versionen: Version in `app/build.gradle.kts` erhöhen und pushen → GitHub baut das neue AAB → in der Play Console als neuen Release hochladen.
Dringende Updates: beim Release die „Update-Priorität“ auf 4 oder 5 setzen (geht nur über die Play-Developer-API) – dann verlangt die App das Update sofort; sonst lädt sie es im Hintergrund.

## Antworten: Datensicherheit

- Erhebt oder teilt die App Nutzerdaten? **Seit 2.17.0 neu zu beantworten (bitte vor dem Einreichen prüfen lassen) – bis 2.16.x war die Antwort „Nein“.**
- Unverändert: kein Konto, keine Werbung, keine Analyse, kein Absturz-Dienst. Spielstand und Einstellungen liegen nur auf dem Gerät.
  Der Abbruch-Bericht in den Einstellungen bleibt auf dem Gerät.
- Netzwerkzugriffe: (1) Play Store für Updates, (2) **Vorlesen mit „Nova“** (nur wenn „Lux spricht“ eingeschaltet ist): Die App lädt fertige MP3-Dateien von
  stimmen.mathenachhilfe-online.com. Texte, die erst in der Aufgabe entstehen (z. B. Rückmeldungen, die **eingegebene Zahlen** enthalten), schickt sie an diesen Server;
  der lässt sie einmalig von OpenAI vertonen und speichert die Audiodatei. Gesendet werden nur Text und Sprache – kein Name, kein Konto, keine Geräte-Kennung.
- **Offene Frage für das Formular:** Ob diese Texte nach Googles Definition als „erhobene“ oder „geteilte“ Daten gelten (Eingaben des Nutzers verlassen das Gerät und
  gehen über unseren Server an einen Dritten), muss vor dem Einreichen geklärt werden. Im Zweifel angeben. Keine Rechtsberatung.
- Verschlüsselung bei der Übertragung: Ja (HTTPS). Löschung von Daten: nicht zutreffend (es gibt keine).

## Antworten: Altersfreigabe (Fragebogen)

- Kategorie: Referenz/Bildung (bzw. „Alle anderen App-Typen“)
- Gewalt, Blut, Angst: **Nein** (In der „Ballerbude“ lässt eine Drohne bunte Ballons platzen – keine Figuren, keine Waffen-Darstellung. Falls der Fragebogen
  nach „Schießen auf Objekte“ fragt: ehrlich „Ja, Comic-Stil, keine Lebewesen“ angeben.)
- Sexualität, Sprache, Drogen, Glücksspiel: **Nein** (Zufallsexperimente wie Würfel und Galton-Brett sind Mathematik, kein Glücksspiel, kein Einsatz.)
- Nutzer-Interaktion, Standort-Weitergabe, Käufe: **Nein**
- Erwartetes Ergebnis: USK 0 / PEGI 3.

## Antworten: Zielgruppe

- Altersgruppen: **16–17** und **18+** (Oberstufe ab Klasse 11). Nicht „unter 13“ ankreuzen – sonst gelten die Regeln für Kinder-Apps („Familien-Richtlinie“)
  mit zusätzlichen Auflagen, die für eine Oberstufen-App nicht gedacht sind.
- „Könnte die App Kinder ungewollt ansprechen?“ – ehrlich: **Ja, möglich** (bunte Figur Lux). Das ist erlaubt; Google kann dann den Hinweis „Nicht für Kinder konzipiert“ anzeigen.

## Offen / ehrlich

- Auf einem echten Gerät hat der Entwickler-Assistent nichts getestet (kein Gerät vorhanden): Play In-App Updates lassen sich erst mit einer über den Store installierten Version prüfen.
- Die Bildschirmfotos stammen aus den automatischen Tests (echte App-Oberfläche, 3D-Bühne gerendert), nicht von einem Handy.
- Die Datenschutzerklärung der Website muss die App nennen (lokaler Spielstand, Vorlesen mit Nova über den eigenen Server und OpenAI, Play-Store-Updates) – Vorschlag: `store/datenschutz-abschnitt.md`. Keine Rechtsberatung.
