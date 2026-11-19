package de.raumlicht.core.model

import de.raumlicht.core.model.Families.col
import de.raumlicht.core.model.Part.Num
import de.raumlicht.core.model.Part.Txt
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/** Schwierigkeit im endlosen Üben (seit 2.10.0). Jede Stufe ist messbar schwerer als die vorige – siehe [Practice.metrics] und DifficultyTest. */
enum class JamLevel(val title: String, val emoji: String, val hint: String) {
    EINSTIEG("Einstieg", "🌱", "ein, zwei Rechenschritte, glatte Zahlen"),
    ABI("Abi", "🎓", "mehrere Schritte wie in der Prüfung"),
    PROFI("Profi", "🔥", "mehrstufig, mit Parametern und krummen Zwischenwerten"),
}

/** Kennzahlen einer Aufgabe – damit lässt sich belegen, dass eine Stufe schwerer ist als die andere. */
data class Metrics(val steps: Double, val wayLength: Double, val objects: Double, val parameters: Double, val nonInteger: Double) {
    /** Eine Zahl für alles: Rechenschritte zählen am meisten, dann Länge des Rechenwegs, beteiligte Objekte, Parameter und krumme Werte */
    val score get() = steps * 10 + wayLength / 10 + objects * 3 + parameters * 5 + nonInteger * 20
}

object Practice {
    // Einstieg: Aufgaben ohne Gleichung mit Parameter – ablesen, mitteln, abziehen, einsetzen. Alles mit „nach t auflösen“ gehört schon zur Abi-Stufe.
    private val easy = setOf(Gen.Kind.MIDPOINT, Gen.Kind.VECTOR, Gen.Kind.AIM, Gen.Kind.RADIUS, Gen.Kind.MOTION_POS, Gen.Kind.MOTION_TIME)
    fun tier(k: Gen.Kind) = if (k in easy) JamLevel.EINSTIEG else JamLevel.ABI
    private fun proTopics(k: ProGen.Kind) = when (k) { ProGen.Kind.MIRROR_DIST, ProGen.Kind.PARALLEL_PARAM -> setOf(JamTopic.EBENEN); else -> setOf(JamTopic.WINKEL) }

    /** Eine Aufgabenart mit Stufe und Themen – egal, aus welchem Baukasten sie kommt */
    class Source(val id: String, val tier: JamLevel, val topics: Set<JamTopic>, val make: (Long) -> Level)
    /** Alle Aufgabenarten des endlosen Übens */
    val sources: List<Source> =
        Gen.active.map { k -> Source(k.name, tier(k), JamTopic.entries.filter { it != JamTopic.MIX && k in it.kinds }.toSet()) { seed -> Gen.level(k, seed) } } +
        MoreGen.Kind.entries.map { k -> Source(k.name, k.tier, setOf(k.topic)) { seed -> MoreGen.level(k, seed) } } +
        ProGen.Kind.entries.map { k -> Source(k.name, JamLevel.PROFI, proTopics(k)) { seed -> ProGen.level(k, seed) } }
    /** Aufgabenarten zu Thema und Stufe ([level] null = alle Stufen) */
    fun sources(topic: JamTopic, level: JamLevel?) = sources.filter { (level == null || it.tier == level) && (topic == JamTopic.MIX || topic in it.topics) }
    fun count(topic: JamTopic, level: JamLevel?) = sources(topic, level).size
    fun available(topic: JamTopic, level: JamLevel?) = count(topic, level) > 0
    /** Aufgabe Nr. [n] einer Übungsrunde zum Thema in der gewählten Stufe */
    fun level(topic: JamTopic, level: JamLevel?, session: Long, n: Int): Level {
        val pool = sources(topic, level).ifEmpty { sources(topic, null) }
        return pool[Random(session * 131 + n).nextInt(pool.size)].make(session * 1000 + n)
    }

