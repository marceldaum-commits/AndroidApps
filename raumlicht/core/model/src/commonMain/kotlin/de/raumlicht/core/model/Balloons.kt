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
    /** Fehlschuss ins Leere: kostet keine Punkte, aber die Serie */
    fun miss() = copy(combo = 0)
    /** Mit Aufgabe: ein Ballon, der nicht zur Regel passt */
    fun wrong() = copy(points = maxOf(0, points - 1), combo = 0)
    fun hit(kind: BalloonKind): BalloonScore =
        if (kind == BalloonKind.DUD) copy(points = maxOf(0, points + kind.points), combo = 0)
        else (combo + 1).let { c -> copy(points = points + kind.points + bonus, combo = c, bestCombo = maxOf(bestCombo, c), hits = hits + 1) }
}

/** Aufgabe in der Ballerbude: Nur Ballons, deren Zahl zur Regel passt, sollen abgeschossen werden. */
class BalloonRule(val text: String, val pool: IntRange, val ok: (Int) -> Boolean)
enum class BalloonMode { SOLO, QUIZ, DUEL }

/**
 * Ballerbude: Leuchtballons steigen von der Showbühne auf, antippen heißt abschießen.
 * [BalloonMode.SOLO] ist reiner Spaß; bei [BalloonMode.QUIZ] tragen die Ballons Zahlen und es zählt nur, was zur Regel passt
 * (alle 15 Sekunden eine neue); bei [BalloonMode.DUEL] gehört jedem Spieler eine Hälfte.
 * Der ganze Ablauf einer Runde steht mit dem Startwert [seed] fest (prüfbar); zum Ende hin kommen die Ballons dichter und schneller.
 */
class BalloonGame(seed: Long, val duration: Double = 45.0, val mode: BalloonMode = BalloonMode.SOLO) {
    /** Reihenfolge der Regeln dieser Runde (nur mit Aufgabe) */
    val rules: List<BalloonRule> = RULES.shuffled(Random(seed)).take((duration / RULE_TIME).toInt().coerceAtLeast(1))
    fun ruleAt(t: Double) = rules[(t / RULE_TIME).toInt().coerceIn(0, rules.lastIndex)]
    val balloons: List<Balloon> = run {
        val r = Random(seed); val out = ArrayList<Balloon>(); var t = 0.3
        while (t < duration - 1.5) {
            val heat = t / duration
            val kind = if (mode == BalloonMode.QUIZ) listOf(BalloonKind.PINK, BalloonKind.CYAN, BalloonKind.VIOLET)[r.nextInt(3)]
                else r.nextInt(100).let { when { it < 7 -> BalloonKind.GOLD; it < 19 -> BalloonKind.DUD; it < 34 -> BalloonKind.GREEN; it < 56 -> BalloonKind.PINK; it < 78 -> BalloonKind.CYAN; else -> BalloonKind.VIOLET } }
            // mit Zahlen etwas gemächlicher – man muss ja lesen
            val calm = if (mode == BalloonMode.QUIZ) 0.62 else 1.0
            out += Balloon(out.size, kind, t, 1.3 + r.nextDouble() * 5.4, (1.1 + r.nextDouble() * 0.7) * (1 + heat * 0.9) * (if (kind == BalloonKind.GOLD) 1.5 else 1.0) * calm, r.nextDouble() * 6.28)
            t += (0.85 - 0.5 * heat) * (0.7 + r.nextDouble() * 0.6) / calm.let { if (it < 1) 0.85 else 1.0 }
        }
        out
    }
    /** Zahl auf jedem Ballon (nur mit Aufgabe): knapp die Hälfte passt zur Regel, die bei seinem Start gilt */
    val numbers: List<Int> = balloons.map { b -> val r = Random(seed * 31 + b.id); val rule = ruleAt(b.born + 2.0)
        val fits = rule.pool.filter(rule.ok); val rest = rule.pool.filter { !rule.ok(it) }
        if (r.nextInt(100) < 45 || rest.isEmpty()) fits[r.nextInt(fits.size)] else rest[r.nextInt(rest.size)] }
    fun pos(b: Balloon, t: Double) = V3(4.0, b.x + sin((t - b.born) * 1.7 + b.sway) * 0.45, 0.5 + (t - b.born) * b.speed)
    /** Ballons, die gerade in der Luft sind (noch nicht getroffen, noch nicht oben hinaus) */
    fun alive(t: Double, popped: Set<Int>) = balloons.filter { it.born <= t && it.id !in popped && pos(it, t).z < TOP }
    fun byId(id: String) = id.removePrefix("b").toIntOrNull()?.let { balloons.getOrNull(it) }
    /** Im Duell: 0 = linke Hälfte, 1 = rechte Hälfte */
    fun side(p: V3) = if (p.y < 4.0) 0 else 1
    fun scene(t: Double, popped: Set<Int>, mood: String = "idle"): String = Stage.encode(Bounds(),
        alive(t, popped).map { Stage.sphere("b${it.id}", pos(it, t), it.kind.radius, it.kind.color, if (mode == BalloonMode.QUIZ) Geo.num(numbers[it.id]) else null) } +
            (if (mode == BalloonMode.DUEL) listOf(Stage.line("mitte", V3(4.0, 4.0, 0.05), V3(0.0, 0.0, 1.0), 0.0, 6.6, "white", dashed = true)) else emptyList()) + Stage.drone(LUX, mood) + Stage.podium(), lock = true)

    companion object {
        const val TOP = 7.2; const val RULE_TIME = 15.0; val LUX = V3(8, 0, 1)
        private fun prime(n: Int) = n > 1 && (2..n / 2).none { n % it == 0 }
        val RULES = listOf(
            BalloonRule("gerade Zahlen", 1..30) { it % 2 == 0 }, BalloonRule("Vielfache von 3", 1..40) { it % 3 == 0 }, BalloonRule("Primzahlen", 1..30, ::prime),
            BalloonRule("Quadratzahlen", 1..50) { n -> (1..7).any { it * it == n } }, BalloonRule("alles größer als √50", 1..15) { it * it > 50 }, BalloonRule("Teiler von 24", 1..24) { 24 % it == 0 },
            BalloonRule("negative Zahlen", -9..9) { it < 0 }, BalloonRule("Lösungen von x² = 16", -6..6) { it * it == 16 }, BalloonRule("Nullstellen von x² − 5x + 6", 0..6) { it * it - 5 * it + 6 == 0 },
        )
    }
}
