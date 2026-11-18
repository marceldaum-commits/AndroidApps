# Wissensgarten im Google Play Store veröffentlichen

> **Ruht gerade (seit 18.11.2026):** Vorerst gibt es den Wissensgarten nur als WebApp (PWA). Keine APK, kein Play-Store-Eintrag.
> Diese Anleitung gilt wieder, sobald es als Android-App weitergeht. Kauf von Plus läuft bis dahin über den Shop auf
> mathenachhilfe-online.com (Code per E-Mail).

Stand: **Version 0.19.0 vom 16.11.2026** (versionCode 33). Alles, was die Play Console abfragt – zuerst als Klick-Liste,
darunter die Antworten im Detail.

## Schritt für Schritt (Klick-Liste für die Play Console)

1. **Datei holen:** GitHub → Releases → *Wissensgarten 0.19.0* → `Wissensgarten-0.19.0-PlayStore.aab` herunterladen.
   Das AAB ist schon mit dem Upload-Schlüssel signiert (der Workflow nimmt ihn aus den GitHub-Secrets `WG_KEYSTORE_…`;
   das Original liegt nur in Bitwarden, nie in Git).
2. Play Console → **App erstellen** → Name „Wissensgarten“, Standardsprache **Deutsch (Deutschland)**, *App*, *Kostenlos*,
   Erklärungen bestätigen → *App erstellen*.
3. **Dashboard → „App einrichten“** – die Aufgaben der Reihe nach abhaken (Antworten: Abschnitt 4 und 5 unten):
   - *Datenschutzerklärung* → `https://www.mathenachhilfe-online.com/datenschutz/`
   - *App-Zugriff* → „Alle Funktionen ohne Einschränkungen verfügbar“ + Hinweis zum Eltern-Tor (Tabelle unten)
   - *Anzeigen* → **Nein**
   - *Einstufung des Inhalts* → E-Mail angeben, Kategorie **Referenz, Nachrichten oder Bildung**, Fragebogen (unten) → *Speichern* → *Einstufung einreichen*
   - *Zielgruppe und Inhalte* → Altersgruppen **6–8, 9–12, 13–15, 16–17, 18+**; „Kinder als Zielgruppe“ → Familienprogramm-Richtlinien bestätigen
   - *Nachrichten-App* Nein · *COVID/Gesundheit* Nein · *Finanzfunktionen* keine · *Regierungs-App* Nein
   - *Datensicherheit* → Formular nach Abschnitt 5
   - *Kategorie und Kontaktdaten* → **Bildung**, Kontakt-E-Mail, Website `https://www.mathenachhilfe-online.com`
   - *Store-Eintrag* → Texte, Symbol, Titelbild, Screenshots aus diesem Ordner (Abschnitt 6)
4. **Testen → Interner Test** → *Neuen Release erstellen* → bei der ersten Frage **Play App Signing: „Von Google verwalteten Schlüssel verwenden“**
   → AAB hochladen → Release-Name „0.19.0“ → Versionshinweise (Text aus `CHANGELOG.md`, Abschnitt 0.19.0) → *Speichern* → *Überprüfen* → *Einführung starten*.
   *Tester*: E-Mail-Liste anlegen (z. B. eigene Familie), Link „Im Web teilen“ an die Tester schicken → App über Play installieren und ausprobieren.
5. **Produktion:**
   - *Firmen-/Organisationskonto* (so wurde das Konto beantragt): **Produktion → Neuen Release erstellen** → dasselbe AAB aus der
     App-Bundle-Bibliothek übernehmen → *Einführung starten*. Die erste Prüfung durch Google dauert meist einige Tage.
   - *Falls die Play Console doch „Private Konto“-Regeln anzeigt* (Hinweis „Geschlossener Test erforderlich“): zuerst
     **Testen → Geschlossener Test** mit mindestens **12 Testern**, die **14 Tage** dabeibleiben; danach „Produktionszugriff beantragen“.
6. **Updates später:** neue Version auf GitHub (passiert automatisch beim Ausliefern) → das neue `…-PlayStore.aab` unter
   *Produktion → Neuen Release erstellen* hochladen. versionCode steigt bei jeder Version, das passt automatisch.

## 1. Vorher einmalig
- **Datenschutz-URL:** `https://www.mathenachhilfe-online.com/datenschutz/` – diese Adresse steht bereits in der App (Einstellungen → Über,
  Datenschutz-Bildschirm) und in der WebApp. Vorschlag: Der Leitstand ergänzt die **Haupt-Datenschutzerklärung** um einen Abschnitt
  „Wissensgarten (App und WebApp)“; der vollständige, für Eltern geschriebene Text liegt als Vorlage in `web/datenschutz/index.html`
  (wird aus derselben Quelle erzeugt wie der Datenschutz-Bildschirm der App). Eine eigene Seite ist nicht nötig.
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
- Die Spracherkennung (Brummi-Radio, Sprech-Spiel) läuft über die Android-Spracherkennung; die App erhält nur Text, keine Aufnahmen
  → bei „Audio“ **nicht erhoben** angeben.
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

## 9. In-App-Käufe: Wissensgarten Plus (kommt später)
Heute ist alles kostenlos; im Laden (Reiter „⭐ Plus“) steht „Kommt bald“, Plus-Pakete sind bis dahin gratis.
Store-Text und Datenschutzerklärung kündigen Plus bereits an (Kauf nur durch Eltern, über Google Play).

**Wenn es losgehen soll:**
1. Play Console → *Monetarisierung* → **Zahlungsprofil** des Firmenkontos anlegen (Bankverbindung, Steuerangaben).
2. *Produkte → In-App-Produkte* → Produkt **`plus_familie`**, einmaliger Kauf (kein Abo – passt besser zu Familien).
   Vorschlag: 4,99 € einmalig (Einführungspreis 2,99 €). Alternativ einzelne Pakete à 0,99–1,99 €.
3. Ich binde Google Play Billing in der Play-Variante an (Kauf nur hinter dem Eltern-Tor, „Kauf wiederherstellen“-Knopf)
   und schalte `Monetization.ENFORCED` ein. Alle Grundpakete bleiben frei; nur Pakete mit `tier: plus` (heute 6, später neue) brauchen Plus.
4. Fragebogen anpassen: **IARC** „digitale Käufe: ja“, **Datensicherheit** „Finanzinfos → Kaufverlauf“ (von Google verarbeitet,
   Zweck App-Funktionen). Play zeigt dann automatisch „Enthält In-App-Käufe“ im Store-Eintrag.
5. Familien-Richtlinie: Käufe in Kinder-Apps sind erlaubt, wenn Kinder nicht unter Druck gesetzt werden und
   ein Erwachsener bestätigt – beides ist eingebaut (Eltern-Tor, kein „Jetzt kaufen!“-Drängeln, Brummi bettelt nie).
