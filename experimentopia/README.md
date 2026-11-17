# Experimentopia 🧪 – das Versuchslabor für die Hosentasche (Klasse 3–10)

Viele Versuche kann man zu Hause nicht machen: Säure und Lauge mischen, Knallgas zünden, einen Hammer auf dem Mond fallen lassen,
ein Klavier mit dem Flaschenzug heben. In Experimentopia geht das – auf acht Inseln, mit **Blubb**, einem neugierigen Laborkolben,
dessen Flüssigkeit mit seiner Laune die Farbe wechselt (Recherche: `../docs/RECHERCHE.md`).

**Version 2.1.1 vom 17.11.2026** · Android-App und WebApp (gleicher Code, gleiche Version) · 200 Versuche in 19 Fachbereichen (Physik, Chemie, Biologie) · 34 Geräte im Freien Labor · 19 Reaktionsgleichungen · 13 Abzeichen

## So forscht man in Experimentopia

Jeder Versuch folgt dem Forscher-Kreislauf (Predict – Observe – Explain):

1. **Rätsel** – Blubb erzählt, was er vorhat.
2. **Vermuten** – mit „Wie sicher bist du?“ (🤔 / 🙂 / 😎). Eine falsche Vermutung kostet **nie** etwas: Sie wird als 💡 Aha-Moment gefeiert.
3. **Ausprobieren** – im Labor selbst tun: eintauchen, Gewichte setzen, Seil ziehen, Kabel legen, Tropfen zugeben, Atome zählen, heizen, pusten, fallen lassen.
4. **Auflösung** – Vermutung und Ergebnis nebeneinander.
5. **Blubb erklären** – Blubb versteht es nicht und fragt „Warum?“. Lernen durch Lehren (Protégé-Effekt).
6. **Merksatz + Knobelfrage** – das Wissen auf eine neue Lage übertragen.

Sterne: ⭐ Versuch gemacht · ⭐ Blubb gleich richtig erklärt · ⭐ Knobelfrage gelöst. Das **Forscherbuch** sammelt Vermutungen, Ergebnisse und Merksätze
und zeigt, wie gut man sich selbst einschätzt („Wenn du dir ganz sicher warst, lagst du in 4 von 5 Fällen richtig“).

## Themen und Suche

19 Fachbereiche: Schwimmen, Dichte & Verdrängung · Hebel & Drehmoment · Rollen & schiefe Ebene · Kraft & Bewegung (Newton) · Fallen & Schwerkraft ·
Pendel & Schwingungen · Stromkreise · Magnetismus · Wärme & Teilchen · Licht & Farben · Schall & Töne (Physik) · Stoffe trennen · Säuren & Laugen · Chemische Reaktionen (Chemie) · Vererbung & Genetik · Zellen, Mikroskop & Osmose · Nährstoffe & Enzyme · Pflanzen & Ökosysteme · Körper & Gesundheit (Biologie).
Die Themensuche filtert nach Fach und Fachbereich und sucht Stichwörter in Titeln, Fragen, Merksätzen und Fachwörtern.

## Die Inseln

