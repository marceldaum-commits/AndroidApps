package de.kurvenflug.core.model

import kotlin.math.abs
import kotlin.math.min

/**
 * Oberstufe (Klasse 11–13): Ableitung, Extrempunkte, Integral, Vektoren.
 * Die Kurve ist ein Polynom f(x) = c0 + c1·x + c2·x² + c3·x³ (Koeffizienten in [Level.fixed]).
 *
 * - [Kind.TANGENT]: Die Sonde gleitet auf f und löst sich bei x₀ tangential – Steigung m = f'(x₀) einstellen.
 * - [Kind.PEAK]: Die Sonde fliegt f ab und hält bei x – dort, wo f'(x) = 0 (Hoch-/Tiefpunkt) bzw. f''(x) = 0 (Wendepunkt).
 * - [Kind.AREA]: Treibstoff = Fläche unter f ab x = 0 – so viel tanken, dass die Sonde genau an der Station ankommt.
 * - [Kind.VECTOR]: Gerade in Parameterform X = P + t · v – Richtungsvektor so wählen, dass die Sonde nach Zeit T ankommt.
 */
object Upper {
    private const val STEP = 0.02
    private const val R = Flight.R

    val kinds = setOf(Kind.TANGENT, Kind.PEAK, Kind.AREA, Kind.VECTOR)

    /** Bei diesen zählt nur, wo die Sonde ankommt (nicht, was sie unterwegs streift) */
    val endOnly = setOf(Kind.POINT, Kind.PEAK, Kind.AREA, Kind.VECTOR)

    fun c(v: Map<String, Double>) = DoubleArray(4) { v["c$it"] ?: 0.0 }
    fun f(c: DoubleArray, x: Double) = c[0] + c[1] * x + c[2] * x * x + c[3] * x * x * x
    fun df(c: DoubleArray, x: Double) = c[1] + 2 * c[2] * x + 3 * c[3] * x * x
    fun ddf(c: DoubleArray, x: Double) = 2 * c[2] + 6 * c[3] * x
    /** Stammfunktion F mit F(0) = 0 */
    fun F(c: DoubleArray, x: Double) = c[0] * x + c[1] * x * x / 2 + c[2] * x * x * x / 3 + c[3] * x * x * x * x / 4

    fun path(level: Level, values: Map<String, Double>): List<Vec> {
        val v = level.values(values)
        val c = c(v)
        return when (level.kind) {
            Kind.TANGENT -> {
                val x0 = v.getValue("x0"); val y0 = f(c, x0); val m = v.getValue("m")
                curve(c, firstVisible(c), x0) + generateSequence(x0) { it + STEP }.takeWhile { it <= R + 1e-9 }
                    .map { Vec(it, y0 + m * (it - x0)) }.takeWhile { abs(it.y) <= R }.toList()
            }
            Kind.PEAK -> curve(c, firstVisible(c), v.getValue("x"))
            Kind.AREA -> {
                // Die Sonde fliegt ab x = 0 auf f und verbraucht dabei die Fläche unter f als Treibstoff
                val fuel = v.getValue("A"); val out = ArrayList<Vec>(); var x = 0.0; var used = 0.0
                out += Vec(0.0, f(c, 0.0))
                while (x < R) {
                    val nx = min(R, x + STEP)
                    val add = (f(c, x) + f(c, nx)) / 2 * (nx - x)
                    if (used + add >= fuel) { val part = (fuel - used) / add; val ex = x + (nx - x) * part; out += Vec(ex, f(c, ex)); break }
                    used += add; x = nx; out += Vec(x, f(c, x))
                }
                out
            }
            Kind.VECTOR -> {
                val p = Vec(v.getValue("px"), v.getValue("py")); val t = v.getValue("T")
                val q = Vec(p.x + t * v.getValue("v1"), p.y + t * v.getValue("v2"))
                val n = maxOf(1, (kotlin.math.hypot(q.x - p.x, q.y - p.y) / STEP).toInt())
                (0..n).map { i -> val s = i.toDouble() / n; Vec(p.x + (q.x - p.x) * s, p.y + (q.y - p.y) * s) }
            }
            else -> emptyList()
        }
    }

    /** Erste Stelle von links, an der die Kurve im Bild ist */
    private fun firstVisible(c: DoubleArray): Double {
        var x = -R
        while (x < R && abs(f(c, x)) > R) x += STEP
        return x
    }

