package de.kurvenflug.core.model

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

/**
 * Tagesmission: jeden Tag ein neues Level, für alle gleich (Startwert = Tag).
 * Erst wird eine Lösung auf dem Regler-Raster gewürfelt, dann die Stationen *auf* diese Bahn gelegt –
 * so ist jede Mission garantiert lösbar. Danach wird geprüft, dass die Startwerte nicht schon treffen.
 */
object Daily {
    const val PREFIX = "daily-"

    fun id(day: Long) = "$PREFIX$day"
    fun isDaily(id: String) = id.startsWith(PREFIX)

    fun forDay(day: Long): Level {
        val r = Random(day * 7919 + 17)
        repeat(200) {
            val l = when (Math.floorMod(day, 4L).toInt()) {
                0 -> line(day, r)
                1 -> parabola(day, r)
                2 -> exp(day, r)
                else -> throwLevel(day, r)
            }
            if (l != null && valid(l)) return l
        }
        // Notfall (sollte nie passieren – im Test für 3 Jahre geprüft)
        return Levels.byId("2-6")!!.copy(id = id(day), chapter = 0, number = 0, title = "Tagesmission")
    }

    private fun valid(l: Level): Boolean {
        val win = Flight.fly(l, l.solution)
        return win.success && win.cells.size == l.cells.size && !Flight.fly(l, l.start).success
    }

    private fun Random.pick(min: Double, max: Double, step: Double): Double {
        val n = ((max - min) / step).toInt()
        return min + nextInt(n + 1) * step
    }

    private fun mk(day: Long, kind: Kind, task: String, targets: List<Body>, params: List<Param>, sol: Map<String, Double>,
                   fixed: Map<String, Double> = emptyMap(), cells: List<Body> = emptyList(), rocks: List<Body> = emptyList(),
                   planet: Planet = Planet.EARTH, tip: String) =
        Level(id(day), 0, 0, "Tagesmission", kind, task, targets, params, sol, fixed, rocks, cells, planet = planet, tip = tip)

    /** Asteroiden, die nicht auf der Lösungsbahn liegen. */
    private fun rocks(r: Random, path: List<Vec>, avoid: List<Body>, n: Int): List<Body> {
        val out = ArrayList<Body>()
        var tries = 0
        while (out.size < n && tries++ < 200) {
            val b = Body(r.pick(-8.0, 8.0, 1.0), r.pick(-8.0, 8.0, 1.0), 0.9)
            val clear = path.all { hypot(it.x - b.x, it.y - b.y) > b.r + 1.0 } &&
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
}
