package de.kurvenflug.core.model

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin

/** Punkt im Koordinatensystem (eine Einheit = ein Kästchen, beim Wurf = ein Meter). */
data class Vec(val x: Double, val y: Double)

/** Rundes Objekt: Ziel-Station, Asteroid oder Energiezelle. */
data class Body(val x: Double, val y: Double, val r: Double) {
    fun contains(p: Vec, extra: Double = 0.0) = hypot(p.x - x, p.y - y) <= r + extra
}

/**
 * Die Spielarten von Kurvenflug. Jede ist ein Stück Schulstoff – das Rechnen ist die Steuerung selbst.
 * - [POINT]: Koordinaten (Kl. 5/6) – erst x nach rechts/links, dann y nach oben/unten.
 * - [LINE]: Geraden y = m·x + b (Kl. 7/8)
 * - [PARABOLA]: Parabeln in Scheitelform y = a·(x − d)² + e (Kl. 9)
 * - [THROW]: schräger Wurf mit Winkel und Geschwindigkeit auf Erde, Mond, Mars (Physik Kl. 7–10)
 * - [SINE]: Schwingungen y = a·sin(b·x) + d (Kl. 10)
 * - [EXP]: Wachstum und Zerfall y = a·bˣ + d (Kl. 10)
 * - [ORBIT]: Schwerkraft-Schleuder – Planeten ziehen die Sonde an, Kraft ~ M / r² (Physik Kl. 10)
 * - [RENDEZVOUS]: zwei Sonden auf zwei Geraden – Treffpunkt = Lösung des Gleichungssystems (Kl. 8)
 * - [PROP] / [ANTI]: proportionale y = k·x und antiproportionale Zuordnung y = k : x (Kl. 6/7)
 * - [LOG]: Logarithmus y = log_b(x) + d (Kl. 10)
 * - [RADIUS]: Funkwelle mit Radius r – Abstand per Pythagoras (Kl. 8/9)
 * - [LASER]: Laserstrahl über Spiegel – Einfallswinkel = Ausfallswinkel (Physik Kl. 7)
 * - [RAMP]: Kugel rollt von Höhe h die Rampe hinab – Energieerhaltung (Physik Kl. 8/9)
 * - [MOTION]: gleichförmige Bewegung im s-t-Diagramm s = v·t + s₀ (Physik Kl. 7)
 * - [TANGENT], [PEAK], [AREA], [VECTOR]: Oberstufe (Ableitung, Extrempunkte, Integral, Vektoren), siehe [Upper]
 */
enum class Kind { POINT, LINE, PARABOLA, THROW, SINE, EXP, ORBIT, RENDEZVOUS, PROP, ANTI, LOG, RADIUS, LASER, RAMP, MOTION, TANGENT, PEAK, AREA, VECTOR, ACCEL, POWER, EXPE, BINOM }

/** Hat diese Spielart eine Funktionsgleichung y = f(x)? */
val Kind.isFunction get() = this in setOf(Kind.LINE, Kind.PARABOLA, Kind.SINE, Kind.EXP, Kind.PROP, Kind.ANTI, Kind.LOG, Kind.MOTION, Kind.ACCEL, Kind.POWER, Kind.EXPE, Kind.BINOM)

/** Funktionen, die erst rechts von x = 0 definiert/sinnvoll sind (Zeit, Logarithmus, k : x). */
val Kind.startsAtZero get() = this == Kind.ANTI || this == Kind.LOG || this == Kind.MOTION || this == Kind.ACCEL

/** Einstellbarer Wert in der Formel. */
data class Param(
    val key: String,
    val label: String,
    val min: Double,
    val max: Double,
    val step: Double,
    val start: Double,
    val unit: String = "",
) {
    /** Auf das Raster einrasten und begrenzen (verhindert 0,30000000004). */
    fun snap(v: Double): Double {
        val s = round((v.coerceIn(min, max) - min) / step) * step + min
        return round(s * 1000) / 1000
    }
}

/** Himmelskörper für den Wurf – echte Fallbeschleunigungen. */
enum class Planet(val label: String, val g: Double, val emoji: String) {
    EARTH("Erde", 9.81, "🌍"), MOON("Mond", 1.62, "🌕"), MARS("Mars", 3.71, "🔴")
}