    private fun curve(c: DoubleArray, from: Double, to: Double): List<Vec> {
        val out = ArrayList<Vec>()
        var x = from
        while (x <= to + 1e-9) { val y = f(c, x); if (abs(y) <= R) out += Vec(x, y); x += STEP }
        if (out.isEmpty() || abs(out.last().x - to) > 1e-6) f(c, to).takeIf { abs(it) <= R }?.let { out += Vec(to, it) }
        return out
    }

    /** Koeffizient genau: Dezimalzahl, wenn sie aufgeht (0,5; 0,125), sonst Bruch (x³/3, 2x³/9) */
    private fun term(a: Double, pow: String): String {
        if (abs(a * 1000 - kotlin.math.round(a * 1000)) < 1e-9) return (if (a == 1.0 && pow.isNotEmpty()) "" else num(a, 3)) + pow
        for (d in 2..64) {
            val n = a * d
            if (abs(n - kotlin.math.round(n)) < 1e-9) {
                val nn = kotlin.math.round(n).toLong()
                return if (pow.isEmpty()) "$nn/$d" else (if (nn == 1L) "" else "$nn") + pow + "/$d"
            }
        }
        return num(a, 3) + pow
    }

    private fun join(coefs: List<Pair<Double, String>>): String {
        val parts = ArrayList<String>()
        for ((k, pow) in coefs) {
            if (k == 0.0) continue
            val t = term(abs(k), pow)
            parts += if (parts.isEmpty()) (if (k < 0) "−" else "") + t else (if (k < 0) "− " else "+ ") + t
        }
        return parts.joinToString(" ").ifEmpty { "0" }
    }

    /** f(x) als Text: „0,5x² − 2x + 1“, „x³/3 − 4x“ */
    fun text(c: DoubleArray) = join(listOf(c[3] to "x³", c[2] to "x²", c[1] to "x", c[0] to ""))
    /** f'(x) als Text */
    fun derivText(c: DoubleArray) = join(listOf(3 * c[3] to "x²", 2 * c[2] to "x", c[1] to ""))
    /** f''(x) als Text */
    fun deriv2Text(c: DoubleArray) = join(listOf(6 * c[3] to "x", 2 * c[2] to ""))
    /** Stammfunktion als Text */
    fun integralText(c: DoubleArray) = join(listOf(c[3] / 4 to "x⁴", c[2] / 3 to "x³", c[1] / 2 to "x²", c[0] to "x"))

    // ------------------------------------------------------------------ Kapitel

    private fun t(x: Double, y: Double, r: Double = 0.6) = Body(x, y, r)
    private fun poly(vararg k: Double) = mapOf("c0" to k.getOrElse(0) { 0.0 }, "c1" to k.getOrElse(1) { 0.0 }, "c2" to k.getOrElse(2) { 0.0 }, "c3" to k.getOrElse(3) { 0.0 })
    private val pm = Param("m", "m", -8.0, 8.0, 0.5, 0.0)
    private val px = Param("x", "x", -9.0, 9.0, 0.5, 0.0)
    private val pA = Param("A", "Treibstoff A", 0.0, 40.0, 0.25, 1.0)
    private fun pv(k: String, l: String) = Param(k, l, -9.0, 9.0, 0.5, 1.0)

    private fun tangent(n: Int, title: String, c: Map<String, Double>, x0: Double, m: Double, xs: Double, extra: String = "", rocks: List<Body> = emptyList()): Level {
        val k = c(c); val y0 = f(k, x0)
        return Level("19-$n", 19, n, title, Kind.TANGENT,
            "Die Sonde gleitet auf f(x) = ${text(k)} und löst sich bei x₀ = ${num(x0)} – tangential! Stell die Steigung m ein.$extra",
            listOf(t(xs, y0 + m * (xs - x0))), listOf(pm), mapOf("m" to m), fixed = c + ("x0" to x0), asteroids = rocks,
            tip = "Tangente: m = f'(x₀). f'(x) = ${derivText(k)} → f'(${num(x0)}) = ${num(m)}.")
    }

