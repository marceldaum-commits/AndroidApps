package de.raumlicht.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Kapitel 28 (Lage von Gerade und Ebene): jede Lösung wird unabhängig nachgerechnet */
class TourElevenTest {
    private fun dot(a: V3, b: V3) = (a dot b).toInt()
    private fun multiple(a: V3, b: V3): Int? { // k mit a = k · b (ganzzahlig, k ≠ 0) oder null
        val k = (0..2).firstOrNull { b.ints[it] != 0 }?.let { a.ints[it].toDouble() / b.ints[it] } ?: return null
        return if (k != 0.0 && (0..2).all { Math.abs(a.ints[it] - k * b.ints[it]) < 1e-9 }) k.toInt() else null
    }
    /** 0 = parallel, 1 = schneidet, 2 = liegt in */
    private fun lage(a: V3, d: V3) = when { dot(TourEleven.nE, d) != 0 -> 1; dot(TourEleven.nE, a) == TourEleven.cE -> 2; else -> 0 }
    /** 0 = parallel, 1 = schneiden sich, 2 = identisch */
    private fun lage(e1: TourTen.Eq, e2: TourTen.Eq): Int { val k = multiple(e2.n, e1.n) ?: return 1; return if (e2.c == k * e1.c) 2 else 0 }

    @Test fun geradeEbene() {
        assertEquals(0, lage(TourEleven.a1, TourEleven.d1)); assertEquals(2, lage(TourEleven.a2, TourEleven.d2)); assertEquals(1, lage(TourEleven.a3, TourEleven.d3))
        // Schnittpunkt aus 28-3: t = 1 → S(3|2|1) liegt auf g und in E
        val s = TourEleven.a3 + TourEleven.d3 * 1.0
        assertEquals(listOf(3, 2, 1), s.ints); assertEquals(TourEleven.cE, dot(TourEleven.nE, s))
        // 28-4: für a = −1 liegt g in E; für jedes andere a nicht
        assertEquals(TourEleven.c4, dot(TourEleven.n4, TourEleven.a4))
        for (a in -4..4) assertEquals("a=$a", a == TourEleven.A4, dot(TourEleven.n4, TourEleven.d4(a)) == 0)
    }

    @Test fun ebeneEbene() {
        assertEquals(2, lage(TourEleven.s5[0], TourEleven.s5[1])); assertEquals(0, lage(TourEleven.s6[0], TourEleven.s6[1]))
        // 28-7: parallel (und nicht identisch) genau für k = 2
        for (k in 0..6) assertEquals("k=$k", if (k == TourEleven.K7) 0 else 1, lage(TourEleven.e7, TourEleven.f7(k)))
        // 28-8: Schnittgerade (5 − t|2|t) liegt in beiden Ebenen, P(1|2|4) darauf
        for (t in 0..5) { val q = V3(5 - t, 2, t); TourEleven.s8.forEach { assertEquals(it.c, dot(it.n, q)) } }
        TourEleven.s8.forEach { assertEquals(it.c, dot(it.n, TourEleven.p8)) }
        assertEquals(1, TourEleven.p8.ints[0])
    }

    @Test fun aufgabenLoesen() {
        val ch = Levels.chapters.first { it.number == 28 }
        assertEquals(8, ch.levels.size); assertEquals(1, ch.levels.count { it.abi })
        for (l in ch.levels) {
            assertTrue("${l.id}: Lösung", l.evaluate(l.solution).success)
            assertTrue("${l.id}: Start löst nicht", !l.evaluate(l.start).success)
            assertTrue("${l.id}: Rechenweg", l.way.size >= 3 && l.tip.isNotBlank())
        }
        assertEquals("lage-ebene", Theory.forLevel(Levels.byId("28-1")!!).key)
        assertTrue(Soon.topics.none { it.title.contains("Lage von Gerade und Ebene") })
    }
}
