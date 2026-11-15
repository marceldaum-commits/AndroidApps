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
        val r = (v * 100).roundToInt() / 100.0
        val s = if (abs(r - r.roundToInt()) < 1e-9) abs(r).roundToInt().toString() else abs(r).toString().replace('.', ',')
        return if (r < 0) "−$s" else s
    }
    fun num(v: Int) = num(v.toDouble())
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
