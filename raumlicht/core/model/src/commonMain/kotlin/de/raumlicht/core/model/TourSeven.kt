package de.raumlicht.core.model

import de.raumlicht.core.model.Part.Num
import de.raumlicht.core.model.Part.Txt

/** Kapitel 22–23 (seit 2.7.0): Integrale und Flächen (Analysis), Erwartung, Sigma-Regel und Testen (Stochastik). */
internal object TourSeven {
    // ───────────────────────── 22 · Integrale und Flächen ─────────────────────────
    /** Bildet den Graphen in die Ebene x₁ = 4 ab: x läuft nach rechts (x₂), f(x) nach oben (x₃). */
    class Graph(val from: Double, val to: Double, val sx: Double, val ox: Double, val sy: Double, val oz: Double, val f: (Double) -> Double) {
        fun at(x: Double, y: Double = f(x)) = V3(4.0, ox + x * sx, oz + y * sy)
        fun curve(id: String = "bahn", color: String = "cyan", g: (Double) -> Double = f) = Stage.edges(id, (0..48).map { val x = from + (to - from) * it / 48; at(x, g(x)) }.zipWithNext(), color)
        fun axis() = Stage.axes2d(ox, oz, sx, sy, from, to)
        /** Leuchtfläche zwischen der Kurve und [base] (Standard: x-Achse) über [a, b] */
        fun area(id: String, a: Double, b: Double, color: String = "yellow", base: (Double) -> Double = { 0.0 }): Obj {
            val n = 24; val xs = (0..n).map { a + (b - a) * it / n }
            val top = xs.map { at(it) }; val bottom = xs.map { at(it, base(it)) }; val outline = top + bottom.reversed()
            // schmale Streifen statt einer großen Fläche: so folgt die Füllung auch gewölbten Kurven genau
            val strips = (0 until n).map { listOf(bottom[it], bottom[it + 1], top[it + 1], top[it]) }
            return Stage.edges(id, (outline.zipWithNext() + listOf(outline.last() to outline.first())).filter { (p, q) -> !p.near(q) }, color, faces = strips)
        }
    }
    private fun integral(id: String, title: String, mission: String, name: String, range: IntRange, solution: Int, start: Int, what: String, ok: String, lines: (Int) -> List<List<Part>>,
                         tip: String, way: List<String>, abi: Boolean = false, mistakes: Map<Int, String> = emptyMap(), objects: (Int, Phase) -> List<Obj>) =
        Families.scalar(id, 22, title, mission, name, range, solution, start, tip, way, "yellow", abi = abi,
            formula = lines, objects = objects, check = { a -> Families.compare(a, solution, what, ok, mistakes) })

