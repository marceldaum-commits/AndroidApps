package de.raumlicht.core.model

import de.raumlicht.core.model.Families.col
import de.raumlicht.core.model.Part.Num
import de.raumlicht.core.model.Part.Txt
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Weitere Übungsaufgaben zur Analytischen Geometrie (seit 2.11.0): Punkte mit Bedingungen (viele richtige Antworten), Spiegeln an
 * Koordinatenebenen und -achsen, Parallelogramm und Raute, dieselbe Gerade in anderer Darstellung, drei Punkte auf einer Geraden.
 * Jede Art gehört zu einer Stufe; Zahlen und Texte entstehen aus dem Seed.
 */
object MoreGen {
    enum class Kind(val tier: JamLevel, val topic: JamTopic) {
        OPEN_POINT(JamLevel.EINSTIEG, JamTopic.PUNKTE), MIRROR_PLANE(JamLevel.EINSTIEG, JamTopic.PUNKTE),
        MIRROR_AXIS(JamLevel.ABI, JamTopic.PUNKTE), PARALLELOGRAM(JamLevel.ABI, JamTopic.PUNKTE), COLLINEAR(JamLevel.ABI, JamTopic.GERADEN), SAME_LINE(JamLevel.ABI, JamTopic.GERADEN),
        MIRROR_TWICE(JamLevel.PROFI, JamTopic.PUNKTE), RHOMBUS(JamLevel.PROFI, JamTopic.PUNKTE),
    }

    private val B = Bounds()
    /** Raum mit negativen Koordinaten – fürs Spiegeln */
    private val S = Bounds(listOf(-5, -5, -4), listOf(5, 5, 4))
    private fun inRoom(v: V3, b: Bounds = B) = (0..2).all { v.list[it] >= b.min[it] && v.list[it] <= b.max[it] }
    private fun Random.point(pad: Int = 0) = V3(nextInt(pad, 9 - pad), nextInt(pad, 9 - pad), nextInt(pad, 7 - pad))
    private fun Random.dir(max: Int): V3 { while (true) { val d = V3(nextInt(-max, max + 1), nextInt(-max, max + 1), nextInt(-max, max + 1)); if (!d.isZero) return d } }
    /** Punkt im Spiegel-Raum, keine Koordinate null */
    private fun Random.signed(max: Int) = nextInt(1, max + 1) * (if (nextBoolean()) 1 else -1)
    private fun Random.offAxes() = V3(signed(4), signed(4), signed(3))
    private val X = listOf("x₁", "x₂", "x₃")
    private fun planeName(k: Int) = (0..2).filter { it != k }.joinToString("") { X[it] } + "-Ebene"          // Ebene mit x_k = 0
    private fun flip(p: V3, vararg axes: Int) = V3.of(p.ints.mapIndexed { i, v -> if (i in axes) -v else v })
    private fun unit(k: Int) = V3(if (k == 0) 1 else 0, if (k == 1) 1 else 0, if (k == 2) 1 else 0)
    private fun lineText(name: String, a: V3, d: List<Part>, param: String = "t") = listOf(Txt("$name: x⃗ = "), col(a), Txt(" + $param · ")) + d
    private fun n(v: Double) = Geo.num(v)
    private fun n(v: Int) = Geo.num(v)

    fun level(kind: Kind, seed: Long, id: String = "mehr-${kind.name.lowercase()}-$seed"): Level {
        val r = Random(seed * 41 + kind.ordinal + 2000)
        repeat(20_000) { build(kind, r, id)?.let { return it } }
        error("Keine Aufgabe für $kind/$seed gefunden")
    }

