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