    private val ch22 = run {
        val g1 = Graph(0.0, 3.0, 2.0, 1.0, 0.7, 0.8) { 2 * it }
        val g2 = Graph(0.0, 2.0, 3.0, 1.0, 0.4, 0.8) { 3 * it * it }
        val g3 = Graph(0.0, 2.0, 3.0, 1.0, 0.8, 0.8) { -3 * it * it + 8 * it }
        val g4 = Graph(0.0, 3.0, 2.0, 1.0, 0.9, 2.0) { it * it - 2 * it }
        val g5 = Graph(0.0, 5.0, 1.3, 0.8, 0.45, 0.8) { 2 * it }
        val g6 = Graph(0.0, 4.0, 1.6, 0.8, 0.38, 0.8) { -3 * it * it + 12 * it }
        listOf(
            integral("22-1", "Fläche unter der Rampe", "Unter der Rampe f soll ein Leuchtfeld liegen. Wie groß ist die Fläche zwischen Rampe und x-Achse von 0 bis 3?", "A", 0..18, 9, 3,
                "Die Fläche", "9 Flächeneinheiten – genau das halbe Rechteck.",
                { a -> listOf(listOf(Txt("f(x) = 2x,  x von 0 bis 3")), listOf(Txt("A = ∫ f(x) dx = "), Num(Geo.num(a), "a"))) },
                tip = "Stammfunktion von 2x ist F(x) = x². Dann F(3) − F(0).",
                way = listOf("A = ∫₀³ 2x dx", "= [x²]₀³ = 9 − 0", "= 9 (Probe: Dreieck ½ · 3 · 6 = 9)"),
                mistakes = mapOf(18 to "18 ist Grundseite mal Höhe – ein Rechteck. Unter der Rampe liegt nur die Hälfte.", 6 to "6 ist f(3), die Höhe am rechten Rand. Gesucht ist die Fläche.")) { _, _ ->
                listOf(g1.area("fläche", 0.0, 3.0), g1.curve(), g1.axis()) },
            integral("22-2", "Fläche unter der Parabel", "Jetzt ist der Rand gekrümmt. Wie groß ist die Fläche unter f von 0 bis 2?", "A", 0..24, 8, 2,
                "Die Fläche", "8 Flächeneinheiten – nur ein Drittel des Rechtecks.",
                { a -> listOf(listOf(Txt("f(x) = 3x²,  x von 0 bis 2")), listOf(Txt("A = ∫ f(x) dx = "), Num(Geo.num(a), "a"))) },
                tip = "Stammfunktion von 3x² ist F(x) = x³. Dann F(2) − F(0).",
                way = listOf("A = ∫₀² 3x² dx", "= [x³]₀²", "= 8 − 0 = 8"),
                mistakes = mapOf(12 to "12 ist f(2), die Höhe am rechten Rand. Gesucht ist die Fläche – dafür brauchst du die Stammfunktion.", 24 to "24 wäre das ganze Rechteck 2 · 12. Unter der Parabel liegt weniger.")) { _, _ ->
                listOf(g2.area("fläche", 0.0, 2.0), g2.curve(), g2.axis()) },
            integral("22-3", "Zwischen zwei Kurven", "Das Leuchtfeld liegt zwischen dem Bogen f und der Rampe g. Wie groß ist es?", "A", 0..10, 4, 0,
                "Die Fläche", "4 Flächeneinheiten zwischen Bogen und Rampe.",
                { a -> listOf(listOf(Txt("f(x) = −3x² + 8x")), listOf(Txt("g(x) = 2x,  Schnittstellen x = 0 und x = 2")), listOf(Txt("A = ∫ (f(x) − g(x)) dx = "), Num(Geo.num(a), "a"))) },
                tip = "Erst die Differenz bilden: f(x) − g(x) = −3x² + 6x. Die integrierst du von 0 bis 2.",
                way = listOf("f(x) − g(x) = −3x² + 6x", "∫₀² (−3x² + 6x) dx = [−x³ + 3x²]₀²", "= −8 + 12 = 4"),
                mistakes = mapOf(8 to "8 ist die Fläche unter f allein. Zieh die Fläche unter g ab – oder integriere gleich f − g.")) { _, _ ->
                listOf(g3.area("fläche", 0.0, 2.0, base = { 2 * it }), g3.curve(), g3.curve("rampe", "pink") { 2 * it }, g3.axis()) },
            integral("22-4", "Plus und Minus", "Ein Teil der Kurve liegt unter der x-Achse. Welchen Wert hat das Integral von 0 bis 3?", "I", -5..5, 0, 2,
                "Der Wert", "0 – die Fläche unter der Achse hebt die darüber genau auf.",
                { a -> listOf(listOf(Txt("f(x) = x² − 2x,  x von 0 bis 3")), listOf(Txt("∫ f(x) dx = "), Num(Geo.num(a), "a"))) },
                tip = "Stammfunktion: F(x) = ⅓x³ − x². Flächen unter der x-Achse zählt das Integral negativ.",
                way = listOf("F(x) = ⅓x³ − x²", "F(3) − F(0) = 9 − 9", "= 0: unter der Achse −⁴⁄₃, darüber +⁴⁄₃"),
                mistakes = mapOf(3 to "3 ist f(3). Für das Integral brauchst du die Stammfunktion F(x) = ⅓x³ − x².")) { _, _ ->
                listOf(g4.area("minus", 0.0, 2.0, "pink"), g4.area("plus", 2.0, 3.0, "yellow"), g4.curve(), g4.axis()) },
            integral("22-5", "Obere Grenze gesucht", "Das Leuchtfeld unter der Rampe soll genau 16 Flächeneinheiten groß sein. Bis zu welcher Stelle b muss es reichen?", "b", 0..5, 4, 1,
                "b", "b = 4 – dann ist die Fläche 4² = 16.",
                { a -> listOf(listOf(Txt("f(x) = 2x,  x von 0 bis b")), listOf(Txt("∫ f(x) dx = b² = 16")), listOf(Txt("b = "), Num(Geo.num(a), "a"))) },
                tip = "Das Integral von 0 bis b ist [x²] = b². Das soll 16 sein.",
                way = listOf("∫₀ᵇ 2x dx = [x²]₀ᵇ = b²", "b² = 16", "b = 4 (b = −4 liegt links von 0)")) { a, _ ->
                (if (a > 0) listOf(g5.area("fläche", 0.0, a.toDouble())) else emptyList()) + listOf(g5.curve(), g5.axis(), Stage.point("grenze", g5.at(a.toDouble(), 0.0), "yellow", "b", drop = false)) },
            integral("22-6", "Abi-Check: Einlass", "Die Rate r gibt an, wie viele Gäste gerade pro Stunde kommen (in Hundert). Wie viele Hundert Gäste kommen in den ersten zwei Stunden?", "N", 0..24, 16, 10,
                "Die Anzahl", "16 Hundert – also 1600 Gäste in zwei Stunden.",
                { a -> listOf(listOf(Txt("r(t) = −3t² + 12t  (Hundert Gäste pro Stunde)")), listOf(Txt("N = ∫ r(t) dt von 0 bis 2 = "), Num(Geo.num(a), "a"))) },
                tip = "Aus einer Änderungsrate wird der Bestand durch Integrieren. Stammfunktion: R(t) = −t³ + 6t².",
                way = listOf("N = ∫₀² (−3t² + 12t) dt", "= [−t³ + 6t²]₀² = −8 + 24", "= 16 Hundert = 1600 Gäste"), abi = true,
                mistakes = mapOf(12 to "12 ist r(2) – so viele kommen zum Zeitpunkt t = 2 pro Stunde. Gesucht ist die Gesamtzahl: das Integral.",
                    24 to "2 · 12 = 24 würde stimmen, wenn die Rate die ganze Zeit 12 wäre. Sie steigt aber erst an – integriere.")) { _, _ ->
                listOf(g6.area("fläche", 0.0, 2.0), g6.curve(), g6.axis()) },
        )
    }

