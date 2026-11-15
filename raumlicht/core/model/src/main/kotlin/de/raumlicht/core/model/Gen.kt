package de.raumlicht.core.model

import de.raumlicht.core.model.Part.Txt
import kotlin.random.Random

/** Themen der Jam-Session. */
enum class JamTopic(val title: String, val emoji: String, val color: String, val kinds: List<Gen.Kind>) {
    PUNKTE("Punkte & Vektoren", "🛸", "cyan", listOf(Gen.Kind.MIDPOINT, Gen.Kind.VECTOR)),
    GERADEN("Geraden", "🔦", "green", listOf(Gen.Kind.AIM, Gen.Kind.PARAM, Gen.Kind.TRACE)),
    SCHNITT("Schnittpunkte", "✨", "yellow", listOf(Gen.Kind.CROSS)),
    EBENEN("Ebenen", "🔷", "violet", listOf(Gen.Kind.PIERCE_FLAT, Gen.Kind.PIERCE_SKEW)),
    WINKEL("Winkel, Abstände, Kugeln", "📐", "orange", listOf(Gen.Kind.ANGLE, Gen.Kind.DIST_PLANE, Gen.Kind.FOOT, Gen.Kind.RADIUS)),
    BEWEGUNG("Bewegung & Rotation", "🛫", "green", listOf(Gen.Kind.MOTION_POS, Gen.Kind.MOTION_SPEED, Gen.Kind.MOTION_TIME, Gen.Kind.ROTATION)),
    MIX("Alles gemischt", "🎧", "pink", Gen.Kind.entries),
}

/**
 * Erzeugt aus einer Zahl (Seed) immer dieselbe Aufgabe – mit ganzzahliger Lösung, die im Raum liegt.
 * Grundlage für Jam-Session (endlos üben) und Tagesgig (jeden Tag eine neue Aufgabe für alle).
 */
object Gen {
    enum class Kind { MIDPOINT, VECTOR, AIM, PARAM, TRACE, CROSS, PIERCE_FLAT, PIERCE_SKEW, ANGLE, DIST_PLANE, FOOT, RADIUS, MOTION_POS, MOTION_SPEED, MOTION_TIME, ROTATION }

    private val B = Bounds()
    private fun inRoom(v: V3) = (0..2).all { v.list[it] >= B.min[it] && v.list[it] <= B.max[it] }
    private fun Random.point(lo: Int = 0, pad: Int = 0) = V3(nextInt(lo + pad, 9 - pad), nextInt(lo + pad, 9 - pad), nextInt(lo, 7 - pad))
    private fun Random.dir(max: Int = 2): V3 { while (true) { val d = V3(nextInt(-max, max + 1), nextInt(-max, max + 1), nextInt(-max, max + 1)); if (!d.isZero) return d } }
    private fun gcd(a: Int, b: Int): Int = if (b == 0) kotlin.math.abs(a) else gcd(b, a % b)
    private fun V3.reduced(): V3 { val g = ints.fold(0, ::gcd); return if (g <= 1) this else V3(ints[0] / g, ints[1] / g, ints[2] / g) }
    private val normals = listOf(V3(1, 1, 1), V3(1, 1, 0), V3(1, 0, 1), V3(0, 1, 1), V3(1, 2, 2), V3(2, 1, 2), V3(1, 1, 2), V3(2, 1, 1))

    fun level(kind: Kind, seed: Long, id: String = "jam-${kind.name.lowercase()}-$seed", chapter: Int = 0): Level {
        val r = Random(seed * 31 + kind.ordinal)
        repeat(10_000) {
            build(kind, r, id, chapter)?.let { return it }
        }
        error("Keine Aufgabe für $kind/$seed gefunden")
    }