/** Ergebnis eines Flugs. */
data class FlightResult(
    /** sichtbarer Weg der Sonde (bis zum Absturz / Verlassen des Bildes) */
    val path: List<Vec>,
    val hitTargets: Set<Int>,
    val cells: Set<Int>,
    /** Index des getroffenen Asteroiden oder null */
    val crash: Int?,
    val success: Boolean,
    /** zweite Sonde (nur Rendezvous) */
    val path2: List<Vec> = emptyList(),
)

object Flight {
    /** Spielfeld: −10 … 10 in beide Richtungen. */
    const val R = 10.0
    private const val STEP = 0.02

    /** Wert der Funktion an der Stelle x (nur für Funktions-Level). */
    fun f(level: Level, v: Map<String, Double>, x: Double): Double {
        val p = level.values(v)
        return when (level.kind) {
            Kind.LINE -> p.getValue("m") * x + p.getValue("b")
            Kind.PARABOLA -> p.getValue("a") * (x - p.getValue("d")).let { it * it } + p.getValue("e")
            // c verschiebt die Welle nach rechts (fehlt c, ist es 0 – frühere Level bleiben gleich)
            Kind.SINE -> p.getValue("a") * sin(p.getValue("b") * (x - (p["c"] ?: 0.0))) + p.getValue("d")
            Kind.EXP -> p.getValue("a") * powK(p.getValue("b"), x) + p.getValue("d")
            Kind.PROP -> p.getValue("k") * x
            Kind.ANTI -> if (x == 0.0) Double.POSITIVE_INFINITY else p.getValue("k") / x
            Kind.LOG -> if (x <= 0) Double.NEGATIVE_INFINITY else kotlin.math.ln(x) / kotlin.math.ln(p.getValue("b")) + p.getValue("d")
            Kind.MOTION -> p.getValue("v") * x + p.getValue("s0")
            Kind.ACCEL -> 0.5 * p.getValue("a") * x * x + p.getValue("v0") * x + (p["s0"] ?: 0.0)
            // Potenz: bei gebrochener Hochzahl (Wurzel) nur für x ≥ 0
            Kind.POWER -> { val n = p.getValue("n"); if (x < 0 && n != kotlin.math.round(n)) Double.NaN else p.getValue("a") * powK(x, n) + (p["d"] ?: 0.0) }
            Kind.EXPE -> p.getValue("a") * kotlin.math.exp(p.getValue("k") * x) + (p["d"] ?: 0.0)
            // Histogramm: Säule k steht bei x = k − 9 (Breite 1), Höhe 10 = 100 %
            Kind.BINOM -> Stochastik.height(p, x)
            else -> error("keine Funktion")
        }
    }

    /** Die ganze Bahn als Punktfolge (noch ohne Kollisionen). */
    fun path(level: Level, values: Map<String, Double>): List<Vec> {
        val v = level.values(values)
        return when (level.kind) {
            Kind.POINT -> {
                // Erst auf der x-Achse entlang, dann nach oben/unten – so „liest“ man Koordinaten
                val x = v.getValue("x"); val y = v.getValue("y")
                line(Vec(0.0, 0.0), Vec(x, 0.0)) + line(Vec(x, 0.0), Vec(x, y)).drop(1)
            }
            Kind.LINE, Kind.PARABOLA, Kind.SINE, Kind.EXP, Kind.PROP, Kind.ANTI, Kind.LOG, Kind.MOTION, Kind.ACCEL, Kind.POWER, Kind.EXPE, Kind.BINOM -> {
                // Sichtbarer Teil des Graphen von links nach rechts – die Sonde „springt“ am Rand ins Bild
                val out = ArrayList<Vec>()
                var x = if (level.kind.startsAtZero) STEP else -R
                while (x <= R + 1e-9) {
                    val y = f(level, values, x)
                    if (abs(y) <= R) out += Vec(x, y) else if (out.isNotEmpty()) break
                    x += STEP
                }
                out
            }
            Kind.THROW -> {
                val a = toRadians(v.getValue("angle")); val s = v.getValue("v"); val g = level.planet.g
                val vx = s * cos(a); val vy = s * sin(a)
                val out = ArrayList<Vec>()
                // Zeitschritt so, dass die Punkte etwa STEP auseinander liegen
                val dt = STEP / max(0.5, s)
                var t = 0.0
                while (t < 60) {
                    val p = Vec(level.launch.x + vx * t, level.launch.y + vy * t - 0.5 * g * t * t)
                    out += p
                    if (p.y < level.ground || p.x > R + 0.5 || p.x < -R - 0.5) break
                    t += dt
                }
                out
            }
            Kind.ORBIT -> orbit(level, v)
            Kind.RENDEZVOUS -> lineTo(v.getValue("m1"), v.getValue("b1"), -R, v.getValue("x"))
            Kind.RADIUS -> {
                // Funkwelle: Kreis um den Sender, gegen den Uhrzeigersinn abgefahren
                val r = v.getValue("r"); val c = level.launch
                val n = max(60, (2 * kotlin.math.PI * r / STEP).toInt())
                (0..n).map { i -> val a = 2 * kotlin.math.PI * i / n; Vec(c.x + r * cos(a), c.y + r * sin(a)) }
            }
            Kind.LASER -> laser(level, v.getValue("angle"))
            Kind.RAMP -> ramp(level, v.getValue("h"))
            Kind.TANGENT, Kind.PEAK, Kind.AREA, Kind.VECTOR -> Upper.path(level, values)
        }
    }

