package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test

class ExtrasTest {
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)
    private fun solvable(l: Level) { assertTrue(l.id, l.evaluate(l.solution).success); assertFalse(l.id, l.evaluate(l.start).success)
        l.controls.forEach { c -> listOf(l.solution.getValue(c.key), c.start).forEach { v -> v.forEachIndexed { i, x -> assertTrue("${l.id}/${c.key}", x in c.min[i]..c.max[i]) } } }
        Phase.entries.forEach { ph -> val o = l.objects(if (ph == Phase.DONE) l.solution else l.start, ph); assertEquals(l.id, o.size, o.map { it.id }.toSet().size) } }

    @Test fun typicalMistakesGetNamed() {
        fun msg(id: String, key: String, vararg v: Int) = Levels.byId(id)!!.evaluate(mapOf(key to v.toList())).message
        assertTrue(msg("1-4", "P", 8, 4, 4).contains("durch 2 teilen"))                 // addiert, nicht halbiert
        assertTrue(msg("2-1", "v", -3, -4, 0).contains("A − B"))                         // falsch herum abgezogen
        assertTrue(msg("2-1", "v", 4, 5, 0).contains("Ortsvektor"))                      // A nicht abgezogen
        assertTrue(msg("3-2", "d", 2, 5, 4).contains("zieh A ab"))                       // Richtung vom Ursprung
        assertTrue(msg("6-3", "P", 4, 4, 3).contains("Lotfußpunkt"))                     // auf halbem Weg stehen geblieben
        assertTrue(msg("7-5", "a", 4).contains("Sinus"))                                 // Kosinus statt Sinus (60° statt 30°)
        assertTrue(msg("9-3", "P", 2, 6, 4).contains("Austrittspunkt"))                  // zweite Lösung der quadratischen Gleichung
        assertTrue(msg("10-2", "a", 18).contains("Hälfte")); assertTrue(msg("10-3", "a", 18).contains("⅓"))
        assertTrue(msg("8-3", "a", 9).contains("Normalenvektors"))
        assertTrue(msg("14-3", "a", -1).contains("Hochpunkt")); assertTrue(msg("13-5", "a", 20).contains("Gegenereignis"))
        // Ein beliebiger anderer Fehler bekommt weiter die allgemeine Rückmeldung
        assertEquals("x₁ stimmt ✓ – x₂ und x₃ noch nicht.", msg("1-4", "P", 4, 0, 0))
    }

    @Test fun newChaptersAreMathematicallyRight() {
        // Stochastik
        assertEquals(0.25, 0.5 * 0.5, 0.0); assertEquals(0.10, 2.0 / 5 * (1.0 / 4), 1e-12); assertEquals(0.75, 1 - 0.25, 0.0); assertEquals(2.0, 20 * 0.1, 1e-12)
        // Analysis: Nullstelle, Extrema, Wendestelle, Fläche (numerisch)
        fun f1(x: Double) = x * x - 2 * x - 3
        assertEquals(0.0, f1(3.0), 0.0)
        fun d(f: (Double) -> Double, x: Double) = (f(x + 1e-5) - f(x - 1e-5)) / 2e-5
        fun d2(f: (Double) -> Double, x: Double) = (f(x + 1e-3) - 2 * f(x) + f(x - 1e-3)) / 1e-6
        val f2 = { x: Double -> -x * x + 4 * x }; assertEquals(0.0, d(f2, 2.0), 1e-6); assertTrue(d2(f2, 2.0) < 0)
        val f3 = { x: Double -> x * x * x - 3 * x }; assertEquals(0.0, d(f3, 1.0), 1e-6); assertTrue(d2(f3, 1.0) > 0); assertTrue(d2(f3, -1.0) < 0)
        val f4 = { x: Double -> x * x * x - 6 * x * x + 9 * x }; assertEquals(0.0, d2(f4, 2.0), 1e-3)
        val area = (0 until 20000).sumOf { val x = 2.0 * (it + 0.5) / 20000; -3 * x * x + 6 * x } * 2.0 / 20000; assertEquals(4.0, area, 1e-6)
        // Lux fährt auf der Kurve mit
        val l = Levels.byId("14-2")!!
        val at0 = l.objects(mapOf("a" to listOf(0)), Phase.AIM).first { it.type == "drone" }.p!!; val at2 = l.objects(mapOf("a" to listOf(2)), Phase.AIM).first { it.type == "drone" }.p!!
        assertTrue(at2[2] > at0[2] + 3)
    }

    @Test fun guidesAreSolvableStepByStep() {
        assertEquals(4, Guides.all.size)
        Guides.all.forEach { g -> g.parts.forEach { solvable(it.level) }; assertTrue(g.parts.size in 2..3) }
        val step = Guides.byId("weg-durchstoss")!!.parts[0].level
        assertEquals("Mit t = 1 ergibt die linke Seite 8 statt 13.", step.evaluate(mapOf("a" to listOf(1))).message)
        // Die Schritte passen zusammen: t aus Schritt 1 liefert den Punkt aus Schritt 2
        assertTrue((p(1, 0, 1) + p(1, 2, 1) * 2).near(p(3, 4, 3))); assertTrue(Geo.onPlane(p(3, 4, 3), p(1, 1, 2), 13.0))
        assertTrue(Geo.mirrorAtPlane(p(1, 1, 0), p(1, 2, 2), 12.0).near(p(3, 5, 4))); assertTrue(Geo.footOnPlane(p(1, 1, 0), p(1, 2, 2), 12.0).near(p(2, 3, 2)))
    }

    @Test fun learnStateFromStars() {
        val none = Learn.topics(emptyMap()); assertTrue(none.all { it.grip == Grip.OFFEN }); assertEquals(Levels.chapters.size, none.size)
        val ch1 = Levels.chapters[0].levels
        assertEquals(Grip.SICHER, Learn.topics(ch1.associate { it.id to 3 })[0].grip)
        assertEquals(Grip.WACKELIG, Learn.topics(ch1.associate { it.id to 1 })[0].grip)
        assertEquals(Grip.WACKELIG, Learn.topics(mapOf("1-1" to 3))[0].grip)
        assertEquals("1 sicher · 0 wackelig · ${Levels.chapters.size - 1} offen", Learn.summary(Learn.topics(ch1.associate { it.id to 3 })))
    }

    @Test fun plannerSpreadsTheWork() {
        val plan = Plan(100, setOf(3, 4)); assertEquals(plan, Plan.decode(plan.encode())); assertNull(Plan.decode("quatsch")); assertNull(Plan.decode("5;"))
        assertEquals(11, Planner.open(plan, emptyMap()).size)
        assertEquals(3, Planner.today(plan, 96, emptyMap()).size)                 // 11 Aufgaben auf 4 Tage → 3 pro Tag
        assertEquals(Planner.MAX_PER_DAY, Planner.today(plan, 99, emptyMap()).size)
        assertTrue(Planner.today(plan, 101, emptyMap()).isEmpty()); assertNull(Planner.headline(plan, 101, emptyMap()))
        assertEquals("Klausur in 4 Tagen · heute 3 Aufgaben", Planner.headline(plan, 96, emptyMap()))
        assertEquals("Klausur heute – viel Erfolg!", Planner.headline(plan, 100, emptyMap()))
        val all = Levels.chapters.filter { it.number in plan.chapters }.flatMap { it.levels }.associate { it.id to 3 }
        assertEquals("Klausur in 1 Tag · alles sitzt ✓", Planner.headline(plan, 99, all))
        // Erst das noch nie Gespielte, dann die Wackelkandidaten
        assertEquals("3-2", Planner.open(plan, mapOf("3-1" to 2)).first().id); assertEquals("3-1", Planner.open(plan, mapOf("3-1" to 2)).last().id)
    }

    @Test fun classCodeRoundTrip() {
        val pack = listOf("3-1", "5-2", "9-3", "14-5").map { Levels.byId(it)!! }
        val code = ClassCode.encode(pack)
        assertEquals(pack.map { it.id }, ClassCode.find("Bitte spielen: $code")!!.map { it.id }); assertEquals(pack.map { it.id }, ClassCode.find(code.lowercase())!!.map { it.id })
        assertNull(ClassCode.find(code.dropLast(1) + if (code.last() == 'A') 'B' else 'A')); assertNull(ClassCode.find("RLK-ZZ0")); assertNull(ClassCode.find("nichts"))
        assertTrue(ClassCode.shareText(pack).contains(code)); assertTrue(ClassCode.resultText(pack, 3).contains("3 von 4"))
        assertEquals(ClassCode.MAX, ClassCode.find(ClassCode.encode(Levels.all.take(30)))!!.size)
    }

    @Test fun matchIsFairAndEnds() {
        var m = Match(); assertNull(m.winner)
        // Blau trifft dreimal, Pink zweimal: entschieden erst nach voller Runde
        listOf(true, true, true, true, true).forEach { m = m.after(it); assertNull(m.winner) }      // 3:2, Pink ist noch dran
        m = m.after(false); assertEquals(0, m.winner); assertEquals(listOf(3, 2), m.scores)
        var t = Match(); repeat(6) { t = t.after(true) }; assertNull(t.winner)                       // 3:3 → weiter
        t = t.after(false).after(true); assertEquals(1, t.winner)
    }

    @Test fun calculatorFacts() {
        fun l(vararg v: Int) = ShowItem(ShowType.LASER, v.toList())
        fun e(vararg v: Int) = ShowItem(ShowType.PLANE, v.toList())
        fun d(vararg v: Int) = ShowItem(ShowType.DOT, v.toList())
        fun b(vararg v: Int) = ShowItem(ShowType.BALL, v.toList())
        assertEquals(listOf("Laser 1 & Scheibe 2: Durchstoßpunkt (3|5|4), Winkel 41,81°"), Show.facts(listOf(l(1, 1, 0, 1, 2, 2), e(0, 0, 1, 4))))
        assertTrue(Show.facts(listOf(l(1, 1, 1, 2, 1, 1), l(7, 1, 1, -1, 1, 1))).single().contains("schneiden sich in (5|3|3)"))
        assertTrue(Show.facts(listOf(l(1, 3, 1, 1, 2, 2), l(4, 1, 0, 1, 0, -2))).single().contains("windschief, Abstand 3"))
        assertEquals(listOf("Scheibe 1 & Punkt 2: Abstand 3, Lotfußpunkt (2|4|3)"), Show.facts(listOf(e(1, 2, 2, 16), d(1, 2, 1))))
        assertTrue(Show.facts(listOf(e(0, 0, 1, 3), b(4, 4, 0, 4))).single().contains("Schnittkreis mit Radius 2,65"))
        assertTrue(Show.facts(listOf(d(2, 0, 4), d(6, 4, 0))).single().contains("Mittelpunkt (4|2|2)"))
        assertTrue(Show.facts(listOf(l(8, 0, 1, -2, 2, 1), b(4, 4, 3, 3))).single().contains("zwei Treffer"))
        assertTrue(Show.facts(listOf(e(1, 1, 0, 8), e(0, 1, 1, 6))).single().contains("Winkel 60°"))
        // Punkt lässt sich wie die anderen Bausteine teilen
        val items = listOf(d(1, 2, 3), l(0, 0, 0, 1, 1, 1)); assertEquals(items, Show.decode(Show.encode(items)))
    }

    @Test fun finaleGivesLuxSomethingToDo() {
        // Stochastik: Lux schießt genau die gezogenen pinken Kugeln ab, danach sind sie weg
        val l = Levels.byId("13-1")!!
        val fin = l.finale!!(l.solution); assertEquals(3, fin.beams.size); assertTrue(fin.beams.all { it.hit && it.color == "pink" })
        val before = l.objects(l.solution, Phase.AIM).count { it.type == "sphere" }; val after = l.objects(l.solution, Phase.DONE).count { it.type == "sphere" }
        assertEquals(10, before); assertEquals(7, after)
        assertEquals(fin.beams.map { it.to.list }, l.objects(l.solution, Phase.AIM).filter { it.type == "sphere" }.take(3).map { it.p })
        assertEquals(2, Levels.byId("13-3")!!.finale!!(mapOf("a" to listOf(2))).beams.size)
        // Achterbahn: Lux fährt die ganze Bahn ab und hält an der gesuchten Stelle
        val ride = Levels.byId("14-2")!!; val lap = ride.finale!!(ride.solution).path
        assertTrue(lap.size > 15); assertEquals(ride.objects(ride.solution, Phase.DONE).first { it.type == "drone" }.p, lap.last().list)
        assertTrue(lap.all { it.y in 0.0..8.0 && it.z in 0.0..6.5 })
        // Ohne eigenes Finale: Lux blitzt die wichtigen Punkte an – aber nur, wenn nicht schon geflogen oder geschossen wurde
        val plane = Levels.byId("5-3")!!
        val def = Finale.default(plane.objects(plane.solution, Phase.AIM), plane.objects(plane.solution, Phase.DONE), plane.evaluate(plane.solution))
        assertEquals(3, def.beams.size); assertTrue(def.beams.all { it.from.near(V3(8, 0, 1)) })
        val fly = Levels.byId("1-1")!!
        assertTrue(Finale.default(fly.objects(fly.solution, Phase.AIM), fly.objects(fly.solution, Phase.DONE), fly.evaluate(fly.solution)).beams.isEmpty())
    }
}