| Insel | Versuche (Auswahl) | Klasse |
|---|---|---|
| 🌊 Wasserinsel | Holz vs. Nagel, Riesenkerze, nackte Orange, Ei im Salzwasser, Knetboot, Eis in Spiritus, Dichteturm, Archimedes | 3–7 |
| ⚖️ Kraftwerk-Insel | Wippe, Schubkarre, Nussknacker, Flaschenzug, schiefe Ebene, Newton: Wagen und Skateboards (F = m·a, actio = reactio, Trägheit) | 3–10 |
| ⚡ Blitzinsel | Leitertest, Bleistiftmine, UND/ODER-Schalter, Reihe/Parallel, Kurzschluss, Ohmsches Gesetz, Magnete, Pole | 3–9 |
| 🌋 Chemie-Vulkan | Kaffeefilter, Sand-Salz-Wasser trennen, Chromatografie, Rotkohl-Zaubersaft, pH-Wert, Neutralisation, pH-Sprung, Verdünnen, Natron-Ballon, Knallgas, Magnesium, Rost, Fotosynthese … | 3–10 |
| 🔥 Feuer & Eis | Was schmilzt zuerst, Heizkurve mit Teilchenblick, Mischtemperatur, Badewasser, glühendes Eisen | 3–8 |
| 🌈 Licht & Klang | Malkasten, Licht mischen, Farbfilter, Prisma (echte Brechung), Gläser-Xylofon, Röhren-Orgel, Stimmgerät, „Alle meine Entchen“, Saite | 3–8 |
| 🚀 Fallturm-Insel | Kugel gegen Feder, Papier-Trick, Vakuumröhre, Fallzeit, Mond, Pendel, Sekundenpendel | 3–10 |
| 🌿 Bio-Dschungel | Mendels Erbsen, Wunderblume, Blutgruppen, Osmose, Plasmolyse, Nährstoff-Nachweise, Speichel-Enzym, Wasserpest, Hasen und Luchse, Mikroskop, Kresse keimen lassen, Nahrungsketten, Herz und Puls | 3–10 |

## Korrekt gerechnet

Alle richtigen Antworten werden aus Modellen in `core/model` **berechnet**, nicht hingeschrieben, und in `CatalogTest`/`PhysicsTest` geprüft:
Auftrieb (ρ-Vergleich, Eintauchtiefe), Hebelgesetz, Flaschenzug (Goldene Regel), Stromkreise (Reihe/Parallel, Kurzschluss),
pH aus [H⁺]−[OH⁻] mit Kw, Atomzählung aller Reaktionen, begrenzender Stoff beim Natron-Ballon, Richmann-Mischungsregel, Heizkurve mit Schmelz- und
Verdampfungswärme, Fall mit Luftwiderstand (numerisch) und ohne, exakte Pendeldauer (elliptisches Integral), Röhren f = c/(4L),
Prisma mit Cauchy-Brechzahl und Snellius, Kreuzungsquadrat und Aussaat (Mendel, AB0), Osmose, Enzymaktivität, Lichtstärke ∼ 1/d², Lotka-Volterra, Trennverfahren (kürzester Weg), subtraktive Farbmischung (`BiologyTest`), Sehfeld und Auflösung beim Mikroskop, Keimbedingungen, Nahrungsnetz (längste Kette, 10-%-Regel, Anreicherung), Herzzeitvolumen und Erholungspuls (`Biology2Test`). Ziele, die man einstellen muss (z. B. 0,2 A, pH 2, 880 Hz, T = 2 s), sind mit den Reglern erreichbar.

## Musik und Klänge (selbst komponiert)

Alles entsteht im Code (`shared/.../fx/Music.kt`, `Synth.kt`) – keine Samples, keine Lizenzen; Android und Web klingen gleich.
Seit 2.4.0 richtige, eingängige Lieder statt Zufallstönen: vier selbst komponierte Melodien (8 Takte, Frage und Antwort, klarer Schluss) mit Bass auf 1 und 3,
gezupften Begleit-Akkorden auf den Nachschlägen und leichtem Schlagzeug (Kick, Snare, Hi-Hat) – fröhlich und hüpfend, nicht einlullend.

| Welt | Lieder, Tonart, Tempo | Melodie-Instrument | Farbtupfer |
|---|---|---|---|
| Labor (Start, Menüs) | „Forscher-Marsch“ / „Treppauf“, C- oder F-Dur, 118 | E-Piano | Glas |
| Physik | „Treppauf“ / „Forscher-Marsch“, D-Dur, 124 | Glockenspiel + E-Piano | Glas-Funkeln |
| Chemie | „Geheimlabor“, a-Moll (dorisch), 108 | gezupft wie eine Mandoline | Blubbern |
| Biologie | „Sommerwiese“ / „Forscher-Marsch“, F-Dur, 112 | Marimba + Flöte | Vögel |

