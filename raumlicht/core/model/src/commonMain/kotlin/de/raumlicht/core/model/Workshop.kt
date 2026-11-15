package de.raumlicht.core.model

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

// ───────────────────────── Achterbahn-Baumeister ─────────────────────────

/** Was bei einer Probefahrt herauskommt. [xs]: Ort des Wagens Bild für Bild (60 pro Sekunde). */
class TrackRun(val xs: List<Double>, val finished: Boolean, val endSpeed: Double, val topSpeed: Double, val stuckAt: Double?, val stars: Int) {
    val seconds get() = xs.size / 60.0
}
class TrackGoal(val title: String, val text: String, val start: List<Int>, val stars: List<Pair<Double, Double>> = emptyList(), val minEndSpeed: Double = 0.0, val solution: List<Int>)

/**
 * Selbst gebaute Bahn: fünf Stützpunkte (bei x = 0, 2, 4, 6, 8) mit Höhen von 1 bis 6, dazwischen eine glatte Kurve (kubischer Spline).
 * Der Wagen fährt mit echter Physik: bergab wird er schneller, bergauf langsamer – ist ein Hügel höher, als sein Schwung reicht, rollt er zurück.
 */
class BuiltTrack(val heights: List<Int>) {
    private val n = heights.size - 1
    // natürlicher kubischer Spline: zweite Ableitungen an den Stützstellen (Thomas-Algorithmus)
    private val m: DoubleArray = DoubleArray(n + 1).also { m ->
        val a = DoubleArray(n + 1); val b = DoubleArray(n + 1) { 1.0 }; val c = DoubleArray(n + 1); val d = DoubleArray(n + 1)
        for (i in 1 until n) { a[i] = DX; b[i] = 4 * DX; c[i] = DX; d[i] = 6.0 * (heights[i + 1] - 2 * heights[i] + heights[i - 1]) / DX }
        for (i in 1..n) { val w = a[i] / b[i - 1]; b[i] -= w * c[i - 1]; d[i] -= w * d[i - 1] }
        m[n] = d[n] / b[n]; for (i in n - 1 downTo 0) m[i] = (d[i] - c[i] * m[i + 1]) / b[i]
    }
    val length get() = n * DX
    private fun seg(x: Double) = (x / DX).toInt().coerceIn(0, n - 1)
    /** Höhe der Bahn */
    fun y(x: Double): Double { val i = seg(x); val a = (i + 1) * DX - x; val b = x - i * DX
        return (m[i] * a * a * a + m[i + 1] * b * b * b) / (6 * DX) + (heights[i] / DX - m[i] * DX / 6) * a + (heights[i + 1] / DX - m[i + 1] * DX / 6) * b }
    /** Steigung der Bahn */
    fun slope(x: Double): Double { val i = seg(x); val a = (i + 1) * DX - x; val b = x - i * DX
        return (-m[i] * a * a + m[i + 1] * b * b) / (2 * DX) - (heights[i] / DX - m[i] * DX / 6) + (heights[i + 1] / DX - m[i + 1] * DX / 6) }
    fun at(x: Double) = V3(4.0, x, y(x).coerceIn(0.15, 6.7))

    /** Probefahrt: Start ganz links mit einem kleinen Schubs. */
    fun run(goal: TrackGoal? = null): TrackRun {
        val xs = ArrayList<Double>(); var x = 0.0; var v = PUSH; var top = v; var stuck: Double? = null; var turned = false
        val dt = 1 / 60.0
        while (xs.size < 60 * 14) {
            xs += x
            val s = slope(x); val norm = sqrt(1 + s * s)
            // Hangabtrieb minus Rollreibung
            val a = -G * s / norm - FRICTION * G / norm * (if (v > 0) 1 else if (v < 0) -1 else 0)
            val v2 = v + a * dt
            if (v > 0 && v2 <= 0 && !turned) { turned = true; stuck = x }                 // hier geht dem Wagen der Schwung aus
            v = v2; x += v / norm * dt; top = maxOf(top, abs(v))
            if (x >= length) { xs += length; return TrackRun(xs, true, v, top, null, stars(goal, length)) }
            if (x <= 0.0 && turned) { xs += 0.0; break }
            if (turned && abs(v) < 0.03 && abs(a) < 0.2) break                              // im Tal ausgerollt
        }
        return TrackRun(xs, false, 0.0, top, stuck ?: x, stars(goal, stuck ?: x))
    }
    /** Sterne zählen, wenn die Bahn durch sie führt und der Wagen bis dorthin kommt */
    private fun stars(goal: TrackGoal?, reach: Double) = goal?.stars?.count { (sx, sy) -> sx <= reach + 1e-9 && abs(y(sx) - sy) < 0.45 } ?: 0
    fun solves(goal: TrackGoal, r: TrackRun = run(goal)) = r.finished && r.stars == goal.stars.size && r.endSpeed >= goal.minEndSpeed

