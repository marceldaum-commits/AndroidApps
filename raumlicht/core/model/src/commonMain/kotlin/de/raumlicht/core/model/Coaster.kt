package de.raumlicht.core.model

import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin

/**
 * Was man an einer Stelle der Bahn „erlebt“: Höhe, Steigung, Krümmung.
 * [defined]: die Funktion hat hier einen (endlichen) Wert – sonst ist [height] NaN und es gibt nichts anzuzeigen.
 * [smooth]: auch f′ und f″ gibt es hier (am Rand des Definitionsbereichs nicht) – sonst stehen [slope] und [bend] auf 0.
 * [turn]: hier liegt wirklich eine Wendestelle der Bahn. [offscreen]: der Wert liegt außerhalb des gezeichneten (abgeschnittenen) Bildes.
 */
data class RideData(val x: Double, val height: Double, val slope: Double, val bend: Double,
                    val defined: Boolean = true, val smooth: Boolean = true, val turn: Boolean = false, val offscreen: Boolean = false) {
    /** Steigungswinkel in Grad (positiv = bergauf) */
    val angle get() = Geo.deg(atan(slope))
    val percent get() = slope * 100
    val slopeWord get() = when { !smooth -> UNDEFINED; abs(slope) < 0.05 -> "waagerecht"; slope > 0 -> "bergauf"; else -> "bergab" }
    /** „Wendestelle“ steht nur dort, wo die Bahn wirklich eine hat – ln(x) oder eine Gerade sind bloß (fast) gerade */
    val bendWord get() = when { !smooth -> UNDEFINED; abs(bend) < 0.05 && turn -> "keine Krümmung (Wendestelle)"; abs(bend) < 1e-6 -> "gerade (keine Krümmung)"
        abs(bend) < 0.05 -> "fast gerade"; bend > 0 -> "Linkskurve (f″ > 0)"; else -> "Rechtskurve (f″ < 0)" }
    val arrow get() = when { !smooth -> "·"; abs(slope) < 0.05 -> "→"; slope > 0 -> "↗"; else -> "↘" }
    companion object { const val UNDEFINED = "nicht definiert" }
}

enum class SpotKind(val title: String, val letter: String, val color: String, val rule: String) {
    HIGH("Hochpunkt", "H", "yellow", "f′ = 0 und f″ < 0"), LOW("Tiefpunkt", "T", "green", "f′ = 0 und f″ > 0"),
    TURN("Wendepunkt", "W", "pink", "f″ = 0 und f″ wechselt das Vorzeichen – hier ist die Steigung am größten oder kleinsten"), ZERO("Nullstelle", "N", "white", "f(x) = 0")
}
/** [note]: eigener Regeltext, wenn der übliche nicht stimmt (Knick, flache Stelle mit f″ = 0, Sattelpunkt) */
data class Spot(val kind: SpotKind, val x: Double, val note: String? = null) {
    val rule get() = note ?: kind.rule
}

/** Was eine eigene Funktion zusätzlich mitbringt: die echten Werte (nicht abgeschnitten), wo sie glatt ist, bis wohin die Fläche sinnvoll ist */
class RideOwn(val raw: (Double) -> Double, val smooth: (Double) -> Boolean, val lo: Double, val hi: Double,
              /** die Fläche vom Start bis x gibt es nur für x < [areaEnd] (davor liegt keine Lücke und keine Polstelle) */
              val areaEnd: Double,
              /** Hinweis, wo die Funktion im Abschnitt definiert ist – null, wenn überall (bis auf einzelne Stellen) */
              val domain: String?,
              /** so viele besondere Stellen wurden gefunden (markiert werden höchstens [Coaster.MAX_SPOTS]) */
              val found: Int)

/**
 * Eine Achterbahn ist der Graph einer Funktion. Beim Mitfahren aus der Fahrersicht spürt man,
 * was f′ (Steigung) und f″ (Krümmung) bedeuten.
 */
