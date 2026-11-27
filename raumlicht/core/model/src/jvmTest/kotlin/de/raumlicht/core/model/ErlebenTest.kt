package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

/** Profil seit 2.18.32: nur Geometrie im Raum als Aufgaben; Analysis und Zufall zum Erleben – mit dem Rotationskörper-Labor. */
class ErlebenTest {
    @Test fun nurNochGeometrieInDerTour() {
        assertEquals(18, Levels.chapters.size); assertEquals(107, Levels.all.size); assertTrue(Levels.chapters.all { it.area == Area.GEOMETRIE })
        assertEquals(10, Levels.removed.size); assertEquals(52, Levels.removed.sumOf { it.levels.size })
        assertEquals(setOf(12, 13, 14, 15, 16, 17, 22, 23, 24, 25), Levels.removed.map { it.number }.toSet())
        for (c in Levels.removed) for (l in c.levels) assertNull("gestrichen: ${l.id}", Levels.byId(l.id))
    }

    /** Angezeigt wird lückenlos 1 bis 17; die internen Nummern (Sterne, Links von außen) bleiben. */
    @Test fun kapitelLueckenlosAngezeigt() {
        assertEquals((1..18).toList(), Levels.chapters.map { Levels.shown(it.number) })
        assertEquals(12, Levels.shown(18)); assertEquals(15, Levels.shown(21)); assertEquals(16, Levels.shown(26)); assertEquals(17, Levels.shown(27)); assertEquals(18, Levels.shown(28)); assertEquals(5, Levels.shown(5))
        assertEquals("12-3", Levels.label("18-3")); assertEquals("16-8", Levels.label("26-8")); assertEquals("5-6", Levels.label("5-6")); assertEquals("tag-7", Levels.label("tag-7")); assertEquals("14-1", Levels.label("14-1"))
        // von außen verlinkte Ziele gibt es weiter
        for (id in listOf("5-6", "3-1", "5-1")) assertNotNull(id, Levels.byId(id))
    }

    /** Nirgends taucht mehr eine gestrichene Aufgabe auf: Abi-Teil, Pflichtteil, Tagesaufgabe, endloses Üben, Formelsammlung, „Kommt bald“. */
    @Test fun nichtsGestrichenesTauchtAuf() {
        assertEquals(listOf("zelt", "leinwand", "drohnen", "planetarium", "dach", "kran", "ballon", "rutsche", "pyramide", "spiegelsaal", "schiffe"), Exams.all.map { it.id })
        val gone = Levels.removed.flatMap { it.levels }.map { it.id }.toSet()
        for (seed in 1L..40) { val q = Quick.pick(seed); assertEquals(Quick.TASKS, q.size); assertTrue(q.none { it.id in gone }); assertEquals(q.size, q.map { it.id }.toSet().size) }
        for (day in 20_770L..20_830L) assertNotEquals("Rotationskörper", Daily.forDay(day).title)
        assertFalse(Gen.Kind.ROTATION in Gen.active); assertFalse(JamTopic.entries.any { Gen.Kind.ROTATION in it.kinds }); assertEquals("Bewegung", JamTopic.BEWEGUNG.title)
        assertTrue(Practice.sources.none { it.id == "ROTATION" })
        for (t in JamTopic.entries) for (n in 0..30) assertNotEquals("Rotationskörper", Practice.level(t, null, 5, n).title)
        for (card in Sheet.cards) for (n in card.chapters) assertTrue("${card.title} → Kapitel $n", Levels.chapters.any { it.number == n })
        for (t in Soon.topics) for (bad in listOf("Normalverteilung", "Zufall", "Sinus", "Scharen", "Unabhängig")) assertFalse(t.title, t.title.contains(bad))
        assertEquals(10, Erleben.links.size); assertTrue(Erleben.links.all { it.url.startsWith("https://trainer.mathenachhilfe-online.com/#/u/") && it.unit.matches(Regex("[a-z0-9-]+")) })
        assertTrue(Pro.benefits.first().contains("107 Aufgaben in 18 Kapiteln"))
    }

    /** Rotationskörper-Labor: Jedes Volumen stimmt mit der Scheiben-Rechnung überein – für alle Voreinstellungen und für alle einstellbaren Werte. */
    @Test fun volumenIstNachgerechnet() {
        for ((name, s) in RotationLab.presets) { assertNull(name, RotationLab.problem(s)); assertEquals(name, RotationLab.numeric(s), RotationLab.volume(s), 1e-4) }
        var checked = 0
        for (shape in RotationLab.Shape.entries) for (p in RotationLab.RANGE_P.getValue(shape)) for (q in RotationLab.RANGE_Q[shape] ?: 0..0)
            for (a in RotationLab.RANGE_X) for (b in RotationLab.RANGE_X) { val s = RotationLab.Setup(shape, p, q, a, b)
                if (RotationLab.problem(s) != null) continue
                assertEquals("$s", RotationLab.numeric(s, 4000), RotationLab.volume(s), 1e-3 * (1 + RotationLab.volume(s))); assertTrue("$s", RotationLab.volume(s) > 0)
                assertEquals("$s", 6, RotationLab.way(s).size); assertTrue(RotationLab.way(s).last().contains("π ≈")); checked++ }
        assertTrue("genug Einstellungen geprüft: $checked", checked > 400)
    }

