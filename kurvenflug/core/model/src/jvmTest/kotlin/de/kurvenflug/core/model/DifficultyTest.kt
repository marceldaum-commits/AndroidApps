package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Nachweis: Jede Stufe des Generators ist messbar schwerer als die vorige, „Extrem“ ist wirklich anspruchsvoll.
 * Schreibt die Kennzahlen-Tabelle nach build/schwierigkeit.md (für die Meldung an den Leitstand).
 */
class DifficultyTest {
    private val seeds = 1L..12L

    private data class Row(val tier: Int, val params: Double, val tol: Double, val rocks: Double, val steps: Double, val window: Double, val score: Double)

    private fun row(tier: Int, levels: List<Level>): Row {
        val m = levels.map { Difficulty.of(it) }
        return Row(tier, m.map { it.params.toDouble() }.average(), m.map { it.tolerance }.average(), m.map { it.obstacles.toDouble() }.average(),
            m.map { it.steps.toDouble() }.average(), m.map { it.window }.average(), m.map { it.score }.average())
    }

    /** Gleicher Startwert für alle Stufen – so wird dasselbe Grund-Level verglichen */
    private fun levels(kind: Kind, tier: Int) = seeds.map { Gen.level(kind, it * 7_331, tier) }

    @Test fun everyTierIsSolvableButOnlyInASmallRange() {
        for (kind in Gen.kinds) for (t in 0..Tier.MAX) for (l in levels(kind, t)) {
            assertNotEquals("$kind/$t Ersatz-Level", Gen.FALLBACK, l.title)
            assertTrue("$kind/$t nicht lösbar", Flight.fly(l, l.solution).success)
            assertFalse("$kind/$t schon mit Startwerten gelöst", Flight.fly(l, l.start).success)
            l.params.forEach { p -> assertEquals("$kind/$t/${p.key} nicht auf dem Raster", l.solution.getValue(p.key), p.snap(l.solution.getValue(p.key)), 1e-9) }
            assertTrue("$kind/$t Lösungsbereich leer", Difficulty.window(l) > 0)
        }
    }

    @Test fun eachTierIsStrictlyHarderPerKind() {
        val out = StringBuilder()
        for (kind in Gen.kinds) {
            val rows = (0..Tier.MAX).map { row(it, levels(kind, it)) }
            out.appendLine("$kind: " + rows.joinToString("  ") { "%d: p%.1f r%.2f a%.1f s%.1f w%.3f g%.1f".format(it.tier, it.params, it.tol, it.rocks, it.steps, it.window, it.score) })
            File("build").mkdirs(); File("build/schwierigkeit-arten.txt").writeText(out.toString())
            rows.zipWithNext { a, b ->
                val what = "$kind: ${Tier.names[b.tier]} gegenüber ${Tier.names[a.tier]}"
                assertTrue("$what – Gesamtwert ${b.score} nicht > ${a.score}", b.score > a.score)
                assertTrue("$what – Lösungsbereich größer", b.window <= a.window + 1e-9)
                assertTrue("$what – Ziel größer", b.tol <= a.tol + 1e-9)
                assertTrue("$what – weniger Parameter", b.params >= a.params)
            }
        }
    }

    @Test fun tableAndOverallMonotony() {
        val rows = (0..Tier.MAX).map { t -> row(t, Gen.kinds.flatMap { levels(it, t) }) }
        rows.zipWithNext { a, b ->
            val what = "${Tier.names[b.tier]} gegenüber ${Tier.names[a.tier]}"
            assertTrue("$what: Gesamtwert", b.score > a.score)
            assertTrue("$what: Lösungsbereich", b.window < a.window)
            assertTrue("$what: Zielzone", b.tol <= a.tol)
            assertTrue("$what: Hindernisse", b.rocks >= a.rocks)
            assertTrue("$what: Rechenschritte", b.steps >= a.steps)
            assertTrue("$what: Parameter", b.params >= a.params)
        }
        // „Extrem“ ist wirklich anspruchsvoll
        val x = rows.last()
        assertTrue("Extrem: Zielzone ${x.tol}", x.tol <= 0.35)
        assertTrue("Extrem: Hindernisse ${x.rocks}", x.rocks >= 4.5)
        assertTrue("Extrem: Lösungsbereich ${x.window}", x.window <= 0.05)
        assertTrue("Extrem: Rechenschritte ${x.steps}", x.steps >= 1.5 * rows.first().steps)

        fun f(v: Double, d: Int = 2) = String.format(java.util.Locale.GERMANY, "%.${d}f", v)
        val md = buildString {
            appendLine("| Stufe | Parameter | Zielzone (Radius, Kästchen) | Hindernisse | Rechenschritte | Lösungsbereich | Gesamtwert |")
            appendLine("|---|---|---|---|---|---|---|")
            rows.forEach { r ->
                appendLine("| ${Tier.label(r.tier)} | ${f(r.params, 1)} | ${f(r.tol)} | ${f(r.rocks, 1)} | ${f(r.steps, 1)} | ${f(r.window * 100, 1)} % | ${f(r.score, 1)} |")
            }
        }
        File("build").mkdirs(); File("build/schwierigkeit.md").writeText(md)
        println(md)
    }

    @Test fun modesClimbToExtreme() {
        assertEquals(listOf(0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4), (0 until 19).map { Storm.tier(it) })
        assertEquals(Tier.MAX, Storm.tier(100))
        assertTrue(Storm.level(1, 20).title.contains("Extrem"))
        assertEquals(listOf(2, 2, 3, 4, 4), listOf(1, 6, 7, 13, 99).map { Andromeda.tier(it) })
        assertTrue(Andromeda.planet(13).levels.all { it.title.contains("Extrem") })
    }
}