class Ride(val id: String, val title: String, val text: String, val from: Double, val to: Double,
           private val sx: Double, private val ox: Double, private val sy: Double, private val oz: Double,
           val f: (Double) -> Double, val df: (Double) -> Double, val ddf: (Double) -> Double,
           /** Eigene Funktionen bringen ihre besonderen Stellen fertig mit; [clipped]: die Kurve ist oben/unten abgeschnitten (Polstelle) */
           private val ownSpots: List<Spot>? = null, val clipped: Boolean = false, val own: RideOwn? = null) {
    fun x(u: Double) = from + (to - from) * u.coerceIn(0.0, 1.0)
    fun data(u: Double): RideData { val x = x(u)
        val turn = spots.any { it.kind == SpotKind.TURN && abs(it.x - x) < 0.06 }
        if (own == null) return RideData(x, f(x), df(x), ddf(x), turn = turn)
        // eigene Funktion: der echte Wert (auch wenn das Bild dort abgeschnitten ist) – und ehrlich „nicht definiert“, wo es keinen gibt
        val y = own.raw(x); val defined = y.isFinite(); val smooth = defined && own.smooth(x)
        return RideData(x, if (defined) y else Double.NaN, if (smooth) df(x) else 0.0, if (smooth) ddf(x) else 0.0, defined, smooth, turn, defined && (y < own.lo - 1e-9 || y > own.hi + 1e-9))
    }
    /** Ort in der Bühne: die Bahn liegt in der Ebene x₁ = 4 */
    fun at(x: Double) = V3(4.0, ox + x * sx, oz + f(x) * sy)
    val points: List<V3> get() = (0..SAMPLES).map { at(from + (to - from) * it / SAMPLES) }

    /**
     * Besondere Stellen: Vorzeichenwechsel von f′ (Extrema), f″ (Wendestellen) und f (Nullstellen), auf 0,01 genau.
     * Nullstellen genau am Rand des Abschnitts zählen mit; Extrem- und Wendestellen am Rand nicht (das wären Randextrema).
     */
    val spots: List<Spot> by lazy {
        if (ownSpots != null) return@lazy ownSpots
        fun roots(g: (Double) -> Double, edges: Boolean = false): List<Double> {
            val out = ArrayList<Double>(); val n = 400; var prev = g(from)
            for (i in 1..n) {
                val x1 = from + (to - from) * i / n; val cur = g(x1)
                if (prev == 0.0) out += from + (to - from) * (i - 1) / n
                else if (prev * cur < 0) { var a = from + (to - from) * (i - 1) / n; var b = x1; repeat(40) { val m = (a + b) / 2; if (g(a) * g(m) <= 0) b = m else a = m }; out += (a + b) / 2 }
                prev = cur
            }
            if (abs(prev) < 1e-12) out += to
            return out.filter { edges || (it > from + 1e-6 && it < to - 1e-6) }.map { kotlin.math.round(it * 100) / 100.0 + 0.0 }.distinct()
        }
        (roots(df).map { Spot(if (ddf(it) < 0) SpotKind.HIGH else SpotKind.LOW, it) } + roots(ddf).map { Spot(SpotKind.TURN, it) } + roots(f, edges = true).map { Spot(SpotKind.ZERO, it) }).sortedBy { it.x }
    }
    /**
     * Alle besonderen Stellen, an denen man gerade vorbeifährt (innerhalb von [eps] in x): Fallen mehrere auf dieselbe Stelle
     * (Wendepunkt und Nullstelle, Tiefpunkt und Nullstelle), kommen alle zurück – nicht nur die erste.
     */
    fun spotsNear(x: Double, eps: Double = 0.06): List<Spot> {
        val first = spots.filter { abs(it.x - x) < eps }.minByOrNull { abs(it.x - x) } ?: return emptyList()
        return spots.filter { abs(it.x - first.x) < 1e-9 }
    }
    /** Die erste besondere Stelle, an der man gerade vorbeifährt – siehe [spotsNear] */
    fun spotNear(x: Double, eps: Double = 0.06) = spotsNear(x, eps).firstOrNull()
    fun u(x: Double) = (x - from) / (to - from)

    /**
     * Fläche zwischen Bahn und Nulllinie vom Start bis x (unter der Achse wird abgezogen) – numerisch (Gauß mit zwei Stützstellen je Teilstück).
     * NaN („nicht definiert“), sobald zwischen Start und x eine Lücke oder Polstelle liegt: Dort gibt es keine sinnvolle Fläche.
     */
    fun integral(x: Double): Double {
        if (own != null && !(x < own.areaEnd)) return Double.NaN
        val g = own?.raw ?: f; val n = 400; val h = (x - from) / n; val q = h * 0.2886751345948129       // h / (2·√3)
        return (0 until n).sumOf { val m = from + h * (it + 0.5); g(m - q) + g(m + q) } * h / 2
    }
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
        // gefüllt wird nur, solange es die Fläche gibt (bis vor die erste Lücke oder Polstelle)
        val end = if (own == null || x < own.areaEnd) x else own.areaEnd - (to - from) * 1e-3
        if (fill && end > from + 1e-6) {
            val n = maxOf(2, ((end - from) / (to - from) * 36).toInt()); val xs = (0..n).map { from + (end - from) * it / n }
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
        Ride("ebuckel", "e-Buckel", "4x · e^(−x)", 0.0, 6.0, 1.1, 0.7, 2.5, 1.0, { 4 * it * kotlin.math.exp(-it) }, { 4 * (1 - it) * kotlin.math.exp(-it) }, { 4 * (it - 2) * kotlin.math.exp(-it) }),
        Ride("ln", "Logarithmus", "ln(x)", 0.3, 6.0, 1.2, 0.4, 1.2, 2.2, { kotlin.math.ln(it) }, { 1 / it }, { -1 / (it * it) }),
    )
    fun byId(id: String) = rides.firstOrNull { it.id == id }

    const val CUSTOM = "eigene"
    /** So viele besondere Stellen einer eigenen Funktion werden höchstens markiert – bei mehr wird gleichmäßig über den Abschnitt ausgedünnt */
    const val MAX_SPOTS = 60
    /**
     * Bahn aus einer eigenen Funktion auf [from; to]. Der Maßstab passt sich an, damit jede Kurve in die Bühne passt
     * (x und y dürfen dabei verschieden gestreckt sein – das Achsenkreuz zeigt die echten Werte).
     * Ableitungen werden numerisch bestimmt, Nullstellen, Extrema und Wendestellen automatisch gesucht ([CurveScan]).
     * @return null, wenn die Funktion im Abschnitt (fast) nirgends definiert ist
     */
    fun custom(e: Expr, from: Double, to: Double): Ride? {
        if (!(to > from)) return null
        val len = to - from
        // feines Raster: mindestens 1600 Punkte, bei langen Abschnitten 8 je Einheit (damit auch sin(x) auf [0; 1000] sauber aufgelöst wird)
        val n = (len * 8).toInt().coerceIn(1600, 8000)
        val xs = DoubleArray(n + 1) { if (it == n) to else from + len * it / n }; val v = DoubleArray(n + 1) { e.at(xs[it]) }
        val ys = v.filter { it.isFinite() }.sorted()
        if (ys.size < maxOf(4, n / 400)) return null
        // Polstellen sprengen den Maßstab: Ausreißer abschneiden, sobald sie den Kern der Werte weit überragen
        val q1 = ys[ys.size / 20]; val q9 = ys[ys.size - 1 - ys.size / 20]; val core = maxOf(q9 - q1, 1e-9)
        val lo0 = if (ys.first() < q1 - 3 * core) q1 - core else ys.first(); val hi0 = if (ys.last() > q9 + 3 * core) q9 + core else ys.last()
        val clipped = lo0 > ys.first() || hi0 < ys.last() || ys.size <= n
        var lo = minOf(lo0, 0.0); var hi = maxOf(hi0, 0.0); if (hi - lo < 1e-9) { lo -= 1; hi += 1 }
        val sx = 6.6 / len; val sy = 5.4 / (hi - lo)
        // Für die Bühne: immer endlich und im Fenster. Cockpit, Fläche und Stellensuche arbeiten mit den echten Werten.
        val f = { x: Double -> e.at(x).let { if (it.isNaN()) 0.0 else it.coerceIn(lo, hi) } }
        val h1 = minOf(len * 1e-5, 1e-3); val h2 = minOf(len * 2e-4, 1e-2)
        val d1 = { x: Double -> (e.at(x + h1) - e.at(x - h1)) / (2 * h1) }; val d2 = { x: Double -> (e.at(x + h2) - 2 * e.at(x) + e.at(x - h2)) / (h2 * h2) }
        fun safe(g: (Double) -> Double) = { x: Double -> g(x).let { if (it.isFinite()) it else 0.0 } }
        val scan = CurveScan(e, from, to, xs, v, h2, d1, d2)
        val all = scan.spots()
        // sehr viele Stellen (lange Abschnitte): gleichmäßig über den Abschnitt ausdünnen, statt nur den Anfang zu markieren
        val spots = if (all.size <= MAX_SPOTS) all else (0 until MAX_SPOTS).map { all[(it.toLong() * (all.size - 1) / (MAX_SPOTS - 1)).toInt()] }
        val own = RideOwn(e::at, { x -> d1(x).isFinite() && d2(x).isFinite() }, lo, hi, scan.areaEnd(), scan.domain(), all.size)
        return Ride(CUSTOM, "Eigene Funktion", e.text, from, to, sx, 0.7 - from * sx, sy, 0.5 - lo * sy, f, safe(d1), safe(d2), spots, clipped, own)
    }
}