    private fun build(kind: Kind, r: Random, id: String, ch: Int): Level? = when (kind) {
        Kind.MIDPOINT -> {
            val m = r.point(1, 1); val h = r.dir(3); val a = m - h; val b = m + h
            if (!inRoom(a) || !inRoom(b)) null else Families.point(id, ch, "Mittelpunkt", "Zwischen A und B hängt ein Kabel. Flieg mich genau in die Mitte.", m, hidden = true, name = "M",
                given = listOf(listOf(Txt("A${a.fmt()}   B${b.fmt()}"))),
                tip = "Jede Koordinate einzeln mitteln: (a + b) : 2.",
                way = listOf("M = ½ · (A + B)", "M${m.fmt()}"),
                extras = { listOf(Stage.point("A", a, "cyan", "A${a.fmt()}"), Stage.point("B", b, "cyan", "B${b.fmt()}"), Stage.edges("kabel", listOf(a to b), "cyan")) })
        }
        Kind.VECTOR -> {
            val a = r.point(); val b = r.point(); val v = b - a
            if (v.ints.count { it != 0 } < 2) null else Families.vector(id, ch, "Verbindungsvektor", "Welcher Vektor bringt mich von A nach B?", a, b,
                tip = "Spitze minus Fuß: B − A, Zeile für Zeile.", way = listOf("v⃗ = B − A", "v⃗ = ${v.fmt()}"))
        }
        Kind.AIM -> {
            val a = r.point(); val z = r.point(); val d = z - a
            if (d.ints.count { it != 0 } < 2 || d.ints.any { kotlin.math.abs(it) > 6 }) null else Families.aim(id, ch, "Zielen", "Gib dem Laser in A die Richtung zum Ring.", a, listOf(z),
                // Startrichtung: liegt im Raum und trifft nicht schon
                start = listOf(V3(1, 0, 0), V3(-1, 0, 0), V3(0, 1, 0), V3(0, -1, 0), V3(0, 0, 1)).first { inRoom(a + it) && !Geo.parallel(it, d) },
                tip = "Richtungsvektor = Ziel − Stützpunkt.", way = listOf("d⃗ = Z − A = ${d.fmt()}") + if (d.reduced() != d) listOf("kürzer: ${d.reduced().fmt()}") else emptyList())
        }
        Kind.PARAM -> {
            val d = r.dir(2); val t = listOf(-3, -2, -1, 2, 3, 4).random(r); val a = r.point(); val z = a + d * t
            if (!inRoom(z) || d.ints.count { it != 0 } < 2) null else {
                val i = d.ints.indexOfFirst { it != 0 }
                Families.param(id, ch, "Parameter", "Für welches t lande ich genau im Ring?", a, d, t, -4..6,
                    tip = "Eine Zeile reicht, um t zu finden. Die anderen sind die Probe.",
                    way = listOf("x${"₁₂₃"[i]}: ${Geo.num(a.ints[i])} + t · ${if (d.ints[i] < 0) "(${Geo.num(d.ints[i])})" else Geo.num(d.ints[i])} = ${Geo.num(z.ints[i])}", "t = ${Geo.num(t)}"))
            }
        }
        Kind.TRACE, Kind.PIERCE_FLAT -> {
            val axis = if (kind == Kind.TRACE) 2 else r.nextInt(3); val k = if (kind == Kind.TRACE) 0 else r.nextInt(2, if (axis == 2) 6 else 8)
            val n = V3(if (axis == 0) 1 else 0, if (axis == 1) 1 else 0, if (axis == 2) 1 else 0)
            val s0 = r.point(); val s = V3(if (axis == 0) k else s0.ints[0], if (axis == 1) k else s0.ints[1], if (axis == 2) k else s0.ints[2])
            val d = r.dir(2); val t = r.nextInt(1, 4); val a = s - d * t
            if (s.isZero || d.ints[axis] == 0 || d.ints.count { it != 0 } < 2 || !inRoom(a)) null else {
                val text = "x${"₁₂₃"[axis]} = $k"
                Families.pierce(id, ch, if (kind == Kind.TRACE) "Spurpunkt" else "Durchstoßpunkt",
                    if (kind == Kind.TRACE) "Wo trifft der Laser den Boden? Stell den Sensor dorthin." else "Wo stößt der Laser durch die Scheibe E?", a, d, n, k,
                    tip = "In E gilt $text. Diese Zeile der Geraden liefert t.",
                    way = listOf("$text: ${Geo.num(a.ints[axis])} + t · ${if (d.ints[axis] < 0) "(${Geo.num(d.ints[axis])})" else Geo.num(d.ints[axis])} = $k → t = $t", "S = A + $t · d⃗ = ${s.fmt()}"),
                    planeText = if (kind == Kind.TRACE) "x₃ = 0 (Boden)" else text)
            }
        }
        Kind.PIERCE_SKEW -> {
            val n = normals.random(r); val s = r.point(0, 1); val rhs = (n dot s).toInt()
            val d = r.dir(2); val t = r.nextInt(1, 4); val a = s - d * t
            if (s.isZero || (n dot d) == 0.0 || d.ints.count { it != 0 } < 2 || !inRoom(a)) null else Families.pierce(id, ch, "Schräge Scheibe", "Setz die Gerade in die Ebenengleichung ein: Wo ist der Durchstoßpunkt?", a, d, n, rhs,
                tip = "Jede Koordinate der Geraden in E einsetzen, nach t auflösen.",
                way = listOf("einsetzen → ${Geo.num((n dot a).toInt())} + ${Geo.num((n dot d).toInt())} · t = $rhs", "t = $t", "S = A + $t · d⃗ = ${s.fmt()}"))
        }
        Kind.ANGLE, Kind.DIST_PLANE, Kind.FOOT, Kind.RADIUS -> buildMore(kind, r, id, ch)
        Kind.MOTION_POS, Kind.MOTION_SPEED, Kind.MOTION_TIME, Kind.ROTATION -> buildMotion(kind, r, id, ch)
        Kind.CROSS -> {
            val s = r.point(0, 1); val d1 = r.dir(2); val d2 = r.dir(2)
            val u = listOf(-3, -2, -1, 1, 2, 3).random(r); val w = listOf(-3, -2, -1, 1, 2, 3).random(r)
            val a = s - d1 * u; val b = s - d2 * w
            if (s.isZero || Geo.parallel(d1, d2) || !inRoom(a) || !inRoom(b) || d1.ints.count { it != 0 } < 2 || d2.ints.count { it != 0 } < 2 || u < 0 || w < 0) null
            else Families.crossing(id, ch, "Kreuzung", "Wo kreuzen sich die Strahlen g und h?", a, d1, b, d2,
                tip = "Gleichsetzen und aus zwei Zeilen s und t bestimmen. Die dritte Zeile ist die Probe.",
                way = listOf("gleichsetzen → s = $u, t = $w", "S = A + $u · d⃗ = ${s.fmt()}"))
        }
    }

