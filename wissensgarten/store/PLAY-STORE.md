# Wissensgarten im Google Play Store veröffentlichen

Alles, was die Play Console abfragt – in der Reihenfolge, in der sie es abfragt. Firmenkonto: Die Pflicht
„12 Tester × 14 Tage“ gilt nur für private Konten, ihr könnt direkt in die Produktion.

## 1. Vorher einmalig
- **Datenschutz-Seite hochladen:** `web/datenschutz/index.html` nach `https://www.mathenachhilfe-online.com/datenschutz/`
  (wie damals `web/duell` und `web/klasse`). Diese Adresse kommt in die Play Console.
- **Impressum** auf mathenachhilfe-online.com muss Name/Anschrift/E-Mail enthalten (die Datenschutzerklärung verweist darauf).

## 2. App anlegen
Play Console → *App erstellen* → Name **Wissensgarten**, Sprache **Deutsch**, *App*, *Kostenlos*.

## 3. App-Bundle hochladen
- Datei: `Wissensgarten-<Version>-PlayStore.aab` – hängt automatisch an jedem GitHub-Release.
- **Play App Signing:** aktivieren. Unser Schlüssel (`WG_KEYSTORE_…`) ist der *Upload-Schlüssel* – Google verwaltet den App-Schlüssel.
- Die Play-Version hat **keinen eigenen Updater** (Pflicht) – Updates kommen über Google Play; die App fragt beim Öffnen danach.
- Die Wissens-Safari (Bilderkennung) ist in der Play-Version ausgeschaltet (Familien-Richtlinie, siehe unten).

## 4. App-Inhalte (Menü *Richtlinien → App-Inhalte*)
| Abfrage | Antwort |
|---|---|
| Datenschutzerklärung | `https://www.mathenachhilfe-online.com/datenschutz/` |
| Werbung | **Nein**, keine Werbung |
| App-Zugriff | Alle Funktionen ohne Anmeldung. Hinweis für Prüfer: „Eltern-Bereich (Einstellungen, Teilen, Sync) öffnet sich nach einer Rechenaufgabe (zweistellig × einstellig).“ |
| Einstufung (IARC-Fragebogen) | Kategorie *Bildung/Referenz*; Gewalt, Sex, Sprache, Drogen, Glücksspiel: alles **nein**; Nutzer interagieren: **nein** (Duelle laufen über geteilte Codes, kein Chat); teilt Standort: **nein**; digitale Käufe: **nein** → erwartet: USK 0 / PEGI 3 |
| Zielgruppe | **6–8, 9–12, 13–15, 16–17 und 18+** (Kinder *und* Eltern – „gemischte Zielgruppe“). „Spricht die App Kinder an?“ → **Ja** |
| Familien-Richtlinie | Bestätigen. Wichtig: Bereiche mit Google-Diensten (Sync-Anmeldung, QR-Scanner) liegen hinter dem Eltern-Tor; Lerninhalte gehen ohne. |
| Nachrichten-App/Regierung/Finanzen/Gesundheit | Nein |
| Datensicherheit | siehe Abschnitt 5 |

## 5. Datensicherheits-Formular
- **Werden Nutzerdaten erhoben oder geteilt?** → **Ja** (nur wegen des freiwilligen Geräte-Syncs)
- **Verschlüsselt bei der Übertragung?** → **Ja** (HTTPS)
- **Können Nutzer das Löschen beantragen?** → **Ja** (Einstellungen → „Alle Daten löschen“, löscht auch die Sync-Datei)
- **Erhobene Daten** (alle: *optional*, Zweck *App-Funktionen*, **nicht geteilt**, nicht verarbeitet für Werbung):
  - *Personenbezogene Daten → Name*: Vorname/Spitzname des Profils – nur bei eingeschaltetem Sync, liegt im **eigenen** Google Drive der Familie
  - *App-Aktivitäten → Sonstige nutzergenerierte Inhalte*: Lernstand, eigene Karten – nur bei Sync
  - *Personenbezogene Daten → E-Mail-Adresse*: Google-Konto der Eltern für den Sync (nur auf dem Gerät gespeichert, zur Anzeige)
- **Nicht erhoben:** Standort, Kontakte, Fotos (Fotos/Kamera bleiben auf dem Gerät), Audio (die App bekommt von der Android-Spracherkennung nur Text, speichert/sendet keine Aufnahmen), Geräte-IDs, Absturzberichte, Analysedaten.

## 6. Store-Eintrag (*Wachstum → Store-Präsenz → Haupteintrag*)
| Feld | Datei |
|---|---|
| App-Name (max. 30) | `texte/titel.txt` |
| Kurzbeschreibung (max. 80) | `texte/kurzbeschreibung.txt` |
| Vollständige Beschreibung | `texte/beschreibung.txt` |
| App-Symbol 512 × 512 | `icon-512.png` |
| Titelbild 1024 × 500 | `titelbild-1024x500.png` |
| Smartphone-Screenshots (8) | `handy/*.png` (1080 × 1920) |
| Tablet-Screenshots 7" und 10" (je 8) | `tablet/*.png` (1600 × 2560) |
| Kategorie | **Bildung**; Tags: Lernen, Kinder, Wortschatz |
| Kontakt-E-Mail | eure Support-Adresse |

## 7. Optional: „Teacher Approved“
Apps im Familienprogramm werden automatisch von Lehrkräften begutachtet. Ein Abzeichen gibt es nur, wenn sie überzeugt sind –
Pluspunkte: keine Werbung, Eltern-Tor, fachlich geprüfte Inhalte, Vorlesen und Silben für Leseanfänger.

## 8. Später: Updates
GitHub-Version wie gehabt (push → Release). Für den Store: die neue `…-PlayStore.aab` aus dem Release in der Play Console hochladen
(*Produktion → Neuen Release erstellen*). Die Versionsnummer erhöhe ich bei jeder neuen Version mit.