/**
 * Kurvenuntersuchung einer eigenen Funktion, von der nur Werte bekannt sind – so, dass Rechenrauschen, Polstellen, Lücken und Knicke
 * keine erfundenen Stellen ergeben und flache oder mehrfache Stellen nicht verloren gehen:
 * - **Rauschen**: [Expr.noise] schätzt den Rundungsfehler jedes Werts. Ein Extremum zählt erst, wenn die Kurve davor und danach deutlich
 *   mehr als das Rauschen steigt bzw. fällt; f″ hat erst ein Vorzeichen, wenn es deutlich über seinem Rauschen liegt (und das an zwei Rasterpunkten hintereinander).
 * - **Extrema** werden an f selbst erkannt (Vorzeichenwechsel von f′ = erst steigen, dann fallen), nicht über f″ an einer gerundeten Stelle –
 *   so stimmt die Art auch bei x⁴ (f″ = 0) und an Knicken. Die Stelle ist die Mitte zwischen zwei gleich hohen Punkten links und rechts (genau auch bei flachen Stellen).
 * - **Nullstellen**: Vorzeichenwechsel (Bisektion) und Berührstellen (Extremum mit Wert null). An einer Polstelle wechselt das Vorzeichen, ohne dass f klein wird – keine Nullstelle.
 * - **Wendestellen**: echter Vorzeichenwechsel von f″; die Stelle ist die Mitte zwischen den Durchgängen durch +L und −L. An Knicken (f′ springt) keine Wendestelle.
 * - **Polstellen, Lücken, Sprünge**: nirgends eine Stelle; sie begrenzen außerdem die Fläche ([areaEnd]).
 */