    /** Offene Aufgabe: Jeder Punkt, der alle [rules] erfüllt, ist richtig. [example] ist eine mögliche Lösung (für den Rechenweg und den Test). */
    private fun open(id: String, title: String, mission: String, example: V3, rules: List<Pair<String, (V3) -> Boolean>>, tip: String, way: List<String>, extras: (Phase) -> List<Obj>) = Level(
        id, 0, title, mission, B,
        controls = listOf(Control("P", "P", ControlKind.POINT, listOf(0, 0, 0), B.min, B.max, "yellow")),
        solution = mapOf("P" to example.ints), tip = tip, way = way, handle = Handle("P", V3.ZERO),
        formula = { v -> listOf(listOf(Txt("P = "), col(v.vec("P"), "P"))) },
        objects = { v, ph -> extras(ph) + listOfNotNull(Stage.drone(if (ph == Phase.DONE) v.vec("P") else V3(8, 0, 1), if (ph == Phase.DONE) "happy" else "idle"),
            Stage.point("marke", v.vec("P"), "yellow", if (ph == Phase.DONE) "P${v.vec("P").fmt()}" else null, if (ph == Phase.DONE) "target" else "ghost"),
            if (ph == Phase.AIM) Stage.handle("griff", v.vec("P"), "yellow") else null) },
        evaluate = { v -> val p = v.vec("P"); val bad = rules.filter { !it.second(p) }
            when { p.isZero -> Outcome(false, "Der Ursprung zählt nicht – such dir einen anderen Punkt.", flyTo = p)
                bad.isEmpty() -> Outcome(true, "Passt – ${p.fmt()} erfüllt alle Bedingungen. Es gibt noch viele andere richtige Punkte.", flyTo = p)
                else -> Outcome(false, "Noch nicht: Dein Punkt ${bad.joinToString(" und ") { it.first }} nicht ✗.", flyTo = p) } },
    )

