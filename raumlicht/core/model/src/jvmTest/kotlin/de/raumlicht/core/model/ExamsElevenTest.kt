package de.raumlicht.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pyramide, Spiegelsaal, Schiffe: Lösungen unabhängig nachgerechnet */
class ExamsElevenTest {
    private val eps = 1e-9
    @Test fun pyramide() {
        val m = (ExamsEleven.pyA + ExamsEleven.pyC) * 0.5
        assertEquals(listOf(3, 3, 4), V3(m.x, m.y, m.z + 4).ints); assertEquals(ExamsEleven.pyS.ints, V3(m.x, m.y, 4.0).ints)
        val cross = (ExamsEleven.pyB - ExamsEleven.pyA) cross (ExamsEleven.pyS - ExamsEleven.pyA)
        assertEquals(listOf(0, -24, 18), cross.ints); assertTrue(Geo.parallel(cross, ExamsEleven.pyN))
        listOf(ExamsEleven.pyA, ExamsEleven.pyB, ExamsEleven.pyS).forEach { assertEquals(ExamsEleven.PY_C.toDouble(), ExamsEleven.pyN dot it, eps) }
        assertEquals(ExamsEleven.PY_V.toDouble(), 36.0 * 4 / 3, eps)
        assertEquals(4.0, Geo.distPointPlane(ExamsEleven.pyQ, ExamsEleven.pyN, ExamsEleven.PY_C.toDouble()), eps)
    }
    @Test fun spiegelsaal() {
        val n = ExamsEleven.mN; val c = ExamsEleven.M_C.toDouble()
        val t = Geo.linePlane(ExamsEleven.mL, n, n, c)!!; assertEquals(1.0, t, eps)
        assertEquals(ExamsEleven.mF.ints, (ExamsEleven.mL + n * t).ints); assertEquals(c, n dot ExamsEleven.mF, eps)
        assertEquals(3.0, Geo.distPointPlane(ExamsEleven.mL, n, c), eps)
        assertEquals(ExamsEleven.mL2.ints, (ExamsEleven.mF * 2.0 - ExamsEleven.mL).ints)
        assertEquals(5.0, (ExamsEleven.mZ - ExamsEleven.mL2).length, eps)
        // Z liegt auf derselben Seite wie L, L′ auf der anderen: der Weg über den Spiegel ist wirklich |L′Z|
        assertTrue((n dot ExamsEleven.mZ) < c && (n dot ExamsEleven.mL) < c && (n dot ExamsEleven.mL2) > c)
    }
    @Test fun schiffe() {
        assertEquals(ExamsEleven.sPos3.ints, (ExamsEleven.sa + ExamsEleven.sad * 3.0).ints)
        val ll = Geo.lineLine(ExamsEleven.sa, ExamsEleven.sad, ExamsEleven.sb, ExamsEleven.sbd)
        assertEquals(ExamsEleven.sCross.ints, ll.point!!.ints)
        // Abstand der Schiffe zur Zeit t, ausprobiert in ganzen Minuten und halben: Minimum bei t = 3
        fun dist(t: Double) = ((ExamsEleven.sa + ExamsEleven.sad * t) - (ExamsEleven.sb + ExamsEleven.sbd * t)).length
        val best = (0..80).map { it / 10.0 }.minBy { dist(it) }
        assertEquals(ExamsEleven.S_TIME.toDouble(), best, eps); assertEquals(ExamsEleven.S_DIST.toDouble(), dist(3.0), eps)
        assertTrue("sie kreuzen sich nie gleichzeitig", dist(2.0) > 0 && dist(4.0) > 0)
    }
    @Test fun teileLoesbar() {
        for (id in listOf("pyramide", "spiegelsaal", "schiffe")) {
            val e = Exams.byId(id)!!; assertEquals(id, 12, e.maxPoints); assertEquals(4, e.parts.size)
            for (part in e.parts) { val l = part.level
                assertTrue("${l.id}: Lösung", l.evaluate(l.solution).success); assertTrue("${l.id}: Start löst nicht", !l.evaluate(l.start).success)
                assertTrue("${l.id}: Rechenweg", l.way.size >= 2 && l.tip.isNotBlank()) }
        }
    }
}