private class CurveScan(val e: Expr, val from: Double, val to: Double, val xs: DoubleArray, val v: DoubleArray, val h2: Double,
                        val d1: (Double) -> Double, val d2: (Double) -> Double) {
    private val m = xs.size - 1; private val step = (to - from) / m
    private val noise = DoubleArray(m + 1) { n(xs[it]) }
    private val found = ArrayList<Spot>(); private val breaks = ArrayList<Double>()
    private fun n(x: Double) = e.noise(x).let { if (it.isFinite()) it else Double.POSITIVE_INFINITY }
    private fun fin(i: Int) = v[i].isFinite()
    private fun shown(x: Double) = kotlin.math.round(x * 100) / 100.0 + 0.0              // + 0.0: aus −0 wird 0

    /** Stelle zwischen a und b, an der g das Vorzeichen wechselt ([neg]: g(a) < 0). NaN, wenn g unterwegs nicht definiert ist. */
    private inline fun bisect(a0: Double, b0: Double, neg: Boolean, g: (Double) -> Double): Double {
        var a = a0; var b = b0
        repeat(44) { val mid = (a + b) / 2; val gm = g(mid); if (gm.isNaN()) return Double.NaN; if (gm == 0.0) return mid; if ((gm < 0) == neg) a = mid else b = mid }
        return (a + b) / 2
    }
    /** Größter Betrag von f in der Umgebung von r (1, 2, 4, 8 Rasterschritte; [side] 0 = beide Seiten) */
    private fun around(r: Double, side: Int = 0): Double {
        var b = 0.0
        for (k in SPAN) for (sg in SIDES) if (side == 0 || sg == side) { val y = e.at(r + sg * k * step); if (y.isFinite() && abs(y) > b) b = abs(y) }
        return b
    }
    /** Ist f bei r null? Entweder winzig gegenüber der Umgebung – oder im Rahmen des Rundungsfehlers (und klar kleiner als die Umgebung: an Polstellen ist auch der Fehler riesig). */
    private fun isZero(r: Double, y: Double, side: Int = 0): Boolean {
        if (!y.isFinite()) return false
        val b = around(r, side); return b > 0 && (abs(y) <= 1e-6 * b || (abs(y) <= 64 * n(r) && abs(y) <= 1e-2 * b))
    }
    private fun add(kind: SpotKind, r: Double, note: String? = null) {
        val x = shown(r).coerceIn(from, to)
        // angezeigt wird die gerundete Stelle: Ist f genau dort nicht definiert (Lücke wie bei (x² − 4)/(x − 2)), gibt es die Stelle nicht
        if (e.at(x).isFinite() && e.at(r).isFinite()) found += Spot(kind, x, note)
    }
    /** Knick oder Spitze bei r? Dann bleibt der Sprung der einseitigen Steigungen gleich groß, wenn man näher heranrückt (bei glatten Kurven schrumpft er mit). */
    private fun kink(r: Double, y: Double): Boolean {
        val db = 4 * h2; val da = 10 * db
        val nb = abs(e.at(r + db) - 2 * y + e.at(r - db)); val na = abs(e.at(r + da) - 2 * y + e.at(r - da))
        return nb.isFinite() && na.isFinite() && nb > 400 * n(r) && nb / db > 0.5 * na / da
    }

    private fun zeros() {
        var a = -1
        for (i in 0..m) {
            if (!fin(i)) { a = -1; breaks += xs[i]; continue }
            if (v[i] == 0.0) continue
            // Vorzeichenwechsel zwischen zwei Rasterpunkten (dazwischen höchstens zwei Punkte, die genau null sind); mindestens einer liegt klar über dem Rauschen
            if (a >= 0 && (v[a] < 0) != (v[i] < 0) && i - a <= 3 && (abs(v[a]) > 64 * noise[a] || abs(v[i]) > 64 * noise[i])) {
                val r = bisect(xs[a], xs[i], v[a] < 0) { e.at(it) }
                if (r.isNaN()) breaks += xs[a] else if (isZero(r, e.at(r))) add(SpotKind.ZERO, r) else breaks += r      // Polstelle oder Sprung
            }
            a = i
        }
        // Nullstellen genau am Rand des Abschnitts zählen mit
        for (i in intArrayOf(0, m)) if (fin(i) && (v[i] == 0.0 && around(xs[i], if (i == 0) 1 else -1) > 0 || v[i] != 0.0 && abs(v[i]) <= 64 * noise[i] && isZero(xs[i], v[i], if (i == 0) 1 else -1)))
            add(SpotKind.ZERO, xs[i])
    }

    /** Extrema im durchgehend definierten Stück [s; t] des Rasters */
    private fun extrema(s: Int, t: Int) {
        val at = ArrayList<Int>(); val high = ArrayList<Boolean>()
        fun tau(i: Int, j: Int) = 200 * maxOf(noise[i], noise[j])
        var dir = 0; var hiI = s; var loI = s
        for (i in s + 1..t) {
            val y = v[i]; if (y > v[hiI]) hiI = i; if (y < v[loI]) loI = i
            when (dir) {
                0 -> if (y - v[loI] > tau(i, loI)) { dir = 1; hiI = i } else if (v[hiI] - y > tau(i, hiI)) { dir = -1; loI = i }
                1 -> if (v[hiI] - y > tau(i, hiI)) { at += hiI; high += true; dir = -1; loI = i }
                else -> if (y - v[loI] > tau(i, loI)) { at += loI; high += false; dir = 1; hiI = i }
            }
        }
        for (k in at.indices) {
            val i = at[k]; val q = if (high[k]) -1.0 else 1.0; val y0 = v[i]                   // q · (f − y0) wächst nach beiden Seiten
            val a = if (k == 0) s else at[k - 1]; val b = if (k == at.size - 1) t else at[k + 1]
            var pl = 0.0; for (j in a until i) pl = maxOf(pl, q * (v[j] - y0)); var pr = 0.0; for (j in i + 1..b) pr = maxOf(pr, q * (v[j] - y0))
            val p = minOf(pl, pr); if (!(p > 0) || i <= s || i >= t) continue
            // Mitte zwischen zwei gleich hohen Punkten knapp über (unter) dem Extremum: genau auch dort, wo die Kurve ganz flach ist
            val level = minOf(0.5 * p, maxOf(1e-6 * p, 1000 * noise[i]))
            var jl = i - 1; while (jl > a && q * (v[jl] - y0) < level) jl--
            var jr = i + 1; while (jr < b && q * (v[jr] - y0) < level) jr++
            val xl = bisect(xs[jl], xs[jl + 1], false) { q * (e.at(it) - y0) - level }; val xr = bisect(xs[jr - 1], xs[jr], true) { q * (e.at(it) - y0) - level }
            val r = (xl + xr) / 2; val y = if (r.isNaN()) Double.NaN else e.at(r)
            // Polstelle, Lücke oder Sprung statt Extremum: der Wert schießt über die Rasterwerte hinaus, oder der Abstand zum Nachbarwert schrumpft beim Heranrücken nicht
            var singular = !y.isFinite() || q * (y0 - y) > 10 * maxOf(q * (v[i - 1] - y0), q * (v[i + 1] - y0), 0.0) + level
            if (!singular) for (sg in SIDES) { val ea = abs(y - e.at(r + sg * step)); val eb = abs(y - e.at(r + sg * step / 10))
                if (!ea.isFinite() || !eb.isFinite() || (eb > 0.5 * ea && eb > 1000 * n(r))) singular = true }
            if (singular) { breaks += if (r.isNaN()) xs[i] else r; continue }
            val word = if (high[k]) "von + nach −" else "von − nach +"
            val w = 20 * h2; val curve = abs(d2(r)); val side = maxOf(abs(d2(r - w)), abs(d2(r + w)))
            val note = when {
                kink(r, y) -> "Knick: f′ wechselt das Vorzeichen $word (f′ gibt es hier nicht)"
                curve.isFinite() && side.isFinite() && curve < 0.05 * side -> "f′ = 0 und f′ wechselt das Vorzeichen $word (f″ = 0 hilft hier nicht)"
                else -> null
            }
            add(if (high[k]) SpotKind.HIGH else SpotKind.LOW, r, note)
            if (isZero(r, y)) add(SpotKind.ZERO, r)                                              // Berührstelle: doppelte (vierfache …) Nullstelle
        }
    }

    /** Wendestellen im durchgehend definierten Stück [s; t] des Rasters */
    private fun turns(s: Int, t: Int) {
        val d = DoubleArray(t - s + 1)
        var state = 0; var stateI = -1; var pend = 0; var pendI = -1; var count = 0
        for (i in s..t) {
            val c = d2(xs[i]); d[i - s] = c
            val tau = 80 * maxOf(noise[maxOf(i - 1, s)], noise[i], noise[minOf(i + 1, t)]) / (h2 * h2)
            val sg = if (c.isFinite() && abs(c) > tau) (if (c > 0) 1 else -1) else 0
            if (sg == 0) { pend = 0; count = 0; continue }
            if (sg == pend) count++ else { pend = sg; pendI = i; count = 1 }
            if (count >= 2) { if (state != 0 && sg != state) turn(stateI, pendI, state, d[stateI - s], d[pendI - s]); state = sg }
            if (sg == state) stateI = i
        }
    }
    /** f″ hat bei Rasterpunkt [ia] sicher das Vorzeichen [sign] und bei [ib] sicher das andere: Wendestelle dazwischen */
    private fun turn(ia: Int, ib: Int, sign: Int, da: Double, db: Double) {
        val level = 0.5 * minOf(abs(da), abs(db))
        val x1 = bisect(xs[ia], xs[ib], false) { sign * d2(it) - level }; val x2 = bisect(xs[ia], xs[ib], false) { sign * d2(it) + level }
        val r = (x1 + x2) / 2; if (r.isNaN()) return
        val c = d2(r); val y = e.at(r)
        if (!c.isFinite() || abs(c) > level || !y.isFinite() || kink(r, y)) return               // Polstelle oder Knick: keine Wendestelle
        val w = 20 * h2; val slope = abs(d1(r)); val side = maxOf(abs(d1(r - w)), abs(d1(r + w)))
        add(SpotKind.TURN, r, if (slope.isFinite() && side.isFinite() && slope < 0.05 * side) "Sattelpunkt: f′ = 0 und f″ wechselt das Vorzeichen – waagerechte Tangente, aber kein Extremum" else null)
    }

    /** Alle besonderen Stellen, nach x geordnet (bei gleicher Stelle: Extremum, Wendepunkt, Nullstelle) */
    fun spots(): List<Spot> {
        found.clear(); breaks.clear()
        val pieces = pieces()
        pieces.forEach { (s, t) -> extrema(s, t) }; pieces.forEach { (s, t) -> turns(s, t) }; zeros()
        return found.distinctBy { it.kind to it.x }.sortedBy { it.x }
    }
    /** durchgehend definierte Stücke des Rasters */
    private fun pieces(): List<Pair<Int, Int>> {
        val out = ArrayList<Pair<Int, Int>>(); var s = 0
        while (s <= m) { if (!fin(s)) { s++; continue }; var t = s; while (t < m && fin(t + 1)) t++; out += s to t; s = t + 1 }
        return out
    }
    /** Die Fläche vom Start bis x gibt es nur für x vor der ersten Lücke, Polstelle oder Sprungstelle (erst nach [spots] aufrufen) */
    fun areaEnd(): Double = breaks.minOrNull() ?: Double.POSITIVE_INFINITY
    /** Hinweis, wo die Funktion definiert ist – nur wenn ein ganzes Stück des Abschnitts fehlt (einzelne Polstellen und Lücken zählen nicht) */
    fun domain(): String? {
        val merged = ArrayList<Pair<Int, Int>>()
        for (p in pieces()) if (merged.isNotEmpty() && p.first - merged.last().second <= 2) merged[merged.size - 1] = merged.last().first to p.second else merged += p
        if (merged.isEmpty() || (merged.size == 1 && merged[0].first <= 1 && merged[0].second >= m - 1)) return null
        // Rand des Definitionsbereichs zwischen einem definierten und einem nicht definierten Rasterpunkt
        fun edge(inside: Double, outside: Double): Double { var a = inside; var b = outside; repeat(30) { val mid = (a + b) / 2; if (e.at(mid).isFinite()) a = mid else b = mid }; return a }
        val parts = merged.take(3).map { (s, t) -> "zwischen ${Geo.num(if (s <= 1) from else edge(xs[s], xs[s - 1]))} und ${Geo.num(if (t >= m - 1) to else edge(xs[t], xs[t + 1]))}" }
        return "Definiert nur " + parts.joinToString(" sowie ") + if (merged.size > 3) " …" else ""
    }

    private companion object { val SPAN = intArrayOf(1, 2, 4, 8); val SIDES = intArrayOf(-1, 1) }
}
