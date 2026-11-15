package de.raumlicht.core.model

import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin

/** Was man an einer Stelle der Bahn „erlebt“: Höhe, Steigung, Krümmung. */
data class RideData(val x: Double, val height: Double, val slope: Double, val bend: Double) {
    /** Steigungswinkel in Grad (positiv = bergauf) */
    val angle get() = Math.toDegrees(atan(slope))
    val percent get() = slope * 100
    val slopeWord get() = when { abs(slope) < 0.05 -> "waagerecht"; slope > 0 -> "bergauf"; else -> "bergab" }
    val bendWord get() = when { abs(bend) < 0.05 -> "kein Krümmen (Wendestelle)"; bend > 0 -> "Linkskurve (f″ > 0)"; else -> "Rechtskurve (f″ < 0)" }
    val arrow get() = when { abs(slope) < 0.05 -> "→"; slope > 0 -> "↗"; else -> "↘" }
}

enum class SpotKind(val title: String, val letter: String, val color: String, val rule: String) {
    HIGH("Hochpunkt", "H", "yellow", "f′ = 0 und f″ < 0"), LOW("Tiefpunkt", "T", "green", "f′ = 0 und f″ > 0"),
    TURN("Wendepunkt", "W", "pink", "f″ = 0 – hier ist die Steigung am größten oder kleinsten"), ZERO("Nullstelle", "N", "white", "f(x) = 0")
}
data class Spot(val kind: SpotKind, val x: Double)

/**
 * Eine Achterbahn ist der Graph einer Funktion. Beim Mitfahren aus der Fahrersicht spürt man,
 * was f′ (Steigung) und f″ (Krümmung) bedeuten.
 */