    private fun build(kind: Kind, r: Random, id: String): Level? = when (kind) {
        Kind.OPEN_POINT -> {
            val i = r.nextInt(3); val j = (i + 1 + r.nextInt(2)) % 3; val k = 3 - i - j; val c = r.nextInt(1, 5)
            fun axis(a: Int) = Stage.line("achse$a", V3.ZERO, unit(a), 0.0, B.max[a].toDouble(), "cyan", "${X[a]}-Achse")
            when (r.nextInt(3)) {
                0 -> open(id, "Zwei Ebenen zugleich", "Gib einen Punkt an – nicht den Ursprung –, der in der ${planeName(i)} und zugleich in der ${planeName(j)} liegt.",
                    unit(k) * c, listOf("liegt in der ${planeName(i)}" to { p: V3 -> abs(p.list[i]) < 1e-9 }, "liegt in der ${planeName(j)}" to { p: V3 -> abs(p.list[j]) < 1e-9 }),
                    tip = "In der ${planeName(i)} ist ${X[i]} = 0, in der ${planeName(j)} ist ${X[j]} = 0. Beides zusammen lässt nur noch ${X[k]} frei.",
                    way = listOf("${X[i]} = 0 und ${X[j]} = 0", "zum Beispiel ${(unit(k) * c).fmt()} – jeder Punkt der ${X[k]}-Achse passt")) { listOf(axis(k)) }
                1 -> open(id, "Gleich weit von zwei Achsen", "Gib einen Punkt an – nicht den Ursprung –, der in der ${planeName(k)} liegt und von der ${X[i]}-Achse genauso weit entfernt ist wie von der ${X[j]}-Achse.",
                    unit(i) * c + unit(j) * c, listOf("liegt in der ${planeName(k)}" to { p: V3 -> abs(p.list[k]) < 1e-9 }, "ist von beiden Achsen gleich weit entfernt" to { p: V3 -> abs(abs(p.list[i]) - abs(p.list[j])) < 1e-9 }),
                    tip = "In der ${planeName(k)} ist ${X[k]} = 0. Der Abstand zur ${X[i]}-Achse ist dort einfach |${X[j]}| – und umgekehrt.",
                    way = listOf("${X[k]} = 0, Abstände |${X[j]}| und |${X[i]}| müssen gleich sein", "zum Beispiel ${(unit(i) * c + unit(j) * c).fmt()}")) { listOf(axis(i), axis(j)) }
                else -> open(id, "Zwischen zwei Achsen", "Gib einen Punkt an – nicht den Ursprung –, der von der ${X[i]}-Achse genauso weit entfernt ist wie von der ${X[j]}-Achse. Er darf irgendwo im Raum liegen.",
                    unit(i) * c + unit(j) * c + unit(k) * minOf(c, B.max[k]), listOf("ist von beiden Achsen gleich weit entfernt" to { p: V3 -> abs(abs(p.list[i]) - abs(p.list[j])) < 1e-9 }),
                    tip = "Abstand zur ${X[i]}-Achse: √(${X[j]}² + ${X[k]}²). Zur ${X[j]}-Achse: √(${X[i]}² + ${X[k]}²). Gleich sind sie, wenn |${X[i]}| = |${X[j]}| ist.",
                    way = listOf("√(${X[j]}² + ${X[k]}²) = √(${X[i]}² + ${X[k]}²) → |${X[i]}| = |${X[j]}|", "zum Beispiel ${(unit(i) * c + unit(j) * c + unit(k) * minOf(c, B.max[k])).fmt()} – ${X[k]} ist frei wählbar")) { listOf(axis(i), axis(j)) }
            }
        }
        Kind.MIRROR_PLANE -> {
            val p = r.offAxes(); val k = r.nextInt(3); val q = flip(p, k)
            Families.point(id, 0, "Spiegeln an der Ebene", "Die ${planeName(k)} ist ein Spiegel. Wo liegt das Spiegelbild von P?", q, hidden = true, name = "P′", bounds = S,
                given = listOf(listOf(Txt("P${p.fmt()}"))), mistakes = listOf(flip(p, 0, 1, 2) to "Das ist die Spiegelung am Ursprung – dabei drehen sich alle drei Vorzeichen. An der Ebene dreht sich nur eines."),
                tip = "Beim Spiegeln an der ${planeName(k)} ändert sich nur ${X[k]} – das Vorzeichen dreht sich um. Die beiden anderen Koordinaten bleiben.",
                way = listOf("nur ${X[k]} wechselt das Vorzeichen: ${n(p.ints[k])} → ${n(q.ints[k])}", "P′${q.fmt()}"),
                extras = { ph -> listOf(Stage.plane("spiegel", unit(k), 0.0, "violet", planeName(k)), Stage.point("P", p, "cyan", "P${p.fmt()}")) +
                    if (ph == Phase.DONE) listOf(Stage.line("weg", p, q - p, 0.0, 1.0, "yellow", dashed = true)) else emptyList() })
        }
        Kind.MIRROR_AXIS -> {
            val p = r.offAxes(); val k = r.nextInt(3); val (a, b) = (0..2).filter { it != k }; val q = flip(p, a, b)
            Families.point(id, 0, "Spiegeln an der Achse", "Jetzt ist die ${X[k]}-Achse der Spiegel. Wo landet das Spiegelbild von P?", q, hidden = true, name = "P′", bounds = S,
                given = listOf(listOf(Txt("P${p.fmt()}"))),
                mistakes = listOf(flip(p, k) to "Du hast an der ${planeName(k)} gespiegelt. An der ${X[k]}-Achse bleibt ${X[k]} gleich und die beiden anderen wechseln.", flip(p, a) to "Erst eine Koordinate gedreht – an einer Achse wechseln zwei.", flip(p, b) to "Erst eine Koordinate gedreht – an einer Achse wechseln zwei."),
                tip = "An der ${X[k]}-Achse bleibt ${X[k]} stehen. Die beiden anderen Koordinaten wechseln das Vorzeichen – wie eine halbe Drehung um die Achse.",
                way = listOf("${X[k]} bleibt: ${n(p.ints[k])}", "${X[a]} und ${X[b]} wechseln das Vorzeichen", "P′${q.fmt()}, Abstand zur Achse √(${n(p.ints[a] * p.ints[a])} + ${n(p.ints[b] * p.ints[b])}) = ${n(sqrt((p.ints[a] * p.ints[a] + p.ints[b] * p.ints[b]).toDouble()))}"),
                extras = { ph -> listOf(Stage.line("achse", unit(k) * S.min[k], unit(k), 0.0, (S.max[k] - S.min[k]).toDouble(), "violet", "${X[k]}-Achse"), Stage.point("P", p, "cyan", "P${p.fmt()}")) +
                    if (ph == Phase.DONE) listOf(Stage.line("weg", p, q - p, 0.0, 1.0, "yellow", dashed = true)) else emptyList() })
        }
        Kind.PARALLELOGRAM -> {
            val a = r.point(); val u = r.dir(4); val v = r.dir(4); val b = a + u; val d = a + v; val c = b + v; val m = (a + c) * 0.5
            if (Geo.parallel(u, v) || !inRoom(b) || !inRoom(c) || !inRoom(d) || (a + c).ints.any { it % 2 != 0 } || u.ints.count { it != 0 } < 2) null
            else Families.point(id, 0, "Parallelogramm", "Vier Haken A, B, C, D tragen ein Lichtsegel. Prüfe, ob es ein Parallelogramm ist – und setz den Spot auf den Schnittpunkt der Diagonalen.", m, hidden = true, name = "M",
                given = listOf(listOf(Txt("A${a.fmt()}   B${b.fmt()}")), listOf(Txt("C${c.fmt()}   D${d.fmt()}"))),
                mistakes = listOf(a + c to "Du hast A und C addiert – jetzt noch durch 2 teilen."),
                tip = "Parallelogramm heißt: AB⃗ und DC⃗ sind gleich. Dann halbieren sich die Diagonalen – ihr Schnittpunkt ist die Mitte von A und C.",
                way = listOf("AB⃗ = B − A = ${u.fmt()}, DC⃗ = C − D = ${u.fmt()} – gleich, also ein Parallelogramm", "die Diagonalen halbieren sich: M = ½ · (A + C)", "M${m.fmt()} (Probe: ½ · (B + D) ergibt dasselbe)"),
                extras = { ph -> listOf(Stage.edges("segel", listOf(a to b, b to c, c to d, d to a), "cyan"), Stage.point("A", a, "cyan", "A"), Stage.point("B", b, "cyan", "B"), Stage.point("C", c, "cyan", "C"), Stage.point("D", d, "cyan", "D")) +
                    if (ph == Phase.DONE) listOf(Stage.edges("diagonalen", listOf(a to c, b to d), "yellow")) else emptyList() })
        }
        Kind.COLLINEAR -> {
            val a = r.point(); val d = r.dir(2); val f = r.nextInt(1, 3); val g = listOf(-1, 2, 3, 4).let { it[r.nextInt(it.size)] }; val b = a + d * f; val c = a + d * g
            val j = r.nextInt(3); val ref = (0..2).firstOrNull { it != j && d.ints[it] != 0 }
            if (g == f || d.ints[j] == 0 || ref == null || !inRoom(b) || !inRoom(c)) null else {
                val sol = c.ints[j]; val ratio = g.toDouble() / f
                fun cAt(k: Int) = V3.of(c.ints.mapIndexed { i, x -> if (i == j) k else x })
                Families.scalar(id, 0, "Drei auf einer Geraden", "A und B hängen fest. Für welches k liegt auch C genau auf der Geraden durch A und B?", "k", 0..B.max[j], sol, if (sol == 0) 1 else 0,
                    tip = "Drei Punkte liegen auf einer Geraden, wenn AC⃗ ein Vielfaches von AB⃗ ist. Den Faktor liest du an einer bekannten Koordinate ab.",
                    way = listOf("AB⃗ = ${(b - a).fmt()}", "AC⃗ = r · AB⃗: aus ${X[ref]} folgt r = ${n(c.ints[ref] - a.ints[ref])} : ${n(b.ints[ref] - a.ints[ref])} = ${n(ratio)}", "k = ${n(a.ints[j])} + ${n(ratio)} · ${n(b.ints[j] - a.ints[j])} = ${n(sol)}"), color = "yellow",
                    formula = { k -> listOf(listOf(Txt("A${a.fmt()}   B${b.fmt()}")), listOf(Txt("C("), *c.ints.indices.flatMap { i -> listOf(if (i == j) Num(Geo.num(k), "a") else Txt(n(c.ints[i])), Txt(if (i < 2) "|" else ")")) }.toTypedArray())) },
                    objects = { k, _ -> listOf(Stage.fullLine("g", a, d, B, "cyan", "g"), Stage.point("A", a, "cyan", "A"), Stage.point("B", b, "cyan", "B"), Stage.point("C", cAt(k), "yellow", "C")) },
                    check = { k -> Families.compare(k, sol, "k", "k = ${n(sol)} – C liegt genau auf der Geraden.") })
            }
        }
        Kind.SAME_LINE -> {
            val a = r.point(); val d = r.dir(2); val q = a + d * 2; val f = listOf(-2, -1, 2, 3).let { it[r.nextInt(it.size)] }; val e = d * f; val j = r.nextInt(3)
            val ref = (0..2).firstOrNull { it != j && d.ints[it] != 0 }
            if (d.ints[j] == 0 || ref == null || !inRoom(q) || !inRoom(a + d) || abs(e.ints[j]) > 6) null else {
                val sol = e.ints[j]
                Families.scalar(id, 0, "Dieselbe Gerade?", "Zwei Schreibweisen – eine Gerade? Für welches k beschreibt h dieselbe Gerade wie g?", "k", -6..6, sol, 0,
                    tip = "Dieselbe Gerade heißt zweierlei: Die Richtungsvektoren sind Vielfache voneinander, und der Stützpunkt von h liegt auf g.",
                    way = listOf("Richtungen: aus ${X[ref]} folgt der Faktor ${n(e.ints[ref])} : ${n(d.ints[ref])} = ${n(f)}", "k = ${n(f)} · ${n(d.ints[j])} = ${n(sol)}", "Stützpunkt ${q.fmt()} liegt auf g (t = 2) – also dieselbe Gerade"), color = "yellow",
                    formula = { k -> listOf(lineText("g", a, listOf(col(d))), lineText("h", q, listOf(Part.Col(e.ints.mapIndexed { i, x -> if (i == j) Geo.num(k) else n(x) }, "a")), "s")) },
                    objects = { k, _ -> val dir = V3.of(e.ints.mapIndexed { i, x -> if (i == j) k else x })
                        listOf(Stage.fullLine("g", a, d, B, "cyan", "g"), Stage.point("A", a, "cyan", "A"), Stage.point("Q", q, "yellow", "Q")) + if (dir.isZero) emptyList() else listOf(Stage.fullLine("h", q, dir, B, "yellow", "h")) },
                    check = { k -> Families.compare(k, sol, "k", "k = ${n(sol)} – h ist dieselbe Gerade wie g, nur anders aufgeschrieben.") })
            }
        }
        Kind.MIRROR_TWICE -> {
            val p = r.offAxes(); val i = r.nextInt(3); val j = (i + 1 + r.nextInt(2)) % 3; val k = 3 - i - j; val p1 = flip(p, i); val p2 = flip(p1, j)
            val sq = p.ints[i] * p.ints[i] + p.ints[j] * p.ints[j]
            Families.point(id, 0, "Zweimal spiegeln", "P wird erst an der ${planeName(i)} gespiegelt, das Bild dann an der ${planeName(j)}. Wo landet P″ am Ende?", p2, hidden = true, name = "P″", bounds = S,
                given = listOf(listOf(Txt("P${p.fmt()}"))),
                mistakes = listOf(p1 to "Das ist erst das Bild nach der ersten Spiegelung. Spiegle es noch an der ${planeName(j)}.", flip(p, j) to "Du hast nur an der ${planeName(j)} gespiegelt – die erste Spiegelung fehlt.", flip(p, 0, 1, 2) to "Alle drei Vorzeichen gedreht – das wäre die Spiegelung am Ursprung. Hier wechseln nur zwei."),
                tip = "Schritt für Schritt: Jede Spiegelung an einer Koordinatenebene dreht genau ein Vorzeichen. Zwei hintereinander wirken wie eine Spiegelung an einer Achse.",
                way = listOf("an der ${planeName(i)}: ${X[i]} wechselt → P′${p1.fmt()}", "an der ${planeName(j)}: ${X[j]} wechselt → P″${p2.fmt()}", "zusammen wechseln ${X[i]} und ${X[j]} – das ist die Spiegelung an der ${X[k]}-Achse",
                    "Probe: |PP″| = 2 · √(${n(p.ints[i] * p.ints[i])} + ${n(p.ints[j] * p.ints[j])}) = 2 · √${n(sq)} ≈ ${n(2 * sqrt(sq.toDouble()))}"),
                extras = { ph -> listOf(Stage.plane("spiegel1", unit(i), 0.0, "violet", planeName(i)), Stage.plane("spiegel2", unit(j), 0.0, "cyan", planeName(j)), Stage.point("P", p, "cyan", "P${p.fmt()}")) +
                    if (ph == Phase.DONE) listOf(Stage.point("P1", p1, "pink", "P′"), Stage.line("weg1", p, p1 - p, 0.0, 1.0, "pink", dashed = true), Stage.line("weg2", p1, p2 - p1, 0.0, 1.0, "yellow", dashed = true)) else emptyList() })
        }
        Kind.RHOMBUS -> {
            val a = r.point(); val u = r.dir(3); val perm = listOf(listOf(1, 0, 2), listOf(2, 1, 0), listOf(0, 2, 1), listOf(1, 2, 0), listOf(2, 0, 1)).let { it[r.nextInt(it.size)] }
            val v = V3.of(perm.map { u.ints[it] * (if (r.nextBoolean()) 1 else -1) }).let { V3(it.x, abs(it.y), it.z) }
            val b = a + u; val d = a + v; val c = b + v; val len = (u dot u).toInt(); val vy = v.ints[1]; val rest = len - v.ints[0] * v.ints[0] - v.ints[2] * v.ints[2]; val m = (a + c) * 0.5
            if (Geo.parallel(u, v) || vy == 0 || u.ints.count { it != 0 } < 2 || !inRoom(b) || !inRoom(c) || !inRoom(d) || a.ints[1] - vy >= 0 && false) null else {
                val sol = d.ints[1]; fun dAt(k: Int) = V3(d.x, k.toDouble(), d.z)
                Families.scalar(id, 0, "Raute mit Parameter", "A, B und D spannen ein Parallelogramm ABCD auf. Für welches k ist es eine Raute? (D soll rechts von A liegen: k > ${n(a.ints[1])}.)", "k", a.ints[1]..B.max[1], sol, a.ints[1],
                    tip = "Eine Raute ist ein Parallelogramm mit vier gleich langen Seiten. Es reicht |AD⃗| = |AB⃗| – rechne mit den Quadraten, dann brauchst du keine Wurzel.",
                    way = listOf("AB⃗ = ${u.fmt()}, |AB⃗|² = ${n(len)}", "AD⃗ = (${n(v.ints[0])}|k − ${n(a.ints[1])}|${n(v.ints[2])}), |AD⃗|² = ${n(v.ints[0] * v.ints[0])} + (k − ${n(a.ints[1])})² + ${n(v.ints[2] * v.ints[2])}",
                        "gleich lang: (k − ${n(a.ints[1])})² = ${n(rest)} → k = ${n(a.ints[1])} ± ${n(sqrt(rest.toDouble()))}, wegen k > ${n(a.ints[1])} also k = ${n(sol)}",
                        "C = B + AD⃗ = ${c.fmt()}, die Diagonalen schneiden sich in M${m.fmt()}, Seitenlänge √${n(len)} ≈ ${n(sqrt(len.toDouble()))}"), color = "yellow",
                    formula = { k -> listOf(listOf(Txt("A${a.fmt()}   B${b.fmt()}")), listOf(Txt("D(${n(d.ints[0])}|"), Num(Geo.num(k), "a"), Txt("|${n(d.ints[2])})")), listOf(Txt("|AB⃗| = |AD⃗| ?"))) },
                    objects = { k, ph -> val dd = dAt(k); val cc = b + (dd - a)
                        listOf(Stage.edges("seiten", listOf(a to b, a to dd) + if (inRoom(cc)) listOf(b to cc, dd to cc) else emptyList(), "cyan"), Stage.point("A", a, "cyan", "A"), Stage.point("B", b, "cyan", "B"), Stage.point("D", dd, "yellow", "D")) +
                            if (ph == Phase.DONE) listOf(Stage.point("C", c, "cyan", "C"), Stage.edges("diagonalen", listOf(a to c, b to d), "yellow"), Stage.point("M", m, "pink", "M", drop = false)) else emptyList() },
                    check = { k -> Families.compare(k, sol, "k", "k = ${n(sol)} – alle vier Seiten sind √${n(len)} lang: eine Raute.") })
            }
        }
    }
}
