package de.quanta.core.model

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
 */
enum class Kind { POINT, LINE, PARABOLA, THROW }

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
)

object Flight {
    /** Spielfeld: −10 … 10 in beide Richtungen. */
    const val R = 10.0
    private const val STEP = 0.02

    /** Wert der Funktion an der Stelle x (nur für LINE/PARABOLA). */
    fun f(level: Level, v: Map<String, Double>, x: Double): Double {
        val p = level.values(v)
        return when (level.kind) {
            Kind.LINE -> p.getValue("m") * x + p.getValue("b")
            Kind.PARABOLA -> p.getValue("a") * (x - p.getValue("d")).let { it * it } + p.getValue("e")
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
            Kind.LINE, Kind.PARABOLA -> {
                // Sichtbarer Teil des Graphen von links nach rechts – die Sonde „springt“ am Rand ins Bild
                val out = ArrayList<Vec>()
                var x = -R
                while (x <= R + 1e-9) {
                    val y = f(level, values, x)
                    if (abs(y) <= R) out += Vec(x, y) else if (out.isNotEmpty()) break
                    x += STEP
                }
                out
            }
            Kind.THROW -> {
                val a = Math.toRadians(v.getValue("angle")); val s = v.getValue("v"); val g = level.planet.g
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
        }
    }

    /** Fliegt die Bahn ab: Ziele, Energiezellen, Asteroiden. */
    fun fly(level: Level, values: Map<String, Double>): FlightResult {
        val full = path(level, values)
        val hits = linkedSetOf<Int>(); val cells = linkedSetOf<Int>()
        var crash: Int? = null
        val flown = ArrayList<Vec>()
        for (p in full) {
            flown += p
            val a = level.asteroids.indexOfFirst { it.contains(p) }
            if (a >= 0) { crash = a; break }
            if (level.kind != Kind.POINT) level.targets.forEachIndexed { i, t -> if (t.contains(p)) hits += i }
            level.cells.forEachIndexed { i, c -> if (c.contains(p, 0.15)) cells += i }
            // Alle Stationen erreicht → andocken, nicht weiterfliegen
            if (level.kind != Kind.POINT && hits.size == level.targets.size) break
        }
        // Koordinaten: zählt nur, wo die Sonde ankommt
        if (level.kind == Kind.POINT && crash == null) full.lastOrNull()?.let { end ->
            level.targets.forEachIndexed { i, t -> if (t.contains(end)) hits += i }
        }
        return FlightResult(flown, hits, cells, crash, crash == null && hits.size == level.targets.size)
    }

    /** Sterne: 3 beim ersten Versuch, je Fehlversuch einer weniger (mindestens 1). Mit Zielhilfe höchstens 2. */
    fun stars(attempt: Int, helped: Boolean): Int = min(if (helped) 2 else 3, max(1, 4 - attempt))

    private fun line(a: Vec, b: Vec): List<Vec> {
        val n = max(1, (hypot(b.x - a.x, b.y - a.y) / STEP).toInt())
        return (0..n).map { i -> val t = i.toDouble() / n; Vec(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t) }
    }
}

/** Zahlen deutsch formatiert: 0,5 statt 0.5, ohne überflüssige Nullen, echtes Minuszeichen. */
fun num(v: Double, digits: Int = 2): String {
    val r = Math.round(v * 10.0.pow(digits)) / 10.0.pow(digits)
    val s = if (r == Math.floor(r)) r.toLong().toString() else r.toString().trimEnd('0')
    return s.replace('.', ',').replace("-", "−")
}

private fun Double.pow(n: Int): Double { var r = 1.0; repeat(n) { r *= this }; return r }