    /**
     * Laserstrahl: geradeaus, an Spiegeln reflektiert (Einfallswinkel = Ausfallswinkel), höchstens 12 Knicke.
     * Mit Kristall ([Level.crystal], Rechteck mit Brechzahl n): an jeder Kante gebrochen nach Snellius
     * n₁·sin α₁ = n₂·sin α₂ – oder totalreflektiert, wenn es keinen Brechungswinkel gibt.
     */
    private fun laser(level: Level, angleDeg: Double): List<Vec> {
        var px = level.launch.x; var py = level.launch.y
        val a = toRadians(angleDeg)
        var dx = cos(a); var dy = sin(a)
        val out = arrayListOf(Vec(px, py))
        // Flächen: Spiegel, dann die vier Kristallkanten
        val c = level.crystal
        val faces = level.mirrors + (c?.let { k -> listOf(
            listOf(k[0], k[1], k[2], k[1]), listOf(k[2], k[1], k[2], k[3]), listOf(k[2], k[3], k[0], k[3]), listOf(k[0], k[3], k[0], k[1]),
        ) } ?: emptyList())
        fun inside(x: Double, y: Double) = c != null && x > c[0] && x < c[2] && y > c[1] && y < c[3]
        var last = -1
        repeat(13) {
            var best = Double.MAX_VALUE; var hit = -1
            faces.forEachIndexed { i, m ->
                if (i == last) return@forEachIndexed
                val ex = m[2] - m[0]; val ey = m[3] - m[1]
                val den = dx * ey - dy * ex
                if (abs(den) < 1e-12) return@forEachIndexed
                val t = ((m[0] - px) * ey - (m[1] - py) * ex) / den
                val u = ((m[0] - px) * dy - (m[1] - py) * dx) / den
                if (t > 1e-9 && u in 0.0..1.0 && t < best) { best = t; hit = i }
            }
            val edge = listOf(if (dx > 0) (R + 0.5 - px) / dx else if (dx < 0) (-R - 0.5 - px) / dx else Double.MAX_VALUE,
                if (dy > 0) (R + 0.5 - py) / dy else if (dy < 0) (-R - 0.5 - py) / dy else Double.MAX_VALUE).min()
            val t = min(best, edge)
            val n = max(1, (t / STEP).toInt())
            for (i in 1..n) out += Vec(px + dx * t * i / n, py + dy * t * i / n)
            if (hit < 0 || best > edge) return out
            val wasInside = inside(px + dx * t * 0.5, py + dy * t * 0.5)
            px += dx * t; py += dy * t
            val m = faces[hit]
            val ex = m[2] - m[0]; val ey = m[3] - m[1]; val len = hypot(ex, ey)
            var nx = -ey / len; var ny = ex / len
            if (hit < level.mirrors.size) {
                // Spiegeln an der Normalen
                val d = dx * nx + dy * ny
                dx -= 2 * d * nx; dy -= 2 * d * ny
            } else {
                // Brechung: Normale gegen die Laufrichtung drehen
                if (dx * nx + dy * ny > 0) { nx = -nx; ny = -ny }
                val n1 = if (wasInside) c!![4] else 1.0; val n2 = if (wasInside) 1.0 else c!![4]
                val r = n1 / n2
                val cosI = -(dx * nx + dy * ny)
                val k = 1 - r * r * (1 - cosI * cosI)
                if (k < 0) { dx += 2 * cosI * nx; dy += 2 * cosI * ny }          // Totalreflexion
                else { val f = r * cosI - kotlin.math.sqrt(k); dx = r * dx + f * nx; dy = r * dy + f * ny }
                val l = hypot(dx, dy); dx /= l; dy /= l
            }
            last = hit
        }
        return out
    }

