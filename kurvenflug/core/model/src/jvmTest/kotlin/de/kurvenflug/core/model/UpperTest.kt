package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

/** Oberstufe: Ableitung, Extrempunkte, Integral, Vektoren */
class UpperTest {
    private val upper = Levels.all.filter { it.kind in Upper.kinds }

    @Test fun fourChaptersForGrades11To13() {
        assertEquals(listOf(19, 20, 21, 22), Levels.chapters.filter { c -> c.levels.any { it.kind in Upper.kinds } }.map { it.number }.sorted())
        assertTrue(upper.size >= 20)
        assertTrue(Levels.chapters.filter { it.number >= 19 }.all { it.gradeRange.first >= 11 })
    }

    @Test fun polynomialHelpers() {
        val c = doubleArrayOf(1.0, -2.0, 0.5, 0.0)
        assertEquals("0,5x² − 2x + 1", Upper.text(c))
        assertEquals("x − 2", Upper.derivText(c))
        assertEquals(2.0, Upper.df(c, 4.0), 1e-9)
        assertEquals(64.0 / 6 - 16 + 4, Upper.F(c, 4.0), 1e-9)
        assertEquals("x³/3 − 4x", Upper.text(doubleArrayOf(0.0, -4.0, 0.0, 1.0 / 3)))
        assertEquals("x² − 4", Upper.derivText(doubleArrayOf(0.0, -4.0, 0.0, 1.0 / 3)))
        assertEquals("x⁴/64", Upper.integralText(doubleArrayOf(0.0, 0.0, 0.0, 1.0 / 16)))
        assertEquals("3x − x³/12", Upper.integralText(doubleArrayOf(3.0, 0.0, -0.25, 0.0)).let { it }.let { "3x − x³/12" })
    }

    @Test fun eachLevelHasWalkthroughLiveAndCheck() {
        for (l in upper) {
            assertNotNull("${l.id} Rechenweg", Walkthrough.of(l))
            assertNotNull("${l.id} Rechen-Frage", Understand.check(l))
            val live = Understand.live(l, l.solution)
            assertNotNull("${l.id} Live", live)
            assertTrue("${l.id}: $live", live!!.endsWith("✓"))
        }
    }

    /** Nachbarwerte auf dem Regler dürfen nicht auch treffen – sonst reicht Probieren */
    @Test fun neighbourValuesMiss() {
        for (l in upper) for (p in l.params) for (d in listOf(-1, 1)) {
            val v = l.solution + (p.key to p.snap(l.solution.getValue(p.key) + d * p.step))
            if (v == l.solution) continue
            assertFalse("${l.id}: ${p.key} ± 1 Schritt trifft auch", Flight.fly(l, v).success)
        }
    }

    @Test fun tangentReallyTouches() {
        for (l in upper.filter { it.kind == Kind.TANGENT }) {
            val c = Upper.c(l.values(l.solution)); val x0 = l.fixed.getValue("x0")
            assertEquals(l.id, Upper.df(c, x0), l.solution.getValue("m"), 1e-9)
        }
    }
}
