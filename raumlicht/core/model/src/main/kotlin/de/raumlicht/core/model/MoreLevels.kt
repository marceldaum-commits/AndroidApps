package de.raumlicht.core.model

import de.raumlicht.core.model.Families.col
import de.raumlicht.core.model.Part.Num
import de.raumlicht.core.model.Part.Txt
import kotlin.math.roundToInt

/** Kapitel 7–10: Winkel, Abstände, Kugeln, Kreuzprodukt. */
internal object MoreLevels {
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)
    private fun lineText(name: String, a: V3, d: V3, param: String = "t") = listOf(Txt("$name: x⃗ = "), col(a), Txt(" + $param · "), col(d))
    private fun loop(vararg pts: V3) = pts.indices.map { pts[it] to pts[(it + 1) % pts.size] }

    // ───────────────────────── 7 · Winkel ─────────────────────────
    /** Winkel in 15°-Schritten einstellen. [u], [v] sind die Richtungen, zwischen denen der Bogen gezeichnet wird. */
    private fun angle(id: String, title: String, mission: String, o: V3, u: V3, v: V3, degrees: Int, formula: List<List<Part>>, tip: String, way: List<String>,
                      abi: Boolean = false, objects: (Phase) -> List<Obj>) =
        Families.scalar(id, 7, title, mission, "α", 0..12, degrees / 15, 0, tip, way, "yellow", step = 15, suffix = "°", abi = abi,
            formula = { a -> formula + listOf(listOf(Txt("α = "), Num("${a * 15}°", "a"))) },
            objects = { _, ph -> objects(ph) + Stage.arc("bogen", o, u, v, 1.8) },
            check = { a -> Families.compare(a * 15, degrees, "Der Winkel", "Genau $degrees° – perfekt ausgerichtet!") })

    private val ch7 = run {
        val o1 = p(1, 1, 2); val a1 = p(5, 0, 0); val b1 = p(5, 5, 0)
        val o2 = p(2, 1, 1); val a2 = p(4, 4, 0); val b2 = p(0, 4, 4)
        val a3 = p(1, 1, 0); val d3 = p(3, 4, 5); val f3 = p(3, 4, 0)
        val o4 = p(4, 4, 2); val n4a = p(1, 1, 0); val n4b = p(0, 1, 1)
        val a5 = p(2, 3, 0); val d5 = p(0, 1, 1); val s5 = p(2, 6, 3); val pr5 = V3(-0.5, 0.5, 1.0)
        listOf(
            angle("7-1", "Öffnungswinkel", "Zwei Scheinwerfer-Arme a⃗ und b⃗. Wie groß ist der Winkel zwischen ihnen?", o1, a1, b1, 45,
                listOf(listOf(Txt("a⃗ = "), col(a1), Txt("   b⃗ = "), col(b1)), listOf(Txt("cos α = (a⃗ · b⃗) : (|a⃗| · |b⃗|)"))),
                tip = "a⃗ · b⃗ = 25, |a⃗| = 5, |b⃗| = √50. Also cos α = 25 : (5 · √50) = 1 : √2.",
                way = listOf("a⃗ · b⃗ = 5·5 + 0·5 + 0·0 = 25", "|a⃗| = 5, |b⃗| = √50 = 5√2", "cos α = 25 : (25√2) = 1 : √2 → α = 45°")) {
                listOf(Stage.point("O", o1, "white"), Stage.vector("a", o1, o1 + a1, "cyan", "a⃗"), Stage.vector("b", o1, o1 + b1, "pink", "b⃗"))
            },
            angle("7-2", "Schräg im Raum", "Diesmal liegen die Arme nicht in einer Koordinatenebene. Die Formel ist dieselbe.", o2, a2, b2, 60,
                listOf(listOf(Txt("a⃗ = "), col(a2), Txt("   b⃗ = "), col(b2)), listOf(Txt("cos α = (a⃗ · b⃗) : (|a⃗| · |b⃗|)"))),
                tip = "a⃗ · b⃗ = 16, beide Längen sind √32. cos α = 16 : 32.",
                way = listOf("a⃗ · b⃗ = 0 + 16 + 0 = 16", "|a⃗| = |b⃗| = √32", "cos α = 16 : 32 = ½ → α = 60°")) {
                listOf(Stage.point("O", o2, "white"), Stage.vector("a", o2, o2 + a2, "cyan", "a⃗"), Stage.vector("b", o2, o2 + b2, "pink", "b⃗"))
            },
            angle("7-3", "Steigungswinkel", "Der Laser steigt vom Boden auf. Unter welchem Winkel zur Bodenebene?", a3, d3, f3, 45,
                listOf(lineText("g", a3, d3), listOf(Txt("E: x₃ = 0    sin α = |n⃗ · d⃗| : (|n⃗| · |d⃗|)"))),
                tip = "Bei Gerade und Ebene steht der Sinus in der Formel. n⃗ = (0|0|1), also |n⃗ · d⃗| = 5 und |d⃗| = √50.",
                way = listOf("n⃗ = (0|0|1), n⃗ · d⃗ = 5", "|d⃗| = √(9 + 16 + 25) = √50 = 5√2", "sin α = 5 : (5√2) = 1 : √2 → α = 45°")) {
                listOf(Stage.point("A", a3, "cyan", "A"), Stage.line("g", a3, d3, 0.0, 1.0, "cyan", "g"), Stage.line("schatten", a3, f3, 0.0, 1.0, "pink", dashed = true))
            },
            angle("7-4", "Zwei Scheiben", "Zwei Glasscheiben schneiden sich. Der Winkel zwischen Ebenen ist der Winkel zwischen ihren Normalenvektoren.", o4, n4a, n4b, 60,
                listOf(listOf(Txt("E₁: x₁ + x₂ = 8    E₂: x₂ + x₃ = 6")), listOf(Txt("cos α = |n⃗₁ · n⃗₂| : (|n⃗₁| · |n⃗₂|)"))),
                tip = "n⃗₁ = (1|1|0), n⃗₂ = (0|1|1). Skalarprodukt 1, beide Längen √2.",
                way = listOf("n⃗₁ = (1|1|0), n⃗₂ = (0|1|1)", "n⃗₁ · n⃗₂ = 1, |n⃗₁| = |n⃗₂| = √2", "cos α = 1 : 2 → α = 60°")) {
                listOf(Stage.plane("E1", n4a, 8.0, "violet", "E₁"), Stage.plane("E2", n4b, 6.0, "cyan", "E₂"), Stage.vector("n1", o4, o4 + n4a * 2, "violet", "n⃗₁"), Stage.vector("n2", o4, o4 + n4b * 2, "cyan", "n⃗₂"))
            },
            angle("7-5", "Abi-Check: Einfallswinkel", "Ein Laser trifft das Sonnensegel E. Unter welchem Winkel trifft er auf?", s5, -d5, -pr5, 30,
                listOf(lineText("g", a5, d5), listOf(Txt("E: x₁ + x₂ = 8    sin α = |n⃗ · d⃗| : (|n⃗| · |d⃗|)"))), abi = true,
                tip = "n⃗ = (1|1|0), d⃗ = (0|1|1): n⃗ · d⃗ = 1, beide Längen √2. Achtung: Sinus, nicht Kosinus.",
                way = listOf("n⃗ = (1|1|0), n⃗ · d⃗ = 1", "|n⃗| = |d⃗| = √2", "sin α = 1 : 2 → α = 30°")) {
                listOf(Stage.plane("E", p(1, 1, 0), 8.0, "violet", "E"), Stage.point("A", a5, "cyan", "A"), Stage.line("g", a5, s5 - a5, 0.0, 1.0, "cyan", "g"),
                    Stage.line("spur", s5, -pr5, 0.0, 2.2, "pink", dashed = true))
            },
        )
    }

    // ───────────────────────── 8 · Abstände ─────────────────────────
    private val ch8 = run {
        val a1 = p(1, 0, 1); val d1 = p(1, 2, 2); val q1 = p(5, 2, 6); val f1 = p(3, 4, 5)
        val a2 = p(0, 1, 0); val d2 = p(2, 2, 1); val q2 = p(5, 3, 4); val f2 = p(4, 5, 2)
        val n3 = p(1, 2, 2); val q3 = p(1, 2, 2)
        val ga = p(1, 3, 1); val gd = p(1, 2, 2); val hb = p(4, 1, 0); val hd = p(1, 0, -2)
        val rel = Geo.lineLine(ga, gd, hb, hd); val fg = ga + gd * rel.s; val fh = hb + hd * rel.t
        listOf(
            Families.point("8-1", 8, "Lot auf den Strahl", "Von P soll ein Halteseil auf kürzestem Weg zum Laserstrahl g. Wo trifft es den Strahl?",
                f1, hidden = true, name = "F",
                given = listOf(listOf(Txt("P(5|2|6)")), lineText("g", a1, d1)),
                tip = "F liegt auf g, also F = A + t · d⃗. Das Seil steht senkrecht auf g: (P − F) · d⃗ = 0.",
                way = listOf("(P − A) · d⃗ = (4|2|5) · (1|2|2) = 18", "d⃗ · d⃗ = 9 → t = 18 : 9 = 2", "F = (1|0|1) + 2 · (1|2|2) = (3|4|5)"),
                extras = { ph -> listOf(Stage.fullLine("g", a1, d1, Bounds(), "cyan", "g"), Stage.point("P", q1, "cyan", "P(5|2|6)")) +
                    if (ph == Phase.DONE) listOf(Stage.line("lot", q1, f1 - q1, 0.0, 1.0, "yellow")) else emptyList() }),
            Families.scalar("8-2", 8, "Abstand Punkt–Gerade", "Wie lang muss das Seil von P zum Strahl g mindestens sein?", "d", 0..8, 3, 1,
                tip = "Erst den Lotfußpunkt F bestimmen (wie eben), dann die Länge von PF⃗.",
                way = listOf("t = (P − A) · d⃗ : (d⃗ · d⃗) = 18 : 9 = 2 → F(4|5|2)", "PF⃗ = (−1|2|−2)", "d = √(1 + 4 + 4) = 3"),
                formula = { a -> listOf(listOf(Txt("P(5|3|4)")), lineText("g", a2, d2), listOf(Txt("d(P, g) = "), Num(Geo.num(a), "a"))) },
                objects = { _, ph -> listOf(Stage.fullLine("g", a2, d2, Bounds(), "cyan", "g"), Stage.point("P", q2, "cyan", "P(5|3|4)")) +
                    if (ph == Phase.DONE) listOf(Stage.line("lot", q2, f2 - q2, 0.0, 1.0, "pink"), Stage.point("F", f2, "pink", "F(4|5|2)")) else listOf(Stage.stub("richtung", q2, f2 - q2, "pink", length = 0.9)) },
                check = { a -> Families.compare(a, 3, "Der Abstand", "Genau 3 – das Seil reicht bis zum Strahl.") { ok -> listOf(Beam(q2, q2 + (f2 - q2) * (a / 3.0), "pink", ok)) } }),
            Families.scalar("8-3", 8, "Parallele Scheiben", "Zwei Glasscheiben hängen parallel. Wie groß ist ihr Abstand?", "d", 0..8, 3, 1,
                tip = "Gleicher Normalenvektor (1|2|2) mit Länge 3. Abstand = |c₂ − c₁| : |n⃗|.",
                way = listOf("n⃗ = (1|2|2), |n⃗| = 3", "|18 − 9| = 9", "d = 9 : 3 = 3"),
                formula = { a -> listOf(listOf(Txt("E₁: x₁ + 2x₂ + 2x₃ = 9")), listOf(Txt("E₂: x₁ + 2x₂ + 2x₃ = 18")), listOf(Txt("d = "), Num(Geo.num(a), "a"))) },
                objects = { _, ph -> listOf(Stage.plane("E1", n3, 9.0, "violet", "E₁"), Stage.plane("E2", n3, 18.0, "cyan", "E₂"), Stage.point("Q", q3, "pink")) +
                    if (ph == Phase.DONE) listOf(Stage.line("lot", q3, n3, 0.0, 1.0, "pink")) else listOf(Stage.stub("richtung", q3, n3, "pink", length = 0.9)) },
                check = { a -> Families.compare(a, 3, "Der Abstand", "3 Einheiten – der Strahl endet genau auf der zweiten Scheibe.") { ok -> listOf(Beam(q3, q3 + n3 * (a / 3.0), "pink", ok)) } }),
            Families.scalar("8-4", 8, "Knapp vorbei", "Die Strahlen g und h sind windschief. Wie nah kommen sie sich?", "d", 0..8, 3, 1,
                tip = "n⃗ = d⃗₁ × d⃗₂ steht auf beiden senkrecht. Abstand = |(B − A) · n⃗| : |n⃗|.",
                way = listOf("d⃗₁ × d⃗₂ = (−4|4|−2), gekürzt n⃗ = (2|−2|1), |n⃗| = 3", "(B − A) · n⃗ = (3|−2|−1) · (2|−2|1) = 9", "d = 9 : 3 = 3"),
                formula = { a -> listOf(lineText("g", ga, gd, "s"), lineText("h", hb, hd), listOf(Txt("d(g, h) = "), Num(Geo.num(a), "a"))) },
                objects = { _, ph -> listOf(Stage.fullLine("g", ga, gd, Bounds(), "cyan", "g"), Stage.fullLine("h", hb, hd, Bounds(), "yellow", "h")) +
                    if (ph == Phase.DONE) listOf(Stage.line("lot", fg, fh - fg, 0.0, 1.0, "pink"), Stage.point("Fg", fg, "pink"), Stage.point("Fh", fh, "pink")) else emptyList() },
                check = { a -> Families.compare(a, 3, "Der Abstand", "3 Einheiten liegen zwischen den Strahlen.") { ok -> listOf(Beam(fg, fg + (fh - fg) * (a / 3.0), "pink", ok)) } }),
            Families.param("8-5", 8, "Abi-Check: Sicherheitsabstand", "Eine Drohne fliegt auf g an der Mastspitze P vorbei (t in Sekunden). Zu welchem Zeitpunkt ist sie P am nächsten?",
                a2, d2, 2, 0..5, hidden = true, abi = true,
                tip = "Am nächsten ist die Drohne, wenn die Verbindung zu P senkrecht auf der Flugrichtung steht: (P − A − t · d⃗) · d⃗ = 0.",
                way = listOf("(P − A) · d⃗ = (5|2|4) · (2|2|1) = 18", "d⃗ · d⃗ = 9", "t = 18 : 9 = 2 (Abstand dann 3)"),
                extras = { ph -> listOf(Stage.edges("mast", listOf(p(5, 3, 0) to q2), "white"), Stage.point("P", q2, "white", "P(5|3|4)", drop = false)) +
                    if (ph == Phase.DONE) listOf(Stage.line("lot", q2, f2 - q2, 0.0, 1.0, "yellow", dashed = true)) else emptyList() }),
        )
    }

    // ───────────────────────── 9 · Kugeln ─────────────────────────
    private val ch9 = run {
        val m = p(4, 4, 3); val ball = "K: (x₁ − 4)² + (x₂ − 4)² + (x₃ − 3)² = 9"
        val a3 = p(8, 0, 1); val d3 = p(-2, 2, 1); val s3 = p(6, 2, 2)
        val touch = p(5, 6, 5); val dome = p(4, 4, 0); val m6 = p(3, 3, 3); val n6 = p(1, 2, 2)
        listOf(
            Families.scalar("9-1", 9, "Discokugel", "Die Discokugel hat den Mittelpunkt M. Stell den Radius so ein, dass P genau auf der Kugel liegt.", "r", 1..5, 3, 1,
                tip = "Der Radius ist der Abstand von M zu P: die Länge von MP⃗ = (2|1|2).",
                way = listOf("MP⃗ = P − M = (2|1|2)", "r = √(4 + 1 + 4) = 3", "K: (x₁ − 4)² + (x₂ − 4)² + (x₃ − 3)² = 9"), color = "cyan",
                formula = { a -> listOf(listOf(Txt("M(4|4|3)   P(6|5|5)")), listOf(Txt("K: (x₁ − 4)² + (x₂ − 4)² + (x₃ − 3)² = "), Num(Geo.num(a * a), "a")), listOf(Txt("r = "), Num(Geo.num(a), "a"))) },
                objects = { a, _ -> listOf(Stage.sphere("K", m, a.toDouble(), "cyan"), Stage.point("M", m, "white", "M", drop = false), Stage.point("P", p(6, 5, 5), "yellow", "P(6|5|5)")) },
                check = { a -> Families.compare(a, 3, "Der Radius", "r = 3 – P liegt genau auf der Kugel.") }),
            Families.point("9-2", 9, "Durchmesser", "A und B liegen sich auf der Kugel genau gegenüber. Wo ist der Mittelpunkt?",
                m, hidden = true, name = "M",
                given = listOf(listOf(Txt("A(2|3|1)   B(6|5|5)"))),
                tip = "Der Mittelpunkt der Kugel ist der Mittelpunkt der Strecke AB.",
                way = listOf("M = ½ · (A + B)", "((2+6):2 | (3+5):2 | (1+5):2)", "M(4|4|3), Radius |AM⃗| = 3"),
                extras = { ph -> listOf(Stage.point("A", p(2, 3, 1), "cyan", "A"), Stage.point("B", p(6, 5, 5), "cyan", "B"), Stage.edges("d", listOf(p(2, 3, 1) to p(6, 5, 5)), "cyan")) +
                    if (ph == Phase.DONE) listOf(Stage.sphere("K", m, 3.0, "cyan")) else emptyList() }),
            Families.sensor("9-3", 9, "Erster Treffer", "Der Laser trifft die Discokugel. Wo trifft er zuerst auf? Das gibt eine quadratische Gleichung.",
                listOf(Ray("g", a3, d3, "cyan", s3)), s3,
                listOf(Check("liegt auf g") { Geo.onLine(it, a3, d3) }, Check("liegt auf der Kugel") { kotlin.math.abs((it - m).length - 3) < Geo.EPS }),
                given = listOf(lineText("g", a3, d3), listOf(Txt(ball))),
                tip = "g in die Kugelgleichung einsetzen: (4 − 2t)² + (−4 + 2t)² + (−2 + t)² = 9. Die kleinere Lösung ist der erste Treffer.",
                way = listOf("9t² − 36t + 36 = 9 → t² − 4t + 3 = 0", "t = 1 oder t = 3 (Ein- und Austritt)", "erster Treffer: S = (8|0|1) + 1 · (−2|2|1) = (6|2|2)"),
                extras = { listOf(Stage.sphere("K", m, 3.0, "violet"), Stage.point("M", m, "white", "M", drop = false)) }),
            Families.plane("9-4", 9, "Tangentialebene", "Eine Glasscheibe soll die Kugel genau im Punkt B berühren. Stell ihre Gleichung ein.",
                listOf("B" to touch), p(1, 2, 2), 27, maxC = 30, normal = p(1, 2, 2),
                normalHint = "B liegt in E ✓ – aber die Scheibe schneidet die Kugel. Der Normalenvektor muss von M nach B zeigen.",
                given = listOf(listOf(Txt("M(4|4|3)   B(5|6|5)"))),
                tip = "Der Radius MB⃗ steht senkrecht auf der Tangentialebene: n⃗ = MB⃗ = (1|2|2). Dann B einsetzen.",
                way = listOf("n⃗ = MB⃗ = (1|2|2)", "B einsetzen: 5 + 12 + 10 = 27", "E: x₁ + 2x₂ + 2x₃ = 27"),
                extras = { ph -> listOf(Stage.sphere("K", m, 3.0, "cyan"), Stage.point("M", m, "white", "M", drop = false)) +
                    if (ph == Phase.DONE) listOf(Stage.line("radius", m, touch - m, 0.0, 1.0, "yellow")) else emptyList() }),
            Families.scalar("9-5", 9, "Lichtkuppel", "Eine Scheibe in Höhe 3 schneidet die Lichtkuppel in einem Kreis. Welchen Radius hat der Schnittkreis?", "r′", 1..6, 4, 1,
                tip = "Kugelradius 5, Abstand der Ebene vom Mittelpunkt 3. Pythagoras: r′² = 5² − 3².",
                way = listOf("Abstand von M zur Ebene: d = 3", "r′² = r² − d² = 25 − 9 = 16", "r′ = 4"), color = "yellow",
                formula = { a -> listOf(listOf(Txt("K: M(4|4|0), r = 5    E: x₃ = 3")), listOf(Txt("r′ = "), Num(Geo.num(a), "a"))) },
                objects = { a, _ -> listOf(Stage.sphere("K", dome, 5.0, "cyan"), Stage.plane("E", p(0, 0, 1), 3.0, "violet", "E"), Stage.circle("kreis", p(4, 4, 3), p(0, 0, 1), a.toDouble(), "yellow")) },
                check = { a -> Families.compare(a, 4, "Der Schnittkreis", "r′ = 4 – der Kreis liegt genau auf der Kuppel.") }),
            Families.scalar("9-6", 9, "Abi-Check: Berührung", "Schieb die Scheibe E: x₁ + 2x₂ + 2x₃ = c so weit nach oben, dass sie die Kugel gerade noch berührt.", "c", 12..26, 24, 15,
                tip = "Berühren heißt: Abstand von M zur Ebene = Radius. |n⃗ · m⃗ − c| : 3 = 3, mit n⃗ · m⃗ = 15.", abi = true,
                way = listOf("n⃗ · m⃗ = 3 + 6 + 6 = 15, |n⃗| = 3", "|15 − c| : 3 = 3 → |15 − c| = 9", "nach oben: c = 24 (Berührpunkt (4|5|5))"), color = "violet",
                formula = { a -> listOf(listOf(Txt("K: M(3|3|3), r = 3")), listOf(Txt("E: x₁ + 2x₂ + 2x₃ = "), Num(Geo.num(a), "a"))) },
                objects = { a, ph -> listOf(Stage.sphere("K", m6, 3.0, "cyan"), Stage.point("M", m6, "white", "M", drop = false), Stage.plane("E", n6, a.toDouble(), "violet", "E")) +
                    if (ph == Phase.DONE) listOf(Stage.point("B", p(4, 5, 5), "yellow", "B(4|5|5)", drop = false)) else emptyList() },
                check = { a -> when {
                    a == 24 -> Outcome(true, "Die Scheibe berührt die Kugel in genau einem Punkt.")
                    a < 24 -> Outcome(false, "Die Scheibe schneidet die Kugel noch – der Abstand zu M ist kleiner als der Radius.")
                    else -> Outcome(false, "Zu weit: Die Scheibe schwebt schon neben der Kugel.")
                } }),
        )
    }

    // ───────────────────────── 10 · Kreuzprodukt: Flächen, Volumen, Schnittgerade ─────────────────────────
    private val ch10 = run {
        val o1 = p(1, 2, 2); val a1 = p(2, 2, 1); val b1 = p(4, 1, -1)
        val ta = p(1, 1, 2); val tb = p(5, 5, 4); val tc = p(5, 2, 1)
        val o3 = p(1, 1, 0); val a3 = p(3, 0, 0); val b3 = p(0, 2, 0); val c3 = p(1, 1, 3)
        val base = arrayOf(p(1, 1, 0), p(4, 1, 0), p(4, 4, 0), p(1, 4, 0)); val top = p(2, 2, 4)
        listOf(
            Families.scalar("10-1", 10, "Lichtsegel", "Zwei Kanten a⃗ und b⃗ spannen ein Segel in Form eines Parallelogramms auf. Wie groß ist seine Fläche?", "A", 0..16, 9, 0,
                tip = "Die Fläche ist die Länge des Kreuzprodukts: A = |a⃗ × b⃗|.",
                way = listOf("a⃗ × b⃗ = (2·(−1) − 1·1 | 1·4 − 2·(−1) | 2·1 − 2·4) = (−3|6|−6)", "|a⃗ × b⃗| = √(9 + 36 + 36) = √81", "A = 9"), color = "yellow",
                formula = { a -> listOf(listOf(Txt("a⃗ = "), col(a1), Txt("   b⃗ = "), col(b1)), listOf(Txt("A = |a⃗ × b⃗| = "), Num(Geo.num(a), "a"))) },
                objects = { _, ph -> listOf(Stage.edges("segel", loop(o1, o1 + a1, o1 + a1 + b1, o1 + b1), "yellow", faces = listOf(listOf(o1, o1 + a1, o1 + a1 + b1, o1 + b1))),
                    Stage.vector("a", o1, o1 + a1, "cyan", "a⃗"), Stage.vector("b", o1, o1 + b1, "pink", "b⃗")) +
                    if (ph == Phase.DONE) listOf(Stage.vector("n", o1, o1 + p(-1, 2, -2) * 0.9, "green", "a⃗×b⃗")) else emptyList() },
                check = { a -> Families.compare(a, 9, "Die Fläche", "9 Flächeneinheiten – so groß ist das Segel.") }),
            Families.scalar("10-2", 10, "Dreieckssegel", "Drei Drohnen halten ein dreieckiges Segel ABC. Wie groß ist es?", "A", 0..16, 9, 0,
                tip = "Ein Dreieck ist ein halbes Parallelogramm: A = ½ · |AB⃗ × AC⃗|.",
                way = listOf("AB⃗ = (4|4|2), AC⃗ = (4|1|−1)", "AB⃗ × AC⃗ = (−6|12|−12), Länge √324 = 18", "A = ½ · 18 = 9"), color = "yellow",
                formula = { a -> listOf(listOf(Txt("A(1|1|2)   B(5|5|4)   C(5|2|1)")), listOf(Txt("A = ½ · |AB⃗ × AC⃗| = "), Num(Geo.num(a), "a"))) },
                objects = { _, _ -> listOf(Stage.edges("segel", loop(ta, tb, tc), "yellow", faces = listOf(listOf(ta, tb, tc))), Stage.point("A", ta, "cyan", "A"), Stage.point("B", tb, "cyan", "B"), Stage.point("C", tc, "cyan", "C")) },
                check = { a -> Families.compare(a, 9, "Die Fläche", "9 Flächeneinheiten – halb so viel wie das Parallelogramm.") }),
            Families.scalar("10-3", 10, "Zeltvolumen", "Ein schiefes Zelt: Grundfläche von a⃗ und b⃗ aufgespannt, c⃗ führt zur Spitze. Wie viel Raum ist darin?", "V", 0..12, 6, 0,
                tip = "Pyramide mit Parallelogramm als Grundfläche: V = ⅓ · |(a⃗ × b⃗) · c⃗|.",
                way = listOf("a⃗ × b⃗ = (0|0|6)", "(a⃗ × b⃗) · c⃗ = 6 · 3 = 18", "V = ⅓ · 18 = 6"), color = "yellow",
                formula = { a -> listOf(listOf(Txt("a⃗ = "), col(a3), Txt(" b⃗ = "), col(b3), Txt(" c⃗ = "), col(c3)), listOf(Txt("V = ⅓ · |(a⃗ × b⃗) · c⃗| = "), Num(Geo.num(a), "a"))) },
                objects = { _, _ ->
                    val q = listOf(o3, o3 + a3, o3 + a3 + b3, o3 + b3); val s = o3 + c3
                    listOf(Stage.edges("zelt", loop(*q.toTypedArray()) + q.map { it to s }, "yellow", faces = q.indices.map { listOf(q[it], q[(it + 1) % 4], s) }),
                        Stage.vector("a", o3, o3 + a3, "cyan", "a⃗"), Stage.vector("b", o3, o3 + b3, "pink", "b⃗"), Stage.vector("c", o3, s, "green", "c⃗"))
                },
                check = { a -> Families.compare(a, 6, "Das Volumen", "6 Volumeneinheiten passen ins Zelt.") }),
            Families.aim("10-4", 10, "Schnittgerade", "Zwei Scheiben schneiden sich in einer Geraden. Der Laser in A liegt auf beiden – schick den Strahl genau die Schnittkante entlang.",
                p(4, 0, 2), listOf(p(0, 4, 2)), hidden = true, solution = p(-1, 1, 0), line = "s",
                given = listOf(listOf(Txt("E₁: x₁ + x₂ + x₃ = 6    E₂: x₃ = 2"))),
                tip = "Die Schnittgerade liegt in beiden Ebenen, steht also auf beiden Normalenvektoren senkrecht: d⃗ = n⃗₁ × n⃗₂.",
                way = listOf("n⃗₁ = (1|1|1), n⃗₂ = (0|0|1)", "n⃗₁ × n⃗₂ = (1|−1|0)", "in den Raum hinein: d⃗ = (−1|1|0)"),
                extras = { listOf(Stage.plane("E1", p(1, 1, 1), 6.0, "violet", "E₁"), Stage.plane("E2", p(0, 0, 1), 2.0, "pink", "E₂")) }),
            Families.scalar("10-5", 10, "Abi-Check: Festivalzelt", "Ein Zelt hat eine quadratische Grundfläche ABCD und die Spitze S. Berechne sein Volumen.", "V", 0..12, 6, 0,
                tip = "V = ⅓ · Grundfläche · Höhe. Die Grundfläche liegt am Boden, die Höhe ist die x₃-Koordinate von S.", abi = true,
                way = listOf("Grundfläche: Quadrat mit Seitenlänge 3 → G = 9", "Höhe h = 4 (x₃-Koordinate von S)", "V = ⅓ · 9 · 4 = 12"), color = "yellow", step = 2,
                formula = { a -> listOf(listOf(Txt("A(1|1|0)  B(4|1|0)  C(4|4|0)  D(1|4|0)  S(2|2|4)")), listOf(Txt("V = ⅓ · G · h = "), Num(Geo.num(a * 2), "a"))) },
                objects = { _, _ -> listOf(Stage.edges("zelt", loop(*base) + base.map { it to top }, "yellow", faces = base.indices.map { listOf(base[it], base[(it + 1) % 4], top) }),
                    Stage.point("S", top, "cyan", "S(2|2|4)")) },
                check = { a -> Families.compare(a * 2, 12, "Das Volumen", "12 Volumeneinheiten – das Zelt steht.") }),
        )
    }

    val chapters = listOf(
        Chapter(7, "Im richtigen Winkel", "Winkel", "Theater", "📐", "yellow", listOf(
            "Scheinwerfer müssen im richtigen Winkel stehen – und den rechnest du mit dem Skalarprodukt aus.",
            "Zwei Vektoren oder zwei Ebenen: Kosinus. Gerade und Ebene: Sinus.",
            "Stell den Winkel in 15°-Schritten ein. Der Bogen in der Bühne zeigt, welcher Winkel gemeint ist."), ch7),
        Chapter(8, "Auf Abstand", "Abstände", "Messehalle", "📏", "pink", listOf(
            "Sicherheitsabstände sind Pflicht. Kürzester Weg heißt immer: senkrecht.",
            "Punkt–Gerade: Lotfußpunkt suchen. Parallele Ebenen und windschiefe Geraden: Normalenvektor nutzen.",
            "Der Strahl zeigt dir, ob deine Länge passt."), ch8),
        Chapter(9, "Discokugel", "Kugeln", "Großraumdisco", "🪩", "cyan", listOf(
            "Eine Kugel ist alles, was vom Mittelpunkt gleich weit entfernt ist.",
            "Kugelgleichung: (x₁ − m₁)² + (x₂ − m₂)² + (x₃ − m₃)² = r².",
            "Trifft ein Laser die Kugel, gibt das eine quadratische Gleichung – mit Ein- und Austritt."), ch9),
        Chapter(10, "Zugabe", "Kreuzprodukt · Flächen · Volumen", "Stadion", "🎆", "green", listOf(
            "Das Kreuzprodukt kann mehr als Normalenvektoren.",
            "Seine Länge ist eine Fläche. Mit einem dritten Vektor wird daraus ein Volumen.",
            "Letzter Gig der Tour. Zeig, was du kannst."), ch10),
    )
}
