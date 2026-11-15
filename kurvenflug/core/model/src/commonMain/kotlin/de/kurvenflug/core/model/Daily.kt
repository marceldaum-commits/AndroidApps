package de.kurvenflug.core.model

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

/** Tagesmission: jeden Tag ein neues Level, für alle gleich (Startwert = Tag). */
object Daily {
    const val PREFIX = "daily-"
    private val rotation = listOf(Kind.LINE, Kind.PARABOLA, Kind.EXP, Kind.THROW)

    fun id(day: Long) = "$PREFIX$day"
    fun isDaily(id: String) = id.startsWith(PREFIX)

    /** Kurzname für Karte und Widget */
    fun label(l: Level): String = when (l.kind) {
        Kind.LINE -> "Gerade 📈"; Kind.PARABOLA -> "Parabel 🌙"; Kind.EXP -> "Wachstum 🦠"; Kind.THROW -> "Wurf ${l.planet.emoji}"
        else -> "Überraschung"
    }

    fun forDay(day: Long): Level =
        Gen.level(rotation[floorModK(day, 4L).toInt()], day * 7919 + 17, 1).let { it.copy(id = id(day), title = if (it.title == Gen.FALLBACK) it.title else "Tagesmission") }
}

/**
 * Level-Generator für Tagesmission, Duell, Asteroidensturm und Boss-Kämpfe.
 * Erst wird eine Lösung auf dem Regler-Raster gewürfelt, dann die Stationen *auf* diese Bahn gelegt –
 * so ist jedes Level garantiert lösbar. Danach wird geprüft, dass die Startwerte nicht schon treffen.
 */
object Gen {
    /** Bahnarten, die der Generator kann (Schleuder braucht Handarbeit). */
    const val FALLBACK = "Ersatz-Mission"
    val kinds = listOf(Kind.POINT, Kind.LINE, Kind.PARABOLA, Kind.SINE, Kind.EXP, Kind.THROW, Kind.RENDEZVOUS)

    /** @param difficulty 0 = leicht, 1 = normal (ein, zwei Asteroiden), 2 = schwer (mehr Asteroiden, krumme Zahlen) */
    fun level(kind: Kind, seed: Long, difficulty: Int = 1, id: String = "gen-$seed"): Level {
        val r = Random(seed)
        val day = seed
        repeat(300) {
            val l = when (kind) {
                Kind.POINT -> point(day, r, difficulty)
                Kind.LINE -> line(day, r)
                Kind.PARABOLA -> parabola(day, r)
                Kind.SINE -> sine(day, r)
                Kind.EXP -> exp(day, r)
                Kind.THROW -> throwLevel(day, r)
                Kind.RENDEZVOUS -> rendezvous(day, r, difficulty)
                else -> null
            }?.let { harder(it, r, difficulty) }
            if (l != null && valid(l)) return l.copy(id = id)
        }
        // Notfall (sollte nie passieren – im Test geprüft)
        return Levels.byId("2-6")!!.copy(id = id, chapter = 0, number = 0, title = FALLBACK)
    }

    /** Zufällige Bahnart + Level – für Duell und Asteroidensturm. */
    fun any(seed: Long, difficulty: Int = 1, id: String = "gen-$seed"): Level =
        level(kinds[floorModK(seed, kinds.size.toLong()).toInt()], seed, difficulty, id)

    /** Schwer: zusätzliche Asteroiden nahe (aber nicht auf) der Lösungsbahn. */
    private fun harder(l: Level, r: Random, difficulty: Int): Level {
        if (difficulty < 2 || l.kind == Kind.POINT || l.kind == Kind.RENDEZVOUS) return l
        val path = Flight.path(l, l.solution)
        val extra = rocks(r, path, l.targets + l.asteroids, 2, gap = 0.35)
        return l.copy(asteroids = l.asteroids + extra)
    }

    private fun valid(l: Level): Boolean {
        val win = Flight.fly(l, l.solution)
        return win.success && win.cells.size == l.cells.size && !Flight.fly(l, l.start).success
    }

    private fun Random.pick(min: Double, max: Double, step: Double): Double {
        val n = ((max - min) / step).toInt()
        return min + nextInt(n + 1) * step
    }

    private fun mk(@Suppress("UNUSED_PARAMETER") day: Long, kind: Kind, task: String, targets: List<Body>, params: List<Param>, sol: Map<String, Double>,
                   fixed: Map<String, Double> = emptyMap(), cells: List<Body> = emptyList(), rocks: List<Body> = emptyList(),
                   planet: Planet = Planet.EARTH, tip: String) =
        Level("gen", 0, 0, "Mission", kind, task, targets, params, sol, fixed, rocks, cells, planet = planet, tip = tip)

