package de.experimentopia.core.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/** Optik: Werte wie im Physikbuch, Ziele mit den Reglern erreichbar, Antworten stimmen. */
class OpticsTest {
    private fun near(a: Double, b: Double, tol: Double = 0.05) = assertTrue("$a ≈ $b", abs(a - b) <= tol)

    @Test fun schatten() {
        near(Optics.shadow(10.0, 20.0, 100.0), 50.0); near(Optics.shadow(10.0, 40.0, 100.0), 25.0)
        near(Optics.lampFor(10.0, 20.0, 100.0), 50.0)
    }

    @Test fun spiegel() {
        near(Optics.reflectDir(-40.0, 0.0), 40.0)
        near(Optics.incidence(-40.0, 0.0), 50.0); near(Optics.incidence(-55.0, 0.0), 35.0)
        // Spiegel um 10° gedreht → Strahl um 20°
        near(Optics.angleDiff(Optics.reflectDir(-55.0, 10.0), Optics.reflectDir(-55.0, 0.0)), 20.0)
        near(Optics.reflectDir(-40.0, Optics.mirrorFor(-40.0, 130.0)), 130.0)
    }

    @Test fun brechung() {
        near(Optics.refract(42.0, 1.0, 1.33)!!, 30.2, 0.1)
        near(Optics.critical(1.5, 1.0)!!, 41.8, 0.1); near(Optics.critical(2.42, 1.0)!!, 24.4, 0.1); near(Optics.critical(1.33, 1.0)!!, 48.75, 0.1)
        assertNull(Optics.refract(45.0, 1.5, 1.0)); assertNull(Optics.critical(1.0, 1.33))
        near(Optics.refract(0.0, 1.0, 1.5)!!, 0.0)
    }

    @Test fun linse() {
        near(Optics.imageDist(10.0, 30.0)!!, 15.0); near(Optics.imageDist(10.0, 25.0)!!, 16.667, 0.01); near(Optics.imageDist(10.0, 15.0)!!, 30.0)
        near(Optics.magnification(10.0, 15.0)!!, 2.0); near(Optics.objectFor(10.0, 2.0), 15.0); near(Optics.objectFor(10.0, 1.0), 20.0)
        assertNull(Optics.imageDist(10.0, 5.0)); near(Optics.virtualMag(10.0, 5.0)!!, 2.0)
    }

    @Test fun antworten() {
        fun ans(id: String) = Catalog.byId(id)!!.predict.let { it.choices[it.answer].text.replace(Typo.NBSP, ' ') }
        fun tr(id: String) = Catalog.byId(id)!!.transfer.let { it.choices[it.answer].text.replace(Typo.NBSP, ' ') }
        assertEquals("näher an die Figur", ans("x1")); assertEquals("25 cm", ans("x2")); assertEquals("40 cm", tr("x2"))
        assertEquals("genauso schräg zur anderen Seite", ans("x3")); assertEquals("35°", ans("x4")); assertEquals("20°", tr("x4"))
        assertEquals("es knickt zum Lot hin (steiler)", ans("x5")); assertEquals("42°", tr("x6")); assertEquals("kleiner", ans("x7")); assertEquals("49°", tr("x7"))
        assertEquals("Sie steht auf dem Kopf", ans("x8")); assertEquals("etwa 17 cm", ans("x9")); assertEquals("30 cm", tr("x9"))
        assertEquals("15 cm", ans("x10")); assertEquals("20 cm", tr("x10")); assertEquals("gar kein scharfes Bild", ans("x11"))
    }

    /** Mit den Reglern (ganze cm / ganze Grad / halbe cm) ist jedes Ziel erreichbar */
    @Test fun zieleErreichbar() {
        Topic.OPTIK.experiments().forEach { e ->
            when (val s = e.setup) {
                is ShadowSetup -> s.targetCm?.let { t -> assertTrue(e.id, (2..(s.wallCm.toInt() - 2)).any { abs(Optics.shadow(s.objCm, it.toDouble(), s.wallCm) - t) <= 1.0 }) }
                is MirrorSetup -> s.targets.forEach { t -> assertTrue("${e.id} $t", (-45..90).any { Optics.angleDiff(Optics.reflectDir(s.beamDeg, it.toDouble()), t) <= 2.0 }) }
                is RefractSetup -> when (s.goal) {
                    RefractGoal.HIT -> assertTrue(e.id, (0..85).any { a -> Optics.refract(a.toDouble(), s.top.n, s.bottom.n)?.let { abs(it - s.targetDeg!!) <= 1.5 } == true })
                    RefractGoal.TOTAL -> assertTrue(e.id, (0..85).any { Optics.refract(it.toDouble(), s.top.n, s.bottom.n) == null })
                    else -> {}
                }
                is LensSetup -> {
                    val gs = s.g?.let { listOf(it) } ?: (6..100).map { it / 2.0 }
                    val ok = gs.any { g -> when (s.goal) {
                        LensGoal.MAGNIFIER -> g < s.f
                        else -> (10..140).any { sc -> val b = Optics.imageDist(s.f, g); b != null && abs(sc / 2.0 - b) <= 0.5 && (s.mag == null || abs(b / g - s.mag) <= 0.05) }
                    } }
                    assertTrue(e.id, ok)
                }
                else -> fail("${e.id}: unerwartetes Gerät")
            }
        }
    }

    @Test fun kommtBald() {
        assertEquals("Gratis-Umfang", 78, Pro.freeCount)
        assertTrue(Coming.all.size >= 15)
        Subject.entries.forEach { assertTrue("$it hat geplante Themen", Coming.of(it).isNotEmpty()) }
        // geplante Themen gibt es noch nicht als Fachbereich
        Coming.all.forEach { u -> assertTrue(u.title, Topic.entries.none { it.title == u.title }); assertTrue(u.grades.first >= 3 && u.grades.last <= 10) }
    }
}