    /**
     * Rampe: Kugel startet in Höhe h (Meter über dem Boden), rollt reibungsfrei bis zur Kante in Höhe h0
     * und fliegt waagerecht ab mit v = √(2·g·(h − h0)) – Lageenergie wird Bewegungsenergie.
     */
    private fun ramp(level: Level, h: Double): List<Vec> {
        val v = level.values(emptyMap())
        val h0 = v.getValue("h0"); val g = level.planet.g
        val groundY = level.ground
        val edge = Vec(RAMP_EDGE_X, groundY + h0)
        val top = groundY + RAMP_TOP
        // Rampenform: y = Kante + (Spitze − Kante)·(1 − s)², waagerecht an der Kante
        val y0 = groundY + h
        if (h <= h0) return listOf(rampPoint(level, 1 - kotlin.math.sqrt(((y0 - edge.y) / (top - edge.y)).coerceIn(0.0, 1.0))))
        val s0 = 1 - kotlin.math.sqrt(((y0 - edge.y) / (top - edge.y)).coerceIn(0.0, 1.0))
        val out = ArrayList<Vec>()
        val n = 120
        for (i in 0..n) out += rampPoint(level, s0 + (1 - s0) * i / n)
        val speed = kotlin.math.sqrt(2 * g * (h - h0))
        val dt = STEP / max(0.5, speed)
        var t = dt
        while (t < 30) {
            val p = Vec(edge.x + speed * t, edge.y - 0.5 * g * t * t)
            out += p
            if (p.y < groundY || p.x > R + 0.5) break
            t += dt
        }
        return out
    }

    const val RAMP_EDGE_X = -4.0
    const val RAMP_START_X = -9.5
    /** Höhe der Rampenspitze über dem Boden (m) */
    const val RAMP_TOP = 18.0

    fun rampPoint(level: Level, s: Double): Vec {
        val h0 = level.values(emptyMap()).getValue("h0")
        val edgeY = level.ground + h0; val top = level.ground + RAMP_TOP
        return Vec(RAMP_START_X + (RAMP_EDGE_X - RAMP_START_X) * s, edgeY + (top - edgeY) * (1 - s) * (1 - s))
    }

    /** Rendezvous: Bahn der zweiten Sonde – kommt von rechts. */
    fun path2(level: Level, values: Map<String, Double>): List<Vec> {
        if (level.kind != Kind.RENDEZVOUS) return emptyList()
        val v = level.values(values)
        return lineTo(v.getValue("m2"), v.getValue("b2"), R, v.getValue("x"))
    }

    /** Gerade y = m·x + b von x0 bis x1, nur der sichtbare Teil. */
    private fun lineTo(m: Double, b: Double, x0: Double, x1: Double): List<Vec> {
        val n = max(1, (abs(x1 - x0) / STEP).toInt())
        return (0..n).map { i -> val x = x0 + (x1 - x0) * i / n; Vec(x, m * x + b) }.filter { abs(it.y) <= R }
            .ifEmpty { listOf(Vec(x1, m * x1 + b)) }
    }

    /** Wo sich die beiden Rendezvous-Geraden schneiden (null = parallel). */
    fun intersection(m1: Double, b1: Double, m2: Double, b2: Double): Vec? {
        if (abs(m1 - m2) < 1e-9) return null
        val x = (b2 - b1) / (m1 - m2)
        return Vec(x, m1 * x + b1)
    }

