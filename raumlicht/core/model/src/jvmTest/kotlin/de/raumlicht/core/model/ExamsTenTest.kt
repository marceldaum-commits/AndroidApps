package de.raumlicht.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/** Die vier neuen Abi-Prüfungen (Dach, Kran, Ballon, Rutsche): jede Lösung wird unabhängig nachgerechnet */
class ExamsTenTest {
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)
    private val eps = 1e-9

    @Test fun dach() {
        val n = ExamsTen.roofN; val c = ExamsTen.ROOF_C.toDouble()
        listOf(ExamsTen.roofA, ExamsTen.roofB, ExamsTen.roofC).forEach { assertEquals(c, n dot it, eps) }
        // n = AB × AC (bis auf Faktor 9)
        val cross = (ExamsTen.roofB - ExamsTen.roofA) cross (ExamsTen.roofC - ExamsTen.roofA)
        assertEquals(p(9, 18, 18).ints, cross.ints)
        assertEquals(2.0, Geo.distPointPlane(ExamsTen.chimney, n, c), eps)
        // Regentropfen
        assertEquals(ExamsTen.rainS.ints, (ExamsTen.rainA + ExamsTen.rainD * Geo.linePlane(ExamsTen.rainA, ExamsTen.rainD, n, c)!!).ints)
        // Spiegelbild: Mitte von P und P′ liegt in E, PP′ steht senkrecht auf E
        val mid = (ExamsTen.mirrorP + ExamsTen.mirrorQ) * 0.5
        assertEquals(c, n dot mid, eps)
        assertTrue(Geo.parallel(ExamsTen.mirrorQ - ExamsTen.mirrorP, n))
        assertEquals(p(2, 3, 2).ints, mid.ints)
    }

    @Test fun kran() {
        val s = ExamsTen.craneS
        assertEquals(s.ints, (ExamsTen.craneA + ExamsTen.craneD * 2.0).ints)
        assertEquals(s.ints, (ExamsTen.ropeB + ExamsTen.ropeD * 3.0).ints)
        assertEquals(45.0, Geo.angle(ExamsTen.craneD, ExamsTen.ropeD), 1e-9)
        // Abstand windschiefer Geraden
        val ll = Geo.lineLine(ExamsTen.craneA, ExamsTen.craneD, ExamsTen.cableK, ExamsTen.cableD)
        assertEquals(3.0, ll.distance, eps); assertTrue(ll.relation == Relation.SKEW)
        // Seilmitte: M liegt auf h, in der Mitte von S und W
        assertEquals(ExamsTen.ropeM.ints, ((s + ExamsTen.ropeW) * 0.5).ints)
        assertTrue(Geo.onLine(ExamsTen.ropeW, ExamsTen.ropeB, ExamsTen.ropeD))
    }

    @Test fun ballon() {
        val m = ExamsTen.balloonM; val r = ExamsTen.BALLOON_R.toDouble()
        assertEquals(m.ints, ((ExamsTen.balloonA + ExamsTen.balloonB) * 0.5).ints)
        assertEquals(r, (ExamsTen.balloonB - ExamsTen.balloonA).length / 2, eps)
        // Berührpunkt liegt auf der Kugel, MT ∥ n, c = n · T
        assertEquals(r, (ExamsTen.touch - m).length, eps)
        assertEquals(ExamsTen.touchN.ints, (ExamsTen.touch - m).ints)
        assertEquals(ExamsTen.TOUCH_C.toDouble(), ExamsTen.touchN dot ExamsTen.touch, eps)
        assertEquals(1.0, (ExamsTen.flier - m).length - r, eps)
    }

    @Test fun rutsche() {
        val a = ExamsTen.slideA; val d = ExamsTen.slideD
        assertEquals(ExamsTen.slideGround.ints, (a + d * Geo.linePlane(a, d, p(0, 0, 1), 0.0)!!).ints)
        // Winkel Gerade–Ebene: sin α = |d · n| / (|d| |n|)
        val sin = abs(d dot p(0, 0, 1)) / d.length
        assertEquals(45.0, Math.toDegrees(Math.asin(sin)), 1e-9)
        // Handlauf: parallel, Abstand 2
        assertTrue(Geo.parallel(d, d))
        assertEquals(2.0, Geo.distPointLine(ExamsTen.railB, a, d), eps)
        // Punkt in Höhe 1
        assertEquals(1.0, ExamsTen.slideMid.z, eps); assertTrue(Geo.onLine(ExamsTen.slideMid, a, d))
        assertEquals(sqrt(2.0), d.length, eps)
    }

    @Test fun alleTeileLoesbarMitHinweisen() {
        for (id in listOf("dach", "kran", "ballon", "rutsche")) {
            val e = Exams.byId(id)!!
            assertEquals(id, 12, e.maxPoints); assertEquals(4, e.parts.size)
            for (part in e.parts) {
                val l = part.level
                assertTrue("${l.id}: Lösung", l.evaluate(l.solution).success)
                assertTrue("${l.id}: Start löst nicht", !l.evaluate(l.start).success)
                assertTrue("${l.id}: Rechenweg", l.way.size >= 2 && l.tip.isNotBlank())
                assertTrue("${l.id}: Abi", l.id.startsWith("abi-$id-"))
            }
        }
    }
}
