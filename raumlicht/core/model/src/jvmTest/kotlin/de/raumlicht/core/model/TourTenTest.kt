package de.raumlicht.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Kapitel 27: jedes Gleichungssystem wird unabhängig nachgerechnet (Cramersche Regel mit ganzen Zahlen) */
class TourTenTest {
    private fun m(e: List<TourTen.Eq>) = e.map { it.n.ints }
    private fun det(a: List<List<Int>>) =
        a[0][0] * (a[1][1] * a[2][2] - a[1][2] * a[2][1]) - a[0][1] * (a[1][0] * a[2][2] - a[1][2] * a[2][0]) + a[0][2] * (a[1][0] * a[2][1] - a[1][1] * a[2][0])
    private fun rank(a: List<List<Int>>): Int {
        if (det(a) != 0) return 3
        val rows = a.filter { r -> r.any { it != 0 } }
        if (rows.isEmpty()) return 0
        val cross = { u: List<Int>, v: List<Int> -> listOf(u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0]) }
        return if (rows.any { u -> rows.any { v -> cross(u, v).any { it != 0 } } }) 2 else 1
    }
    private fun solve(e: List<TourTen.Eq>): List<Int> {
        val a = m(e); val d = det(a); assertTrue("Determinante darf nicht 0 sein", d != 0)
        return (0..2).map { k ->
            val b = a.mapIndexed { i, r -> r.mapIndexed { j, v -> if (j == k) e[i].c else v } }
            assertEquals(0, det(b) % d); det(b) / d
        }
    }

    @Test fun eindeutigeSysteme() {
        assertEquals(TourTen.p1.ints, solve(TourTen.s1))
        assertEquals(TourTen.p2.ints, solve(TourTen.s2))
        assertEquals(TourTen.p3.ints, solve(TourTen.s3))
        assertEquals(TourTen.p7.ints, solve(TourTen.s7))
    }

    @Test fun unendlichVieleLoesungen() {
        val s = TourTen.s5; val a = m(s)
        assertEquals(2, rank(a))
        // III = I + II, auch rechts
        assertEquals(s[0].c + s[1].c, s[2].c)
        (0..2).forEach { assertEquals(a[0][it] + a[1][it], a[2][it]) }
        // der gesuchte Punkt erfüllt alle drei; (4 − t | 2 | t) ist die ganze Lösungsmenge
        s.forEach { assertEquals(it.c, (it.n dot TourTen.p5).toInt()) }
        (0..5).forEach { t -> val pt = V3(4 - t, 2, t); s.forEach { assertEquals(it.c, (it.n dot pt).toInt()) } }
    }

    @Test fun keineLoesung() {
        val s = TourTen.s6
        assertEquals(2, rank(m(s)))                       // Koeffizientenmatrix hat Rang 2 …
        assertEquals(s[0].n.ints, s[1].n.ints)            // … I und II sind parallel
        assertTrue(s[0].c != s[1].c)                      // … und verschieden: Widerspruch 0 = −2
        assertEquals(0, TourTen.n6)
    }

    @Test fun rechenschritt() {
        val i = TourTen.s4[0]; val ii = TourTen.s4[1]
        assertEquals(0, ii.n.ints[0] - TourTen.K4 * i.n.ints[0])
        assertEquals(-5, ii.n.ints[1] - TourTen.K4 * i.n.ints[1]); assertEquals(-4, ii.n.ints[2] - TourTen.K4 * i.n.ints[2]); assertEquals(-5, ii.c - TourTen.K4 * i.c)
    }

    @Test fun aufgabenLoesen() {
        val ch = Levels.chapters.first { it.number == 27 }
        assertEquals(7, ch.levels.size)
        assertEquals(1, ch.levels.count { it.abi })
        for (l in ch.levels) {
            assertTrue("${l.id}: Lösung muss stimmen", l.evaluate(l.solution).success)
            assertTrue("${l.id}: Startwert löst nicht", !l.evaluate(l.start).success)
            assertTrue("${l.id}: Weg und Tipp vorhanden", l.way.size >= 3 && l.tip.isNotBlank())
            assertEquals("${l.id}: Karte", "lgs", Theory.forLevel(l).key)
        }
    }
}