    private fun peak(n: Int, title: String, c: Map<String, Double>, x: Double, what: String, tip: String, start: Double = 0.0): Level {
        val k = c(c)
        return Level("20-$n", 20, n, title, Kind.PEAK, "f(x) = ${text(k)}. Die Sonde fliegt die Kurve ab und hält bei x. Halte genau am $what!",
            listOf(t(x, f(k, x), 0.35)), listOf(px.copy(start = start)), mapOf("x" to x), fixed = c, hidden = true, tip = tip)
    }

    private fun area(n: Int, title: String, c: Map<String, Double>, xs: Double, tip: String): Level {
        val k = c(c); val a = F(k, xs) - F(k, 0.0)
        return Level("21-$n", 21, n, title, Kind.AREA,
            "f(x) = ${text(k)}. Der Treibstoff ist die Fläche unter f ab x = 0. Wie viel brauchst du bis zur Station bei x = ${num(xs)}?",
            listOf(t(xs, f(k, xs), 0.03)), listOf(pA), mapOf("A" to a), fixed = c, tip = tip)
    }

    private fun vector(n: Int, title: String, p: Vec, tt: Double, v: Vec, task: String, extra: List<Body> = emptyList(), rocks: List<Body> = emptyList()): Level {
        val q = Vec(p.x + tt * v.x, p.y + tt * v.y)
        return Level("22-$n", 22, n, title, Kind.VECTOR, task, extra + t(q.x, q.y, 0.45), listOf(pv("v1", "v₁"), pv("v2", "v₂")),
            mapOf("v1" to v.x, "v2" to v.y), fixed = mapOf("px" to p.x, "py" to p.y, "T" to tt), launch = p, asteroids = rocks,
            tip = "v = (Q − P) : t = ((${num(q.x)} − ${num(p.x)}) : ${num(tt)} | (${num(q.y)} − ${num(p.y)}) : ${num(tt)}).")
    }

