package de.raumlicht.core.model

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

// ───────────────────────── Galton-Brett ─────────────────────────

/**
 * Galton-Brett: Kugeln fallen durch [rows] Nagelreihen und gehen an jedem Nagel mit Wahrscheinlichkeit p nach rechts.
 * In den Fächern entsteht die Binomialverteilung – zum Zusehen.
 */
class Galton(val rows: Int = 8) {
    private fun binom(n: Int, k: Int): Double { var r = 1.0; for (i in 1..k) r = r * (n - k + i) / i; return r }
    /** Wahrscheinlichkeit für Fach k (k-mal rechts) */
    fun pmf(p: Double): List<Double> = (0..rows).map { k -> binom(rows, k) * p.pow(k) * (1 - p).pow(rows - k) }
    fun mean(p: Double) = rows * p
    fun sigma(p: Double) = sqrt(rows * p * (1 - p))

    // Abstände passen sich der Reihenzahl an, damit auch 12 Reihen auf die Bühne passen
    private val step = minOf(STEP, 6.4 / rows); private val fall = minOf(DROP, 4.4 / rows)
    private fun peg(row: Int, rights: Int) = V3(4.0, 4.0 + (rights - row / 2.0) * step, TOP - row * fall)
    /** Weg einer Kugel: von oben bis ins Fach. Rückgabe: Fach und Wegpunkte. */
    fun drop(p: Double, random: Random): Pair<Int, List<V3>> {
        var rights = 0; val path = ArrayList<V3>(); path += peg(0, 0) + V3(0.0, 0.0, 0.4)
        for (row in 0 until rows) { path += peg(row, rights) + V3(0.0, 0.0, 0.12); if (random.nextDouble() < p) rights++ }
        path += peg(rows, rights); path += V3(4.0, peg(rows, rights).y, 0.3)
        return rights to path
    }

    /** Bühne: Nägel, Balken je Fach (Höhe nach Anzahl), die erwartete Verteilung als gelbe Linie und optional die fallende Kugel */
    fun scene(counts: List<Int>, p: Double, ball: V3? = null): String {
        val total = counts.sum(); val expected = pmf(p); val peak = maxOf(counts.maxOrNull() ?: 0, 1)
        val scale = minOf(0.22, 1.9 / peak)
        val pegs = (0 until rows).flatMap { r -> (0..r).map { i -> peg(r, i) + V3(0.0, 0.0, -0.06) to peg(r, i) + V3(0.0, 0.0, 0.06) } }
        val bars = (0..rows).filter { counts[it] > 0 }.map { k -> val x = peg(rows, k).y; val h = 0.1 + counts[k] * scale
            val w = step * 0.38; listOf(V3(4.0, x - w, 0.08), V3(4.0, x + w, 0.08), V3(4.0, x + w, h), V3(4.0, x - w, h)) }
        val curve = (0..rows).map { k -> V3(4.0, peg(rows, k).y, 0.1 + expected[k] * maxOf(total, 1) * scale) }
        return Stage.encode(Bounds(), listOfNotNull(
            Stage.edges("nägel$rows", pegs, "white"),
            if (bars.isNotEmpty()) Stage.edges("balken${bars.size}", bars.flatMap { q -> q.indices.map { q[it] to q[(it + 1) % 4] } }, "cyan", faces = bars) else null,
            if (total > 0) Stage.edges("erwartung", curve.zipWithNext(), "yellow") else null,
            ball?.let { Stage.point("kugel", it, "pink", drop = false) },
            Stage.drone(V3(8, 0, 1), if (total >= 200) "happy" else "idle"),
            Stage.podium(),
        ))
    }
    companion object { const val STEP = 0.8; const val DROP = 0.48; const val TOP = 5.9; val ROWS = 4..12 }
}

// ───────────────────────── Flug durch den Raum ─────────────────────────

data class FlightEvent(val t: Double, val title: String, val text: String, val color: String)
data class FlightData(val pos: V3, val t: Double, val nearest: String, val distance: Double)

