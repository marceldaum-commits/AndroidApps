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
        assertEquals(listOf("Festivalzelt", "Laser & Leinwand", "Drohnen-Show", "Lichtbogen", "Lostrommel"), Exams.all.map { it.title })
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
        Flights.all.forEach { fl -> assertTrue(fl.id, (0..10).all { fl.at(it / 10.0).let { q -> q.x in 0.0..8.0 && q.y in 0.0..8.0 && q.z in 0.0..6.0 } }); assertTrue(fl.scene().contains("\"style\":\"line\"")); assertTrue(fl.events.isNotEmpty()) }
    }

    @Test fun coasterExtrasDerivativeAndIntegral() {
        val r = Coaster.byId("huegel")!!                                   // −x² + 4x
        assertEquals(32.0 / 3, r.integral(4.0), 1e-3); assertEquals(0.0, r.integral(0.0), 1e-9)
        val wave = Coaster.byId("welle")!!                                 // 2·sin(x): eine volle Periode hebt sich auf
        assertEquals(0.0, wave.integral(2 * Math.PI), 1e-3); assertEquals(4.0, wave.integral(Math.PI), 1e-3)
        val plain = r.scene(); val both = r.scene(2.0, derivative = true, fill = true)
        assertFalse(plain.contains("ableitung")); assertTrue(both.contains("\"id\":\"ableitung\"") && both.contains("\"id\":\"dpunkt\"") && both.contains("\"id\":\"plus"))
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
}