Jedes Stück wechselt mit dem nächsten ab (andere Melodie, andere Begleitung, manchmal ein anderes Melodie-Instrument oder eine zweite Stimme), dazwischen 2–5 Sekunden Stille.
Im Versuch selbst ist es still. Android startet die Musik gleich beim Öffnen und richtet sich nach der Medien-Lautstärke; der Browser erst nach dem ersten Antippen.
Beim Vorlesen wird die Musik leiser.

## Vorlesen

`Speech.kt` macht Texte sprechbar (chemische Formeln, Einheiten, Rechenzeichen, Kreuzungen, Abkürzungen; Test `SpeechTest`).
Stimme: **„Nova – KI-generierte Stimme“** (OpenAI), vertont auf dem gemeinsamen Stimmen-Server (stimmen.mathenachhilfe-online.com) – die App spricht nie mit OpenAI.
`Nova.kt` teilt jeden Text in Sätze (≤ 300 Zeichen) und berechnet den Dateinamen (SHA-256 von „nova|de|Text“, 32 Hex-Zeichen, Test `NovaTest` mit dem Prüfwert des Servers).
Beim Bau der WebApp entsteht `webapp/vorlesen/texte.json` mit allen festen Sätzen (Gradle-Aufgabe `novaTexte`); der Server holt die Liste alle 30 Minuten und vertont Neues vorab.
Abspielen: erst vom Gerät/Browser-Speicher, dann `/nova/<h[0:2]>/<h>.mp3`, bei neuen Sätzen `POST /jetzt` (nur der Text); ohne Internet liest leise die Stimme des Geräts bzw. Browsers.
Android: `AndroidReader.kt` (MediaPlayer, Dateien in `files/nova/`), Web: `nova.js` (WebAudio, Speicher „experimentopia-nova“).

## Lernen nach Fach (am Schulstoff entlang)

Auf dem Startbildschirm: Physik, Chemie oder Biologie wählen. `core/model/Curriculum.kt` ordnet die Fachbereiche nach der Klassenstufe,
in der sie beginnen, und filtert nach der eingestellten Klasse. Je Bereich: Lernziel (Grundidee), Fortschritt, „▶️ Weiter“ (nächster passender Versuch),
Theorie und alle Versuche zum Aufklappen.

## Theorie zum Nachlesen

Jeder Versuch hat einen Knopf „📖 Theorie“ (oben, bei der Vermutung und im Labor). Er öffnet eine Theorie-Seite zum Fachbereich
(`core/model/Theory.kt`, 19 Karten): Grundidee mit Blubb, Punkte mit Bildern, Alltagsbeispiel, Fachwörter und „Für Profis (ab Klasse 7)“ mit Formeln.
Die Seiten erklären die Idee, verraten aber keine Versuchslösung (Alltagsbeispiele sind bewusst andere als in den Versuchen).

## Sicherheit

In der App ist jeder Versuch gefahrlos. Wo Kinder etwas in echt nachmachen könnten (Strom, Feuer, Säuren, heißes Wasser, Teichwasser …),
zeigt der Versuch einen Sicherheitshinweis (`core/model/Safety.kt`), auch im Forscherbuch.

## Frei und Pro (Forscher-Pass)

Frei: die ganze Wasserinsel, von jeder anderen Insel die ersten 5 Versuche, in jedem Thema die ersten 2, Rätsel des Tages, Forscherbuch, Themensuche, Lernen nach Fach, Theorie, 6 Geräte im Freien Labor.
Der **Forscher-Pass** (7,99 € einmalig inkl. 19 % USt., kein Abo) öffnet alles. Kauf auf der Website (Woo-Kasse) nur nach einer Frage für Erwachsene;
freigeschaltet wird mit einem signierten Code (Ed25519, Präfix „EX“, App 6, Produkt 1), den die App ohne Server prüft (`License.kt`, `Ed25519.kt`, Tests).
Codes erzeugt der Lizenz-Dienst (`pro/pro_code.py`). Schlüssel und Kasse stehen in `pro/pro.properties` (seit 2.1.0 eingetragen: Produkt 4143; ohne Kasse würde nichts gesperrt).
Bestandsschutz: Schon gemachte Versuche bleiben immer offen. Die Play-Fassung hat (noch) keinen Kauf und ist ganz frei.

