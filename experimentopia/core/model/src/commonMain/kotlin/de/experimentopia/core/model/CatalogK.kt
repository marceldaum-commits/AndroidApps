package de.experimentopia.core.model

import de.experimentopia.core.model.Pick.nearest

/**
 * Erweiterung (Version 2.18.0): Druck & Luftdruck – Druck = Kraft ÷ Fläche, Nagelbrett, Wasserdruck, Hydraulik, Magdeburger Halbkugeln (Lehrplan Kl. 7–9).
 * Alle Zahlen kommen aus [Druck]; Antworten werden berechnet, nicht hingeschrieben.
 */
internal object CatalogK {
    private val W = Island.KRAFT
    private fun kpa(v: Double) = "${Fmt.num(v / 1000, 0)} kPa"
    private fun bar(v: Double) = "${Fmt.num(v, 1)} bar"
    private fun metre(v: Double) = "${Fmt.num(v, 0)} m"
    private fun newton(v: Double) = "${Fmt.num(v, 0)} N"
    private fun kg(v: Double) = "${Fmt.num(v, 0)} kg"
    private fun near(q: String, opts: List<Double>, value: Double, fmt: (Double) -> String) = quiz(q, opts.map { c(fmt(it)) }, nearest(opts, value))
    private val p900 = Druck.nailPressure(900)                                   // 190,8 kPa
    private val f700 = Druck.weight(Druck.PERSON_KG)                              // 686,7 N