    /** Gravitationskonstante im Spiel (Einheiten so gewählt, dass Massen 10–60 gut spielbar sind). */
    const val G = 1.0

    /**
     * Schwerkraft-Schleuder: Jeder Planet zieht mit a = G·M / r² in seine Richtung.
     * Schritt für Schritt gerechnet (Leapfrog-Verfahren – hält die Energie stabil, keine falschen Spiralen).
     */
    private fun orbit(level: Level, v: Map<String, Double>): List<Vec> {
        val a = toRadians(v.getValue("angle")); val s = v.getValue("v")
        var x = level.launch.x; var y = level.launch.y
        var vx = s * cos(a); var vy = s * sin(a)
        val wells = level.asteroids.zip(level.masses).filter { it.second > 0 }
        fun acc(px: Double, py: Double): Pair<Double, Double> {
            var ax = 0.0; var ay = 0.0
            for ((b, m) in wells) {
                val dx = b.x - px; val dy = b.y - py
                val r2 = dx * dx + dy * dy
                val r = kotlin.math.sqrt(r2)
                ax += G * m * dx / (r2 * r); ay += G * m * dy / (r2 * r)
            }
            return ax to ay
        }
        val out = arrayListOf(Vec(x, y))
        var t = 0.0
        var (ax, ay) = acc(x, y)
        while (t < 40 && out.size < 6000) {
            val dt = STEP / max(0.5, hypot(vx, vy))
            vx += 0.5 * dt * ax; vy += 0.5 * dt * ay
            x += dt * vx; y += dt * vy
            acc(x, y).let { ax = it.first; ay = it.second }
            vx += 0.5 * dt * ax; vy += 0.5 * dt * ay
            t += dt
            out += Vec(x, y)
            if (abs(x) > R + 0.5 || abs(y) > R + 0.5) break
        }
        return out
    }

    /** Der gerade Anfang einer Bahn bis zum ersten Knick (Laser: bis zum ersten Spiegel). */
    fun firstLeg(path: List<Vec>): List<Vec> {
        if (path.size < 3) return path
        var dir: Vec? = null
        for (i in 1 until path.size) {
            val dx = path[i].x - path[i - 1].x; val dy = path[i].y - path[i - 1].y
            val len = hypot(dx, dy)
            if (len < 1e-9) continue
            val d = Vec(dx / len, dy / len)
            val first = dir
            if (first == null) dir = d
            else if (abs(first.x * d.y - first.y * d.x) > 1e-3 || first.x * d.x + first.y * d.y < 0) return path.take(i)
        }
        return path
    }

    /** Bahnen der Form y = f(x) */
    val functionKinds = setOf(Kind.LINE, Kind.PARABOLA, Kind.SINE, Kind.EXP, Kind.PROP, Kind.ANTI, Kind.LOG, Kind.MOTION, Kind.ACCEL, Kind.POWER, Kind.EXPE, Kind.BINOM)

