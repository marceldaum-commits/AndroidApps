package de.raumlicht.core.model

import kotlin.math.sin
import kotlin.random.Random

/** Sorten in der Ballerbude: normale Ballons, der seltene Goldballon und der Blindgänger, den man besser in Ruhe lässt. */
enum class BalloonKind(val color: String, val points: Int, val radius: Double) {
    PINK("pink", 1, 0.5), CYAN("cyan", 1, 0.5), VIOLET("violet", 1, 0.5), GREEN("green", 2, 0.36), GOLD("yellow", 5, 0.42), DUD("orange", -2, 0.62)
}
data class Balloon(val id: Int, val kind: BalloonKind, val born: Double, val x: Double, val speed: Double, val sway: Double)

/** Punktestand: Treffer in Folge bauen eine Serie auf, alle fünf Treffer zählt jeder Ballon einen Punkt mehr. */
data class BalloonScore(val points: Int = 0, val combo: Int = 0, val bestCombo: Int = 0, val hits: Int = 0) {
    val bonus get() = combo / 5
    fun hit(kind: BalloonKind): BalloonScore =
        if (kind == BalloonKind.DUD) copy(points = maxOf(0, points + kind.points), combo = 0)
        else (combo + 1).let { c -> copy(points = points + kind.points + bonus, combo = c, bestCombo = maxOf(bestCombo, c), hits = hits + 1) }
}

/**
 * Ballerbude – nur zum Spaß, ohne Rechnen: Leuchtballons steigen von der Showbühne auf, antippen heißt abschießen.
 * Der ganze Ablauf einer Runde steht mit dem Startwert [seed] fest (prüfbar); zum Ende hin kommen die Ballons dichter und schneller.
 */
class BalloonGame(seed: Long, val duration: Double = 45.0) {
    val balloons: List<Balloon> = run {
        val r = Random(seed); val out = ArrayList<Balloon>(); var t = 0.3
        while (t < duration - 1.5) {
            val heat = t / duration
            val kind = r.nextInt(100).let { when { it < 7 -> BalloonKind.GOLD; it < 19 -> BalloonKind.DUD; it < 34 -> BalloonKind.GREEN; it < 56 -> BalloonKind.PINK; it < 78 -> BalloonKind.CYAN; else -> BalloonKind.VIOLET } }
            out += Balloon(out.size, kind, t, 1.3 + r.nextDouble() * 5.4, (1.1 + r.nextDouble() * 0.7) * (1 + heat * 0.9) * (if (kind == BalloonKind.GOLD) 1.5 else 1.0), r.nextDouble() * 6.28)
            t += (0.85 - 0.5 * heat) * (0.7 + r.nextDouble() * 0.6)
        }
        out
    }
    fun pos(b: Balloon, t: Double) = V3(4.0, b.x + sin((t - b.born) * 1.7 + b.sway) * 0.45, 0.5 + (t - b.born) * b.speed)
    /** Ballons, die gerade in der Luft sind (noch nicht getroffen, noch nicht oben hinaus) */
    fun alive(t: Double, popped: Set<Int>) = balloons.filter { it.born <= t && it.id !in popped && pos(it, t).z < TOP }
    fun byId(id: String) = id.removePrefix("b").toIntOrNull()?.let { balloons.getOrNull(it) }
    fun scene(t: Double, popped: Set<Int>, mood: String = "idle"): String = Stage.encode(Bounds(),
        alive(t, popped).map { Stage.sphere("b${it.id}", pos(it, t), it.kind.radius, it.kind.color) } + Stage.drone(LUX, mood) + Stage.podium())

    companion object { const val TOP = 7.2; val LUX = V3(8, 0, 1) }
}
