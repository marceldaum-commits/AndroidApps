package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test

class CoasterTest {
    private fun spots(id: String) = Coaster.byId(id)!!.spots.map { it.kind to it.x }

    @Test fun specialSpotsAreFound() {
        // Nullstellen genau am Rand des Abschnitts zählen mit (A7) – Extrem- und Wendestellen am Rand nicht
        assertEquals(listOf(SpotKind.ZERO to 0.0, SpotKind.HIGH to 2.0, SpotKind.ZERO to 4.0), spots("huegel"))
        assertEquals(listOf(SpotKind.ZERO to -1.73, SpotKind.HIGH to -1.0, SpotKind.TURN to 0.0, SpotKind.ZERO to 0.0, SpotKind.LOW to 1.0, SpotKind.ZERO to 1.73), spots("bergtal"))
        assertEquals(listOf(SpotKind.ZERO to 0.0, SpotKind.HIGH to 1.0, SpotKind.TURN to 2.0, SpotKind.LOW to 3.0, SpotKind.ZERO to 3.0), spots("doppel"))   // x(x − 3)²: bei 3 berührt die Bahn die Nulllinie
        assertEquals(listOf(SpotKind.ZERO to -1.0, SpotKind.LOW to 1.0, SpotKind.ZERO to 3.0), spots("schuessel"))
        assertEquals(SpotKind.ZERO to 0.0, spots("welle").first()); assertEquals(SpotKind.ZERO to 0.0, spots("ebuckel").first())
        assertEquals(listOf(SpotKind.ZERO to 1.0), spots("ln"))
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

    /** A8: Fallen Stellen zusammen, werden alle genannt – nicht nur die erste */
    @Test fun coincidingSpotsAreAllReported() {
        fun near(id: String, x: Double) = Coaster.byId(id)!!.spotsNear(x).map { it.kind }
        assertEquals(listOf(SpotKind.LOW, SpotKind.ZERO), near("doppel", 3.02)); assertEquals(listOf(SpotKind.TURN, SpotKind.ZERO), near("bergtal", 0.01))
        assertEquals(listOf(SpotKind.TURN, SpotKind.ZERO), near("welle", 3.15)); assertEquals(listOf(SpotKind.TURN, SpotKind.ZERO), near("welle", 6.27))
        assertEquals(listOf(SpotKind.HIGH), near("doppel", 1.0)); assertEquals(emptyList<SpotKind>(), near("doppel", 1.5))
        assertEquals(SpotKind.LOW, Coaster.byId("doppel")!!.spotNear(3.0)!!.kind)
        // zwei verschiedene Stellen dicht beieinander werden nicht zusammengeworfen: nur die nächste
        val r = Coaster.custom(Expr.parse("(x-1)(x-1,04)").getOrThrow(), 0.0, 2.0)!!
        assertEquals(listOf(SpotKind.ZERO to 1.0), r.spotsNear(1.005).map { it.kind to it.x })
    }

    /** A5: „Wendestelle“ steht nur dort, wo die Bahn wirklich eine hat. A12: Der Regeltext nennt den Vorzeichenwechsel. */
    @Test fun bendWordNamesATurnOnlyWhereThereIsOne() {
        val ln = Coaster.byId("ln")!!
        for (i in 0..200) assertFalse(ln.data(i / 200.0).bendWord.contains("Wende"))
        assertEquals("fast gerade", ln.data(ln.u(5.0)).bendWord); assertEquals("Rechtskurve (f″ < 0)", ln.data(ln.u(1.0)).bendWord)
        val bt = Coaster.byId("bergtal")!!
        assertEquals("keine Krümmung (Wendestelle)", bt.data(0.5).bendWord); assertTrue(bt.data(0.5).turn); assertEquals("Linkskurve (f″ > 0)", bt.data(0.75).bendWord)
        val line = Coaster.custom(Expr.parse("2x-4").getOrThrow(), 0.0, 4.0)!!
        for (i in 0..50) assertEquals("gerade (keine Krümmung)", line.data(i / 50.0).bendWord)
        val x5 = Coaster.custom(Expr.parse("x^5").getOrThrow(), -2.0, 2.0)!!                     // flach um 0: Wendestelle nur direkt bei 0
        assertEquals("keine Krümmung (Wendestelle)", x5.data(0.5).bendWord); assertEquals("fast gerade", x5.data(x5.u(0.1)).bendWord)
        assertTrue(SpotKind.TURN.rule.startsWith("f″ = 0 und f″ wechselt das Vorzeichen"))
        assertEquals(SpotKind.HIGH.rule, Spot(SpotKind.HIGH, 1.0).rule); assertEquals("eigener Text", Spot(SpotKind.HIGH, 1.0, "eigener Text").rule)
    }
}