    // ───────────────────────── 23 · Erwartung, Sigma-Regel, Testen ─────────────────────────
    private val lux = V3(8, 0, 1)
    private fun spot(i: Int, perRow: Int) = V3(4.0, 1.4 + (i % perRow) * 1.3, 1.3 + (i / perRow) * 1.3)
    /** Leuchtkugeln in Reihen, Farbe je Kugel */
    private fun dots(colors: List<String>, perRow: Int = 5, r: Double = 0.45) = colors.mapIndexed { i, c -> Stage.sphere("k$i", spot(i, perRow), r, c) } + Stage.podium()
    private fun sign(id: String, at: V3, text: String, color: String = "yellow") = Stage.point(id, at, color, text, style = "ghost", drop = false)

    private val wheel = List(5) { "violet" } + List(3) { "cyan" } + List(2) { "yellow" }
    /** Die acht gleich wahrscheinlichen Ausgänge von drei Versuchen (gelb = Treffer) als drei Kugeln übereinander */
    private val triples = (0 until 8).map { n -> listOf(n and 4 != 0, n and 2 != 0, n and 1 != 0) }
    private fun tripleDots(only: (List<Boolean>) -> Boolean = { true }) = triples.mapIndexed { i, t -> if (!only(t)) emptyList() else t.mapIndexed { j, hit ->
        Stage.sphere("t${i}_$j", V3(4.0, 0.9 + i * 0.9, 3.6 - j * 0.9), 0.33, if (hit) "yellow" else "violet") } }.flatten() + Stage.podium()

