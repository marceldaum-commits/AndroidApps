package de.experimentopia.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Biology2Test {
    fun ans(id: String) = Catalog.byId(id)!!.predict.let { it.choices[it.answer].text.replace(Typo.NBSP, ' ') }
    fun tr(id: String) = Catalog.byId(id)!!.transfer.let { it.choices[it.answer].text.replace(Typo.NBSP, ' ') }

    @Test fun mikroskop() {
        assertEquals(Sight.PUNKT, Microscope.sight(Microbe.PANTOFFEL, 1))
        assertEquals(40, Microscope.best(Microbe.PANTOFFEL)); assertEquals(10, Microscope.best(Microbe.WASSERFLOH))
        assertEquals(100, Microscope.best(Microbe.AUGENTIER)); assertEquals(null, Microscope.best(Microbe.BAKTERIUM))
        assertEquals(Sight.ZU_GROSS, Microscope.sight(Microbe.WASSERFLOH, 400))
        assertEquals("nur ein winziger Punkt", ans("u1")); assertEquals("40-fach", tr("u1"))
        assertEquals("zu groß – nur ein Teil passt ins Bild", ans("u2")); assertEquals("10-fach", tr("u2"))
        assertEquals("nur ein winziger Punkt", ans("u3")); assertEquals("etwa 300", tr("u3")); assertEquals(0.55, Microscope.fieldMm(400), 1e-9)
        assertEquals("zum Licht", ans("u4")); assertEquals("40-fach", ans("u5"))
        // jedes Ziel ist einstellbar
        listOf("u1", "u2", "u3", "u4", "u5").forEach { id -> val s = Catalog.byId(id)!!.setup as MicroscopeSetup; assertTrue(id, Microscope.best(s.target!!) != null) }
    }

    @Test fun keimung() {
        val warm = Dish("w", true, 22.0)
        assertTrue(Germination.canGerminate(warm)); assertTrue(!Germination.canGerminate(Dish("t", false, 22.0)))
        assertTrue(!Germination.canGerminate(Dish("u", true, 22.0, air = false))); assertTrue(Germination.canGerminate(Dish("d", true, 22.0, light = false)))
        assertEquals(1.5, Germination.daysToSprout(warm), 1e-9)
        assertTrue(Germination.daysToSprout(Dish("k", true, 5.0)) > 7)
        assertTrue(Germination.heightCm(Dish("d", true, 22.0, light = false), 6.0) > Germination.heightCm(warm, 6.0))
        assertEquals("nur die nasse", ans("q1")); assertEquals("keimt noch gar nicht", ans("q2")); assertEquals("nein", ans("q3"))
        assertEquals("ja", ans("q4")); assertEquals("die im Dunkeln", ans("q5"))
    }

    @Test fun nahrungsketten() {
        assertTrue(FoodWeb.valid(listOf(Organism.GRAS, Organism.HASE, Organism.FUCHS)))
        assertTrue(!FoodWeb.valid(listOf(Organism.HASE, Organism.FUCHS)))
        assertTrue(!FoodWeb.valid(listOf(Organism.GRAS, Organism.FUCHS)))
        assertEquals(4, FoodWeb.longest())
        assertEquals(setOf(Organism.EULE), FoodWeb.starving(Organism.MAUS))
        assertEquals(100.0, FoodWeb.energy(10000.0, 2), 1e-9)
        assertEquals("eine Pflanze", ans("j1")); assertEquals("Gras", tr("j1")); assertEquals("4", ans("j2"))
        assertEquals("etwa 100 kJ", ans("j3")); assertEquals("die Schleiereule", ans("j4")); assertEquals("Ringelnatter", ans("j5"))
    }

    @Test fun herz() {
        assertEquals(4.9, Heart.litersPerMinute(30, Activity.RUHE), 1e-9)
        assertTrue(Heart.pulse(10, Activity.SPRINT) <= Heart.maxPulse(10))
        assertEquals("schlägt schneller", ans("v1")); assertEquals("80", ans("v2")); assertEquals("etwa 70", tr("v2"))
        assertEquals("etwa 5 Liter", ans("v3")); assertEquals("etwa 100 000-mal", ans("v4")); assertEquals("etwa 37 Millionen", tr("v4"))
        assertEquals("der des trainierten Kindes", ans("v5"))
    }

    @Test fun sicherheit() {
        Safety.ids.forEach { assertTrue("Sicherheitshinweis für unbekannten Versuch $it", Catalog.byId(it) != null) }
        // Versuche mit echter Gefahr haben einen Hinweis
        listOf("s1", "s7", "c7", "c8", "c11", "c19", "c20", "c22", "h6", "h8", "e4", "u1", "w10").forEach { assertTrue(it, Safety.note(it) != null) }
    }

    @Test fun traegheit() {
        assertEquals(Glide.V0, Glide.at(2.0, friction = false).second, 1e-12)
        assertTrue(Glide.at(10.0, friction = true).second <= 0.0)
        assertEquals("rollt immer weiter, gleich schnell", ans("n9"))
        assertEquals(Lab.NEWTON, Catalog.byId("n9")!!.setup.lab)
    }

    @Test fun zweihundert() { assertEquals(211, Catalog.all.size); assertEquals(Island.BIO, Catalog.byId("v1")!!.island) }
}

class TheoryTest {
    @org.junit.Test fun jedesThemaHatTheorie() = Topic.entries.forEach { t ->
        val c = Theory.of(t)
        org.junit.Assert.assertTrue("$t", c.idea.isNotBlank() && c.basics.size >= 3 && c.pro.size >= 2 && c.everyday.isNotBlank() && c.words.isNotEmpty())
    }
    @org.junit.Test fun jederVersuchFindetSeineTheorie() = Catalog.all.forEach { org.junit.Assert.assertEquals(it.topic, Theory.of(it).topic) }
}

class CurriculumTest {
    @org.junit.Test fun reihenfolgeUndAuswahl() {
        Subject.entries.forEach { s ->
            val all = Curriculum.topics(s)
            org.junit.Assert.assertEquals(Topic.entries.count { it.subject == s }, all.size)
            org.junit.Assert.assertEquals(all.map { Curriculum.startGrade(it) }.sorted(), all.map { Curriculum.startGrade(it) })
        }
        // Klasse 3: nur Fachbereiche mit Versuchen für Klasse 3
        Curriculum.topics(Subject.PHYSIK, 3).forEach { t -> org.junit.Assert.assertTrue(t.experiments().any { 3 in it.grades }) }
        // nächster Versuch passt zur Klasse und ist noch nicht gemacht
        val t = Topic.STROM
        val n = Curriculum.next(t, emptySet(), 5)!!
        org.junit.Assert.assertTrue(5 in n.grades)
        org.junit.Assert.assertNull(Curriculum.next(t, t.experiments().map { it.id }.toSet(), 5))
    }
}
