package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test

class GeoTest {
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)

    @Test fun crossAndDot() {
        assertEquals(p(-2, -2, 4), p(2, 0, 1) cross p(0, 2, 1))
        assertEquals(0.0, p(1, 1, -2) dot p(2, 0, 1), 0.0)
    }

    @Test fun linePlaneIntersection() {
        val t = Geo.linePlane(p(1, 1, 1), p(1, 2, 0), p(1, 1, 1), 9.0)!!
        assertEquals(2.0, t, 1e-9)
        assertNull(Geo.linePlane(p(0, 0, 1), p(1, 1, 0), p(0, 0, 1), 4.0))
    }

    @Test fun lineRelations() {
        val x = Geo.lineLine(p(1, 1, 1), p(2, 1, 1), p(7, 1, 1), p(-1, 1, 1))
        assertEquals(Relation.INTERSECT, x.relation); assertTrue(x.point!!.near(p(5, 3, 3)))
        assertEquals(2.0, x.s, 1e-9); assertEquals(2.0, x.t, 1e-9)
        assertEquals(Relation.SKEW, Geo.lineLine(p(0, 6, 1), p(2, -1, 1), p(7, 1, 0), p(-1, 1, 0)).relation)
        assertEquals(Relation.PARALLEL, Geo.lineLine(p(0, 0, 0), p(1, 2, 3), p(1, 0, 0), p(2, 4, 6)).relation)
        assertEquals(Relation.IDENTICAL, Geo.lineLine(p(0, 0, 0), p(1, 2, 3), p(2, 4, 6), p(-1, -2, -3)).relation)
        // Abstand windschiefer Geraden: x-Achse und Parallele zur y-Achse in Höhe 3
        assertEquals(3.0, Geo.lineLine(p(0, 0, 0), p(1, 0, 0), p(0, 0, 3), p(0, 1, 0)).distance, 1e-9)
    }

    @Test fun distancesAndMirror() {
        assertEquals(3.0, Geo.distPointPlane(p(1, 2, 1), p(1, 2, 2), 16.0), 1e-9)
        assertTrue(Geo.footOnPlane(p(4, 4, 4), p(1, 1, 1), 6.0).near(p(2, 2, 2)))
        assertTrue(Geo.mirrorAtPlane(p(2, 2, 3), p(1, 1, 0), 8.0).near(p(6, 6, 3)))
        assertEquals(1.0, Geo.distPointLine(p(0, 1, 0), p(0, 0, 0), p(1, 0, 0)), 1e-9)
    }

    @Test fun directions() {
        assertTrue(Geo.sameDirection(p(1, 2, 1), p(2, 4, 2)))
        assertFalse(Geo.sameDirection(p(-1, -2, -1), p(2, 4, 2)))
        assertTrue(Geo.parallel(p(-1, -2, -1), p(2, 4, 2)))
        assertFalse(Geo.parallel(p(0, 0, 0), p(2, 4, 2)))
    }

    @Test fun numbersInGermanNotation() {
        assertEquals("−3", Geo.num(-3)); assertEquals("0,5", Geo.num(0.5)); assertEquals("(4|−4|3)", p(4, -4, 3).fmt())
        assertEquals("1,41", Geo.num(1.41421))
        // große Zahlen ohne Exponent und ohne Überlauf, sehr große lesbar mit Zehnerpotenz
        assertEquals("485000000", Geo.num(4.85e8)); assertEquals("12345678,5", Geo.num(1.23456785e7)); assertEquals("21474836,48", Geo.num(21474836.48)); assertEquals("−21474837", Geo.num(-21474837.0))
        assertEquals("999999999999,99", Geo.num(999999999999.99)); assertEquals("1 · 10¹²", Geo.num(1e12)); assertEquals("4,85 · 10¹⁴", Geo.num(4.85e14)); assertEquals("−4,85 · 10¹⁴", Geo.num(-4.85e14))
        assertEquals("1 · 10¹⁵", Geo.num(9.999e14)); assertEquals("1,5 · 10³⁰⁰", Geo.num(1.5e300)); assertEquals("2,35 · 10²⁰", Geo.num(2.346e20))
        assertEquals("∞", Geo.num(Double.POSITIVE_INFINITY)); assertEquals("−∞", Geo.num(Double.NEGATIVE_INFINITY)); assertEquals("nicht definiert", Geo.num(Double.NaN))
        // kaufmännisch und symmetrisch gerundet; nie „−0“
        assertEquals("0,13", Geo.num(0.125)); assertEquals("−0,13", Geo.num(-0.125)); assertEquals("2,5", Geo.num(2.5)); assertEquals("−2,5", Geo.num(-2.5)); assertEquals("−0,38", Geo.num(-0.375))
        listOf(-0.004, -0.0, 0.0, 0.004, -1e-300).forEach { assertEquals("0", Geo.num(it)) }; assertEquals("−0,01", Geo.num(-0.006)); assertEquals("0,01", Geo.num(0.005))
        // normale Zahlen wie bisher: Komma, echtes Minus, keine überflüssigen Nullen
        assertEquals("0,07", Geo.num(0.07)); assertEquals("12,3", Geo.num(12.30)); assertEquals("−7", Geo.num(-7.001)); assertEquals("100", Geo.num(99.999)); assertEquals("0,1", Geo.num(0.1))
        assertEquals("1234567,89", Geo.num(1234567.89)); assertEquals("41,81", Geo.num(41.8103)); assertEquals("2147483647", Geo.num(Int.MAX_VALUE)); assertEquals("−2147483648", Geo.num(Int.MIN_VALUE))
        // wie die alte Rechnung für alle gewöhnlichen positiven Werte (dort gab es nie ein Problem)
        val r = kotlin.random.Random(5); repeat(20000) { val v = r.nextDouble(0.0, 5000.0); val old = (Math.round(v * 100) / 100.0).let { if (it == Math.rint(it)) it.toLong().toString() else it.toString().replace('.', ',') }
            assertEquals(old, Geo.num(v)); if (old != "0") assertEquals("−$old", Geo.num(-v)) }
    }

    @Test fun exitFromRoom() {
        assertEquals(4.0, Geo.exit(p(0, 0, 0), p(2, 1, 0), p(0, 0, 0), p(8, 8, 6)), 1e-9)
    }
}
