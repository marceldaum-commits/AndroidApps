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
    }

    @Test fun exitFromRoom() {
        assertEquals(4.0, Geo.exit(p(0, 0, 0), p(2, 1, 0), p(0, 0, 0), p(8, 8, 6)), 1e-9)
    }
}
