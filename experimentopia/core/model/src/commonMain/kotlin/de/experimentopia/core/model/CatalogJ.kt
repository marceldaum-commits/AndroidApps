package de.experimentopia.core.model

import de.experimentopia.core.model.Pick.nearest

/**
 * Erweiterung (Version 2.17.0): Energie-Achterbahn – Lageenergie, Bewegungsenergie, Energieerhaltung, Looping, Reibung (Lehrplan Kl. 7–10).
 * Alle Zahlen kommen aus [Energie]; Antworten werden berechnet, nicht hingeschrieben.
 */
internal object CatalogJ {
    private val K = Island.KRAFT
    private fun kj(v: Double) = "${Fmt.num(v, 0)} kJ"
    private fun kmh(v: Double) = "${Fmt.num(v, 0)} km/h"
    private fun m(v: Double) = "${Fmt.num(v, 1)} m"
    private fun near(q: String, opts: List<Double>, value: Double, fmt: (Double) -> String) = quiz(q, opts.map { c(fmt(it)) }, nearest(opts, value))
    private val v20 = Energie.speed(20.0)                                   // 19,81 m/s
    private val e20 = Energie.potential(Energie.WAGON_KG, 20.0) / 1000      // 78,5 kJ

    private val raw = listOf(
        Experiment("z1", K, "Hoch hinauf, viel Energie", "⛰️", 7..8,
            "Blubb sitzt im Achterbahn-Wagen (400 kg) und wird von der Kette nach oben gezogen. Je höher, desto mehr Energie steckt im Wagen – aber wie viel?",
            near("Wie viel Lageenergie hat der Wagen (400 kg) in 20 m Höhe?", listOf(8.0, 78.0, 400.0, 780.0), e20, ::kj),
            CoasterSetup(CoasterMode.ENERGY, 5.0, minEnergyKJ = 100.0),
            "Zieh den Wagen so hoch, dass er mindestens 100 kJ Lageenergie hat.",
            "E = m · g · h = 400 kg · 9,81 m/s² · 20 m = 78 480 J ≈ ${Fmt.num(e20, 1)} kJ. Für 100 kJ brauchst du h = E ÷ (m · g) = 100 000 J ÷ (400 kg · 9,81 m/s²) ≈ 25,5 m, also mindestens 26 m.",
            listOf("Lageenergie wächst mit Masse und Höhe: E = m · g · h. Doppelte Höhe, doppelte Energie.", "Die Lageenergie hängt nur von der Farbe des Wagens ab.", "Je höher, desto weniger Energie."),
            "Lageenergie: E = m · g · h. Doppelte Höhe oder doppelte Masse, doppelte Energie.",
            quiz("Du ziehst denselben Wagen auf die dreifache Höhe. Die Lageenergie wird …", listOf(c("dreimal so groß"), c("gleich groß"), c("neunmal so groß")), 0)),

        Experiment("z2", K, "Wie schnell unten?", "💨", 7..9,
            "Der Wagen rollt von der Spitze des ersten Hügels ohne Motor nach unten. Blubb hält sich fest. Wie schnell wird es?",
            near("Der Wagen startet in Ruhe 20 m hoch (ohne Reibung). Wie schnell ist er unten?", listOf(20.0, 71.0, 140.0, 280.0), Energie.toKmh(v20), ::kmh),
            CoasterSetup(CoasterMode.SPEED, 5.0, targetKmh = 72.0),
            "Wähle die Starthöhe so, dass der Wagen unten 72 km/h schnell ist (± 3 %).",
            "Die Lageenergie wird zu Bewegungsenergie: m · g · h = ½ · m · v². Also v = √(2 · g · h) = √(2 · 9,81 · 20) ≈ ${Fmt.num(v20, 1)} m/s ≈ ${Fmt.num(Energie.toKmh(v20), 0)} km/h. Für 72 km/h = 20 m/s brauchst du h = v² ÷ (2 · g) = 400 ÷ 19,62 ≈ 20,4 m.",
            listOf("Aus Lageenergie wird Bewegungsenergie: m · g · h = ½ · m · v². Je höher der Start, desto schneller unten.", "Der Wagen wird beim Fallen langsamer.", "Das Tempo hängt nur von der Länge der Strecke ab."),
            "Ohne Reibung: v = √(2 · g · h). Tempo unten hängt nur von der Starthöhe ab.",
            near("Aus welcher Höhe muss der Wagen starten, um unten 36 km/h (10 m/s) zu haben?", listOf(2.5, 5.1, 10.2, 20.0), Energie.heightForSpeed(10.0), ::m)),

        Experiment("z3", K, "Schwer oder leicht – wer ist schneller?", "⚖️", 7..9,
            "Zwei Wagen starten oben gleichzeitig: ein leerer (200 kg) und ein voll besetzter (800 kg). Wer ist unten schneller?",
            quiz("Beide starten in 20 m Höhe, ohne Reibung. Wer ist unten schneller?", listOf(c("Beide gleich schnell", "🟰"), c("Der schwere Wagen", "🏋️"), c("Der leichte Wagen", "🪶")), 0),
            CoasterSetup(CoasterMode.SPEED, 10.0, adjustMass = true, targetKmh = 54.0),
            "Probiere verschiedene Massen aus. Dann stell die Starthöhe so ein, dass unten 54 km/h herauskommen (± 3 %).",
            "Die Masse kürzt sich: m · g · h = ½ · m · v² ergibt v = √(2 · g · h), ohne m. Bei 20 m sind es für 200 kg und für 800 kg beide ≈ ${Fmt.num(Energie.toKmh(v20), 0)} km/h. Für 54 km/h = 15 m/s: h = 15² ÷ (2 · 9,81) ≈ 11,5 m.",
            listOf("Der schwere Wagen hat mehr Energie, muss aber auch mehr bewegen. Das hebt sich auf: Das Tempo hängt nicht von der Masse ab.", "Schwere Wagen sind immer schneller.", "Leichte Wagen sind immer schneller."),
            "Ohne Reibung hängt das Tempo unten nicht von der Masse ab.",
            quiz("Ein Kind (30 kg) und ein Erwachsener (90 kg) rutschen von derselben Höhe reibungsfrei. Wer ist unten schneller?", listOf(c("Beide gleich schnell"), c("Der Erwachsene"), c("Das Kind")), 0)),

        Experiment("z4", K, "Der Looping", "🔄", 8..10,
            "Blubb will durch einen Looping mit 5 m Radius fahren. Oben hängt er mit dem Kopf nach unten – er darf nicht zu langsam sein, sonst fällt er heraus!",
            near("Welche Starthöhe braucht der Wagen für einen Looping mit R = 5 m mindestens (ohne Reibung)?", listOf(5.0, 12.5, 25.0, 50.0), Energie.loopMinHeight(), ::m),
            CoasterSetup(CoasterMode.LOOP, 6.0, maxStartM = 13.0),
            "Schaffe den Looping mit einer Starthöhe von höchstens 13 m.",
            "Oben (2R = 10 m hoch) braucht der Wagen v² ≥ g · R = 49,05 m²/s². Aus v² = 2 · g · (h − 2R) folgt h ≥ 2,5 · R = 12,5 m. Mit 13 m klappt es, mit 12 m nicht.",
            listOf("Oben im Looping muss der Wagen schnell genug sein (v² ≥ g · R). Das kostet Energie: h ≥ 2,5 · R.", "Der Looping klappt aus jeder Höhe.", "Je niedriger der Start, desto sicherer der Looping."),
            "Looping ohne Reibung: Starthöhe mindestens das 2,5-Fache des Radius.",
            near("Ein Looping hat einen Radius von 8 m. Mindeststarthöhe ohne Reibung?", listOf(8.0, 16.0, 20.0, 40.0), Energie.loopMinHeight(8.0), ::m)),

        Experiment("z5", K, "Reibung klaut Energie", "🔥", 8..10,
            "Die Räder und die Luft bremsen den Wagen. Im Labor nimmt die Reibung 20 % der Energie als Wärme weg. Reicht die alte Starthöhe noch?",
            near("Mit 20 % Reibung: Welche Starthöhe braucht der Wagen jetzt mindestens für den Looping (R = 5 m)?", listOf(10.0, 12.5, 15.6, 25.0), Energie.loopMinHeight(friction = 0.2), ::m),
            CoasterSetup(CoasterMode.LOOP, 13.0, frictionPct = 20, maxStartM = 16.0),
            "Schaffe den Looping mit 20 % Reibung und einer Starthöhe von höchstens 16 m.",
            "Nur 80 % der Energie bleiben: 0,8 · h ≥ 2,5 · R = 12,5 m, also h ≥ 12,5 ÷ 0,8 = 15,6 m. Mit 16 m klappt es (0,8 · 16 = 12,8 m ≥ 12,5 m), mit 15 m nicht (12 m).",
            listOf("Reibung macht Energie zu Wärme. Es bleibt weniger für Tempo übrig, also muss man höher starten.", "Reibung macht den Wagen schneller.", "Reibung ändert nichts an der Energie."),
            "Mit Reibung muss man höher starten: nutzbar bleibt nur (1 − Reibungsanteil) der Energie.",
            quiz("Wo bleibt die Energie, die die Reibung wegnimmt?", listOf(c("Sie wird zu Wärme"), c("Sie verschwindet"), c("Sie wird zu Lageenergie")), 0)),

        Experiment("z6", K, "Der zweite Hügel", "🏔️", 7..9,
            "Hinter dem ersten Hügel wartet ein zweiter, 10 m hoch. Die Reibung nimmt 10 % der Energie. Schafft der Wagen den zweiten Hügel?",
            near("Mit 10 % Reibung: Wie hoch muss der Start mindestens sein, damit der Wagen über den 10-m-Hügel kommt?", listOf(10.0, 11.1, 15.0, 20.0), Energie.hillMinHeight(friction = 0.1), ::m),
            CoasterSetup(CoasterMode.HILL, 8.0, frictionPct = 10, maxStartM = 12.0),
            "Bring den Wagen über den zweiten Hügel (10 m) mit einer Starthöhe von höchstens 12 m.",
            "Nutzbar bleiben 90 % der Energie: 0,9 · h ≥ 10 m, also h ≥ 10 ÷ 0,9 = 11,1 m. Mit 12 m klappt es (0,9 · 12 = 10,8 m), mit 11 m nicht (9,9 m).",
            listOf("Der Wagen kommt nur so hoch, wie seine nutzbare Energie reicht. Mit Reibung muss der erste Hügel höher sein als der zweite.", "Der zweite Hügel darf höher sein als der erste.", "Reibung hilft beim Hinaufkommen."),
            "Der zweite Hügel muss niedriger sein als der erste – mit Reibung sogar deutlich.",
            quiz("Der erste Hügel ist 30 m hoch. Der Wagen kann ohne Motor und ohne Reibung höchstens auf …", listOf(c("30 m Höhe"), c("60 m Höhe"), c("15 m Höhe")), 0)),

        Experiment("z7", K, "Halbe Höhe, halbe Energie", "📊", 8..10,
            "Der Wagen startet aus 30 m Höhe. Unterwegs wird Lageenergie zu Bewegungsenergie. Wo sind beide gleich groß?",
            near("Bei Start aus 30 m: In welcher Höhe sind Lage- und Bewegungsenergie gleich groß?", listOf(7.5, 15.0, 20.0, 30.0), 15.0, ::m),
            CoasterSetup(CoasterMode.SPLIT, 30.0, adjustStart = false),
            "Fahr den Wagen auf der Strecke an die Stelle, an der Lage- und Bewegungsenergie etwa gleich groß sind.",
            "Die Gesamtenergie bleibt gleich: E = m · g · H. Der Bewegungsanteil ist (H − h) ÷ H. Er ist 50 %, wenn h = H ÷ 2 = 15 m.",
            listOf("Die Summe aus Lage- und Bewegungsenergie bleibt gleich. Bei halber Höhe ist die Hälfte schon zu Bewegung geworden.", "Nur unten ist Energie im Wagen.", "Bewegungsenergie ist immer größer."),
            "Energieerhaltung: Lageenergie + Bewegungsenergie bleibt gleich (ohne Reibung).",
            quiz("Der Wagen startet in 40 m Höhe. In 10 m Höhe ist der Anteil der Bewegungsenergie …", listOf(c("75 %"), c("25 %"), c("50 %")), 0)),

        Experiment("z8", K, "Das Tempo am Ziel", "🏁", 9..10,
            "Die große Bahn ist 45 m hoch, die Reibung nimmt 10 % der Energie. Der Betreiber will wissen: Wie schnell sind die Fahrgäste unten?",
            near("Start aus 45 m, 10 % Reibung. Wie schnell ist der Wagen unten?", listOf(60.0, 100.0, 120.0, 150.0), Energie.toKmh(Energie.speed(45.0, 0.1)), ::kmh),
            CoasterSetup(CoasterMode.SPEED, 20.0, frictionPct = 10, targetKmh = 100.0),
            "Wähle mit 10 % Reibung die Starthöhe so, dass unten 100 km/h herauskommen (± 3 %).",
            "v = √(2 · g · h · 0,9) = √(2 · 9,81 · 45 · 0,9) = √794,6 ≈ ${Fmt.num(Energie.speed(45.0, 0.1), 1)} m/s ≈ ${Fmt.num(Energie.toKmh(Energie.speed(45.0, 0.1)), 0)} km/h. Für 100 km/h = 27,8 m/s: h = v² ÷ (2 · g · 0,9) ≈ 43,7 m.",
            listOf("Nur 90 % der Energie werden zu Tempo: v = √(2 · g · h · 0,9). Das ist etwas langsamer als ohne Reibung.", "Reibung ändert das Tempo nicht.", "Die Fahrgäste sind schneller als ohne Reibung."),
            "Mit Reibung: v = √(2 · g · h · (1 − Reibungsanteil)).",
            near("Ohne Reibung aus 45 m: Wie schnell ist der Wagen unten?", listOf(60.0, 107.0, 130.0, 160.0), Energie.toKmh(Energie.speed(45.0)), ::kmh)),
    )

    private fun mixed(q: Quiz, shift: Int): Quiz {
        val numeric = q.choices.all { ch -> ch.text.firstOrNull()?.let { it.isDigit() || it == '−' } == true }
        if (numeric || q.choices.size < 2) return q
        val k = shift % q.choices.size
        val rotated = q.choices.drop(k) + q.choices.take(k)
        return q.copy(choices = rotated, answer = (q.answer - k + q.choices.size) % q.choices.size)
    }

    val energy: List<Experiment> = raw.mapIndexed { i, e -> e.copy(predict = mixed(e.predict, i), transfer = mixed(e.transfer, i + 1)) }
}
