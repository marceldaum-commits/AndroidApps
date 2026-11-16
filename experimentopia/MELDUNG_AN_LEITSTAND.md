# Meldungen an den Leitstand – Experimentopia

Neueste oben.

## 16.11.2026 – Vorstellung und erster Web-Stand: Version 0.2.0 vom 16.11.2026

**Was liegt wo**
- Ordner `experimentopia/` in marceldaum-commits/AndroidApps. Web-Root: **`experimentopia/webapp/`** (index.html im Wurzelverzeichnis, nur statische Dateien, 15 MB, alle Dateiarten erlaubt).
- Entwicklungs-Branch `ccr-2ccbaa44-nkkmyu`; der Workflow `experimentopia-web-test.yml` prüft die WebApp und spult **`experimentopia-test`** vor. `experimentopia-live` fasse ich nicht an.
- Version 0.2.0 vom 16.11.2026: Android und Web gleich (eine Quelle: `app/build.gradle.kts`, der Workflow bricht ab, wenn `webapp/version.json` abweicht).

### 1. Was ist Experimentopia?
Ein virtuelles Versuchslabor für **Klasse 3 bis 10**, Fächer **Physik und Chemie** (Sachunterricht in Klasse 3/4).
Versuche, die zu Hause nicht gehen (Säure und Lauge, Knallgas, Fallen auf dem Mond, Flaschenzug mit Klavier), werden in der App ausprobiert.
7 Inseln, 67 Versuche, 20 Geräte im „Freien Labor“ mit eigenen Werten. Maskottchen: Blubb, ein Laborkolben.
Jeder Versuch folgt dem Forscher-Kreislauf: Vermuten (mit Selbsteinschätzung) → Ausprobieren → Auflösung → Blubb erklären (Lernen durch Lehren) → Merksatz → Knobelfrage.
Falsche Vermutungen kosten nichts, sie werden als „Aha-Moment“ gefeiert. Forscherbuch mit Auswertung der Selbsteinschätzung.

### 2. Technik und Zeitplan
- Kotlin, Compose Multiplatform: gemeinsame Oberfläche (`shared/`) für Android und Web (Kotlin/Wasm, `web/`), Rechenlogik in `core/model` (wie Raumlicht).
- **Android:** GitHub-APK mit eigenem Updater (beim Start höchstens alle 20 h, abschaltbar); Play-Fassung (`play`-Flavor) mit Play In-App Updates API, ohne Selbst-Installieren.
- **Web:** Ladebild inline in HTML/CSS ab dem ersten Byte, echter Ladebalken, Service Worker (offline, Update automatisch an ruhiger Stelle = Startseite oder Hintergrund).
  Getestet in Chromium: Start lokal nach 1,3 s, 320 px Breite ohne waagrechtes Scrollen, keine Konsolenfehler.
- Ton im Code erzeugt (Android: SoundPool/AudioTrack, Web: WebAudio mit denselben Klängen), keine Audiodateien.
- Zeitplan: 0.2.0 jetzt zum Test. Danach nach eurer Prüfung Feinschliff; Pro erst nach Freigabe (siehe 4).

### 3. Standards
1. Web = Android: erfüllt (eine Codebasis, gleiche Version).
2. Start ohne weißen Bildschirm: erfüllt (Ladebild, Ladebalken; Schriften auf benutzte Zeichen gekürzt: 116 KB + 359 KB).
3. Schwierigkeit: Es gibt **keine Schwierigkeitsstufen**, nur einen Klassenfilter (3–10), der Versuche hervorhebt. Ein Stufen-Test entfällt darum.
4. Fachliche Prüfung: Alle richtigen Antworten werden aus Physik- und Chemie-Modellen **berechnet** und in Tests gegen Lehrbuchwerte geprüft
   (z. B. pH-Sprung 11,68 bei 11 mL, Prisma-Mindestablenkung 38,6°, Pendel mit elliptischem Integral, Atombilanz aller 12 Reaktionen, Richmann, Fallzeiten mit und ohne Luft).
   Bitte trotzdem stichprobenartig gegenprüfen lassen: `core/model/src/jvmTest/.../CatalogTest.kt` listet jede erwartete Antwort.
5. Datenschutz: kein Konto, keine Werbung, keine Analyse, Daten nur im Browser bzw. auf dem Gerät. Text unten.
6. Barrierefreiheit: 320 px geprüft; Kontraste hell auf dunkel. Offener Punkt: Bedienung per Tastatur in den Laboren (Zeichenflächen) noch nicht vollständig.
7. Fremde Inhalte: alle Texte, Versuche und Zahlen selbst geschrieben. Bibliotheken: Kotlin, Compose Multiplatform, AndroidX, kotlinx (Apache 2.0); Schriften Noto Color Emoji und DejaVu Sans (OFL bzw. frei). Keine GPL.