    /** Asteroiden, die nicht auf der Lösungsbahn liegen. */
    private fun rocks(r: Random, path: List<Vec>, avoid: List<Body>, n: Int, gap: Double = 1.0): List<Body> {
        val out = ArrayList<Body>()
        var tries = 0
        while (out.size < n && tries++ < 200) {
            val b = Body(r.pick(-8.0, 8.0, 1.0), r.pick(-8.0, 8.0, 1.0), 0.9)
            val clear = path.all { hypot(it.x - b.x, it.y - b.y) > b.r + gap } &&
                avoid.all { hypot(it.x - b.x, it.y - b.y) > 2.5 } && out.all { hypot(it.x - b.x, it.y - b.y) > 3 }
            if (clear) out += b
        }
        return out
    }

    private fun pointsOn(l: Level, xs: List<Double>) = xs.map { x -> Body(x, Flight.f(l, l.solution, x), 0.7) }

    private fun line(day: Long, r: Random): Level? {
        val m = r.pick(-2.0, 2.0, 0.5).takeIf { it != 0.0 } ?: return null
        val b = r.pick(-5.0, 5.0, 1.0)
        val pm = Param("m", "m", -5.0, 5.0, 0.5, 0.0); val pb = Param("b", "b", -9.0, 9.0, 1.0, 0.0)
        var l = mk(day, Kind.LINE, "", emptyList(), listOf(pm, pb), mapOf("m" to m, "b" to b), tip = "Steigung = Höhenunterschied : Breitenunterschied. b ist der Wert bei x = 0.")
        val xs = listOf(r.pick(-7.0, -2.0, 1.0), r.pick(2.0, 7.0, 1.0))
        val t = pointsOn(l, xs).takeIf { ts -> ts.all { abs(it.y) <= 8.5 } } ?: return null
        val cell = pointsOn(l, listOf((xs[0] + xs[1]) / 2)).map { it.copy(r = 0.45) }
        l = l.copy(targets = t, cells = cell, task = "Durch beide Stationen: (${num(t[0].x)} | ${num(t[0].y)}) und (${num(t[1].x)} | ${num(t[1].y)}).")
        return l.copy(asteroids = rocks(r, Flight.path(l, l.solution), t, 2))
    }

    private fun parabola(day: Long, r: Random): Level? {
        val a = listOf(-1.0, -0.5, -0.25, 0.25, 0.5, 1.0)[r.nextInt(6)]
        val d = r.pick(-4.0, 4.0, 1.0); val e = r.pick(-6.0, 6.0, 1.0)
        val params = listOf(Param("a", "a", -3.0, 3.0, 0.25, 1.0), Param("d", "d", -8.0, 8.0, 1.0, 0.0), Param("e", "e", -9.0, 9.0, 1.0, 0.0))
        var l = mk(day, Kind.PARABOLA, "", emptyList(), params, mapOf("a" to a, "d" to d, "e" to e), tip = "Der Scheitel ist (d | e). Setz dann den zweiten Punkt ein, um a zu finden.")
        val x2 = d + listOf(-4.0, -3.0, -2.0, 2.0, 3.0, 4.0)[r.nextInt(6)]
        if (abs(x2) > 8) return null
        // Reihenfolge von links nach rechts – die Sonde fliegt von links
        val t = pointsOn(l, listOf(d, x2).sorted()).takeIf { ts -> ts.all { abs(it.y) <= 8.5 } } ?: return null
        l = l.copy(targets = t, task = "${if (a < 0) "Gipfelstation" else "Talstation"} bei (${num(d)} | ${num(e)}), zweite Station bei (${num(x2)} | ${num(Flight.f(l, l.solution, x2))}).")
        return l.copy(asteroids = rocks(r, Flight.path(l, l.solution), t, 2))
    }

    private fun exp(day: Long, r: Random): Level? {
        val b = listOf(0.5, 2.0, 3.0)[r.nextInt(3)]
        val a = listOf(0.25, 0.5, 1.0, 2.0)[r.nextInt(4)]
        val params = listOf(Param("a", "a", 0.25, 4.0, 0.25, 1.0), Param("b", "b", 0.25, 4.0, 0.25, 1.0))
        var l = mk(day, Kind.EXP, "", emptyList(), params, mapOf("a" to a, "b" to b), fixed = mapOf("d" to 0.0),
            tip = "Bei x = 0 ist bˣ = 1 – dort liest du a ab. Pro Kästchen wird mit b malgenommen.")
        val xs = listOf(0.0, if (b > 1) r.pick(1.0, 3.0, 1.0) else r.pick(-3.0, -1.0, 1.0)).sorted()
        val t = pointsOn(l, xs).takeIf { ts -> ts.all { it.y in 0.2..8.5 } } ?: return null
        l = l.copy(targets = t, task = "Wachstum oder Zerfall? Durch (${num(t[0].x)} | ${num(t[0].y)}) und (${num(t[1].x)} | ${num(t[1].y)}).")
        return l.copy(asteroids = rocks(r, Flight.path(l, l.solution), t, 1))
    }