    private val ch23 = listOf(
        Families.scalar("23-1", 23, "Fairer Einsatz", "Am Glücksrad gewinnt man in 5 von 10 Feldern nichts, in 3 Feldern 2 € und in 2 Feldern 7 €. Welcher Einsatz macht das Spiel fair?", "E", 0..7, 2, 0,
            tip = "Fair heißt: Der Einsatz ist so groß wie der Gewinn im Schnitt. Erwartungswert = Summe aus Gewinn mal Wahrscheinlichkeit.",
            way = listOf("E = 0 · 0,5 + 2 · 0,3 + 7 · 0,2", "= 0 + 0,6 + 1,4", "= 2 € – bei diesem Einsatz gewinnt auf Dauer keiner"), color = "pink", suffix = " €",
            formula = { a -> listOf(listOf(Txt("Gewinn: 0 € (50 %) · 2 € (30 %) · 7 € (20 %)")), listOf(Txt("fairer Einsatz = Erwartungswert = "), Num("${a} €", "a"))) },
            finale = { Finale(acts = listOf(
                Act("Drei blaue Felder bringen 2 €: 2 · 0,3 = 0,60 €.", beams = (5..7).map { Beam(lux, spot(it, 5), "cyan", false) }),
                Act("Zwei goldene Felder bringen 7 €: 7 · 0,2 = 1,40 €.", beams = (8..9).map { Beam(lux, spot(it, 5), "yellow", false) }),
                Act("Zusammen 2 € pro Spiel – das ist der faire Einsatz.", ms = 900))) },
            objects = { _, _ -> dots(wheel) + listOf(sign("s0", V3(4.0, 7.4, 1.3), "0 €", "violet"), sign("s1", V3(4.0, 2.7, 3.3), "2 €", "cyan"), sign("s2", V3(4.0, 5.9, 3.3), "7 €", "yellow")) },
            check = { a -> Families.compare(a, 2, "Der Erwartungswert", "2 € – so viel gewinnt man im Schnitt pro Spiel.",
                mapOf(3 to "(0 + 2 + 7) : 3 = 3 wäre richtig, wenn alle drei Gewinne gleich wahrscheinlich wären. Gewichte mit 0,5, 0,3 und 0,2.")) }),
        Families.scalar("23-2", 23, "Höchstens einmal", "Ein Blitzlicht zündet mit 50 %. Ich löse dreimal aus. Wie wahrscheinlich zündet es höchstens einmal?", "P", 0..20, 10, 0,
            tip = "Höchstens einmal heißt: keinmal oder genau einmal. Von den 8 gleich wahrscheinlichen Ausgängen zählen 1 + 3.",
            way = listOf("P(X = 0) = 1 : 8", "P(X = 1) = 3 : 8 (drei Reihenfolgen)", "P(X ≤ 1) = 4 : 8 = 50 %"), color = "pink", step = 5, suffix = " %",
            formula = { a -> listOf(listOf(Txt("n = 3, p = 50 % · 8 gleich wahrscheinliche Ausgänge")), listOf(Txt("P(X ≤ 1) = "), Num("${a * 5} %", "a"))) },
            finale = { Finale(acts = listOf(
                Act("Keinmal: nur ein Ausgang von acht.", tripleDots { t -> t.count { it } == 0 }),
                Act("Genau einmal: drei Ausgänge – der Treffer kann vorn, in der Mitte oder hinten stehen.", tripleDots { t -> t.count { it } <= 1 }),
                Act("Zusammen 4 von 8: 50 %.", ms = 900))) },
            objects = { _, ph -> if (ph == Phase.DONE) tripleDots { t -> t.count { it } <= 1 } else tripleDots() },
            check = { a -> Families.compare(a * 5, 50, "Die Wahrscheinlichkeit", "Genau 50 % – vier von acht Ausgängen.",
                mapOf(15 to "Nah an 12,5 %? Das wäre nur „keinmal“. Dazu kommt „genau einmal“ mit drei Ausgängen.", 40 to "Nah an 37,5 %? Das wäre nur „genau einmal“. „Höchstens einmal“ schließt „keinmal“ mit ein.")) }),
        Families.scalar("23-3", 23, "Wie oft mindestens?", "Ein Blitzlicht zündet mit 50 %. Wie oft muss ich mindestens auslösen, damit es mit mindestens 90 % Sicherheit wenigstens einmal zündet?", "n", 1..8, 4, 1,
            tip = "Gegenereignis: Es zündet nie – das passiert mit 0,5ⁿ. Diese Zahl darf höchstens 10 % sein.",
            way = listOf("1 − 0,5ⁿ ≥ 0,9, also 0,5ⁿ ≤ 0,1", "n = 3: 0,125 – noch zu groß", "n = 4: 0,0625 → 93,75 % Sicherheit, also n = 4"), color = "pink",
            formula = { a -> listOf(listOf(Txt("p = 50 %, mindestens ein Treffer mit ≥ 90 %")), listOf(Txt("1 − 0,5ⁿ ≥ 0,9")), listOf(Txt("n = "), Num(Geo.num(a), "a"))) },
            finale = { Finale(acts = listOf(Act("Bei vier Versuchen bleibt es nur in 1 von 16 Fällen dunkel – das sind 6,25 %.", beams = (0 until 4).map { Beam(lux, spot(it, 8), "yellow", false) }),
                Act("Also zündet es mit 93,75 % wenigstens einmal. Drei Versuche hätten nur 87,5 % gebracht.", ms = 1100))) },
            objects = { a, _ -> dots(List(a) { "yellow" }, perRow = 8, r = 0.38) },
            check = { a -> when {
                a == 4 -> Outcome(true, "n = 4 – ab dann liegt die Sicherheit bei mindestens 90 % (genau: 93,75 %).")
                a < 4 -> Outcome(false, "Mit ${if (a == 1) "1 Versuch" else "$a Versuchen"} ist die Sicherheit erst ${Geo.num(100 - 100.0 / (1 shl a))} %. Das reicht noch nicht.")
                else -> Outcome(false, "Mit $a Versuchen reicht es auch – aber gefragt ist die kleinste Anzahl. Probier weniger.") } }),
        Families.scalar("23-4", 23, "Sigma-Regel", "400 Blitzlichter, jedes zündet mit 50 %. Etwa 95 % aller Shows liegen zwischen μ − 2σ und μ + 2σ. Wo liegt die untere Grenze?", "k", 32..40, 36, 40,
            tip = "μ = n · p und σ = √(n · p · (1 − p)). Dann μ − 2 · σ.",
            way = listOf("μ = 400 · 0,5 = 200", "σ = √(400 · 0,5 · 0,5) = √100 = 10", "μ − 2σ = 200 − 20 = 180 (obere Grenze 220)"), color = "pink", step = 5,
            formula = { a -> listOf(listOf(Txt("n = 400, p = 0,5 (binomialverteilt)")), listOf(Txt("μ − 2σ = "), Num(Geo.num(a * 5), "a"))) },
            finale = { Finale(acts = listOf(Act("Im Mittel zünden 200 der 400 Blitzlichter.", ms = 1000), Act("Zwei Standardabweichungen sind 20: Fast immer liegt die Anzahl zwischen 180 und 220.", ms = 1100))) },
            objects = { a, ph -> dots((0 until 20).map { if (it % 2 == 0) "yellow" else "cyan" }, perRow = 5, r = 0.36) + sign("grenze", V3(4.0, 4.0, 6.0), if (ph == Phase.DONE) "180 bis 220" else "ab ${a * 5}?") },
            check = { a -> Families.compare(a * 5, 180, "Die Grenze", "180 – zwischen 180 und 220 liegen etwa 95 % aller Shows.",
                mapOf(190 to "190 ist μ − σ. Für rund 95 % brauchst du zwei Standardabweichungen.", 200 to "200 ist der Erwartungswert μ selbst. Zieh 2 · σ ab.")) }),
        Families.scalar("23-5", 23, "Abi-Check: Hypothesentest", "Der Hersteller sagt: Die Blitzlichter zünden mit 50 %. Ich vermute mehr und teste 100 Stück (Irrtumswahrscheinlichkeit höchstens 5 %). Ab wie vielen Zündungen lehne ich seine Angabe ab?", "k", 50..65, 59, 50, abi = true,
            tip = "Rechtsseitiger Test: Die Grenze liegt bei μ + 1,64 · σ. Erst μ und σ für n = 100 und p = 0,5 ausrechnen, dann aufrunden.",
            way = listOf("μ = 100 · 0,5 = 50, σ = √25 = 5", "μ + 1,64 · σ = 50 + 8,2 = 58,2", "ablehnen ab k = 59 (genau gerechnet: P(X ≥ 59) ≈ 4,4 %)"), color = "pink",
            formula = { a -> listOf(listOf(Txt("H₀: p = 0,5 · n = 100 · Signifikanzniveau 5 %")), listOf(Txt("Ablehnungsbereich: X ≥ "), Num(Geo.num(a), "a"))) },
            finale = { Finale(acts = listOf(Act("Stimmt die Angabe, erwarte ich 50 Zündungen, mit einer Streuung von 5.", ms = 1100),
                Act("59 oder mehr kämen dann nur in etwa 4 von 100 Tests vor – so viel Zufall glaube ich nicht.", ms = 1200))) },
            objects = { a, ph -> dots((0 until 20).map { if (it < (a * 20 + 50) / 100) "yellow" else "cyan" }, perRow = 5, r = 0.36) + sign("grenze", V3(4.0, 4.0, 6.0), if (ph == Phase.DONE) "ablehnen ab 59" else "ablehnen ab $a?") },
            check = { a -> Families.compare(a, 59, "Die Grenze", "Ab 59 Zündungen – dann liegt die Irrtumswahrscheinlichkeit unter 5 %.",
                mapOf(58 to "58,2 ist die Rechengrenze. 58 liegt noch darunter – P(X ≥ 58) wäre etwa 6,7 %, also zu viel. Runde auf.",
                    60 to "μ + 2σ = 60 gehört zum zweiseitigen 95-%-Bereich. Einseitig nimmt man 1,64 · σ.", 55 to "55 ist μ + σ. Das passiert auch bei p = 0,5 noch oft (rund 18 %).")) }),
    )

