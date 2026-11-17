# Kurvenflug in Google Play – Klick-Liste für Marcel

Alles, was du brauchst, liegt im Repo unter `kurvenflug/store/` bzw. am GitHub-Release:

| Was | Wo |
|---|---|
| App-Paket (AAB, signiert) | GitHub → Releases → `Kurvenflug-<Version>-play.aab` |
| Titel, Kurz- und Langbeschreibung, Release-Text | `store/de-DE/*.txt` |
| 8 Handy-Screenshots (1080 × 2160) | `store/screenshots/` |
| Feature-Grafik 1024 × 500 | `store/grafik/feature_1024x500.png` |
| App-Symbol 512 × 512 | `store/grafik/icon_512.png` |
| Lizenzprüfung | `store/LIZENZEN.md` |

**Wichtigste Regel:** Beim Anlegen der App (Schritt 2) den **bestehenden Schlüssel hochladen** – nicht „von Google
erstellen lassen“. Nur so kann man zwischen GitHub-APK und Play-Version wechseln, ohne zu deinstallieren (Spielstand bleibt).
Diese Wahl lässt sich später nicht mehr ändern.

---

## 1. App anlegen
1. play.google.com/console → **Alle Apps** → **App erstellen**.
2. App-Name: `Kurvenflug – Mathe im All` · Standardsprache: **Deutsch – de-DE** · **App** (nicht Spiel)* · **Kostenlos**.
3. Erklärungen (Programmrichtlinien, US-Exportgesetze) anhaken → **App erstellen**.

\* Spiel geht auch; „App“ + Kategorie „Lernen“ passt besser zu einer Lern-App.

## 2. App-Signatur mit dem bestehenden Schlüssel (einmalig!)
1. Links **Testen und veröffentlichen → Einrichtung → App-Signatur** (bzw. beim ersten Release unter „App-Integrität“).
2. **„Anderen Schlüssel verwenden“ / „Signaturschlüssel ändern“** → **„Schlüssel aus Java-Schlüsselspeicher exportieren und hochladen“**.
3. **„Öffentlichen Verschlüsselungsschlüssel herunterladen“** → Datei `encryption_public_key.pem` mit einem Texteditor öffnen, den ganzen Inhalt kopieren.
4. GitHub → Repo **AndroidApps** → **Actions** → links **„Kurvenflug: Signaturschlüssel für Google Play“** → **Run workflow** →
   Inhalt einfügen → **Run workflow**. Nach ca. 1 Minute: Lauf öffnen → unten Artefakt **kurvenflug-play-signaturschluessel** herunterladen.
5. Die heruntergeladene Datei entpacken → darin liegt `kurvenflug-signaturschluessel.zip` → diese ZIP in der Play Console hochladen → **Speichern**.
6. Einen eigenen Upload-Schlüssel musst du **nicht** anlegen – derselbe Schlüssel signiert auch die AAB-Uploads.

> Klappt der Workflow nicht: Alternative am PC mit Java – Play Console zeigt bei Schritt 2 den Befehl `java -jar pepk.jar …`
> an; Schlüsseldatei und Passwörter stehen in Bitwarden.

## 3. Hauptseite des Store-Eintrags (Wachstum → Store-Präsenz → Hauptseite)
- **App-Name:** Inhalt von `store/de-DE/titel.txt`
- **Kurzbeschreibung:** `store/de-DE/kurzbeschreibung.txt`
- **Vollständige Beschreibung:** `store/de-DE/beschreibung.txt`
- **App-Symbol:** `store/grafik/icon_512.png` · **Feature-Grafik:** `store/grafik/feature_1024x500.png`
- **Screenshots Smartphone:** alle 8 aus `store/screenshots/` in dieser Reihenfolge
- Tablets: optional (kann leer bleiben)

## 4. App-Inhalte (Richtlinie → App-Inhalte) – die Antworten
**Datenschutzerklärung:** `https://www.mathenachhilfe-online.com/datenschutz/`
(Bitte dort einen Absatz zu Kurvenflug ergänzen, Vorschlag unten.)

**App-Zugriff:** „Alle Funktionen sind ohne besonderen Zugriff verfügbar“.

**Werbung:** „Nein, meine App enthält keine Werbung“.

**Einstufung des Inhalts (IARC-Fragebogen):** E-Mail angeben · Kategorie **„Alle anderen App-Typen“** (bzw. „Lernen“) ·
alle Fragen **Nein**: keine Gewalt, keine Angst-Inhalte, keine Sexualität, keine Schimpfwörter, keine Drogen,
kein Glücksspiel, **keine Kommunikation zwischen Nutzern** (Codes werden nur über das normale Teilen-Menü in andere Apps geschickt),
keine Standortfreigabe, keine digitalen Käufe. Erwartet: **USK ab 0 / PEGI 3**.

**Zielgruppe und Inhalte:** Altersgruppen **9–12**, **13–15**, **16–17** (Klasse 5–13; 18+ optional für Eltern/Lehrkräfte).
→ Weil Kinder unter 13 dabei sind, gilt das **Familienprogramm**: Fragen zu Werbung **Nein**, zu Daten **keine Erhebung**.
„Könnte die App ungewollt Kinder ansprechen?“ entfällt, weil sie absichtlich für Kinder ist.

**Nachrichten-Apps / Behördliche Apps / Finanz / Gesundheit / COVID:** jeweils **Nein**.

**Datensicherheit:**
1. „Erhebt oder teilt deine App erforderliche Arten von Nutzerdaten?“ → **Nein**.
   (Die Play-Version hat keine Internet-Berechtigung; Spielstand und Einstellungen bleiben auf dem Gerät.)
