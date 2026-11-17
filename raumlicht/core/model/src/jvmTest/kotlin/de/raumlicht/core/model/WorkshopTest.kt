package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class WorkshopTest {
    @Test fun splineGoesSmoothlyThroughTheSupports() {
        val t = BuiltTrack(listOf(6, 2, 5, 2, 1))
        (0..4).forEach { assertEquals(t.heights[it].toDouble(), t.y(it * 2.0), 1e-9) }
        // an den Stützstellen kein Knick: Steigung von links und rechts gleich
        (1..3).forEach { assertEquals(t.slope(it * 2.0 - 1e-7), t.slope(it * 2.0 + 1e-7), 1e-4) }
        assertEquals((t.y(3.001) - t.y(2.999)) / 0.002, t.slope(3.0), 1e-4)
        assertEquals(3.0, BuiltTrack(listOf(3, 3, 3, 3, 3)).y(5.3), 1e-9); assertEquals(0.0, BuiltTrack(listOf(3, 3, 3, 3, 3)).slope(5.3), 1e-9)
    }

    @Test fun cartObeysEnergy() {
        // eben: der Schubs reicht nicht – ausgerollt
        val flat = BuiltTrack(listOf(3, 3, 3, 3, 3)).run(); assertFalse(flat.finished); assertTrue(flat.stuckAt!! in 0.5..4.0)
        // bergab kommt er an und wird schneller
        val down = BuiltTrack(listOf(6, 4, 3, 2, 1)).run(); assertTrue(down.finished); assertTrue(down.endSpeed > 6.0); assertEquals(8.0, down.xs.last(), 1e-9)
        // ohne Reibung wäre v² = v0² + 2·g·Δh; mit Reibung etwas weniger
        val ideal = kotlin.math.sqrt(BuiltTrack.PUSH * BuiltTrack.PUSH + 2 * BuiltTrack.G * 5); assertTrue(down.endSpeed < ideal && down.endSpeed > ideal * 0.85)
        // ein Hügel höher als der Start: der Wagen kehrt vorher um und rollt zurück
        val wall = BuiltTrack(listOf(4, 1, 6, 1, 1)).run(); assertFalse(wall.finished); assertTrue(wall.stuckAt!! > 2.0 && wall.stuckAt!! < 4.0)
        assertTrue(wall.xs.zipWithNext().any { (a, b) -> b < a })
        assertTrue(BuiltTrack(listOf(6, 1, 5, 1, 1)).run().finished)                                   // knapp niedriger als der Start: reicht
        assertTrue(listOf(flat, down, wall).all { r -> r.xs.all { it in 0.0..8.0 } && r.seconds <= 14.1 })
    }

    @Test fun everyGoalIsSolvableButNotFromTheStart() {
        BuiltTrack.goals.forEach { g ->
            assertTrue(g.title, BuiltTrack(g.solution).solves(g)); assertFalse(g.title, BuiltTrack(g.start).solves(g))
            assertTrue(g.title, (g.solution + g.start).all { it in BuiltTrack.HEIGHTS })
            val s = BuiltTrack(g.start).scene(g); assertTrue(s.contains("\"flat\":true") && s.contains("\"type\":\"handle\"") && s.contains("\"axes\":[2]"))
        }
        // Stern verfehlt: Bahn führt zu tief am Stern vorbei
        val g = BuiltTrack.goals[1]; val r = BuiltTrack(listOf(6, 4, 2, 1, 1)).run(g); assertTrue(r.finished); assertEquals(0, r.stars)
        // Tempo-Auftrag: ankommen allein reicht nicht
        assertFalse(BuiltTrack(listOf(2, 2, 2, 1, 1)).solves(BuiltTrack.goals[3]))
    }

    @Test fun ghostScoreRewardsTheTrueDerivative() {
        assertEquals(5, Ghost.rides.size)
        Ghost.rides.forEach { r ->
            val exact = (0 until Ghost.COLUMNS).map { r.df(Ghost.x(r, it)) }
            assertEquals(r.id, 100, Ghost.score(r, exact))
            val (lo, hi) = Ghost.window(r); assertTrue(lo < 0 && hi > 0 && exact.all { it in lo..hi })
            assertTrue(r.id, Ghost.score(r, exact.map { -it }) < 60)                                    // gespiegelt: klar schlechter
            assertEquals(0, Ghost.score(r, List(Ghost.COLUMNS) { null }))
            val near = exact.map { it + (hi - lo) * 0.03 }; assertTrue(r.id, Ghost.score(r, near) in 88..99)   // leicht daneben: fast voll
        }
        assertTrue(Ghost.verdict(95).startsWith("Wie abgepaust")); assertTrue(Ghost.verdict(20).contains("steigt"))
        // G: Eine flache Linie auf (oder knapp neben) der Achse zeichnet das Vorzeichen von f′ nicht nach – unter 50 %, also nie „die Richtung passt“
        Ghost.rides.forEach { r ->
            val (lo, hi) = Ghost.window(r); val exact = (0 until Ghost.COLUMNS).map { r.df(Ghost.x(r, it)) }
            listOf(0.0, (hi - lo) * 0.02, -(hi - lo) * 0.02).forEach { level -> val flat = List(Ghost.COLUMNS) { level }
                assertTrue("${r.id} $level: ${Ghost.score(r, flat)}", Ghost.score(r, flat) < 50); assertFalse(Ghost.verdict(Ghost.score(r, flat)).contains("Richtung passt")); assertTrue(Ghost.direction(r, flat) < 0.75) }
            assertEquals(1.0, Ghost.direction(r, exact), 0.0); assertEquals(0.0, Ghost.direction(r, exact.map { -it }), 0.0); assertEquals(0.0, Ghost.direction(r, List(Ghost.COLUMNS) { null }), 0.0)
            // gute Zeichnungen bleiben gut: richtige Form mit halber oder anderthalbfacher Höhe, grob in Stufen gezeichnet
            assertTrue(r.id, Ghost.score(r, exact.map { it * 0.5 }) >= 50); assertTrue(r.id, Ghost.score(r, exact.map { it * 1.3 }) >= 75)
            val steps = exact.chunked(4).flatMap { c -> List(c.size) { c.average() } }; assertTrue("${r.id} ${Ghost.score(r, steps)}", Ghost.score(r, steps) >= 85)
            // wer überall die richtige Seite trifft, bekommt mindestens das erste Lob – und dann stimmt „die Richtung passt“ auch
            for (s in 50..100) if (Ghost.verdict(s).contains("Richtung passt")) assertTrue(s in 50..74)
        }
    }

    @Test fun ringRunsAreSolvedByRingMinusStart() {
        (1L..40L).forEach { seed -> val r = RingRun(seed)
            assertEquals(3, r.hits(r.d)); assertEquals(r.d, r.rings[0] - r.a); assertEquals("Alle drei Ringe! Richtung = Ring minus Start.", r.verdict(r.d))
            assertTrue(r.rings.all { it.x in 0.0..8.0 && it.y in 0.0..8.0 && it.z in 1.0..6.0 })
            assertEquals(0, r.hits(r.d * -1.0)); assertEquals(0, r.hits(V3(0, 0, 0)))                    // rückwärts oder gar nicht: nichts
            assertEquals(3, r.hits(r.d * 2.0))                                                            // doppelt so schnell, gleiche Bahn
            assertTrue(r.scene(r.d).contains("\"color\":\"green\"")); assertTrue(r.tMax(r.d) >= 3.0)
        }
        val r = RingRun(5); val off = r.d + V3(0, 0, 1)
        assertTrue(r.hits(off) < 3); assertTrue(r.verdict(off).contains("verfehlt um"))
        assertNotEquals(RingRun(1).rings, RingRun(2).rings)
    }
}