    // Vektorpaare mit „schönen“ Winkeln und Normalen/Versätze mit ganzzahliger Länge
    private val anglePairs = listOf(Triple(V3(1, 0, 0), V3(1, 1, 0), 45), Triple(V3(1, 1, 0), V3(0, 1, 1), 60), Triple(V3(1, 0, 0), V3(0, 1, 0), 90),
        Triple(V3(1, 1, 0), V3(1, -1, 0), 90), Triple(V3(1, 1, 0), V3(0, -1, -1), 120), Triple(V3(1, 0, 0), V3(-1, 1, 0), 135))
    private val wholeNormals = listOf(V3(1, 2, 2), V3(2, 1, 2), V3(2, 2, 1), V3(0, 0, 1), V3(0, 1, 0), V3(1, 0, 0))
    private fun V3.perm(k: Int) = when (k % 3) { 0 -> this; 1 -> V3(z, x, y); else -> V3(y, z, x) }

    private fun buildMore(kind: Kind, r: Random, id: String, ch: Int): Level? = when (kind) {
        Kind.ANGLE -> {
            val (u0, v0, deg) = anglePairs.random(r); val k = r.nextInt(3); val u = u0.perm(k) * r.nextInt(2, 5); val v = v0.perm(k) * r.nextInt(2, 5); val o = r.point(0, 1)
            if (!inRoom(o + u) || !inRoom(o + v)) null else Families.scalar(id, ch, "Winkel", "Wie groß ist der Winkel zwischen a⃗ und b⃗?", "α", 0..12, deg / 15, 0,
                tip = "cos α = (a⃗ · b⃗) : (|a⃗| · |b⃗|). Ist das Skalarprodukt negativ, ist der Winkel größer als 90°.",
                way = listOf("a⃗ · b⃗ = ${Geo.num(u dot v)}", "|a⃗| · |b⃗| = ${Geo.num(u.length * v.length)}", "α = $deg°"), color = "yellow", step = 15, suffix = "°",
                formula = { a -> listOf(listOf(Txt("a⃗ = "), Families.col(u), Txt("   b⃗ = "), Families.col(v)), listOf(Txt("α = "), Part.Num("${a * 15}°", "a"))) },
                objects = { _, _ -> listOf(Stage.point("O", o, "white"), Stage.vector("a", o, o + u, "cyan", "a⃗"), Stage.vector("b", o, o + v, "pink", "b⃗"), Stage.arc("bogen", o, u, v, 1.5)) },
                check = { a -> Families.compare(a * 15, deg, "Der Winkel", "Genau $deg°!") })
        }
        Kind.DIST_PLANE, Kind.FOOT -> {
            val n = wholeNormals.random(r); val f = r.point(0, 1); val k = r.nextInt(1, if (n.length > 1.5) 3 else 5); val q = f + n * k; val rhs = (n dot f).toInt()
            val dist = (k * n.length).toInt(); val text = Geo.planeText(n, rhs)
            if (!inRoom(q) || dist > 8) null else if (kind == Kind.FOOT) Families.point(id, ch, "Lotfußpunkt", "Wo trifft der kürzeste Weg von P die Scheibe E?", f, hidden = true, name = "F",
                given = listOf(listOf(Txt("P${q.fmt()}   E: $text"))),
                tip = "Lotgerade durch P mit dem Normalenvektor als Richtung, in E einsetzen.",
                way = listOf("Lotgerade: x⃗ = P + t · ${n.fmt()}", "in E einsetzen → t = ${Geo.num(-k)}", "F${f.fmt()}"),
                extras = { listOf(Stage.plane("E", n, rhs.toDouble(), "violet", "E"), Stage.point("P", q, "cyan", "P${q.fmt()}")) })
            else Families.scalar(id, ch, "Abstand Punkt–Ebene", "Wie weit ist P von der Scheibe E entfernt?", "d", 0..8, dist, if (dist == 1) 2 else 1,
                tip = "d = |n⃗ · p⃗ − c| : |n⃗|.",
                way = listOf("n⃗ · p⃗ = ${Geo.num(n dot q)}, |n⃗| = ${Geo.num(n.length)}", "d = |${Geo.num(n dot q)} − ${Geo.num(rhs)}| : ${Geo.num(n.length)} = $dist"),
                formula = { a -> listOf(listOf(Txt("P${q.fmt()}   E: $text")), listOf(Txt("d(P, E) = "), Part.Num(Geo.num(a), "a"))) },
                objects = { _, ph -> listOf(Stage.plane("E", n, rhs.toDouble(), "violet", "E"), Stage.point("P", q, "cyan", "P${q.fmt()}")) +
                    if (ph == Phase.DONE) listOf(Stage.line("lot", q, f - q, 0.0, 1.0, "pink")) else listOf(Stage.stub("richtung", q, f - q, "pink", length = 0.9)) },
                check = { a -> Families.compare(a, dist, "Der Abstand", "Genau $dist – der Strahl endet auf dem Glas.") { ok -> listOf(Beam(q, q + (f - q) * (a.toDouble() / dist), "pink", ok)) } })
        }
        Kind.RADIUS -> {
            val off = (wholeNormals.filter { it.length > 1.5 } + listOf(V3(2, 0, 0), V3(0, 2, 0), V3(0, 0, 2), V3(3, 0, 0), V3(0, 0, 3))).random(r) * (if (r.nextBoolean()) 1 else -1)
            val m = r.point(0, 2); val rad = off.length.toInt(); val q = m + off
            if (!inRoom(q) || m.z - rad < 0 || m.z + rad > 6) null else Families.scalar(id, ch, "Kugelradius", "Stell den Radius so ein, dass P auf der Kugel um M liegt.", "r", 1..5, rad, if (rad == 1) 2 else 1,
                tip = "Der Radius ist die Länge von MP⃗.", way = listOf("MP⃗ = ${off.fmt()}", "r = ${Geo.num(rad)}"), color = "cyan",
                formula = { a -> listOf(listOf(Txt("M${m.fmt()}   P${q.fmt()}")), listOf(Txt("r = "), Part.Num(Geo.num(a), "a"))) },
                objects = { a, _ -> listOf(Stage.sphere("K", m, a.toDouble(), "cyan"), Stage.point("M", m, "white", "M", drop = false), Stage.point("P", q, "yellow", "P${q.fmt()}")) },
                check = { a -> Families.compare(a, rad, "Der Radius", "r = $rad – P liegt auf der Kugel.") })
        }
        else -> null
    }