    /** Bekannte Körper mit den Schulformeln: Zylinder, Kegel, Kugel, Halbkugel. */
    @Test fun bekannteKoerper() {
        fun pi(s: RotationLab.Setup) = RotationLab.piTimes(s).toString()
        assertEquals("16", pi(RotationLab.Setup(RotationLab.Shape.GERADE, 0, 2, 0, 4)))          // Zylinder r = 2, h = 4: π · 4 · 4
        assertEquals("9", pi(RotationLab.Setup(RotationLab.Shape.GERADE, 1, 0, 0, 3)))           // Kegel r = 3, h = 3: ⅓ · π · 9 · 3
        assertEquals("36", pi(RotationLab.Setup(RotationLab.Shape.KREIS, 3, 0, -3, 3)))          // Kugel r = 3: ⁴⁄₃ · π · 27
        assertEquals("18", pi(RotationLab.Setup(RotationLab.Shape.KREIS, 3, 0, 0, 3)))           // Halbkugel
        assertEquals("8", pi(RotationLab.Setup(RotationLab.Shape.WURZEL, 1, 0, 0, 4)))           // ∫ x dx von 0 bis 4
        assertEquals("15/2", pi(RotationLab.Setup(RotationLab.Shape.WURZEL, 1, 1, 0, 3)))        // ∫ (x + 1) dx von 0 bis 3
        assertEquals("8/5", pi(RotationLab.Setup(RotationLab.Shape.PARABEL, 5, 0, 0, 2)))        // ¼ · 32/5
        assertEquals("21", pi(RotationLab.Setup(RotationLab.Shape.GERADE, 1, 1, 0, 3)))          // ∫ (x + 1)² dx = 64/3 − 1/3
        assertEquals(36 * PI, RotationLab.volume(RotationLab.Setup(RotationLab.Shape.KREIS, 3, 0, -3, 3)), 1e-9)
        assertTrue(RotationLab.check(RotationLab.Setup(RotationLab.Shape.GERADE, 0, 2, 0, 4))!!.contains("16π")); assertTrue(RotationLab.check(RotationLab.Setup(RotationLab.Shape.KREIS, 3, 0, -3, 3))!!.contains("36π"))
        assertTrue(RotationLab.check(RotationLab.Setup(RotationLab.Shape.GERADE, 1, 0, 0, 3))!!.contains("9π")); assertNull(RotationLab.check(RotationLab.Setup(RotationLab.Shape.WURZEL, 1, 0, 0, 4)))
        assertEquals("Kugel mit Radius 3", RotationLab.name(RotationLab.Setup(RotationLab.Shape.KREIS, 3, 0, -3, 3))); assertTrue(RotationLab.name(RotationLab.Setup(RotationLab.Shape.GERADE, 0, 2, 0, 4)).startsWith("Zylinder"))
    }

    /** Unpassende Eingaben bekommen eine verständliche Rückmeldung statt eines falschen Körpers. */
    @Test fun verstaendlicheRueckmeldung() {
        assertTrue(RotationLab.problem(RotationLab.Setup(RotationLab.Shape.GERADE, 1, 0, 3, 3))!!.contains("kleiner")); assertTrue(RotationLab.problem(RotationLab.Setup(RotationLab.Shape.GERADE, 1, 0, 4, 2))!!.contains("kleiner"))
        assertTrue(RotationLab.problem(RotationLab.Setup(RotationLab.Shape.WURZEL, 1, 0, -2, 3))!!.contains("negative Zahl")); assertTrue(RotationLab.problem(RotationLab.Setup(RotationLab.Shape.KREIS, 2, 0, -3, 2))!!.contains("von −2 bis 2"))
        assertTrue(RotationLab.problem(RotationLab.Setup(RotationLab.Shape.GERADE, 0, 0, 0, 3))!!.contains("dreht sich nichts")); assertTrue(RotationLab.problem(RotationLab.Setup(RotationLab.Shape.GERADE, 2, 0, 0, 3))!!.contains("zu dick")); assertTrue(RotationLab.problem(RotationLab.Setup(RotationLab.Shape.GERADE, 1, 0, -2, 3))!!.contains("unter die Achse"))
        assertEquals("f(x) = x + 1", RotationLab.text(RotationLab.Setup(RotationLab.Shape.GERADE, 1, 1, 0, 3))); assertEquals("f(x) = 2", RotationLab.text(RotationLab.Setup(RotationLab.Shape.GERADE, 0, 2, 0, 4)))
        assertEquals("f(x) = −2 · x + 4", RotationLab.text(RotationLab.Setup(RotationLab.Shape.GERADE, -2, 4, 0, 2))); assertEquals("f(x) = √(x + 1)", RotationLab.text(RotationLab.Setup(RotationLab.Shape.WURZEL, 1, 1, 0, 3)))
        assertEquals("f(x) = √(9 − x²)", RotationLab.text(RotationLab.Setup(RotationLab.Shape.KREIS, 3, 0, -3, 3))); assertEquals("f(x) = 0,5 · x²", RotationLab.text(RotationLab.Setup(RotationLab.Shape.PARABEL, 5, 0, 0, 2)))
        val way = RotationLab.way(RotationLab.Setup(RotationLab.Shape.GERADE, 1, 1, 0, 3))
        assertEquals(listOf("Formel: V = π · ∫ f(x)² dx von 0 bis 3", "Quadrieren: f(x)² = x² + 2 · x + 1", "Stammfunktion: F(x) = 1/3 · x³ + 1 · x² + 1 · x",
            "Grenzen einsetzen: F(3) = 21 und F(0) = 0", "Abziehen: F(3) − F(0) = 21", "Mit π malnehmen: V = 21 · π ≈ 65,97"), way)
        assertTrue(abs(RotationLab.profile(RotationLab.Setup(RotationLab.Shape.KREIS, 3, 0, -3, 3)).maxOf { it.second } - 3.0) < 1e-9)
    }
}