    // ───────────────────────── Zwei Prüfungsaufgaben im Abi-Stil ─────────────────────────
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)
    private val m = p(3, 3, 2); private val n = p(1, 2, 2); private val touch = p(4, 5, 4); private val mirror = p(5, 7, 6)
    private fun wall() = Stage.plane("E", n, 22.0, "violet", "E")
    private fun center() = Stage.point("M", m, "white", "M(3|3|2)", drop = false)
    private val dome = Exam("planetarium", "Planetarium", "🌐", "Im Planetarium hängt eine Lichtkugel mit Mittelpunkt M(3|3|2) vor der Glaswand E: x₁ + 2x₂ + 2x₃ = 22.", listOf(
        ExamPart("a", 2, Families.scalar("abi-planetarium-a", 0, "Abstand zur Wand", "Wie weit ist der Mittelpunkt M von der Glaswand entfernt?", "d", 0..9, 3, 0,
            tip = "Abstand Punkt–Ebene: |n⃗ · M − c| : |n⃗| mit n⃗ = (1|2|2).", way = listOf("n⃗ · M = 3 + 6 + 4 = 13", "|13 − 22| = 9, |n⃗| = 3", "d = 3"),
            formula = { a -> listOf(listOf(Txt("d(M, E) = "), Num(Geo.num(a), "a"))) },
            objects = { _, ph -> listOf(wall(), center()) + if (ph == Phase.DONE) listOf(Stage.line("lot", m, touch - m, 0.0, 1.0, "pink")) else listOf(Stage.stub("richtung", m, n, "pink", length = 0.9)) },
            check = { a -> Families.compare(a, 3, "Der Abstand", "3 Einheiten bis zur Wand.", mapOf(9 to "9 ist |n⃗ · M − c|. Teile noch durch die Länge des Normalenvektors: 3.")) })),
        ExamPart("b", 3, Families.point("abi-planetarium-b", 0, "Berührpunkt", "Die Kugel bekommt den Radius 3 und berührt die Wand. In welchem Punkt?", touch, hidden = true, name = "B",
            tip = "Der Berührpunkt ist der Lotfußpunkt von M: von M aus in Richtung n⃗ gehen, bis die Ebenengleichung stimmt.",
            way = listOf("x⃗ = (3|3|2) + t · (1|2|2)", "13 + 9t = 22 → t = 1", "B(4|5|4)"),
            extras = { listOf(wall(), center(), Stage.sphere("K", m, 3.0, "cyan")) })),
        ExamPart("c", 3, Families.point("abi-planetarium-c", 0, "Spiegelbild", "Die Wand spiegelt. Wo sieht man das Spiegelbild M′ des Mittelpunkts?", mirror, hidden = true, name = "M′",
            mistakes = listOf(touch to "Das ist der Berührpunkt B auf der Wand. Das Spiegelbild liegt genauso weit dahinter."),
            tip = "Vom Mittelpunkt zum Lotfußpunkt B und denselben Weg noch einmal: M′ = 2 · B − M.",
            way = listOf("B(4|5|4) ist der Lotfußpunkt", "M′ = 2 · B − M = (8|10|8) − (3|3|2)", "M′(5|7|6)"),
            extras = { ph -> listOf(wall(), center()) + if (ph == Phase.DONE) listOf(Stage.line("lot", m, mirror - m, 0.0, 1.0, "pink", dashed = true)) else emptyList() })),
        ExamPart("d", 4, Families.scalar("abi-planetarium-d", 0, "Schnittkreis", "Jetzt wird die Kugel auf r = 5 aufgeblasen und ragt durch die Wand. Welchen Radius hat der Schnittkreis?", "r′", 0..6, 4, 0,
            tip = "Pythagoras: r′² = r² − d² mit r = 5 und dem Abstand d = 3 aus Teil a.", way = listOf("r′² = 25 − 9 = 16", "r′ = 4"),
            formula = { a -> listOf(listOf(Txt("r = 5, d = 3")), listOf(Txt("r′ = "), Num(Geo.num(a), "a"))) },
            objects = { a, _ -> listOf(wall(), center(), Stage.sphere("K", m, 5.0, "cyan")) + if (a > 0) listOf(Stage.circle("kreis", touch, n, a.toDouble(), "yellow")) else emptyList() },
            check = { a -> Families.compare(a, 4, "Der Radius", "r′ = 4.", mapOf(2 to "5 − 3 = 2 reicht nicht: Hier gilt der Satz des Pythagoras, r′² = 25 − 9.")) })),
    ))

    private val gate = Graph(0.0, 4.0, 1.6, 0.8, 0.38, 0.8) { -3 * it * it + 12 * it }
    private fun gatePart(label: String, pts: Int, id: String, title: String, mission: String, name: String, range: IntRange, solution: Int, start: Int, what: String, tip: String, way: List<String>,
                         mistakes: Map<Int, String> = emptyMap(), unit: String = "", objects: (Int, Phase) -> List<Obj>) =
        ExamPart(label, pts, Families.scalar(id, 0, title, mission, name, range, solution, start, tip, way, "yellow", suffix = unit,
            formula = { a -> listOf(listOf(Txt("r(t) = −3t² + 12t")), listOf(Txt("$name = "), Num(Geo.num(a) + unit, "a"))) },
            objects = objects, check = { a -> Families.compare(a, solution, what, "Richtig: ${Geo.num(solution)}$unit.", mistakes) }))
    private val entry = Exam("einlass", "Einlass", "🎟️", "Am Festival-Einlass kommen r(t) = −3t² + 12t Hundert Gäste pro Stunde (t in Stunden nach Öffnung, 0 ≤ t ≤ 4).", listOf(
        gatePart("a", 2, "abi-einlass-a", "Größter Andrang", "Zu welchem Zeitpunkt t kommen die meisten Gäste pro Stunde?", "t", 0..4, 2, 0, "Der Zeitpunkt",
            "Maximum der Rate: r′(t) = 0 setzen.", listOf("r′(t) = −6t + 12", "r′(t) = 0 → t = 2", "r″(2) = −6 < 0 → Maximum"), unit = " h") { a, _ ->
            listOf(gate.curve(), gate.axis(), Stage.point("stelle", gate.at(a.toDouble()), "yellow", drop = false)) },
        gatePart("b", 3, "abi-einlass-b", "Spitzenwert", "Wie viele Hundert Gäste pro Stunde sind das dann?", "r", 0..14, 12, 0, "Der Wert",
            "Die Stelle t = 2 in r einsetzen.", listOf("r(2) = −3 · 4 + 12 · 2", "= −12 + 24", "= 12 Hundert Gäste pro Stunde"),
            mapOf(2 to "t = 2 ist der Zeitpunkt. Gefragt ist der Wert der Rate dort: r(2).")) { _, _ ->
            listOf(gate.curve(), gate.axis(), Stage.point("stelle", gate.at(2.0), "yellow", drop = false)) },
        gatePart("c", 3, "abi-einlass-c", "Der Strom versiegt", "Nach wie vielen Stunden kommt niemand mehr?", "t", 0..6, 4, 1, "Der Zeitpunkt",
            "Nullstelle der Rate: −3t² + 12t = 0, ausklammern.", listOf("−3t · (t − 4) = 0", "t = 0 (Öffnung) oder t = 4", "nach 4 Stunden"),
            mapOf(0 to "t = 0 ist die Öffnung – da geht es erst los. Gesucht ist die andere Nullstelle."), unit = " h") { a, _ ->
            listOf(gate.curve(), gate.axis(), Stage.point("stelle", gate.at(a.toDouble().coerceAtMost(4.0), 0.0), "yellow", drop = false)) },
        gatePart("d", 4, "abi-einlass-d", "Alle Gäste", "Wie viele Hundert Gäste kommen insgesamt in den vier Stunden?", "N", 0..40, 32, 20, "Die Anzahl",
            "Gesamtzahl = Integral der Rate von 0 bis 4. Stammfunktion: R(t) = −t³ + 6t².", listOf("N = ∫₀⁴ (−3t² + 12t) dt = [−t³ + 6t²]₀⁴", "= −64 + 96", "= 32 Hundert = 3200 Gäste"),
            mapOf(12 to "12 ist der Spitzenwert der Rate. Gesucht ist die Gesamtzahl – das Integral.")) { _, _ ->
            listOf(gate.area("fläche", 0.0, 4.0), gate.curve(), gate.axis()) },
    ))
    val exams = listOf(dome, entry)

    val chapters = listOf(
        Chapter(22, "Integrale & Flächen", "Stammfunktion, Fläche, zwischen zwei Kurven, Bestand aus Änderungsrate", "Lichthof", "🌊", "yellow", listOf(
            "Die Ableitung zerlegt, das Integral sammelt ein: Aus vielen schmalen Streifen wird eine Fläche.",
            "Hauptsatz: Stammfunktion suchen, obere Grenze einsetzen, untere abziehen.",
            "Achtung Vorzeichen: Was unter der x-Achse liegt, zählt das Integral negativ."), ch22, Area.ANALYSIS),
        Chapter(23, "Erwartung, Sigma, Testen", "fairer Einsatz, Bernoulli-Kette, Mindestanzahl, Sigma-Regel, Hypothesentest", "Lostrommel-Finale", "🎯", "pink", listOf(
            "Zufall lässt sich nicht vorhersagen – aber auf Dauer ziemlich genau berechnen.",
            "Erwartungswert μ sagt, was im Schnitt passiert. Die Standardabweichung σ sagt, wie stark es schwankt.",
            "Beim Testen fragst du: Passt das Ergebnis noch zum Zufall – oder stimmt die Behauptung nicht?"), ch23, Area.STOCHASTIK),
    )
}