## Bildschirme

| Start mit Blubb | Inselpfad | Rätsel | Vermuten + Sicherheit |
|---|---|---|---|
| ![](screenshots/01_start.png) | ![](screenshots/02_insel.png) | ![](screenshots/03_raetsel.png) | ![](screenshots/04_vermuten.png) |
| Ausprobieren | Aha-Moment | Blubb erklären | Geschafft |
| ![](screenshots/05_ei_im_salzwasser.png) | ![](screenshots/06_aha_moment.png) | ![](screenshots/07_blubb_erklaeren.png) | ![](screenshots/08_geschafft.png) |
| Dichteturm | Wippe | Flaschenzug | Lampen in Reihe |
| ![](screenshots/10_dichteturm.png) | ![](screenshots/11_wippe.png) | ![](screenshots/12_flaschenzug.png) | ![](screenshots/13_lampen_in_reihe.png) |
| Leitertest | Ohmsches Gesetz | Magnet | Rotkohl-Saft |
| ![](screenshots/14_leitertest.png) | ![](screenshots/15_ohmsches_gesetz.png) | ![](screenshots/16_magnet.png) | ![](screenshots/17_rotkohl.png) |
| pH-Sprung | Atom-Baukasten | Natron-Ballon | Heizplatte |
| ![](screenshots/18_ph_sprung.png) | ![](screenshots/19_knallgas.png) | ![](screenshots/20_natron_ballon.png) | ![](screenshots/21_heizplatte.png) |
| Teilchenblick | Mischtemperatur | Licht mischen | Prisma |
| ![](screenshots/22_teilchen.png) | ![](screenshots/23_eisen_ins_wasser.png) | ![](screenshots/24_licht_mischen.png) | ![](screenshots/25_prisma.png) |
| Lied spielen | Stimmgerät | Fallturm | Pendel |
| ![](screenshots/26_lied_spielen.png) | ![](screenshots/27_stimmen.png) | ![](screenshots/28_kugel_gegen_feder.png) | ![](screenshots/29_pendel.png) |
| Freies Labor | Eigene Werte | Freier Stromkreis | Forscherbuch |
| ![](screenshots/30_freies_labor.png) | ![](screenshots/31_eigene_werte.png) | ![](screenshots/32_freier_stromkreis.png) | ![](screenshots/33_forscherbuch.png) |
| Selbsteinschätzung | Abzeichen | Einstellungen | Forscher-Pass (vorbereitet) |
| ![](screenshots/34_selbsteinschaetzung.png) | ![](screenshots/35_abzeichen.png) | ![](screenshots/36_einstellungen.png) | ![](screenshots/37_forscher_pass.png) |
| Forscherbuch mit Erklärung | Themensuche | Thema Newton | Newton-Wagen |
| ![](screenshots/38_forscherbuch_erklaerung.png) | ![](screenshots/39_themensuche.png) | ![](screenshots/40_thema_newton.png) | ![](screenshots/41_newton_wagen.png) |
| Skateboards | Eisen auf Quecksilber | Mendels Erbsen | Blutgruppen |
| ![](screenshots/42_skateboards.png) | ![](screenshots/43_quecksilber.png) | ![](screenshots/44_mendel_erbsen.png) | ![](screenshots/45_blutgruppen.png) |
| Osmose an der Zwiebel | Stärke-Nachweis | Enzym im Kühlschrank | Wasserpest |
| ![](screenshots/46_osmose_zwiebel.png) | ![](screenshots/47_staerke_nachweis.png) | ![](screenshots/48_enzym.png) | ![](screenshots/49_wasserpest.png) |
| Hasen und Luchse | Stoffe trennen | Chromatografie | Malkasten |
| ![](screenshots/50_hasen_und_luchse.png) | ![](screenshots/51_stoffe_trennen.png) | ![](screenshots/52_chromatografie.png) | ![](screenshots/53_malkasten.png) |
| Mikroskop | Augentierchen | Kresse im Dunkeln | Nahrungskette |
| ![](screenshots/54_mikroskop.png) | ![](screenshots/55_augentierchen.png) | ![](screenshots/56_kresse_im_dunkeln.png) | ![](screenshots/57_nahrungskette.png) |
| Ohne Mäuse | Herz und Puls | Erholungspuls | Theorie |
| ![](screenshots/58_ohne_maeuse.png) | ![](screenshots/59_herz_und_puls.png) | ![](screenshots/60_erholungspuls.png) | ![](screenshots/61_theorie.png) |
| Theorie für Profis | Lernen nach Fach | Biologie, Klasse 9 | Musik und Vorlesen |
| ![](screenshots/62_theorie_profi.png) | ![](screenshots/63_lernen_nach_fach.png) | ![](screenshots/64_fach_biologie.png) | ![](screenshots/65_musik_und_vorlesen.png) |
| Forscher-Pass | Insel mit Pass | Pass aktiv | |
| ![](screenshots/66_forscher_pass_kaufen.png) | ![](screenshots/67_insel_mit_pass.png) | ![](screenshots/68_forscher_pass_aktiv.png) | ![](screenshots/69_raetsel_nochmal.png) |