    private val wholeSpeeds = listOf(V3(1, 2, 2), V3(2, 1, 2), V3(2, 2, 1), V3(0, 3, 4), V3(3, 4, 0), V3(4, 0, 3), V3(2, 0, 0), V3(0, 0, 3))
    /** Rotationskörper mit „schönem“ Volumen: Funktionstext, f, obere Grenze, Volumen in Vielfachen von π, Schrittweite, Rechenweg */
    private class Turn(val text: String, val to: Int, val piTimes: Int, val step: Int, val way: List<String>, val f: (Double) -> Double)
    private val turns = listOf(
        Turn("1", 3, 3, 1, listOf("f(x)² = 1", "∫₀³ 1 dx = 3", "V = 3π")) { 1.0 },
        Turn("2", 2, 8, 4, listOf("f(x)² = 4", "∫₀² 4 dx = 8", "V = 8π")) { 2.0 },
        Turn("2", 3, 12, 4, listOf("f(x)² = 4", "∫₀³ 4 dx = 12", "V = 12π")) { 2.0 },
        Turn("3", 2, 18, 9, listOf("f(x)² = 9", "∫₀² 9 dx = 18", "V = 18π")) { 3.0 },
        Turn("x", 3, 9, 3, listOf("f(x)² = x², Stammfunktion ⅓x³", "[⅓x³]₀³ = 9", "V = 9π")) { it },
        Turn("√x", 2, 2, 1, listOf("f(x)² = x, Stammfunktion ½x²", "[½x²]₀² = 2", "V = 2π")) { kotlin.math.sqrt(it) },
        Turn("√x", 4, 8, 2, listOf("f(x)² = x, Stammfunktion ½x²", "[½x²]₀⁴ = 8", "V = 8π")) { kotlin.math.sqrt(it) },
        Turn("√(x + 1)", 2, 4, 2, listOf("f(x)² = x + 1, Stammfunktion ½x² + x", "[½x² + x]₀² = 4", "V = 4π")) { kotlin.math.sqrt(it + 1) },
        Turn("√(x + 1)", 4, 12, 4, listOf("f(x)² = x + 1, Stammfunktion ½x² + x", "[½x² + x]₀⁴ = 12", "V = 12π")) { kotlin.math.sqrt(it + 1) },
    )

