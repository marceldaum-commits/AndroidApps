package de.kurvenflug.core.model

import kotlin.math.hypot
import kotlin.random.Random

/**
 * 🔥 Extrem-Prüfung je Kapitel: freiwillige Zusatz-Aufgabe nach dem letzten Level (öffnet, wenn alle Level geschafft sind).
 * Aus der anspruchsvollsten Aufgabe des Kapitels: Zielzonen halbiert, Regler doppelt so fein, Asteroiden dicht an der Bahn.
 * Zählt nicht zum Kapitel-Abschluss – kein Fortschritt geht verloren. Nachweis „schwerer als jedes Level des Kapitels“:
 * ChapterExtremeTest.
 */
object ChapterExtreme {
    const val PREFIX = "x-"
    const val TITLE = "🔥 Extrem-Prüfung"

    fun isExtreme(id: String) = id.startsWith(PREFIX)
    fun id(chapter: Int) = "$PREFIX$chapter"

    private val cache = HashMap<Int, Level?>()

    fun forChapter(chapter: Int): Level? = cache.getOrPut(chapter) {
        Levels.chapters.firstOrNull { it.number == chapter }?.let { make(it) }
    }

    fun levelFor(id: String): Level? = if (!isExtreme(id)) null else id.removePrefix(PREFIX).toIntOrNull()?.let { forChapter(it) }

    /** Varianten von sehr schwer bis etwas leichter – die erste, die lösbar bleibt, wird genommen. */
    private val variants = listOf(Triple(0.5, 4, 0.25), Triple(0.5, 3, 0.3), Triple(0.6, 3, 0.3), Triple(0.75, 2, 0.35), Triple(0.75, 0, 0.0))

    /** Grundlage: das anspruchsvollste Level mit verfeinerbaren Reglern (ganzzahliges n bleibt ganzzahlig) (Unbekannte, Rechenschritte, Hindernisse, enge Ziele) – bei Gleichstand das letzte */
    fun base(c: Chapter): Level? = (c.levels.filter { l -> l.params.none { it.key == "n" } }.ifEmpty { c.levels }).reversed().maxByOrNull { l ->
        Difficulty.steps(l) + l.params.size + l.asteroids.size * 0.5 + 1.0 / l.targets.map { it.r }.average()
    }

    private fun make(c: Chapter): Level? {
        val base = base(c) ?: return null
        val minR = c.levels.minOf { l -> l.targets.minOf { it.r } }
        for ((shrink, n, gap) in variants) {
            val r = Random(c.number * 7_717L)
            var l = base.copy(
                id = id(c.number), number = c.levels.size + 1, title = TITLE,
                // enger als jede Zielzone im ganzen Kapitel
                targets = base.targets.map { it.copy(r = minOf(it.r * shrink, minR * 0.95)) },
                params = base.params.map { if (it.key == "n") it else it.copy(step = it.step / 2) },
                task = base.task + " 🔥 Extrem: kleinere Ziele, Regler doppelt so fein" + if (n > 0) ", Asteroiden dicht an der Bahn." else ".",
            )
            if (n > 0) {
                val flown = Flight.fly(l, l.solution).path
                l = l.copy(asteroids = l.asteroids + rocks(r, flown, l.targets + l.cells + l.asteroids, n, gap))
            }
            if (Flight.fly(l, l.solution).success && !Flight.fly(l, l.start).success) return l
        }
        return null
    }

    /** Asteroiden knapp neben (nicht auf) der Lösungsbahn: Abstand zur Bahn zwischen gap und gap + 1,2 Kästchen. */
    private fun rocks(r: Random, path: List<Vec>, avoid: List<Body>, n: Int, gap: Double): List<Body> {
        if (path.size < 5) return emptyList()
        val out = ArrayList<Body>()
        var tries = 0
        while (out.size < n && tries++ < 400) {
            val p = path[r.nextInt(path.size)]
            val b = Body(p.x + r.nextInt(-3, 4) * 0.5, p.y + r.nextInt(-3, 4) * 0.5, 0.7)
            if (kotlin.math.abs(b.x) > 9 || kotlin.math.abs(b.y) > 9) continue
            val d = path.minOf { hypot(it.x - b.x, it.y - b.y) } - b.r
            if (d < gap || d > gap + 1.2) continue
            if (avoid.any { hypot(it.x - b.x, it.y - b.y) < it.r + b.r + 0.8 } || out.any { hypot(it.x - b.x, it.y - b.y) < 2.2 }) continue
            out += b
        }
        return out
    }
}