/** Fahrersicht in der Geometrie: Man fliegt eine Gerade entlang – durch Ebenen hindurch, an Kugeln vorbei. */
class Flight(val id: String, val title: String, val a: V3, val d: V3, val tMax: Double,
             val planes: List<Triple<String, V3, Int>>, val balls: List<Triple<String, V3, Int>> = emptyList()) {
    fun at(u: Double) = a + d * (tMax * u.coerceIn(0.0, 1.0))
    fun data(u: Double): FlightData {
        val pos = at(u); val near = planes.minByOrNull { Geo.distPointPlane(pos, it.second, it.third.toDouble()) }
        return FlightData(pos, tMax * u.coerceIn(0.0, 1.0), near?.first ?: "–", near?.let { Geo.distPointPlane(pos, it.second, it.third.toDouble()) } ?: 0.0)
    }
    /** Was unterwegs passiert: Durchstoßpunkte mit den Ebenen, Ein- und Austritt bei Kugeln */
    val events: List<FlightEvent> by lazy {
        (planes.mapNotNull { (name, n, c) -> Geo.linePlane(a, d, n, c.toDouble())?.takeIf { it > 1e-9 && it < tMax - 1e-9 }?.let {
            FlightEvent(it, "Durchstoßpunkt mit $name", "t = ${Geo.num(it)} → S${(a + d * it).fmt()}", "violet") } } +
            balls.flatMap { (name, m, r) ->
                // |a + t·d − m|² = r²  →  quadratische Gleichung in t
                val w = a - m; val qa = d dot d; val qb = 2 * (w dot d); val qc = (w dot w) - r * r; val disc = qb * qb - 4 * qa * qc
                if (disc < 0) emptyList() else listOf((-qb - sqrt(disc)) / (2 * qa) to "Eintritt in", (-qb + sqrt(disc)) / (2 * qa) to "Austritt aus").filter { it.first > 1e-9 && it.first < tMax - 1e-9 }
                    .map { (t, word) -> FlightEvent(t, "$word $name", "t = ${Geo.num(t)} → ${(a + d * t).fmt()}", "cyan") }
            }).sortedBy { it.t }
    }
    fun eventNear(t: Double, eps: Double = 0.06) = events.firstOrNull { abs(it.t - t) < eps }
    val text get() = "x⃗ = ${a.fmt()} + t · ${d.fmt()}"

    fun scene(): String = Stage.encode(Bounds(), listOf(
        Obj("bahn", "track", corners = (0..40).map { at(it / 40.0).list }, color = "cyan", style = "line"),
        Stage.point("start", a, "cyan", "A${a.fmt()}"),
    ) + planes.mapIndexed { i, (name, n, c) -> Stage.plane("e$i", n, c.toDouble(), listOf("violet", "pink", "green")[i % 3], name) } +
        balls.mapIndexed { i, (_, m, r) -> Stage.sphere("k$i", m, r.toDouble(), "yellow") } +
        events.mapIndexed { i, e -> Stage.point("s$i", a + d * e.t, e.color, style = "target", drop = false) })
}

