package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

class LevelsTest {
    @Test fun everyLevelIsSolvableWithItsSolution() {
        for (l in Levels.all) {
            val r = Flight.fly(l, l.solution)
            assertTrue("${l.id} nicht lösbar: Treffer ${r.hitTargets}, Absturz ${r.crash}", r.success)
        }
    }

    @Test fun solutionsLieOnTheSliderGrid() {
        for (l in Levels.all) {
            assertEquals("${l.id}: Lösung für alle Regler", l.params.map { it.key }.toSet(), l.solution.keys)
            for (p in l.params) {
                val v = l.solution.getValue(p.key)
                assertEquals("${l.id}/${p.key} nicht einstellbar", v, p.snap(v), 1e-9)
            }
        }
    }

    @Test fun startValuesDoNotSolveTheLevel() {
        for (l in Levels.all) assertFalse("${l.id} schon mit Startwerten gelöst", Flight.fly(l, l.start).success)
    }

    @Test fun energyCellsLieOnTheSolutionPath() {
        for (l in Levels.all) {
            val r = Flight.fly(l, l.solution)
            assertEquals("${l.id}: Energiezellen nicht erreichbar", l.cells.indices.toSet(), r.cells)
        }
    }

    @Test fun idsAreUniqueAndNumbered() {
        assertEquals(Levels.all.size, Levels.all.map { it.id }.toSet().size)
        Levels.chapters.forEach { c -> c.levels.forEachIndexed { i, l -> assertEquals("${c.number}-${i + 1}", l.id) } }
    }

    /** Trigonometrie: nur wer rechnet, trifft – Laser höchstens 4 Winkel, Funk höchstens 5 Radien. */
    @Test fun trigonometryNeedsCalculation() {
        val c = Levels.chapters.first { it.number == 14 }
        assertEquals(7, c.levels.size)
        for (l in c.levels) {
            val p = l.params.single()
            val wins = generateSequence(p.min) { it + p.step }.takeWhile { it <= p.max + 1e-9 }.count { Flight.fly(l, mapOf(p.key to it)).success }
            assertTrue("${l.id}: $wins Stellungen treffen", wins in 1..(if (l.kind == Kind.LASER) 4 else 5))
        }
    }
}
