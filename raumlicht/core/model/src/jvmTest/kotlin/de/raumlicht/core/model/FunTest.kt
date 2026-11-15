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
        assertEquals(BalloonScore(points = 9, combo = 0, bestCombo = 6, hits = 6), s.hit(BalloonKind.PINK).miss().copy(points = 9, hits = 6))   // Fehlschuss: nur die Serie ist weg
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

    @Test fun ownFlightFromOwnLinePlanesAndSphere() {
        val f = Flights.custom("0,0,1;2,2,1;1,0,0,4;1,1,1,16;4,4,3,2").getOrThrow()
        assertEquals("x⃗ = (0|0|1) + t · (2|2|1)", f.text); assertEquals(4.0, f.tMax, 1e-9)                       // verlässt den Raum bei (8|8|5)
        assertEquals(listOf("Durchstoßpunkt mit E₁: x₁ = 4", "Durchstoßpunkt mit E₂: x₁ + x₂ + x₃ = 16"), f.events.filter { it.title.startsWith("Durch") }.map { it.title })
        assertEquals(2.0, f.events.first { it.title.contains("E₁") }.t, 1e-9); assertEquals(3.0, f.events.first { it.title.contains("E₂") }.t, 1e-9)
        assertEquals(2, f.events.count { it.title.contains("K") }); assertTrue(f.scene().contains("\"type\":\"sphere\""))
        assertEquals(Flights.EXAMPLE.let { Flights.custom(it).getOrThrow().planes.size }, 1)
        val bare = Flights.custom("1,1,1;1,0,0;;;").getOrThrow(); assertTrue(bare.planes.isEmpty() && bare.events.isEmpty()); assertEquals(7.0, bare.tMax, 1e-9)
        fun err(c: String) = Flights.custom(c).exceptionOrNull()!!.message!!
        assertTrue(err("0,0,1;0,0,0;;;").contains("Richtung darf nicht")); assertTrue(err("9,0,1;1,0,0;;;").contains("im Raum liegen")); assertTrue(err("0,0,1;-1,0,0;;;").contains("sofort aus dem Raum"))
        assertTrue(err("0,0,1;1,1,0;0,0,0,3;;").contains("Normalenvektor")); assertTrue(err("0,0,1;1,1;;;").contains("3 ganze Zahlen")); assertTrue(err("0,0,1;1,1,0;;;4,4,3,9").contains("Radius"))
    }

    @Test fun galtonFitsTheStageForAnyRowCount() {
        Galton.ROWS.forEach { n -> val g = Galton(n); assertEquals(n + 1, g.pmf(0.3).size); assertEquals(1.0, g.pmf(0.3).sum(), 1e-9); assertEquals(n * 0.3, g.mean(0.3), 1e-9)
            val (bin, path) = g.drop(1.0, kotlin.random.Random(1)); assertEquals(n, bin)                                  // immer rechts: letztes Fach
            path.forEach { assertTrue("$n $it", it.y in 0.5..7.6 && it.z in 0.2..6.6) }
            assertTrue(g.scene(List(n + 1) { it }, 0.5).contains("nägel$n")) }
    }

    @Test fun chanceRunsFollowTheLawOfLargeNumbers() {
        val r = ChanceRun(1.0 / 6, 10_000, 4); assertEquals(r.total, ChanceRun(1.0 / 6, 10_000, 4).total)
        assertEquals(1.0 / 6, r.freq(10_000), 0.012); assertTrue(abs(r.total - r.mean) < 4 * r.sigma)                     // viele Versuche: nah an p
        assertTrue((1..10_000).all { r.hits[it] - r.hits[it - 1] in 0..1 }); assertEquals(0.0, r.freq(0), 0.0)
        assertTrue(r.verdict(r.total).startsWith("Volltreffer")); assertTrue(r.verdict(r.total + 5000).contains("näher an n · p"))
        assertEquals(0, ChanceRun(0.0, 50, 1).total); assertEquals(50, ChanceRun(1.0, 50, 1).total)
        assertEquals(listOf(0.5, 1.0 / 6, 0.25), ChanceLab.presets.map { it.p })
    }

    @Test fun quizBalloonsCarryNumbersAndDuelHasTwoHalves() {
        val q = BalloonGame(3, mode = BalloonMode.QUIZ)
        assertEquals(3, q.rules.size); assertEquals(3, q.rules.map { it.text }.toSet().size); assertEquals(q.rules[1], q.ruleAt(20.0)); assertEquals(q.rules[2], q.ruleAt(99.0))
        assertTrue(q.balloons.none { it.kind == BalloonKind.GOLD || it.kind == BalloonKind.DUD })
        // in jedem Abschnitt gibt es passende und unpassende Zahlen – sonst wäre nichts zu entscheiden
        (0..2).forEach { k -> val rule = q.rules[k]; val ns = q.balloons.filter { ((it.born + 2.0) / 15).toInt().coerceAtMost(2) == k }.map { q.numbers[it.id] }
            assertTrue(rule.text, ns.any(rule.ok) && ns.any { !rule.ok(it) } && ns.all { it in rule.pool }) }
        val b = q.balloons[5]; assertTrue(q.scene(b.born + 0.5, emptySet()).contains("\"label\":\"${Geo.num(q.numbers[5])}\""))
        BalloonGame.RULES.forEach { r -> assertTrue(r.text, r.pool.any(r.ok) && r.pool.any { !r.ok(it) }) }
        assertEquals(listOf(2, 3), (0..6).filter(BalloonGame.RULES.first { it.text.startsWith("Nullstellen") }.ok)); assertEquals(listOf(2, 3, 5, 7, 11, 13), (1..14).filter(BalloonGame.RULES.first { it.text == "Primzahlen" }.ok))
        assertEquals(BalloonScore(points = 0, combo = 0), BalloonScore().wrong()); assertEquals(2, BalloonScore(points = 3, combo = 4).wrong().points)
        val d = BalloonGame(3, mode = BalloonMode.DUEL); assertEquals(0, d.side(V3(4.0, 3.9, 2.0))); assertEquals(1, d.side(V3(4.0, 4.0, 2.0)))
        assertTrue(d.scene(5.0, emptySet()).contains("\"id\":\"mitte\"") && !BalloonGame(3).scene(5.0, emptySet()).contains("mitte"))
        assertEquals(BalloonGame(3).balloons, BalloonGame(3, mode = BalloonMode.SOLO).balloons)
    }
}