    private fun throwLevel(day: Long, r: Random): Level? {
        val planet = Planet.entries[r.nextInt(3)]
        val angle = r.pick(30.0, 65.0, 5.0)
        val vMax = if (planet == Planet.EARTH) 14.0 else 7.0
        val v = r.pick(4.0, vMax, 0.5)
        val params = listOf(Param("angle", "Winkel", 5.0, 85.0, 1.0, 45.0, "°"), Param("v", "Tempo", 1.0, 20.0, 0.5, if (planet == Planet.EARTH) 8.0 else 3.0, "m/s"))
        var l = mk(day, Kind.THROW, "", emptyList(), params, mapOf("angle" to angle, "v" to v), planet = planet,
            tip = "Wurfweite auf ebenem Boden: w = v² · sin(2α) : g.")
        val path = Flight.path(l, l.solution)
        val land = path.last { it.y >= l.launch.y }
        val dist = land.x - l.launch.x
        if (land.x > 9.0 || dist < 6) return null
        val t = listOf(Body(land.x, l.launch.y, 0.8))
        l = l.copy(targets = t, task = "${planet.emoji} ${planet.label}: Ziel ${num(dist, 1)} m entfernt. Winkel und Tempo – du entscheidest!")
        return l
    }

    private fun point(day: Long, r: Random, difficulty: Int): Level? {
        val x = r.pick(-8.0, 8.0, 1.0); val y = r.pick(-8.0, 8.0, 1.0)
        if (x == 0.0 || y == 0.0) return null
        val px = Param("x", "x", -9.0, 9.0, 1.0, 0.0); val py = Param("y", "y", -9.0, 9.0, 1.0, 0.0)
        // Ab „normal“: getarnt, nur ein Rechen-Funkspruch verrät die Koordinaten
        val hidden = difficulty >= 1
        val task = if (!hidden) "Flieg zur Station bei (${num(x)} | ${num(y)})." else {
            val k = r.nextInt(2, 5)
            val a = r.nextInt(1, 9)
            "Getarnt! Funkspruch: x = ${num(x * k)} : $k,  y = ${num(y - a)} + $a"
        }
        return mk(day, Kind.POINT, task, listOf(Body(x, y, 0.7)), listOf(px, py), mapOf("x" to x, "y" to y),
            tip = "Erst x (rechts/links), dann y (oben/unten). Minus heißt links bzw. unten.").copy(hidden = hidden)
    }

    private fun sine(day: Long, r: Random): Level? {
        val a = listOf(-3.0, -2.0, 2.0, 3.0, 4.0)[r.nextInt(5)]
        val b = listOf(0.5, 1.0, 2.0)[r.nextInt(3)]
        val d = r.pick(-3.0, 3.0, 1.0)
        val params = listOf(Param("a", "a", -5.0, 5.0, 0.5, 1.0), Param("b", "b", 0.25, 3.0, 0.25, 1.0), Param("d", "d", -8.0, 8.0, 1.0, 0.0))
        var l = mk(day, Kind.SINE, "", emptyList(), params, mapOf("a" to a, "b" to b, "d" to d),
            tip = "Bei x = 0 ist sin = 0 – dort liest du d ab. Der erste Berg liegt bei x = π/2 : b.")
        val peak = kotlin.math.PI / 2 / b
        val t = pointsOn(l, listOf(0.0, peak))
        l = l.copy(targets = t, task = "Durch (0 | ${num(d)}) und den ersten Wellen-${if (a > 0) "berg" else "tal"} bei x ≈ ${num(peak)} (y = ${num(a + d)}).")
        return l.copy(asteroids = rocks(r, Flight.path(l, l.solution), t, 2))
    }

    private fun rendezvous(day: Long, r: Random, difficulty: Int): Level? {
        val step = if (difficulty >= 2) 0.5 else 1.0
        val x = r.pick(-6.0, 6.0, step); val y = r.pick(-6.0, 6.0, step)
        val m1 = r.pick(-3.0, 3.0, 0.5); val m2 = r.pick(-3.0, 3.0, 0.5)
        if (m1 == m2 || (x == 0.0 && y == 0.0)) return null
        val b1 = y - m1 * x; val b2 = y - m2 * x
        if (b1 != roundHalfUp(b1 * 2) / 2.0 || b2 != roundHalfUp(b2 * 2) / 2.0 || kotlin.math.abs(b1) > 9 || kotlin.math.abs(b2) > 9) return null
        val params = listOf(Param("x", "x", -9.0, 9.0, 0.5, 0.0), Param("y", "y", -9.0, 9.0, 0.5, 0.0))
        fun lin(m: Double, b: Double) = "y = ${when (m) { 1.0 -> ""; -1.0 -> "−"; else -> num(m) }}x ${if (b < 0) "−" else "+"} ${num(kotlin.math.abs(b))}"
        return mk(day, Kind.RENDEZVOUS, "A: ${lin(m1, b1)} und B: ${lin(m2, b2)}. Wo treffen sie sich?",
            listOf(Body(x, y, 0.35)), params, mapOf("x" to x, "y" to y), fixed = mapOf("m1" to m1, "b1" to b1, "m2" to m2, "b2" to b2),
            tip = "Beide Gleichungen gleichsetzen, nach x auflösen, dann y ausrechnen.").copy(hidden = true)
    }
}
