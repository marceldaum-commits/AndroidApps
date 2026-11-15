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

    /** f, f′ und f″ stehen unverzerrt am selben Achsenkreuz: f′ kreuzt die x-Achse unter jedem Extremum, f″ unter jedem Wendepunkt. */
    @Test fun derivativesShareOneTrueAxis() {
        (Coaster.rides + Coaster.custom(Expr.parse("e^(-0,3x)*sin(3x)").getOrThrow(), 0.0, 8.0)!! + Coaster.custom(Expr.parse("x^4-4x^2").getOrThrow(), -2.0, 2.0)!!).forEach { r ->
            val o = Json.parseToJsonElement(r.scene(r.from, derivative = true, fill = false, second = true)).jsonObject.getValue("objects").jsonArray.map { it.jsonObject }
            fun id(e: JsonObject) = e["id"]!!.jsonPrimitive.content
            val axes = o.first { it["type"]!!.jsonPrimitive.content == "axes2d" }
            val oz = axes["p"]!!.jsonArray[2].jsonPrimitive.double; val ox = axes["p"]!!.jsonArray[1].jsonPrimitive.double
            val sx = axes["d"]!!.jsonArray[0].jsonPrimitive.double; val sy = axes["d"]!!.jsonArray[1].jsonPrimitive.double
            fun segs(name: String) = o.first { id(it) == name }["segments"]!!.jsonArray.map { s -> s.jsonArray.map { p -> p.jsonArray.map { it.jsonPrimitive.double } } }
            // jede Kurve liegt genau dort, wo das Achsenkreuz ihren Wert anzeigt – ohne Stauchung
            listOf("ableitung" to r.df, "zweite" to r.ddf).forEach { (name, g) ->
                val s = segs(name); assertTrue("${r.id} $name", s.size >= 90)
                s.forEach { (a, _) -> assertEquals("${r.id} $name", g((a[1] - ox) / sx), (a[2] - oz) / sy, 1e-6); assertTrue(a[2] in 0.05..6.8) }
            }
            val track = o.first { id(it) == "bahn" }["corners"]!!.jsonArray.map { p -> p.jsonArray.map { it.jsonPrimitive.double } }
            track.forEach { p -> assertEquals(r.id, r.f((p[1] - ox) / sx), (p[2] - oz) / sy, 1e-6); assertTrue(r.id, p[2] in 0.3..6.5) }
            fun crosses(name: String, x: Double): Boolean { val y = ox + x * sx; val seg = segs(name).minBy { abs((it[0][1] + it[1][1]) / 2 - y) }
                return minOf(seg[0][2], seg[1][2]) - 0.03 <= oz && oz <= maxOf(seg[0][2], seg[1][2]) + 0.03 }
            val ex = r.spots.filter { it.kind == SpotKind.HIGH || it.kind == SpotKind.LOW }; val turns = r.spots.filter { it.kind == SpotKind.TURN }
            ex.forEach { assertTrue("${r.id} f′ bei ${it.x}", crosses("ableitung", it.x)) }; turns.forEach { assertTrue("${r.id} f″ bei ${it.x}", crosses("zweite", it.x)) }
            assertEquals(r.id, ex.take(12).size, o.count { id(it).startsWith("lot-ableitung") }); assertEquals(r.id, turns.take(12).size, o.count { id(it).startsWith("lot-zweite") })
        }
        // ohne Zusätze bleibt die Bühne wie sie war
        val h = Coaster.byId("huegel")!!; assertEquals(h.scene(), h.scene(1.0, derivative = false, fill = false))
    }
}
