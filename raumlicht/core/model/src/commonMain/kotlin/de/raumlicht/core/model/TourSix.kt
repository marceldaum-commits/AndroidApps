package de.raumlicht.core.model

import de.raumlicht.core.model.Families.col
import de.raumlicht.core.model.Part.Num
import de.raumlicht.core.model.Part.Txt

/** Kapitel 18–21 (seit 2.7.0): Ebenen in allen Formen, Schnitte und Schnittwinkel, Spiegeln und Lote, Kugel und Ebene. */
internal object TourSix {
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)
    private fun lineText(name: String, a: V3, d: V3, param: String = "t") = listOf(Txt("$name: x⃗ = "), col(a), Txt(" + $param · "), col(d))
    private fun loop(vararg pts: V3) = pts.indices.map { pts[it] to pts[(it + 1) % pts.size] }

    /** Winkel in 15°-Schritten einstellen */
    private fun angle(id: String, chapter: Int, title: String, mission: String, degrees: Int, formula: List<List<Part>>, tip: String, way: List<String>,
                      abi: Boolean = false, mistakes: Map<Int, String> = emptyMap(), objects: (Phase) -> List<Obj>) =
        Families.scalar(id, chapter, title, mission, "α", 0..6, degrees / 15, 0, tip, way, "yellow", step = 15, suffix = "°", abi = abi,
            formula = { a -> formula + listOf(listOf(Txt("α = "), Num("${a * 15}°", "a"))) },
            objects = { _, ph -> objects(ph) },
            check = { a -> Families.compare(a * 15, degrees, "Der Winkel", "Genau $degrees° – perfekt ausgerichtet!", mistakes) })

    // ───────────────────────── 18 · Ebenen in allen Formen ─────────────────────────
    private val ch18 = run {
        val a1 = p(2, 0, 0); val b1 = p(0, 3, 0); val c1 = p(0, 0, 1)
        val q2 = p(1, 2, 3); val n2 = p(2, 1, 2)
        val n3 = p(3, 4, 6); val s1 = p(4, 0, 0); val s2 = p(0, 3, 0); val s3 = p(0, 0, 2)
        val t1 = p(6, 0, 0); val t2 = p(0, 3, 0); val t3 = p(0, 0, 2)
        val n5 = p(2, -1, 2)
        val a6 = p(1, 0, 2); val d6 = p(1, 1, 0); val b6 = p(2, 1, 2); val q6 = p(3, 0, 1)
        listOf(
            Families.plane("18-1", 18, "Parameterform umwandeln", "Die Glasscheibe ist in Parameterform gegeben. Bring sie in die Koordinatenform.",
                listOf("A" to a1, "B" to b1, "C" to c1), p(3, 2, 6), 6,
                given = listOf(listOf(Txt("E: x⃗ = "), col(a1), Txt(" + r · "), col(b1 - a1), Txt(" + s · "), col(c1 - a1))),
                tip = "Der Normalenvektor steht auf beiden Spannvektoren senkrecht: n⃗ = (−2|3|0) × (−2|0|1). Dann A einsetzen, um c zu bekommen.",
                way = listOf("n⃗ = (−2|3|0) × (−2|0|1) = (3|2|6)", "A einsetzen: 3·2 + 2·0 + 6·0 = 6", "E: 3x₁ + 2x₂ + 6x₃ = 6")),
            Families.plane("18-2", 18, "Normalenform lesen", "Hier steht die Scheibe in Normalenform. Lies den Normalenvektor ab und rechne c aus.",
                listOf("P" to q2), n2, 10, normal = n2,
                normalHint = "P liegt drin – aber die Scheibe ist verdreht. Der Normalenvektor steht in der Normalenform hinter dem Malpunkt.",
                given = listOf(listOf(Txt("E: [x⃗ − "), col(q2), Txt("] · "), col(n2), Txt(" = 0"))),
                tip = "Ausmultiplizieren: n⃗ · x⃗ = n⃗ · P. Links stehen die Vorfaktoren 2, 1, 2 – rechts die Zahl n⃗ · P.",
                way = listOf("n⃗ = (2|1|2)", "c = n⃗ · P = 2·1 + 1·2 + 2·3 = 10", "E: 2x₁ + x₂ + 2x₃ = 10"),
                extras = { ph -> if (ph == Phase.DONE) listOf(Stage.vector("normal", q2, q2 + n2, "pink", "n⃗")) else emptyList() }),
            Families.point("18-3", 18, "Spurpunkt", "Wo durchstößt die Scheibe die x₁-Achse? Dort wird der erste Haken befestigt.",
                s1, hidden = true, name = "S₁",
                given = listOf(listOf(Txt("E: 3x₁ + 4x₂ + 6x₃ = 12"))),
                mistakes = listOf(p(3, 0, 0) to "3 ist der Vorfaktor von x₁. Auf der x₁-Achse gilt x₂ = x₃ = 0 – also 3x₁ = 12."),
                tip = "Auf der x₁-Achse sind x₂ und x₃ null. Übrig bleibt 3x₁ = 12.",
                way = listOf("x₂ = 0 und x₃ = 0 einsetzen", "3x₁ = 12 → x₁ = 4", "S₁(4|0|0); genauso S₂(0|3|0) und S₃(0|0|2)"),
                extras = { ph -> listOf(Stage.plane("E", n3, 12.0, "violet", "E")) +
                    if (ph == Phase.DONE) listOf(Stage.point("S2", s2, "cyan", "S₂(0|3|0)"), Stage.point("S3", s3, "cyan", "S₃(0|0|2)"), Stage.edges("spur", loop(s1, s2, s3), "cyan")) else emptyList() }),
            Families.plane("18-4", 18, "Aus den Achsenabschnitten", "Die Scheibe hängt an drei Haken auf den Achsen. Mach aus der Achsenabschnittsform eine Koordinatenform mit ganzen Zahlen.",
                listOf("S₁" to t1, "S₂" to t2, "S₃" to t3), p(1, 2, 3), 6,
                given = listOf(listOf(Txt("Achsenabschnittsform: x₁ : 6 + x₂ : 3 + x₃ : 2 = 1"))),
                tip = "Achsenabschnittsform: x₁ : 6 + x₂ : 3 + x₃ : 2 = 1. Mit 6 malnehmen, dann stehen nur noch ganze Zahlen da.",
                way = listOf("x₁ : 6 + x₂ : 3 + x₃ : 2 = 1", "mal 6: x₁ + 2x₂ + 3x₃ = 6", "Probe mit S₂(0|3|0): 2 · 3 = 6 ✓"),
                extras = { listOf(Stage.edges("spur", loop(t1, t2, t3), "cyan")) }),
            Families.scalar("18-5", 18, "Punktprobe mit Lücke", "Der Sensor P(2|a|1) soll genau in der Scheibe sitzen. Welches a passt?", "a", 0..8, 3, 0,
                tip = "P in die Gleichung einsetzen: 2·2 − a + 2·1 = 3. Nach a auflösen.",
                way = listOf("2·2 − a + 2·1 = 3", "6 − a = 3", "a = 3, also P(2|3|1)"), color = "yellow",
                formula = { a -> listOf(listOf(Txt("E: 2x₁ − x₂ + 2x₃ = 3")), listOf(Txt("P(2|"), Num(Geo.num(a), "a"), Txt("|1)"))) },
                objects = { a, _ -> listOf(Stage.plane("E", n5, 3.0, "violet", "E"), Stage.point("P", p(2, a, 1), "yellow", "P(2|$a|1)")) },
                check = { a -> Families.compare(a, 3, "a", "a = 3 – der Sensor sitzt genau in der Scheibe.") }),
            Families.plane("18-6", 18, "Abi-Check: Gerade und Punkt", "Eine Traverse g und ein Aufhängepunkt P spannen eine Leinwand auf. Stell die Koordinatenform ihrer Ebene E auf.",
                listOf("A" to a6, "B" to b6, "P" to q6), p(1, -1, 2), 5, abi = true,
                given = listOf(lineText("g", a6, d6), listOf(Txt("P(3|0|1)"))),
                tip = "Zwei Spannvektoren: die Richtung von g, also (1|1|0), und AP⃗ = (2|0|−1). Ihr Kreuzprodukt ist n⃗.",
                way = listOf("AP⃗ = P − A = (2|0|−1)", "n⃗ = (1|1|0) × (2|0|−1) = (−1|1|−2), also auch (1|−1|2)", "c = n⃗ · A = 1 − 0 + 4 = 5 → E: x₁ − x₂ + 2x₃ = 5"),
                extras = { listOf(Stage.fullLine("g", a6, d6, Bounds(), "cyan", "g")) }),
        )
    }

    // ───────────────────────── 19 · Schnitte und Schnittwinkel ─────────────────────────
    private val ch19 = run {
        val na = p(1, 1, 0); val nb = p(1, 0, 0)
        val a2 = p(1, 1, 1); val d2 = p(1, 0, 1); val n2 = p(1, 1, 0); val hit2 = p(5, 1, 5)
        val n3a = p(1, 1, 1); val n3b = p(1, -1, 1); val a3 = p(4, 2, 0); val z3 = p(0, 2, 4)
        val a4 = p(1, 2, 5); val n4 = p(2, 1, 2)
        val a5 = p(1, 2, 1); val d5 = p(1, 0, -1); val n5 = p(2, 1, 2); val f5 = Geo.footOnPlane(a5, n5, 18.0)
        listOf(
            angle("19-1", 19, "Zwei Glaswände", "Zwei Glaswände treffen sich in einer Kante. Unter welchem Winkel?", 45,
                listOf(listOf(Txt("E₁: x₁ + x₂ = 8")), listOf(Txt("E₂: x₁ = 4")), listOf(Txt("cos α = |n⃗₁ · n⃗₂| : (|n⃗₁| · |n⃗₂|)"))),
                tip = "Der Winkel zwischen zwei Ebenen ist der spitze Winkel zwischen ihren Normalenvektoren (1|1|0) und (1|0|0) – dafür sorgt der Betrag im Zähler.",
                way = listOf("n⃗₁ = (1|1|0), n⃗₂ = (1|0|0)", "n⃗₁ · n⃗₂ = 1, |n⃗₁| = √2, |n⃗₂| = 1", "cos α = 1 : √2 → α = 45°")) { ph ->
                listOf(Stage.plane("E1", na, 8.0, "violet", "E₁"), Stage.plane("E2", nb, 4.0, "cyan", "E₂")) +
                    if (ph == Phase.DONE) listOf(Stage.arc("bogen", p(4, 4, 3), V3(-1.0, 1.0, 0.0), V3(0.0, 1.0, 0.0), 1.6)) else emptyList()
            },
            angle("19-2", 19, "Laser auf Glas", "Der Laser g trifft die Glaswand E. Wie groß ist der Winkel zwischen Strahl und Wand?", 30,
                listOf(lineText("g", a2, d2), listOf(Txt("E: x₁ + x₂ = 6")), listOf(Txt("sin α = |d⃗ · n⃗| : (|d⃗| · |n⃗|)"))),
                tip = "Bei Gerade und Ebene nimmt man den Sinus: sin α = |d⃗ · n⃗| : (|d⃗| · |n⃗|) mit d⃗ = (1|0|1) und n⃗ = (1|1|0).",
                way = listOf("d⃗ · n⃗ = 1", "|d⃗| = √2, |n⃗| = √2", "sin α = 1 : 2 → α = 30°"),
                mistakes = mapOf(60 to "60° ist der Winkel zwischen Strahl und Normalenvektor. Zur Wand selbst fehlt der Rest auf 90° – oder gleich den Sinus nehmen.")) { ph ->
                listOf(Stage.plane("E", n2, 6.0, "violet", "E"), Stage.line("g", a2, d2, 0.0, 4.0, "cyan", "g"), Stage.point("S", hit2, "pink", if (ph == Phase.DONE) "S(5|1|5)" else null))
            },
            Families.aim("19-3", 19, "Schnittkante", "Zwei Scheiben schneiden sich in einer Kante. A liegt schon drauf – in welche Richtung läuft die Kante nach oben (x₃ > 0)?",
                a3, listOf(p(3, 2, 1), z3), hidden = true, line = "s",
                missText = { "Diese Richtung liegt noch nicht in beiden Scheiben. Sie muss auf beiden Normalenvektoren senkrecht stehen." },
                hitText = "Genau – das ist die Richtung der Schnittkante. Jedes Vielfache davon beschreibt dieselbe Gerade.",
                given = listOf(listOf(Txt("E₁: x₁ + x₂ + x₃ = 6")), listOf(Txt("E₂: x₁ − x₂ + x₃ = 2"))),
                tip = "Die Schnittgerade liegt in beiden Ebenen, steht also auf beiden Normalenvektoren senkrecht: d⃗ = n⃗₁ × n⃗₂.",
                way = listOf("n⃗₁ × n⃗₂ = (1|1|1) × (1|−1|1) = (2|0|−2)", "gekürzt: (1|0|−1); nach oben zeigt d⃗ = (−1|0|1)", "s: x⃗ = (4|2|0) + t · (−1|0|1)"),
                extras = { listOf(Stage.plane("E1", n3a, 6.0, "violet", "E₁"), Stage.plane("E2", n3b, 2.0, "cyan", "E₂")) }),
            Families.scalar("19-4", 19, "Nie berühren", "Die Laserbahn g hat die Richtung (2|a|−3) und soll die Scheibe E nie treffen. Stell a so ein, dass g parallel zu E läuft.", "a", -3..5, 2, 0,
                tip = "Parallel heißt: Die Richtung (2|a|−3) steht senkrecht auf dem Normalenvektor (2|1|2). Skalarprodukt null setzen.",
                way = listOf("(2|a|−3) · (2|1|2) = 4 + a − 6", "a − 2 = 0", "a = 2; A liegt nicht in E (2 + 2 + 10 = 14), also echt parallel"), color = "cyan",
                formula = { a -> listOf(listOf(Txt("g: x⃗ = "), col(a4), Txt(" + t · "), Part.Col(listOf("2", Geo.num(a), "−3"), "a")), listOf(Txt("E: 2x₁ + x₂ + 2x₃ = 12"))) },
                objects = { a, _ -> listOf(Stage.plane("E", n4, 12.0, "violet", "E"), Stage.fullLine("g", a4, p(2, a, -3), Bounds(), "cyan", "g"), Stage.point("A", a4, "cyan", "A(1|2|5)")) },
                check = { a -> Families.compare(a, 2, "a", "a = 2 – der Strahl läuft parallel an der Scheibe vorbei.") }),
            Families.scalar("19-5", 19, "Abstand Gerade–Ebene", "Der Strahl g läuft parallel zur Scheibe E. Wie groß ist der Abstand?", "d", 0..8, 4, 1,
                tip = "Bei einer parallelen Geraden hat jeder Punkt denselben Abstand. Nimm A und rechne |n⃗ · A − c| : |n⃗|.",
                way = listOf("n⃗ · A = 2·1 + 1·2 + 2·1 = 6", "|6 − 18| = 12, |n⃗| = 3", "d = 12 : 3 = 4"),
                formula = { a -> listOf(lineText("g", a5, d5), listOf(Txt("E: 2x₁ + x₂ + 2x₃ = 18")), listOf(Txt("d(g, E) = "), Num(Geo.num(a), "a"))) },
                objects = { _, ph -> listOf(Stage.plane("E", n5, 18.0, "violet", "E"), Stage.fullLine("g", a5, d5, Bounds(), "cyan", "g"), Stage.point("A", a5, "cyan", "A(1|2|1)")) +
                    if (ph == Phase.DONE) listOf(Stage.line("lot", a5, f5 - a5, 0.0, 1.0, "pink")) else listOf(Stage.stub("richtung", a5, n5, "pink", length = 0.9)) },
                check = { a -> Families.compare(a, 4, "Der Abstand", "4 Einheiten – überall gleich, weil g parallel läuft.") { ok -> listOf(Beam(a5, a5 + (f5 - a5) * (a / 4.0), "pink", ok)) } }),
            Families.point("19-6", 19, "Abi-Check: Kante trifft Boden", "Die Schnittkante der beiden Scheiben soll im Boden verankert werden. Wo trifft sie die x₁x₂-Ebene?",
                a3, hidden = true, name = "S", abi = true,
                given = listOf(listOf(Txt("E₁: x₁ + x₂ + x₃ = 6")), listOf(Txt("E₂: x₁ − x₂ + x₃ = 2"))),
                mistakes = listOf(p(6, 0, 0) to "Dieser Punkt liegt nur in E₁. Er muss beide Gleichungen erfüllen."),
                tip = "Am Boden ist x₃ = 0. Dann bleiben zwei Gleichungen: x₁ + x₂ = 6 und x₁ − x₂ = 2.",
                way = listOf("x₃ = 0: x₁ + x₂ = 6 und x₁ − x₂ = 2", "addieren: 2x₁ = 8 → x₁ = 4, dann x₂ = 2", "S(4|2|0)"),
                extras = { ph -> listOf(Stage.plane("E1", n3a, 6.0, "violet", "E₁"), Stage.plane("E2", n3b, 2.0, "cyan", "E₂")) +
                    if (ph == Phase.DONE) listOf(Stage.line("kante", a3, z3 - a3, 0.0, 1.0, "yellow", "s")) else emptyList() }),
        )
    }

    // ───────────────────────── 20 · Spiegeln und Lote ─────────────────────────
    private val ch20 = run {
        val q1 = p(1, 2, 1); val z1 = p(3, 3, 2); val r1 = p(5, 4, 3)
        val a2 = p(0, 1, 0); val d2 = p(1, 1, 1); val q2 = p(4, 2, 1); val f2 = p(2, 3, 2); val r2 = p(0, 4, 3)
        val n3 = p(1, 2, 2); val z3 = p(2, 2, 2)
        val a4 = p(1, 1, 0); val d4 = p(1, 0, 1)
        val ga = p(2, 4, 1); val gd = p(2, -1, 0); val hb = p(4, 7, 6); val hd = p(0, 1, -1)
        val rel = Geo.lineLine(ga, gd, hb, hd); val fg = ga + gd * rel.s; val fh = hb + hd * rel.t
        val n6 = p(2, 1, 2); val q6 = p(2, 2, 0); val f6 = Geo.footOnPlane(q6, n6, 18.0); val r6 = Geo.mirrorAtPlane(q6, n6, 18.0)
        listOf(
            Families.point("20-1", 20, "Am Punkt spiegeln", "Die Lampe P wird am Mast-Punkt Z gespiegelt. Wo hängt das Gegenstück P′?",
                r1, hidden = true, name = "P′",
                given = listOf(listOf(Txt("P(1|2|1)   Z(3|3|2)"))),
                mistakes = listOf(p(2, 1, 1) to "Das ist nur der Weg von P nach Z. Häng ihn an Z noch einmal an."),
                tip = "Z ist die Mitte zwischen P und P′. Also von Z aus denselben Weg noch einmal: P′ = Z + PZ⃗ = 2 · Z − P.",
                way = listOf("PZ⃗ = Z − P = (2|1|1)", "P′ = Z + PZ⃗ = (3|3|2) + (2|1|1)", "P′(5|4|3)"),
                extras = { ph -> listOf(Stage.point("P", q1, "cyan", "P(1|2|1)"), Stage.point("Z", z1, "white", "Z(3|3|2)")) +
                    if (ph == Phase.DONE) listOf(Stage.line("weg", q1, r1 - q1, 0.0, 1.0, "yellow", dashed = true)) else emptyList() }),
            Families.point("20-2", 20, "An der Geraden spiegeln", "Jetzt ist der Laserstrahl g der Spiegel. Wo landet das Spiegelbild von P?",
                r2, hidden = true, name = "P′",
                given = listOf(listOf(Txt("P(4|2|1)")), lineText("g", a2, d2)),
                mistakes = listOf(f2 to "Das ist der Lotfußpunkt F. Das Spiegelbild liegt genauso weit dahinter: P′ = 2 · F − P."),
                tip = "Erst den Lotfußpunkt F auf g bestimmen: (P − A − t · d⃗) · d⃗ = 0 mit dem Stützpunkt A(0|1|0). Dann P′ = 2 · F − P.",
                way = listOf("Stützpunkt A(0|1|0): (P − A) · d⃗ = (4|1|1) · (1|1|1) = 6, d⃗ · d⃗ = 3 → t = 2", "F = (0|1|0) + 2 · (1|1|1) = (2|3|2)", "P′ = 2 · F − P = (4|6|4) − (4|2|1) = (0|4|3)"),
                extras = { ph -> listOf(Stage.fullLine("g", a2, d2, Bounds(), "cyan", "g"), Stage.point("P", q2, "cyan", "P(4|2|1)")) +
                    if (ph == Phase.DONE) listOf(Stage.point("F", f2, "pink", "F(2|3|2)"), Stage.line("weg", q2, r2 - q2, 0.0, 1.0, "yellow", dashed = true)) else emptyList() }),
            Families.scalar("20-3", 20, "Scheibe am Punkt spiegeln", "Die Scheibe E wird am Punkt Z gespiegelt. Die neue Scheibe E′ ist parallel zu E. Welche Zahl c′ steht rechts in ihrer Gleichung?", "c′", 0..24, 14, 6,
                tip = "Parallel heißt: gleicher Normalenvektor. Z liegt genau in der Mitte, also ist n⃗ · Z = 10 der Mittelwert von 6 und c′.",
                way = listOf("n⃗ · Z = 2 + 4 + 4 = 10", "10 liegt mittig zwischen 6 und c′", "c′ = 2 · 10 − 6 = 14 → E′: x₁ + 2x₂ + 2x₃ = 14"), color = "cyan",
                formula = { a -> listOf(listOf(Txt("E: x₁ + 2x₂ + 2x₃ = 6")), listOf(Txt("Z(2|2|2)")), listOf(Txt("E′: x₁ + 2x₂ + 2x₃ = "), Num(Geo.num(a), "a"))) },
                objects = { a, _ -> listOf(Stage.plane("E", n3, 6.0, "violet", "E"), Stage.point("Z", z3, "white", "Z(2|2|2)"), Stage.plane("E2", n3, a.toDouble(), "cyan", "E′")) },
                check = { a -> Families.compare(a, 14, "c′", "c′ = 14 – Z liegt genau in der Mitte zwischen beiden Scheiben.",
                    mapOf(10 to "10 ist n⃗ · Z – das wäre die Scheibe durch Z selbst. Die gespiegelte liegt genauso weit dahinter: 2 · 10 − 6.")) }),
            Families.param("20-4", 20, "Abstand nach Wunsch", "Ich fliege auf g durch die Scheibe E hindurch. Bei welchem t bin ich dahinter genau 3 Einheiten von E entfernt?",
                a4, d4, 5, 0..6, hidden = true,
                given = listOf(listOf(Txt("E: x₁ + 2x₂ + 2x₃ = 9"))), success = "t = 5 – bei (6|1|5) bin ich genau 3 Einheiten hinter der Scheibe.",
                tip = "Abstand eines Punktes von E: |n⃗ · x⃗ − 9| : 3. Für g ist n⃗ · x⃗ = 3 + 3t. Also |3t − 6| : 3 = 3.",
                way = listOf("n⃗ · (A + t · d⃗) = 3 + 3t mit n⃗ = (1|2|2)", "|3 + 3t − 9| : 3 = 3 → |t − 2| = 3", "t = 5 (t = −1 liegt vor dem Start) → Punkt (6|1|5)"),
                extras = { listOf(Stage.plane("E", p(1, 2, 2), 9.0, "violet", "E")) }),
            Families.scalar("20-5", 20, "Kabel zwischen zwei Strahlen", "Zwischen den windschiefen Traversen g und h soll ein Kabel gespannt werden. Wie lang ist es mindestens?", "d", 0..9, 6, 1,
                tip = "n⃗ = d⃗₁ × d⃗₂ = (1|2|2) steht auf beiden senkrecht. Abstand = |(B − A) · n⃗| : |n⃗| mit den Stützpunkten A von g und B von h.",
                way = listOf("d⃗₁ × d⃗₂ = (2|−1|0) × (0|1|−1) = (1|2|2), |n⃗| = 3", "Stützpunkte A(2|4|1) und B(4|7|6): (B − A) · n⃗ = (2|3|5) · (1|2|2) = 18", "d = 18 : 3 = 6"),
                formula = { a -> listOf(lineText("g", ga, gd, "s"), lineText("h", hb, hd), listOf(Txt("d(g, h) = "), Num(Geo.num(a), "a"))) },
                objects = { _, ph -> listOf(Stage.fullLine("g", ga, gd, Bounds(), "cyan", "g"), Stage.fullLine("h", hb, hd, Bounds(), "yellow", "h")) +
                    if (ph == Phase.DONE) listOf(Stage.line("lot", fg, fh - fg, 0.0, 1.0, "pink"), Stage.point("Fg", fg, "pink"), Stage.point("Fh", fh, "pink")) else emptyList() },
                check = { a -> Families.compare(a, 6, "Der Abstand", "6 Einheiten – kürzer geht das Kabel nicht.",
                    mapOf(9 to "Du hast durch 2 geteilt? Die Länge von n⃗ = (1|2|2) ist √9 = 3.")) { ok -> listOf(Beam(fg, fg + (fh - fg) * (a / 6.0), "pink", ok)) } }),
            Families.scalar("20-6", 20, "Abi-Check: Spiegelbild", "P wird an der Scheibe E gespiegelt. Wie weit sind P und sein Spiegelbild P′ voneinander entfernt?", "d", 0..12, 8, 1, abi = true,
                tip = "Erst den Abstand von P zur Scheibe: |n⃗ · P − 18| : |n⃗|. Das Spiegelbild liegt genauso weit dahinter.",
                way = listOf("n⃗ · P = 4 + 2 + 0 = 6, |n⃗| = 3", "Abstand P–E = |6 − 18| : 3 = 4", "P′ liegt 4 dahinter → |PP′| = 8"),
                formula = { a -> listOf(listOf(Txt("P(2|2|0)")), listOf(Txt("E: 2x₁ + x₂ + 2x₃ = 18")), listOf(Txt("|PP′| = "), Num(Geo.num(a), "a"))) },
                objects = { _, ph -> listOf(Stage.plane("E", n6, 18.0, "violet", "E"), Stage.point("P", q6, "cyan", "P(2|2|0)")) +
                    if (ph == Phase.DONE) listOf(Stage.point("F", f6, "pink"), Stage.point("P2", r6, "yellow", "P′"), Stage.line("lot", q6, r6 - q6, 0.0, 1.0, "pink")) else listOf(Stage.stub("richtung", q6, n6, "pink", length = 0.9)) },
                check = { a -> Families.compare(a, 8, "Die Entfernung", "8 Einheiten: 4 bis zur Scheibe und 4 dahinter.",
                    mapOf(4 to "4 ist der Abstand von P zur Scheibe. P′ liegt genauso weit dahinter – also das Doppelte.")) { ok -> listOf(Beam(q6, q6 + (r6 - q6) * (a / 8.0), "pink", ok)) } }),
        )
    }

    // ───────────────────────── 21 · Kugel und Ebene ─────────────────────────
    private val ch21 = run {
        val m1 = p(3, 3, 2); val n1 = p(1, 2, 2); val b2 = p(4, 5, 4)
        val m3 = p(5, 5, 4); val c3 = p(4, 3, 2)
        val ma = p(2, 2, 2); val mb = p(6, 4, 6); val touch = p(4, 3, 4)
        val m6 = p(4, 4, 3); val a6 = p(1, 1, 4); val d6 = p(1, 1, 0)
        listOf(
            Families.scalar("21-1", 21, "Kugel an die Wand", "Die Lichtkugel soll die Glaswand E genau berühren. Wie groß muss der Radius sein?", "r", 1..5, 3, 1,
                tip = "Berühren heißt: Der Abstand des Mittelpunkts zur Ebene ist genau der Radius. Abstand = |n⃗ · M − c| : |n⃗|.",
                way = listOf("n⃗ · M = 3 + 6 + 4 = 13", "|13 − 22| = 9, |n⃗| = 3", "r = 9 : 3 = 3"), color = "cyan",
                formula = { a -> listOf(listOf(Txt("M(3|3|2)")), listOf(Txt("E: x₁ + 2x₂ + 2x₃ = 22")), listOf(Txt("r = "), Num(Geo.num(a), "a"))) },
                objects = { a, _ -> listOf(Stage.plane("E", n1, 22.0, "violet", "E"), Stage.sphere("K", m1, a.toDouble(), "cyan"), Stage.point("M", m1, "white", "M", drop = false)) },
                check = { a -> if (a > 3) Outcome(false, "Zu groß – die Kugel ragt durch die Wand. Der Abstand von M zur Wand ist kleiner.")
                    else Families.compare(a, 3, "Der Radius", "r = 3 – die Kugel berührt die Wand in genau einem Punkt.") }),
            Families.point("21-2", 21, "Berührpunkt", "Die Kugel mit r = 3 berührt die Wand. Genau dort kommt ein Sensor hin – wo?",
                b2, hidden = true, name = "B",
                given = listOf(listOf(Txt("M(3|3|2)   r = 3")), listOf(Txt("E: x₁ + 2x₂ + 2x₃ = 22"))),
                tip = "Der Berührpunkt ist der Lotfußpunkt von M auf E. Geh von M aus in Richtung n⃗ = (1|2|2), bis die Gleichung stimmt.",
                way = listOf("Lotgerade: x⃗ = (3|3|2) + t · (1|2|2)", "einsetzen: 13 + 9t = 22 → t = 1", "B = (3|3|2) + (1|2|2) = (4|5|4)"),
                extras = { ph -> listOf(Stage.plane("E", n1, 22.0, "violet", "E"), Stage.sphere("K", m1, 3.0, "cyan"), Stage.point("M", m1, "white", "M", drop = false)) +
                    if (ph == Phase.DONE) listOf(Stage.line("lot", m1, b2 - m1, 0.0, 1.0, "pink")) else emptyList() }),
            Families.point("21-3", 21, "Mitte des Schnittkreises", "Die Scheibe E schneidet die Kugel in einem Kreis. Wo liegt dessen Mittelpunkt M′?",
                c3, hidden = true, name = "M′",
                given = listOf(listOf(Txt("K: (x₁ − 5)² + (x₂ − 5)² + (x₃ − 4)² = 13")), listOf(Txt("E: x₁ + 2x₂ + 2x₃ = 14"))),
                tip = "M′ ist der Lotfußpunkt des Kugelmittelpunkts M(5|5|4) auf E. Lotgerade durch M mit Richtung n⃗ = (1|2|2).",
                way = listOf("Lotgerade: x⃗ = (5|5|4) + t · (1|2|2)", "einsetzen: 23 + 9t = 14 → t = −1", "M′ = (5|5|4) − (1|2|2) = (4|3|2)"),
                extras = { ph -> listOf(Stage.plane("E", n1, 14.0, "violet", "E"), Stage.sphere("K", m3, kotlin.math.sqrt(13.0), "cyan"), Stage.point("M", m3, "white", "M", drop = false)) +
                    if (ph == Phase.DONE) listOf(Stage.circle("kreis", c3, n1, 2.0, "yellow"), Stage.line("lot", m3, c3 - m3, 0.0, 1.0, "pink")) else emptyList() }),
            Families.scalar("21-4", 21, "Radius des Schnittkreises", "Dieselbe Kugel, dieselbe Scheibe. Wie groß ist der Radius r′ des Schnittkreises?", "r′", 1..6, 2, 1,
                tip = "Pythagoras im Schnitt: r² = d² + r′². Hier ist r² = 13 und d = |MM′| = 3.",
                way = listOf("d = |MM′| = |(1|2|2)| = 3", "r′² = r² − d² = 13 − 9 = 4", "r′ = 2"), color = "yellow",
                formula = { a -> listOf(listOf(Txt("K: … = 13   M(5|5|4)   M′(4|3|2)")), listOf(Txt("r′² = r² − d²")), listOf(Txt("r′ = "), Num(Geo.num(a), "a"))) },
                objects = { a, _ -> listOf(Stage.plane("E", n1, 14.0, "violet", "E"), Stage.sphere("K", m3, kotlin.math.sqrt(13.0), "cyan"), Stage.point("M", m3, "white", "M", drop = false),
                    Stage.point("M2", c3, "pink", "M′"), Stage.circle("kreis", c3, n1, a.toDouble(), "yellow")) },
                check = { a -> Families.compare(a, 2, "Der Radius", "r′ = 2 – der Kreis liegt genau auf der Kugel.", mapOf(4 to "4 ist r′². Zieh noch die Wurzel.")) }),
            Families.scalar("21-5", 21, "Zwei Kugeln", "Zwei Lichtkugeln sollen sich genau berühren. Die erste hat r₁ = 3. Stell r₂ passend ein.", "r₂", 1..5, 3, 1,
                tip = "Zwei Kugeln berühren sich von außen, wenn der Abstand der Mittelpunkte gleich r₁ + r₂ ist.",
                way = listOf("M₁M₂⃗ = (4|2|4)", "|M₁M₂⃗| = √(16 + 4 + 16) = 6", "r₂ = 6 − 3 = 3"), color = "pink",
                formula = { a -> listOf(listOf(Txt("M₁(2|2|2), r₁ = 3")), listOf(Txt("M₂(6|4|6), r₂ = "), Num(Geo.num(a), "a"))) },
                objects = { a, ph -> listOf(Stage.sphere("K1", ma, 3.0, "cyan"), Stage.sphere("K2", mb, a.toDouble(), "pink"), Stage.point("M1", ma, "white", "M₁", drop = false), Stage.point("M2", mb, "white", "M₂", drop = false)) +
                    if (ph == Phase.DONE) listOf(Stage.point("B", touch, "yellow", "B(4|3|4)", drop = false)) else emptyList() },
                check = { a -> if (a > 3) Outcome(false, "Zu groß – die Kugeln überlappen. Rechne den Abstand der Mittelpunkte aus.")
                    else Families.compare(a, 3, "r₂", "r₂ = 3 – die Kugeln berühren sich genau in der Mitte bei B(4|3|4).") }),
            Families.scalar("21-6", 21, "Abi-Check: Flug durch die Kugel", "Eine Drohne fliegt auf g durch die Lichtkugel (t in Sekunden). Wie viele Sekunden ist sie in der Kugel?", "Δt", 0..8, 4, 0, abi = true,
                tip = "g in die Kugelgleichung einsetzen: (t − 3)² + (t − 3)² + 1 = 9. Das gibt Ein- und Austrittszeit.",
                way = listOf("(1 + t − 4)² + (1 + t − 4)² + (4 − 3)² = 9", "2 · (t − 3)² = 8 → t = 1 oder t = 5", "Δt = 5 − 1 = 4 Sekunden"), suffix = " s",
                formula = { a -> listOf(lineText("g", a6, d6), listOf(Txt("K: (x₁ − 4)² + (x₂ − 4)² + (x₃ − 3)² = 9")), listOf(Txt("Δt = "), Num(Geo.num(a), "a"), Txt(" s"))) },
                objects = { _, ph -> listOf(Stage.sphere("K", m6, 3.0, "cyan"), Stage.fullLine("g", a6, d6, Bounds(), "yellow", "g"), Stage.point("A", a6, "yellow", "A(1|1|4)")) +
                    if (ph == Phase.DONE) listOf(Stage.point("ein", p(2, 2, 4), "pink", "t = 1"), Stage.point("aus", p(6, 6, 4), "pink", "t = 5")) else emptyList() },
                check = { a -> Families.compare(a, 4, "Die Zeit", "4 Sekunden – von t = 1 bis t = 5.",
                    mapOf(5 to "t = 5 ist der Austritt. Gefragt ist die Dauer: Austritt minus Eintritt.", 1 to "t = 1 ist der Eintritt. Gefragt ist die Dauer: Austritt minus Eintritt.")) }),
        )
    }

    val chapters = listOf(
        Chapter(18, "Ebenen in allen Formen", "Parameter-, Normalen-, Koordinaten- und Achsenabschnittsform, Spurpunkte", "Glaspalast", "🔶", "violet", listOf(
            "Eine Ebene, viele Schreibweisen. Im Abi musst du zwischen allen wechseln können.",
            "Parameterform: ein Punkt und zwei Spannvektoren. Koordinatenform: n₁x₁ + n₂x₂ + n₃x₃ = c – die Vorfaktoren bilden den Normalenvektor n⃗. Das Kreuzprodukt bringt dich von der einen zur anderen.",
            "Spurpunkte sind die Stellen, an denen die Ebene die Achsen trifft – damit kannst du sie schnell zeichnen."), ch18),
        Chapter(19, "Schnitte & Schnittwinkel", "Schnittgerade, Winkel Ebene–Ebene und Gerade–Ebene, parallel, Abstand", "Spiegelkabinett", "✂️", "green", listOf(
            "Wo sich zwei Scheiben treffen, entsteht eine Kante – die Schnittgerade.",
            "Ihre Richtung steht auf beiden Normalenvektoren senkrecht: Kreuzprodukt.",
            "Winkel: zwischen zwei Ebenen mit dem Kosinus, zwischen Gerade und Ebene mit dem Sinus."), ch19),
        Chapter(20, "Spiegeln & Lote", "Spiegeln an Punkt, Gerade und Ebene, Abstand windschiefer Geraden", "Varieté", "🔄", "orange", listOf(
            "Spiegeln heißt immer: zum Spiegel hin und genauso weit dahinter.",
            "Der Weg zum Spiegel ist ein Lot. Hast du den Lotfußpunkt F, ist das Spiegelbild P′ = 2 · F − P.",
            "Windschiefe Geraden kommen sich nur an einer Stelle am nächsten – dort steht das Lot auf beiden senkrecht."), ch20),
        Chapter(21, "Kugel & Ebene", "Berühren, Schnittkreis, zwei Kugeln, Durchflug", "Planetarium", "🌐", "cyan", listOf(
            "Eine Ebene kann eine Kugel verfehlen, berühren oder schneiden. Entscheidend ist der Abstand des Mittelpunkts.",
            "Schneidet sie, entsteht ein Kreis. Sein Radius r′ kommt aus dem Satz des Pythagoras: r² = d² + r′² (r Kugelradius, d Abstand des Mittelpunkts von der Ebene).",
            "Fliegt etwas durch die Kugel, gibt das eine quadratische Gleichung – mit Eintritt und Austritt."), ch21),
    )
}
