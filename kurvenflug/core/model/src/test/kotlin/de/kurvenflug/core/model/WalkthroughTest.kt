package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

class WalkthroughTest {
    @Test fun parabolaShiftIsExplainedStepByStep() {
        val s = Walkthrough.of(Levels.byId("3-1")!!)!! // y = x² + e durch (2 | 1)
        assertEquals(listOf(4.0, 4.0, -3.0), s.map { it.answer })
        assertEquals("e", s.last().sets)
        assertTrue(s.first().text.contains("?"))
    }

    @Test fun everyStepOffersThreeDifferentChoicesIncludingTheAnswer() {
        for (l in Levels.all) {
            val steps = Walkthrough.of(l) ?: continue
            for (st in steps) {
                assertEquals("${l.id}: ${st.text}", 3, st.choices.map { num(it) }.toSet().size)
                assertTrue("${l.id}: ${st.text}", st.choices.any { num(it) == st.answerText })
            }
        }
    }

    /** Die meisten Level mit Funktionskurve, Funk, Rendezvous und Trigonometrie bekommen einen Rechenweg. */
    @Test fun mostCalculableLevelsHaveAWalkthrough() {
        val kinds = setOf(Kind.LINE, Kind.PARABOLA, Kind.PROP, Kind.ANTI, Kind.LOG, Kind.MOTION, Kind.RADIUS, Kind.RENDEZVOUS)
        val candidates = Levels.all.filter { it.kind in kinds || it.triangle != null }
        val covered = candidates.filter { Walkthrough.of(it) != null }
        val report = candidates.joinToString("\n") { "${it.id} ${it.kind}: ${if (Walkthrough.of(it) != null) "ja" else "–"}" }
        java.io.File(System.getProperty("java.io.tmpdir"), "walkthrough-coverage.txt").writeText(report)
        assertTrue("nur ${covered.size} von ${candidates.size}:\n$report", covered.size * 4 >= candidates.size * 3)
        // Trigonometrie vollständig (außer der Spiegel-Aufgabe ohne Dreieck)
        assertEquals(6, Levels.chapters.first { it.number == 14 }.levels.count { Walkthrough.of(it) != null })
    }
}
