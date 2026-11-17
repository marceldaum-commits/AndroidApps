package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class PlayTest {
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)

    @Test fun buildLevelsHaveExactlyOneSolution() {
        // Jede „Funktionen bauen“-Aufgabe: Unter allen Reglerstellungen führt genau eine Bahn durch alle Tore
        Levels.chapters.first { it.number == 15 }.levels.forEach { l ->
            fun all(i: Int): List<Values> = if (i == l.controls.size) listOf(emptyMap()) else (l.controls[i].min[0]..l.controls[i].max[0]).flatMap { v -> all(i + 1).map { it + (l.controls[i].key to listOf(v)) } }
            val hits = all(0).filter { l.evaluate(it).success }
            assertEquals(l.id, listOf(l.solution), hits)
            assertTrue(l.finale!!(l.solution).path.size > 15)
        }
        assertTrue(Levels.byId("15-1")!!.evaluate(mapOf("m" to listOf(1), "b" to listOf(1))).message.contains("1 von 2 Toren"))
    }

    @Test fun newStochasticsAndAnalysisAreRight() {
        assertEquals(0.32, 2 * 0.2 * 0.8, 1e-12); assertEquals(42, 100 - (40 + 30 - 12)); assertEquals(0.3, 12.0 / 40, 1e-12); assertEquals(5.0, Math.sqrt(100 * 0.5 * 0.5), 1e-12)
        val g = Galton(4); assertEquals(6.0, g.pmf(0.5)[2] * 16, 1e-9)                         // 4 über 2 = 6
        fun d(f: (Double) -> Double, x: Double) = (f(x + 1e-5) - f(x - 1e-5)) / 2e-5
        assertEquals(6.0, d({ it * it }, 3.0), 1e-6); assertEquals(12.0, d({ (2 * it + 1) * (2 * it + 1) }, 1.0), 1e-5)
        assertTrue((0..4).all { it == 2 || it * (8 - 2 * it) < 8 }); assertEquals(800.0, 100 * Math.pow(2.0, 3.0), 0.0)
        // Fehler bekommen Namen – und landen damit im Fehler-Tagebuch
        val msg = Levels.byId("17-1")!!.evaluate(mapOf("a" to listOf(9))).message
        assertTrue(msg.contains("f(3), die Höhe")); assertTrue(MistakeBook.isNamed(msg))
        assertTrue(MistakeBook.isNamed(Levels.byId("2-1")!!.evaluate(mapOf("v" to listOf(-3, -4, 0))).message))
        assertFalse(MistakeBook.isNamed(Levels.byId("1-1")!!.evaluate(mapOf("P" to listOf(4, 0, 0))).message))
        // Die neuen Abi-Aufgaben
        assertEquals(listOf("Festivalzelt", "Laser & Leinwand", "Drohnen-Show", "Lichtbogen", "Lostrommel", "Planetarium", "Einlass"), Exams.all.map { it.title })
    }

    @Test fun galtonBoardFollowsTheBinomialDistribution() {
        val g = Galton(); val p = 0.5
        assertEquals(1.0, g.pmf(p).sum(), 1e-9); assertEquals(4.0, g.mean(p), 1e-9); assertEquals(Math.sqrt(2.0), g.sigma(p), 1e-9)
        assertEquals(1.0, Galton().pmf(0.3).sum(), 1e-9); assertTrue(Galton().pmf(0.3).let { it.indexOf(it.max()) } == 2)
        val r = Random(7); val counts = IntArray(9)
        repeat(4000) { val (bin, path) = g.drop(p, r); counts[bin]++; assertEquals(g.rows + 3, path.size); assertTrue(path.all { it.y in 0.0..8.0 && it.z in 0.0..6.6 }) }
        g.pmf(p).forEachIndexed { k, e -> assertEquals("Fach $k", e, counts[k] / 4000.0, 0.03) }
        val scene = g.scene(counts.toList(), p, V3(4.0, 4.0, 3.0))
        assertTrue(scene.contains("\"id\":\"erwartung\"") && scene.contains("\"id\":\"kugel\"")); assertFalse(g.scene(List(9) { 0 }, p).contains("balken"))
    }

    @Test fun flightsReportWhatHappensOnTheWay() {
        val f = Flights.byId("glas")!!
        assertEquals(listOf(2.0, 3.0), f.events.map { it.t }); assertTrue(f.events[0].text.contains("S(4|4|3)")); assertEquals("S(6|6|4)", f.events[1].text.substringAfter("→ "))
        val start = f.data(0.0); assertTrue(start.pos.near(p(0, 0, 1))); assertEquals(4.0, start.distance, 1e-9); assertEquals("E₁: x₁ = 4", start.nearest)
        assertEquals(0.0, f.data(0.5).distance, 1e-9); assertNotNull(f.eventNear(2.03)); assertNull(f.eventNear(1.5))
        // Kugel: Ein- und Austritt sind die Lösungen der quadratischen Gleichung aus Kapitel 9
        val k = Flights.byId("kugel")!!
        assertEquals(listOf("Eintritt in K", "Durchstoßpunkt mit E: x₃ = 4", "Austritt aus K"), k.events.map { it.title }); assertEquals(listOf(1.0, 3.0, 3.0), k.events.map { it.t })
        // E3: Durchstoßpunkt und Austritt liegen beide bei t = 3 in (2|6|4) – beide werden gemeldet
        assertEquals(listOf("Durchstoßpunkt mit E: x₃ = 4", "Austritt aus K"), k.eventsNear(3.01).map { it.title })
        k.eventNear(2.98)!!.let { assertEquals("Durchstoßpunkt mit E: x₃ = 4 und Austritt aus K", it.title); assertEquals("t = 3 → S(2|6|4)", it.text); assertEquals(3.0, it.t, 0.0) }
        assertEquals("Eintritt in K", k.eventNear(1.0)!!.title); assertEquals(k.events[0], k.eventNear(1.0)); assertNull(k.eventNear(2.0)); assertEquals(emptyList<FlightEvent>(), k.eventsNear(2.0))
        // E4: Der Flug „Schräg durch den Raum“ endet auf dem Boden x₃ = 0 in (6|2|0); die Ebene x₃ = 1 heißt nicht „Boden“
        val s = Flights.byId("schraeg")!!
        assertTrue(s.at(1.0).near(p(6, 2, 0))); assertEquals(listOf("E₁: x₂ = 4", "E₂: x₁ + 2x₃ = 9", "E₃: x₃ = 1"), s.planes.map { it.first })
        assertEquals(listOf(2.0 to "S(3|5|3)", 3.0 to "S(4|4|2)", 4.0 to "S(5|3|1)"), s.events.map { it.t to it.text.substringAfter("→ ") })
        s.planes.forEach { (name, n, c) -> val e = s.events.single { it.title.endsWith(name) }; assertEquals(name, c.toDouble(), n dot (s.a + s.d * e.t), 1e-9) }
        Flights.all.forEach { fl -> assertTrue(fl.planes.none { it.first.contains("Boden") }) }
        Flights.all.forEach { fl -> assertTrue(fl.id, (0..10).all { fl.at(it / 10.0).let { q -> q.x in 0.0..8.0 && q.y in 0.0..8.0 && q.z in 0.0..6.0 } }); assertTrue(fl.scene().contains("\"style\":\"line\"")); assertTrue(fl.events.isNotEmpty()) }
    }

    @Test fun coasterExtrasDerivativeAndIntegral() {
        val r = Coaster.byId("huegel")!!                                   // −x² + 4x
        assertEquals(32.0 / 3, r.integral(4.0), 1e-3); assertEquals(0.0, r.integral(0.0), 1e-9)
        val wave = Coaster.byId("welle")!!                                 // 2·sin(x): eine volle Periode hebt sich auf
        assertEquals(0.0, wave.integral(2 * Math.PI), 1e-3); assertEquals(4.0, wave.integral(Math.PI), 1e-3)
        val plain = r.scene(); val both = r.scene(2.0, derivative = true, fill = true)
        assertFalse(plain.contains("ableitung")); assertTrue(both.contains("\"id\":\"ableitung\"") && both.contains("\"id\":\"punkt-ableitung\"") && both.contains("\"id\":\"plus"))
        assertTrue(wave.scene(5.0, derivative = false, fill = true).contains("\"id\":\"minus"))     // unter der Achse: orange
        assertFalse(r.scene(0.0, derivative = false, fill = true).contains("plus"))
    }

    @Test fun quickCalendarAndLooks() {
        val pick = Quick.pick(3); assertEquals(Quick.TASKS, pick.size); assertTrue(pick.none { it.abi }); assertEquals(pick.map { it.id }, Quick.pick(3).map { it.id })
        assertEquals(listOf(Area.GEOMETRIE, Area.GEOMETRIE, Area.ANALYSIS, Area.ANALYSIS, Area.STOCHASTIK, Area.STOCHASTIK), pick.map { l -> Levels.chapters.first { it.number == l.chapter }.area })
        assertEquals(setOf(20_770L, 20_771L), Calendar.solved(mapOf("tag-20770" to 3, "tag-20771" to 1, "1-1" to 3)))
        val grid = Calendar.grid(20_772); assertEquals(5, grid.size); assertTrue(grid.all { it.size == 7 })
        assertEquals(java.time.DayOfWeek.MONDAY, java.time.LocalDate.ofEpochDay(grid[0][0]!!).dayOfWeek)
        assertTrue(grid.last().contains(20_772L)); assertTrue(grid.flatten().filterNotNull().all { it <= 20_772 })
        assertEquals(3, Calendar.bestRun(setOf(1, 2, 3, 7, 8))); assertEquals(0, Calendar.bestRun(emptySet()))
        assertEquals(0, Looks.trails[0].badges); assertEquals(0, Looks.hats[0].badges)
    }

    /** E1: Ohne Ebene gibt es keinen Abstand. E2: Eine Tangente berührt die Kugel – ein Ereignis, nicht Ein- und Austritt an derselben Stelle. */
    @Test fun ownFlightWithoutPlaneAndTangent() {
        val bare = Flights.custom("0,0,1;2,2,1;;;").getOrThrow()
        for (i in 0..10) bare.data(i / 10.0).let { assertFalse(it.hasPlane); assertTrue(it.distance.isNaN()); assertEquals("–", it.nearest) }
        Flights.byId("glas")!!.data(0.5).let { assertTrue(it.hasPlane); assertEquals(0.0, it.distance, 1e-9) }
        val tangent = Flights.custom("0,0,3;1,0,0;;;4,2,3,2").getOrThrow()                       // Abstand der Geraden zum Mittelpunkt (4|2|3) ist genau 2 = r
        assertEquals(listOf("Berührt K"), tangent.events.map { it.title }); assertEquals(4.0, tangent.events[0].t, 1e-9); assertEquals("t = 4 → (4|0|3)", tangent.events[0].text)
        assertEquals(listOf("Eintritt in K", "Austritt aus K"), Flights.custom("0,1,3;1,0,0;;;4,2,3,2").getOrThrow().events.map { it.title })
        assertEquals(emptyList<String>(), Flights.custom("0,0,4;1,0,0;;;4,2,3,2").getOrThrow().events.map { it.title })       // knapp vorbei
        // schräge Tangente: Abstand von (1|1|4) zur Geraden durch (0|0|3) in Richtung (1|1|0) ist 1 = r
        assertEquals(listOf("Berührt K"), Flights.custom("0,0,3;1,1,0;;;1,1,4,1").getOrThrow().events.map { it.title })
    }

    /** F: Binomial-Labor – Streubereich mit echter Wahrscheinlichkeit, ausgeschriebene Ansätze am Rand, ehrliche Sätze für sehr kleine und sehr große Wahrscheinlichkeiten */
    @Test fun binomialTextsAreTrue() {
        val b = Binomial(10, 0.5)
        assertEquals(Triple(4, 6, 0.65625), b.spread()!!.let { Triple(it.first, it.second, Math.round(it.third * 1e6) / 1e6) }); assertEquals("Mit 66 % zwischen 4 und 6 Treffern (μ ± σ)", b.spreadText())
        val rare = Binomial(100, 0.01)                                                         // μ = 1, σ ≈ 0,99: nur die 1 liegt im Bereich – mit 37 %, nicht „meist“
        assertEquals("Mit 37 % genau 1 Treffer (μ ± σ)", rare.spreadText()); assertEquals(1 to 1, rare.spread()!!.let { it.first to it.second })
        assertEquals("Mit 73 % zwischen 45 und 55 Treffern (μ ± σ)", Binomial(100, 0.5).spreadText())
        for (n in listOf(1, 2, 3, 7, 10, 50, 100)) for (pc in 1..99) { val d = Binomial(n, pc / 100.0); val (lo, hi, v) = d.spread()!!
            assertTrue(lo in 0..n && hi in lo..n && lo >= d.mean - d.sigma - 1e-9 && hi <= d.mean + d.sigma + 1e-9); assertEquals((lo..hi).sumOf { d.exactly(it) }, v, 1e-9); assertFalse(d.spreadText().contains("−")) }
        // F2: kleine Summen werden ausgeschrieben
        assertEquals("P(X ≤ 0) = P(X = 0)", b.approach(BinomialMode.AT_MOST, 0)); assertEquals("P(X ≤ 1) = P(X = 0) + P(X = 1)  (kumuliert)", b.approach(BinomialMode.AT_MOST, 1))
        assertEquals("P(X ≤ 2) = P(X = 0) + P(X = 1) + P(X = 2)  (kumuliert)", b.approach(BinomialMode.AT_MOST, 2)); assertEquals("P(X ≤ 3) = P(X = 0) + … + P(X = 3)  (kumuliert)", b.approach(BinomialMode.AT_MOST, 3))
        assertEquals("P(X ≤ 10) = 1  (mehr als 10 Treffer gibt es nicht)", b.approach(BinomialMode.AT_MOST, 10))
        assertEquals("P(X ≥ 10) = P(X = 10)", b.approach(BinomialMode.AT_LEAST, 10)); assertEquals("P(X ≥ 9) = P(X = 9) + P(X = 10)", b.approach(BinomialMode.AT_LEAST, 9))
        assertEquals("P(X ≥ 8) = 1 − P(X ≤ 7)  (Gegenereignis)", b.approach(BinomialMode.AT_LEAST, 8)); assertEquals("P(X ≥ 0) = 1", b.approach(BinomialMode.AT_LEAST, 0))
        assertEquals(b.exactly(9) + b.exactly(10), b.atLeast(9), 1e-12); assertEquals(b.exactly(0) + b.exactly(1), b.atMost(1), 1e-12)
        // F3: genau 1 heißt „immer“, knapp darunter „fast immer“, knapp über 0 „selten“ – nie „etwa 0 von 100“
        assertEquals("Immer: Mindestens 0 Treffer gibt es sicher.", b.story(BinomialMode.AT_LEAST, 0)); assertEquals("Immer: Höchstens 10 Treffer gibt es sicher.", b.story(BinomialMode.AT_MOST, 10))
        assertEquals(0.99657, rare.atMost(4), 1e-5); assertEquals("Fast immer: In mehr als 99 von 100 Durchgängen gibt es höchstens 4 Treffer.", rare.story(BinomialMode.AT_MOST, 4))
        assertEquals(0.0029, rare.exactly(5), 1e-4); assertEquals("Selten: In weniger als 1 von 100 Durchgängen gibt es genau 5 Treffer.", rare.story(BinomialMode.EXACT, 5))
        assertEquals("Praktisch nie: Genau 10 Treffer gibt es so gut wie nie.", rare.story(BinomialMode.EXACT, 10)); assertEquals("Praktisch immer: Höchstens 9 Treffer gibt es so gut wie sicher.", rare.story(BinomialMode.AT_MOST, 9))
        assertEquals("In etwa 17 von 100 Durchgängen gibt es höchstens 3 Treffer.", b.story(BinomialMode.AT_MOST, 3))
        for (n in listOf(1, 5, 10, 100)) for (pc in listOf(1, 10, 50, 90, 99)) for (m in BinomialMode.entries) for (k in 0..n) { val d = Binomial(n, pc / 100.0); val t = d.story(m, k); val v = d.ask(m, k)
            assertFalse(t, t.contains("etwa 0 von 100") || t.contains("etwa 100 von 100")); assertEquals(t, d.certain(m, k), t.startsWith("Immer")); if (t.contains("so gut wie sicher")) assertTrue(v < 1.0 || !d.certain(m, k)) }
    }
}