object Flights {
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)
    val all = listOf(
        Flight("glas", "Durch zwei Scheiben", p(0, 0, 1), p(2, 2, 1), 4.0, listOf(Triple("E₁: x₁ = 4", p(1, 0, 0), 4), Triple("E₂: x₁ + x₂ + x₃ = 16", p(1, 1, 1), 16))),
        Flight("kugel", "Durch die Discokugel", p(8, 0, 1), p(-2, 2, 1), 4.0, listOf(Triple("E: x₃ = 4", p(0, 0, 1), 4)), listOf(Triple("K", p(4, 4, 3), 3))),
        Flight("schraeg", "Schräg durch den Raum", p(1, 7, 5), p(1, -1, -1), 5.0, listOf(Triple("E₁: x₂ = 4", p(0, 1, 0), 4), Triple("E₂: x₁ + 2x₃ = 9", p(1, 0, 2), 9), Triple("Boden: x₃ = 1", p(0, 0, 1), 1))),
    )
    fun byId(id: String) = all.firstOrNull { it.id == id }

    const val CUSTOM = "eigener"
    /** Eigener Flug als Text: „A;Richtung;Ebene 1;Ebene 2;Kugel“, jeweils Zahlen mit Komma getrennt; Ebene = n₁,n₂,n₃,c; Kugel = m₁,m₂,m₃,r; leer = nicht dabei */
    const val EXAMPLE = "0,0,1;2,2,1;1,0,0,4;;4,4,3,2"
    /**
     * Baut einen eigenen Flug: Start, Richtung, bis zu zwei Ebenen (Normalenvektor und rechte Seite) und eine Kugel.
     * Der Flug endet, wo die Gerade den Raum (0…8 | 0…8 | 0…6) verlässt.
     * @return der Flug oder eine Meldung, was nicht passt
     */
    fun custom(code: String): Result<Flight> = runCatching {
        fun bad(msg: String): Nothing = throw IllegalArgumentException(msg)
        val parts = code.split(';').map { p -> p.split(',').map { it.trim() }.filter { it.isNotEmpty() } }
        fun ints(i: Int, n: Int, what: String): List<Int>? { val p = parts.getOrNull(i).orEmpty(); if (p.isEmpty()) return null
            if (p.size != n || p.any { it.toIntOrNull() == null }) bad("$what braucht $n ganze Zahlen."); return p.map { it.toInt() } }
        val a = ints(0, 3, "Der Startpunkt")?.let { V3(it[0], it[1], it[2]) } ?: bad("Der Startpunkt fehlt.")
        val d = ints(1, 3, "Die Richtung")?.let { V3(it[0], it[1], it[2]) } ?: bad("Die Richtung fehlt.")
        if (d.isZero) bad("Die Richtung darf nicht (0|0|0) sein – sonst fliege ich nirgendwohin.")
        val b = Bounds(); if (a.x !in 0.0..8.0 || a.y !in 0.0..8.0 || a.z !in 0.0..6.0) bad("Der Start muss im Raum liegen: x₁ und x₂ von 0 bis 8, x₃ von 0 bis 6.")
        val tMax = Geo.exit(a, d, b.lo, b.hi); if (tMax < 0.25) bad("Mit dieser Richtung fliege ich sofort aus dem Raum. Dreh die Richtung nach innen.")
        val sub = listOf("₁", "₂")
        val planes = listOfNotNull(ints(2, 4, "Ebene 1"), ints(3, 4, "Ebene 2")).mapIndexed { i, p -> val n = V3(p[0], p[1], p[2])
            if (n.isZero) bad("Der Normalenvektor von Ebene ${i + 1} darf nicht (0|0|0) sein."); Triple("E${sub[i]}: ${Geo.planeText(n, p[3])}", n, p[3]) }
        val ball = ints(4, 4, "Die Kugel")?.let { if (it[3] < 1 || it[3] > 4) bad("Der Radius der Kugel muss zwischen 1 und 4 liegen."); Triple("K", V3(it[0], it[1], it[2]), it[3]) }
        Flight(CUSTOM, "Eigener Flug", a, d, tMax, planes, listOfNotNull(ball))
    }
}

// ───────────────────────── Pflichtteil, Kalender, Lux-Zubehör ─────────────────────────

/** Hilfsmittelfreier Teil: sechs kurze Aufgaben quer durch die drei Gebiete, ohne Tipp. */
object Quick {
    const val TASKS = 6
    fun pick(seed: Long): List<Level> {
        val r = Random(seed)
        return Area.entries.flatMap { area -> Levels.chapters.filter { it.area == area }.flatMap { it.levels }.filter { !it.abi }.shuffled(r).take(TASKS / 3) }
    }
}