## Android und Web

Wie Raumlicht: eine gemeinsame Codebasis (Compose Multiplatform).
- `core/model` – Physik- und Chemie-Modelle, Versuche, Regeln (für Android und Web)
- `shared` – alle Bildschirme und Labore; Plattform-Teile stecken hinter Schnittstellen (Ton, Vibration, Speicher)
- `app` – Android: Room-Speicher, Ton über SoundPool, Updates (GitHub-APK: eigener Updater; Play-Fassung: Play In-App Updates)
- `web` – WebApp (Kotlin/Wasm): Speicher im Browser, Ton über WebAudio, Ladebild mit echtem Balken, Service Worker
- `webapp/` – fertig gebaute WebApp. Der Workflow `experimentopia-web-test.yml` prüft sie und spult den Branch `experimentopia-test` vor;
  der Server zeigt sie unter https://experimentopia-test.mathenachhilfe-online.com (live: https://experimentopia.mathenachhilfe-online.com, setzt der Leitstand).

**Automatische Updates:** WebApp über den Service Worker (lädt neue Versionen im Hintergrund, wechselt auf der Startseite oder im Hintergrund).
GitHub-APK: beim Start höchstens alle 20 Stunden, Banner + Knopf in den Einstellungen (abschaltbar). Play-Fassung: über den Play Store.

## Bauen

```
./gradlew :core:model:jvmTest :core:data:testDebugUnitTest   # Modelle und Speicher
./gradlew :web:webapp                                        # WebApp nach webapp/ (braucht python3 + fonttools für die Schriften)
./gradlew :app:testGithubDebugUnitTest --tests '*ScreenshotTest*'   # Bildschirmfotos nach screenshots/
./gradlew assembleGithubRelease assemblePlayRelease          # APK mit eigenem Updater / Play-Fassung
```

Releases baut `.github/workflows/experimentopia-release.yml` (Tag `experimentopia-v<Version>`), Text aus `CHANGELOG.md`.

Datenschutz: kein Konto, keine Werbung, keine Käufe, keine Kamera, kein Mikrofon. Gespeichert wird nur auf dem Gerät. Ins Internet nur für die Update-Prüfung (abschaltbar).