    /** Fliegt die Bahn ab: Ziele, Energiezellen, Asteroiden. */
    fun fly(level: Level, values: Map<String, Double>): FlightResult {
        if (level.kind == Kind.RENDEZVOUS) return rendezvous(level, values)
        val full = path(level, values)
        val hits = linkedSetOf<Int>(); val cells = linkedSetOf<Int>()
        var crash: Int? = null
        val flown = ArrayList<Vec>()
        // Funktionskurven: Die Station zählt nur, wenn y an ihrer Stelle x stimmt (bis auf ihren Radius).
        // Sonst streift eine steile Kurve den Kreis seitlich, obwohl falsch gerechnet wurde – Raten soll nicht reichen.
        val aligned = level.targets.indices.filter { i ->
            val t = level.targets[i]
            when (level.kind) {
                in functionKinds -> abs(f(level, values, t.x) - t.y) <= t.r
                // Tangente: Höhe der Geraden an der Station muss stimmen (nicht nur seitlich streifen)
                Kind.TANGENT -> { val v = level.values(values); val c = Upper.c(v); val x0 = v.getValue("x0")
                    abs(Upper.f(c, x0) + v.getValue("m") * (t.x - x0) - t.y) <= t.r }
                else -> true
            }
        }.toSet()
        for (p in full) {
            flown += p
            val a = level.asteroids.indexOfFirst { it.contains(p) }
            if (a >= 0) { crash = a; break }
            // Ankommen zählt (Koordinaten, Oberstufe); beim Vektor dürfen frühere Stationen unterwegs gestreift werden
            val endOnly = level.kind in Upper.endOnly
            if (!endOnly || level.kind == Kind.VECTOR) level.targets.forEachIndexed { i, t ->
                if (i in aligned && t.contains(p) && !(endOnly && i == level.targets.lastIndex)) hits += i }
            level.cells.forEachIndexed { i, c -> if (c.contains(p, 0.15)) cells += i }
            // Alle Stationen erreicht → andocken, nicht weiterfliegen
            if (!endOnly && level.kind != Kind.RADIUS && hits.size == level.targets.size) break
        }
        // Koordinaten: zählt nur, wo die Sonde ankommt
        if (level.kind in Upper.endOnly && crash == null) full.lastOrNull()?.let { end ->
            level.targets.forEachIndexed { i, t -> if (t.contains(end) && (level.kind != Kind.VECTOR || i == level.targets.lastIndex)) hits += i }
        }
        return FlightResult(flown, hits, cells, crash, crash == null && hits.size == level.targets.size)
    }

    /** Beide Sonden fliegen bis zur Andock-Stelle x. Treffen sie sich dort genau an der Station? */
    private fun rendezvous(level: Level, values: Map<String, Double>): FlightResult {
        val v = level.values(values)
        val a = path(level, values); val b = path2(level, values)
        val dock = Vec(v.getValue("x"), v.getValue("y"))
        val endA = Vec(dock.x, v.getValue("m1") * dock.x + v.getValue("b1"))
        val endB = Vec(dock.x, v.getValue("m2") * dock.x + v.getValue("b2"))
        val crash = level.asteroids.indexOfFirst { r -> (a + b).any { r.contains(it) } }.takeIf { it >= 0 }
        val hits = level.targets.indices.filter { i -> val t = level.targets[i]; t.contains(dock) && t.contains(endA) && t.contains(endB) }.toSet()
        val cells = level.cells.indices.filter { i -> (a + b).any { level.cells[i].contains(it, 0.15) } }.toSet()
        return FlightResult(a, hits, cells, crash, crash == null && hits.size == level.targets.size, b)
    }

    /** Sterne: 3 beim ersten Versuch, je Fehlversuch einer weniger (mindestens 1). Mit Zielhilfe höchstens 2. */
    fun stars(attempt: Int, helped: Boolean): Int = min(if (helped) 2 else 3, max(1, 4 - attempt))

    private fun line(a: Vec, b: Vec): List<Vec> {
        val n = max(1, (hypot(b.x - a.x, b.y - a.y) / STEP).toInt())
        return (0..n).map { i -> val t = i.toDouble() / n; Vec(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t) }
    }
}

/** Zahlen deutsch formatiert: 0,5 statt 0.5, ohne überflüssige Nullen, echtes Minuszeichen. */
/** Geradengleichung zum Anzeigen: „y = −6“ statt „y = 0x − 6“, „y = 2,25x“ statt „y = 2,25x + 0“, „y = −x + 3“ */
fun linText(m: Double, b: Double): String {
    if (m == 0.0) return "y = ${num(b)}"
    val mx = "${when (m) { 1.0 -> ""; -1.0 -> "−"; else -> num(m) }}x"
    return if (b == 0.0) "y = $mx" else "y = $mx ${if (b < 0) "−" else "+"} ${num(kotlin.math.abs(b))}"
}

fun num(v: Double, digits: Int = 2): String {
    val r = roundHalfUp(v * 10.0.pow(digits)) / 10.0.pow(digits)
    val s = if (r == kotlin.math.floor(r)) r.toLong().toString() else r.toString().trimEnd('0')
    return s.replace('.', ',').replace("-", "−")
}

private fun Double.pow(n: Int): Double { var r = 1.0; repeat(n) { r *= this }; return r }