    private val raw = listOf(
        Experiment("d1", W, "Spitz oder breit?", "🛏️", 7..8,
            "Blubb will sich auf ein Nagelbrett legen – ehrlich! Dafür muss sein Gewicht (70 kg, also 686,7 N) auf viele Nägel verteilt werden, sonst tut es weh.",
            quiz("Blubb legt sich auf ein Brett mit mehr Nägeln. Der Druck auf seine Haut wird …", listOf(c("kleiner", "🙂"), c("größer", "😖"), c("gleich", "🟰")), Pick.index(Druck.nailPressure(1000) < Druck.nailPressure(100))),
            PressureSetup(PressureMode.NAILS, maxKPa = 400.0),
            "Wähle so viele Nägel, dass der Druck auf die Haut höchstens 400 kPa beträgt. Jeder Nagel hat 4 mm² an der Spitze.",
            "Druck = Kraft ÷ Fläche. Mit 500 Nägeln: p = 686,7 N ÷ (500 · 0,000004 m²) = 343 350 Pa ≈ 343 kPa – das ist unter 400 kPa. Mit 400 Nägeln wären es 429 kPa.",
            listOf("Dieselbe Kraft auf eine größere Fläche macht einen kleineren Druck: p = F ÷ A.", "Mehr Nägel machen den Druck größer, weil sie mehr drücken.", "Der Druck hängt nur vom Gewicht ab, nicht von der Fläche."),
            "Druck p = F ÷ A (Kraft durch Fläche). Gleiche Kraft, größere Fläche: kleinerer Druck.",
            quiz("Warum sinkt man mit breiten Skiern im Schnee weniger ein?", listOf(c("Das Gewicht verteilt sich auf mehr Fläche, der Druck sinkt"), c("Ski sind leichter"), c("Schnee ist unter Ski härter")), 0)),

        Experiment("d2", W, "Auf dem Nagelbrett", "🧘", 8..9,
            "Jetzt wird gerechnet: Ein Nagelbrett für 70 kg, damit der Druck unter 200 kPa bleibt – so viel hält die Haut aus, ohne dass es sticht.",
            near("70 kg liegen auf 900 Nägeln (je 4 mm²). Wie groß ist der Druck?", listOf(19.0, 191.0, 1910.0, 19100.0).map { it * 1000 }, p900, ::kpa),
            PressureSetup(PressureMode.NAILS, maxKPa = 200.0),
            "Wähle so viele Nägel, dass der Druck höchstens 200 kPa beträgt (± Rundung auf 100 Nägel).",
            "p = F ÷ A = 686,7 N ÷ (900 · 4·10⁻⁶ m²) = 686,7 ÷ 0,0036 = 190 750 Pa ≈ 191 kPa. Mit 800 Nägeln wären es 215 kPa – knapp zu viel.",
            listOf("Gesamtfläche = Anzahl · Fläche eines Nagels. Der Druck sinkt, wenn die Gesamtfläche steigt.", "Der Druck bleibt gleich, nur die Kraft ändert sich.", "Je mehr Nägel, desto größer der Druck."),
            "Beim Nagelbrett: p = F ÷ (Anzahl · Spitzenfläche).",
            quiz("Du legst dich auf halb so viele Nägel. Der Druck wird …", listOf(c("doppelt so groß"), c("halb so groß"), c("gleich groß")), 0)),

        Experiment("d3", W, "Tief tauchen: 1 bar mehr", "🤿", 7..8,
            "Blubb taucht im Schwimmbad. Je tiefer er kommt, desto mehr drückt das Wasser auf seine Ohren. Außerdem drückt oben die ganze Luft auf das Wasser.",
            near("Wie groß ist der Gesamtdruck (Luft + Wasser) in 10 m Wassertiefe?", listOf(1.0, 2.0, 11.0, 20.0), Druck.total(10.0) / Druck.BAR, ::bar),
            PressureSetup(PressureMode.DIVE, targetBar = 2.0),
            "Tauche so tief, dass der Gesamtdruck 2,0 bar beträgt (± 3 %).",
            "An der Oberfläche drückt die Luft mit ≈ 1,01 bar. Das Wasser dazu: p = ρ · g · h = 1000 kg/m³ · 9,81 m/s² · 10 m = 98 100 Pa ≈ 0,98 bar. Zusammen ≈ 2,0 bar in 10 m Tiefe.",
            listOf("Das Gewicht der Wassersäule kommt zum Luftdruck dazu: p = p_Luft + ρ · g · h. Alle 10 m etwa 1 bar mehr.", "In 10 m Tiefe ist der Druck zehnmal so groß wie oben.", "Der Druck hängt nur von der Breite des Beckens ab."),
            "Wasserdruck: p = ρ · g · h. Alle 10 m Tiefe kommt etwa 1 bar dazu.",
            quiz("Pro 10 Meter Tiefe steigt der Wasserdruck um etwa …", listOf(c("0,1 bar"), c("1 bar"), c("10 bar")), 1)),

        Experiment("d4", W, "Wie tief für 3 bar?", "🌊", 8..9,
            "Ein Tauchlehrer sagt: „Mit Pressluft darfst du Anfänger nur bis 3 bar Gesamtdruck.“ Wie tief ist das eigentlich?",
            near("In welcher Tiefe herrscht ein Gesamtdruck von 3 bar?", listOf(10.0, 20.0, 30.0, 60.0), Druck.depthFor(3 * Druck.BAR), ::metre),
            PressureSetup(PressureMode.DIVE, targetBar = 3.0),
            "Tauche so tief, dass der Gesamtdruck 3,0 bar beträgt (± 3 %).",
            "p = p_Luft + ρ · g · h, also h = (p − p_Luft) ÷ (ρ · g) = (300 000 Pa − 101 325 Pa) ÷ (1000 · 9,81) = 198 675 ÷ 9810 ≈ 20,3 m.",
            listOf("Nach h auflösen: h = (p − Luftdruck) ÷ (ρ · g). Den Luftdruck musst du abziehen.", "3 bar entsprechen 30 m, weil man nichts abziehen muss.", "Die Tiefe ist unabhängig vom Druck."),
            "Tiefe aus dem Druck: h = (p − p_Luft) ÷ (ρ · g).",
            near("In welcher Tiefe herrscht ein Gesamtdruck von 4 bar?", listOf(20.0, 30.0, 40.0, 60.0), Druck.depthFor(4 * Druck.BAR), ::metre)),

        Experiment("d5", W, "Die Hebebühne", "🚗", 8..9,
            "In der Werkstatt hebt eine Hebebühne ein Auto (1 200 kg) mit einem kleinen Pumpenkolben. Wie geht das, ohne dass jemand das Auto stemmt?",
            near("Kleiner Kolben A₁ = 0,002 m² mit F₁ = 200 N, großer Kolben A₂ = 0,10 m². Welche Kraft entsteht am großen Kolben?", listOf(2_000.0, 10_000.0, 20_000.0, 200_000.0), Druck.hydraulicForce(200.0, 0.002, 0.10), ::newton),
            PressureSetup(PressureMode.HYDRAULIC, minForceN = Druck.weight(Druck.CAR_KG), f1 = 200.0, a1 = 0.002),
            "Wähle die Fläche des großen Kolbens so, dass die Kraft das Auto (1 200 kg = 11 772 N) heben kann.",
            "In der Flüssigkeit ist der Druck überall gleich: F₁ ÷ A₁ = F₂ ÷ A₂. Also F₂ = F₁ · A₂ ÷ A₁ = 200 N · 0,10 m² ÷ 0,002 m² = 10 000 N. Fürs Auto (11 772 N) brauchst du A₂ ≥ 0,118 m², also mindestens 0,12 m².",
            listOf("Der Druck ist überall in der Flüssigkeit gleich. Auf der großen Fläche wird daraus eine große Kraft: F₂ = F₁ · A₂ ÷ A₁.", "Die Kraft bleibt gleich, nur das Öl ändert die Richtung.", "Die Kraft wird kleiner, wenn der Kolben größer wird."),
            "Hydraulik: F₂ ÷ F₁ = A₂ ÷ A₁. Doppelte Fläche, doppelte Kraft.",
            quiz("Was ist der Preis für die große Kraft bei der Hebebühne?", listOf(c("Der kleine Kolben muss einen längeren Weg zurücklegen"), c("Man braucht mehr Öl, aber keinen Weg"), c("Es gibt keinen Preis")), 0)),

        Experiment("d6", W, "Kleine Kraft, großer Kolben", "🏗️", 8..9,
            "Blubb will mit nur 100 N Muskelkraft einen schweren Tresor heben. Der Pumpenkolben hat 10 cm² (0,001 m²).",
            near("Mit F₁ = 100 N, A₁ = 0,001 m² und A₂ = 0,05 m²: Welche Masse kann der große Kolben heben?", listOf(5.0, 51.0, 510.0, 5100.0), Druck.hydraulicForce(100.0, 0.001, 0.05) / Druck.G, ::kg),
            PressureSetup(PressureMode.HYDRAULIC, minForceN = 5000.0, f1 = 100.0, a1 = 0.001),
            "Wähle die Fläche des großen Kolbens so, dass er mindestens 5 000 N erzeugt.",
            "F₂ = F₁ · A₂ ÷ A₁ = 100 N · 0,05 m² ÷ 0,001 m² = 5 000 N. Das hebt eine Masse m = F ÷ g = 5 000 N ÷ 9,81 m/s² ≈ 510 kg. Die Fläche ist 50-mal so groß, die Kraft auch.",
            listOf("Das Flächenverhältnis A₂ ÷ A₁ = 50 wird zum Kraftverhältnis. Aus 100 N werden 5 000 N.", "Die Kraft bleibt 100 N.", "Die Kraft wird 50-mal kleiner."),
            "Hydraulik: Flächenverhältnis = Kraftverhältnis.",
            quiz("Das Flächenverhältnis A₂ ÷ A₁ ist 20. Die Kraft am großen Kolben wird …", listOf(c("20-mal so groß"), c("gleich groß"), c("20-mal kleiner")), 0)),

        Experiment("d7", W, "Magdeburger Halbkugeln", "⚫", 8..9,
            "1654 pumpte Otto von Guericke zwei Halbkugeln leer. Dann konnten 16 Pferde sie nicht auseinanderziehen! Was hielt sie zusammen? Die Luft von außen.",
            near("Zwei leergepumpte Halbkugeln mit Radius 10 cm: Mit welcher Kraft drückt die Luft sie zusammen?", listOf(320.0, 3_200.0, 32_000.0, 320_000.0), Druck.hemisphereForce(0.10), ::newton),
            PressureSetup(PressureMode.HEMISPHERE, minForceN = 1000.0),
            "Wähle den Radius so, dass die Luft die Halbkugeln mit mindestens 1 000 N zusammendrückt.",
            "Außen drückt die Luft mit p = 101 325 Pa auf die Fläche A = π · r². Für r = 0,10 m: A = π · 0,01 m² = 0,0314 m², also F = p · A = 101 325 Pa · 0,0314 m² ≈ 3 183 N. Für 1 000 N: r ≥ 5,6 cm, also mindestens 6 cm.",
            listOf("Innen ist (fast) keine Luft, außen drückt sie auf die ganze Querschnittsfläche: F = p · π · r².", "Innen drückt die Luft stärker nach außen.", "Die Kugeln kleben von allein."),
            "Luftdruck drückt auf jede Fläche: F = p · A. Bei Vakuum innen hält diese Kraft die Halbkugeln zusammen.",
            quiz("Was passiert, wenn man in die Halbkugeln wieder Luft lässt?", listOf(c("Sie lassen sich leicht trennen"), c("Sie halten noch fester"), c("Nichts ändert sich")), 0)),

        Experiment("d8", W, "Die Tiefsee-Grenze", "🐋", 8..9,
            "Ein Forschungs-U-Boot soll bis zu 6 bar Gesamtdruck aushalten. Wie tief darf es damit tauchen?",
            near("Bis in welche Tiefe darf das U-Boot bei höchstens 6 bar Gesamtdruck?", listOf(25.0, 50.0, 60.0, 100.0), Druck.depthFor(6 * Druck.BAR), ::metre),
            PressureSetup(PressureMode.DIVE, targetBar = 6.0),
            "Tauche so tief, dass der Gesamtdruck 6,0 bar beträgt (± 3 %).",
            "h = (p − p_Luft) ÷ (ρ · g) = (600 000 Pa − 101 325 Pa) ÷ 9 810 N/m³ ≈ 50,8 m. Tiefer wird der Druck zu groß.",
            listOf("Aus dem erlaubten Druck die Tiefe berechnen: h = (p − Luftdruck) ÷ (ρ · g).", "6 bar sind 60 m, weil 1 bar je 10 m dazukommt.", "Das U-Boot darf beliebig tief tauchen."),
            "Der erlaubte Druck bestimmt die größte Tauchtiefe.",
            quiz("Ein Taucher steigt von 50 m schnell nach oben. Die Luft in seiner Lunge …", listOf(c("dehnt sich aus, weil der Druck sinkt"), c("wird zusammengedrückt"), c("bleibt gleich")), 0)),
    )

    private fun mixed(q: Quiz, shift: Int): Quiz {
        val numeric = q.choices.all { ch -> ch.text.firstOrNull()?.let { it.isDigit() || it == '−' } == true }
        if (numeric || q.choices.size < 2) return q
        val k = shift % q.choices.size
        val rotated = q.choices.drop(k) + q.choices.take(k)
        return q.copy(choices = rotated, answer = (q.answer - k + q.choices.size) % q.choices.size)
    }

    val pressure: List<Experiment> = raw.mapIndexed { i, e -> e.copy(predict = mixed(e.predict, i), transfer = mixed(e.transfer, i + 1)) }
}