2. Damit sind die Detailfragen erledigt → **Speichern** → **Senden**.
   Hinweis: Das Teilen-Menü (Duell-Code, Foto der Flugbahn) startet der Nutzer selbst in einer anderen App – das zählt nicht als Erhebung.

**Vordergrunddienste, Exakte Wecker, Vollbild-Intents:** nicht verwendet (die Play-Version enthält diese Berechtigungen nicht).

**Kategorie und Kontakt (Store-Einstellungen):** Kategorie **Lernen** · Tags z. B. „Mathe“, „Physik“, „Lernspiel“ ·
Kontakt-E-Mail: deine Support-Adresse · Website: `https://www.mathenachhilfe-online.com/`.

## 5. Interner Test (sofort möglich)
1. **Testen und veröffentlichen → Testen → Interner Test → Tester** → E-Mail-Liste anlegen (du selbst + Familie).
2. **Neuen Release erstellen** → `Kurvenflug-<Version>-play.aab` vom GitHub-Release hochladen →
   Release-Name: Versionsnummer · Release-Hinweise: `store/de-DE/release-notes.txt` → **Weiter** → **Speichern und veröffentlichen**.
3. Unter „Tester“ den **Einladungslink** kopieren, am Handy öffnen → „Tester werden“ → App aus dem Play Store installieren.

## 6. Geschlossener Test (Pflicht für neue private Konten vor der Produktion)
1. **Testen → Geschlossener Test → Track „Alpha“ → Tester**: mindestens **12 Personen** (Google-Konten), die **14 Tage am Stück**
   als Tester eingetragen bleiben (z. B. Familie, Freunde, Nachhilfe-Schüler mit Einverständnis der Eltern).
2. Denselben AAB-Release in diesen Track übernehmen („Release aus Bibliothek hinzufügen“) → veröffentlichen.
3. Nach 14 Tagen erscheint im Dashboard **„Zugriff auf die Produktion beantragen“** → Fragen zum Test beantworten → Prüfung abwarten.

## 6a. Kurvenflug Pro (In-App-Produkt) – erst wenn Pro starten soll
Solange das Produkt nicht angelegt ist, bleibt in der App alles frei. Ab dem Anlegen erscheinen die Pro-Sperren.
1. **Monetarisieren → Produkte → In-App-Produkte** (ggf. erst Händlerkonto/Zahlungsprofil einrichten).
2. **Produkt erstellen**: Produkt-ID genau `kurvenflug_pro` · Name „Kurvenflug Pro“ · Beschreibung: „Oberstufe,
   Extrem-Prüfungen, Sturm Profi & Extrem, Andromeda ab Planet 7, bis 6 Spieler, Pro-Werkstatt. Einmalkauf, kein Abo.“
3. **Preis**: 7,99 € (Google zeigt Endpreise inkl. USt. und führt die USt. selbst ab) → für andere Länder umrechnen lassen → **Aktivieren**.
4. **Lizenztester** (Einstellungen → Lizenztests): eigene Google-Konten eintragen → Testkäufe kosten nichts.
5. **Pflicht vor der Freigabe:** Im internen Test einmal kaufen und „Käufe wiederherstellen“ probieren (die Play-Fassung
   hat keine Internet-Berechtigung – der Kauf läuft über die Play-Store-App; das muss einmal auf echtem Gerät klappen).
6. Formulare nachziehen: IARC-Fragebogen → „Digitale Käufe: **Ja**“. Datensicherheit bleibt „keine Daten erhoben“
   (den Kauf wickelt Google ab, die App speichert nur „Pro: ja“). Store-Eintrag zeigt „In-App-Käufe“ automatisch.

## 7. Updates später
Jede neue Version baut GitHub automatisch: `Kurvenflug-<Version>-play.aab` im Release → in der Play Console beim gewünschten
Track „Neuen Release erstellen“ → hochladen. In der App erscheint das Update dann von selbst (Play In-App Updates):
normal im Hintergrund, installiert beim nächsten Schließen; bei einer **wichtigen** Version (Priorität 4–5, setzbar über die
Play Developer API) sofort beim Start.

---

### Vorschlag für die Datenschutzerklärung (Absatz „Kurvenflug“)
> **App „Kurvenflug – Mathe im All“.** Die App erhebt, speichert und übermittelt keine personenbezogenen Daten. Der Spielstand,
> Einstellungen und frei gewählte Spitznamen für mehrere Spieler werden ausschließlich lokal auf dem Gerät gespeichert und beim
> Deinstallieren gelöscht. Die App enthält keine Werbung, keine Analyse- oder Tracking-Werkzeuge.
> Die Version aus Google Play hat keinen Internetzugriff; Updates werden über Google Play bereitgestellt. Die Version aus GitHub
> fragt nur auf GitHub nach neuen Versionen (dabei wird technisch bedingt die IP-Adresse an GitHub übertragen). Die Web-Version
> speichert den Spielstand nur im Browser (localStorage) und sendet keine Spieldaten an den Server.
> **Kurvenflug Pro (optional, Einmalkauf):** In der Play-Version wickelt Google Play den Kauf ab. In der Web- und GitHub-Version
> erfolgt der Kauf über unseren Shop auf mathenachhilfe-online.com (Bestelldaten nach unserer allgemeinen Datenschutzerklärung);
> die App speichert nur den Freischalt-Code bzw. „Pro: ja“ auf dem Gerät, ohne Bezug zu Namen oder E-Mail.
