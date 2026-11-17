package de.raumlicht.core.model

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Punkt oder Vektor im Raum, Schul-Schreibweise (x₁ | x₂ | x₃). */
data class V3(val x: Double, val y: Double, val z: Double) {
    constructor(x: Int, y: Int, z: Int) : this(x.toDouble(), y.toDouble(), z.toDouble())
    operator fun plus(o: V3) = V3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: V3) = V3(x - o.x, y - o.y, z - o.z)
    operator fun times(k: Double) = V3(x * k, y * k, z * k)
    operator fun times(k: Int) = times(k.toDouble())
    operator fun unaryMinus() = V3(-x, -y, -z)
    infix fun dot(o: V3) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: V3) = V3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    val length get() = sqrt(this dot this)
    val isZero get() = length < Geo.EPS
    val list get() = listOf(x, y, z)
    val isInt get() = list.all { abs(it - it.roundToInt()) < Geo.EPS }
    val ints get() = list.map { it.roundToInt() }
    fun near(o: V3) = (this - o).length < Geo.EPS
    /** „(4|−4|3)“ */
    fun fmt() = list.joinToString("|", "(", ")") { Geo.num(it) }
    companion object {
        val ZERO = V3(0, 0, 0)
        fun of(l: List<Int>) = V3(l[0], l[1], l[2])
    }
}

/** Lage zweier Geraden zueinander. */
enum class Relation { IDENTICAL, PARALLEL, INTERSECT, SKEW }
data class LineLine(val relation: Relation, val point: V3? = null, val s: Double = 0.0, val t: Double = 0.0, val distance: Double = 0.0)

/** Analytische Geometrie – alles, was die Level prüfen. */
object Geo {
    const val EPS = 1e-7

    /** Zahl in deutscher Schreibweise: echtes Minus, Komma, keine überflüssigen Nullen. */
    fun num(v: Double): String {
        if (v.isNaN()) return "nicht definiert"
        if (v.isInfinite()) return if (v > 0) "∞" else "−∞"
        val a = abs(v); val sign = if (v < 0) "−" else ""
        // Hundertstel als ganze Zahl – kaufmännisch am Betrag gerundet (0,125 → 0,13 und −0,125 → −0,13), ohne Überlauf bei großen Zahlen
        fun hundredths(h: Long): String { val frac = (h % 100).toInt()
            return (h / 100).toString() + if (frac == 0) "" else "," + (if (frac % 10 == 0) (frac / 10).toString() else frac.toString().padStart(2, '0')) }
        if (a >= 1e12) {
            // sehr große Beträge: lesbar als „4,85 · 10¹⁴“ statt mit „E“
            var e = kotlin.math.floor(kotlin.math.log10(a)).toInt(); var p10 = 1.0; repeat(e) { p10 *= 10 }
            var m = kotlin.math.floor(a / p10 * 100 + 0.5).toLong()
            if (m >= 1000) { m = (m + 5) / 10; e++ } else if (m < 100) { m *= 10; e-- }
            return "$sign${hundredths(m)} · 10${e.toString().map { "⁰¹²³⁴⁵⁶⁷⁸⁹"[it - '0'] }.joinToString("")}"
        }
        val h = kotlin.math.floor(a * 100 + 0.5).toLong()
        return if (h == 0L) "0" else sign + hundredths(h)
    }
    fun num(v: Int) = num(v.toDouble())
    /** Wie [num], aber negative Zahlen in Klammern – für Stellen hinter einem Rechenzeichen: „2 + (−1) · 3“ statt „2 + −1 · 3“ */
    fun par(v: Double) = if ((v * 100).roundToInt() < 0) "(${num(v)})" else num(v)
    fun par(v: Int) = par(v.toDouble())
    /** Zeigt [num] den Wert genau (höchstens zwei Nachkommastellen) – oder ist er gerundet? */
    fun exact(v: Double) = abs(v * 100 - (v * 100).roundToInt()) < 1e-6
    /** „= 4“ für genaue, „≈ 4,12“ für gerundete Werte */
    fun eq(v: Double) = (if (exact(v)) "= " else "≈ ") + num(v)
    /** „a − b“ bzw. „a + b“ ohne doppeltes Rechenzeichen: 11 und −1 → „11 + 1“ */
    fun minus(a: String, b: Double) = if (b < 0) "$a + ${num(-b)}" else "$a − ${num(b)}"
    fun plus(a: String, b: Double) = if (b < 0) "$a − ${num(-b)}" else "$a + ${num(b)}"
    /** Abstand eines Punktes vom Strahl ab [p] in Richtung [d] (liegt der Punkt hinter dem Start, zählt der Abstand zum Startpunkt) */
    fun distPointRay(q: V3, p: V3, d: V3) = if (((q - p) dot d) <= 0) (q - p).length else distPointLine(q, p, d)
    /** „1 Einheit“, „2 Einheiten“, „2,24 Einheiten“ */
    fun units(v: Double) = num(v) + if (num(v) == "1") " Einheit" else " Einheiten"
    /** Bogenmaß → Grad */
    fun deg(rad: Double) = rad * 180.0 / kotlin.math.PI

