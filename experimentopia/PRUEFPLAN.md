# Experimentopia – Prüfplan und Update-Rhythmus (Dauerregeln von Marcel, 18.11.2026)

**Ziel:** Dienstags und freitags ein großes Inhaltsupdate, einmal im Monat gründlich prüfen und reparieren. Wir prüfen **rollierend**: Jeden Monat ist ein Block der 8 Inseln dran.
So muss nie alles auf einmal neu geprüft werden, und jede Insel kommt alle 4 Monate gründlich dran.

## 1. Zweimal pro Woche: großes Inhaltsupdate (Dauerregel Marcel, 18.11.2026 – ersetzt „wöchentlich mittwochs“)

- **Dienstag und Freitag bis 16 Uhr** ein **großes** Inhaltsupdate auf `experimentopia-test` + Meldung in `MELDUNG_AN_LEITSTAND.md`. Der Leitstand prüft am selben Tag und gibt frei.
  Erstes Update: **Freitag, 20.11.2026**.
- Was hinein kommt, entscheidet die größte Wirkung (Reihenfolge aus `docs/LEHRPLAN-ABGLEICH.md`, „Kommt bald“-Kacheln):
  neue Versuche und Themen · bestehende Themen ausweiten · bestehende Aufgaben korrigieren und vervollständigen · neue Optionen in der App.
- **Pflicht (Marcel wörtlich): „mathematisch korrekt, vollständig, nachvollziehbar. Keine Abkürzungen, weil die Schüler damit lernen.“** Für Physik, Chemie und Biologie heißt das:
  - fachlich korrekt, immer mit Einheiten;
  - Formeln in der Theorie mit Bedeutung der Größen und einem Beispiel;
  - Rechenwege mit allen Schritten (in Auflösung, Begründung und Theorie);
  - jedes Ergebnis nachgerechnet – Antworten werden aus dem Modell berechnet und in Tests gegen Buchwerte geprüft.
- Weiter Pflicht wie bisher: Ziel mit den Reglern erreichbar, drei Begründungen, Merksatz, Knobelfrage, Bildschirmfoto angesehen, Version + Datum überall.
- **Meldung enthält:** was neu ist · wie viele Versuche/Aufgaben (neu, geändert, gesamt) · wie geprüft (Tests, Bildschirmfotos, Starttest).
- **Fragen an Marcel** (Regel 21.11.: „fragen nicht immer doppelt stellen.. nur über dich!“): Entscheidungsfragen nur über die AppZentrale (send_message mit Hintergrund, Optionen, Empfehlung), nie selbst per Auswahlfenster.
- **Rechtsbezug** (Ablauf AppZentrale ab 19.11.2026): Jede Live-Meldung enthält die Zeile „Rechtsbezug: ja/nein (was)“ – Preise/Kauf, Codes, Texte für Eltern und Kinder, Datenschutz, Werbung, Bewertungen, KI, Mikrofon. Mit Rechtsbezug prüft der Raum „Recht“ vor dem Live-Gang.
- Ist eine „Kommt bald“-Kachel fertig gebaut, wird sie aus `Coming` in `core/model/.../Upcoming.kt` gestrichen und in der Lehrplan-Liste auf ✅ gesetzt.

## 2. Monatlich (letzte Woche des Monats): Prüfung

### a) Rollierender Insel-Block (gründlich)

| Block | Inseln | Monate |
|---|---|---|
| A | 🌊 Wasserinsel · ⚖️ Kraftwerk-Insel | Dez · Apr · Aug |
| B | ⚡ Blitzinsel · 🔥 Feuer & Eis | Jan · Mai · Sep |
| C | 🌋 Chemie-Vulkan · 🌿 Bio-Dschungel | Feb · Jun · Okt |
| D | 🌈 Licht & Klang · 🚀 Fallturm-Insel | Mär · Jul · Nov |

**Erster Lauf: Dezember 2026, Block A.** Block D wurde im November mit der Optik (2.7.0) schon neu geprüft.

Je Versuch im Block:
- [ ] **Fachlich richtig**: Physik, Chemie bzw. Biologie stimmen; Zahlen sind mit dem Modell nachgerechnet.
- [ ] **Eindeutig**: genau eine richtige Antwort, keine Fangfrage, Ablenker klar falsch (`EindeutigTest`).
- [ ] **Erklärung**: Formel mit Bedeutung der Größen und Einheiten, Beispiel durchgerechnet.
- [ ] **Theorie-Karte** des Themas passt dazu (Grundlagen Kl. 3–6, „Für Profis“ ab Kl. 7).
- [ ] **Klassenstufe** stimmt (Lehrplan-Abgleich).
- [ ] **Labor**: Ziel erreichbar, eigene Werte möglich, verständliche Meldung bei unpassender Eingabe.
- [ ] Bildschirmfotos des Blocks angesehen (Text passt, nichts abgeschnitten).

### b) Neu-Content des Monats
- [ ] Alle in diesem Monat neu gebauten Versuche noch einmal nach der Liste oben durchgehen.

### c) Rundum-Prüfung (jeden Monat, ganz)
- [ ] **Tests grün**: `./gradlew :core:model:jvmTest :app:testGithubDebugUnitTest :app:lintGithubDebug :web:webapp`
- [ ] **Start**: `node tools/web-start.mjs` mit Chromium und WebKit (iPhone/iPad-Ersatz), `node tools/web-update.mjs`.
- [ ] **Echtes iPhone/iPad** (Marcel): `…/?diag=1` zeigt „fertig“, keine weiße Seite.
- [ ] **Zählung**: Zahl der Versuche, Themen und Geräte stimmt in App (Einstellungen, Forscher-Pass-Text), README, CHANGELOG und Website. Gratis-Umfang stimmt (`OpticsTest.kommtBald`).
- [ ] **Links**: Impressum, Datenschutz, Update-Prüfung (`version.json`), Eltern-Bericht-Link.
- [ ] **Eltern-Kopplung**: Rundlauf koppeln → Bericht senden → auf der Eltern-Seite lesbar → Kopplung aufheben.
- [ ] **Offline**: Nach dem ersten Laden startet die App ohne Netz.

### Ergebnis
- Fehler sofort beheben, als Version mit Datum auf Test legen und in `MELDUNG_AN_LEITSTAND.md` melden („Monatsprüfung Block X“).
- Hier unten eintragen, was geprüft wurde:

| Monat | Block | Ergebnis | Version |
|---|---|---|---|
| Nov 2026 | D (Optik neu) + Theorie aller Themen | 7 Stellen korrigiert, 70 Formel-/Beispielpunkte ergänzt | 2.6.2 / 2.7.0 |
