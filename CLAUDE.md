# Leitlinien für alle Apps in diesem Repo

Dieses Repo enthält ausschließlich **Android-Apps** (Kotlin, Jetpack Compose, Material 3). Oberfläche auf Deutsch.

## Oberstes Ziel: Kinder und Schüler begeistern

Unsere Zielgruppe sind Kinder, Schüler und ihre Eltern. Eine App muss **fesseln, zum Mitmachen bewegen und im Gedächtnis
bleiben**. Statische Karteikarten- oder Formular-Oberflächen reichen nicht. Das gilt für jede neue App, jede neue Funktion
und **auch für jedes Zusatz- oder Bezahl-Angebot** (z. B. Plus-Wissenspakete) – Zusätze dürfen nie „nackter“ sein als der Rest.

**Pflicht in jeder App / Funktion**
- **Bewegung**: Animationen für Feedback, Übergänge und Belohnungen (Federn, Ploppen, Partikel, Konfetti).
- **Interaktion statt Lesen**: selbst tun – antippen, wischen, ziehen, zusammenbauen, zuordnen. Mehrere Spielarten mischen,
  damit es nie eintönig wird.
- **Bilder**: Inhalte bekommen ein Merkbild (Bild + Wort merkt man sich besser). Bei Wissenspaketen ist `img` Pflicht.
- **Charakter**: eine Figur, die mitfiebert (im Wissensgarten: Brummi die Hummel) – lobt, tröstet, gibt Tipps.
- **Ton und Haptik**: kurze, freundliche Sounds (im Code erzeugt, keine Lizenzprobleme) und Vibration – abschaltbar.
- **Belohnung ohne Druck**: Fortschritt sichtbar machen, Serien und Boni feiern, aber keine Strafen, keine Angst vor
  verlorenen Serien.
- **Kurze Einheiten**: eine Runde in wenigen Minuten schaffbar.
- **Kinderfreundlich & datensparsam**: keine Werbung, keine unnötigen Daten, Eltern verstehen sofort, was passiert.

**Recherche**: Vor größeren Neuerungen kurz recherchieren, was Kinder aktuell begeistert (Lernspiele, Spiele-Trends,
Lernforschung) und die Erkenntnisse in den Entwurf einfließen lassen.

## Arbeitsweise

- Jede App in eigenem Unterordner, eigenes README mit Bildschirmfotos (Robolectric/Roborazzi, `screenshots/`).
- Bildschirmfotos nach Änderungen ansehen und prüfen, bevor ausgeliefert wird.
- Fertige APKs in den gemeinsamen Google-Drive-Ordner – **jede App in ihren eigenen Unterordner**.
- Datenbank-Änderungen nur mit Migration + Migrationstest; Lernfortschritt darf nie verloren gehen.

## Neue App aufsetzen (Pflicht-Ausstattung, Vorbild: `wissensgarten/`)

- **Ordner**: `<appname>/` im Repo-Stamm, eigenes Gradle-Projekt, Paketname `de.<appname>.app`, minSdk 26.
  Modular wie der Wissensgarten (`core/model` reine Logik + Tests, `core/data` Room, `app` Compose).
- **Git**: nur auf den Entwicklungs-Branch der Sitzung pushen, keine Pull Requests ohne Auftrag. Mathe-Trainer nicht anfassen.
- **Automatische Releases**: eigener Workflow `.github/workflows/<appname>-release.yml` (Kopie des
  Wissensgarten-Workflows, Pfade/Tag anpassen). Tag `<appname>-v<versionName>`, Release-Text aus `<appname>/CHANGELOG.md`.
  Signieren mit denselben Secrets `WG_KEYSTORE_BASE64` / `WG_KEYSTORE_PROPERTIES` (gleicher Schlüssel für alle Apps),
  Keystore und `keystore.properties` nie committen.
- **Ausliefern** = versionCode/versionName erhöhen + CHANGELOG-Abschnitt (kindgerecht, für Eltern verständlich) + pushen.
- **Immer das Datum zur Version**: in der CHANGELOG-Überschrift (`## 0.3.0 – Titel (15.11.2026)`), im Release-Namen und in der App
  (Einstellungen: „Version 0.3.0 vom 15.11.2026“, Vorbild `raumlicht/`: `VERSION_DATE` in `app/build.gradle.kts`). Auch in Antworten an den Nutzer Version und Datum nennen.
- **Update in der App**: Updater wie im Wissensgarten (GitHub-Releases prüfen, beim Start höchstens alle 20 h,
  Banner + Knopf in den Einstellungen, abschaltbar, `SELF_UPDATE`-Schalter für eine spätere Play-Store-Version).
- **Google Drive**: APK zusätzlich in den gemeinsamen Drive-Ordner, Unterordner je App (sobald `GDRIVE_SA_JSON` eingerichtet ist).
- **Qualität**: Unit-Tests für Logik, Robolectric/Roborazzi-Bildschirmfotos in `screenshots/` (vor Auslieferung ansehen),
  Lint ohne Fehler, Sounds im Code erzeugt, Musik/Ton/Vibration abschaltbar, keine Werbung, keine unnötigen Berechtigungen.
- **Recherche** vorher in `docs/RECHERCHE.md` ergänzen.
