package de.kurvenflug.app.ui

import de.kurvenflug.app.ui.flight.handlesFor
import de.kurvenflug.core.model.Levels
import de.kurvenflug.core.model.Vec
import org.junit.Assert.*
import org.junit.Test

/** Ziehen an den Griffen setzt genau die Werte, die zur gezogenen Stelle passen (eingerastet). */
class HandlesTest {
    @Test fun lineHandlesSetInterceptAndSlope() {
        val l = Levels.byId("2-6")!!
        val h = handlesFor(l, mapOf("m" to 0.0, "b" to 0.0))
        assertEquals(mapOf("b" to 1.0), h[0].apply(Vec(0.0, 1.2)))
        assertEquals(mapOf("m" to 0.5), h[1].apply(Vec(4.0, 2.1)))
    }

    @Test fun parabolaVertexAndWidth() {
        val l = Levels.byId("3-6")!!
        val h = handlesFor(l, mapOf("a" to 1.0, "d" to 0.0, "e" to 5.0))
        assertEquals(mapOf("d" to -2.0, "e" to 3.0), h[0].apply(Vec(-2.2, 3.1)))
        assertEquals(mapOf("a" to -0.25), h[1].apply(Vec(2.0, 4.0)))
    }

    @Test fun throwAimArrowSetsAngleAndSpeed() {
        val l = Levels.byId("4-4")!!
        val h = handlesFor(l, mapOf("angle" to 45.0, "v" to 8.0)).single()
        val r = h.apply(Vec(l.launch.x + 3.0, l.launch.y + 3.0))   // 45°, Länge √18 ≈ 4,24 → v ≈ 7
        assertEquals(45.0, r.getValue("angle"), 0.0)
        assertEquals(7.0, r.getValue("v"), 0.0)
    }

    @Test fun onlyFreeParamsAreChanged() {
        val l = Levels.byId("2-1")!!   // nur b frei
        assertEquals(1, handlesFor(l, emptyMap()).size)
    }
}
