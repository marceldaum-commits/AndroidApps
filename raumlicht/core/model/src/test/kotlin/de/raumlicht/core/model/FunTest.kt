package de.raumlicht.core.model

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class FunTest {
    @Test fun balloonRoundIsFixedBySeedAndGetsDenser() {
        val g = BalloonGame(7); assertEquals(g.balloons, BalloonGame(7).balloons); assertNotEquals(g.balloons, BalloonGame(8).balloons)
        assertTrue(g.balloons.size in 60..130); assertTrue(g.balloons.all { it.born < g.duration && it.x in 1.3..6.7 })
        assertTrue(g.balloons.count { it.born < 15 } < g.balloons.count { it.born >= 30 })                 // am Ende mehr los
        assertTrue(g.balloons.any { it.kind == BalloonKind.GOLD } && g.balloons.any { it.kind == BalloonKind.DUD })
        val b = g.balloons.first(); assertEquals(listOf(b), g.alive(b.born + 0.1, emptySet())); assertEquals(emptyList<Balloon>(), g.alive(b.born + 0.1, setOf(b.id)))
        assertTrue(g.alive(b.born + 30, emptySet()).none { it.id == b.id })                                  // längst oben hinaus
        assertEquals(b, g.byId("b${b.id}")); assertNull(g.byId("lux"))
        val s = g.scene(10.0, emptySet()); assertTrue(s.contains("\"plain\":true") && s.contains("\"type\":\"sphere\""))   // Showbühne, kein Koordinatensystem
    }

    @Test fun combosPayAndDudsCost() {
        var s = BalloonScore(); repeat(5) { s = s.hit(BalloonKind.PINK) }
        assertEquals(5, s.points); assertEquals(5, s.combo); assertEquals(1, s.bonus)
        s = s.hit(BalloonKind.GOLD); assertEquals(11, s.points)                                             // 5 + 1 Serienbonus
        s = s.hit(BalloonKind.DUD); assertEquals(9, s.points); assertEquals(0, s.combo); assertEquals(6, s.bestCombo); assertEquals(6, s.hits)
        assertEquals(0, BalloonScore().hit(BalloonKind.DUD).points)                                          // nie unter null
    }

    /** f′ und f teilen sich die x-Achse: unter jedem Hoch- und Tiefpunkt geht die Ableitungsbahn genau durch die Achse. */
    @Test fun derivativeTrackCrossesTheAxisBelowEveryExtremum() {
        (Coaster.rides + Coaster.custom(Expr.parse("e^(-0,3x)*sin(3x)").getOrThrow(), 0.0, 8.0)!! + Coaster.custom(Expr.parse("x^4-4x^2").getOrThrow(), -2.0, 2.0)!!).forEach { r ->
            val o = Json.parseToJsonElement(r.scene(r.from, derivative = true, fill = false)).jsonObject.getValue("objects").jsonArray.map { it.jsonObject }
            val axisZ = o.first { it["type"]!!.jsonPrimitive.content == "axes2d" }["p"]!!.jsonArray[2].jsonPrimitive.double
            assertTrue(r.id, o.none { it["id"]!!.jsonPrimitive.content == "dnull" })                         // keine zweite, verschobene Nulllinie mehr
            val segs = o.first { it["id"]!!.jsonPrimitive.content == "ableitung" }["segments"]!!.jsonArray.map { s -> s.jsonArray.map { p -> p.jsonArray.map { it.jsonPrimitive.double } } }
            assertTrue(r.id, segs.size >= 90 && segs.all { s -> s.all { it[2] in 0.05..6.8 } })               // ganz im Bild, nichts plattgedrückt
            val track = o.first { it["id"]!!.jsonPrimitive.content == "bahn" }["corners"]!!.jsonArray.map { p -> p.jsonArray.map { it.jsonPrimitive.double } }
            assertTrue(r.id, track.all { it[2] in 0.3..6.5 })
            val ex = r.spots.filter { it.kind == SpotKind.HIGH || it.kind == SpotKind.LOW }; assertTrue(r.id, ex.isNotEmpty())
            ex.forEach { s ->
                val y = track.first()[1] + (s.x - r.from) / (r.to - r.from) * (track.last()[1] - track.first()[1])     // Bühnen-Ort der Stelle
                val seg = segs.minBy { abs((it[0][1] + it[1][1]) / 2 - y) }
                assertTrue("${r.id} x=${s.x}", minOf(seg[0][2], seg[1][2]) - 0.02 <= axisZ && axisZ <= maxOf(seg[0][2], seg[1][2]) + 0.02)   // f′ kreuzt dort die Achse
            }
            assertEquals(r.id, ex.take(12).size, o.count { it["id"]!!.jsonPrimitive.content.startsWith("lot") })
        }
        assertEquals(1.0, Coaster.byId("huegel")!!.derivativeScale, 1e-9)                                    // f′ = −2x + 4 passt unverzerrt
    }
}