### 4. Pro-Konzept (bitte Marcels Freigabe einholen – gebaut ist nur der Schalter, ausgeschaltet)
**Kostenlos bleibt der Kern:** die ganze Wasserinsel, von jeder anderen Insel die ersten 3 Versuche (27 von 67), das Rätsel des Tages (jeden Tag ein anderer der 67 Versuche),
Forscherbuch, Abzeichen und 6 Geräte im Freien Labor (Wasserbecken, Wippe, Stromkreis, Rotkohl, Licht, Fallturm).

**Pro („Forscher-Pass“):** alle 67 Versuche (die anspruchsvolleren für Klasse 7–10: Ohmsches Gesetz, pH-Sprung, Reaktionsgleichungen, Pendel, Mond), alle 20 Geräte mit eigenen Werten,
Atom-Baukasten mit allen 12 Reaktionen. Einmalkauf, kein Abo, nur hinter Eltern-Abfrage.

**Preisvorschlag:** **7,99 € einmalig** (Einführung 4,99 €).
| | brutto | netto | USt 19 % |
|---|---|---|---|
| Forscher-Pass | 7,99 € | 6,71 € | 1,28 € |
| Einführungspreis | 4,99 € | 4,19 € | 0,80 € |

Vergleich (Drittquellen, Stand und Länder unterschiedlich, bitte vor Veröffentlichung prüfen): Toca Lab: Elements 4,49 € einmalig (ein Thema, ohne Lernkontrolle);
ANTON-Plus 9,99 €/Jahr (alle Fächer, Abo); sofatutor rund 32 €/Monat (Abo). Experimentopia liegt als Spezialist (Physik/Chemie, Klasse 3–10) unter einem ANTON-Jahr, aber dauerhaft.

**Technik nach Freigabe:** Freischalt-Code wie vorgegeben (Version 1, App-Byte 6, Präfix „EX-“, Produkt 1 = Pro, Ed25519, euer Base32-Alphabet), Prüfung offline mit dem öffentlichen Schlüssel,
Freischalt-Link `?pro=<Code>`, Vorbild `kurvenflug/pro/`. Sperren erst, wenn ihr eine Kassen-URL eintragt. Play-Fassung vorerst ohne Kauf-Knopf. Kein KI-Tutor geplant.

### 5. Konzept Lehrerzugang (nicht gebaut)
- **Lehrkraft sieht:** je Schüler (Spitzname) erledigte Versuche, Sterne, Aha-Momente, ob die Erklärung beim ersten Mal stimmte, und die Selbsteinschätzung (wie oft „ganz sicher“ richtig war).
  Je Versuch: wie viele Kinder richtig vermutet haben – gut für den Einstieg in die nächste Stunde („Die Hälfte dachte, das Ei sinkt …“).
- **Lehrkraft steuert:** Aufträge = Versuch(e) bis Datum, optional nur bestimmte Inseln oder Klassenstufe; Rätsel der Woche für die Klasse.
- **Daten der App (so knapp wie möglich):** Klassen-Code, Spitzname, Versuch-ID, Sterne (0–3), Vermutung richtig ja/nein, Sicherheit (0–2), Erklärung beim ersten Mal ja/nein, Datum. Keine Texte, keine Geräte-Kennung, kein Name.
- **Beitritt:** In den Einstellungen „Klasse beitreten“ → Code eintippen oder QR scannen (Web: nur eintippen) → Spitznamen wählen. Austreten jederzeit, dann löscht der Server die Daten.

### 6. Für die App-Karte auf der Website
- **Name:** Experimentopia
- **Symbol:** 🧪
- **Zielgruppe:** Klasse 3–10 · Physik & Chemie
- **Beschreibung:** Das Versuchslabor für die Hosentasche: vermuten, ausprobieren und dem Laborkolben Blubb erklären, warum das Ei im Salzwasser schwimmt.

### 7. Abschnitt für die Datenschutzerklärung
> **Experimentopia (WebApp und Android-App):** Experimentopia speichert den Spielstand, das Forscherbuch und die Einstellungen ausschließlich lokal im Browser (localStorage) bzw. auf dem Gerät.
> Es gibt keine Anmeldung, keine Werbung und keine Analyse- oder Tracking-Dienste; es werden keine personenbezogenen Daten an uns oder Dritte übertragen.
> Die WebApp nutzt einen Service Worker, um die App für die Offline-Nutzung zwischenzuspeichern und automatisch zu aktualisieren. Die Android-App (Download-Fassung) fragt
> höchstens alle 20 Stunden bei GitHub nach einer neuen Version (abschaltbar); dabei werden außer der normalen technischen Anfrage keine Daten gesendet.
> Töne werden im Gerät erzeugt; Kamera und Mikrofon werden nicht verwendet.

**Bitte prüfen:** Start auf iPhone/iPad (Safari ab 18.2), Ladezeit über den Server mit gzip, Offline nach dem ersten Besuch, automatisches Update beim nächsten Push.
