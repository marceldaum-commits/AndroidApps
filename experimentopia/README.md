# Experimentopia 🧪 – das Versuchslabor für die Hosentasche (Klasse 3–10)

Viele Versuche kann man zu Hause nicht machen: Säure und Lauge mischen, Knallgas zünden, einen Hammer auf dem Mond fallen lassen,
ein Klavier mit dem Flaschenzug heben. In Experimentopia geht das – auf acht Inseln, mit **Blubb**, einem neugierigen Laborkolben,
dessen Flüssigkeit mit seiner Laune die Farbe wechselt (Recherche: `../docs/RECHERCHE.md`).

**Version 0.4.0 vom 17.11.2026** · Android-App und WebApp (gleicher Code, gleiche Version) · 180 Versuche in 18 Fachbereichen (Physik, Chemie, Biologie) · 30 Geräte im Freien Labor · 19 Reaktionsgleichungen · 13 Abzeichen

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

18 Fachbereiche: Schwimmen, Dichte & Verdrängung · Hebel & Drehmoment · Rollen & schiefe Ebene · Kraft & Bewegung (Newton) · Fallen & Schwerkraft ·
Pendel & Schwingungen · Stromkreise · Magnetismus · Wärme & Teilchen · Licht & Farben · Schall & Töne (Physik) · Stoffe trennen · Säuren & Laugen · Chemische Reaktionen (Chemie) · Vererbung & Genetik · Zellen & Osmose · Nährstoffe & Enzyme · Pflanzen & Ökosysteme (Biologie).
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
| 🌿 Bio-Dschungel | Mendels Erbsen, Wunderblume, Blutgruppen, Osmose, Plasmolyse, Nährstoff-Nachweise, Speichel-Enzym, Wasserpest, Hasen und Luchse | 3–10 |

## Korrekt gerechnet

Alle richtigen Antworten werden aus Modellen in `core/model` **berechnet**, nicht hingeschrieben, und in `CatalogTest`/`PhysicsTest` geprüft:
Auftrieb (ρ-Vergleich, Eintauchtiefe), Hebelgesetz, Flaschenzug (Goldene Regel), Stromkreise (Reihe/Parallel, Kurzschluss),
pH aus [H⁺]−[OH⁻] mit Kw, Atomzählung aller Reaktionen, begrenzender Stoff beim Natron-Ballon, Richmann-Mischungsregel, Heizkurve mit Schmelz- und
Verdampfungswärme, Fall mit Luftwiderstand (numerisch) und ohne, exakte Pendeldauer (elliptisches Integral), Röhren f = c/(4L),
Prisma mit Cauchy-Brechzahl und Snellius, Kreuzungsquadrat und Aussaat (Mendel, AB0), Osmose, Enzymaktivität, Lichtstärke ∼ 1/d², Lotka-Volterra, Trennverfahren (kürzester Weg), subtraktive Farbmischung (`BiologyTest`). Ziele, die man einstellen muss (z. B. 0,2 A, pH 2, 880 Hz, T = 2 s), sind mit den Reglern erreichbar.

## Frei und Pro

Vorbereitet wie bei Raumlicht, aber **ausgeschaltet** (`Pro.ENABLED = false`): Heute ist alles frei. Später frei: die ganze Wasserinsel,
von jeder Insel die ersten 3 Versuche, Rätsel des Tages, Forscherbuch, 6 Geräte im Freien Labor. Der „Forscher-Pass“ (Einmalkauf, kein Abo) öffnet alles.
Eingeschaltet wird erst, wenn es einen Bezahlweg gibt.

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