    private fun buildMotion(kind: Kind, r: Random, id: String, ch: Int): Level? = when (kind) {
        Kind.MOTION_POS -> {
            val a = r.point(); val v = r.dir(2); val t = r.nextInt(2, 5); val q = a + v * t
            if (!inRoom(q) || q.isZero || v.ints.count { it != 0 } < 2) null else Families.point(id, ch, "Flugplan", "Ich starte in A und fliege jede Sekunde um v⃗ weiter. Wo bin ich nach $t Sekunden?", q, hidden = true, home = a,
                given = listOf(listOf(Txt("A${a.fmt()},  v⃗ = "), Families.col(v), Txt("  pro Sekunde"))),
                tip = "Ort zur Zeit t: x⃗ = A + t · v⃗.", way = listOf("x⃗($t) = A + $t · v⃗", "P${q.fmt()}"),
                extras = { listOf(Stage.point("A", a, "cyan", "A"), Stage.vector("v", a, a + v, "pink", "v⃗")) })
        }
        Kind.MOTION_SPEED -> {
            val v = wholeSpeeds.random(r).let { V3(it.x * listOf(-1, 1).random(r), it.y * listOf(-1, 1).random(r), it.z) }; val a = r.point(0, 1); val sp = v.length.toInt()
            if (!inRoom(a + v)) null else Families.scalar(id, ch, "Tempo", "Wie schnell fliege ich, wenn ich pro Sekunde um v⃗ weiterkomme?", "v", 0..8, sp, 1,
                tip = "Tempo = Länge des Vektors.", way = listOf("|v⃗| = √(${v.ints.joinToString(" + ") { (it * it).toString() }})", "= $sp Einheiten pro Sekunde"),
                formula = { x -> listOf(listOf(Txt("v⃗ = "), Families.col(v)), listOf(Txt("|v⃗| = "), Part.Num(Geo.num(x), "a"))) },
                objects = { _, _ -> listOf(Stage.point("A", a, "cyan", "A"), Stage.vector("v", a, a + v, "pink", "v⃗")) },
                check = { x -> Families.compare(x, sp, "Das Tempo", "$sp Einheiten pro Sekunde.") })
        }
        Kind.MOTION_TIME -> {
            val v = r.dir(2); val t = r.nextInt(2, 6); val a = r.point(); val q = a + v * t; val h = q.ints[2]
            if (!inRoom(q) || v.ints[2] == 0 || v.ints.count { it != 0 } < 2 || h == 0) null else Families.param(id, ch, "Flughöhe", "Nach wie vielen Sekunden erreiche ich die Höhe $h?", a, v, t, 0..6, hidden = true,
                tip = "Höhe ist die x₃-Koordinate: ${Geo.num(a.ints[2])} + t · ${if (v.ints[2] < 0) "(${Geo.num(v.ints[2])})" else Geo.num(v.ints[2])} = $h.",
                way = listOf("x₃(t) = $h", "t = $t → Ort ${q.fmt()}"), extras = { listOf(Stage.plane("E", V3(0, 0, 1), h.toDouble(), "violet", "x₃ = $h")) })
        }
        Kind.ROTATION -> {
            val k = turns.random(r); val origin = V3(4.0, 1.0 + r.nextInt(0, 3), 3.0)
            val profile = (0..28).map { val x = k.to * it / 28.0; x to k.f(x) }
            Families.scalar(id, ch, "Rotationskörper", "Die Kurve f dreht sich um die x-Achse. Welches Volumen hat der Körper?", "V", 0..(k.piTimes / k.step + 4), k.piTimes / k.step, 0,
                tip = "V = π · ∫ f(x)² dx. Erst f(x)² ausrechnen, dann integrieren.", way = k.way, color = "violet", step = k.step, suffix = "π",
                formula = { x -> listOf(listOf(Txt("f(x) = ${k.text},  x von 0 bis ${k.to}")), listOf(Txt("V = π · ∫ f(x)² dx = "), Part.Num("${x * k.step}π", "a"))) },
                objects = { _, _ -> listOf(Stage.lathe("körper", origin, profile)) },
                check = { x -> Families.compare(x * k.step, k.piTimes, "Das Volumen", "V = ${k.piTimes}π.") })
        }
        else -> null
    }