    val chapters: List<Chapter> = listOf(
        Chapter(19, "Ableitung & Tangente", "Klasse 11", "🏂",
            listOf(
                "Die Sonde gleitet auf einer Kurve wie ein Snowboard auf der Piste. An einer Stelle x₀ springt sie ab – geradeaus, genau in Richtung der Kurve.",
                "Diese Gerade heißt Tangente. Ihre Steigung ist die Ableitung: m = f'(x₀).",
                "Ableiten: x² wird zu 2x, x³ zu 3x², x zu 1, Zahlen fallen weg. Davor stehende Faktoren bleiben.",
                "Also: f'(x) bilden, x₀ einsetzen, fertig ist m. Los!",
            ),
            listOf(
                tangent(1, "Absprung", poly(0.0, 0.0, 0.5), 2.0, 2.0, 5.0),
                tangent(2, "Bergab", poly(4.0, 0.0, -0.25), -2.0, 1.0, 4.0),
                tangent(3, "Verschoben", poly(0.0, -2.0, 1.0), 3.0, 4.0, 4.5),
                tangent(4, "Hügelkante", poly(4.0, 0.0, -0.5), 2.0, -2.0, 5.0),
                tangent(5, "Kubisch", poly(0.0, 0.0, 0.0, 0.125), 2.0, 1.5, 6.0, rocks = listOf(Body(4.0, 2.0, 0.8))),
                tangent(6, "Wendig", poly(0.0, -1.0, 0.0, 1.0 / 9), -3.0, 2.0, 1.0, " Achtung, Asteroid!", listOf(Body(-1.0, 1.0, 0.8))),
            )),
        Chapter(20, "Extrempunkte", "Klasse 11", "⛰️",
            listOf(
                "Ganz oben auf dem Berg ist die Kurve kurz waagrecht – die Steigung ist dort 0.",
                "Also: f'(x) = 0 setzen und nach x auflösen. Das sind die Kandidaten für Hoch- und Tiefpunkte.",
                "Hoch oder tief? f''(x) < 0: Hochpunkt (Berg). f''(x) > 0: Tiefpunkt (Tal).",
                "Und wo die Kurve von Links- in Rechtskurve wechselt, ist f''(x) = 0: der Wendepunkt. Die Station ist versteckt – rechne!",
            ),
            listOf(
                peak(1, "Gipfel", poly(1.0, 2.0, -0.5), 2.0, "Hochpunkt", "f'(x) = −x + 2 = 0 → x = 2."),
                peak(2, "Talsohle", poly(1.0, 3.0, 0.5), -3.0, "Tiefpunkt", "f'(x) = x + 3 = 0 → x = −3."),
                peak(3, "Doppelgipfel", poly(0.0, -4.0, 0.0, 1.0 / 3), -2.0, "Hochpunkt", "f'(x) = x² − 4 = 0 → x = ±2. f''(−2) = −4 < 0: Hochpunkt bei x = −2."),
                peak(4, "Tiefer Krater", poly(0.0, -4.0, 0.0, 1.0 / 3), 2.0, "Tiefpunkt", "f'(x) = x² − 4 = 0 → x = ±2. f''(2) = 4 > 0: Tiefpunkt bei x = 2.", start = -4.0),
                peak(5, "Hoher Kamm", poly(-2.0, 3.0, 1.0, -1.0 / 3), 3.0, "Hochpunkt", "f'(x) = −x² + 2x + 3 = 0 → x = 3 oder x = −1. f''(3) = −4 < 0."),
                peak(6, "Wendepunkt", poly(0.0, 0.0, -1.0, 1.0 / 6), 2.0, "Wendepunkt", "f''(x) = x − 2 = 0 → x = 2."),
            )),
        Chapter(21, "Integral", "Klasse 12", "⛽",
            listOf(
                "Neuer Antrieb: Die Sonde fliegt auf der Kurve und verbraucht dabei Treibstoff – so viel wie die Fläche unter der Kurve.",
                "Die Fläche von 0 bis b ist das Integral: ∫₀ᵇ f(x) dx = F(b) − F(0).",
                "F ist die Stammfunktion: aus x wird x²/2, aus x² wird x³/3, aus einer Zahl k wird k·x.",
                "Tank genau so viel, dass die Sonde an der Station ankommt!",
            ),
            listOf(
                area(1, "Rechteck", poly(2.0), 4.0, "Konstante Höhe 2, Breite 4: Fläche 2 · 4 = 8."),
                area(2, "Dreieck", poly(0.0, 1.0), 6.0, "F(x) = x²/2 → F(6) = 18."),
                area(3, "Trapez", poly(1.0, 0.5), 4.0, "F(x) = x + x²/4 → F(4) = 4 + 4 = 8."),
                area(4, "Parabelbogen", poly(0.0, 0.0, 0.25), 6.0, "F(x) = x³/12 → F(6) = 216 : 12 = 18."),
                area(5, "Kuppel", poly(3.0, 0.0, -0.25), 3.0, "F(x) = 3x − x³/12 → F(3) = 9 − 2,25 = 6,75."),
                area(6, "Steil", poly(0.0, 0.0, 0.0, 1.0 / 16), 4.0, "F(x) = x⁴/64 → F(4) = 256 : 64 = 4."),
            )),
        Chapter(22, "Vektoren", "Klasse 11–13", "🧭",
            listOf(
                "Ein Vektor ist ein Pfeil: v = (v₁ | v₂) – v₁ nach rechts, v₂ nach oben.",
                "Die Sonde startet bei P und fliegt t Sekunden lang mit v: X = P + t · v.",
                "Soll sie nach t Sekunden bei Q sein, gilt: v = (Q − P) : t – für jede Koordinate einzeln.",
                "Stell v₁ und v₂ ein!",
            ),
            listOf(
                vector(1, "Erster Pfeil", Vec(-6.0, -4.0), 1.0, Vec(8.0, 6.0), "Start P = (−6 | −4), Flugzeit t = 1. Station Q = (2 | 2). Welcher Vektor v bringt dich hin?"),
                vector(2, "Halbe Kraft", Vec(-6.0, -6.0), 2.0, Vec(5.0, 3.0), "Start P = (−6 | −6), t = 2. Station Q = (4 | 0)."),
                vector(3, "Rückwärts", Vec(4.0, 5.0), 2.0, Vec(-4.0, -4.0), "Start P = (4 | 5), t = 2. Station Q = (−4 | −3) – es geht nach links unten!"),
                vector(4, "Zwei Stationen", Vec(-8.0, -6.0), 3.0, Vec(3.0, 2.0), "Start P = (−8 | −6), t = 3. Erst durch (−5 | −4), am Ende bei (1 | 0).",
                    extra = listOf(t(-5.0, -4.0, 0.45))),
                vector(5, "Steil hinauf", Vec(0.0, -8.0), 4.0, Vec(-1.5, 4.0), "Start P = (0 | −8), t = 4. Station Q = (−6 | 8)."),
            )),
    )
}