    private val NUMBER = Regex("""\d+(,\d+)?"""); private val PARAMETER = Regex("""(?<![A-Za-zÄÖÜäöüß₀-₉])[tsa](?![A-Za-zÄÖÜäöüß′⃗])""")
    /** Kennzahlen einer einzelnen Aufgabe, gelesen aus Rechenweg, Tipp und Bühne */
    fun metrics(l: Level): Metrics {
        val way = l.way.joinToString(" "); val numbers = NUMBER.findAll(way).map { it.value }.toList()
        val odd = numbers.count { ',' in it } + way.count { it == '√' }
        return Metrics(l.way.size.toDouble(), way.length.toDouble(), l.objects(l.solution, Phase.DONE).count { it.type != "drone" }.toDouble(),
            PARAMETER.findAll(way + " " + l.tip + " " + l.mission + " " + given(l)).map { it.value }.toSet().size.toDouble(), if (numbers.isEmpty()) 0.0 else odd.toDouble() / (numbers.size + way.count { it == '√' }))
    }
    /** Die Angaben der Aufgabe als Text (Geraden- und Ebenengleichungen) – dort stehen die Parameter */
    private fun given(l: Level) = l.formula(l.start).joinToString(" ") { line -> line.joinToString("") { p -> when (p) { is Part.Txt -> p.s; is Part.Num -> ""; is Part.Col -> " " } } }
    /** Stichprobe einer Stufe: jede Aufgabenart mit [perKind] verschiedenen Zahlen */
    fun sample(level: JamLevel, perKind: Int = 40): List<Level> = sources.filter { it.tier == level }.flatMap { src -> (1L..perKind).map { src.make(it) } }
    fun average(levels: List<Level>): Metrics { val m = levels.map(::metrics)
        return Metrics(m.map { it.steps }.average(), m.map { it.wayLength }.average(), m.map { it.objects }.average(), m.map { it.parameters }.average(), m.map { it.nonInteger }.average()) }
}

/** Profi-Aufgaben: mehrstufig (Lot + Abstand + Spiegelung, Kugel + Ebene), mit Parametern und krummen Zwischenwerten. Die Lösung ist trotzdem eine ganze Zahl bzw. ein Gitterpunkt. */
object ProGen {
    enum class Kind { MIRROR_DIST, SKEW_DIST, CUT_CIRCLE, MIRROR_LINE, PARALLEL_PARAM }

    private val B = Bounds()
    private fun inRoom(v: V3) = v.x >= B.min[0] && v.x <= B.max[0] && v.y >= B.min[1] && v.y <= B.max[1] && v.z >= B.min[2] && v.z <= B.max[2]
    private fun Random.point(pad: Int = 0) = V3(nextInt(pad, 9 - pad), nextInt(pad, 9 - pad), nextInt(pad, 7 - pad))
    private fun Random.dir(max: Int): V3 { while (true) { val d = V3(nextInt(-max, max + 1), nextInt(-max, max + 1), nextInt(-max, max + 1)); if (!d.isZero) return d } }
    private fun Random.sign() = if (nextBoolean()) 1 else -1
    /** Normalenvektoren der Länge 3 – damit bleiben Abstände ganzzahlig, die Zwischenwerte aber nicht */
    private val normals = listOf(V3(1, 2, 2), V3(2, 1, 2), V3(2, 2, 1), V3(2, -1, 2), V3(1, -2, 2), V3(2, 2, -1), V3(-1, 2, 2))
    private fun lineText(name: String, a: V3, d: V3, param: String = "t") = listOf(Txt("$name: x⃗ = "), col(a), Txt(" + $param · "), col(d))
    private fun int(v: Double) = Geo.num(v)
    /** Drittel als Bruch: 2 → „⅔“, −1 → „−⅓“, 3 → „1“ (die Lotfußpunkte der Profi-Aufgaben liegen oft auf Dritteln) */
    private fun third(k: Int): String = if (k % 3 == 0) int(k / 3.0) else (if (k < 0) "−" else "") + (if (kotlin.math.abs(k) >= 3) "${kotlin.math.abs(k) / 3} " else "") + (if (kotlin.math.abs(k) % 3 == 1) "⅓" else "⅔")
    /** Punkt hinter einem Namen: „(3|4|5)“, bei gerundeten Koordinaten „ ≈ (3,33|6,67|2,33)“ */
    private fun approx(p: V3) = (if (p.list.all { Geo.exact(it) }) "" else " ≈ ") + p.fmt()