    /** Aufgabe Nr. [n] einer Jam-Session zum Thema (für jede Sitzung andere Aufgaben über [session]). */
    fun jam(topic: JamTopic, session: Long, n: Int): Level {
        val kind = topic.kinds[Random(session * 131 + n).nextInt(topic.kinds.size)]
        return level(kind, session * 1000 + n)
    }
}

/** Tagesgig: jeden Tag eine Aufgabe, für alle dieselbe. Die Themen wechseln reihum. */
object Daily {
    const val PREFIX = "tag-"
    fun id(day: Long) = "$PREFIX$day"
    fun isDaily(id: String) = id.startsWith(PREFIX)
    fun forDay(day: Long): Level {
        val kind = Gen.Kind.entries[(day % Gen.Kind.entries.size).toInt()]
        return Gen.level(kind, day * 7919 + 13, id(day))
    }
}

/** Abzeichen – für Fortschritt, nie fürs bloße Wiederkommen. */
data class Badge(val id: String, val emoji: String, val title: String, val text: String, val earned: Boolean)

object Badges {
    fun all(stars: Map<String, Int>, jamBest: Int): List<Badge> {
        val tour = stars.filterKeys { Levels.byId(it) != null }
        val dailies = stars.keys.count { Daily.isDaily(it) }
        fun chapterDone(n: Int) = Levels.chapters.first { it.number == n }.levels.all { (tour[it.id] ?: 0) > 0 }
        val abi = Levels.all.filter { it.abi }
        return listOf(
            Badge("start", "🎬", "Erster Gig", "Ein Level geschafft", tour.isNotEmpty()),
            Badge("perfekt5", "🎯", "Rechnen statt raten", "5 Level beim ersten Versuch", tour.values.count { it == 3 } >= 5),
            Badge("perfekt20", "💎", "Präzisionsarbeit", "20 Level mit drei Sternen", tour.values.count { it == 3 } >= 20),
            Badge("geraden", "🔦", "Laser-Profi", "Kapitel 3 und 4 gespielt", chapterDone(3) && chapterDone(4)),
            Badge("ebenen", "🔷", "Glasklar", "Kapitel 5 gespielt", chapterDone(5)),
            Badge("kugel", "🔮", "Discokönig:in", "Kapitel 9 gespielt", chapterDone(9)),
            Badge("abi3", "🎓", "Abi-Luft", "3 Abi-Checks bestanden", abi.count { (tour[it.id] ?: 0) > 0 } >= 3),
            Badge("abi", "🏆", "Abi-ready", "Alle Abi-Checks bestanden", abi.all { (tour[it.id] ?: 0) > 0 }),
            Badge("halb", "🚐", "Halbe Tour", "Die Hälfte aller Gigs gespielt", tour.size * 2 >= Levels.all.size),
            Badge("tour", "🎆", "Tour geschafft", "Alle Gigs gespielt", tour.size == Levels.all.size),
            Badge("tag3", "📅", "Stammgast", "3 Tagesgigs gelöst", dailies >= 3),
            Badge("tag10", "🗓️", "Residency", "10 Tagesgigs gelöst", dailies >= 10),
            Badge("jam5", "🎧", "Im Flow", "Jam-Session: 5 in Folge beim ersten Versuch", jamBest >= 5),
            Badge("jam15", "🔥", "Unaufhaltsam", "Jam-Session: 15 in Folge", jamBest >= 15),
        )
    }
}

