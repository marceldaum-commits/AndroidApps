package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

/** Inhaltspaket 0.20: jedes Level lösbar, erklärt (Tipp, Rechenweg, Rechen-Frage, Live-Rechnung), Raten reicht nicht. */
class MoreTest {
    private val levels = More.chapters.flatMap { it.levels }

    @Test fun sevenChaptersAboutThirtyFiveLevels() {
        assertEquals(7, More.chapters.size)
        assertTrue(levels.size >= 35)
        assertEquals("Kapitelnummern eindeutig", Levels.chapters.size, Levels.chapters.map { it.number }.toSet().size)
        assertEquals("Level-IDs eindeutig", Levels.all.size, Levels.all.map { it.id }.toSet().size)
    }

    @Test fun everyLevelIsExplained() {
        for (l in levels) {
            assertTrue("${l.id} Tipp", l.tip.length > 10)
            assertNotNull("${l.id} Rechenweg passt nicht zur Lösung", Walkthrough.of(l))
            val c = Understand.check(l); assertNotNull("${l.id} Rechen-Frage", c)
            assertEquals("${l.id}: ${c!!.options}", 3, c.options.distinct().size)
            val live = Understand.live(l, l.solution)
            assertNotNull("${l.id} Live-Rechnung", live)
            assertTrue("${l.id}: $live", live!!.endsWith("✓"))
        }
    }

    @Test fun solvableAndStartDoesNotSolve() {
        for (l in levels) {
            assertTrue("${l.id} nicht lösbar", Flight.fly(l, l.solution).success)
            assertFalse("${l.id} Startwerte lösen schon", Flight.fly(l, l.start).success)
        }
    }

    /** Ein Reglerschritt daneben darf nicht treffen – sonst reicht Probieren */
    @Test fun neighbourValuesMiss() {
        for (l in levels + Upper.chapters.flatMap { it.levels }) for (p in l.params) for (d in listOf(-1, 1)) {
            val v = l.solution + (p.key to p.snap(l.solution.getValue(p.key) + d * p.step))
            if (v == l.solution) continue
            assertFalse("${l.id}: ${p.key} ${if (d > 0) "+" else "−"}1 Schritt trifft auch", Flight.fly(l, v).success)
        }
    }

    @Test fun earlierSineLevelsUnchanged() {
        // c fehlt → 0: alte Sinus-Level rechnen exakt wie vorher
        val l = Levels.byId("5-4")!!
        assertEquals(l.solution.getValue("a") * kotlin.math.sin(l.solution.getValue("b") * 1.3) + l.solution.getValue("d"), Flight.f(l, l.solution, 1.3), 0.0)
    }
}

/** Binomialverteilung: Formeln stimmen, alle Level lösbar, erklärt, Raten reicht nicht */
class StochastikTest {
    private val levels = Stochastik.chapter.levels

    @Test fun formulas() {
        assertEquals(120.0, Stochastik.choose(10, 3), 1e-9)
        assertEquals(0.375, Stochastik.pmf(4, 0.5, 2), 1e-12)
        assertEquals(1.0, Stochastik.cdf(7, 0.3, 7), 1e-12)
        assertEquals(22.0 / 64, Stochastik.cdf(6, 0.5, 2), 1e-12)
        assertEquals((0..5).sumOf { Stochastik.pmf(10, 0.5, it) }, Stochastik.cdf(10, 0.5, 5), 1e-12)
    }

    @Test fun levelsAreSolvableExplainedAndNotGuessable() {
        assertTrue(levels.size >= 6)
        assertTrue("Dichte und kumuliert", levels.any { (it.fixed["cum"] ?: 0.0) > 0.5 } && levels.any { (it.fixed["cum"] ?: 0.0) < 0.5 })
        for (l in levels) {
            assertTrue("${l.id} lösbar", Flight.fly(l, l.solution).success)
            assertFalse("${l.id} Start", Flight.fly(l, l.start).success)
            assertNotNull("${l.id} Rechenweg", Walkthrough.of(l))
            assertNotNull("${l.id} Frage", Understand.check(l))
            assertTrue("${l.id} Live", Understand.live(l, l.solution)!!.endsWith("✓"))
            for (p in l.params) for (d in listOf(-1, 1)) {
                val v = l.solution + (p.key to p.snap(l.solution.getValue(p.key) + d * p.step))
                if (v != l.solution) assertFalse("${l.id}: ${p.key} ±1 trifft auch", Flight.fly(l, v).success)
            }
        }
    }
}
