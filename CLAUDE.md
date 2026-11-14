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
