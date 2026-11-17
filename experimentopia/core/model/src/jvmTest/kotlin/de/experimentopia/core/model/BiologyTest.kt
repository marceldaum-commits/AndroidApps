package de.experimentopia.core.model

import org.junit.Test
import org.junit.Assert

private fun assertEquals(a: Any?, b: Any?) = if (a is Double && b is Double) Assert.assertEquals(a, b, 1e-9) else Assert.assertEquals(a, b)
private fun assertEquals(a: Double, b: Double, d: Double) = Assert.assertEquals(a, b, d)
private fun assertTrue(c: Boolean, msg: String = "") = Assert.assertTrue(msg, c)

class BiologyTest {
    fun ans(id: String) = Catalog.byId(id)!!.predict.let { it.choices[it.answer].text.replace(Typo.NBSP, ' ') }
    fun tr(id: String) = Catalog.byId(id)!!.transfer.let { it.choices[it.answer].text.replace(Typo.NBSP, ' ') }

    @Test fun mendel() {
        assertEquals(mapOf("Aa" to 1.0), Genetics.cross("AA", "aa"))
        assertEquals(0.25, Genetics.share(Trait.ERBSE, "Aa", "Aa", "grün"))
        assertEquals(0.75, Genetics.share(Trait.ERBSE, "Aa", "Aa", "gelb"))
        assertEquals(0.5, Genetics.share(Trait.WUNDERBLUME, "Rr", "Rr", "rosa"))
        assertEquals("Aa", Genetics.norm("aA"))
        assertEquals(0.25, Blood.share("A0", "B0", "0")); assertEquals(0.25, Blood.share("A0", "B0", "AB"))
        val seeds = Genetics.sow("Aa", "Aa", 4000, 1)
        assertEquals(0.25, seeds.count { it == "aa" } / 4000.0, 0.03)
        assertEquals("alle gelb", ans("g1")); assertEquals("etwa ein Viertel", ans("g2")); assertEquals("50 %", ans("g3")); assertEquals("0 %", tr("g3"))
        assertEquals("50 %", ans("g4")); assertEquals("ja", ans("g5")); assertEquals("ja", ans("g6")); assertEquals("25 %", tr("g6"))
        assertEquals("Aa × aa", ans("g7")); assertEquals("aa × aa", tr("g7"))
    }

    @Test fun osmoseUndNachweise() {
        assertTrue(Osmosis.massChange(0.0) > 0); assertTrue(Osmosis.massChange(5.0) < 0); assertEquals(0, Osmosis.direction(1.0))
        assertEquals("Der Streifen wird leichter und schlapp", ans("o1")); assertEquals("etwa 1 %", ans("o4"))
        assertEquals("Kartoffel", ans("e1")); assertEquals("Butter", ans("e2")); assertEquals("nein", ans("e3")); assertEquals("Honig", tr("e3"))
        assertEquals("Eiklar", ans("e4")); assertEquals("ja", tr("e4"))
    }

    @Test fun enzyme() {
        assertTrue(Enzyme.activity(37.0) > Enzyme.activity(20.0)); assertEquals(0.0, Enzyme.activity(80.0))
        assertTrue(Enzyme.minutesUntilGone(37.0) < 10); assertTrue(Enzyme.minutesUntilGone(5.0) > 2 * Enzyme.minutesUntilGone(37.0))
        assertEquals("sie verschwindet", ans("e5")); assertEquals("bei 5 °C", ans("e6")); assertEquals("um 37 °C", tr("e6"))
        assertEquals("nein, gar nicht mehr", ans("e7")); assertEquals("bleibt", ans("e8"))
    }

    @Test fun fotosyntheseUndOekologie() {
        assertEquals(0.25, Photosynthesis.light(20.0), 1e-12)
        assertEquals("mehr Bläschen", ans("p1")); assertEquals("hört auf zu blubbern", ans("p2")); assertEquals("mehr", ans("p3"))
        assertEquals("ein Viertel", ans("p4")); assertEquals("ein Neuntel", tr("p4"))
        val p = PredatorPrey.simulate(400.0, 15.0)
        assertTrue(p.all { it.hares >= 0 && it.lynx >= 0 })
        assertTrue(PredatorPrey.firstPeak(p.map { it.hares }) < PredatorPrey.firstPeak(p.map { it.lynx }), "Luchse folgen den Hasen")
        assertEquals("Sie werden mehr – etwas später", ans("p5")); assertEquals("die Hasen", tr("p5"))
        assertTrue(PredatorPrey.simulate(400.0, 15.0, withLynx = false).last().hares > 1400)
        assertEquals("Sie werden weniger", ans("p7")); assertEquals("Sie werden weniger", tr("p7"))
    }

    @Test fun trennenUndFarben() {
        assertEquals("Filtern", ans("t1")); assertEquals("nein, es ist gelöst und läuft durch", tr("t1"))
        assertEquals("Eindampfen", ans("t2")); assertEquals("mit dem Magneten", ans("t3")); assertEquals("Scheidetrichter", ans("t4"))
        assertEquals("2", ans("t5")); assertEquals("Sieben", tr("t5")); assertEquals("Gelb", tr("t6"))
        assertEquals(2, Separation.steps(setOf(Component.SAND, Component.SALZ, Component.WASSER)))
        assertEquals("Grün", ans("m1")); assertEquals("Orange", tr("m1")); assertEquals("ein matschiges Braun-Grau", ans("m2")); assertEquals("Weiß", tr("m2"))
        assertEquals("Gelb + Rot", ans("m3")); assertEquals("Violett / Blauviolett", tr("m3")); assertEquals("Weiß zum Rot geben", ans("m4"))
        // Malkasten-Ziele erreichbar
        listOf("m1", "m2", "m3", "m4").forEach { id ->
            val s = Catalog.byId(id)!!.setup as PaintSetup
            val combos = s.paints.flatMap { a -> s.paints.map { b -> mapOf(a to 1) + mapOf(b to 1) } } + listOf(s.paints.associateWith { 1 })
            assertTrue(combos.any { Painting.name(Painting.mix(it)) == s.targetName }, "Ziel von $id erreichbar")
        }
    }

    @Test fun bioInsel() {
        assertTrue(Catalog.of(Island.BIO).size >= 27)
        assertTrue(Catalog.all.size >= 180)
        assertTrue(Search.find("Mendel").any { it.id == "g1" })
        assertTrue(Search.find("", Subject.BIOLOGIE).all { it.island == Island.BIO })
    }
}
