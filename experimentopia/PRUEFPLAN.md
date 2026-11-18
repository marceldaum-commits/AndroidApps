# Experimentopia – Prüfplan (Dauerregel von Marcel, ab 18.11.2026)

**Ziel:** Jede Woche neuer Inhalt, einmal im Monat gründlich prüfen und reparieren. Wir prüfen **rollierend**: Jeden Monat ist ein Block der 8 Inseln dran.
So muss nie alles auf einmal neu geprüft werden, und jede Insel kommt alle 4 Monate gründlich dran.

## 1. Wöchentlich: neuer Inhalt

- Mindestens **ein neuer Versuch oder eine neue Erklärung** pro Woche. Am liebsten kommt er aus `docs/LEHRPLAN-ABGLEICH.md` (Spalte „geplant“ oder eine „Kommt bald“-Kachel).
- **Mittwochs** auf `experimentopia-test`, mit kurzer Meldung in `MELDUNG_AN_LEITSTAND.md`. Live schaltet der Leitstand.
- Pflicht für jeden neuen Inhalt (schon in den Tests verankert):
  - Die Antwort wird aus dem Modell berechnet (`CatalogTest`, `OpticsTest` …).
  - Das Ziel ist mit den Reglern erreichbar.
  - Drei Begründungen, Merksatz und Knobelfrage sind da.
  - Die Theorie-Karte hat Formel, Größen, Einheiten und Rechenbeispiel.
  - Es gibt ein Bildschirmfoto in `screenshots/` (vor der Auslieferung ansehen).
  - Version und Datum stehen in `app/build.gradle.kts`, `CHANGELOG.md`, in der App und in der Meldung.
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