    fun level(kind: Kind, seed: Long, id: String = "profi-${kind.name.lowercase()}-$seed"): Level {
        val r = Random(seed * 37 + kind.ordinal + 1000)
        repeat(20_000) { build(kind, r, id)?.let { return it } }
        error("Keine Profi-Aufgabe für $kind/$seed gefunden")
    }

    private fun build(kind: Kind, r: Random, id: String): Level? = when (kind) {
        Kind.MIRROR_DIST -> {
            val n = normals[r.nextInt(normals.size)]; val p = r.point(); val k = r.nextInt(1, 4); val sg = r.sign()
            val s = (n dot p).toInt(); val c = s + sg * 3 * k; val f = p + n * (sg * k / 3.0); val p2 = p + n * (sg * 2 * k / 3.0)
            if (!inRoom(f) || !inRoom(p2)) null else Families.scalar(id, 0, "Spiegelbild", "P wird an der Scheibe E gespiegelt. Wie weit liegen P und sein Spiegelbild P′ auseinander?", "d", 0..18, 2 * k, 1,
                tip = "Drei Schritte: Abstand von P zur Ebene mit |n⃗ · P − c| : |n⃗|, dann der Lotfußpunkt – und P′ liegt genauso weit dahinter.",
                way = listOf("n⃗ · P = ${int(s.toDouble())}, |n⃗| = 3", "Abstand P–E = |${Geo.minus(int(s.toDouble()), c.toDouble())}| : 3 = $k", "Lotfußpunkt F = P + t · n⃗ mit t = ${third(sg * k)}: F${approx(f)}", "P′ = 2 · F − P ${if (p2.list.all { Geo.exact(it) }) "=" else "≈"} ${p2.fmt()} → |PP′| = 2 · $k = ${2 * k}"),
                formula = { a -> listOf(listOf(Txt("P${p.fmt()}")), listOf(Txt("E: ${Geo.planeText(n, c)}")), listOf(Txt("|PP′| = "), Num(Geo.num(a), "a"))) },
                objects = { _, ph -> listOf(Stage.plane("E", n, c.toDouble(), "violet", "E"), Stage.point("P", p, "cyan", "P${p.fmt()}")) +
                    if (ph == Phase.DONE) listOf(Stage.point("F", f, "pink", "F"), Stage.point("P2", p2, "yellow", "P′"), Stage.line("lot", p, p2 - p, 0.0, 1.0, "pink")) else listOf(Stage.stub("richtung", p, n * sg, "pink", length = 0.9)) },
                check = { a -> Families.compare(a, 2 * k, "Die Entfernung", "${2 * k} Einheiten: $k bis zur Scheibe und $k dahinter.", mapOf(k to "$k ist der Abstand von P zur Scheibe. P′ liegt genauso weit dahinter – also das Doppelte.")) })
        }
        Kind.SKEW_DIST -> {
            val n = normals[r.nextInt(normals.size)]; val d1 = r.dir(2); val d2 = r.dir(2)
            val nn = d1 cross d2; val a = r.point(); val k = r.nextInt(1, 3); val b = a + n * (k * r.sign()) + d2 * r.nextInt(-1, 2)
            val rel = if (nn.isZero) null else Geo.lineLine(a, d1, b, d2)
            if (abs(d1 dot n) > 1e-9 || abs(d2 dot n) > 1e-9 || nn.isZero || !inRoom(b) || rel == null || rel.point != null || abs(rel.distance - 3 * k) > 1e-9) null else {
                val fg = a + d1 * rel.s; val fh = b + d2 * rel.t; val dot = ((b - a) dot nn); val len = nn.length
                if (!inRoom(fg) || !inRoom(fh)) null else Families.scalar(id, 0, "Windschief", "Die Traversen g und h laufen windschief aneinander vorbei. Wie groß ist ihr kleinster Abstand?", "d", 0..9, 3 * k, 1,
                    tip = "n⃗ = d⃗₁ × d⃗₂ steht auf beiden Richtungen senkrecht. Abstand = |(B − A) · n⃗| : |n⃗| mit den Stützpunkten A und B.",
                    way = listOf("n⃗ = d⃗₁ × d⃗₂ = ${d1.fmt()} × ${d2.fmt()} = ${nn.fmt()}", "|n⃗| = √${int(nn dot nn)} = ${int(len)}", "(B − A) · n⃗ = ${(b - a).fmt()} · ${nn.fmt()} = ${int(dot)}", "d = |${int(dot)}| : ${int(len)} = ${3 * k}"),
                    formula = { v -> listOf(lineText("g", a, d1, "s"), lineText("h", b, d2), listOf(Txt("d(g, h) = "), Num(Geo.num(v), "a"))) },
                    objects = { _, ph -> listOf(Stage.fullLine("g", a, d1, B, "cyan", "g"), Stage.fullLine("h", b, d2, B, "yellow", "h"), Stage.point("A", a, "cyan", "A"), Stage.point("B", b, "yellow", "B")) +
                        if (ph == Phase.DONE) listOf(Stage.line("lot", fg, fh - fg, 0.0, 1.0, "pink"), Stage.point("Fg", fg, "pink"), Stage.point("Fh", fh, "pink")) else emptyList() },
                    check = { v -> Families.compare(v, 3 * k, "Der Abstand", "${3 * k} Einheiten – kürzer geht es nicht.") { ok -> listOf(Beam(fg, fg + (fh - fg) * (v / (3.0 * k)), "pink", ok)) } })
            }
        }
        Kind.CUT_CIRCLE -> {
            val (d, rp, r2) = listOf(Triple(3, 4, 25), Triple(4, 3, 25), Triple(3, 2, 13), Triple(2, 3, 13), Triple(1, 2, 5), Triple(2, 1, 5), Triple(2, 4, 20), Triple(4, 2, 20), Triple(1, 3, 10), Triple(3, 1, 10)).let { it[r.nextInt(it.size)] }
            val n = normals[r.nextInt(normals.size)]; val m = r.point(1); val sg = r.sign(); val s = (n dot m).toInt(); val c = s + sg * 3 * d; val m2 = m + n * (sg * d / 3.0)
            if (!inRoom(m2)) null else Families.scalar(id, 0, "Schnittkreis", "Die Scheibe E schneidet die Lichtkugel K in einem Kreis. Welchen Radius r′ hat er?", "r′", 0..6, rp, 0,
                tip = "Erst den Abstand d des Mittelpunkts M von der Ebene, dann Pythagoras: r′² = r² − d².",
                way = listOf("n⃗ · M = ${int(s.toDouble())}, |n⃗| = 3", "d = |${Geo.minus(int(s.toDouble()), c.toDouble())}| : 3 = $d", "r′² = r² − d² = $r2 − ${d * d} = ${rp * rp}", "r′ = $rp, Kreismittelpunkt M′${approx(m2)}"), color = "yellow",
                formula = { a -> listOf(listOf(Txt("K: M${m.fmt()}, r² = $r2")), listOf(Txt("E: ${Geo.planeText(n, c)}")), listOf(Txt("r′ = "), Num(Geo.num(a), "a"))) },
                objects = { a, ph -> listOf(Stage.plane("E", n, c.toDouble(), "violet", "E"), Stage.sphere("K", m, sqrt(r2.toDouble()), "cyan"), Stage.point("M", m, "white", "M", drop = false)) +
                    (if (a > 0) listOf(Stage.circle("kreis", m2, n, a.toDouble(), "yellow")) else emptyList()) + if (ph == Phase.DONE) listOf(Stage.point("M2", m2, "pink", "M′", drop = false)) else emptyList() },
                check = { a -> Families.compare(a, rp, "Der Radius", "r′ = $rp – der Kreis liegt genau auf der Kugel.", if (rp * rp <= 6 && rp > 1) mapOf(rp * rp to "${rp * rp} ist r′². Zieh noch die Wurzel.") else emptyMap()) })
        }
        Kind.MIRROR_LINE -> {
            val a = r.point(); val d = r.dir(1); val t = r.nextInt(1, 3); val f = a + d * t; val w = r.dir(2); val p = f + w; val p2 = f - w
            if (abs(w dot d) > 1e-9 || d.ints.count { it != 0 } < 2 || !inRoom(f) || !inRoom(p) || !inRoom(p2)) null else Families.point(id, 0, "An der Geraden spiegeln", "Der Laserstrahl g ist der Spiegel. Wo landet das Spiegelbild von P?",
                p2, hidden = true, name = "P′",
                given = listOf(listOf(Txt("P${p.fmt()}")), lineText("g", a, d)),
                mistakes = listOf(f to "Das ist der Lotfußpunkt F. Das Spiegelbild liegt genauso weit dahinter: P′ = 2 · F − P."),
                tip = "Erst den Lotfußpunkt F auf g: (P − A − t · d⃗) · d⃗ = 0 nach t auflösen. Dann P′ = 2 · F − P.",
                way = listOf("(P − A) · d⃗ = ${(p - a).fmt()} · ${d.fmt()} = ${int((p - a) dot d)}, d⃗ · d⃗ = ${int(d dot d)}", "t = ${int((p - a) dot d)} : ${int(d dot d)} = $t", "F = A + $t · d⃗ = ${f.fmt()}", "P′ = 2 · F − P = ${p2.fmt()}"),
                extras = { ph -> listOf(Stage.fullLine("g", a, d, B, "cyan", "g"), Stage.point("P", p, "cyan", "P${p.fmt()}")) +
                    if (ph == Phase.DONE) listOf(Stage.point("F", f, "pink", "F${f.fmt()}"), Stage.line("weg", p, p2 - p, 0.0, 1.0, "yellow", dashed = true)) else emptyList() })
        }
        Kind.PARALLEL_PARAM -> {
            val n = normals[r.nextInt(normals.size)]; val p = r.nextInt(-3, 4); val q = r.nextInt(-3, 4); val rest = (n.x * p + n.z * q).toInt(); val ny = n.y.toInt()
            val a0 = r.point(); val c = (n dot r.point(1)).toInt(); val sol = if (ny != 0 && rest % ny == 0) -rest / ny else 99
            if (p == 0 || q == 0 || sol == 0 || abs(sol) > 4 || abs((n dot a0) - c) < 1e-9) null else Families.scalar(id, 0, "Parallel mit Parameter", "Die Laserbahn g soll die Scheibe E nie treffen. Die mittlere Zahl im Richtungsvektor ist a (farbig markiert) – für welches a läuft g parallel zu E?", "a", -4..4, sol, 0,
                tip = "Parallel heißt: Die Richtung von g steht senkrecht auf dem Normalenvektor – Skalarprodukt null. Danach prüfen, dass A nicht in E liegt.",
                way = listOf("d⃗ · n⃗ = ${int(n.x)} · ${Geo.par(p)} + ${Geo.par(n.y)} · a + ${Geo.par(n.z)} · ${Geo.par(q)}", "${Geo.plus((if (ny == 1) "" else if (ny == -1) "−" else int(n.y) + " · ") + "a", rest.toDouble())} = 0", "a = ${int(sol.toDouble())}", "Probe: n⃗ · A = ${int(n dot a0)} ≠ ${int(c.toDouble())}, A liegt nicht in E – also echt parallel"), color = "cyan",
                formula = { a -> listOf(listOf(Txt("g: x⃗ = "), col(a0), Txt(" + t · "), Part.Col(listOf(int(p.toDouble()), Geo.num(a), int(q.toDouble())), "a")), listOf(Txt("E: ${Geo.planeText(n, c)}"))) },
                objects = { a, _ -> listOf(Stage.plane("E", n, c.toDouble(), "violet", "E"), Stage.fullLine("g", a0, V3(p, a, q), B, "cyan", "g"), Stage.point("A", a0, "cyan", "A${a0.fmt()}")) },
                check = { a -> Families.compare(a, sol, "a", "a = ${int(sol.toDouble())} – der Strahl läuft parallel an der Scheibe vorbei.") })
        }
    }
}
