package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class ExprTest {
    private fun v(src: String, x: Double) = Expr.parse(src).getOrThrow().at(x)

    @Test fun readsTermsTheWayPeopleTypeThem() {
        assertEquals(2.0, v("x^3-3x", 2.0), 1e-9); assertEquals(-4.0, v("-x^2", 2.0), 1e-9)                 // Minus bindet schwächer als hoch
        assertEquals(9.0, v("3(x+1)", 2.0), 1e-9); assertEquals(2 * sin(1.0), v("2sin(x)", 1.0), 1e-9); assertEquals(sin(2.0), v("sin 2x", 1.0), 1e-9)
        assertEquals(1.5, v("f(x) = 0,5x + 1", 1.0), 1e-9); assertEquals(5.0, v("x² + 1", 2.0), 1e-9); assertEquals(3.0, v("√(x+5)", 4.0), 1e-9)
        assertEquals(exp(-1.0) * cos(3.0), v("e^(-x)·cos(3x)", 1.0), 1e-9); assertEquals(0.25, v("2^-x", 2.0), 1e-9); assertEquals(PI, v("π", 0.0), 1e-9)
        assertEquals(512.0, v("2^3^2", 0.0), 1e-9); assertEquals(1.0, v("x:2", 2.0), 1e-9); assertEquals(sin(3.0) * 3, v("xsin(x)", 3.0), 1e-9)
        assertEquals(2.0, v("ln(e^2)", 0.0), 1e-9); assertEquals(3.0, v("log(1000)", 0.0), 1e-9); assertEquals(4.0, v("abs(-4)", 0.0), 1e-9)
        assertTrue(v("1/x", 0.0).isInfinite()); assertTrue(v("sqrt(x)", -1.0).isNaN())
        assertEquals("x³−3x", Expr.parse(" x^3-3x ").getOrThrow().text)
    }

    @Test fun explainsWhatItDoesNotUnderstand() {
        fun err(src: String) = Expr.parse(src).exceptionOrNull()!!.message!!
        assertTrue(err("").contains("Tipp einen Term ein")); assertTrue(err("2*(x+1").contains("schließende Klammer")); assertTrue(err("x+1)").contains("Klammer zu viel"))
        assertTrue(err("foo(x)").contains("kenne ich nicht")); assertTrue(err("x+").contains("hört mittendrin auf")); assertTrue(err("x $ 2").contains("„$“"))
    }

    @Test fun ownFunctionBecomesARideWithItsSpecialPoints() {
        val r = Coaster.custom(Expr.parse("x^3-3x").getOrThrow(), -2.0, 2.0)!!
        assertEquals(listOf(SpotKind.ZERO to -1.73, SpotKind.HIGH to -1.0, SpotKind.TURN to 0.0, SpotKind.ZERO to 0.0, SpotKind.LOW to 1.0, SpotKind.ZERO to 1.73), r.spots.map { it.kind to it.x })
        assertEquals(-9.0, r.data(0.0).let { it.slope } - 18, 1e-3)                                   // f′(−2) = 9, numerisch
        assertEquals(2.0, r.data(0.25).height, 1e-9); assertFalse(r.clipped)
        // jede Kurve passt in die Bühne, egal wie groß die Werte sind
        listOf("1000x^2" to (-3.0 to 3.0), "0,001sin(x)" to (0.0 to 60.0), "x" to (100.0 to 400.0), "5" to (0.0 to 4.0), "e^x" to (0.0 to 12.0)).forEach { (t, iv) ->
            val ride = Coaster.custom(Expr.parse(t).getOrThrow(), iv.first, iv.second)!!
            ride.points.forEach { p -> assertTrue("$t $p", p.y in 0.6..7.4 && p.z in 0.4..6.0) }
            assertTrue(t, ride.scene(ride.x(0.5), derivative = true, fill = true).contains("axes2d"))
        }
        // Konstante und Gerade: keine erfundenen Extrema oder Wendestellen
        assertEquals(emptyList<Spot>(), Coaster.custom(Expr.parse("5").getOrThrow(), 0.0, 4.0)!!.spots)
        assertEquals(listOf(SpotKind.ZERO to 2.0), Coaster.custom(Expr.parse("2x-4").getOrThrow(), 0.0, 4.0)!!.spots.map { it.kind to it.x })
        // lange Fahrt: sin über 10 Perioden hat 20 Extrema
        val wave = Coaster.custom(Expr.parse("sin(x)").getOrThrow(), 0.0, 20 * PI)!!
        assertEquals(20, wave.spots.count { it.kind == SpotKind.HIGH || it.kind == SpotKind.LOW })
    }

    @Test fun polesAreClippedNotInvented() {
        val r = Coaster.custom(Expr.parse("1/x").getOrThrow(), -2.0, 2.0)!!
        assertTrue(r.clipped); assertEquals(emptyList<Spot>(), r.spots)                                   // der Vorzeichensprung bei 0 ist keine Nullstelle
        r.points.forEach { p -> assertTrue(p.z.isFinite() && p.z in 0.4..6.0) }
        val t = Coaster.custom(Expr.parse("tan(x)").getOrThrow(), 0.0, 6.0)!!
        assertEquals(listOf(3.14), t.spots.filter { it.kind == SpotKind.ZERO }.map { it.x })
        assertNull(Coaster.custom(Expr.parse("sqrt(x)").getOrThrow(), -9.0, -1.0))                        // nirgends definiert
        assertNotNull(Coaster.custom(Expr.parse("sqrt(x)").getOrThrow(), -1.0, 9.0))
    }
}
