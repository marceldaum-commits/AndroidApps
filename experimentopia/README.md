# Experimentopia 🧪 – das Versuchslabor für die Hosentasche (Klasse 3–10)

Viele Versuche kann man zu Hause nicht machen: Säure und Lauge mischen, Knallgas zünden, einen Hammer auf dem Mond fallen lassen,
ein Klavier mit dem Flaschenzug heben. In Experimentopia geht das – auf sieben Inseln, mit **Blubb**, einem neugierigen Laborkolben,
dessen Flüssigkeit mit seiner Laune die Farbe wechselt (Recherche: `../docs/RECHERCHE.md`).

**Version 0.1.0 vom 16.11.2026** · 68 Versuche · 20 Geräte im Freien Labor · 12 Reaktionsgleichungen · 13 Abzeichen

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

## Die Inseln

| Insel | Versuche (Auswahl) | Klasse |
|---|---|---|
| 🌊 Wasserinsel | Holz vs. Nagel, Riesenkerze, nackte Orange, Ei im Salzwasser, Knetboot, Eis in Spiritus, Dichteturm, Archimedes | 3–7 |
| ⚖️ Kraftwerk-Insel | Kind gegen Papa auf der Wippe, Gleichgewicht, Hebelgesetz, feste/lose Rolle, Klavier mit Flaschenzug, schiefe Ebene | 3–8 |
| ⚡ Blitzinsel | Leitertest, Bleistiftmine, UND/ODER-Schalter, Reihe/Parallel, Kurzschluss, Ohmsches Gesetz, Magnete, Pole | 3–9 |
| 🌋 Chemie-Vulkan | Rotkohl-Zaubersaft, pH-Wert, Neutralisation, pH-Sprung, Verdünnen, Natron-Ballon, Knallgas, Magnesium, Rost, Fotosynthese … | 3–10 |
| 🔥 Feuer & Eis | Was schmilzt zuerst, Heizkurve mit Teilchenblick, Mischtemperatur, Badewasser, glühendes Eisen | 3–8 |
| 🌈 Licht & Klang | Licht mischen, Farbfilter, Prisma (echte Brechung), Gläser-Xylofon, Röhren-Orgel, Stimmgerät, „Alle meine Entchen“, Saite | 3–8 |
| 🚀 Fallturm-Insel | Kugel gegen Feder, Papier-Trick, Vakuumröhre, Fallzeit, Mond, Pendel, Sekundenpendel | 3–10 |

## Korrekt gerechnet

Alle richtigen Antworten werden aus Modellen in `core/model` **berechnet**, nicht hingeschrieben, und in `CatalogTest`/`PhysicsTest` geprüft:
Auftrieb (ρ-Vergleich, Eintauchtiefe), Hebelgesetz, Flaschenzug (Goldene Regel), Stromkreise (Reihe/Parallel, Kurzschluss),
pH aus [H⁺]−[OH⁻] mit Kw, Atomzählung aller Reaktionen, begrenzender Stoff beim Natron-Ballon, Richmann-Mischungsregel, Heizkurve mit Schmelz- und
Verdampfungswärme, Fall mit Luftwiderstand (numerisch) und ohne, exakte Pendeldauer (elliptisches Integral), Röhren f = c/(4L),
Prisma mit Cauchy-Brechzahl und Snellius. Ziele, die man einstellen muss (z. B. 0,2 A, pH 2, 880 Hz, T = 2 s), sind mit den Reglern erreichbar.

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

## Bauen

```
./gradlew :core:model:test :core:data:testDebugUnitTest      # Modelle und Speicher
./gradlew :app:testGithubDebugUnitTest --tests '*ScreenshotTest*'   # Bildschirmfotos nach screenshots/
./gradlew assembleGithubRelease                              # APK (Fassung mit eigenem Update-Knopf)
```

Releases baut `.github/workflows/experimentopia-release.yml` (Tag `experimentopia-v<Version>`), Text aus `CHANGELOG.md`.

Datenschutz: kein Konto, keine Werbung, keine Käufe, keine Kamera, kein Mikrofon. Gespeichert wird nur auf dem Gerät. Ins Internet nur für die Update-Prüfung (abschaltbar).