/** Spickzettel: die wichtigsten Formeln zum Nachschlagen, nach Kapiteln sortiert. */
/** [chapters]: Kapitel der Tour, in denen genau das geübt wird; [jam]: passendes Thema der Jam-Session */
data class SheetCard(val emoji: String, val title: String, val color: String, val lines: List<String>, val chapters: List<Int> = emptyList(), val jam: JamTopic? = null)

object Sheet {
    val cards = listOf(
        SheetCard("🛸", "Punkte & Vektoren", "cyan", listOf(
            "Verbindungsvektor: AB⃗ = B − A (Spitze minus Fuß)",
            "Mittelpunkt: M = ½ · (A + B)",
            "Schwerpunkt Dreieck: S = ⅓ · (A + B + C)",
            "Länge: |a⃗| = √(a₁² + a₂² + a₃²)"), listOf(1, 2, 11), JamTopic.PUNKTE),
        SheetCard("🔦", "Geraden", "green", listOf(
            "Parameterform: g: x⃗ = A + t · d⃗",
            "Richtungsvektor durch A und B: d⃗ = B − A",
            "Punktprobe: Punkt einsetzen – alle drei Zeilen müssen dasselbe t liefern",
            "Spurpunkt: eine Koordinate null setzen, t ausrechnen"), listOf(3), JamTopic.GERADEN),
        SheetCard("✨", "Lage zweier Geraden", "yellow", listOf(
            "Richtungsvektoren Vielfache? → parallel oder identisch",
            "Sonst gleichsetzen: Lösung → Schnittpunkt, Widerspruch → windschief",
            "Abstand windschief: d = |(B − A) · n⃗| : |n⃗| mit n⃗ = d⃗₁ × d⃗₂"), listOf(4, 8), JamTopic.SCHNITT),
        SheetCard("🔷", "Ebenen", "violet", listOf(
            "Koordinatenform: E: n₁x₁ + n₂x₂ + n₃x₃ = c",
            "Normalenvektor aus Spannvektoren: n⃗ = u⃗ × v⃗",
            "c bestimmen: einen Punkt der Ebene einsetzen",
            "Durchstoßpunkt: Gerade in E einsetzen, t ausrechnen",
            "Schnittgerade zweier Ebenen: Richtung n⃗₁ × n⃗₂"), listOf(5), JamTopic.EBENEN),
        SheetCard("📐", "Skalarprodukt & Winkel", "orange", listOf(
            "a⃗ · b⃗ = a₁b₁ + a₂b₂ + a₃b₃",
            "senkrecht ⇔ a⃗ · b⃗ = 0",
            "Vektoren / Ebenen: cos α = |a⃗ · b⃗| : (|a⃗| · |b⃗|)",
            "Gerade und Ebene: sin α = |n⃗ · d⃗| : (|n⃗| · |d⃗|)"), listOf(6, 7), JamTopic.WINKEL),
        SheetCard("📏", "Abstände & Spiegeln", "pink", listOf(
            "Punkt–Ebene: d = |n⃗ · p⃗ − c| : |n⃗|",
            "Lotfußpunkt auf Ebene: Lotgerade P + t · n⃗ in E einsetzen",
            "Lotfußpunkt auf Gerade: (P − A − t · d⃗) · d⃗ = 0",
            "Spiegelpunkt: P′ = P + 2 · PF⃗"), listOf(6, 8), JamTopic.WINKEL),
        SheetCard("🔮", "Kugeln", "cyan", listOf(
            "K: (x₁ − m₁)² + (x₂ − m₂)² + (x₃ − m₃)² = r²",
            "Gerade trifft Kugel: einsetzen → quadratische Gleichung (0, 1 oder 2 Treffer)",
            "Tangentialebene in B: n⃗ = MB⃗, dann B einsetzen",
            "Schnittkreis: r′² = r² − d² (d = Abstand der Ebene von M)"), listOf(9), JamTopic.WINKEL),
        SheetCard("🎆", "Kreuzprodukt", "green", listOf(
            "a⃗ × b⃗ = (a₂b₃ − a₃b₂ | a₃b₁ − a₁b₃ | a₁b₂ − a₂b₁)",
            "steht senkrecht auf a⃗ und b⃗",
            "Parallelogramm: A = |a⃗ × b⃗|, Dreieck: die Hälfte",
            "Pyramide (Parallelogramm-Grundfläche): V = ⅓ · |(a⃗ × b⃗) · c⃗|, Tetraeder: ⅙"), listOf(10)),
        SheetCard("🛫", "Bewegung", "orange", listOf(
            "Ort zur Zeit t: x⃗(t) = A + t · v⃗",
            "Geschwindigkeit: |v⃗|",
            "Treffen: beide Orte mit demselben t gleichsetzen",
            "Kleinster Abstand: d⃗(t) · (w⃗ − v⃗) = 0"), listOf(11), JamTopic.BEWEGUNG),
        SheetCard("🏺", "Rotationskörper", "violet", listOf(
            "V = π · ∫ f(x)² dx (Rotation um die x-Achse)",
            "Zylinder: πr²h · Kegel: ⅓πr²h · Kugel: ⁴⁄₃πr³"), listOf(12), JamTopic.BEWEGUNG),
    )
}