    /** Bühne: Bahn, Griffe an den Stützpunkten (nur hoch und runter), Sterne des Auftrags, Ziel-Flagge */
    fun scene(goal: TrackGoal?, handles: Boolean = true): String = Stage.encode(Bounds(), listOf(
        Obj("bahn", "track", corners = (0..64).map { at(length * it / 64).list }, color = "cyan"),
        Stage.axes2d(0.0, 0.0, 1.0, 1.0, 0.0, length),
        Stage.point("ziel", at(length) + V3(0.0, 0.0, 0.6), "green", "Ziel", style = "ghost", drop = false),
    ) + (if (handles) heights.indices.map { i -> Obj("h$i", "handle", p = listOf(4.0, i * DX, heights[i].toDouble()), color = "yellow", axes = listOf(2)) } else emptyList()) +
        goal?.stars.orEmpty().mapIndexed { i, (sx, sy) -> Stage.point("stern$i", V3(4.0, sx, sy), "yellow", "★", style = "target", drop = false) }, lock = true)

    companion object {
        const val DX = 2.0; const val G = 5.0; const val FRICTION = 0.03; const val PUSH = 0.8
        val HEIGHTS = 1..6
        val goals = listOf(
            TrackGoal("Erste Fahrt", "Bring den Wagen ins Ziel. Tipp: Er braucht Gefälle – auf ebener Strecke rollt er aus.", listOf(3, 3, 3, 3, 3), solution = listOf(5, 4, 3, 2, 1)),
            TrackGoal("Sternenhügel", "Hol den Stern auf dem Hügel – und komm trotzdem an.", listOf(3, 3, 3, 3, 3), listOf(4.0 to 4.0), solution = listOf(6, 2, 4, 2, 1)),
            TrackGoal("Tal und Berg", "Zwei Sterne: einer im Tal, einer auf dem Berg.", listOf(4, 4, 4, 4, 4), listOf(2.0 to 1.0, 6.0 to 4.0), solution = listOf(6, 1, 3, 4, 2)),
            TrackGoal("Mit Schwung ins Ziel", "Rausch mit mindestens Tempo 6 durchs Ziel.", listOf(3, 3, 3, 3, 3), minEndSpeed = 6.0, solution = listOf(6, 4, 3, 2, 1)),
            TrackGoal("Dreierlei", "Drei Sterne: Tal, Gipfel, Tal. Der Gipfel darf nicht höher sein, als dein Schwung reicht.", listOf(4, 4, 4, 4, 4), listOf(2.0 to 2.0, 4.0 to 5.0, 6.0 to 2.0), solution = listOf(6, 2, 5, 2, 1)),
        )
    }
}

// ───────────────────────── Geisterfahrt gegen f′ ─────────────────────────

