package de.raumlicht.core.model

import de.raumlicht.core.model.Families.col
import de.raumlicht.core.model.Part.Num
import de.raumlicht.core.model.Part.Txt

/** Alle Gigs: sechs Kapitel analytische Geometrie, vom Punkt im Raum bis zur Spiegelung an der Ebene. */
object Levels {
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)
    private val FLOOR = p(0, 0, 1)
    private fun lineText(name: String, a: V3, d: V3, param: String = "t") = listOf(Txt("$name: x⃗ = "), col(a), Txt(" + $param · "), col(d))
    private fun loop(vararg pts: V3) = pts.indices.map { pts[it] to pts[(it + 1) % pts.size] }

    // ───────────────────────── 1 · Punkte im Raum ─────────────────────────
    private val ch1 = listOf(
        Families.point("1-1", 1, "Bodenmarke", "Soundcheck! Schick mich zur leuchtenden Marke am Boden. Lies ihre Koordinaten am Raster ab.",
            p(4, 2, 0), hidden = false,
            tip = "Erst x₁ (nach vorn), dann x₂ (nach rechts). Die Marke liegt am Boden – also x₃ = 0.",
            way = listOf("4 Schritte auf der x₁-Achse nach vorn", "2 Schritte parallel zur x₂-Achse nach rechts", "Höhe 0 → P(4|2|0)")),
        Families.point("1-2", 1, "Abheben", "Jetzt hängt die Marke in der Luft. Die gestrichelten Linien zeigen den Weg: vor, rüber, hoch.",
            p(2, 5, 3), hidden = false,
            tip = "Folge den gestrichelten Linien: x₁ vor, x₂ nach rechts, dann x₃ nach oben.",
            way = listOf("x₁ = 2 (nach vorn)", "x₂ = 5 (nach rechts)", "x₃ = 3 (nach oben) → P(2|5|3)")),
        Families.point("1-3", 1, "Hinter der Bühne", "Die Bühne geht auch nach hinten und nach links weiter – dort sind die Koordinaten negativ.",
            p(-3, 4, 2), hidden = false, bounds = Bounds(listOf(-4, -4, 0), listOf(5, 5, 6)),
            tip = "Die Marke liegt hinter dem Ursprung: x₁ ist negativ.",
            way = listOf("x₁ = −3 (3 Schritte nach hinten)", "x₂ = 4, x₃ = 2 → P(−3|4|2)")),
        Families.point("1-4", 1, "Halbe Strecke", "Zwischen A und B hängt ein Kabel. Ich soll genau in die Mitte – die Marke siehst du diesmal nicht. Rechne!",
            p(4, 2, 2), hidden = true, name = "M",
            given = listOf(listOf(Txt("A(2|0|4)   B(6|4|0)"))),
            tip = "Mittelpunkt: jede Koordinate einzeln mitteln, z. B. x₁ = (2 + 6) : 2.",
            way = listOf("M = ½ · (A + B)", "x₁ = (2+6):2 = 4, x₂ = (0+4):2 = 2, x₃ = (4+0):2 = 2", "M(4|2|2)"),
            extras = { listOf(Stage.point("A", p(2, 0, 4), "cyan", "A(2|0|4)"), Stage.point("B", p(6, 4, 0), "cyan", "B(6|4|0)"), Stage.edges("kabel", listOf(p(2, 0, 4) to p(6, 4, 0)), "cyan")) }),
        Families.point("1-5", 1, "Spiegelbild", "Die Glaswand ist die x₁x₃-Ebene. Flieg mich zum Spiegelbild von Q.",
            p(2, -3, 4), hidden = true, name = "Q′", bounds = Bounds(listOf(0, -5, 0), listOf(6, 5, 6)),
            given = listOf(listOf(Txt("Q(2|3|4), gespiegelt an der x₁x₃-Ebene"))),
            tip = "Beim Spiegeln an der x₁x₃-Ebene wechselt nur x₂ das Vorzeichen.",
            way = listOf("x₁x₃-Ebene: dort ist x₂ = 0", "Spiegeln ändert nur das Vorzeichen von x₂", "Q′(2|−3|4)"),
            extras = { listOf(Stage.pane("wand", listOf(p(0, 0, 0), p(6, 0, 0), p(6, 0, 6), p(0, 0, 6)), "violet", "x₁x₃-Ebene"), Stage.point("Q", p(2, 3, 4), "cyan", "Q(2|3|4)", guides = true)) }),
        run {
            val base = arrayOf(p(1, 1, 0), p(5, 1, 0), p(5, 5, 0), p(1, 5, 0)); val s = p(3, 3, 5)
            Families.point("1-6", 1, "Abi-Check: Pyramide", "Eine quadratische Pyramide ABCDS ist 5 hoch. Die Spitze S liegt senkrecht über der Mitte der Grundfläche. Setz den Spot auf S.",
                s, hidden = true, name = "S", abi = true,
                given = listOf(listOf(Txt("A(1|1|0)  B(5|1|0)  C(5|5|0)  D(1|5|0)"))),
                tip = "Mitte der Grundfläche = Mittelpunkt der Diagonale AC. Dann 5 nach oben.",
                way = listOf("Mittelpunkt von AC: ((1+5):2 | (1+5):2 | 0) = (3|3|0)", "Senkrecht darüber, Höhe 5", "S(3|3|5)"),
                extras = { ph ->
                    listOf(Stage.edges("grund", loop(*base), "cyan")) + "ABCD".mapIndexed { i, c -> Stage.point("e$c", base[i], "cyan", c.toString(), drop = false) } +
                        if (ph == Phase.DONE) listOf(Stage.edges("seiten", base.map { it to s }, "yellow", faces = base.indices.map { listOf(base[it], base[(it + 1) % 4], s) })) else emptyList()
                })
        },
    )

    // ───────────────────────── 2 · Vektoren ─────────────────────────
    private val ch2 = listOf(
        Families.vector("2-1", 2, "Erste Verschiebung", "Ein Vektor sagt: so weit vor, so weit rüber, so weit hoch. Bring mich von A nach B.",
            p(1, 1, 0), p(4, 5, 0),
            tip = "Spitze minus Fuß: B − A, Zeile für Zeile.",
            way = listOf("v⃗ = B − A", "(4−1 | 5−1 | 0−0)", "v⃗ = (3|4|0)")),
        Families.vector("2-2", 2, "Quer durch den Raum", "Diesmal geht es auch zurück und nach oben. Achte auf die Vorzeichen.",
            p(2, 6, 1), p(6, 2, 4),
            tip = "B − A: Wird eine Koordinate kleiner, ist die Komponente negativ.",
            way = listOf("v⃗ = B − A", "(6−2 | 2−6 | 4−1)", "v⃗ = (4|−4|3)")),
        run {
            val o = p(1, 1, 1); val u = p(3, 1, 2)
            Families.vector("2-3", 2, "Zwei Etappen", "Die erste Etappe u⃗ bin ich schon geflogen. Welcher Vektor w⃗ bringt mich jetzt ans Ziel?",
                o + u, p(2, 7, 4), name = "w⃗",
                given = listOf(listOf(Txt("Start O(1|1|1),  u⃗ = "), col(u))),
                tip = "Erst ausrechnen, wo ich nach u⃗ bin: O + u⃗. Von dort zum Ziel: B − A.",
                way = listOf("A = O + u⃗ = (4|2|3)", "w⃗ = B − A = (2−4 | 7−2 | 4−3)", "w⃗ = (−2|5|1)"),
                extras = { listOf(Stage.point("O", o, "violet", "O(1|1|1)"), Stage.vector("u", o, o + u, "violet", "u⃗")) })
        },
        Families.param("2-4", 2, "Dreimal dasselbe", "Der Vektor v⃗ ist fest. Wie oft muss ich ihn hintereinander fliegen, um bei Z zu landen?",
            p(1, 1, 0), p(1, 2, 1), 3, 0..5, live = true, symbol = "r", vectorName = "v⃗", prefix = "OZ⃗ = ", lineLabel = null,
            tip = "Vergleiche eine Zeile: 1 + r · 1 = 4.",
            way = listOf("A + r · v⃗ = Z", "x₁: 1 + r·1 = 4 → r = 3", "Probe x₂: 1 + 3·2 = 7 ✓, x₃: 0 + 3·1 = 3 ✓")),
        run {
            val a = p(1, 1, 1); val b = p(5, 2, 1); val c = p(6, 6, 3); val d = p(2, 5, 3)
            Families.point("2-5", 2, "Parallelogramm", "Drei Ecken eines Lichtsegels hängen schon. Wo muss die vierte Ecke D hin, damit ABCD ein Parallelogramm wird?",
                d, hidden = true, name = "D",
                given = listOf(listOf(Txt("A(1|1|1)  B(5|2|1)  C(6|6|3)"))),
                tip = "Im Parallelogramm gilt: von A nach D ist derselbe Vektor wie von B nach C.",
                way = listOf("BC⃗ = C − B = (1|4|2)", "D = A + BC⃗ = (1+1 | 1+4 | 1+2)", "D(2|5|3)"),
                extras = { ph ->
                    listOf(Stage.point("A", a, "cyan", "A"), Stage.point("B", b, "cyan", "B"), Stage.point("C", c, "cyan", "C"),
                        if (ph == Phase.DONE) Stage.edges("segel", loop(a, b, c, d), "cyan", faces = listOf(listOf(a, b, c, d))) else Stage.edges("segel", listOf(a to b, b to c), "cyan"))
                })
        },
        run {
            val a = p(1, 2, 0); val b = p(7, 1, 3); val c = p(4, 6, 3)
            Families.point("2-6", 2, "Abi-Check: Schwerpunkt", "Drei Drohnen tragen ein dreieckiges Segel ABC. Der Spot gehört in den Schwerpunkt S.",
                p(4, 3, 2), hidden = true, name = "S", abi = true,
                given = listOf(listOf(Txt("A(1|2|0)  B(7|1|3)  C(4|6|3)")), listOf(Txt("OS⃗ = ⅓ · (OA⃗ + OB⃗ + OC⃗)"))),
                tip = "Alle drei x₁-Werte addieren und durch 3 teilen. Dann x₂, dann x₃.",
                way = listOf("x₁ = (1+7+4):3 = 4", "x₂ = (2+1+6):3 = 3,  x₃ = (0+3+3):3 = 2", "S(4|3|2)"),
                extras = { listOf(Stage.point("A", a, "cyan", "A"), Stage.point("B", b, "cyan", "B"), Stage.point("C", c, "cyan", "C"), Stage.edges("segel", loop(a, b, c), "cyan", faces = listOf(listOf(a, b, c)))) })
        },
    )

    // ───────────────────────── 3 · Geraden ─────────────────────────
    private val ch3 = listOf(
        Families.aim("3-1", 3, "Erster Schuss", "Ein Laser ist eine Gerade: Stützpunkt plus Richtung. Der Laser steht im Ursprung – gib ihm die Richtung zum Ring.",
            p(0, 0, 0), listOf(p(2, 4, 2)),
            tip = "Richtungsvektor = Ziel − Stützpunkt. Jedes Vielfache davon trifft auch, z. B. (1|2|1).",
            way = listOf("d⃗ = Z − A = (2|4|2)", "kürzer: d⃗ = (1|2|1)", "g: x⃗ = (0|0|0) + t · (1|2|1)")),
        Families.aim("3-2", 3, "Stützvektor", "Jetzt steht der Laser irgendwo im Raum. Die Richtung ist trotzdem Ziel minus Start.",
            p(6, 1, 1), listOf(p(2, 5, 4)),
            tip = "d⃗ = Z − A = (2−6 | 5−1 | 4−1).",
            way = listOf("d⃗ = Z − A", "(2−6 | 5−1 | 4−1)", "d⃗ = (−4|4|3)")),
        Families.param("3-3", 3, "Parameter t", "Die Gerade steht. Für welches t lande ich genau im Ring? Diesmal ohne Vorschau.",
            p(1, 0, 1), p(1, 2, 1), 3, -4..6,
            tip = "Eine Zeile reicht: 1 + t · 1 = 4. Danach die anderen Zeilen als Probe.",
            way = listOf("x₁: 1 + t = 4 → t = 3", "Probe x₂: 0 + 3·2 = 6 ✓", "Probe x₃: 1 + 3·1 = 4 ✓")),
        Families.param("3-4", 3, "Rückwärtsgang", "Der Ring hängt hinter dem Stützpunkt. Geht das überhaupt? Klar – mit dem richtigen t.",
            p(6, 5, 4), p(1, 1, 1), -3, -5..5, start = 1,
            tip = "t darf negativ sein: dann läuft man gegen die Richtung von d⃗.",
            way = listOf("x₁: 6 + t = 3 → t = −3", "Probe x₂: 5 − 3 = 2 ✓", "Probe x₃: 4 − 3 = 1 ✓")),
        Families.aim("3-5", 3, "Zwei Ringe", "Ein Strahl, zwei Ringe. Das klappt nur, wenn beide auf derselben Geraden liegen – finde die Richtung.",
            p(1, 1, 1), listOf(p(3, 4, 2), p(5, 7, 3)), solution = p(2, 3, 1),
            tip = "Richtung zum ersten Ring: Z₁ − A. Prüfe: Kommt man mit t = 2 beim zweiten Ring an?",
            way = listOf("d⃗ = Z₁ − A = (2|3|1)", "Punktprobe Z₂: A + 2 · d⃗ = (5|7|3) ✓", "Beide Ringe liegen auf g")),
        run {
            val a = p(2, 1, 6); val d = p(1, 2, -2); val s = p(5, 7, 0)
            Families.sensor("3-6", 3, "Abi-Check: Spurpunkt", "Der Laser hängt an der Decke und zeigt schräg nach unten. Wo trifft er den Boden? Stell den Sensor genau dorthin.",
                listOf(Ray("g", a, d, "cyan", s)), s,
                listOf(Check("liegt auf g") { Geo.onLine(it, a, d) }, Check("liegt am Boden (x₃ = 0)") { it.z == 0.0 }),
                given = listOf(lineText("g", a, d), listOf(Txt("Boden: x₃ = 0"))), abi = true,
                tip = "Am Boden ist x₃ = 0. Setz die dritte Zeile gleich null: 6 − 2t = 0.",
                way = listOf("x₃ = 0: 6 − 2t = 0 → t = 3", "S = (2|1|6) + 3 · (1|2|−2)", "S(5|7|0)"))
        },
    )

    // ───────────────────────── 4 · Lagebeziehungen ─────────────────────────
    private fun crossing(id: String, title: String, mission: String, a: V3, d1: V3, b: V3, d2: V3, tip: String, way: List<String>, abi: Boolean = false): Level {
        val s = Geo.lineLine(a, d1, b, d2).point!!
        return Families.sensor(id, 4, title, mission, listOf(Ray("g", a, d1, "cyan", s), Ray("h", b, d2, "yellow", s)), s,
            listOf(Check("liegt auf g") { Geo.onLine(it, a, d1) }, Check("liegt auf h") { Geo.onLine(it, b, d2) }),
            given = listOf(lineText("g", a, d1, "s"), lineText("h", b, d2)), tip = tip, way = way, abi = abi)
    }
    private val skewA = p(0, 6, 1); private val skewD = p(2, -1, 1); private val skewB = p(7, 1, 0)
    private val ch4 = listOf(
        crossing("4-1", "Kreuzung", "Zwei Laser, ein Treffpunkt. Wo kreuzen sich g und h? Dort gehört der Sensor hin.",
            p(1, 1, 1), p(2, 1, 1), p(7, 1, 1), p(-1, 1, 1),
            tip = "Gleichsetzen. Zeilen 2 und 3 liefern s = t, das in Zeile 1 einsetzen.",
            way = listOf("x₂: 1 + s = 1 + t → s = t", "x₁: 1 + 2s = 7 − s → s = 2", "S = (1|1|1) + 2 · (2|1|1) = (5|3|3)")),
        crossing("4-2", "Gegenlicht", "Noch eine Kreuzung – diesmal mit krummeren Zahlen. Erst rechnen, dann schießen.",
            skewA, skewD, skewB, p(-1, 1, 1),
            tip = "Zeile 2 und 3: 6 − s = 1 + t und 1 + s = t. Addiere beide Gleichungen.",
            way = listOf("x₃: t = 1 + s", "x₂: 6 − s = 1 + (1 + s) → s = 2, t = 3", "Probe x₁: 0 + 4 = 7 − 3 ✓ → S(4|4|3)")),
        Families.aim("4-3", 4, "Parallel", "Der zweite Laser h soll exakt parallel zu g laufen. Dann trifft er den Ring.",
            p(4, 0, 1), listOf(p(8, 6, 3)), solution = p(2, 3, 1), line = "h",
            given = listOf(lineText("g", p(1, 1, 2), p(2, 3, 1), "s")),
            tip = "Parallele Geraden haben denselben Richtungsvektor (oder ein Vielfaches).",
            way = listOf("g hat die Richtung (2|3|1)", "h ∥ g → gleiche Richtung", "Probe: (4|0|1) + 2 · (2|3|1) = (8|6|3) ✓"),
            extras = { listOf(Stage.fullLine("g", p(1, 1, 2), p(2, 3, 1), Bounds(), "pink", "g")) }),
        Level("4-4", 4, "Windschief?", "Im Moment laufen die Strahlen aneinander vorbei – windschief. Stell die fehlende Komponente c so ein, dass sie sich treffen.",
            Bounds(), listOf(Control("c", "c", ControlKind.SCALAR, listOf(0), listOf(-3), listOf(3), "yellow")), mapOf("c" to listOf(1)),
            tip = "Aus Zeile 1 und 2 folgen s und t. Setz beide in Zeile 3 ein und löse nach c.",
            way = listOf("x₁, x₂: 2s = 7 − t und 6 − s = 1 + t → s = 2, t = 3", "x₃: 1 + 2 = 0 + 3c", "c = 1"),
            formula = { v -> listOf(lineText("g", skewA, skewD, "s"), listOf(Txt("h: x⃗ = "), col(skewB), Txt(" + t · "), Part.Col(listOf("−1", "1", Geo.num(v.int("c"))), "c"))) },
            objects = { v, ph ->
                val d2 = V3(-1.0, 1.0, v.int("c").toDouble())
                listOf(Stage.drone(p(8, 0, 1), if (ph == Phase.DONE) "happy" else "idle"), Stage.point("qa", skewA, "cyan"), Stage.point("qb", skewB, "yellow"),
                    Stage.fullLine("g", skewA, skewD, Bounds(), "cyan", "g"), Stage.fullLine("h", skewB, d2, Bounds(), "yellow", "h")) +
                    if (ph == Phase.DONE) listOf(Stage.point("S", p(4, 4, 3), "pink", "S(4|4|3)", "target")) else emptyList()
            },
            evaluate = { v ->
                val b = Bounds(); val d2 = V3(-1.0, 1.0, v.int("c").toDouble())
                val r = Geo.lineLine(skewA, skewD, skewB, d2)
                if (r.relation == Relation.INTERSECT) Outcome(true, "Sie treffen sich in ${r.point!!.fmt()} – nicht mehr windschief!", listOf(Beam(skewA, r.point, "cyan", true), Beam(skewB, r.point, "yellow", true)))
                else Outcome(false, "Immer noch windschief: Die Strahlen verfehlen sich um ${Geo.num(r.distance)} Einheiten.",
                    listOf(Beam(skewA, skewA + skewD * Geo.exit(skewA, skewD, b.outerLo, b.outerHi), "cyan", false), Beam(skewB, skewB + d2 * Geo.exit(skewB, d2, b.outerLo, b.outerHi), "yellow", false)))
            }),
        crossing("4-5", "Abi-Check: Flugbahnen", "Zwei Show-Drohnen fliegen geradlinig auf den Bahnen g und h. Kreuzen sich die Bahnen? Markiere den Schnittpunkt.",
            p(0, 0, 2), p(3, 4, 1), p(7, 0, 1), p(-2, 2, 1), abi = true,
            tip = "Gleichsetzen: 3s = 7 − 2t, 4s = 2t, 2 + s = 1 + t. Aus Zeile 2: t = 2s.",
            way = listOf("x₂: 4s = 2t → t = 2s", "x₁: 3s = 7 − 4s → s = 1, t = 2", "Probe x₃: 2 + 1 = 1 + 2 ✓ → S(3|4|3)")),
    )

    // ───────────────────────── 5 · Ebenen ─────────────────────────
    private fun pierce(id: String, title: String, mission: String, a: V3, d: V3, n: V3, rhs: Int, planeText: String, tip: String, way: List<String>,
                       abi: Boolean = false, extras: List<Obj> = emptyList()): Level {
        val s = a + d * Geo.linePlane(a, d, n, rhs.toDouble())!!
        return Families.sensor(id, 5, title, mission, listOf(Ray("g", a, d, "cyan", s)), s,
            listOf(Check("liegt auf g") { Geo.onLine(it, a, d) }, Check("liegt in E") { Geo.onPlane(it, n, rhs.toDouble()) }),
            given = listOf(lineText("g", a, d), listOf(Txt("E: $planeText"))), tip = tip, way = way, abi = abi,
            extras = { extras + Stage.plane("E", n, rhs.toDouble(), "violet", "E") })
    }
    private val ch5 = listOf(
        pierce("5-1", "Glasdecke", "Über der Bühne hängt eine Glasscheibe E. Wo stößt der Laser durch? Das ist der Durchstoßpunkt.",
            p(1, 1, 0), p(1, 2, 2), FLOOR, 4, "x₃ = 4",
            tip = "In E gilt x₃ = 4. Dritte Zeile der Geraden: 0 + 2t = 4.",
            way = listOf("x₃ = 4: 2t = 4 → t = 2", "S = (1|1|0) + 2 · (1|2|2)", "S(3|5|4)")),
        pierce("5-2", "Schräge Scheibe", "Jetzt steht die Scheibe schräg. Setz die Gerade in die Ebenengleichung ein.",
            p(1, 1, 1), p(1, 2, 0), p(1, 1, 1), 9, "x₁ + x₂ + x₃ = 9",
            tip = "Einsetzen: (1 + t) + (1 + 2t) + 1 = 9.",
            way = listOf("(1 + t) + (1 + 2t) + 1 = 9", "3 + 3t = 9 → t = 2", "S = (1|1|1) + 2 · (1|2|0) = (3|5|1)")),
        Families.plane("5-3", 5, "Achsenabschnitte", "Bau die Scheibe selbst: Sie soll die Achsen in A, B und C schneiden. Stell n⃗ und c ein – die Scheibe dreht sich mit.",
            listOf("A" to p(6, 0, 0), "B" to p(0, 3, 0), "C" to p(0, 0, 2)), p(1, 2, 3), 6,
            tip = "A einsetzen: n₁ · 6 = c. B: n₂ · 3 = c. C: n₃ · 2 = c. Wähle c = 6.",
            way = listOf("A: 6n₁ = c,  B: 3n₂ = c,  C: 2n₃ = c", "mit c = 6: n₁ = 1, n₂ = 2, n₃ = 3", "E: x₁ + 2x₂ + 3x₃ = 6")),
        run {
            val q = p(2, 2, 1); val u = p(2, 0, 1); val w = p(0, 2, 1); val m = q + u + w
            Families.aim("5-4", 5, "Normalenvektor", "Der Spot soll senkrecht auf die Leinwand treffen. Gesucht ist ein Vektor, der auf u⃗ und auf v⃗ senkrecht steht.",
                p(3, 3, 5), listOf(m), hidden = true, name = "n⃗", line = "Spot", solution = p(1, 1, -2),
                given = listOf(listOf(Txt("u⃗ = "), col(u), Txt("   v⃗ = "), col(w))),
                tip = "Kreuzprodukt u⃗ × v⃗ – oder zwei Gleichungen: n⃗ · u⃗ = 0 und n⃗ · v⃗ = 0. Der Spot hängt über der Leinwand, also zeigt n⃗ nach unten.",
                way = listOf("u⃗ × v⃗ = (0·1 − 1·2 | 1·0 − 2·1 | 2·2 − 0·0) = (−2|−2|4)", "gekürzt und nach unten gedreht: n⃗ = (1|1|−2)", "Probe: n⃗ · u⃗ = 2 − 2 = 0 ✓, n⃗ · v⃗ = 2 − 2 = 0 ✓"),
                extras = { listOf(Stage.pane("wand", listOf(q, q + u * 2, q + u * 2 + w * 2, q + w * 2), "violet", "Leinwand"), Stage.vector("u", q, q + u, "pink", "u⃗"), Stage.vector("v", q, q + w, "green", "v⃗")) })
        },
        Families.plane("5-5", 5, "Drei Punkte", "Drei Drohnen spannen eine Lichtfläche auf. Finde ihre Koordinatengleichung.",
            listOf("A" to p(3, 2, 0), "B" to p(1, 2, 2), "C" to p(0, 4, 2)), p(2, 1, 2), 8,
            tip = "Normalenvektor = AB⃗ × AC⃗. Danach A einsetzen, um c zu bekommen.",
            way = listOf("AB⃗ = (−2|0|2), AC⃗ = (−3|2|2)", "AB⃗ × AC⃗ = (−4|−2|−4) → n⃗ = (2|1|2)", "A einsetzen: 2·3 + 2 + 0 = 8 → E: 2x₁ + x₂ + 2x₃ = 8")),
        pierce("5-6", "Abi-Check: Schatten", "Ein Scheinwerfer strahlt parallel in Richtung v⃗. Wo landet der Schatten der Mastspitze T auf der LED-Wand E?",
            p(2, 2, 5), p(1, 2, -1), p(0, 1, 0), 6, "x₂ = 6", abi = true,
            tip = "Schattengerade durch T mit Richtung v⃗. In E ist x₂ = 6: 2 + 2t = 6.",
            way = listOf("g: x⃗ = (2|2|5) + t · (1|2|−1)", "x₂ = 6: 2 + 2t = 6 → t = 2", "Schattenpunkt S(4|6|3)"),
            extras = listOf(Stage.edges("mast", listOf(p(2, 2, 0) to p(2, 2, 5)), "white"), Stage.point("T", p(2, 2, 5), "white", "T(2|2|5)", drop = false))),
    )

    // ───────────────────────── 6 · Skalarprodukt, Abstand, Spiegelung ─────────────────────────
    private val ch6 = run {
        val o = p(4, 2, 1); val a = p(2, 1, 2)
        val distP = p(1, 2, 1); val distN = p(1, 2, 2)
        val mirA = p(4, 1, 1); val mirZ = p(2, 7, 4); val mirS = p(0, 5, 3)
        listOf(
            Level("6-1", 6, "Rechter Winkel", "Zwei Traversen sollen genau rechtwinklig stehen. Stell die fehlende Komponente c ein.",
                Bounds(), listOf(Control("c", "c", ControlKind.SCALAR, listOf(0), listOf(-2), listOf(5), "pink")), mapOf("c" to listOf(2)),
                tip = "Senkrecht heißt: Skalarprodukt = 0. Also 2 · (−2) + 1 · c + 2 · 1 = 0.",
                way = listOf("a⃗ · b⃗ = 2·(−2) + 1·c + 2·1", "−4 + c + 2 = 0", "c = 2"),
                formula = { v -> listOf(listOf(Txt("a⃗ = "), col(a), Txt("   b⃗ = "), Part.Col(listOf("−2", Geo.num(v.int("c")), "1"), "c")), listOf(Txt("a⃗ ⊥ b⃗  ⇔  a⃗ · b⃗ = 0"))) },
                objects = { v, ph ->
                    val b = V3(-2.0, v.int("c").toDouble(), 1.0)
                    listOf(Stage.drone(p(8, 0, 1), if (ph == Phase.DONE) "happy" else "idle"), Stage.point("O", o, "white"), Stage.vector("a", o, o + a, "cyan", "a⃗"), Stage.vector("b", o, o + b, "pink", "b⃗"))
                },
                evaluate = { v ->
                    val dot = a dot V3(-2.0, v.int("c").toDouble(), 1.0)
                    if (dot == 0.0) Outcome(true, "a⃗ · b⃗ = 0 – perfekter rechter Winkel!")
                    else Outcome(false, "a⃗ · b⃗ = ${Geo.num(dot)}. Für einen rechten Winkel muss 0 herauskommen.")
                }),
            Families.point("6-2", 6, "Kürzester Weg", "Von P soll ein Lichtstrahl auf kürzestem Weg zur Scheibe E. Wo trifft er auf? Das ist der Lotfußpunkt.",
                p(2, 2, 2), hidden = true, name = "F",
                given = listOf(listOf(Txt("P(4|4|4)   E: x₁ + x₂ + x₃ = 6"))),
                tip = "Lotgerade durch P mit dem Normalenvektor (1|1|1) als Richtung, dann in E einsetzen.",
                way = listOf("Lotgerade: x⃗ = (4|4|4) + t · (1|1|1)", "in E: (4+t) + (4+t) + (4+t) = 6 → t = −2", "F(2|2|2)"),
                extras = { ph -> listOf(Stage.plane("E", p(1, 1, 1), 6.0, "violet", "E"), Stage.point("P", p(4, 4, 4), "cyan", "P(4|4|4)")) +
                    if (ph == Phase.DONE) listOf(Stage.line("lot", p(4, 4, 4), p(-2, -2, -2), 0.0, 1.0, "yellow")) else emptyList() }),
            Families.point("6-3", 6, "Hinter dem Spiegel", "E ist jetzt ein Spiegel. Wo erscheint das Spiegelbild P′ von P?",
                p(6, 6, 3), hidden = true, name = "P′",
                given = listOf(listOf(Txt("P(2|2|3)   E: x₁ + x₂ = 8"))),
                tip = "Erst den Lotfußpunkt F bestimmen, dann denselben Weg noch einmal gehen: P′ = P + 2 · PF⃗.",
                way = listOf("Lotgerade: (2|2|3) + t · (1|1|0), in E: (2+t) + (2+t) = 8 → t = 2", "F(4|4|3)", "P′ = P + 2 · PF⃗ = (6|6|3)"),
                extras = { ph -> listOf(Stage.plane("E", p(1, 1, 0), 8.0, "violet", "E"), Stage.point("P", p(2, 2, 3), "cyan", "P(2|2|3)")) +
                    if (ph == Phase.DONE) listOf(Stage.line("lot", p(2, 2, 3), p(4, 4, 0), 0.0, 1.0, "yellow", dashed = true)) else emptyList() }),
            Level("6-4", 6, "Abstand", "Der Strahl läuft senkrecht auf die Scheibe zu. Wie lang muss er sein, damit er genau auf dem Glas endet?",
                Bounds(), listOf(Control("a", "d", ControlKind.SCALAR, listOf(1), listOf(0), listOf(8), "pink")), mapOf("a" to listOf(3)),
                tip = "Abstand Punkt–Ebene: |n⃗ · p⃗ − c| : |n⃗|. Hier ist |n⃗| = √(1 + 4 + 4) = 3.",
                way = listOf("n⃗ · p⃗ = 1·1 + 2·2 + 2·1 = 7", "|7 − 16| = 9,  |n⃗| = 3", "d = 9 : 3 = 3"),
                formula = { v -> listOf(listOf(Txt("P(1|2|1)   E: x₁ + 2x₂ + 2x₃ = 16")), listOf(Txt("d(P, E) = "), Num(Geo.num(v.int("a")), "a"))) },
                objects = { _, ph ->
                    listOf(Stage.drone(p(8, 0, 1), if (ph == Phase.DONE) "happy" else "idle"), Stage.plane("E", distN, 16.0, "violet", "E"), Stage.point("P", distP, "cyan", "P(1|2|1)"),
                        Stage.stub("richtung", distP, distN, "pink", length = 1.0)) +
                        if (ph == Phase.DONE) listOf(Stage.line("lot", distP, distN, 0.0, 1.0, "pink"), Stage.point("F", distP + distN, "pink", "F(2|4|3)")) else emptyList()
                },
                evaluate = { v ->
                    val len = v.int("a")
                    val end = distP + distN * (len / 3.0)
                    when {
                        len == 3 -> Outcome(true, "Genau 3 Einheiten – der Strahl endet auf dem Glas.", listOf(Beam(distP, end, "pink", true)))
                        len < 3 -> Outcome(false, "Zu kurz: Der Strahl endet vor der Scheibe.", listOf(Beam(distP, end, "pink", false)))
                        else -> Outcome(false, "Zu lang: Der Strahl schießt durch die Scheibe hindurch.", listOf(Beam(distP, end, "pink", false)))
                    }
                }),
            Families.sensor("6-5", 6, "Abi-Check: Über Bande", "Der Laser in A soll den Ring Z über den Spiegel treffen. Auf welchen Punkt des Spiegels musst du zielen?",
                emptyList(), mirS, listOf(Check("liegt auf dem Spiegel (x₁ = 0)") { it.x == 0.0 }),
                given = listOf(listOf(Txt("A(4|1|1)   Z(2|7|4)   Spiegel: x₁ = 0"))), abi = true,
                tip = "Spiegle Z am Spiegel: Z′(−2|7|4). Die Gerade von A nach Z′ durchstößt den Spiegel im gesuchten Punkt.",
                way = listOf("Z′(−2|7|4), g: x⃗ = (4|1|1) + t · (−6|6|3)", "x₁ = 0: 4 − 6t = 0 → t = ⅔", "S = (0|5|3)"),
                shots = { pt, ok ->
                    val d = pt - mirA
                    when {
                        ok -> listOf(Beam(mirA, pt, "cyan", false), Beam(pt, mirZ, "cyan", true))
                        pt.x == 0.0 && !d.isZero -> { val r = V3(-d.x, d.y, d.z); val b = Bounds(); listOf(Beam(mirA, pt, "cyan", false), Beam(pt, pt + r * Geo.exit(pt, r, b.outerLo, b.outerHi), "cyan", false)) }
                        else -> listOf(Beam(mirA, pt, "cyan", false))
                    }
                },
                extras = { listOf(Stage.pane("spiegel", listOf(p(0, 0, 0), p(0, 8, 0), p(0, 8, 6), p(0, 0, 6)), "violet", "Spiegel"), Stage.point("A", mirA, "cyan", "A(4|1|1)"), Stage.point("Z", mirZ, "yellow", "Z(2|7|4)", "target")) }),
        )
    }

    val chapters = listOf(
        Chapter(1, "Soundcheck", "Punkte im Raum", "Kellerclub", "🎚️", "cyan", listOf(
            "Hi, ich bin Lux – deine Lichtdrohne. Du sagst, wohin. Ich fliege.",
            "Jeder Ort auf der Bühne hat drei Koordinaten: x₁ nach vorn, x₂ nach rechts, x₃ nach oben.",
            "Dreh die Bühne mit dem Finger. Zieh den Griff oder tippe die Zahlen – dann „Los“."), ch1),
        Chapter(2, "Drohnen-Choreo", "Vektoren", "Dachterrasse", "🛸", "pink", listOf(
            "Ein Vektor ist eine Flugansage: so weit vor, so weit rüber, so weit hoch.",
            "Von A nach B? Immer Spitze minus Fuß: B − A.",
            "Wer rechnet statt rät, landet beim ersten Versuch – das gibt drei Sterne."), ch2),
        Chapter(3, "Erster Laser", "Geraden", "Club", "🔦", "green", listOf(
            "Ein Laserstrahl ist eine Gerade: ein Stützpunkt und eine Richtung.",
            "g: x⃗ = A + t · d⃗. Mit t wanderst du auf dem Strahl entlang.",
            "Du siehst nur den Anfang des Strahls. Den Rest verrät dir die Rechnung."), ch3),
        Chapter(4, "Strahlen kreuzen", "Lagebeziehungen", "Konzerthalle", "✨", "yellow", listOf(
            "Zwei Geraden im Raum können sich schneiden, parallel laufen – oder windschief aneinander vorbei.",
            "Schnittpunkt gesucht? Gleichsetzen und das Gleichungssystem lösen.",
            "Stimmt die dritte Zeile nicht, verfehlen sich die Strahlen."), ch4),
        Chapter(5, "Glas & Leinwand", "Ebenen", "Open Air", "🔷", "violet", listOf(
            "Scheiben und Leinwände sind Ebenen. Am schnellsten rechnet man mit der Koordinatenform.",
            "Der Normalenvektor n⃗ steht senkrecht auf der Ebene – seine Einträge sind die Vorfaktoren.",
            "Durchstoßpunkt: Gerade in die Ebenengleichung einsetzen, t ausrechnen, fertig."), ch5),
        Chapter(6, "Headliner", "Skalarprodukt · Abstand · Spiegelung", "Festival", "🏟️", "orange", listOf(
            "Die große Bühne. Hier zählt jeder Winkel.",
            "Skalarprodukt null heißt senkrecht. Damit findest du Lote, Abstände und Spiegelbilder.",
            "Das ist Abi-Niveau – und du bist bereit."), ch6),
    )
    val all = chapters.flatMap { it.levels }
    fun byId(id: String) = all.firstOrNull { it.id == id }
    fun next(id: String): Level? = all.getOrNull(all.indexOfFirst { it.id == id } + 1)
}
