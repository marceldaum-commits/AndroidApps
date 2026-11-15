package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test

class CoasterTest {
    private fun spots(id: String) = Coaster.byId(id)!!.spots.map { it.kind to it.x }

    @Test fun specialSpotsAreFound() {
        assertEquals(listOf(SpotKind.HIGH to 2.0), spots("huegel"))
        assertEquals(listOf(SpotKind.ZERO to -1.73, SpotKind.HIGH to -1.0, SpotKind.TURN to 0.0, SpotKind.ZERO to 0.0, SpotKind.LOW to 1.0, SpotKind.ZERO to 1.73), spots("bergtal"))
        assertEquals(listOf(SpotKind.HIGH to 1.0, SpotKind.TURN to 2.0, SpotKind.LOW to 3.0, SpotKind.ZERO to 3.0), spots("doppel"))   // x(x − 3)²: bei 3 berührt die Bahn die Nulllinie
        assertEquals(listOf(SpotKind.LOW to 1.0, SpotKind.ZERO to 3.0), spots("schuessel"))
        val wave = spots("welle")                                  // 2·sin(x): Hochpunkt bei π/2, Wende- und Nullstelle bei π, Tiefpunkt bei 3π/2 …
        assertTrue(wave.contains(SpotKind.HIGH to 1.57)); assertTrue(wave.contains(SpotKind.TURN to 3.14)); assertTrue(wave.contains(SpotKind.LOW to 4.71)); assertTrue(wave.contains(SpotKind.ZERO to 6.28))
    }

    @Test fun rideDataMatchesTheDerivatives() {
        val r = Coaster.byId("huegel")!!
        val start = r.data(0.0); assertEquals(0.0, start.x, 0.0); assertEquals(4.0, start.slope, 1e-9); assertEquals("bergauf", start.slopeWord); assertEquals(400.0, start.percent, 1e-9)
        assertEquals(75.96, start.angle, 0.01); assertEquals("Rechtskurve (f″ < 0)", start.bendWord); assertEquals("↗", start.arrow)
        val top = r.data(0.5); assertEquals(4.0, top.height, 1e-9); assertEquals("waagerecht", top.slopeWord); assertEquals("→", top.arrow)
        assertEquals("bergab", r.data(0.9).slopeWord)
        assertEquals(SpotKind.HIGH, r.spotNear(2.03)!!.kind); assertNull(r.spotNear(2.5)); assertEquals(0.5, r.u(2.0), 1e-9)
        // Ableitungen stimmen mit dem Differenzenquotienten überein – für alle Bahnen
        Coaster.rides.forEach { ride -> for (i in 1..9) { val x = ride.x(i / 10.0)
            assertEquals(ride.id, (ride.f(x + 1e-5) - ride.f(x - 1e-5)) / 2e-5, ride.df(x), 1e-4); assertEquals(ride.id, (ride.df(x + 1e-5) - ride.df(x - 1e-5)) / 2e-5, ride.ddf(x), 1e-4) } }
    }

    @Test fun tracksStayInsideTheRoom() = Coaster.rides.forEach { r ->
        r.points.forEach { p -> assertTrue("${r.id}: $p", p.y in 0.0..8.0 && p.z in 0.15..6.0) }
        assertTrue(r.scene().contains("\"type\":\"track\"")); assertEquals(Ride.SAMPLES + 1, r.points.size)
        assertEquals(Coaster.rides.size, Coaster.rides.map { it.id }.toSet().size)
    }
}
