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
           val f: (Double) -> Double, val df: (Double) -> Double, val ddf: (Double) -> Double,
           /** Eigene Funktionen bringen ihre besonderen Stellen fertig mit; [clipped]: die Kurve ist oben/unten abgeschnitten (Polstelle) */
           private val ownSpots: List<Spot>? = null, val clipped: Boolean = false) {
    fun x(u: Double) = from + (to - from) * u.coerceIn(0.0, 1.0)
    fun data(u: Double) = x(u).let { RideData(it, f(it), df(it), ddf(it)) }
    /** Ort in der Bühne: die Bahn liegt in der Ebene x₁ = 4 */
    fun at(x: Double) = V3(4.0, ox + x * sx, oz + f(x) * sy)
    val points: List<V3> get() = (0..SAMPLES).map { at(from + (to - from) * it / SAMPLES) }

    /** Besondere Stellen: Vorzeichenwechsel von f′ (Extrema), f″ (Wendestellen) und f (Nullstellen), auf 0,01 genau. */
    val spots: List<Spot> by lazy {
        if (ownSpots != null) return@lazy ownSpots
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
    /** Maßstab in der Höhe: [sy] je Einheit, x-Achse auf Höhe [oz] */
    private class Frame(val sy: Double, val oz: Double)
    private val plain = Frame(sy, oz)
    /**
     * Mit Ableitungen stehen alle Kurven unverzerrt am selben Achsenkreuz: Werte von f, f′ und f″ lassen sich an derselben y-Achse ablesen,
     * und f′ geht genau unter jedem Hoch- und Tiefpunkt durch die x-Achse (f″ unter jedem Wendepunkt).
     * Dafür wird der Maßstab so gewählt, dass alles Gezeigte ins Bild passt.
     */
    private fun frame(d1: Boolean, d2: Boolean): Frame {
        if (!d1 && !d2) return plain
        val xs = (0..120).map { from + (to - from) * it / 120 }
        // nur bei Polstellen: Ausreißer der Ableitungen sollen den Maßstab nicht sprengen – sonst zählt der ganze Wertebereich
        fun range(g: (Double) -> Double): Pair<Double, Double> { val v = xs.map(g).sorted(); val cut = if (clipped) v.size / 20 else 0; return v[cut] to v[v.size - 1 - cut] }
        val rs = listOfNotNull(xs.map { f(it) }.let { it.min() to it.max() }, if (d1) range(df) else null, if (d2) range(ddf) else null)
        val lo = minOf(rs.minOf { it.first }, 0.0); val hi = maxOf(rs.maxOf { it.second }, 0.0)
        val s = 5.6 / maxOf(hi - lo, 1e-9)
        return Frame(s, 0.5 - lo * s)
    }
    private fun at(x: Double, fr: Frame, g: (Double) -> Double = f) = V3(4.0, ox + x * sx, fr.oz + g(x) * fr.sy)
    /** Eine Ableitung als zusätzliche Bahn samt Punkt an der Stelle x; [marks]: an diesen Stellen von f kreuzt sie die x-Achse (Lot dorthin) */
    private fun extraCurve(id: String, name: String, color: String, g: (Double) -> Double, x: Double, fr: Frame, marks: List<Spot>): List<Obj> {
        val pts = (0..96).map { at(from + (to - from) * it / 96, fr, g) }
        return listOf(
            // was nicht ins Bild passt (neben Polstellen), wird weggelassen statt plattgedrückt
            Stage.edges(id, pts.zipWithNext().filter { (a, b) -> a.z in 0.05..6.8 && b.z in 0.05..6.8 }, color),
            Stage.point("punkt-$id", at(x, fr, g).let { V3(it.x, it.y, it.z.coerceIn(0.05, 6.8)) }, color, name, drop = false),
        ) + marks.take(12).mapIndexed { i, s -> val top = at(s.x, fr); Stage.line("lot-$id-$i", V3(4.0, top.y, fr.oz), V3(0.0, 0.0, top.z - fr.oz), 0.0, 1.0, color, dashed = true) }
    }
    /**
     * Bühne mit Zusätzen: [derivative] zeigt f′ als pinke, [second] f″ als violette Bahn samt Punkt an der Stelle x,
     * [fill] füllt die Fläche vom Start bis x mit Licht (gelb über, orange unter der Nulllinie).
     */
    fun scene(x: Double, derivative: Boolean, fill: Boolean, second: Boolean = false): String {
        val fr = frame(derivative, second)
        val extra = ArrayList<Obj>()
        if (derivative) extra += extraCurve("ableitung", "f′", "pink", df, x, fr, spots.filter { it.kind == SpotKind.HIGH || it.kind == SpotKind.LOW })
        if (second) extra += extraCurve("zweite", "f″", "violet", ddf, x, fr, spots.filter { it.kind == SpotKind.TURN })
        if (fill && x > from + 1e-6) {
            val n = maxOf(2, ((x - from) / (to - from) * 36).toInt()); val xs = (0..n).map { from + (x - from) * it / n }
            fun quads(sign: Int) = xs.zipWithNext().filter { (a, b) -> f((a + b) / 2) * sign > 0 }.map { (a, b) -> listOf(V3(4.0, ox + a * sx, fr.oz), V3(4.0, ox + b * sx, fr.oz), at(b, fr), at(a, fr)) }
            quads(1).takeIf { it.isNotEmpty() }?.let { q -> extra += Stage.edges("plus${q.size}", listOf(q.first()[0] to q.first()[3]), "yellow", faces = q) }
            quads(-1).takeIf { it.isNotEmpty() }?.let { q -> extra += Stage.edges("minus${q.size}", listOf(q.first()[0] to q.first()[3]), "orange", faces = q) }
        }
        return Stage.encode(Bounds(), base(fr) + extra)
    }

    fun scene(): String = Stage.encode(Bounds(), base(plain))
    private fun base(fr: Frame): List<Obj> = (listOf(
        Obj("bahn", "track", corners = (0..SAMPLES).map { at(from + (to - from) * it / SAMPLES, fr).list }, color = "cyan"),
        Stage.axes2d(ox, fr.oz, sx, fr.sy, from, to),
    // Markierungen stehen wie Schilder neben der Strecke (nach hinten versetzt), nicht mitten auf der Bahn.
    // Bei sehr vielen Stellen (lange eigene Fahrten) bekommen nur Hoch- und Tiefpunkte ein Schild – sonst verdecken sich die Schilder
    ) + spots.filter { spots.size <= 12 || it.kind == SpotKind.HIGH || it.kind == SpotKind.LOW }.mapIndexed { i, s -> Stage.point("stelle$i", at(s.x, fr) + V3(-0.9, 0.0, 0.5), s.kind.color, s.kind.letter, style = "ghost", drop = false) })

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

    const val CUSTOM = "eigene"
    /**
     * Bahn aus einer eigenen Funktion auf [from; to]. Der Maßstab passt sich an, damit jede Kurve in die Bühne passt
     * (x und y dürfen dabei verschieden gestreckt sein – das Achsenkreuz zeigt die echten Werte).
     * Ableitungen werden numerisch bestimmt, Nullstellen, Extrema und Wendestellen automatisch gesucht.
     * @return null, wenn die Funktion im Abschnitt nirgends definiert ist
     */
    fun custom(e: Expr, from: Double, to: Double): Ride? {
        if (!(to > from)) return null
        val n = 480; val xs = (0..n).map { from + (to - from) * it / n }
        val ys = xs.map { e.at(it) }.filter { it.isFinite() }.sorted()
        if (ys.size < n / 4) return null
        // Polstellen sprengen den Maßstab: Ausreißer abschneiden, sobald sie den Kern der Werte weit überragen
        val q1 = ys[ys.size / 20]; val q9 = ys[ys.size - 1 - ys.size / 20]; val core = maxOf(q9 - q1, 1e-9)
        val lo0 = if (ys.first() < q1 - 3 * core) q1 - core else ys.first(); val hi0 = if (ys.last() > q9 + 3 * core) q9 + core else ys.last()
        val clipped = lo0 > ys.first() || hi0 < ys.last() || ys.size <= n
        var lo = minOf(lo0, 0.0); var hi = maxOf(hi0, 0.0); if (hi - lo < 1e-9) { lo -= 1; hi += 1 }
        val sx = 6.6 / (to - from); val sy = 5.4 / (hi - lo)
        // Für die Bühne: immer endlich und im Fenster. Die Stellensuche unten arbeitet mit den echten Werten.
        val f = { x: Double -> e.at(x).let { if (it.isNaN()) 0.0 else it.coerceIn(lo, hi) } }
        val h1 = (to - from) * 1e-5; val h2 = (to - from) * 2e-4
        val d1 = { x: Double -> (e.at(x + h1) - e.at(x - h1)) / (2 * h1) }; val d2 = { x: Double -> (e.at(x + h2) - 2 * e.at(x) + e.at(x - h2)) / (h2 * h2) }
        fun safe(g: (Double) -> Double) = { x: Double -> g(x).let { if (it.isFinite()) it else 0.0 } }
        /** [touch]: auch Stellen zählen, an denen g die Null nur berührt (doppelte Nullstelle von f) */
        fun roots(g: (Double) -> Double, touch: Boolean = false): List<Double> {
            val m = 800; val v = (0..m).map { g(from + (to - from) * it / m) }
            val scale = v.filter { it.isFinite() }.map { abs(it) }.sorted().let { if (it.isEmpty()) return emptyList() else it[it.size * 9 / 10] }
            if (scale < 1e-9) return emptyList()                                       // überall null (z. B. f′ einer Konstanten): keine einzelne Stelle
            val eps = scale * 1e-6; val out = ArrayList<Double>()
            for (i in 1..m) { val a0 = v[i - 1]; val b0 = v[i]
                // genau auf einem Abtastpunkt null: zählt bei Vorzeichenwechsel (oder Berührung, wenn gewünscht)
                if (b0 == 0.0 && i < m && a0.isFinite() && v[i + 1].isFinite() && abs(a0) > eps && abs(v[i + 1]) > eps && (touch || a0 * v[i + 1] < 0)) { out += from + (to - from) * i / m; continue }
                if (!a0.isFinite() || !b0.isFinite() || a0 * b0 >= 0 || (abs(a0) < eps && abs(b0) < eps)) continue
                var a = from + (to - from) * (i - 1) / m; var b = from + (to - from) * i / m
                repeat(50) { val mid = (a + b) / 2; val gm = g(mid); if (!gm.isFinite()) return@repeat; if (g(a) * gm <= 0) b = mid else a = mid }
                val r = (a + b) / 2
                if (abs(g(r)) < maxOf(abs(a0), abs(b0)) * 0.5 + eps) out += r                // an Polstellen springt das Vorzeichen, ohne dass g klein wird
            }
            return out.filter { it > from + 1e-9 && it < to - 1e-9 }.map { Math.round(it * 100) / 100.0 }.distinct()
        }
        val spots = (roots(d1).map { Spot(if (d2(it) < 0) SpotKind.HIGH else SpotKind.LOW, it) } + roots(d2).map { Spot(SpotKind.TURN, it) } + roots(e::at, touch = true).map { Spot(SpotKind.ZERO, it) })
            .sortedBy { it.x }.take(60)
        return Ride(CUSTOM, "Eigene Funktion", e.text, from, to, sx, 0.7 - from * sx, sy, 0.5 - lo * sy, f, safe(d1), safe(d2), spots, clipped)
    }
}