/** Grafisches Ableiten als Zeichenspiel: zu einer Kurve f die Ableitung mit dem Finger skizzieren, dann mit der echten f′ vergleichen. */
object Ghost {
    const val COLUMNS = 48
    /** Zeichenfenster für f′: alle Werte von f′ im Abschnitt, mit etwas Luft, die Nulllinie immer dabei */
    fun window(r: Ride): Pair<Double, Double> {
        val v = (0..COLUMNS).map { r.df(r.from + (r.to - r.from) * it / COLUMNS) }
        val lo = minOf(v.min(), 0.0); val hi = maxOf(v.max(), 0.0); val pad = maxOf((hi - lo) * 0.2, 0.5)
        return (lo - pad) to (hi + pad)
    }
    fun x(r: Ride, col: Int) = r.from + (r.to - r.from) * (col + 0.5) / COLUMNS
    /** Trefferquote in Prozent: mittlerer Abstand zur echten Ableitung, gemessen an der Höhe des Fensters. Nicht gezeichnete Spalten zählen als daneben. */
    fun score(r: Ride, guess: List<Double?>): Int {
        val (lo, hi) = window(r); val span = hi - lo
        val err = guess.indices.map { c -> guess[c]?.let { minOf(abs(it - r.df(x(r, c))) / span, 1.0) } ?: 1.0 }.average()
        return (100 * (1 - 2.5 * err)).toInt().coerceIn(0, 100)                                // 40 % der Fensterhöhe daneben = 0 Punkte
    }
    fun verdict(score: Int) = when { score >= 90 -> "Wie abgepaust! $score %."; score >= 75 -> "Stark: $score % – die Form stimmt."; score >= 50 -> "$score % – die Richtung passt, schau auf die Hoch- und Tiefpunkte: Dort muss f′ null sein."
        else -> "$score % – Tipp: Wo f steigt, liegt f′ über der Achse; wo f fällt, darunter." }
    val drawn = COLUMNS * 7 / 10
}

// ───────────────────────── Raumflug mit Ringen ─────────────────────────

/** Runde beim Ringflug: Start [a] und drei Ringe auf einer Geraden. Gesucht ist der Richtungsvektor, mit dem Lux durch alle fliegt. */
class RingRun(seed: Long) {
    val a: V3; val d: V3
    init {
        val r = Random(seed); var aa: V3; var dd: V3
        do { aa = V3(r.nextInt(0, 9), r.nextInt(0, 9), r.nextInt(1, 6)); dd = V3(r.nextInt(-2, 3), r.nextInt(-2, 3), r.nextInt(-1, 2)) }
        while (dd.isZero || (abs(dd.x) + abs(dd.y) == 0.0) || !inside(aa + dd * 3.0) || gcd(dd) != 1)
        a = aa; d = dd
    }
    val rings: List<V3> = (1..3).map { a + d * it.toDouble() }
    private fun inside(p: V3) = p.x in 0.0..8.0 && p.y in 0.0..8.0 && p.z in 1.0..6.0
    private fun gcd2(a: Int, b: Int): Int = if (b == 0) abs(a) else gcd2(b, a % b)
    private fun gcd(v: V3): Int = gcd2(gcd2(v.x.toInt(), v.y.toInt()), v.z.toInt())
    /** Abstand jedes Rings von der Flugbahn A + t · guess (nur nach vorn, t ≥ 0) */
    fun misses(guess: V3): List<Double> = rings.map { p -> if (guess.isZero) (p - a).length else { val t = maxOf(0.0, ((p - a) dot guess) / (guess dot guess)); (p - (a + guess * t)).length } }
    fun hits(guess: V3) = misses(guess).count { it < RADIUS }
    /** Wo die Flugbahn den Raum verlässt */
    fun tMax(guess: V3) = if (guess.isZero) 0.0 else Geo.exit(a, guess, Bounds().lo, Bounds().hi).coerceIn(0.0, 12.0)
    fun verdict(guess: V3): String { val m = misses(guess); val h = hits(guess)
        return when { guess.isZero -> "Mit (0|0|0) fliege ich nirgendwohin."; h == 3 -> "Alle drei Ringe! Richtung = Ring minus Start."
            else -> "$h von 3. Ring ${m.indexOfFirst { it >= RADIUS } + 1} verfehlt um ${Geo.num(m.first { it >= RADIUS })}. Tipp: Richtung = R₁ − A." } }
    fun scene(guess: V3): String { val t = tMax(guess)
        return Stage.encode(Bounds(), listOf(
            Obj("bahn", "track", corners = (0..40).map { (a + guess * (t * it / 40)).list }.ifEmpty { listOf(a.list) }, color = "cyan", style = "line"),
            Stage.point("start", a, "cyan", "A${a.fmt()}"),
        ) + rings.flatMapIndexed { i, p -> listOf(Stage.circle("ring$i", p, d, RADIUS, if (misses(guess)[i] < RADIUS) "green" else "yellow"),
            Stage.point("mitte$i", p, "yellow", "R" + SUB[i], style = "ghost", drop = false)) })
    }
    companion object { const val RADIUS = 0.45; private val SUB = listOf("₁", "₂", "₃") }
}