    /** Winkel zwischen zwei Vektoren in Grad */
    fun angle(a: V3, b: V3) = deg(kotlin.math.acos(((a dot b) / (a.length * b.length)).coerceIn(-1.0, 1.0)))
    /** „x₁ + 2x₂ − x₃ = 6“ */
    fun planeText(n: V3, rhs: Int): String {
        val names = listOf("x₁", "x₂", "x₃"); val sb = StringBuilder()
        n.ints.forEachIndexed { i, k ->
            if (k == 0) return@forEachIndexed
            val a = kotlin.math.abs(k); val f = if (a == 1) "" else a.toString()
            sb.append(if (sb.isEmpty()) (if (k < 0) "−" else "") else if (k < 0) " − " else " + ").append(f).append(names[i])
        }
        return "$sb = ${num(rhs)}"
    }

    fun parallel(a: V3, b: V3) = !a.isZero && !b.isZero && (a cross b).length < EPS
    fun sameDirection(a: V3, b: V3) = parallel(a, b) && (a dot b) > 0

    /** Parameter t, bei dem p + t·d die Ebene n·x = c trifft (null: parallel). */
    fun linePlane(p: V3, d: V3, n: V3, c: Double): Double? {
        val nd = n dot d
        return if (abs(nd) < EPS) null else (c - (n dot p)) / nd
    }

    fun lineLine(p1: V3, d1: V3, p2: V3, d2: V3): LineLine {
        val w = p2 - p1
        val n = d1 cross d2
        if (n.isZero) {
            val dist = (w cross d1).length / d1.length
            return LineLine(if (dist < EPS) Relation.IDENTICAL else Relation.PARALLEL, distance = dist)
        }
        val dist = abs(w dot n) / n.length
        // p1 + s·d1 = p2 + t·d2  →  s, t aus dem Kreuzprodukt
        val s = ((w cross d2) dot n) / (n dot n)
        val t = ((w cross d1) dot n) / (n dot n)
        return if (dist < EPS) LineLine(Relation.INTERSECT, p1 + d1 * s, s, t) else LineLine(Relation.SKEW, s = s, t = t, distance = dist)
    }

    fun distPointLine(q: V3, p: V3, d: V3) = ((q - p) cross d).length / d.length
    fun distPointPlane(q: V3, n: V3, c: Double) = abs((n dot q) - c) / n.length
    fun footOnPlane(q: V3, n: V3, c: Double) = q + n * ((c - (n dot q)) / (n dot n))
    fun mirrorAtPlane(q: V3, n: V3, c: Double) = q + n * (2 * (c - (n dot q)) / (n dot n))
    fun onLine(q: V3, p: V3, d: V3) = distPointLine(q, p, d) < EPS
    fun onPlane(q: V3, n: V3, c: Double) = abs((n dot q) - c) < EPS

    /** Größtes t > 0, bei dem p + t·d noch im Quader [lo, hi] liegt (für Strahlen, die den Raum verlassen). */
    fun exit(p: V3, d: V3, lo: V3, hi: V3): Double {
        var best = Double.MAX_VALUE
        for (i in 0..2) {
            val di = d.list[i]; val pi = p.list[i]
            if (abs(di) < EPS) continue
            val t = ((if (di > 0) hi.list[i] else lo.list[i]) - pi) / di
            if (t > EPS && t < best) best = t
        }
        return if (best == Double.MAX_VALUE) 0.0 else best
    }
}