class Ride(val id: String, val title: String, val text: String, val from: Double, val to: Double,
           private val sx: Double, private val ox: Double, private val sy: Double, private val oz: Double,
           val f: (Double) -> Double, val df: (Double) -> Double, val ddf: (Double) -> Double) {
    fun x(u: Double) = from + (to - from) * u.coerceIn(0.0, 1.0)
    fun data(u: Double) = x(u).let { RideData(it, f(it), df(it), ddf(it)) }
    /** Ort in der Bühne: die Bahn liegt in der Ebene x₁ = 4 */
    fun at(x: Double) = V3(4.0, ox + x * sx, oz + f(x) * sy)
    val points: List<V3> get() = (0..SAMPLES).map { at(from + (to - from) * it / SAMPLES) }

    /** Besondere Stellen: Vorzeichenwechsel von f′ (Extrema), f″ (Wendestellen) und f (Nullstellen), auf 0,01 genau. */
    val spots: List<Spot> by lazy {
        fun roots(g: (Double) -> Double): List<Double> {
            val out = ArrayList<Double>(); val n = 400; var prev = g(from)
            for (i in 1..n) {
                val x1 = from + (to - from) * i / n; val cur = g(x1)
                if (prev == 0.0) out += from + (to - from) * (i - 1) / n
                else if (prev * cur < 0) { var a = from + (to - from) * (i - 1) / n; var b = x1; repeat(40) { val m = (a + b) / 2; if (g(a) * g(m) <= 0) b = m else a = m }; out += (a + b) / 2 }
                prev = cur
            }
            return out.filter { it > from + 1e-6 && it < to - 1e-6 }.map { Math.round(it * 100) / 100.0 }.distinct()
        }
        (roots(df).map { Spot(if (ddf(it) < 0) SpotKind.HIGH else SpotKind.LOW, it) } + roots(ddf).map { Spot(SpotKind.TURN, it) } + roots(f).map { Spot(SpotKind.ZERO, it) }).sortedBy { it.x }
    }
    /** Die besondere Stelle, an der man gerade vorbeifährt (innerhalb von [eps] in x) */
    fun spotNear(x: Double, eps: Double = 0.06) = spots.firstOrNull { abs(it.x - x) < eps }
    fun u(x: Double) = (x - from) / (to - from)

    /** Fläche zwischen Bahn und Nulllinie vom Start bis x (unter der Achse wird abgezogen) – numerisch */
    fun integral(x: Double): Double { val n = 400; val h = (x - from) / n; return (0 until n).sumOf { f(from + h * (it + 0.5)) } * h }
    /** f′ als zweite Bahn: so gestaucht, dass sie in den Raum passt, mit derselben Nulllinie */
    private val dScale: Double by lazy { val m = (0..60).maxOf { abs(df(from + (to - from) * it / 60)) }; if (m < 1e-9) 1.0 else minOf(sy, 2.2 / m) }
    /** Nulllinie von f′: die von f – nur wenn f′ sonst im Boden verschwände, liegt sie höher (dann eigene gestrichelte Linie) */
    private val dZero: Double by lazy { val low = (0..60).minOf { df(from + (to - from) * it / 60) }; maxOf(oz, 0.15 - low * dScale) }
    private fun dAt(x: Double) = V3(4.0, ox + x * sx, (dZero + df(x) * dScale).coerceIn(0.05, 6.8))
    /**
     * Bühne mit Zusätzen: [derivative] zeigt f′ als pinke Bahn samt Punkt an der Stelle x,
     * [fill] füllt die Fläche vom Start bis x mit Licht (gelb über, orange unter der Nulllinie).
     */
    fun scene(x: Double, derivative: Boolean, fill: Boolean): String {
        val extra = ArrayList<Obj>()
        if (derivative) { extra += Stage.edges("ableitung", (0..48).map { dAt(from + (to - from) * it / 48) }.zipWithNext(), "pink"); extra += Stage.point("dpunkt", dAt(x), "pink", "f′", drop = false)
            if (dZero > oz + 1e-6) extra += Stage.line("dnull", V3(4.0, ox + from * sx, dZero), V3(0.0, (to - from) * sx, 0.0), 0.0, 1.0, "pink", dashed = true) }
        if (fill && x > from + 1e-6) {
            val n = maxOf(2, ((x - from) / (to - from) * 36).toInt()); val xs = (0..n).map { from + (x - from) * it / n }
            fun quads(sign: Int) = xs.zipWithNext().filter { (a, b) -> f((a + b) / 2) * sign > 0 }.map { (a, b) -> listOf(V3(4.0, ox + a * sx, oz), V3(4.0, ox + b * sx, oz), at(b), at(a)) }
            quads(1).takeIf { it.isNotEmpty() }?.let { q -> extra += Stage.edges("plus${q.size}", listOf(q.first()[0] to q.first()[3]), "yellow", faces = q) }
            quads(-1).takeIf { it.isNotEmpty() }?.let { q -> extra += Stage.edges("minus${q.size}", listOf(q.first()[0] to q.first()[3]), "orange", faces = q) }
        }
        return Stage.encode(Bounds(), base() + extra)
    }

    fun scene(): String = Stage.encode(Bounds(), base())
    private fun base(): List<Obj> = (listOf(
        Obj("bahn", "track", corners = points.map { it.list }, color = "cyan"),
        Stage.axes2d(ox, oz, sx, sy, from, to),
    // Markierungen stehen wie Schilder neben der Strecke (nach hinten versetzt), nicht mitten auf der Bahn
    ) + spots.mapIndexed { i, s -> Stage.point("stelle$i", at(s.x) + V3(-0.9, 0.0, 0.5), s.kind.color, s.kind.letter, style = "ghost", drop = false) })

    companion object { const val SAMPLES = 72 }
}

object Coaster {
    val rides = listOf(
        Ride("huegel", "Sanfter Hügel", "−x² + 4x", 0.0, 4.0, 1.5, 1.0, 1.0, 1.0, { -it * it + 4 * it }, { -2 * it + 4 }, { -2.0 }),
        Ride("bergtal", "Berg und Tal", "x³ − 3x", -2.0, 2.0, 1.5, 4.0, 1.0, 3.0, { it * it * it - 3 * it }, { 3 * it * it - 3 }, { 6 * it }),
        Ride("doppel", "Doppelkurve", "x³ − 6x² + 9x", 0.0, 4.0, 1.5, 1.0, 1.0, 1.0, { it * it * it - 6 * it * it + 9 * it }, { 3 * it * it - 12 * it + 9 }, { 6 * it - 12 }),
        Ride("schuessel", "Schüssel", "x² − 2x − 3", -1.0, 4.0, 1.4, 1.5, 0.5, 2.5, { it * it - 2 * it - 3 }, { 2 * it - 2 }, { 2.0 }),
        Ride("welle", "Welle", "2 · sin(x)", 0.0, 7.0, 1.0, 0.5, 1.0, 3.0, { 2 * sin(it) }, { 2 * cos(it) }, { -2 * sin(it) }),
    )
    fun byId(id: String) = rides.firstOrNull { it.id == id }
}