/** Kalender der Tagesaufgaben – zeigt, was geschafft ist. Verpasste Tage kosten nichts. */
object Calendar {
    /** Tag im Monat (1–31) zu einem Tag seit dem 1.1.1970 – ohne Kalender-Bibliothek, damit es überall läuft */
    fun dayOfMonth(epochDay: Long): Int {
        val z = epochDay + 719_468; val era = (if (z >= 0) z else z - 146_096) / 146_097
        val doe = z - era * 146_097; val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100); val mp = (5 * doy + 2) / 153
        return (doy - (153 * mp + 2) / 5 + 1).toInt()
    }
    fun solved(stars: Map<String, Int>): Set<Long> = stars.keys.filter { Daily.isDaily(it) }.mapNotNull { it.removePrefix(Daily.PREFIX).toLongOrNull() }.toSet()
    /** Die letzten [weeks] Wochen bis einschließlich der laufenden, Montag zuerst; künftige Tage als null */
    fun grid(today: Long, weeks: Int = 5): List<List<Long?>> {
        val monday = today - (today + 3).mod(7L)          // Tag 0 (1.1.1970) war ein Donnerstag
        return (weeks - 1 downTo 0).map { w -> (0..6).map { d -> (monday - 7L * w + d).takeIf { it <= today } } }
    }
    /** Längste Folge aufeinanderfolgender gelöster Tage – nur zum Freuen */
    fun bestRun(days: Set<Long>): Int { var best = 0; for (d in days) if (d - 1 !in days) { var n = 1; while (d + n in days) n++; best = maxOf(best, n) }; return best }
}

/** Zubehör für Lux: Leuchtspur und Aufsatz, freigeschaltet durch Erfolge. */
data class Extra(val id: String, val name: String, val emoji: String, val badges: Int)
object Looks {
    val trails = listOf(Extra("none", "ohne Spur", "▫️", 0), Extra("sparks", "Funkenspur", "✨", 2), Extra("rainbow", "Regenbogen", "🌈", 5))
    val hats = listOf(Extra("none", "ohne", "▫️", 0), Extra("antenna", "Antenne", "📡", 1), Extra("halo", "Lichtring", "😇", 4), Extra("crown", "Krone", "👑", 8))
}

// ───────────────────────── Zufallsmaschine ─────────────────────────

/** Ein Zufallsversuch mit Trefferwahrscheinlichkeit [p] (Zähler/Nenner oder Prozent), [n]-mal wiederholt. Der Ablauf steht mit [seed] fest. */
class ChanceRun(val p: Double, val n: Int, seed: Long) {
    /** hits[i] = Treffer nach i Versuchen */
    val hits: IntArray = IntArray(n + 1).also { h -> val r = Random(seed); for (i in 1..n) h[i] = h[i - 1] + if (r.nextDouble() < p) 1 else 0 }
    val total get() = hits[n]
    fun freq(i: Int) = if (i == 0) 0.0 else hits[i].toDouble() / i
    val mean get() = n * p
    val sigma get() = sqrt(n * p * (1 - p))
    /** Wie gut war die Wette? */
    fun verdict(bet: Int): String { val d = abs(bet - total)
        return when { d == 0 -> "Volltreffer – genau $total!"; d <= maxOf(1.0, sigma) -> "Stark getippt: nur $d daneben."; d <= 2 * sigma -> "Nicht schlecht: $d daneben."; else -> "$d daneben – der Zufall hält sich näher an n · p, als man denkt." } }
}
data class ChancePreset(val id: String, val title: String, val event: String, val num: Int, val den: Int) { val p get() = num.toDouble() / den }
object ChanceLab {
    val presets = listOf(ChancePreset("muenze", "💰 Münze", "Kopf", 1, 2), ChancePreset("wuerfel", "🎲 Würfel", "eine Sechs", 1, 6), ChancePreset("rad", "🎡 Glücksrad", "das Gewinnfeld (1 von 4)", 1, 4))
    val counts = listOf(10, 100, 1000, 10_000)
}
